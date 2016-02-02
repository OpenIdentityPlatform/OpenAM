/*
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
 * $Id: EntitlementService.java,v 1.13 2010/01/08 23:59:32 veiming Exp $
 *
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 */

package com.sun.identity.entitlement.opensso;

import static com.sun.identity.policy.PolicyEvaluator.REALM_DN;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.security.auth.Subject;

import org.forgerock.openam.entitlement.PolicyConstants;
import org.forgerock.openam.entitlement.utils.EntitlementUtils;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.openam.utils.CollectionUtils;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.ApplicationManager;
import com.sun.identity.entitlement.ApplicationType;
import com.sun.identity.entitlement.ApplicationTypeManager;
import com.sun.identity.entitlement.EntitlementConfiguration;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.interfaces.ISaveIndex;
import com.sun.identity.entitlement.interfaces.ISearchIndex;
import com.sun.identity.entitlement.interfaces.ResourceName;
import com.sun.identity.entitlement.util.SearchFilter;
import com.sun.identity.monitoring.MonitoringUtil;
import com.sun.identity.policy.PolicyConfig;
import com.sun.identity.policy.PolicyEvaluator;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceSchemaManager;

/**
 *
 */
public class EntitlementService extends EntitlementConfiguration {
    /**
     * Entitlement Service name.
     */
    public static final String SERVICE_NAME = "sunEntitlementService";
    public static final String ATTR_NAME_SUBJECT_ATTR_NAMES = "subjectAttributeNames";
    public static final String ATTR_NAME_META = "meta";
    public static final String CONFIG_CONDITIONS = "conditions";
    public static final String CONFIG_SUBJECTS = "subjects";
    public static final String CONFIG_ENTITLEMENT_COMBINER = "entitlementCombiner";
    public static final String CONFIG_SEARCH_INDEX_IMPL = "searchIndexImpl";
    public static final String CONFIG_SAVE_INDEX_IMPL = "saveIndexImpl";
    public static final String CONFIG_RESOURCE_COMP_IMPL = "resourceComparator";
    public static final String APPLICATION_CLASSNAME = "applicationClassName";

    private static final String SCHEMA_APPLICATIONS = "applications";
    private static final String CONFIG_SUBJECT_ATTRIBUTES_COLLECTORS = "subjectAttributesCollectors";
    private static final String SCHEMA_SUBJECT_ATTRIBUTES_COLLECTORS = "subjectAttributesCollectors";
    private static final String SCHEMA_OPENSSO_SUBJECT_ATTRIBUTES_COLLECTOR = "OpenSSOSubjectAttributesCollector";
    private static final String NETWORK_MONITOR_ENABLED = "network-monitor-enabled";
    private static final String MIGRATED_TO_ENTITLEMENT_SERVICES = "migratedtoentitlementservice";
    private static final String XACML_PRIVILEGE_ENABLED = "xacml-privilege-enabled";
    private static final String REALM_DN_TEMPLATE = "ou={0},ou=default,ou=OrganizationConfig,ou=1.0,ou="
            + SERVICE_NAME + ",ou=services,{1}";

    private String realm;

    /**
     * Constructor.
     */
    public EntitlementService(String realm) {
        this.realm = realm;
    }

    /**
     * Returns set of attribute values of a given attribute name,
     *
     * @param attrName attribute name.
     * @return set of attribute values of a given attribute name,
     */
    public Set<String> getConfiguration(String attrName) {
        return getConfiguration(EntitlementUtils.getAdminToken(), attrName);
    }

