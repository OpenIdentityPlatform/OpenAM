/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS.
 */
package org.forgerock.openam.upgrade.steps;

import static com.sun.identity.shared.xml.XMLUtils.getNodeAttributeValue;
import static com.sun.identity.shared.xml.XMLUtils.parseAttributeValuePairTags;
import static org.forgerock.openam.utils.CollectionUtils.transformSet;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.InputStream;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.delegation.DelegationManager;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SMSUtils;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import org.forgerock.openam.upgrade.UpgradeServices;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import org.forgerock.openam.upgrade.UpgradeUtils;
import org.forgerock.openam.utils.IOUtils;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.util.promise.Function;
import org.forgerock.util.promise.NeverThrowsException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Upgrade step looks at the delegation XML (amDelegation.xml) and compares it against the current service model to
 * identify any additions. Additions include new permission or privilege entries, or additional permissions that have
 * been added to a preexisting privilege. These additions are then reflected in the service model during upgrade.
 * <p />
 * This upgrade logic makes assumptions that the XML is in a consistent format as well as the the service model. It also
 * makes an assumption that for each listed permission under a privilege that there is a corresponding permission entry.
 *
 * @since 12.0.0
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.UpgradeServiceSchemaStep")
public class DelegationConfigUpgradeStep extends AbstractUpgradeStep {

    private static final String AUDIT_REPORT = "upgrade.delegation";
    private static final String DELEGATION_PLACEHOLDER = "%DELEGATION_UPDATE_DATA%";
    private static final String AUDIT_PERM_NEW_START = "upgrade.delegation.permission.new.start";
    private static final String AUDIT_PERM_NEW = "upgrade.delegation.permission.new";
    private static final String AUDIT_PRIV_NEW_START = "upgrade.delegation.privilege.new.start";
    private static final String AUDIT_PRIV_NEW = "upgrade.delegation.privilege.new";
    private static final String AUDIT_PRIV_UPDATE_START = "upgrade.delegation.privilege.update.start";
    private static final String AUDIT_PRIV_UPDATE = "upgrade.delegation.privilege.update";
    private static final String AUDIT_UPGRADE_SUCCESS = "upgrade.success";
    private static final String AUDIT_UPGRADE_FAIL = "upgrade.failed";

    private static final String DELEGATION_XML = "amDelegation.xml";

    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String PERMISSIONS = "Permissions";
    private static final String PERMISSION = "Permission";
    private static final String PRIVILEGES = "Privileges";
    private static final String PRIVILEGE = "Privilege";
    private static final String LIST_OF_PERMISSIONS = "listOfPermissions";
    private static final String RESOURCE = "resource";
    private static final int CONFIG_PRIORITY = 0;

    private final List<ChangeSet<String, Node>> newPermissions;
    private final List<ChangeSet<String, Node>> newPrivileges;
    private final List<ChangeSet<String, Set<String>>> privilegeUpdates;

    private final ServiceConfigManager configManager;
    private final Function<String, String, NeverThrowsException> tagSwapFunc;

    private ServiceConfig permissionsConfig;
    private ServiceConfig privilegesConfig;

    @Inject
    public DelegationConfigUpgradeStep(
            @Named(DelegationManager.DELEGATION_SERVICE) final ServiceConfigManager configManager,
            @Named("tagSwapFunc") final Function<String, String, NeverThrowsException> tagSwapFunc,
            final PrivilegedAction<SSOToken> adminTokenAction,
            @Named(DataLayerConstants.DATA_LAYER_BINDING) final ConnectionFactory connectionFactory) {
        super(adminTokenAction, connectionFactory);

        this.configManager = configManager;
        this.tagSwapFunc = tagSwapFunc;

        newPermissions = new ArrayList<ChangeSet<String, Node>>();
        newPrivileges = new ArrayList<ChangeSet<String, Node>>();
        privilegeUpdates = new ArrayList<ChangeSet<String, Set<String>>>();
    }

