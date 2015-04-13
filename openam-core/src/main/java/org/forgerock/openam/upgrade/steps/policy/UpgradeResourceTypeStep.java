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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.upgrade.steps.policy;

import static com.sun.identity.shared.xml.XMLUtils.getNodeAttributeValue;
import static org.forgerock.openam.entitlement.utils.EntitlementUtils.*;
import static org.forgerock.openam.upgrade.UpgradeServices.LF;
import static org.forgerock.openam.utils.CollectionUtils.isNotEmpty;
import static org.forgerock.openam.utils.CollectionUtils.transformSet;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.EntitlementConfiguration;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.opensso.DataStore;
import com.sun.identity.entitlement.util.SearchFilter;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SMSUtils;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import org.forgerock.openam.entitlement.ResourceType;
import org.forgerock.openam.entitlement.configuration.ResourceTypeSmsAttributes;
import org.forgerock.openam.entitlement.configuration.SmsAttribute;
import org.forgerock.openam.entitlement.service.ResourceTypeService;
import org.forgerock.openam.entitlement.utils.EntitlementUtils;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import org.forgerock.openam.upgrade.UpgradeServices;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.promise.Function;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.query.QueryFilter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.inject.Inject;
import javax.inject.Named;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This upgrade step is responsible for introducing the Resource Type policy model object. It runs after
 * UpgradeEntitlementSubConfigsStep to ensure that we don't upgrade the model before the schema is upgraded.
 *
 * @since 13.0.0
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.UpgradeEntitlementSubConfigsStep")
public class UpgradeResourceTypeStep extends AbstractEntitlementUpgradeStep {

    private static final String RESOURCES_TYPE_NAME_SUFFIX = "ResourceType";
    private static final String RESOURCE_TYPE_DESCRIPTION = "This resource type was created during upgrade for ";

    private static final String AUDIT_REPORT = "upgrade.entitlement.resourcetype.report";
    private static final String AUDIT_CREATE_RESOURCE_TYPE_START = "upgrade.entitlement.create.resourcetype.start";
    private static final String AUDIT_MODIFIED_APP_UUID_START = "upgrade.entitlement.modified.applicationuuid.start";
    private static final String AUDIT_MODIFIED_POLICY_UUID_START = "upgrade.entitlement.modified.policyuuid.start";
    private static final String AUDIT_NEW_RESOURCE_TYPE = "upgrade.entitlement.new.resourcetype";
    private static final String AUDIT_MODIFIED_APPLICATION = "upgrade.entitlement.modified.application";
    private static final String AUDIT_MODIFIED_POLICIES = "upgrade.entitlement.modified.policy";

    private static final String POLICY_SEARCH =
            "(&(ou=application={0})(|(!(ou=resourceTypeUuid=*))(ou=resourceTypeUuid=\\00)))";

    private final ResourceTypeService resourceTypeService;
    private final ServiceConfigManager configManager;
    private final Set<String> defaultApplicationNames;
    private class ResourceTypeState {
        private boolean applicationNeedsResourceType = false;
        private boolean policiesNeedsResourceType = false;
        private String appName;
        private String resourceTypeName;
        private Set<String> actions;
        private Set<String> patterns;
        private Set<String> policyNames;
    }
    private final Map<String, Set<ResourceTypeState>> resourceTypeStatePerRealm;

    private int upgradeableApplicationCount = 0;
    private int upgradeablePrivilegeCount = 0;

    @Inject
    public UpgradeResourceTypeStep(
            @Named(EntitlementUtils.SERVICE_NAME) final ServiceConfigManager configManager,
            final ResourceTypeService resourceTypeService,
            final PrivilegedAction<SSOToken> adminTokenAction,
            @DataLayer(ConnectionType.DATA_LAYER) final ConnectionFactory connectionFactory) {
        super(adminTokenAction, connectionFactory);

        this.configManager = configManager;
        this.resourceTypeService = resourceTypeService;
        this.defaultApplicationNames = new HashSet<String>();
        this.resourceTypeStatePerRealm = new HashMap<String, Set<ResourceTypeState>>();
    }

