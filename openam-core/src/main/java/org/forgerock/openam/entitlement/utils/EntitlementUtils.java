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
package org.forgerock.openam.entitlement.utils;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.ApplicationManager;
import com.sun.identity.entitlement.ApplicationType;
import com.sun.identity.entitlement.ApplicationTypeManager;
import com.sun.identity.entitlement.DenyOverride;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.PrivilegeManager;
import static com.sun.identity.entitlement.opensso.EntitlementService.APPLICATION_CLASSNAME;
import static com.sun.identity.entitlement.opensso.EntitlementService.ATTR_NAME_META;
import static com.sun.identity.entitlement.opensso.EntitlementService.ATTR_NAME_SUBJECT_ATTR_NAMES;
import static com.sun.identity.entitlement.opensso.EntitlementService.CONFIG_ACTIONS;
import static com.sun.identity.entitlement.opensso.EntitlementService.CONFIG_APPLICATION_DESC;
import static com.sun.identity.entitlement.opensso.EntitlementService.CONFIG_CONDITIONS;
import static com.sun.identity.entitlement.opensso.EntitlementService.CONFIG_ENTITLEMENT_COMBINER;
import static com.sun.identity.entitlement.opensso.EntitlementService.CONFIG_RESOURCES;
import static com.sun.identity.entitlement.opensso.EntitlementService.CONFIG_RESOURCE_COMP_IMPL;
import static com.sun.identity.entitlement.opensso.EntitlementService.CONFIG_SAVE_INDEX_IMPL;
import static com.sun.identity.entitlement.opensso.EntitlementService.CONFIG_SEARCH_INDEX_IMPL;
import static com.sun.identity.entitlement.opensso.EntitlementService.CONFIG_SUBJECTS;
import com.sun.identity.security.AdminTokenAction;
import java.security.AccessController;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.forgerock.util.Reject;

/**
 * Utility methods for managing entitlements.
 */
public final class EntitlementUtils {

    private EntitlementUtils() {
    }

    /**
     * Constructs an {@link ApplicationType} object based on the provided information.
     *
     * @param name The name of the application type.
     * @param data The configuration settings for the application type.
     * @return An {@link ApplicationType} object corresponding to the provided details.
     * @throws InstantiationException If the class settings cannot be instantiated.
     * @throws IllegalAccessException If the class settings cannot be instantiated.
     */
    public static ApplicationType createApplicationType(String name, Map<String, Set<String>> data)
            throws InstantiationException, IllegalAccessException {
        Map<String, Boolean> actions = getActions(data);
        String saveIndexImpl = getAttribute(data, CONFIG_SAVE_INDEX_IMPL);
        Class saveIndex = ApplicationTypeManager.getSaveIndex(saveIndexImpl);
        String searchIndexImpl = getAttribute(data, CONFIG_SEARCH_INDEX_IMPL);
        Class searchIndex = ApplicationTypeManager.getSearchIndex(searchIndexImpl);
        String resourceComp = getAttribute(data, CONFIG_RESOURCE_COMP_IMPL);
        Class resComp = ApplicationTypeManager.getResourceComparator(resourceComp);
        String applicationClassName = getAttribute(data, APPLICATION_CLASSNAME);

        ApplicationType appType = new ApplicationType(name, actions, searchIndex, saveIndex, resComp);
        if (applicationClassName != null) {
            appType.setApplicationClassName(applicationClassName);
        }
        return appType;
    }

    /**
     * Constructs an {@link Application} object based on the provided information.
     *
     * @param applicationType The application's type.
     * @param realm The realm where the application is defined.
     * @param name The name of the application.
     * @param data The configuration settings for the application.
     * @return An {@link Application} object corresponding to the provided details.
     * @throws InstantiationException If the class settings cannot be instantiated.
     * @throws IllegalAccessException If the class settings cannot be instantiated.
     * @throws EntitlementException If the application class cannot be instantiated.
     */
    public static Application createApplication(ApplicationType applicationType, String realm, String name,
            Map<String, Set<String>> data) throws InstantiationException, IllegalAccessException,
        EntitlementException {
        Application app = ApplicationManager.newApplication(realm, name, applicationType);

        Map<String, Boolean> actions = getActions(data); //if the app doesn't come with actions, use applicationType's
        if (actions == null || actions.isEmpty()) {
            actions = applicationType.getActions();
        }

        if (actions != null && !actions.isEmpty()) {
            app.setActions(actions);
        }

        Set<String> resources = data.get(CONFIG_RESOURCES);
        if (resources != null) {
            app.setResources(resources);
        }

        String description = getAttribute(data, CONFIG_APPLICATION_DESC);
        if (description != null) {
            app.setDescription(description);
        }

        String entitlementCombiner = getAttribute(data, CONFIG_ENTITLEMENT_COMBINER);
        Class combiner = getEntitlementCombiner(entitlementCombiner, app);
        app.setEntitlementCombiner(combiner);

        Set<String> conditionClassNames = data.get(CONFIG_CONDITIONS);
        if (conditionClassNames != null) {
            app.setConditions(conditionClassNames);
        }

        Set<String> subjectClassNames = data.get(CONFIG_SUBJECTS);
        if (subjectClassNames != null) {
            app.setSubjects(subjectClassNames);
        }

        String saveIndexImpl = getAttribute(data, CONFIG_SAVE_INDEX_IMPL);
        Class saveIndex = ApplicationTypeManager.getSaveIndex(saveIndexImpl);
        if (saveIndex != null) {
            app.setSaveIndex(saveIndex);
        }

        String searchIndexImpl = getAttribute(data, CONFIG_SEARCH_INDEX_IMPL);
        Class searchIndex = ApplicationTypeManager.getSearchIndex(searchIndexImpl);
        if (searchIndex != null) {
            app.setSearchIndex(searchIndex);
        }

        String resourceComp = getAttribute(data, CONFIG_RESOURCE_COMP_IMPL);
        Class resComp = ApplicationTypeManager.getResourceComparator(resourceComp);
        if (resComp != null) {
            app.setResourceComparator(resComp);
        }

        Set<String> attributeNames = data.get(ATTR_NAME_SUBJECT_ATTR_NAMES);
        if (attributeNames != null) {
            app.setAttributeNames(attributeNames);
        }

        final Set<String> meta = data.get(ATTR_NAME_META);
        if (meta != null) {
            app.setMetaData(data.get(ATTR_NAME_META));
        }

        return app;
    }