    @Override
    public void initialize() throws UpgradeException {
        // Extract out all sub configurations from the delegation XML doc.
        final Document delegationDocument = getDelegationDocument();
        final NodeList configNodes = delegationDocument.getElementsByTagName(SMSUtils.SUB_CONFIG);

        try {
            initConfig();

            // Check each permission/privilege config node against the service model.
            for (int idx = 0; idx < configNodes.getLength(); idx++) {

                final Node configNode = configNodes.item(idx);
                final String id = getNodeAttributeValue(configNode, ID);
                final String name = getNodeAttributeValue(configNode, NAME);

                if (PERMISSION.equals(id)) {
                    checkPermission(name, configNode, permissionsConfig);
                } else if (PRIVILEGE.equals(id)) {
                    checkPrivilege(name, configNode, privilegesConfig);
                }
            }

        } catch (SSOException ssoE) {
            throw new UpgradeException("Failed analysing the delegation delta", ssoE);
        } catch (SMSException smsE) {
            throw new UpgradeException("Failed analysing the delegation delta", smsE);
        }

    }

    /**
     * Cache gets cleared after #initialize is called but before #perform, so need to re-get the config options each
     * time.
     */
    private void initConfig() throws SMSException, SSOException {
        final ServiceConfig delegationConfig = configManager.getGlobalConfig(null);
        permissionsConfig = delegationConfig.getSubConfig(PERMISSIONS);
        privilegesConfig = delegationConfig.getSubConfig(PRIVILEGES);
    }

    /**
     * Given the permission name identifies whether that permission already exists.
     * If it doesn't it registers creation of a new privilege.
     *
     * @param name
     *         the permission name
     * @param configNode
     *         the permission as intended to be, represented in the XML doc
     * @param permissionsConfig
     *         the permission as is, represented in the service model
     *
     * @throws SMSException
     *         should an error occur during the identification of permission changes
     * @throws SSOException
     *         should an error occur during the identification of permission changes
     */
    private void checkPermission(final String name, final Node configNode, final ServiceConfig permissionsConfig)
            throws SMSException, SSOException {

        // Retrieve the identified permission.
        final ServiceConfig permissionConfig = permissionsConfig.getSubConfig(name);

        if (permissionConfig == null) {
            // Register new permission.
            newPermissions.add(ChangeSet.newInstance(name, configNode));
        }
    }

    /**
     * Given the privilege name identifies whether that privilege already exists. If it doesn't it registers creation of
     * a new privilege. If it does it compares that the privileges' permissions list is up to date. If new permissions
     * are present it registers the privilege for an update.
     *
     * @param name
     *         the privilege name
     * @param configNode
     *         the privilege as intended to be, represented in the XML doc
     * @param privilegesConfig
     *         the privilege as is, represented in the service model
     *
     * @throws SMSException
     *         should an error occur during the identification of privilege changes
     * @throws SSOException
     *         should an error occur during the identification of privilege changes
     */
    private void checkPrivilege(final String name, final Node configNode, final ServiceConfig privilegesConfig)
            throws SMSException, SSOException {

        // Retrieve the identified privilege.
        final ServiceConfig privilegeConfig = privilegesConfig.getSubConfig(name);

        if (privilegeConfig != null) {

            // Service API needs updating to support generics.
            @SuppressWarnings("unchecked")
            final Map<String, Set<String>> existingAttributes = privilegeConfig.getAttributes();
            final Map<String, Set<String>> passedAttributes = parseAttributeValuePairTags(configNode);

            final Set<String> existingPermissionList = existingAttributes.get(LIST_OF_PERMISSIONS);
            final Set<String> passedPermissionList = passedAttributes.get(LIST_OF_PERMISSIONS);

            // Identify any permissions missing from the current privilege configuration.
            final Set<String> newPermissions = new HashSet<String>();
            for (String passedPermission : passedPermissionList) {
                if (!existingPermissionList.contains(passedPermission)) {
                    newPermissions.add(passedPermission);
                }
            }

            if (!newPermissions.isEmpty()) {
                // Register new permissions to be added to an existing privilege.
                privilegeUpdates.add(ChangeSet.newInstance(name, newPermissions));
            }
        } else {
            // Register new privilege.
            newPrivileges.add(ChangeSet.newInstance(name, configNode));
        }
    }

