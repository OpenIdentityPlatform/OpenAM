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
 * Copyright 2014-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.upgrade.steps;

import static com.sun.identity.shared.xml.XMLUtils.*;
import static org.forgerock.openam.entitlement.utils.EntitlementUtils.*;
import static org.forgerock.openam.upgrade.UpgradeServices.LF;
import static org.forgerock.openam.utils.CollectionUtils.*;

import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.forgerock.openam.entitlement.ResourceType;
import org.forgerock.openam.entitlement.configuration.ResourceTypeConfiguration;
import org.forgerock.openam.entitlement.service.ApplicationService;
import org.forgerock.openam.entitlement.service.ApplicationServiceFactory;
import org.forgerock.openam.entitlement.utils.EntitlementUtils;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import org.forgerock.openam.upgrade.UpgradeServices;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import org.forgerock.openam.upgrade.steps.policy.AbstractEntitlementUpgradeStep;
import org.forgerock.util.Reject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.ApplicationType;
import com.sun.identity.entitlement.DenyOverride;
import com.sun.identity.entitlement.EntitlementCombiner;
import com.sun.identity.entitlement.EntitlementConfiguration;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.sm.SMSUtils;

/**
 * Upgrade step is responsible for ensuring any new application types and/or applications defined in the
 * entitlement.xml are reflected within the entitlements framework. It is also capable to update existing application
 * types with newly added actions.
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.RemoveRedundantDefaultApplication")
public class UpgradeEntitlementSubConfigsStep extends AbstractEntitlementUpgradeStep {

    private static final String AUDIT_REPORT = "upgrade.entitlementapps";
    private static final String AUDIT_NEW_TYPE = "upgrade.entitlement.new.type";
    private static final String AUDIT_NEW_APPLICATION = "upgrade.entitlement.new.application";
    private static final String AUDIT_MODIFIED_TYPE = "upgrade.entitlement.modified.type";
    private static final String AUDIT_NEW_TYPE_START = "upgrade.entitlement.new.type.start";
    private static final String AUDIT_NEW_RESOURCE_TYPE_START = "upgrade.entitlement.new.resource.type.start";
    private static final String AUDIT_NEW_APPLICATION_START = "upgrade.entitlement.new.application.start";
    private static final String AUDIT_MODIFIED_TYPE_START = "upgrade.entitlement.modified.type.start";
    private static final String AUDIT_MODIFIED_SUB_START = "upgrade.entitlement.modified.subjects.start";
    private static final String AUDIT_MODIFIED_CON_START = "upgrade.entitlement.modified.conditions.start";
    private static final String AUDIT_MODIFIED_DES_START = "upgrade.entitlement.modified.description.start";
    private static final String AUDIT_MODIFIED_COM_START = "upgrade.entitlement.modified.combiners.start";
    private static final String AUDIT_MODIFIED_UUID_START = "upgrade.entitlement.modified.resourcetypeuuid.start";

    private static final String DEFAULT_COMBINER_SHORTNAME = DenyOverride.class.getSimpleName();

    private final EntitlementConfiguration entitlementService;
    private final ResourceTypeConfiguration resourceTypeConfiguration;
    private final ApplicationServiceFactory applicationServiceFactory;
    private final List<Node> missingApplicationTypes;
    private final List<Node> missingApps;
    private final List<Node> missingResourceTypes;
    private final Map<String, Set<String>> changedConditions;
    private final Map<String, String> changedDescriptions;
    private final Map<String, Set<String>> changedSubjects;
    private final Map<String, Set<String>> changedResourceTypeUUIDs;
    private final Map<String, Map<String, Boolean>> missingActions;
    private final Map<String, String> changedCombiners;

    @Inject
    public UpgradeEntitlementSubConfigsStep(
            @Named("RootRealmEntitlementConfiguration") final EntitlementConfiguration entitlementService,
            final ResourceTypeConfiguration resourceTypeConfiguration,
            final PrivilegedAction<SSOToken> adminTokenAction,
            @DataLayer(ConnectionType.DATA_LAYER) final ConnectionFactory connectionFactory,
            final ApplicationServiceFactory applicationServiceFactory) {
        super(adminTokenAction, connectionFactory);
        this.entitlementService = entitlementService;
        this.resourceTypeConfiguration = resourceTypeConfiguration;
        this.applicationServiceFactory = applicationServiceFactory;
        missingApplicationTypes = new ArrayList<Node>();
        missingApps = new ArrayList<Node>();
        missingResourceTypes = new ArrayList<Node>();
        missingActions = new HashMap<String, Map<String, Boolean>>();
        changedConditions = new HashMap<String, Set<String>>();
        changedDescriptions = new HashMap<String, String>();
        changedSubjects = new HashMap<String, Set<String>>();
        changedCombiners = new HashMap<String, String>();
        changedResourceTypeUUIDs = new HashMap<String, Set<String>>();
    }

    @Override
    public void initialize() throws UpgradeException {
        DEBUG.message("Initialising the upgrade entitlement sub-config step");

        final Set<ApplicationType> existingApplicationTypes = entitlementService.getApplicationTypes();
        final Set<String> existingResourceTypeUUIDs = getResourceTypeUUIDs(ROOT_REALM);
        final Set<String> presentTypes = extract(existingApplicationTypes, new TypeNameExtractor());
        final Set<String> presentApps = extract(entitlementService.getApplications(), new AppNameExtractor());

        final Document entitlementDoc = getEntitlementXML();
        final NodeList subConfigs = entitlementDoc.getElementsByTagName(SMSUtils.SUB_CONFIG);

        for (int idx = 0; idx < subConfigs.getLength(); idx++) {

            final Node subConfig = subConfigs.item(idx);
            final String id = getNodeAttributeValue(subConfig, ID);
            final String name = getNodeAttributeValue(subConfig, NAME);

            if (APPLICATION_TYPE.equals(id)) {
                captureMissingEntry(name, subConfig, presentTypes, missingApplicationTypes);
                captureMissingActions(name, subConfig);
            } else if (APPLICATION.equals(id)) {
                captureMissingEntry(name, subConfig, presentApps, missingApps);

                //app will be null if application needs to be created (see missing entries)
                final Application app = getApplication(name);
                final Map<String, Set<String>> subConfigAttrs = parseAttributeValuePairTags(subConfig);

                captureDifferentSet(app == null ? null : app.getSubjects(),
                        getSubjects(subConfigAttrs), changedSubjects, name);

                captureDifferentSet(app == null ? null : app.getConditions(),
                        getConditions(subConfigAttrs), changedConditions, name);

                captureDifferentSet(app == null ? null : app.getResourceTypeUuids(),
                        EntitlementUtils.getResourceTypeUUIDs(subConfigAttrs), changedResourceTypeUUIDs, name);

                Set<String> configDescriptionSet = getDescription(subConfigAttrs);
                String configDescription = null;
                if (configDescriptionSet != null && !configDescriptionSet.isEmpty()) {
                    configDescription = configDescriptionSet.iterator().next();
                }
                captureDifferentString(app == null ? null : app.getDescription(),
                        configDescription, changedDescriptions, name);

                final EntitlementCombiner combiner = (app == null ? null : app.getEntitlementCombiner());

                captureDifferentEntitlementCombiner(combiner == null ? null : combiner.getName(),
                        getCombiner(subConfigAttrs),
                        name);
            } else if (RESOURCE_TYPE.equals(id)) {
                // note that the name variable actually holds the UUID of the ResourceType
                // the name is buried in the config.
                //
                captureMissingEntry(name, subConfig, existingResourceTypeUUIDs, missingResourceTypes);
            }
        }
    }

    /**
     * Helper function which passes through to
     * {@link UpgradeEntitlementSubConfigsStep#captureDifferentString(String, String, String, java.util.Map, String)}
     * setting the necessary variables for altering the Application's Entitlement Combiner.
     *
     * If both old and new name arguments are null, the default DenyOverride will be used.
     *
     * @param oldName the old name used to reference the combiner. May be null.
     * @param newName the new name used to reference the combiner. May be null.
     * @param appName the name of the application
     */
    private void captureDifferentEntitlementCombiner(String oldName, String newName, String appName) {
        captureDifferentString(oldName, newName, appName, changedCombiners, DEFAULT_COMBINER_SHORTNAME);
    }

    /**
     * Adds entries to the passed in map, using the supplied appName as the key. The default value will be
     * used if both provided old and new names are null. The value inserted will be the newName value if it
     * differs from the oldName value.
     *
     * No changes will be made to the map if the old and new names are equal.
     *
     * @param oldName the old String's value. May be null.
     * @param newName the new String's value. May be null.
     * @param appName the name of the application which will act as a key in the map. May not be null.
     * @param map the map to update. May not be null.
     * @param defaultValue the value to use if both old and new Strings are null. May be null.
     */
    private void captureDifferentString(String oldName, String newName, String appName,
                                        Map<String, String> map, String defaultValue) {
        Reject.ifNull(appName);
        Reject.ifNull(map);

        if (oldName == null && newName == null) {
            map.put(appName, defaultValue);
        } else if (newName != null && !newName.equals(oldName)) {
            map.put(appName, newName);
        }
    }

    /**
     * Adds entries to the passed in map, using the supplied appName as the key. The value is a new, empty
     * set if both oldSet and newSet are null, or newSet if the contents of oldSet and newSet differ.
     *
     * If the two sets are the same, no entries are added to the map.
     *
     * @param oldString The older string data. May be null.
     * @param newString The newer string data. May be null.
     * @param map The map into which to push the appropriate string. May not be null.
     * @param appName The key to use when pushing data into the set. May not be null.
     */
    private void captureDifferentString(String oldString, String newString,
                                        Map<String, String> map, String appName) {
        Reject.ifNull(appName);
        Reject.ifNull(map);

        if (oldString == null ? newString != null : !oldString.equals(newString)) {
            map.put(appName, newString);
        }
    }

    /**
     * Adds entries to the passed in map, using the supplied appName as the key. The value is a new, empty
     * set if both oldSet and newSet are null, or newSet if the contents of oldSet and newSet differ.
     *
     * If the two sets are the same, no entries are added to the map.
     *
     * @param oldSet The older set of data. May be null.
     * @param newSet The newer set of data. May be null.
     * @param map The map into which to push the appropriate sets. May not be null.
     * @param appName The key to use when pushing data into the set. May not be null.
     */
    private void captureDifferentSet(Set<String> oldSet, Set<String> newSet,
                                     Map<String, Set<String>> map, String appName) {

        Reject.ifNull(appName);
        Reject.ifNull(map);

        if (oldSet == null && newSet == null) {
            map.put(appName, Collections.<String>emptySet());
        } else if (newSet != null && !newSet.equals(oldSet)) {
            map.put(appName, newSet);
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
    private void captureMissingEntry(final String name,
            final Node subConfig, final Set<String> presentValues, final List<Node> missingNodes) {
        if (!presentValues.contains(name)) {
            DEBUG.message("New entitlement sub-configuration found: " + name);
            missingNodes.add(subConfig);
        }
    }

    /**
     * Compares the provided subconfig element's action list against what is currently present in the existing
     * application type definition and captures the missing entries.
     *
     * @param name The name of the subconfig's element we're interested in
     * @param subConfig The new application type's XML representation.
     */
    private void captureMissingActions(final String name, final Node subConfig) {
        ApplicationType applicationType = getType(name);
        if (applicationType != null) {
            Map<String, Boolean> existingActions = applicationType.getActions();
            Map<String, Boolean> newActions = getActions(parseAttributeValuePairTags(subConfig));
            if (!existingActions.equals(newActions)) {
                newActions.keySet().removeAll(existingActions.keySet());
                missingActions.put(name, newActions);
            }
        }
    }

    @Override
    public boolean isApplicable() {
        return anyHaveEntries(missingApplicationTypes,
                missingApps,
                missingActions,
                changedConditions,
                changedDescriptions,
                changedSubjects,
                changedCombiners,
                missingResourceTypes);
    }

    @Override
    public void perform() throws UpgradeException {
        if (isNotEmpty(missingApplicationTypes)) {
            addMissingApplicationTypes();
        }
        if (isNotEmpty(missingApps)) {
            addMissingApplications();
        }
        if (isNotEmpty(missingActions)) {
            addMissingActions();
        }
        if (isNotEmpty(changedConditions)) {
            addChangedConditions();
        }
        if (isNotEmpty(changedDescriptions)) {
            addChangedDescription();
        }
        if (isNotEmpty(changedSubjects)) {
            addChangedSubjects();
        }
        if (isNotEmpty(changedCombiners)) {
            addChangedCombiners();
        }
        if (isNotEmpty(missingResourceTypes)) {
            addMissingResourceTypes();
        }
        if (isNotEmpty(changedResourceTypeUUIDs)) {
            addMissingResourceTypeUUIDs();
        }

        // Clear application cache to pick up any application changes.
        applicationServiceFactory.create(getAdminSubject(), "/").clearCache();
    }

    /**
     * Alter EntitlementCombiner references.
     *
     * @throws UpgradeException
     */
    private void addChangedCombiners() throws UpgradeException {
        for (final Map.Entry<String, String> entry : changedCombiners.entrySet()) {
            final String name = entry.getKey();
            final String combiner = entry.getValue();

            try {
                UpgradeProgress.reportStart(AUDIT_MODIFIED_COM_START, name);
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("Modifying application " + name + " ; setting combiner: " + combiner);
                }
                final Application application = getApplication(name);
                application.setEntitlementCombinerName(EntitlementUtils.getEntitlementCombiner(combiner));
                entitlementService.storeApplication(application);
                UpgradeProgress.reportEnd(AUDIT_UPGRADE_SUCCESS);
            } catch (EntitlementException ee) {
                UpgradeProgress.reportEnd(AUDIT_UPGRADE_FAIL);
                throw new UpgradeException(ee);
            }
        }
    }

    /**
     * Add missing application types.
     *
     * @throws UpgradeException
     *         should the process of creating new application types fail
     */
    private void addMissingApplicationTypes() throws UpgradeException {
        for (final Node typeNode : missingApplicationTypes) {
            final Map<String, Set<String>> keyValueMap = parseAttributeValuePairTags(typeNode);
            final String name = getNodeAttributeValue(typeNode, NAME);

            UpgradeProgress.reportStart(AUDIT_NEW_TYPE_START, name);
            keyValueMap.put(NAME, Collections.singleton(name));

            try {
                DEBUG.message("Saving new entitlement application type: " + name);
                entitlementService.storeApplicationType(createApplicationType(name, keyValueMap));
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
                entitlementService.storeApplication(createApplication(applicationType, name, keyValueMap));
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
     * Clears the subjects currently associated with an application, then replaces them with
     * the new set of conditions defined.
     *
     * @throws UpgradeException If there was an error while updating the application.
     */
    private void addChangedSubjects() throws UpgradeException {
        for (final Map.Entry<String, Set<String>> entry : changedSubjects.entrySet()) {
            final String name = entry.getKey();
            final Set<String> subjects = entry.getValue();

            try {
                UpgradeProgress.reportStart(AUDIT_MODIFIED_SUB_START, name);
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("Modifying application " + name + " ; adding subjects: " + subjects);
                }
                final Application application = getApplication(name);
                application.setSubjects(subjects);
                entitlementService.storeApplication(application);
                UpgradeProgress.reportEnd(AUDIT_UPGRADE_SUCCESS);
            } catch (EntitlementException ee) {
                UpgradeProgress.reportEnd(AUDIT_UPGRADE_FAIL);
                throw new UpgradeException(ee);
            }
        }
    }

    /**
     * Clears the conditions currently associated with an application, then replaces them with
     * the new set of conditions defined.
     *
     * @throws UpgradeException If there was an error while updating the application.
     */
    private void addChangedConditions() throws UpgradeException {
        for (final Map.Entry<String, Set<String>> entry : changedConditions.entrySet()) {
            final String name = entry.getKey();
            final Set<String> conditions = entry.getValue();

            try {
                UpgradeProgress.reportStart(AUDIT_MODIFIED_CON_START, name);
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("Modifying application " + name + " ; adding conditions: " + conditions);
                }
                final Application application = getApplication(name);
                application.setConditions(conditions);
                entitlementService.storeApplication(application);
                UpgradeProgress.reportEnd(AUDIT_UPGRADE_SUCCESS);
            } catch (EntitlementException ee) {
                UpgradeProgress.reportEnd(AUDIT_UPGRADE_FAIL);
                throw new UpgradeException(ee);
            }
        }
    }

    /**
     * Clears the resourceType UUIDs currently associated with an application, then replaces them with
     * the new set of resourceType UUIDs defined.
     *
     * @throws UpgradeException If there was an error while updating the application.
     */
    private void addMissingResourceTypeUUIDs() throws UpgradeException {
        for (final Map.Entry<String, Set<String>> entry : changedResourceTypeUUIDs.entrySet()) {
            final String name = entry.getKey();
            final Set<String> resourceTypeUUIDs = entry.getValue();

            try {
                UpgradeProgress.reportStart(AUDIT_MODIFIED_UUID_START, name);
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("Modifying application "
                            + name
                            + ": adding resourceType UUIDs: "
                            + resourceTypeUUIDs);
                }
                final Application application = getApplication(name);
                application.addAllResourceTypeUuids(resourceTypeUUIDs);
                entitlementService.storeApplication(application);
                UpgradeProgress.reportEnd(AUDIT_UPGRADE_SUCCESS);
            } catch (EntitlementException ee) {
                UpgradeProgress.reportEnd(AUDIT_UPGRADE_FAIL);
                throw new UpgradeException(ee);
            }
        }
    }

    /**
     * Clears the description currently associated with an application, then replaces it with
     * the new description defined.
     *
     * @throws UpgradeException If there was an error while updating the application.
     */
    private void addChangedDescription() throws UpgradeException {
        for (final Map.Entry<String, String> entry : changedDescriptions.entrySet()) {
            final String name = entry.getKey();
            final String description = entry.getValue();

            try {
                UpgradeProgress.reportStart(AUDIT_MODIFIED_DES_START, name);
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("Modifying application " + name + " ; adding description: " + description);
                }
                final Application application = getApplication(name);
                application.setDescription(description);
                entitlementService.storeApplication(application);
                UpgradeProgress.reportEnd(AUDIT_UPGRADE_SUCCESS);
            } catch (EntitlementException ee) {
                UpgradeProgress.reportEnd(AUDIT_UPGRADE_FAIL);
                throw new UpgradeException(ee);
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
     * Adds any missing ResourceTypes.
     *
     * @throws UpgradeException If there was an error while adding a ResourceType
     */
    private void addMissingResourceTypes() throws UpgradeException {
        for (final Node typeNode : missingResourceTypes) {
            final Map<String, Set<String>> keyValueMap = parseAttributeValuePairTags(typeNode);
            final String uuid = getNodeAttributeValue(typeNode, NAME);
            final String name = retrieveSingleValue(NAME, keyValueMap);
            final ResourceType resourceType = resourceTypeFromMap(uuid, keyValueMap);

            UpgradeProgress.reportStart(AUDIT_NEW_RESOURCE_TYPE_START, name);

            try {
                DEBUG.message("Saving standard resource type {} with UUID {}", name, uuid);
                resourceTypeConfiguration.storeResourceType(getAdminSubject(), ROOT_REALM, resourceType);
                UpgradeProgress.reportEnd(AUDIT_UPGRADE_SUCCESS);
            } catch (EntitlementException eE) {
                UpgradeProgress.reportEnd(AUDIT_UPGRADE_FAIL);
                throw new UpgradeException(eE);
            }
        }
    }

    /**
     * Retrieves the application for the passed application name.
     *
     * @param name
     *         the application name
     *
     * @return an instance of Application associated with the name else null if the name is not present
     */
    private Application getApplication(String name) {
        for (final Application app : entitlementService.getApplications()) {
            if (app.getName().equals(name)) {
                return app;
            }
        }

        return null;
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

        if (isNotEmpty(missingApplicationTypes)) {
            builder.append(BUNDLE.getString(AUDIT_NEW_TYPE));
            builder.append(delimiter);
        }

        if (isNotEmpty(missingApps)) {
            builder.append(BUNDLE.getString(AUDIT_NEW_APPLICATION));
            builder.append(delimiter);
        }

        if (isNotEmpty(missingActions)) {
            builder.append(BUNDLE.getString(AUDIT_MODIFIED_TYPE));
            builder.append(delimiter);
        }

        return builder.toString();
    }

    @Override
    public String getDetailedReport(final String delimiter) {
        final StringBuilder builder = new StringBuilder();
        final Map<String, String> reportEntries = new HashMap<String, String>();

        if (isNotEmpty(missingApplicationTypes)) {
            builder.append(BUNDLE.getString(AUDIT_NEW_TYPE));
            builder.append(':');
            builder.append(delimiter);
        }

        for (final Node typeNode : missingApplicationTypes) {
            builder.append(getNodeAttributeValue(typeNode, NAME));
            builder.append(delimiter);
        }

        if (isNotEmpty(missingApps)) {
            builder.append(BUNDLE.getString(AUDIT_NEW_APPLICATION));
            builder.append(':');
            builder.append(delimiter);
        }

        for (final Node applicationNode : missingApps) {
            builder.append(getNodeAttributeValue(applicationNode, NAME));
            builder.append(delimiter);
        }

        if (isNotEmpty(missingActions)) {
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

    /**
     * Build a set of the UUIDs for all the resource types in the specified realm.
     * @param realm The realm.
     * @return Set of strings representing the resource type uuids.
     */
    private Set<String> getResourceTypeUUIDs(String realm) throws UpgradeException {
        try {
            return resourceTypeConfiguration.getResourceTypesData(getAdminSubject(), realm).keySet();
        } catch (EntitlementException ee) {
            throw new UpgradeException(ee);
        }
    }
}
