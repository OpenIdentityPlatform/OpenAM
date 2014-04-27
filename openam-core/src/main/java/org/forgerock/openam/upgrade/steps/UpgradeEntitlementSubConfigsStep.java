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

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.ApplicationType;
import com.sun.identity.entitlement.EntitlementConfiguration;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.sm.SMSUtils;
import org.forgerock.openam.entitlement.utils.EntitlementUtils;
import org.forgerock.openam.sm.DataLayerConnectionFactory;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import org.forgerock.openam.upgrade.UpgradeServices;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import org.forgerock.openam.upgrade.UpgradeUtils;
import org.forgerock.openam.utils.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.inject.Inject;
import java.io.InputStream;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.sun.identity.shared.xml.XMLUtils.getNodeAttributeValue;
import static com.sun.identity.shared.xml.XMLUtils.parseAttributeValuePairTags;
import static org.forgerock.openam.upgrade.UpgradeServices.LF;

/**
 * Upgrade step is responsible for ensuring any new application types and/or applications defined in the
 * entitlement.xml are reflected within the entitlements framework. It is also capable to update existing application
 * types with newly added actions.
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.UpgradeServiceSchemaStep")
public class UpgradeEntitlementSubConfigsStep extends AbstractUpgradeStep {

    private static final String ENTITLEMENTS_XML = "entitlement.xml";

    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String APPLICATION = "application";
    private static final String APPLICATION_TYPE = "applicationType";
    private static final String REALM = "/";

    private static final String AUDIT_REPORT = "upgrade.entitlementapps";
    private static final String AUDIT_NEW_TYPE = "upgrade.entitlement.new.type";
    private static final String AUDIT_NEW_APPLICATION = "upgrade.entitlement.new.application";
    private static final String AUDIT_MODIFIED_TYPE = "upgrade.entitlement.modified.type";
    private static final String AUDIT_NEW_TYPE_START = "upgrade.entitlement.new.type.start";
    private static final String AUDIT_NEW_APPLICATION_START = "upgrade.entitlement.new.application.start";
    private static final String AUDIT_MODIFIED_TYPE_START = "upgrade.entitlement.modified.type.start";
    private static final String AUDIT_UPGRADE_SUCCESS = "upgrade.success";
    private static final String AUDIT_UPGRADE_FAIL = "upgrade.failed";

    private final EntitlementConfiguration entitlementService;
    private final List<Node> missingTypes;
    private final List<Node> missingApps;
    private final Map<String, Map<String, Boolean>> missingActions;

    @Inject
    public UpgradeEntitlementSubConfigsStep(final EntitlementConfiguration entitlementService,
                                            final PrivilegedAction<SSOToken> adminTokenAction,
                                            final DataLayerConnectionFactory connectionFactory) {
        super(adminTokenAction, connectionFactory);
        this.entitlementService = entitlementService;
        missingTypes = new ArrayList<Node>();
        missingApps = new ArrayList<Node>();
        missingActions = new HashMap<String, Map<String, Boolean>>();
    }

    @Override
    public void initialize() throws UpgradeException {
        DEBUG.message("Initialising the upgrade entitlement sub-config step");

        final Set<ApplicationType> existingApplicationTypes = entitlementService.getApplicationTypes();
        final Set<String> presentTypes = extract(existingApplicationTypes, new TypeNameExtractor());
        final Set<String> presentApps = extract(entitlementService.getApplications(), new AppNameExtractor());

        final Document entitlementDoc = getEntitlementXML();
        final NodeList subConfigs = entitlementDoc.getElementsByTagName(SMSUtils.SUB_CONFIG);

        for (int idx = 0; idx < subConfigs.getLength(); idx++) {

            final Node subConfig = subConfigs.item(idx);
            final String id = getNodeAttributeValue(subConfig, ID);

            if (APPLICATION_TYPE.equals(id)) {
                captureMissingEntry(subConfig, presentTypes, missingTypes);
                captureMissingActions(subConfig);
            } else if (APPLICATION.equals(id)) {
                captureMissingEntry(subConfig, presentApps, missingApps);
            }
        }
    }

    /**
     * Adds the sub-config to the missing node list if it is not present in the current list.
     *
     * @param subConfig
     *         the sub-config that represents an entry
     * @param presentValues
     *         the set of present values
     * @param missingNodes
     *         the list of nodes missing from the present value set
     */
    private void captureMissingEntry(
            final Node subConfig, final Set<String> presentValues, final List<Node> missingNodes) {

        final String name = getNodeAttributeValue(subConfig, NAME);

        if (!presentValues.contains(name)) {
            DEBUG.message("New entitlement sub-configuration found: " + name);
            missingNodes.add(subConfig);
        }
    }

    /**
     * Compares the provided subconfig element's action list against what is currently present in the existing
     * application type definition and captures the missing entries.
     *
     * @param subConfig The new application type's XML representation.
     */
    private void captureMissingActions(final Node subConfig) {
        final String name = getNodeAttributeValue(subConfig, NAME);
        ApplicationType applicationType = getType(name);
        if (applicationType != null) {
            Map<String, Boolean> existingActions = applicationType.getActions();
            Map<String, Boolean> newActions = EntitlementUtils.getActions(parseAttributeValuePairTags(subConfig));
            if (!existingActions.equals(newActions)) {
                newActions.keySet().removeAll(existingActions.keySet());
                missingActions.put(name, newActions);
            }
        }
    }

    @Override
    public boolean isApplicable() {
        return !missingTypes.isEmpty() || !missingApps.isEmpty() || !missingActions.isEmpty();
    }

    @Override
    public void perform() throws UpgradeException {
        if (!missingTypes.isEmpty()) {
            addMissingTypes();
        }
        if (!missingApps.isEmpty()) {
            addMissingApplications();
        }
        if (!missingActions.isEmpty()) {
            addMissingActions();
        }
    }

    /**
     * Add missing application types.
     *
     * @throws UpgradeException
     *         should the process of creating new application types fail
     */
    private void addMissingTypes() throws UpgradeException {
        for (final Node typeNode : missingTypes) {
            final Map<String, Set<String>> keyValueMap = parseAttributeValuePairTags(typeNode);
            final String name = getNodeAttributeValue(typeNode, NAME);

            UpgradeProgress.reportStart(AUDIT_NEW_TYPE_START, name);
            keyValueMap.put(NAME, Collections.singleton(name));


            try {
                DEBUG.message("Saving new entitlement application type: " + name);
                entitlementService.storeApplicationType(EntitlementUtils.createApplicationType(name, keyValueMap));
                UpgradeProgress.reportEnd(AUDIT_UPGRADE_SUCCESS);
            } catch (EntitlementException eE) {
                UpgradeProgress.reportEnd(AUDIT_UPGRADE_FAIL);
                throw new UpgradeException(eE);
            } catch (InstantiationException ie) {
                UpgradeProgress.reportEnd(AUDIT_UPGRADE_FAIL);
                throw new UpgradeException(ie);
            } catch (IllegalAccessException iae) {
                UpgradeProgress.reportEnd(AUDIT_UPGRADE_FAIL);
                throw new UpgradeException(iae);
            }
        }
    }

    /**
     * Adds missing applications.
     *
     * @throws UpgradeException
     *         should the processing of creating new applications fail
     */
    private void addMissingApplications() throws UpgradeException {
        for (final Node applicationNode : missingApps) {
            final Map<String, Set<String>> keyValueMap = parseAttributeValuePairTags(applicationNode);
            final String name = getNodeAttributeValue(applicationNode, NAME);

            UpgradeProgress.reportStart(AUDIT_NEW_APPLICATION_START, name);
            keyValueMap.put(NAME, Collections.singleton(name));

            final String typeName = retrieveSingleValue(APPLICATION_TYPE, keyValueMap);
            final ApplicationType applicationType = getType(typeName);

            if (applicationType == null) {
                UpgradeProgress.reportEnd(AUDIT_UPGRADE_FAIL);
                throw new UpgradeException("Unknown requested application type " + typeName);
            }

            try {
                DEBUG.message("Saving new entitlement application: " + name);
                entitlementService.storeApplication(EntitlementUtils.createApplication(
                        applicationType, REALM, name, keyValueMap));
                UpgradeProgress.reportEnd(AUDIT_UPGRADE_SUCCESS);
            } catch (EntitlementException eE) {
                UpgradeProgress.reportEnd(AUDIT_UPGRADE_FAIL);
                throw new UpgradeException(eE);
            } catch (InstantiationException ie) {
                UpgradeProgress.reportEnd(AUDIT_UPGRADE_FAIL);
                throw new UpgradeException(ie);
            } catch (IllegalAccessException iae) {
                UpgradeProgress.reportEnd(AUDIT_UPGRADE_FAIL);
                throw new UpgradeException(iae);
            }
        }
    }

    /**
     * Adds the missing actions to their corresponding application type's.
     *
     * @throws UpgradeException If there was an error while updating the application type.
     */
    private void addMissingActions() throws UpgradeException {
        for (final Map.Entry<String, Map<String, Boolean>> entry : missingActions.entrySet()) {
            final String name = entry.getKey();
            final Map<String, Boolean> actions = entry.getValue();

            try {
                UpgradeProgress.reportStart(AUDIT_MODIFIED_TYPE_START, name);
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("Modifying application type " + name + " ; adding actions: " + actions);
                }
                final ApplicationType type = getType(name);
                type.getActions().putAll(actions);
                entitlementService.storeApplicationType(type);
                UpgradeProgress.reportEnd(AUDIT_UPGRADE_SUCCESS);
            } catch (EntitlementException ee) {
                UpgradeProgress.reportEnd(AUDIT_UPGRADE_FAIL);
                throw new UpgradeException(ee);
            }
        }
    }

    /**
     * Retrieves the application type for the passed application type name.
     *
     * @param name
     *         the application type name
     *
     * @return an instance of ApplicationType associated with the name else null if the name is not present
     */
    private ApplicationType getType(String name) {
        for (final ApplicationType type : entitlementService.getApplicationTypes()) {
            if (type.getName().equals(name)) {
                return type;
            }
        }

        return null;
    }

    @Override
    public String getShortReport(final String delimiter) {
        final StringBuilder builder = new StringBuilder();

        if (!missingTypes.isEmpty()) {
            builder.append(BUNDLE.getString(AUDIT_NEW_TYPE));
            builder.append(delimiter);
        }

        if (!missingApps.isEmpty()) {
            builder.append(BUNDLE.getString(AUDIT_NEW_APPLICATION));
            builder.append(delimiter);
        }

        if (!missingActions.isEmpty()) {
            builder.append(BUNDLE.getString(AUDIT_MODIFIED_TYPE));
            builder.append(delimiter);
        }

        return builder.toString();
    }

    @Override
    public String getDetailedReport(final String delimiter) {
        final StringBuilder builder = new StringBuilder();
        final Map<String, String> reportEntries = new HashMap<String, String>();

        if (!missingTypes.isEmpty()) {
            builder.append(BUNDLE.getString(AUDIT_NEW_TYPE));
            builder.append(':');
            builder.append(delimiter);
        }

        for (final Node typeNode : missingTypes) {
            builder.append(getNodeAttributeValue(typeNode, NAME));
            builder.append(delimiter);
        }

        if (!missingApps.isEmpty()) {
            builder.append(BUNDLE.getString(AUDIT_NEW_APPLICATION));
            builder.append(':');
            builder.append(delimiter);
        }

        for (final Node applicationNode : missingApps) {
            builder.append(getNodeAttributeValue(applicationNode, NAME));
            builder.append(delimiter);
        }

        if (!missingActions.isEmpty()) {
            builder.append(BUNDLE.getString(AUDIT_MODIFIED_TYPE)).append(": ").append(delimiter);
            for (final Map.Entry<String, Map<String, Boolean>> entry : missingActions.entrySet()) {
                builder.append(INDENT).append(entry.getKey()).append(delimiter);
                for (final String action : entry.getValue().keySet()) {
                    builder.append(INDENT).append(INDENT).append(action).append(delimiter);
                }
            }
        }

        reportEntries.put("%ENTITLEMENT_DATA%", builder.toString());
        reportEntries.put(LF, delimiter);

        return UpgradeServices.tagSwapReport(reportEntries, AUDIT_REPORT);
    }

    /**
     * Retrieves the XML document for entitlements.
     *
     * @return a document instance representing entitlements
     *
     * @throws UpgradeException
     *         should an error occur attempting to read the entitlement xml
     */
    protected Document getEntitlementXML() throws UpgradeException {
        InputStream serviceStream = null;
        final Document doc;

        try {
            DEBUG.message("Reading entitlements configuration file: " + ENTITLEMENTS_XML);
            serviceStream = getClass().getClassLoader().getResourceAsStream(ENTITLEMENTS_XML);
            doc = UpgradeUtils.parseServiceFile(serviceStream, getAdminToken());
        } finally {
            IOUtils.closeIfNotNull(serviceStream);
        }

        return doc;
    }

    /**
     * Extract a set of a defined type from the passed collection using the given extractor.
     *
     * @param collection
     *         the collection from which the set is to be extracted
     * @param extractor
     *         the extractor instance that knows how to extract data from the passed object
     * @param <S>
     *         the collection type
     * @param <T>
     *         the returning set type
     *
     * @return the set of extracted values
     */
    private <S, T> Set<T> extract(final Collection<S> collection, final Extractor<S, T> extractor) {
        final Set<T> values = new HashSet<T>(collection.size());

        for (final S instance : collection) {
            values.add(extractor.getValue(instance));
        }

        return values;
    }

    // Extracts string name from ApplicationType.
    private static class TypeNameExtractor implements Extractor<ApplicationType, String> {

        @Override
        public String getValue(final ApplicationType type) {
            return type.getName();
        }

    }

    // Extracts string name from Application
    private static class AppNameExtractor implements Extractor<Application, String> {

        @Override
        public String getValue(final Application application) {
            return application.getName();
        }

    }

    /**
     * An extractor knows how to extract a given value from the passed object.
     *
     * @param <S>
     *         the original object type
     * @param <T>
     *         the extracted value type
     */
    private static interface Extractor<S, T> {

        /**
         * Extract value from passed object.
         *
         * @param object
         *         passed object
         *
         * @return the extracted value
         */
        public T getValue(final S object);

    }

    /**
     * Helper method to retrieve the set of strings from the passed key/value map.
     *
     * @param key
     *         the key
     * @param keyValueMap
     *         the key/value map
     *
     * @return the set of strings associated with the key or an empty map if the key is not present
     */
    private static Set<String> retrieveValues(final String key,
                                              final Map<String, Set<String>> keyValueMap) {

        return keyValueMap.containsKey(key) ? keyValueMap.get(key) : Collections.<String>emptySet();
    }

    /**
     * Helper method to retrieve the string value from the passed key/value map.
     *
     * @param key
     *         the key
     * @param keyValueMap
     *         the key/value map
     *
     * @return the string associated with the key or null if the key is not present
     */
    private static String retrieveSingleValue(final String key,
                                              final Map<String, Set<String>> keyValueMap) {

        final Set<String> values = retrieveValues(key, keyValueMap);
        return values.isEmpty() ? null : values.iterator().next();
    }
}