    /**
     * Default applications will have resource types associated with them during the schema upgrade, so here we list
     * the other applications that does not already have resource types. We also list policies that does not yet
     * have resource types.
     */
    @Override
    public void initialize() throws UpgradeException {
        DEBUG.message("Initialising the upgrade step for adding resource types to the entitlement model");
        populateDefaultApplications();

        final Set<String> realms = getRealmNamesFromParent();

        for (String realm : realms) {
            final ServiceConfig appConfig = getApplicationsConfig(realm);
            if (appConfig == null) {
                continue;
            }
            final Set<String> appNames = getApplicationNames(appConfig);
            final Set<ResourceTypeState> states = new HashSet<ResourceTypeState>();

            for (String appName : appNames) {
                final Map<String, Set<String>> appData = getApplicationData(appConfig, appName);
                final ResourceTypeState state = new ResourceTypeState();
                state.appName = appName;
                if (applicationEligibleForUpgrade(realm, appName, appData)) {
                    state.applicationNeedsResourceType = true;
                    state.actions = appData.get(CONFIG_ACTIONS);
                    state.patterns = appData.get(CONFIG_RESOURCES);
                    upgradeableApplicationCount += 1;
                }
                state.policyNames = policiesEligibleForUpgrade(appName, realm);
                state.policiesNeedsResourceType = !state.policyNames.isEmpty();
                upgradeablePrivilegeCount += state.policyNames.size();
                states.add(state);
            }
            if (!states.isEmpty()) {
                resourceTypeStatePerRealm.put(realm, states);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isApplicable() {
        return upgradeableApplicationCount > 0 || upgradeablePrivilegeCount > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void perform() throws UpgradeException {

        for (Map.Entry<String, Set<ResourceTypeState>> entry : resourceTypeStatePerRealm.entrySet()) {
            final String realm = entry.getKey();
            final EntitlementConfiguration ec = EntitlementConfiguration.getInstance(getAdminSubject(), realm);
            final PrivilegeManager pm = PrivilegeManager.getInstance(realm, getAdminSubject());

            for (ResourceTypeState state : entry.getValue()) {
                if (state.applicationNeedsResourceType) {
                    ResourceType resourceType = createResourceType(state, realm);
                    upgradeApplication(ec, state.appName, resourceType.getUUID());
                }

                if (state.policiesNeedsResourceType) {
                    final Application application = ec.getApplication(state.appName);
                    final Set<String> uuids = application.getResourceTypeUuids();
                    if (!uuids.isEmpty()) {
                        // there should only be one resource type associated with the application at this stage
                        upgradePrivileges(pm, state.appName, uuids.iterator().next());
                    }
                }
            }
        }
    }

    /**
     * Find all the default applications that are specified in entitlements.xml.
     * @throws UpgradeException If entitlements.xml could not be parsed.
     */
    private void populateDefaultApplications() throws UpgradeException {
        final Document entitlementDoc = getEntitlementXML();
        final NodeList subConfigs = entitlementDoc.getElementsByTagName(SMSUtils.SUB_CONFIG);

        for (int idx = 0; idx < subConfigs.getLength(); idx++) {
            final Node subConfig = subConfigs.item(idx);
            final String id = getNodeAttributeValue(subConfig, ID);
            final String name = getNodeAttributeValue(subConfig, NAME);

            // If an application is mentioned in the entitlement.xml, then it is a standard application
            if (APPLICATION.equals(id)) {
                defaultApplicationNames.add(name);
            }
        }
    }

    /**
     * An application is eligible for upgrade if it's not one of the default applications
     * and it does not contain any resource types.
     * @param realm The realm in which the application resides.
     * @param appName The name of the application.
     * @param appData The config data for the application that holds the resource type uuid.
     * @return True if the application is eligible for upgrade.
     */
    private boolean applicationEligibleForUpgrade(String realm, String appName, Map<String, Set<String>> appData) {
        if (ROOT_REALM.equals(realm)) {
            return !defaultApplicationNames.contains(appName) && !isNotEmpty(appData.get(CONFIG_RESOURCE_TYPE_UUIDS));
        } else {
            return !isNotEmpty(appData.get(CONFIG_RESOURCE_TYPE_UUIDS));
        }
    }

    /**
     * A policy is eligible for upgrade if it does not contain any resource types.
     * @param appName The name of the application with which the policy is associated.
     * @param realm The realm in which the policy resides.
     * @return The policies that are eligible for upgrade.
     */
    protected Set<String> policiesEligibleForUpgrade(String appName, String realm) throws UpgradeException {
        try {
            return DataStore.getInstance()
                    .search(getAdminSubject(), realm, MessageFormat.format(POLICY_SEARCH, appName), 0, false, false);
        } catch (EntitlementException ee) {
            throw new UpgradeException("Policy search failed for application " + appName + " in realm " + realm, ee);
        }
    }

    /**
     * Create the resource type for the given application if a suitable resource type does not already exist.
     * @param state The state object that contains the various parameters for creating the resource type.
     * @param realm The realm in which the application and resource type resides.
     * @return The resource type if it could be created or {@code null} if it could not.
     * @throws UpgradeException If the application types could not be read.
     */
    private ResourceType createResourceType(ResourceTypeState state, String realm)
            throws UpgradeException {

        final Set<QueryFilter<SmsAttribute>> actionFilters = transformSet(state.actions,
                new Function<String, QueryFilter<SmsAttribute>, NeverThrowsException>() {

            @Override
            public QueryFilter<SmsAttribute> apply (String value){
                return QueryFilter.equalTo(ResourceTypeSmsAttributes.ACTIONS, value);
            }
        });
        final Set<QueryFilter<SmsAttribute>> patternFilters = transformSet(state.patterns,
                new Function<String, QueryFilter<SmsAttribute>, NeverThrowsException>() {

            @Override
            public QueryFilter<SmsAttribute> apply (String value){
                return QueryFilter.equalTo(ResourceTypeSmsAttributes.PATTERNS, value);
            }
        });

        final Set<ResourceType> resourceTypes;
        try {
            resourceTypes = resourceTypeService.getResourceTypes(
                    QueryFilter.and(QueryFilter.and(actionFilters), QueryFilter.and(patternFilters)),
                    getAdminSubject(), realm);
        } catch (EntitlementException e) {
            throw new UpgradeException("Failed to retrieve resource type for " + state.appName, e);
        }

        if (!resourceTypes.isEmpty()) {
            // Some matching resource types have been found, return the first one.
            return resourceTypes.iterator().next();
        }

        ResourceType resourceType = ResourceType.builder()
                .setName(state.appName + RESOURCES_TYPE_NAME_SUFFIX)
                .addActions(getActions(state.actions))
                .addPatterns(state.patterns)
                .setDescription(RESOURCE_TYPE_DESCRIPTION + state.appName)
                .generateUUID()
                .build();
        saveResourceType(resourceType, realm);
        state.resourceTypeName = resourceType.getName();

        return resourceType;
    }

    /**
     * Persist the given resource type.
     * @param resourceType The resource type to save.
     * @throws UpgradeException If the resource type failed to persist.
     */
    private void saveResourceType(ResourceType resourceType, String realm) throws UpgradeException {
        try {
            UpgradeProgress.reportStart(AUDIT_CREATE_RESOURCE_TYPE_START, resourceType.getName());
            resourceTypeService.saveResourceType(getAdminSubject(), realm, resourceType);
            UpgradeProgress.reportEnd(AUDIT_UPGRADE_SUCCESS);
        } catch (EntitlementException ee) {
            UpgradeProgress.reportEnd(AUDIT_UPGRADE_FAIL);
            throw new UpgradeException("Failed to create resource type " + resourceType.getName(), ee);
        }
    }

    /**
     * Add the resource type UUID to the application and persist it.
     * @param ec The EntitlementConfiguration for the realm in which the application resides.
     * @param appName Name of the application.
     * @param resourceTypeUUID The resource type associated with the application.
     * @throws UpgradeException If the application failed to persist.
     */
    private void upgradeApplication(EntitlementConfiguration ec, String appName, String resourceTypeUUID)
            throws UpgradeException {
        try {
            UpgradeProgress.reportStart(AUDIT_MODIFIED_APP_UUID_START, appName);
            final Application application = ec.getApplication(appName);
            application.addAllResourceTypeUuids(Collections.singleton(resourceTypeUUID));
            ec.storeApplication(application);
            UpgradeProgress.reportEnd(AUDIT_UPGRADE_SUCCESS);
        } catch (EntitlementException ee) {
            UpgradeProgress.reportEnd(AUDIT_UPGRADE_FAIL);
            throw new UpgradeException("Failed to add resource type uuid to application " + appName, ee);
        }
    }

    /**
     * Add the resource type UUID to the privileges in this application and persist them.
     * @param pm The PrivilegeManager for the given realm.
     * @param appName The name of the application with which the policies are associated.
     * @throws UpgradeException if we fail to gather the policies
     */
    private void upgradePrivileges(PrivilegeManager pm, String appName, String resourceTypeUUID)
            throws UpgradeException {

        final SearchFilter searchFilter = new SearchFilter(APPLICATION, appName);
        try {
            final List<Privilege> privileges = pm.search(Collections.singleton(searchFilter));
            for (Privilege privilege : privileges) {
                if (StringUtils.isEmpty(privilege.getResourceTypeUuid())) {
                    upgradePrivilege(pm, privilege, resourceTypeUUID);
                }
            }
        } catch (EntitlementException ee) {
            throw new UpgradeException("Failed to gather policies for application " + appName, ee);
        }
    }

    /**
     * Add the resource type UUID to the privilege and persist it.
     * @param privilegeManager The PrivilegeManager for the realm in which the privilege resides.
     * @param privilege The privilege to upgrade.
     * @param resourceTypeUUID The resource type associated with the privilege.
     * @throws UpgradeException If the privilege failed to persist.
     */
    private void upgradePrivilege(PrivilegeManager privilegeManager, Privilege privilege, String resourceTypeUUID)
            throws UpgradeException {
        try {
            UpgradeProgress.reportStart(AUDIT_MODIFIED_POLICY_UUID_START, privilege.getName());
            if (privilege != null) {
                privilege.setResourceTypeUuid(resourceTypeUUID);
                privilegeManager.modify(privilege);
            }
            UpgradeProgress.reportEnd(AUDIT_UPGRADE_SUCCESS);
        } catch (EntitlementException ee) {
            UpgradeProgress.reportEnd(AUDIT_UPGRADE_FAIL);
            throw new UpgradeException("Failed to add resource type uuid to privilege " + privilege.getName(), ee);
        }
    }

    private ServiceConfig getApplicationsConfig(String realm) throws UpgradeException {
        try {
            return configManager.getOrganizationConfig(realm, null).getSubConfig(REGISTERED_APPLICATIONS);
        } catch (SSOException e) {
            throw new UpgradeException("Failed to retrieve registered applications in realm " + realm, e);
        } catch (SMSException e) {
            throw new UpgradeException("Failed to retrieve registered applications in realm " + realm, e);
        }
    }

    private Set<String> getApplicationNames(ServiceConfig appConfig) throws UpgradeException {
        try {
            return appConfig.getSubConfigNames();
        } catch (SMSException e) {
            throw new UpgradeException("Failed to retrieve application names.", e);
        }
    }

    private Map<String, Set<String>> getApplicationData(ServiceConfig appConfig, String appName)
            throws UpgradeException {
        try {
            return appConfig.getSubConfig(appName).getAttributes();
        } catch (SMSException e) {
            throw new UpgradeException("Failed to retrieve application data for " + appName, e);
        } catch (SSOException e) {
            throw new UpgradeException("Failed to retrieve application data for " + appName, e);
        }
    }

    /**
     * The parent method is final and can't be mocked out for testing, which is why this one was introduced.
     * @return The set of realm names available in OpenAM.
     * @throws UpgradeException In case retrieving the realm names was not successful.
     */
    protected Set<String> getRealmNamesFromParent() throws UpgradeException {
        return getRealmNames();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getShortReport(final String delimiter) {
        final StringBuilder builder = new StringBuilder();

        if (upgradeableApplicationCount > 0) {
            builder.append(BUNDLE.getString(AUDIT_NEW_RESOURCE_TYPE));
            builder.append(" (").append(upgradeableApplicationCount).append(")").append(delimiter);

            builder.append(BUNDLE.getString(AUDIT_MODIFIED_APPLICATION));
            builder.append(" (").append(upgradeableApplicationCount).append(")").append(delimiter);
        }

        if (upgradeablePrivilegeCount > 0) {
            builder.append(BUNDLE.getString(AUDIT_MODIFIED_POLICIES));
            builder.append(" (").append(upgradeablePrivilegeCount).append(")").append(delimiter);
        }

        return builder.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDetailedReport(final String delimiter) {
        final StringBuilder builder = new StringBuilder();
        final StringBuilder resourceTypeBuilder = new StringBuilder();
        final StringBuilder appsBuilder = new StringBuilder();
        final StringBuilder policyBuilder = new StringBuilder();
        final Map<String, String> reportEntries = new HashMap<String, String>();
        final String realmName = BUNDLE.getString(AUDIT_REALM);

        if (upgradeableApplicationCount > 0 || upgradeablePrivilegeCount > 0) {
            for (Map.Entry<String, Set<ResourceTypeState>> entry : resourceTypeStatePerRealm.entrySet()) {
                resourceTypeBuilder.append(INDENT).append(realmName).append(": ").append(entry.getKey())
                        .append(delimiter);
                appsBuilder.append(INDENT).append(realmName).append(": ").append(entry.getKey()).append(delimiter);
                policyBuilder.append(INDENT).append(realmName).append(": ").append(entry.getKey()).append(delimiter);
                for (ResourceTypeState state : entry.getValue()) {
                    if (state.applicationNeedsResourceType) {
                        resourceTypeBuilder.append(INDENT).append(INDENT).append(state.resourceTypeName)
                                .append(delimiter);
                        appsBuilder.append(INDENT).append(INDENT).append(state.appName).append(delimiter);
                    }

                    if (state.policiesNeedsResourceType) {
                        for (String privilegeName : state.policyNames) {
                            policyBuilder.append(INDENT).append(INDENT).append(privilegeName).append(delimiter);
                        }

                    }
                }
            }
            builder.append(BUNDLE.getString(AUDIT_NEW_RESOURCE_TYPE)).append(delimiter);
            builder.append(resourceTypeBuilder).append(delimiter);
            builder.append(BUNDLE.getString(AUDIT_MODIFIED_APPLICATION)).append(delimiter);
            builder.append(appsBuilder).append(delimiter);
            builder.append(BUNDLE.getString(AUDIT_MODIFIED_POLICIES)).append(delimiter);
            builder.append(policyBuilder).append(delimiter);
        }

        reportEntries.put("%ENTITLEMENT_DATA%", builder.toString());
        reportEntries.put(LF, delimiter);

        return UpgradeServices.tagSwapReport(reportEntries, AUDIT_REPORT);
    }
}