    @Override
    public boolean isApplicable() {
        // Are there any identified additions/updates...
        return !newPrivileges.isEmpty() || !privilegeUpdates.isEmpty() || !newPermissions.isEmpty();
    }

    @Override
    public void perform() throws UpgradeException {
        try {
            initConfig();
            if (!newPermissions.isEmpty()) {
                UpgradeProgress.reportStart(AUDIT_PERM_NEW_START);
                handleNewPermissions();
                UpgradeProgress.reportEnd(AUDIT_UPGRADE_SUCCESS);
            }

            if (!newPrivileges.isEmpty()) {
                UpgradeProgress.reportStart(AUDIT_PRIV_NEW_START);
                handleNewPrivileges();
                UpgradeProgress.reportEnd(AUDIT_UPGRADE_SUCCESS);
            }

            if (!privilegeUpdates.isEmpty()) {
                UpgradeProgress.reportStart(AUDIT_PRIV_UPDATE_START);
                handlePrivilegeUpdates();
                UpgradeProgress.reportEnd(AUDIT_UPGRADE_SUCCESS);
            }
        } catch (SSOException ssoE) {
            UpgradeProgress.reportEnd(AUDIT_UPGRADE_FAIL);
            throw new UpgradeException("Failed performing the upgrade of delegation", ssoE);
        } catch (SMSException smsE) {
            UpgradeProgress.reportEnd(AUDIT_UPGRADE_FAIL);
            throw new UpgradeException("Failed performing the upgrade of delegation", smsE);
        }
    }

    /**
     * For each identified new permission, create a new entry under the permissions config.
     *
     * @throws SSOException
     *         should some error occur during the creation of new permissions
     * @throws SMSException
     *         should some error occur during the creation of new permissions
     */
    private void handleNewPermissions() throws SSOException, SMSException {
        for (ChangeSet<String, Node> change : newPermissions) {
            final String configName = change.getIdentifier();
            final Node configNode = change.getData();

            final Map<String, Set<String>> newAttributes = parseAttributeValuePairTags(configNode);
            newAttributes.put(RESOURCE, transformSet(newAttributes.get(RESOURCE), tagSwapFunc));

            permissionsConfig.addSubConfig(configName, PERMISSION, CONFIG_PRIORITY, newAttributes);
        }
    }

    /**
     * For each identified new privilege, create a new entry under the privileges config.
     *
     * @throws SSOException
     *         should some error occur during the creation of the new privileges
     * @throws SMSException
     *         should some error occur during the creation of the new privileges
     */
    private void handleNewPrivileges() throws SSOException, SMSException {
        for (ChangeSet<String, Node> change : newPrivileges) {
            final String configName = change.getIdentifier();
            final Node configNode = change.getData();

            final Map<String, Set<String>> newAttributes = parseAttributeValuePairTags(configNode);
            privilegesConfig.addSubConfig(configName, PRIVILEGE, CONFIG_PRIORITY, newAttributes);
        }
    }

    /**
     * For each identified privilege update, amend to the list of permissions those which are new.
     *
     * @throws SSOException
     *         should some error occur during updating the identified privileges
     * @throws SMSException
     *         should some error occur during updating the identified privileges
     */
    private void handlePrivilegeUpdates() throws SSOException, SMSException {
        for (ChangeSet<String, Set<String>> change : privilegeUpdates) {
            final String configName = change.getIdentifier();
            final Set<String> newPermissions = change.getData();

            final ServiceConfig privilegeConfig = privilegesConfig.getSubConfig(configName);
            // Adds in the additional permissions to the existing list of permissions.
            privilegeConfig.addAttribute(LIST_OF_PERMISSIONS, newPermissions);
        }
    }

