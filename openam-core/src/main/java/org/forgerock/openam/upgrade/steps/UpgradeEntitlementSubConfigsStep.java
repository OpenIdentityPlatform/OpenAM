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
import com.sun.identity.entitlement.EntitlementCombiner;
import com.sun.identity.entitlement.EntitlementConfiguration;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.interfaces.ISaveIndex;
import com.sun.identity.entitlement.interfaces.ISearchIndex;
import com.sun.identity.entitlement.interfaces.ResourceName;
import com.sun.identity.sm.SMSUtils;
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
import java.util.Date;
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
 * entitlement.xml are reflected within the entitlements framework. It currently does not take into account
 * modifications to existing entries, only new entries.
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.UpgradeServiceSchemaStep")
public class UpgradeEntitlementSubConfigsStep extends AbstractUpgradeStep {

    private static final String ENTITLEMENTS_XML = "entitlement.xml";

    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String ACTIONS = "actions";
    private static final String SEARCH_INDEX = "searchIndexImpl";
    private static final String SAVE_INDEX = "saveIndexImpl";
    private static final String RESOURCE_COMPARATOR = "resourceComparator";
    private static final String ENTITLEMENT_COMBINER = "entitlementCombiner";
    private static final String RESOURCES = "resources";
    private static final String SUBJECTS = "subjects";
    private static final String CONDITIONS = "conditions";
    private static final String APPLICATION = "application";
    private static final String APPLICATION_TYPE = "applicationType";
    private static final String REALM = "/";

    private static final String AUDIT_REPORT = "upgrade.entitlementapps";
    private static final String AUDIT_NEW_TYPE = "upgrade.entitlement.new.type";
    private static final String AUDIT_NEW_APPLICATION = "upgrade.entitlement.new.application";
    private static final String AUDIT_NEW_TYPE_START = "upgrade.entitlement.new.type.start";
    private static final String AUDIT_NEW_APPLICATION_START = "upgrade.entitlement.new.application.start";
    private static final String AUDIT_UPGRADE_SUCCESS = "upgrade.success";
    private static final String AUDIT_UPGRADE_FAIL = "upgrade.failed";

    private final EntitlementConfiguration entitlementService;
    private final List<Node> missingTypes;
    private final List<Node> missingApps;

    @Inject
    public UpgradeEntitlementSubConfigsStep(final EntitlementConfiguration entitlementService,
                                            final PrivilegedAction<SSOToken> adminTokenAction,
                                            final DataLayerConnectionFactory connectionFactory) {
        super(adminTokenAction, connectionFactory);
        this.entitlementService = entitlementService;
        missingTypes = new ArrayList<Node>();
        missingApps = new ArrayList<Node>();
    }