    /**
     * Converts the map of actions into a set format where the map's key->value combinations are separated by an equals
     * character.
     *
     * @param actions The map of actions that needs to be converted.
     * @return The set of actions in key=value format.
     */
    public static Set<String> getActionSet(Map<String, Boolean> actions) {
        Set<String> set = new HashSet<String>();
        if (actions != null) {
            for (String k : actions.keySet()) {
                set.add(k + "=" + Boolean.toString(actions.get(k)));
            }
        }
        return set;
    }

    /**
     * Returns the combiner from the provided set of data within the entitlement format.
     *
     * @param data The entire set of information about an application. May not be null.
     * @return A string of the combiners name, or null if the data set is empty.
     */
    public static String getCombiner(Map<String, Set<String>> data) {
        Reject.ifNull(data);

        Set<String> subData = data.get(CONFIG_ENTITLEMENT_COMBINER);

        if (subData == null || subData.isEmpty()) {
            return null;
        }

        return subData.iterator().next();
    }

    /**
     * Returns the list of subjects from the provided set of data within
     * the entitlement format.
     *
     * @param data The entire set of information about an application
     * @return A set of Strings representing each of the conditions this application supports
     */
    public static Set<String> getSubjects(Map<String, Set<String>> data) {
        Reject.ifNull(data);

        return data.get(CONFIG_SUBJECTS);
    }

    /**
     * Returns the list of conditions from the provided set of data within
     * the entitlement format.
     *
     * @param data The entire set of information about an application
     * @return A set of Strings representing each of the conditions this application supports
     */
    public static Set<String> getConditions(Map<String, Set<String>> data) {
        Reject.ifNull(data);

        return data.get(CONFIG_CONDITIONS);
    }

    /**
     * Returns the list of conditions from the provided set of data within
     * the entitlement format.
     *
     * @param data The entire set of information about an application
     * @return A set of Strings representing each of the conditions this application supports
     */
    public static Set<String> getDescription(Map<String, Set<String>> data) {
        Reject.ifNull(data);

        return data.get(CONFIG_APPLICATION_DESC);
    }

    public static Set<String> getResources(Map<String, Set<String>> data) {
        Reject.ifNull(data);

        return data.get(CONFIG_RESOURCES);
    }

    /**
     * Converts the set of actions in key=value format to an actual map.
     *
     * @param data The set of actions that needs to be converted.
     * @return The map of actions after the conversion.
     */
    public static Map<String, Boolean> getActions(Map<String, Set<String>> data) {
        Map<String, Boolean> results = new HashMap<String, Boolean>();
        Set<String> actions = data.get(CONFIG_ACTIONS);
        if (actions != null) {
            for (String a : actions) {
                int index = a.indexOf('=');
                String name = a;
                Boolean defaultVal = Boolean.TRUE;

                if (index != -1) {
                    name = a.substring(0, index);
                    defaultVal = Boolean.parseBoolean(a.substring(index + 1));
                }
                results.put(name, defaultVal);
            }
        }
        return results;
    }

    /**
     * Returns the first attribute value for the corresponding attributeName in the data map.
     *
     * @param data The map where the attribute should be retrieved from.
     * @param attributeName The name of the attribute that should be retrieved from the map.
     * @return The attribute from the map corresponding to the provided attribute name, or <code>null</code> if no such
     * attribute is present in the map.
     */
    public static String getAttribute(Map<String, Set<String>> data, String attributeName) {
        Set<String> set = data.get(attributeName);
        return (set != null && !set.isEmpty()) ? set.iterator().next() : null;
    }

    /**
     * Returns an admin SSO token for administrative actions.
     *
     * @return An administrative SSO token.
     */
    public static SSOToken getAdminToken() {
        return AccessController.doPrivileged(AdminTokenAction.getInstance());
    }

    /**
     * Attempts to retrieve the Java Class associated with the name of an entitlement combiner.
     *
     * First, we attempt to use the new system, that being the application itself can
     * look up the name from the {@link org.forgerock.openam.entitlement.EntitlementRegistry} such that
     * the name is registered in there. This may fail. This step will be skipped if app is null.
     *
     * Second, attempts to use the given string to find a class using the provided name.
     * This is so that older systems which used the canonical name to refer to the class to instantiate
     * correctly find their class. This may also fail.
     *
     * If this fails, we simply return the default: {@link DenyOverride}.
     *
     * @param name the name used to reference the combiner. Must not be null.
     * @param app the application whose entitlement registry will be used to perform the lookup. Can be null.
     * @return the class represented by the name
     */
    private static Class getEntitlementCombiner(String name, Application app) {

        Reject.ifNull(name);

        if (app != null) {
            app.setEntitlementCombinerName(name);
            if (app.getEntitlementCombiner() != null) {
                return app.getEntitlementCombinerClass();
            }
        }

        try {
            return Class.forName(name);
        } catch (ClassNotFoundException ex) {
            PrivilegeManager.debug.error("EntitlementService.getEntitlementCombiner", ex);
        }

        return DenyOverride.class;
    }
}