    public static int getConfiguration(String attrName, int defaultValue) {
        Set<String> values = getConfiguration(EntitlementUtils.getAdminToken(), attrName);
        if ((values == null) || values.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(values.iterator().next());
        } catch (NumberFormatException e) {
            PolicyConstants.DEBUG.error(
                "EntitlementService.getConfiguration: attribute name=" +
                attrName, e);
            return defaultValue;
        }
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

                if (as != null) {
                    return as.getDefaultValues();
                }
            } else {
                PolicyConstants.DEBUG.error(
                    "EntitlementService.getAttributeValues: " +
                    "admin token is missing");
            }
        } catch (SMSException ex) {
            PolicyConstants.DEBUG.error(
                "EntitlementService.getAttributeValues", ex);
        } catch (SSOException ex) {
            PolicyConstants.DEBUG.error(
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
                PolicyConstants.DEBUG.error(
                    "EntitlementService.getAttributeValues: " +
                    "admin token is missing");
            }
        } catch (SMSException ex) {
            PolicyConstants.DEBUG.error(
                "EntitlementService.setAttributeValues", ex);
        } catch (SSOException ex) {
            PolicyConstants.DEBUG.error(
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
                PolicyConstants.DEBUG.error(
                    "EntitlementService.getApplicationTypes : "+
                    "admin sso token is absent");
            } else {
                ServiceConfig conf = getApplicationTypeCollectionConfig(
                    token);
                Set<String> names = conf.getSubConfigNames();
                for (String name : names) {
                    ServiceConfig appType = conf.getSubConfig(name);
                    Map<String, Set<String>> data = appType.getAttributes();
                    results.add(EntitlementUtils.createApplicationType(name, data));
                }
            }
        } catch (InstantiationException ex) {
            PolicyConstants.DEBUG.error(
                "EntitlementService.getApplicationTypes", ex);
        } catch (IllegalAccessException ex) {
            PolicyConstants.DEBUG.error(
                "EntitlementService.getApplicationTypes", ex);
        } catch (SMSException ex) {
            PolicyConstants.DEBUG.error(
                "EntitlementService.getApplicationTypes", ex);
        } catch (SSOException ex) {
            PolicyConstants.DEBUG.error(
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
            return globalConfig.getSubConfig(EntitlementUtils.APPLICATION_TYPES);
        }
        return null;
    }

    private Set<String> getSet(String str) {
        Set<String> set = new HashSet<String>();
        if (str != null) {
            set.add(str);
        }
        return set;
    }

    private SSOToken getSSOToken() {
        return (getAdminSubject() == PolicyConstants.SUPER_ADMIN_SUBJECT) ?
            EntitlementUtils.getAdminToken() :
            SubjectUtils.getSSOToken(getAdminSubject());
    }

    /**
     * Returns a set of application names for a given search criteria.
     *
     * @param adminSubject Admin Subject
     * @param filters Set of search filter.
     * @return a set of application names for a given search criteria.
     * @throws EntitlementException if search failed.
     */
    public Set<String> searchApplicationNames(
        Subject adminSubject,
        Set<SearchFilter> filters
    ) throws EntitlementException {
        SSOToken token = getSSOToken(adminSubject);

        if (token == null) {
            throw new EntitlementException(451);
        }
        
        String baseDN = getApplicationSearchBaseDN(realm);
        if (!SMSEntry.checkIfEntryExists(baseDN, token)) {
            return Collections.EMPTY_SET;
        }

        try {
            Set<String> dns = SMSEntry.search(token, baseDN,
                getApplicationSearchFilter(filters), 0, 0, false, false);
            Set<String> results = new HashSet<String>();

            for (String dn : dns) {
                if (!LDAPUtils.dnEquals(baseDN, dn)) {
                    results.add(LDAPUtils.rdnValueFromDn(dn));
                }
            }
            return results;
        } catch (SMSException e) {
            throw new EntitlementException(450, e);
        }
    }

    private SSOToken getSSOToken(Subject subject) {
        if (subject == PolicyConstants.SUPER_ADMIN_SUBJECT) {
            return EntitlementUtils.getAdminToken();
        }
        return SubjectUtils.getSSOToken(subject);
    }

    private String getApplicationSearchFilter(Set<SearchFilter> filters) {
        StringBuilder strFilter = new StringBuilder();
        if ((filters == null) || filters.isEmpty()) {
            strFilter.append("(ou=*)");
        } else {
            if (filters.size() == 1) {
                strFilter.append(filters.iterator().next().getFilter());
            } else {
                strFilter.append("(&");
                for (SearchFilter psf : filters) {
                    strFilter.append(psf.getFilter());
                }
                strFilter.append(")");
            }
        }
        return strFilter.toString();
    }

    private static String getApplicationSearchBaseDN(String realm) {
        Object[] args = {EntitlementUtils.REGISTERED_APPLICATIONS, DNMapper.orgNameToDN(realm)};
        return MessageFormat.format(REALM_DN_TEMPLATE, args);
    }

    /**
     * {@inheritDoc}
     */
    public Application getApplication(String name) {
        try {
            final ServiceConfig appConfig = getApplicationConfiguration(getSSOToken(), realm);
            final Set<String> names = appConfig.getSubConfigNames();
            if (appConfig != null && names.contains(name)) {
                return createApplication(appConfig, name);
            }
        } catch (EntitlementException ex) {
            PolicyConstants.DEBUG.error("EntitlementService.getApplication", ex);
        } catch (ClassCastException ex) {
            PolicyConstants.DEBUG.error("EntitlementService.getApplication", ex);
        } catch (InstantiationException ex) {
            PolicyConstants.DEBUG.error("EntitlementService.getApplication", ex);
        } catch (IllegalAccessException ex) {
            PolicyConstants.DEBUG.error("EntitlementService.getApplication", ex);
        } catch (SMSException ex) {
            PolicyConstants.DEBUG.error("EntitlementService.getApplication", ex);
        } catch (SSOException ex) {
            PolicyConstants.DEBUG.error("EntitlementService.getApplication", ex);
        }
        return null;
    }

    /**
     * Returns a set of registered applications.
     *
     * @return a set of registered applications.
     */
    public Set<Application> getApplications() {
        final Set<Application> results = new HashSet<Application>();
        try {
            SSOToken token = getSSOToken();
            final ServiceConfig appConfig = getApplicationConfiguration(token, realm);
            if (appConfig != null) {
                final Set<String> names = appConfig.getSubConfigNames();
                for (String name : names) {
                    results.add(createApplication(appConfig, name));
                }
            }
        } catch (EntitlementException ex) {
            PolicyConstants.DEBUG.error("EntitlementService.getRawApplications", ex);
        } catch (ClassCastException ex) {
            PolicyConstants.DEBUG.error("EntitlementService.getRawApplications", ex);
        } catch (InstantiationException ex) {
            PolicyConstants.DEBUG.error("EntitlementService.getRawApplications", ex);
        } catch (IllegalAccessException ex) {
            PolicyConstants.DEBUG.error("EntitlementService.getRawApplications", ex);
        } catch (SMSException ex) {
            PolicyConstants.DEBUG.error("EntitlementService.getRawApplications", ex);
        } catch (SSOException ex) {
            PolicyConstants.DEBUG.error("EntitlementService.getRawApplications", ex);
        }
        return results;
    }

    /**
     * Get the service config for registered applications.
     * @param token The admin token for access to the Service Config.
     * @param realm The realm from which to retrieve the service config.
     * @return The application Service Config.
     */
    private ServiceConfig getApplicationConfiguration(SSOToken token, String realm) {
        try {
            if (token != null) {
                if (realm.startsWith(SMSEntry.SUN_INTERNAL_REALM_PREFIX) ||
                    realm.startsWith(SMSEntry.SUN_INTERNAL_REALM_PREFIX2)) {
                    realm = "/";
                } 
                // TODO. Since applications for the hidden realms have to be
                // the same as root realm mainly for delegation without any
                // referrals, the hack is to use root realm for hidden realm.
                String hackRealm = LDAPUtils.isDN(realm) ? DNMapper.orgNameToRealmName(realm) : realm;
                ServiceConfigManager mgr = new ServiceConfigManager(SERVICE_NAME, token);
                ServiceConfig orgConfig = mgr.getOrganizationConfig(hackRealm, null);
                if (orgConfig != null) {
                    return orgConfig.getSubConfig(EntitlementUtils.REGISTERED_APPLICATIONS);
                }
            } else {
                PolicyConstants.DEBUG.error("EntitlementService.getApplicationConfiguration, admin token is missing");
            }
        } catch (ClassCastException ex) {
            PolicyConstants.DEBUG.error("EntitlementService.getApplicationConfiguration", ex);
        } catch (SMSException ex) {
            PolicyConstants.DEBUG.error("EntitlementService.getApplicationConfiguration", ex);
        } catch (SSOException ex) {
            PolicyConstants.DEBUG.error("EntitlementService.getApplicationConfiguration", ex);
        }
        return null;
    }

    private Application createApplication(ServiceConfig conf, String appName) throws
            IllegalAccessException, EntitlementException, InstantiationException, SMSException, SSOException {

        final Map<String, Set<String>> data = conf.getSubConfig(appName).getAttributes();
        final ApplicationType applicationType = ApplicationTypeManager.getAppplicationType(
                getAdminSubject(), EntitlementUtils.getAttribute(data, EntitlementUtils.APPLICATION_TYPE));
        return EntitlementUtils.createApplication(applicationType, appName, data);
    }

    /**
     * Returns subject attribute names.
     *
     * @param applicationName  Application name.
     * @param names subject attribute names.
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

            Application appl = ApplicationManager.getApplication(
                PolicyConstants.SUPER_ADMIN_SUBJECT, realm, applicationName);
            if (appl != null) {
                appl.addAttributeNames(names);
            }

            ServiceConfig applConf = getApplicationSubConfig(token, realm,
                applicationName);
            String parentRealm = realm;
            while (applConf == null) {
                parentRealm = getParentRealm(parentRealm);
                if (parentRealm == null) {
                    break;
                }
                applConf = getApplicationSubConfig(token, parentRealm,
                    applicationName);
            }

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
            ServiceConfig conf = orgConfig.getSubConfig(EntitlementUtils.REGISTERED_APPLICATIONS);
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

        Map<String, Boolean> actionMap = EntitlementUtils.getActions(data);
        if (!actionMap.keySet().contains(name)) {
            Set<String> actions = data.get(EntitlementUtils.CONFIG_ACTIONS);
            Set<String> cloned = new HashSet<String>();
            cloned.addAll(actions);
            cloned.add(name + "=" + defVal.toString());
            results = new HashMap<String, Set<String>>();
            results.put(EntitlementUtils.CONFIG_ACTIONS, cloned);
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
                String[] logParams = {realm, name};
                OpenSSOLogger.log(OpenSSOLogger.LogLevel.MESSAGE, Level.INFO,
                    "ATTEMPT_REMOVE_APPLICATION", logParams, getAdminSubject());
                conf.removeSubConfig(name);
                OpenSSOLogger.log(OpenSSOLogger.LogLevel.MESSAGE, Level.INFO,
                    "SUCCEEDED_REMOVE_APPLICATION", logParams,
                    getAdminSubject());

                Map<String, String> params = new HashMap<String, String>();
                params.put(NotificationServlet.ATTR_REALM_NAME, realm);
                Notifier.submit(NotificationServlet.APPLICATIONS_CHANGED,
                    params);
            }
        } catch (SMSException ex) {
            String[] logParams = {realm, name, ex.getMessage()};
            OpenSSOLogger.log(OpenSSOLogger.LogLevel.MESSAGE, Level.INFO,
                "FAILED_REMOVE_APPLICATION", logParams, getAdminSubject());
            Object[] args = {name};
            throw new EntitlementException(EntitlementException.REMOVE_APPLICATION_FAIL, args);
        } catch (SSOException ex) {
            String[] logParams = {realm, name, ex.getMessage()};
            OpenSSOLogger.log(OpenSSOLogger.LogLevel.MESSAGE, Level.INFO,
                "FAILED_REMOVE_APPLICATION", logParams, getAdminSubject());
            Object[] args = {name};
            throw new EntitlementException(EntitlementException.REMOVE_APPLICATION_FAIL, args);
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
            return orgConfig.getSubConfig(EntitlementUtils.REGISTERED_APPLICATIONS);
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
            sc = orgConfig.getSubConfig(EntitlementUtils.REGISTERED_APPLICATIONS);
        }

        if (sc == null) {
            orgConfig.addSubConfig(EntitlementUtils.REGISTERED_APPLICATIONS, SCHEMA_APPLICATIONS, 0,
                Collections.EMPTY_MAP);
            sc = orgConfig.getSubConfig(EntitlementUtils.REGISTERED_APPLICATIONS);
        }
        return sc;
    }

    /**
     * Stores the application to data store.
     *
     * @param appl Application object.
     * @throws EntitlementException if application cannot be stored.
     */
    public void storeApplication(Application appl)
        throws EntitlementException {
        SSOToken token = SubjectUtils.getSSOToken(getAdminSubject());
        try {
            createApplicationCollectionConfig(realm);
            String dn = getApplicationDN(appl.getName(), realm);
            SMSEntry s = new SMSEntry(token, dn);
            s.setAttributes(getApplicationData(appl));

            String[] logParams = {realm, appl.getName()};
            OpenSSOLogger.log(OpenSSOLogger.LogLevel.MESSAGE, Level.INFO,
                "ATTEMPT_SAVE_APPLICATION", logParams, getAdminSubject());
            s.save();
            OpenSSOLogger.log(OpenSSOLogger.LogLevel.MESSAGE, Level.INFO,
                "SUCCEEDED_SAVE_APPLICATION", logParams, getAdminSubject());
            
            Map<String, String> params = new HashMap<String, String>();
            params.put(NotificationServlet.ATTR_REALM_NAME, realm);
            Notifier.submit(NotificationServlet.APPLICATIONS_CHANGED,
                params);
        } catch (SMSException ex) {
            String[] logParams = {realm, appl.getName(), ex.getMessage()};
            OpenSSOLogger.log(OpenSSOLogger.LogLevel.ERROR, Level.INFO,
                "FAILED_SAVE_APPLICATION", logParams, getAdminSubject());
            Object[] arg = {appl.getName()};
            throw new EntitlementException(EntitlementException.MODIFY_APPLICATION_FAIL, arg, ex);
        } catch (SSOException ex) {
            String[] logParams = {realm, appl.getName(), ex.getMessage()};
            OpenSSOLogger.log(OpenSSOLogger.LogLevel.ERROR, Level.INFO,
                "FAILED_SAVE_APPLICATION", logParams, getAdminSubject());
            Object[] arg = {appl.getName()};
            throw new EntitlementException(EntitlementException.MODIFY_APPLICATION_FAIL, arg, ex);
        }
    }

    private String getApplicationDN(String name, String realm) {
        return "ou=" + name + "," + getApplicationSearchBaseDN(realm);
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
                    conf.addSubConfig(applicationType.getName(), EntitlementUtils.APPLICATION_TYPE, 0,
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
        data.put(EntitlementUtils.CONFIG_ACTIONS, EntitlementUtils.getActionSet(applType.getActions()));

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

    private Map<String, Set<String>> getApplicationData(Application appl) {
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        Set<String> setServiceID = new HashSet<String>(2);
        map.put(SMSEntry.ATTR_SERVICE_ID, setServiceID);
        setServiceID.add("application");

        Set<String> setObjectClass = new HashSet<String>(4);
        map.put(SMSEntry.ATTR_OBJECTCLASS, setObjectClass);
        setObjectClass.add(SMSEntry.OC_TOP);
        setObjectClass.add(SMSEntry.OC_SERVICE_COMP);

        Set<String> data = new HashSet<String>();
        map.put(SMSEntry.ATTR_KEYVAL, data);
        data.add(EntitlementUtils.APPLICATION_TYPE + '=' +
                appl.getApplicationType().getName());
        if (appl.getDescription() != null) {
            data.add(EntitlementUtils.CONFIG_DESCRIPTION + "=" + appl.getDescription());
        } else {
            data.add(EntitlementUtils.CONFIG_DESCRIPTION + "=");
        }

        data.add(CONFIG_ENTITLEMENT_COMBINER + "=" +
            appl.getEntitlementCombiner().getClass().getName());

        Set<String> conditions = appl.getConditions();
        if ((conditions != null) && !conditions.isEmpty()) {
            for (String c : conditions) {
                data.add(CONFIG_CONDITIONS + "=" + c);
            }
        } else {
            data.add(CONFIG_CONDITIONS + "=");
        }

        Set<String> subjects = appl.getSubjects();
        if ((subjects != null) && !subjects.isEmpty()) {
            for (String s : subjects) {
                data.add(CONFIG_SUBJECTS + "=" + s);
            }
        } else {
            data.add(CONFIG_SUBJECTS + "=");
        }

        ISaveIndex sIndex = appl.getSaveIndex();
        if (sIndex != null) {
            String saveIndexClassName = sIndex.getClass().getName();
            data.add(CONFIG_SAVE_INDEX_IMPL + "=" + saveIndexClassName);
        }

        ISearchIndex searchIndex = appl.getSearchIndex();
        if (searchIndex != null) {
            String searchIndexClassName = searchIndex.getClass().getName();
            data.add(CONFIG_SEARCH_INDEX_IMPL + "=" + searchIndexClassName);
        }

        ResourceName recComp = appl.getResourceComparator(false);
        if (recComp != null) {
            String resCompClassName = recComp.getClass().getName();
            data.add(CONFIG_RESOURCE_COMP_IMPL + "=" + resCompClassName);
        }

        Set<String> sbjAttributes = appl.getAttributeNames();
        if ((sbjAttributes != null) && !sbjAttributes.isEmpty()) {
            for (String s : sbjAttributes) {
                data.add(ATTR_NAME_SUBJECT_ATTR_NAMES + "=" + s);
            }
        } else {
            data.add(ATTR_NAME_SUBJECT_ATTR_NAMES + "=");
        }

        for (String m : appl.getMetaData()) {
            data.add(ATTR_NAME_META + "=" + m);
        }

        String displayName = appl.getDisplayName();
        data.add(EntitlementUtils.CONFIG_DISPLAY_NAME + "=" + (displayName == null ? "" : displayName));

        if (!appl.getResourceTypeUuids().isEmpty()) {
            Set<String> searchableAttributes = new HashSet<String>();
            for (String resourceTypeUuid : appl.getResourceTypeUuids()) {
                searchableAttributes.add(EntitlementUtils.CONFIG_RESOURCE_TYPE_UUIDS + "=" + resourceTypeUuid);
            }
            map.put(SMSEntry.ATTR_XML_KEYVAL, searchableAttributes);
        }

        map.put("ou", getApplicationIndices(appl));
        return map;
    }

    private Set<String> getApplicationIndices(Application appl) {
        Set<String> info = new HashSet<String>();
        info.add(appl.getName());
        info.add(Application.NAME_ATTRIBUTE + "=" + appl.getName());

        String desc = appl.getDescription();
        if (desc == null) {
            desc = "";
        }
        info.add(Application.DESCRIPTION_ATTRIBUTE + "=" + desc);

        String createdBy = appl.getCreatedBy();
        if (createdBy != null) {
            info.add(Application.CREATED_BY_ATTRIBUTE + createdBy);
        }

        String lastModifiedBy = appl.getLastModifiedBy();
        if (lastModifiedBy != null) {
            info.add(Application.LAST_MODIFIED_BY_ATTRIBUTE + "=" +
                lastModifiedBy);
        }

        long creationDate = appl.getCreationDate();
        if (creationDate > 0) {
            String data = Long.toString(creationDate) + "=" +
                Application.CREATION_DATE_ATTRIBUTE;
            info.add(data);
            info.add("|" + data);
        }

        long lastModifiedDate = appl.getLastModifiedDate();
        if (lastModifiedDate > 0) {
            String data = Long.toString(lastModifiedDate) + "=" +
                Application.LAST_MODIFIED_DATE_ATTRIBUTE;
            info.add(data);
            info.add("|" + data);
        }
        return info;
    }

    /**
     * Returns subject attribute names.
     *
     * @param application Application name.
     * @return subject attribute names.
     */
    public Set<String> getSubjectAttributeNames(String application) {
        try {
            Application app = ApplicationManager.getApplication(
                PolicyConstants.SUPER_ADMIN_SUBJECT, realm, application);
            if (app != null) {
                return app.getAttributeNames();
            }
        } catch (EntitlementException ex) {
            PolicyConstants.DEBUG.error(
                "EntitlementService.getSubjectAttributeNames", ex);
        }
        return Collections.EMPTY_SET;
    }

    /**
     * Returns subject attributes collector names.
     *
     * @return subject attributes collector names.
     * @throws EntitlementException if subject attributes collector names
     * cannot be returned.
     */
    public Set<String> getSubjectAttributesCollectorNames()
        throws EntitlementException {
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
                PolicyConstants.DEBUG.error(
                    "EntitlementService.getSubjectAttributesCollectorNames: " +
                    "admin sso token is absent");
                throw new EntitlementException(285);
            }
        } catch (SMSException ex) {
            PolicyConstants.DEBUG.error(
                "EntitlementService.getSubjectAttributesCollectorNames", ex);
            throw new EntitlementException(286, ex);
        } catch (SSOException ex) {
            PolicyConstants.DEBUG.error(
                "EntitlementService.getSubjectAttributesCollectorNames", ex);
            throw new EntitlementException(286, ex);
        }
        return null;
    }

    /**
     * Returns subject attributes collector configuration.
     *
     * @param name subject attributes collector name
     * @return subject attributes collector configuration.
     * @throws EntitlementException if subject attributes collector
     * configuration cannot be returned.
     */
    public Map<String, Set<String>>
        getSubjectAttributesCollectorConfiguration(String name)
        throws EntitlementException {

        try {
            SSOToken token = getSSOToken();

            if (token != null) {
                OrganizationConfigManager ocm = new OrganizationConfigManager(
                    token, realm);
                ServiceConfig orgConfig = ocm.getServiceConfig(SERVICE_NAME);
                if (orgConfig != null) {
                    Set<String> subConfigNames = orgConfig.getSubConfigNames();
                    if ((subConfigNames == null) || (!subConfigNames.contains(
                        CONFIG_SUBJECT_ATTRIBUTES_COLLECTORS))) {
                        orgConfig.addSubConfig(
                            CONFIG_SUBJECT_ATTRIBUTES_COLLECTORS,
                            SCHEMA_SUBJECT_ATTRIBUTES_COLLECTORS, 0,
                            Collections.EMPTY_MAP);
                    }

                    ServiceConfig conf = orgConfig.getSubConfig(
                        CONFIG_SUBJECT_ATTRIBUTES_COLLECTORS);
                    ServiceConfig subConfig = conf.getSubConfig(name);
                    if (subConfig == null) {
                        Map<String, Set<String>> attrs = Collections.EMPTY_MAP;
                        // copy from parent sub config
                        OrganizationConfigManager pocm =
                            ocm.getParentOrgConfigManager();
                        if (pocm != null) {
                            ServiceConfig porgConfig = pocm.getServiceConfig(
                                SERVICE_NAME);
                            if (porgConfig != null) {
                                ServiceConfig pconf = porgConfig.getSubConfig(
                                    CONFIG_SUBJECT_ATTRIBUTES_COLLECTORS);
                                if (pconf != null) {
                                    ServiceConfig psubConfig =
                                        pconf.getSubConfig(name);
                                    if (psubConfig != null) {
                                        attrs = psubConfig.getAttributes();
                                    }
                                }
                            }
                        }
                        conf.addSubConfig(name,
                            SCHEMA_OPENSSO_SUBJECT_ATTRIBUTES_COLLECTOR, 0,
                            attrs);
			subConfig = conf.getSubConfig(name);
                    }
                    return subConfig.getAttributes();
                }
            } else {
                PolicyConstants.DEBUG.error(
                "EntitlementService.getSubjectAttributesCollectorConfiguration:"
                    + "admin sso token is absent");
                Object[] arg = {name};
                throw new EntitlementException(287, arg);
            }
        } catch (SMSException ex) {
            PolicyConstants.DEBUG.error(
                "EntitlementService.getSubjectAttributesCollectorConfiguration",
                ex);
            Object[] arg = {name};
            throw new EntitlementException(288, arg, ex);
        } catch (SSOException ex) {
            PolicyConstants.DEBUG.error(
                "EntitlementService.getSubjectAttributesCollectorConfiguration",
                ex);
            Object[] arg = {name};
            throw new EntitlementException(288, arg, ex);
        }
        return null;
    }

    /**
     * Sets subject attributes collector configuration.
     *
     * @param name subject attributes collector name
     * @param attrMap subject attributes collector configuration map.
     * @throws EntitlementException if subject attributes collector
     * configuration cannot be set.
     */
    public void setSubjectAttributesCollectorConfiguration(
        String name, Map<String, Set<String>> attrMap)
        throws EntitlementException {

        try {
            SSOToken token = getSSOToken();

            if (token != null) {
                OrganizationConfigManager ocm = new OrganizationConfigManager(
                    token, realm);
                ServiceConfig orgConfig = ocm.getServiceConfig(SERVICE_NAME);
                if (orgConfig != null) {
                    Set<String> subConfigNames = orgConfig.getSubConfigNames();
                    if ((subConfigNames == null) || (!subConfigNames.contains(
                        CONFIG_SUBJECT_ATTRIBUTES_COLLECTORS))) {
                        orgConfig.addSubConfig(
                            CONFIG_SUBJECT_ATTRIBUTES_COLLECTORS,
                            SCHEMA_SUBJECT_ATTRIBUTES_COLLECTORS, 0,
                            Collections.EMPTY_MAP);
                    }

                    ServiceConfig conf = orgConfig.getSubConfig(
                        CONFIG_SUBJECT_ATTRIBUTES_COLLECTORS);
                    ServiceConfig subConfig = conf.getSubConfig(name);
                    if (subConfig == null) {
                        conf.addSubConfig(name,
                            SCHEMA_OPENSSO_SUBJECT_ATTRIBUTES_COLLECTOR, 0,
                            attrMap);
                    } else {
                        subConfig.setAttributes(attrMap);
                    }
                }
            } else {
                PolicyConstants.DEBUG.error(
                "EntitlementService.setSubjectAttributesCollectorConfiguration:"
                    + "admin sso token is absent");
                Object[] arg = {name};
                throw new EntitlementException(289, arg);
            }
        } catch (SMSException ex) {
            PolicyConstants.DEBUG.error(
                "EntitlementService.setSubjectAttributesCollectorConfiguration",
                ex);
            Object[] arg = {name};
            throw new EntitlementException(290, arg, ex);
        } catch (SSOException ex) {
            PolicyConstants.DEBUG.error(
                "EntitlementService.setSubjectAttributesCollectorConfiguration",
                ex);
            Object[] arg = {name};
            throw new EntitlementException(290, arg, ex);
        }

    }

    /**
     * Returns <code>true</code> if OpenAM policy data is migrated to a
     * form that entitlements service can operates on them.
     *
     * @return <code>true</code> if OpenAM policy data is migrated to a
     * form that entitlements service can operates on them.
     */
    public boolean hasEntitlementDITs() {
        try {
            new ServiceSchemaManager(SERVICE_NAME, EntitlementUtils.getAdminToken());
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

    public boolean networkMonitorEnabled() {
        if (!hasEntitlementDITs()) {
            return false;
        }

        Set<String> setMigrated = getConfiguration(NETWORK_MONITOR_ENABLED);
        String migrated = ((setMigrated != null) && !setMigrated.isEmpty()) ?
            setMigrated.iterator().next() : null;
        
        return (migrated != null) ? Boolean.parseBoolean(migrated) : false;     
    }
    
    public void setNetworkMonitorEnabled(boolean enabled) {
        Set<String> values = new HashSet<String>();
        values.add(Boolean.toString(enabled));
        setConfiguration(EntitlementUtils.getAdminToken(), NETWORK_MONITOR_ENABLED, values);
    }

    public void reindexApplications() {
        Set<Application> appls = getApplications();
        for (Application a : appls) {
            try {
                ApplicationManager.saveApplication(getAdminSubject(), realm, a);
            } catch (EntitlementException ex) {
                //ignore
            }
        }
    }

    public boolean doesRealmExist() {
        try {
            OrganizationConfigManager mgr = new OrganizationConfigManager(
                EntitlementUtils.getAdminToken(), realm);
            return true;
        } catch (SMSException ex) {
            return false;
        }
    }

    public Set<String> getParentAndPeerRealmNames()
        throws EntitlementException {
        Set<String> results = new HashSet<String>();

        try {
            OrganizationConfigManager mgr = new OrganizationConfigManager(
                EntitlementUtils.getAdminToken(), realm);
            mgr = mgr.getParentOrgConfigManager();
            String parentRealm = DNMapper.orgNameToRealmName(
                mgr.getOrganizationName());
            results.add(parentRealm);
            Set<String> orgNames = mgr.getSubOrganizationNames();

            for (String o : orgNames) {
                results.add(DNMapper.orgNameToRealmName(o));
            }
        } catch (SMSException ex) {
            PolicyConstants.DEBUG.error("EntitlementService.getSubRealmNames",
                ex);
            // realm no longer exist
        }
        return results;
    }

    private String getParentRealm(String realm) {
        if (realm.equals("/")) {
            return null;
        }

        int idx = realm.indexOf("/");
        if (idx == -1) {
            return null;
        }

        return (idx == 0) ? "/" : realm.substring(0, idx);
    }

    public String getRealmName(String realm) {
        return DNMapper.orgNameToRealmName(realm);
    }

    /**
     * For the passed in Entitlement environment, update the value associated with the key "am.policy.realmDN".
     *
     * @param environment The Entitlement environment to update with new realm DN value.
     * @param subRealm The Sub Realm whose DN value should be stored in the environment map.
     * @return The existing realm DN value stored in the environment map to enable it to be restored, may be
     *         null if the Policy Configuration for the Sub Realm could not be loaded.
     * @see #restoreEnvironmentRealmDn
     */
    public Set<String> updateEnvironmentRealmDn(Map<String, Set<String>> environment, String subRealm) {

        String orgDN = DNMapper.orgNameToDN(subRealm);
        Map orgConfig = null;
        Set<String> savedRealmDn = null;
        try {
            orgConfig = PolicyConfig.getPolicyConfig(orgDN);
        } catch (PolicyException ex) {
            PolicyConstants.DEBUG.error("EntitlementService.updateEnvironmentRealmDn: "
                    + "can not get policy config for sub-realm : " + subRealm + " org : " + orgDN, ex);
        }
        if (orgConfig != null) {
            /**
             * Save the realm name for the current policy config before passing control down to sub-realm
             */
            savedRealmDn = environment.get(PolicyEvaluator.REALM_DN);
            // Update env to point to the realm policy config data.
            environment.put(PolicyEvaluator.REALM_DN, CollectionUtils.asSet(orgDN));
        }
        
        return savedRealmDn;
    }
    
    /**
     * {@inheritDoc}
     */
    public void restoreEnvironmentRealmDn(Map<String, Set<String>> environment, Set<String> savedRealmDn) {
        environment.put(REALM_DN, savedRealmDn);
    }

    /**
     * Whether the overall monitoring framework is enabled and running.
     *
     * @return true if monitoring is enabled, false otherwise.
     */
    public boolean isMonitoringRunning() {
        return MonitoringUtil.isRunning();
    }

    @Override
    public int getPolicyWindowSize() {
        return MonitoringUtil.getPolicyWindowSize();
    }
}