    @Override
    public String getShortReport(String delimiter) {
        final StringBuilder builder = new StringBuilder();

        if (!newPermissions.isEmpty()) {
            builder.append(BUNDLE.getString(AUDIT_PERM_NEW)).append(delimiter);
        }

        if (!newPrivileges.isEmpty()) {
            builder.append(BUNDLE.getString(AUDIT_PRIV_NEW)).append(delimiter);
        }

        if (!privilegeUpdates.isEmpty()) {
            builder.append(BUNDLE.getString(AUDIT_PRIV_UPDATE)).append(delimiter);
        }

        return builder.toString();
    }

    @Override
    public String getDetailedReport(String delimiter) {
        final StringBuilder builder = new StringBuilder();

        if (!newPermissions.isEmpty()) {
            builder.append(BUNDLE.getString(AUDIT_PERM_NEW))
                    .append(':')
                    .append(delimiter)
                    .append(flattenChangeIdentifiers(newPermissions))
                    .append(delimiter);
        }

        if (!newPrivileges.isEmpty()) {
            builder.append(BUNDLE.getString(AUDIT_PRIV_NEW))
                    .append(':')
                    .append(delimiter)
                    .append(flattenChangeIdentifiers(newPrivileges))
                    .append(delimiter);
        }

        if (!privilegeUpdates.isEmpty()) {
            builder.append(BUNDLE.getString(AUDIT_PRIV_UPDATE))
                    .append(':')
                    .append(delimiter)
                    .append(flattenChangeIdentifiers(privilegeUpdates))
                    .append(delimiter);
        }

        final Map<String, String> reportContents = new HashMap<String, String>();
        reportContents.put(DELEGATION_PLACEHOLDER, builder.toString());
        reportContents.put(UpgradeServices.LF, delimiter);

        return UpgradeServices.tagSwapReport(reportContents, AUDIT_REPORT);
    }

    /**
     * Flattens the passed list of change instances to a string delimiter spaced identifier strings.
     *
     * @param changeSets
     *         the list of change instances
     * @param <T>
     *         the type of the change identifier
     * @param <S>
     *         the type of the change data
     *
     * @return a string of change identifiers separated by the delimiter
     */
    private <T, S> String flattenChangeIdentifiers(final List<ChangeSet<T, S>> changeSets) {
        final StringBuilder builder = new StringBuilder();

        for (ChangeSet<T, S> change : changeSets) {
            builder.append(change.getIdentifier().toString()).append(INDENT);
        }

        return builder.toString();
    }

    /**
     * Instantiates a {@link org.w3c.dom.Document} instance that represents the delegation xml configuration file.
     *
     * @return a document instance representing the delegation xml file
     *
     * @throws UpgradeException
     *         should an error occur during creation of the document
     */
    protected Document getDelegationDocument() throws UpgradeException {
        InputStream serviceStream = null;
        final Document doc;

        try {
            DEBUG.message("Reading delegation configuration file: " + DELEGATION_XML);
            serviceStream = getClass().getClassLoader().getResourceAsStream(DELEGATION_XML);
            doc = UpgradeUtils.parseServiceFile(serviceStream, getAdminToken());
        } finally {
            IOUtils.closeIfNotNull(serviceStream);
        }

        return doc;
    }

    /**
     * Internal class to hold the change state.
     *
     * @param <T>
     *         the type of the identifier
     * @param <S>
     *         the type of the data
     */
    private static final class ChangeSet<T, S> {

        private final T identifier;
        private final S data;

        ChangeSet(final T identifier, final S data) {
            this.identifier = identifier;
            this.data = data;
        }

        T getIdentifier() {
            return identifier;
        }

        S getData() {
            return data;
        }

        /**
         * Instantiates a new change instance.
         *
         * @param identifier
         *         the change identifier
         * @param data
         *         the data
         * @param <T>
         *         the type of the identifier
         * @param <S>
         *         the type of the data
         *
         * @return a change instance representing the change set
         */
        static <T, S> ChangeSet<T, S> newInstance(final T identifier, final S data) {
            return new ChangeSet<T, S>(identifier, data);
        }

    }

}
