/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: EntitlementService.java,v 1.35 2009/08/14 22:46:19 veiming Exp $
 */

package com.sun.identity.entitlement.opensso;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.ApplicationManager;
import com.sun.identity.entitlement.ApplicationType;
import com.sun.identity.entitlement.ApplicationTypeManager;
import com.sun.identity.entitlement.EntitlementConfiguration;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.interfaces.ISaveIndex;
import com.sun.identity.entitlement.interfaces.ISearchIndex;
import com.sun.identity.entitlement.interfaces.ResourceName;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.ldap.util.DN;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceSchemaManager;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class EntitlementService extends EntitlementConfiguration {
    /**
     * Entitlement Service name.
     */
    public static final String SERVICE_NAME = "sunEntitlementService";

    private static final String ATTR_NAME_SUBJECT_ATTR_NAMES =
        "subjectAttributeNames";
    private static final String CONFIG_APPLICATIONS = "registeredApplications";
    private static final String CONFIG_APPLICATION = "application";
    private static final String CONFIG_APPLICATION_DESC = "description";
    private static final String SCHEMA_APPLICATIONS = "applications";
    private static final String CONFIG_APPLICATIONTYPE = "applicationType";
    private static final String CONFIG_ACTIONS = "actions";
    private static final String CONFIG_RESOURCES = "resources";
    private static final String CONFIG_CONDITIONS = "conditions";
    private static final String CONFIG_SUBJECTS = "subjects";
    private static final String CONFIG_ENTITLEMENT_COMBINER =
        "entitlementCombiner";
    private static final String CONFIG_SEARCH_INDEX_IMPL = "searchIndexImpl";
    private static final String CONFIG_SAVE_INDEX_IMPL = "saveIndexImpl";
    private static final String CONFIG_RESOURCE_COMP_IMPL = "resourceComparator";
    private static final String CONFIG_APPLICATION_TYPES = "applicationTypes";
    private static final String CONFIG_SUBJECT_ATTRIBUTES_COLLECTORS =
        "subjectAttributesCollectors";
    private static final String USE_NEW_CONSOLE = "usenewconsole";
    private static final String MIGRATED_TO_ENTITLEMENT_SERVICES =
        "migratedtoentitlementservice";
    private static final String XACML_PRIVILEGE_ENABLED =
        "xacml-privilege-enabled";
    private static final String APPLICATION_CLASSNAME = "applicationClassName";

    private String realm;
    private static SSOToken adminToken =
        (SSOToken)AccessController.doPrivileged(
        AdminTokenAction.getInstance());

    /**
     * Constructor.
     */
    public EntitlementService(String realm) {
        this.realm = realm;
    }

    /**
     * Returns set of attribute values of a given attribute name,
     *
     * @param attributeName attribute name.
     * @return set of attribute values of a given attribute name,
     */
    public Set<String> getConfiguration(String attrName) {
        SSOToken token = getSSOToken();
        return getConfiguration(token, attrName);
    }

    private static Set<String> getConfiguration(
        SSOToken token,
        String attrName
    ) {
        try {
            if (token != null) {
                ServiceSchemaManager smgr = new ServiceSchemaManager(
                    SERVICE_NAME, token);
                AttributeSchema as = smgr.getGlobalSchema().getAttributeSchema(
                    attrName);
                return as.getDefaultValues();
            } else {
                PrivilegeManager.debug.error(
                    "EntitlementService.getAttributeValues: " +
                    "admin token is missing", null);
            }
        } catch (SMSException ex) {
            PrivilegeManager.debug.error(
                "EntitlementService.getAttributeValues", ex);
        } catch (SSOException ex) {
            PrivilegeManager.debug.error(
                "EntitlementService.getAttributeValues", ex);
        }
        return Collections.EMPTY_SET;
    }

    private static void setConfiguration(SSOToken token,
        String attrName, Set<String> values) {
        try {
            if (token != null) {
                ServiceSchemaManager smgr = new ServiceSchemaManager(
                    SERVICE_NAME, token);
                AttributeSchema as = smgr.getGlobalSchema().getAttributeSchema(
                    attrName);
                if (as != null) {
                    as.setDefaultValues(values);
                }
            } else {
                PrivilegeManager.debug.error(
                    "EntitlementService.getAttributeValues: " +
                    "admin token is missing", null);
            }
        } catch (SMSException ex) {
            PrivilegeManager.debug.error(
                "EntitlementService.setAttributeValues", ex);
        } catch (SSOException ex) {
            PrivilegeManager.debug.error(
                "EntitlementService.setAttributeValues", ex);
        }
    }

    /**
     * Returns a set of registered application type.
     *
     * @return A set of registered application type.
     */
    public Set<ApplicationType> getApplicationTypes() {
        Set<ApplicationType> results = new HashSet<ApplicationType>();
        try {
            SSOToken token = getSSOToken();

            if (token == null) {
                PrivilegeManager.debug.error(
                    "EntitlementService.getApplicationTypes : "+
                    "admin sso token is absent", null);
            } else {
                ServiceConfig conf = getApplicationTypeCollectionConfig(
                    token);
                Set<String> names = conf.getSubConfigNames();
                for (String name : names) {
                    ServiceConfig appType = conf.getSubConfig(name);
                    Map<String, Set<String>> data = appType.getAttributes();
                    results.add(createApplicationType(name, data));
                }
            }
        } catch (InstantiationException ex) {
            PrivilegeManager.debug.error(
                "EntitlementService.getApplicationTypes", ex);
        } catch (IllegalAccessException ex) {
            PrivilegeManager.debug.error(
                "EntitlementService.getApplicationTypes", ex);
        } catch (SMSException ex) {
            PrivilegeManager.debug.error(
                "EntitlementService.getApplicationTypes", ex);
        } catch (SSOException ex) {
            PrivilegeManager.debug.error(
                "EntitlementService.getApplicationTypes", ex);
        }
        return results;
    }

    private ServiceConfig getApplicationTypeCollectionConfig(SSOToken token)
        throws SMSException, SSOException {
        ServiceConfigManager mgr = new ServiceConfigManager(
            SERVICE_NAME, token);
        ServiceConfig globalConfig = mgr.getGlobalConfig(null);
        if (globalConfig != null) {
            return globalConfig.getSubConfig(CONFIG_APPLICATION_TYPES);
        }
        return null;
    }

    private Set<String> getActionSet(Map<String, Boolean> actions) {
        Set<String> set = new HashSet<String>();
        if (actions != null) {
            for (String k : actions.keySet()) {
                set.add(k + "=" + Boolean.toString(actions.get(k)));
            }
        }
        return set;
    }

    private Map<String, Boolean> getActions(Map<String, Set<String>> data) {
        Map<String, Boolean> results = new HashMap<String, Boolean>();
        Set<String> actions = data.get(CONFIG_ACTIONS);
        for (String a : actions) {
            int index = a.indexOf('=');
            String name = a;
            Boolean defaultVal = Boolean.TRUE;

            if (index != -1) {
                name = a.substring(0, index);
                defaultVal = Boolean.parseBoolean(a.substring(index+1));
            }
            results.put(name, defaultVal);
        }
        return results;
    }

    private String getAttribute(
        Map<String, Set<String>> data,
        String attributeName) {
        Set<String> set = data.get(attributeName);
        return ((set != null) && !set.isEmpty()) ? set.iterator().next() : null;
    }

    private Set<String> getSet(String str) {
        Set<String> set = new HashSet<String>();
        if (str != null) {
            set.add(str);
        }
        return set;
    }

    private Application getRawApplication(String name) {
        SSOToken token = getSSOToken();
        Set<Application> applications = getRawApplications(token);
        for (Application a : applications) {
            if (a.getName().equals(name)) {
                return a;
            }
        }
        return null;
    }

    private Application getApplication(String name) {
        Set<Application> applications = getApplications();
        for (Application a : applications) {
            if (a.getName().equals(name)) {
                return a;
            }
        }
        return null;
    }

    private SSOToken getSSOToken() {
        return (getAdminSubject() == PrivilegeManager.superAdminSubject) ?
            adminToken :
            SubjectUtils.getSSOToken(getAdminSubject());
    }

    /**
     * Returns a set of registered applications.
     *
     * @return a set of registered applications.
     */
    public Set<Application> getApplications() {
        boolean hasWebAgent = false;

        Set<Application> results = getApplications(realm);
        for (Application app : results) {
            if (!hasWebAgent) {
                hasWebAgent = app.getName().equals(
                    ApplicationTypeManager.URL_APPLICATION_TYPE_NAME);
            }
        }

        SSOToken token = getSSOToken();
        if (OpenSSOIndexStore.isOrgAliasMappingResourceEnabled(token) &&
            !hasWebAgent) {
            Set<Application> rootApps = getApplications("/");
            for (Application a : rootApps) {
                if (a.getName().equals(
                    ApplicationTypeManager.URL_APPLICATION_TYPE_NAME)) {
                    try {
                        Set<String> resources =
                            OpenSSOIndexStore.getOrgAliasMappingResources(
                            realm,
                            ApplicationTypeManager.URL_APPLICATION_TYPE_NAME);
                        Application clone = a.refers(realm, resources);
                        results.add(clone);

                    } catch (SMSException ex) {
                        PrivilegeManager.debug.error(
                            "EntitlementService.getApplications", ex);
                    }
                    break;
                }
            }
        }
        return results;
    }

    private Set<Application> getApplications(String curRealm) {
        SSOToken token = getSSOToken();

        Set<Application> results = getRawApplications(token, curRealm);
        for (Application app : results) {
            Set<String> resources = app.getResources();
            Set<String> res = new HashSet<String>();

            for (String r : resources) {
                int idx = r.indexOf('\t');
                if (idx != -1) {
                    res.add(r.substring(idx+1));
                } else {
                    res.add(r);
                }
            }
            app.setResources(res);
        }
        return results;
    }


    /**
     * Returns a set of registered applications.
     *
     * @return a set of registered applications.
     */
    private Set<Application> getRawApplications(SSOToken token) {
        if (realm.startsWith(SMSEntry.SUN_INTERNAL_REALM_PREFIX)) {
            realm = "/";
        }
        return getRawApplications(token, realm);
    }

    /**
     * Returns a set of registered applications.
     *
     * @return a set of registered applications.
     */
    private Set<Application> getRawApplications(
        SSOToken token, String curRealm) {
        Set<Application> results = new HashSet<Application>();
        try {
            if (token != null) {
                // TODO. Since applications for the hidden realms have to be
                // the same as root realm mainly for delegation without any
                // referrals, the hack is to use root realm for hidden realm.
                String hackRealm = (DN.isDN(curRealm)) ?
                    DNMapper.orgNameToRealmName(curRealm) : curRealm;
                ServiceConfigManager mgr = new ServiceConfigManager(
                    SERVICE_NAME, token);
                ServiceConfig orgConfig = mgr.getOrganizationConfig(
                    hackRealm, null);
                if (orgConfig != null) {
                    ServiceConfig conf = orgConfig.getSubConfig(
                        CONFIG_APPLICATIONS);
                    if (conf != null) {
                        Set<String> names = conf.getSubConfigNames();

                        for (String name : names) {
                            ServiceConfig applConf = conf.getSubConfig(name);
                            Map<String, Set<String>> data =
                                applConf.getAttributes();
                            Application app = createApplication(curRealm, name,
                                data);
                            results.add(app);
                        }
                    }
                }
            } else {
                PrivilegeManager.debug.error(
                    "EntitlementService.getApplications, admin token is missing",
                    null);
            }
        } catch (EntitlementException ex) {
            PrivilegeManager.debug.error(
                "EntitlementService.getApplications", ex);
        } catch (ClassCastException ex) {
            PrivilegeManager.debug.error(
                "EntitlementService.getApplications", ex);
        } catch (InstantiationException ex) {
            PrivilegeManager.debug.error(
                "EntitlementService.getApplications", ex);
        } catch (IllegalAccessException ex) {
            PrivilegeManager.debug.error(
                "EntitlementService.getApplications", ex);
        } catch (SMSException ex) {
            PrivilegeManager.debug.error(
                "EntitlementService.getApplications", ex);
        } catch (SSOException ex) {
            PrivilegeManager.debug.error(
                "EntitlementService.getApplications", ex);
        }
        return results;
    }

    private static Class getEntitlementCombiner(String className) {
        if (className == null) {
            return null;
        }
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            PrivilegeManager.debug.error(
                "EntitlementService.getEntitlementCombiner", ex);
        }
        return com.sun.identity.entitlement.DenyOverride.class;
    }

    /**
     * Returns subject attribute names.
     *
     * @param application Application name.
     * @return subject attribute names.
     * @throws EntitlementException if subject attribute names cannot be
     * returned.
     */
    public void addSubjectAttributeNames(
        String applicationName,
        Set<String> names
    ) throws EntitlementException {
        if ((names == null) || names.isEmpty()) {
            return;
        }
        
        try {
            SSOToken token = getSSOToken();

            if (token == null) {
                throw new EntitlementException(225);
            }

            ServiceConfig applConf = getApplicationSubConfig(token, realm,
                applicationName);
            if (applConf != null) {
                Set<String> orig = (Set<String>)
                    applConf.getAttributes().get(ATTR_NAME_SUBJECT_ATTR_NAMES);
                if ((orig == null) || orig.isEmpty()) {
                    orig = new HashSet<String>();
                }
                orig.addAll(names);
                Map<String, Set<String>> map = new
                    HashMap<String, Set<String>>();
                map.put(ATTR_NAME_SUBJECT_ATTR_NAMES, orig);
                applConf.setAttributes(map);
            }
        } catch (SMSException ex) {
            throw new EntitlementException(220, ex);
        } catch (SSOException ex) {
            throw new EntitlementException(220, ex);
        }
    }

    /**
     * Adds a new action.
     *
     * @param appName application name.
     * @param name Action name.
     * @param defVal Default value.
     * @throws EntitlementException if action cannot be added.
     */
    public void addApplicationAction(
        String appName,
        String name,
        Boolean defVal
    ) throws EntitlementException {
        try {
            SSOToken token = SubjectUtils.getSSOToken(getAdminSubject());

            if (token == null) {
                throw new EntitlementException(226);
            }

            ServiceConfig applConf = getApplicationSubConfig(
                token, realm, appName);

            if (applConf != null) {
                Map<String, Set<String>> data =
                    applConf.getAttributes();
                Map<String, Set<String>> result =
                    addAction(data, name, defVal);
                if (result != null) {
                    applConf.setAttributes(result);
                }
            }
        } catch (SMSException ex) {
            throw new EntitlementException(221, ex);
        } catch (SSOException ex) {
            throw new EntitlementException(221, ex);
        }
    }

    private ServiceConfig getApplicationSubConfig(
        SSOToken token,
        String realm,
        String appName
    ) throws SMSException, SSOException {
        ServiceConfig applConf = null;
        ServiceConfigManager mgr = new ServiceConfigManager(SERVICE_NAME,
            token);
        ServiceConfig orgConfig = mgr.getOrganizationConfig(realm, null);
        if (orgConfig != null) {
            ServiceConfig conf = orgConfig.getSubConfig(
                CONFIG_APPLICATIONS);
            if (conf != null) {
                applConf = conf.getSubConfig(appName);
            }
        }
        return applConf;
    }

    private Map<String, Set<String>> addAction(
        Map<String, Set<String>> data,
        String name,
        Boolean defVal
    ) throws EntitlementException {
        Map<String, Set<String>> results = null;

        Map<String, Boolean> actionMap = getActions(data);
        if (!actionMap.keySet().contains(name)) {
            Set<String> actions = data.get(CONFIG_ACTIONS);
            Set<String> cloned = new HashSet<String>();
            cloned.addAll(actions);
            cloned.add(name + "=" + defVal.toString());
            results = new HashMap<String, Set<String>>();
            results.put(CONFIG_ACTIONS, cloned);
        } else {
            Object[] args = {name};
            throw new EntitlementException(222, args);
        }

        return results;
    }

    /**
     * Removes application.
     *
     * @param name name of application to be removed.
     * @throws EntitlementException if application cannot be removed.
     */
    public void removeApplication(String name)
        throws EntitlementException
    {
        try {
            ServiceConfig conf = getApplicationCollectionConfig(realm);
            if (conf != null) {
                conf.removeSubConfig(name);
                Map<String, String> params = new HashMap<String, String>();
                params.put(NotificationServlet.ATTR_REALM_NAME, realm);
                Notifier.submit(NotificationServlet.APPLICATIONS_CHANGED,
                    params);
            }
        } catch (SMSException ex) {
            Object[] args = {name};
            throw new EntitlementException(230, args);
        } catch (SSOException ex) {
            Object[] args = {name};
            throw new EntitlementException(230, args);
        }
    }

    /**
     * Removes application.
     *
     * @param name name of application to be removed.
     * @param resources Resource name to be removed.
     * @throws EntitlementException if application cannot be removed.
     */
    public void removeApplication(String name, Set<String> resources)
        throws EntitlementException {
        Application appl = getApplication(name);
        if (appl != null) {
            Application store = getStorableApplication(appl, false);

            if (store.getResources().isEmpty()) {
                removeApplication(name);
            } else {
                storeApplication(appl, false);
            }
        }
    }

    /**
     * Removes application type.
     *
     * @param name name of application type to be removed.
     * @throws EntitlementException  if application type cannot be removed.
     */
    public void removeApplicationType(String name)
        throws EntitlementException{
        try {
            SSOToken token = SubjectUtils.getSSOToken(getAdminSubject());

            if (token == null) {
                Object[] arg = {name};
                throw new EntitlementException(245, arg);
            }
            ServiceConfig conf = getApplicationTypeCollectionConfig(token);
            if (conf != null) {
                conf.removeSubConfig(name);
            }
        } catch (SMSException ex) {
            Object[] arg = {name};
            throw new EntitlementException(240, arg, ex);
        } catch (SSOException ex) {
            Object[] arg = {name};
            throw new EntitlementException(240, arg, ex);
        }
    }

    private ServiceConfig getApplicationCollectionConfig(String realm)
        throws SMSException, SSOException {
        SSOToken token = getSSOToken();
        ServiceConfigManager mgr = new ServiceConfigManager(SERVICE_NAME,
            token);
        ServiceConfig orgConfig = mgr.getOrganizationConfig(realm, null);
        if (orgConfig != null) {
            return orgConfig.getSubConfig(CONFIG_APPLICATIONS);
        }
        return null;
    }

    private ServiceConfig createApplicationCollectionConfig(String realm)
        throws SMSException, SSOException {
        ServiceConfig sc = null;
        SSOToken token = SubjectUtils.getSSOToken(getAdminSubject());
        ServiceConfigManager mgr = new ServiceConfigManager(SERVICE_NAME,
            token);
        ServiceConfig orgConfig = mgr.getOrganizationConfig(realm, null);
        if (orgConfig != null) {
            sc = orgConfig.getSubConfig(CONFIG_APPLICATIONS);
        }

        if (sc == null) {
            orgConfig.addSubConfig(CONFIG_APPLICATIONS, SCHEMA_APPLICATIONS, 0,
                Collections.EMPTY_MAP);
            sc = orgConfig.getSubConfig(CONFIG_APPLICATIONS);
        }
        return sc;
    }


    /**
     * Stores the application to data store.
     *
     * @param application Application object.
     * @throws EntitlementException if application cannot be stored.
     */
    public void storeApplication(Application appl)
        throws EntitlementException {
        storeApplication(appl, true);
    }
    
    /**
     * Stores the application to data store.
     *
     * @param application Application object.
     * @throws EntitlementException if application cannot be stored.
     */
    public void storeApplication(Application appl, boolean add)
        throws EntitlementException {
        try {
            ServiceConfig orgConfig = createApplicationCollectionConfig(realm);
            ServiceConfig appConfig = orgConfig.getSubConfig(appl.getName());
            if (appConfig == null) {
                orgConfig.addSubConfig(appl.getName(),
                    CONFIG_APPLICATION, 0, getApplicationData(appl, add));
            } else {
                appConfig.setAttributes(getApplicationData(appl, add));
            }
            
            Map<String, String> params = new HashMap<String, String>();
            params.put(NotificationServlet.ATTR_REALM_NAME, realm);
            Notifier.submit(NotificationServlet.APPLICATIONS_CHANGED,
                params);
        } catch (SMSException ex) {
            Object[] arg = {appl.getName()};
            throw new EntitlementException(231, arg, ex);
        } catch (SSOException ex) {
            Object[] arg = {appl.getName()};
            throw new EntitlementException(231, arg, ex);
        }
    }

    /**
     * Stores the application type to data store.
     *
     * @param applicationType Application type  object.
     * @throws EntitlementException if application type cannot be stored.
     */
    public void storeApplicationType(ApplicationType applicationType)
        throws EntitlementException {
        try {
            SSOToken token = SubjectUtils.getSSOToken(getAdminSubject());

            if (token == null) {
                Object[] arg = {applicationType.getName()};
                throw new EntitlementException(246, arg);
            }

            ServiceConfig conf = getApplicationTypeCollectionConfig(token);
            if (conf != null) {
                ServiceConfig sc = conf.getSubConfig(applicationType.getName());
                if (sc == null) {
                    conf.addSubConfig(applicationType.getName(),
                        CONFIG_APPLICATIONTYPE, 0,
                        getApplicationTypeData(applicationType));
                } else {
                    sc.setAttributes(getApplicationTypeData(applicationType));
                }
            }
        } catch (SMSException ex) {
            Object[] arg = {applicationType.getName()};
            throw new EntitlementException(241, arg, ex);
        } catch (SSOException ex) {
            Object[] arg = {applicationType.getName()};
            throw new EntitlementException(241, arg, ex);
        }
    }

    private Map<String, Set<String>> getApplicationTypeData(
        ApplicationType applType) {
        Map<String, Set<String>> data = new HashMap<String, Set<String>>();
        data.put(CONFIG_ACTIONS, getActionSet(applType.getActions()));

        ISaveIndex sIndex = applType.getSaveIndex();
        String saveIndexClassName = (sIndex != null) ?
            sIndex.getClass().getName() : null;
        data.put(CONFIG_SAVE_INDEX_IMPL, (saveIndexClassName == null) ?
            Collections.EMPTY_SET : getSet(saveIndexClassName));

        ISearchIndex searchIndex = applType.getSearchIndex();
        String searchIndexClassName = (searchIndex != null) ?
            searchIndex.getClass().getName() : null;
        data.put(CONFIG_SEARCH_INDEX_IMPL, (searchIndexClassName == null) ?
            Collections.EMPTY_SET : getSet(searchIndexClassName));

        ResourceName recComp = applType.getResourceComparator();
        String resCompClassName = (recComp != null) ?
            recComp.getClass().getName() : null;
        data.put(CONFIG_RESOURCE_COMP_IMPL, (resCompClassName == null) ?
            Collections.EMPTY_SET : getSet(resCompClassName));

        return data;
    }

    private Map<String, Integer> getResourceCount(Set<String> res) {
        Map<String, Integer> results = new HashMap<String, Integer>();
        for (String r : res) {
            int idx = r.indexOf('\t');
            if (idx != -1) {
                try {
                    String resource = r.substring(idx + 1);
                    int cnt = Integer.parseInt(r.substring(0, idx));
                    results.put(resource, cnt);
                } catch (NumberFormatException e) {
                    results.put(r, 1);
                }
            }
        }
        return results;
    }

    private Application getStorableApplication(Application app, boolean add) {
        Application existing = getRawApplication(app.getName());
        Map<String, Integer> existingRes = (existing != null) ?
            getResourceCount(existing.getResources()) :
            Collections.EMPTY_MAP;
        Set<String> resources = app.getResources();
        Set<String> res = new HashSet<String>();

        if (resources != null) {
            for (String r : resources) {
                if (add) {
                    int cnt = (existingRes.containsKey(r)) ?
                        existingRes.get(r) +1 : 1;
                    res.add(cnt + "\t" + r);
                } else {
                    int cnt = (existingRes.containsKey(r)) ?
                        existingRes.get(r) -1 : 0;
                    if (cnt > 0) {
                        res.add(cnt + "\t" + r);
                    }
                }
            }
            for (String r : existingRes.keySet()) {
               if (!resources.contains(r)) {
                   int cnt = existingRes.get(r);
                   res.add(cnt + "\t" + r);
               }
            }
        } else {
            for (String r : existingRes.keySet()) {
                int cnt = existingRes.get(r);
                res.add(cnt + "\t" + r);
            }
        }
        Application clone = app.clone();
        clone.setResources(res);
        return clone;
    }

    private Map<String, Set<String>> getApplicationData(Application appl,
        boolean add) {
        Application app = getStorableApplication(appl, add);
        Set<String> resources = app.getResources();
        
        Map<String, Set<String>> data = new HashMap<String, Set<String>>();
        data.put(CONFIG_APPLICATIONTYPE, 
            getSet(app.getApplicationType().getName()));
        data.put(CONFIG_APPLICATION_DESC, getSet(appl.getDescription()));
        data.put(CONFIG_ACTIONS, getActionSet(app.getActions()));
        data.put(CONFIG_RESOURCES, (resources == null) ? Collections.EMPTY_SET :
            resources);
        data.put(CONFIG_ENTITLEMENT_COMBINER,
            getSet(app.getEntitlementCombiner().getClass().getName()));
        Set<String> conditions = app.getConditions();
        data.put(CONFIG_CONDITIONS, (conditions == null) ?
            Collections.EMPTY_SET : conditions);

        Set<String> subjects = app.getSubjects();
        data.put(CONFIG_SUBJECTS, (subjects == null) ?
            Collections.EMPTY_SET : subjects);

        ISaveIndex sIndex = app.getSaveIndex();
        String saveIndexClassName = (sIndex != null) ? 
            sIndex.getClass().getName() : null;
        data.put(CONFIG_SAVE_INDEX_IMPL, (saveIndexClassName == null) ?
            Collections.EMPTY_SET : getSet(saveIndexClassName));

        ISearchIndex searchIndex = app.getSearchIndex();
        String searchIndexClassName = (searchIndex != null) ?
            searchIndex.getClass().getName() : null;
        data.put(CONFIG_SEARCH_INDEX_IMPL, (searchIndexClassName == null) ?
            Collections.EMPTY_SET : getSet(searchIndexClassName));

        ResourceName recComp = app.getResourceComparator();
        String resCompClassName = (recComp != null) ? 
            recComp.getClass().getName() : null;
        data.put(CONFIG_RESOURCE_COMP_IMPL, (resCompClassName == null) ?
            Collections.EMPTY_SET : getSet(resCompClassName));

        Set<String> sbjAttributes = app.getAttributeNames();
        data.put(ATTR_NAME_SUBJECT_ATTR_NAMES, (sbjAttributes == null) ?
            Collections.EMPTY_SET : sbjAttributes);
        return data;
    }

    private ApplicationType createApplicationType(
        String name,
        Map<String, Set<String>> data
    ) throws InstantiationException, IllegalAccessException {
        Map<String, Boolean> actions = getActions(data);
        String saveIndexImpl = getAttribute(data,
            CONFIG_SAVE_INDEX_IMPL);
        Class saveIndex = ApplicationTypeManager.getSaveIndex(
            saveIndexImpl);
        String searchIndexImpl = getAttribute(data,
            CONFIG_SEARCH_INDEX_IMPL);
        Class searchIndex =
            ApplicationTypeManager.getSearchIndex(searchIndexImpl);
        String resourceComp = getAttribute(data,
            CONFIG_RESOURCE_COMP_IMPL);
        Class resComp =
            ApplicationTypeManager.getResourceComparator(resourceComp);
        String applicationClassName = getAttribute(data,APPLICATION_CLASSNAME);

        ApplicationType appType = new ApplicationType(name, actions,
            searchIndex, saveIndex, resComp);
        if (applicationClassName != null) {
            appType.setApplicationClassName(applicationClassName);
        }
        return appType;
    }
    
    private Application createApplication(
        String realm,
        String name,
        Map<String, Set<String>> data
    ) throws InstantiationException, IllegalAccessException,
        EntitlementException {
        String applicationType = getAttribute(data,
            CONFIG_APPLICATIONTYPE);
        ApplicationType appType = ApplicationTypeManager.getAppplicationType(
            getAdminSubject(), applicationType);
        Application app = ApplicationManager.newApplication(realm, name,
            appType);

        Map<String, Boolean> actions = getActions(data);
        if ((actions != null) && !actions.isEmpty()) {
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

        String entitlementCombiner = getAttribute(data,
            CONFIG_ENTITLEMENT_COMBINER);
        Class combiner = getEntitlementCombiner(
            entitlementCombiner);
        app.setEntitlementCombiner(combiner);

        Set<String> conditionClassNames = data.get(
            CONFIG_CONDITIONS);
        if (conditionClassNames != null) {
            app.setConditions(conditionClassNames);
        }

        Set<String> subjectClassNames = data.get(
            CONFIG_SUBJECTS);
        if (subjectClassNames != null) {
            app.setSubjects(subjectClassNames);
        }

        String saveIndexImpl = getAttribute(data,
            CONFIG_SAVE_INDEX_IMPL);
        Class saveIndex = ApplicationTypeManager.getSaveIndex(
            saveIndexImpl);
        if (saveIndex != null) {
            app.setSaveIndex(saveIndex);
        }

        String searchIndexImpl = getAttribute(data,
            CONFIG_SEARCH_INDEX_IMPL);
        Class searchIndex =
            ApplicationTypeManager.getSearchIndex(searchIndexImpl);
        if (searchIndex != null) {
            app.setSearchIndex(searchIndex);
        }

        String resourceComp = getAttribute(data,
            CONFIG_RESOURCE_COMP_IMPL);
        Class resComp =
            ApplicationTypeManager.getResourceComparator(resourceComp);
        if (resComp != null) {
            app.setResourceComparator(resComp);
        }

        Set<String> attributeNames = data.get(
            ATTR_NAME_SUBJECT_ATTR_NAMES);
        if (attributeNames != null) {
            app.setAttributeNames(attributeNames);
        }

        return app;
    }

    /**
     * Returns subject attribute names.
     *
     * @param application Application name.
     * @return subject attribute names.
     */
    public Set<String> getSubjectAttributeNames(String application) {
        try {
            SSOToken token = SubjectUtils.getSSOToken(getAdminSubject());

            if (token == null) {
                PrivilegeManager.debug.error(
                    "EntitlementService.getSubjectAttributeNames: " +
                    "admin sso token is absent", null);
            } else {
                ServiceConfig applConfig = getApplicationSubConfig(
                    token, realm, application);
                if (applConfig != null) {
                    Application app = createApplication(realm, application,
                        applConfig.getAttributes());
                    return app.getAttributeNames();
                }
            }
        } catch (EntitlementException ex) {
            PrivilegeManager.debug.error(
                "EntitlementService.getSubjectAttributeNames", ex);
        } catch (InstantiationException ex) {
            PrivilegeManager.debug.error(
                "EntitlementService.getSubjectAttributeNames", ex);
        } catch (IllegalAccessException ex) {
            PrivilegeManager.debug.error(
                "EntitlementService.getSubjectAttributeNames", ex);
        } catch (SMSException ex) {
            PrivilegeManager.debug.error(
                "EntitlementService.getSubjectAttributeNames", ex);
        } catch (SSOException ex) {
            PrivilegeManager.debug.error(
                "EntitlementService.getSubjectAttributeNames", ex);
        }
        return Collections.EMPTY_SET;
    }

    /**
     * Returns subject attributes collector names.
     *
     * @return subject attributes collector names.
     */
    public Set<String> getSubjectAttributesCollectorNames() {
        try {
            SSOToken token = getSSOToken();

            if (token != null) {
                ServiceConfigManager mgr = new ServiceConfigManager(
                    SERVICE_NAME, token);
                ServiceConfig orgConfig = mgr.getOrganizationConfig(
                    realm, null);
                if (orgConfig != null) {
                    ServiceConfig conf = orgConfig.getSubConfig(
                        CONFIG_SUBJECT_ATTRIBUTES_COLLECTORS);
                    return conf.getSubConfigNames();
                }
            } else {
                PrivilegeManager.debug.error(
                    "EntitlementService.getSubjectAttributesCollectorNames: " +
                    "admin sso token is absent", null);
            }
        } catch (SMSException ex) {
            PrivilegeManager.debug.error(
                "EntitlementService.getSubjectAttributesCollectorNames", ex);
        } catch (SSOException ex) {
            PrivilegeManager.debug.error(
                "EntitlementService.getSubjectAttributesCollectorNames", ex);
        }
        return null;
    }

    /**
     * Returns subject attributes collector configuration.
     *
     * @param name subject attributes collector name
     * @return subject attributes collector configuration.
     */
    public Map<String, Set<String>>
        getSubjectAttributesCollectorConfiguration(String name) {

        try {
            SSOToken token = getSSOToken();

            if (token != null) {
                ServiceConfigManager mgr = new ServiceConfigManager(
                    SERVICE_NAME, token);
                ServiceConfig orgConfig = mgr.getOrganizationConfig(realm, null);
                if (orgConfig != null) {
                    ServiceConfig conf = orgConfig.getSubConfig(
                        CONFIG_SUBJECT_ATTRIBUTES_COLLECTORS);
                    ServiceConfig sacConfig = conf.getSubConfig(name);
                    if (sacConfig != null) {
                        return sacConfig.getAttributes();
                    }
                }
            } else {
                PrivilegeManager.debug.error(
                "EntitlementService.getSubjectAttributesCollectorConfiguration:"
                    + "admin sso token is absent", null);
            }
        } catch (SMSException ex) {
            PrivilegeManager.debug.error(
                "EntitlementService.getSubjectAttributesCollectorConfiguration",
                ex);
        } catch (SSOException ex) {
            PrivilegeManager.debug.error(
                "EntitlementService.getSubjectAttributesCollectorConfiguration",
                ex);
        }
        return null;
    }

    /**
     * Returns <code>true</code> if OpenSSO policy data is migrated to a
     * form that entitlements service can operates on them.
     *
     * @return <code>true</code> if OpenSSO policy data is migrated to a
     * form that entitlements service can operates on them.
     */
    public boolean hasEntitlementDITs() {
        try {
            new ServiceSchemaManager(SERVICE_NAME, 
                SubjectUtils.getSSOToken(getAdminSubject()));
            return true;
        } catch (SMSException ex) {
            return false;
        } catch (SSOException ex) {
            return false;
        }
    }

    /**
     * Returns <code>true</code> if the system is migrated to support
     * entitlement services.
     *
     * @return <code>true</code> if the system is migrated to support
     * entitlement services.
     */
    public boolean migratedToEntitlementService() {
        if (!hasEntitlementDITs()) {
            return false;
        }

        Set<String> setMigrated = getConfiguration(
            MIGRATED_TO_ENTITLEMENT_SERVICES);
        String migrated = ((setMigrated != null) && !setMigrated.isEmpty()) ?
            setMigrated.iterator().next() : null;
        return (migrated != null) ? Boolean.parseBoolean(migrated) : false;
    }

    /**
     * Returns <code>true</code> if the system stores privileges in
     * XACML format and supports exporting privileges in XACML format
     *
     *
     * @return <code>true</code> if the system stores privileges in
     * XACML format and supports exporting privileges in XACML format
     */
    public boolean xacmlPrivilegeEnabled() {
        if (!hasEntitlementDITs()) {
            return false;
        }
        Set<String> xacmlEnabledSet = getConfiguration(
            XACML_PRIVILEGE_ENABLED);
        String xacmlEnabled = ((xacmlEnabledSet != null)
                && !xacmlEnabledSet.isEmpty()) ?
                xacmlEnabledSet.iterator().next() : null;
        return (xacmlEnabled != null) ? Boolean.parseBoolean(xacmlEnabled)
                : false;
    }


    public static void useNewConsole(boolean flag) {
        if (canSwitchToNewConsole()) {
            Set<String> values = new HashSet<String>();
            values.add(Boolean.toString(flag));
            setConfiguration(adminToken, USE_NEW_CONSOLE, values);
        }
    }

    public static boolean canSwitchToNewConsole() {
        try {
            new ServiceSchemaManager(SERVICE_NAME, adminToken);

            Set<String> setMigrated = getConfiguration(adminToken,
                MIGRATED_TO_ENTITLEMENT_SERVICES);
            String migrated = ((setMigrated != null) && !setMigrated.isEmpty())
                ? setMigrated.iterator().next() : null;
            return (migrated != null) ? Boolean.parseBoolean(migrated) : false;
        } catch (SMSException ex) {
            return false;
        } catch (SSOException ex) {
            return false;
        }
    }

    public static String useNewConsole() {
        if (!canSwitchToNewConsole()) {
            return "";
        }

        Set<String> values = getConfiguration(adminToken, USE_NEW_CONSOLE);
        return ((values != null) && !values.isEmpty()) ?
            values.iterator().next() : "";
    }

    public static boolean toUseNewConsole() {
        String val = useNewConsole();
        return Boolean.parseBoolean(val);
    }
}