    @Override
    public void initialize() throws UpgradeException {
        final Set<String> presentTypes = extract(entitlementService.getApplicationTypes(), new TypeNameExtractor());
        final Set<String> presentApps = extract(entitlementService.getApplications(), new AppNameExtractor());

        final Document entitlementDoc = getEntitlementXML();
        final NodeList subConfigs = entitlementDoc.getElementsByTagName(SMSUtils.SUB_CONFIG);

        for (int idx = 0; idx < subConfigs.getLength(); idx++) {

            final Node subConfig = subConfigs.item(idx);
            final String id = getNodeAttributeValue(subConfig, ID);

            if (APPLICATION_TYPE.equals(id)) {
                captureMissingEntry(subConfig, presentTypes, missingTypes);
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
            missingNodes.add(subConfig);
        }
    }

    @Override
    public boolean isApplicable() {
        return !missingTypes.isEmpty() || !missingApps.isEmpty();
    }

    @Override
    public void perform() throws UpgradeException {
        if (!missingTypes.isEmpty()) {
            addMissingTypes();
        }
        if (!missingApps.isEmpty()) {
            addMissingApplications();
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

            final ApplicationType type = new TypeBuilder().build(keyValueMap);

            try {
                entitlementService.storeApplicationType(type);
                UpgradeProgress.reportEnd(AUDIT_UPGRADE_SUCCESS);
            } catch (EntitlementException eE) {
                UpgradeProgress.reportEnd(AUDIT_UPGRADE_FAIL);
                throw new UpgradeException(eE);
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
            final ApplicationType type = getType(typeName);

            if (type == null) {
                UpgradeProgress.reportEnd(AUDIT_UPGRADE_FAIL);
                throw new UpgradeException("Unknown requested application type " + typeName);
            }

            final Application application = new ApplicationBuilder(type).build(keyValueMap);

            try {
                entitlementService.storeApplication(application);
                UpgradeProgress.reportEnd(AUDIT_UPGRADE_SUCCESS);
            } catch (EntitlementException eE) {
                UpgradeProgress.reportEnd(AUDIT_UPGRADE_FAIL);
                throw new UpgradeException(eE);
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

    // Builds a new instance of ApplicationType from a key/value map.
    private static class TypeBuilder implements Builder<Map<String, Set<String>>, ApplicationType> {

        @Override
        public ApplicationType build(final Map<String, Set<String>> keyValueMap) throws UpgradeException {

            final String name = retrieveSingleValue(NAME, keyValueMap);
            final Set<String> actions = retrieveValues(ACTIONS, keyValueMap);
            final String searchIndexClass = retrieveSingleValue(SEARCH_INDEX, keyValueMap);
            final String saveIndexClass = retrieveSingleValue(SAVE_INDEX, keyValueMap);
            final String resourceComparatorClass = retrieveSingleValue(RESOURCE_COMPARATOR, keyValueMap);

            try {
                return new ApplicationType(name,
                        getActions(actions),
                        getClazz(searchIndexClass, ISearchIndex.class),
                        getClazz(saveIndexClass, ISaveIndex.class),
                        getClazz(resourceComparatorClass, ResourceName.class));

            } catch (InstantiationException iE) {
                throw new UpgradeException(iE);
            } catch (IllegalAccessException iaE) {
                throw new UpgradeException(iaE);
            }
        }

        /**
         * Pulls out the action values from the passed string set.
         *
         * @param actionStrings
         *         action strings in the format actionName=booleanValue
         *
         * @return map of action/values
         */
        private Map<String, Boolean> getActions(final Set<String> actionStrings) {
            final Map<String, Boolean> actions = new HashMap<String, Boolean>(actionStrings.size());

            for (final String actionString : actionStrings) {
                final String[] actionParts = actionString.split("=");
                actions.put(actionParts[0], Boolean.valueOf(actionParts[1]));
            }

            return actions;
        }

    }

    // Builds a new instance of Application from a key/value map.
    private static class ApplicationBuilder implements Builder<Map<String, Set<String>>, Application> {

        private final ApplicationType type;

        public ApplicationBuilder(final ApplicationType type) {
            this.type = type;
        }

        @Override
        public Application build(final Map<String, Set<String>> keyValueMap) throws UpgradeException {
            final String name = retrieveSingleValue(NAME, keyValueMap);
            final String entitlementCombinerClass = retrieveSingleValue(ENTITLEMENT_COMBINER, keyValueMap);
            final Set<String> resources = retrieveValues(RESOURCES, keyValueMap);
            final Set<String> subjects = retrieveValues(SUBJECTS, keyValueMap);
            final Set<String> conditions = retrieveValues(CONDITIONS, keyValueMap);
            final long creationDate = new Date().getTime();

            final Application application = new Application(REALM, name, type);
            application.setEntitlementCombiner(getClazz(entitlementCombinerClass, EntitlementCombiner.class));
            application.setResources(resources);
            application.setSubjects(subjects);
            application.setConditions(conditions);
            application.setCreationDate(creationDate);
            application.setLastModifiedDate(creationDate);
            return application;
        }
    }

    /**
     * A builder knows how to build a new instance from the passed payload.
     *
     * @param <V>
     *         the payload type
     * @param <T>
     *         the new instance type
     */
    private static interface Builder<V, T> {

        /**
         * Build a new instance from the payload.
         *
         * @param payload
         *         the passed payload used to assist construction
         *
         * @return a new instance
         *
         * @throws UpgradeException
         *         should the process of building a new instance fail
         */
        public T build(final V payload) throws UpgradeException;

    }

    /**
     * Gets the class representation of the passed class name.
     *
     * @param className
     *         the class name
     * @param type
     *         the class representation of the super type
     * @param <T>
     *         the class type of the super type
     *
     * @return a class instance representation
     *
     * @throws UpgradeException
     *         should retrieving the class representation fail
     */
    private static <T> Class<? extends T> getClazz(final String className,
                                                   final Class<T> type) throws UpgradeException {
        try {
            return Class.forName(className).asSubclass(type);
        } catch (ClassNotFoundException cnfE) {
            throw new UpgradeException(cnfE);
        }
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
