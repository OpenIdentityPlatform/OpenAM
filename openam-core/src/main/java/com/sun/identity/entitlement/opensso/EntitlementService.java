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
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package com.sun.identity.entitlement.opensso;

import static com.sun.identity.entitlement.ApplicationTypeManager.getAppplicationType;
import static com.sun.identity.entitlement.EntitlementException.*;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.entitlement.PolicyConstants.*;
import static org.forgerock.openam.entitlement.SetupInternalNotificationSubscriptions.*;
import static org.forgerock.openam.entitlement.utils.EntitlementUtils.*;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import jakarta.inject.Inject;
import javax.security.auth.Subject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.entitlement.PolicyConstants;
import org.forgerock.openam.entitlement.service.ApplicationQueryFilterVisitor;
import org.forgerock.openam.entitlement.utils.EntitlementUtils;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.openam.notifications.NotificationBroker;
import org.forgerock.util.query.QueryFilter;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.ApplicationType;
import com.sun.identity.entitlement.ApplicationTypeManager;
import com.sun.identity.entitlement.EntitlementConfiguration;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.PolicyEventType;
import com.sun.identity.entitlement.interfaces.ISaveIndex;
import com.sun.identity.entitlement.interfaces.ISearchIndex;
import com.sun.identity.entitlement.interfaces.ResourceName;
import com.sun.identity.monitoring.MonitoringUtil;
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
public class EntitlementService implements EntitlementConfiguration {
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
    private static final String XACML_PRIVILEGE_ENABLED = "xacml-privilege-enabled";
    private static final String REALM_DN_TEMPLATE = "ou={0},ou=default,ou=OrganizationConfig,ou=1.0,ou="
            + SERVICE_NAME + ",ou=services,{1}";

    private final Subject subject;
    private final String realm;
    private final NotificationBroker broker;

    /**
     * Construct a new instance of {@link EntitlementService}.
     *
     * @param subject the calling subject
     * @param realm the realm
     * @param broker the notification broker for notifying the policyset changes
     */
    @Inject
    public EntitlementService(@Assisted Subject subject, @Assisted String realm, NotificationBroker broker) {
        this.subject = subject;
        this.realm = realm;
        this.broker = broker;
    }

    /**
     * Returns set of attribute values of a given attribute name,
     *
     * @param attrName attribute name.
     * @return set of attribute values of a given attribute name,
     */
    @Override
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
    @Override
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
        return (SUPER_ADMIN_SUBJECT.equals(subject)) ?
            EntitlementUtils.getAdminToken() :
            SubjectUtils.getSSOToken(subject);
    }

    @Override
    public Set<Application> searchApplications(Subject subject, QueryFilter<String> queryFilter)
            throws EntitlementException {

        Set<Application> applications = new LinkedHashSet<>();
        ServiceConfig config = getApplicationConfiguration(getSSOToken(subject), realm);
        if (config == null) {
            return applications;
        }

        try {
            Set<String> appNames = config.getSubConfigNames();
            for (String appName : appNames) {
                ServiceConfig appConfig = config.getSubConfig(appName);
                @SuppressWarnings("unchecked")
                Map<String, Set<String>> appData = appConfig.getAttributes();
                if (queryFilter.accept(new ApplicationQueryFilterVisitor(appName), appConfig)) {
                    ApplicationType appType = getAppplicationType(subject, getAttribute(appData, APPLICATION_TYPE));
                    applications.add(EntitlementUtils.createApplication(appType, appName, appData));
                }
            }
        } catch (SMSException | SSOException | InstantiationException | IllegalAccessException e) {
            DEBUG.error("EntitlementService.searchApplications", e);
            throw new EntitlementException(APPLICATION_SEARCH_FAILED, e);
        } catch (UnsupportedOperationException e) {
            DEBUG.error("EntitlementService.searchApplications", e);
            throw new EntitlementException(INVALID_QUERY_FILTER, e);
        }
        return applications;
    }

    private SSOToken getSSOToken(Subject subject) {
        if (SUPER_ADMIN_SUBJECT.equals(subject)) {
            return EntitlementUtils.getAdminToken();
        }
        return SubjectUtils.getSSOToken(subject);
    }

    private static String getApplicationSearchBaseDN(String realm) {
        Object[] args = {EntitlementUtils.REGISTERED_APPLICATIONS, DNMapper.orgNameToDN(realm)};
        return MessageFormat.format(REALM_DN_TEMPLATE, args);
    }

    @Override
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
    @Override
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
                realm = getEntitlementConfigurationRealm(realm);
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
                subject, EntitlementUtils.getAttribute(data, EntitlementUtils.APPLICATION_TYPE));
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
    @Override
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

            Application appl = getApplicationService(SUPER_ADMIN_SUBJECT, realm).getApplication(applicationName);
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

    /**
     * Removes application.
     *
     * @param name name of application to be removed.
     * @throws EntitlementException if application cannot be removed.
     */
    @Override
    public void removeApplication(String name)
        throws EntitlementException
    {
        try {
            ServiceConfig conf = getApplicationCollectionConfig(realm);
            if (conf != null) {
                String[] logParams = {realm, name};
                OpenSSOLogger.log(OpenSSOLogger.LogLevel.MESSAGE, Level.INFO,
                    "ATTEMPT_REMOVE_APPLICATION", logParams, subject);
                conf.removeSubConfig(name);
                OpenSSOLogger.log(OpenSSOLogger.LogLevel.MESSAGE, Level.INFO,
                    "SUCCEEDED_REMOVE_APPLICATION", logParams,
                        subject);

                publishInternalNotifications(name, realm);
            }
        } catch (SMSException ex) {
            String[] logParams = {realm, name, ex.getMessage()};
            OpenSSOLogger.log(OpenSSOLogger.LogLevel.MESSAGE, Level.INFO,
                "FAILED_REMOVE_APPLICATION", logParams, subject);
            Object[] args = {name};
            throw new EntitlementException(EntitlementException.REMOVE_APPLICATION_FAIL, args);
        } catch (SSOException ex) {
            String[] logParams = {realm, name, ex.getMessage()};
            OpenSSOLogger.log(OpenSSOLogger.LogLevel.MESSAGE, Level.INFO,
                "FAILED_REMOVE_APPLICATION", logParams, subject);
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
    @Override
    public void removeApplicationType(String name)
        throws EntitlementException{
        try {
            SSOToken token = SubjectUtils.getSSOToken(subject);

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
        SSOToken token = SubjectUtils.getSSOToken(subject);
        ServiceConfigManager mgr = new ServiceConfigManager(SERVICE_NAME,
            token);
        ServiceConfig orgConfig = mgr.getOrganizationConfig(realm, null);
        if (orgConfig != null) {
            sc = orgConfig.getSubConfig(EntitlementUtils.REGISTERED_APPLICATIONS);
        }

        if (sc == null) {
            orgConfig.addSubConfig(
                    EntitlementUtils.REGISTERED_APPLICATIONS, SCHEMA_APPLICATIONS, 0, Collections.EMPTY_MAP);
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
    @Override
    public void storeApplication(Application appl)
        throws EntitlementException {
        SSOToken token = SubjectUtils.getSSOToken(subject);
        try {
            createApplicationCollectionConfig(realm);
            String dn = getApplicationDN(appl.getName(), realm);
            SMSEntry s = new SMSEntry(token, dn);
            s.setAttributes(getApplicationData(appl));

            String[] logParams = {realm, appl.getName()};
            OpenSSOLogger.log(OpenSSOLogger.LogLevel.MESSAGE, Level.INFO,
                "ATTEMPT_SAVE_APPLICATION", logParams, subject);
            s.save();
            OpenSSOLogger.log(OpenSSOLogger.LogLevel.MESSAGE, Level.INFO,
                "SUCCEEDED_SAVE_APPLICATION", logParams, subject);
            
            publishInternalNotifications(appl.getName(), realm);
        } catch (SMSException ex) {
            String[] logParams = {realm, appl.getName(), ex.getMessage()};
            OpenSSOLogger.log(OpenSSOLogger.LogLevel.ERROR, Level.INFO,
                "FAILED_SAVE_APPLICATION", logParams, subject);
            Object[] arg = {appl.getName()};
            throw new EntitlementException(EntitlementException.MODIFY_APPLICATION_FAIL, arg, ex);
        } catch (SSOException ex) {
            String[] logParams = {realm, appl.getName(), ex.getMessage()};
            OpenSSOLogger.log(OpenSSOLogger.LogLevel.ERROR, Level.INFO,
                "FAILED_SAVE_APPLICATION", logParams, subject);
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
    @Override
    public void storeApplicationType(ApplicationType applicationType)
        throws EntitlementException {
        try {
            SSOToken token = SubjectUtils.getSSOToken(subject);

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

        return map;
    }

    /**
     * Returns subject attribute names.
     *
     * @param application Application name.
     * @return subject attribute names.
     */
    @Override
    public Set<String> getSubjectAttributeNames(String application) {
        try {
            Application app = getApplicationService(SUPER_ADMIN_SUBJECT, realm).getApplication(application);
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
     * Returns subject attributes collector configuration.
     *
     * @param name subject attributes collector name
     * @return subject attributes collector configuration.
     * @throws EntitlementException if subject attributes collector
     * configuration cannot be returned.
     */
    @Override
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
    @Override
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
    @Override
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
     * Returns <code>true</code> if the system stores privileges in
     * XACML format and supports exporting privileges in XACML format
     *
     *
     * @return <code>true</code> if the system stores privileges in
     * XACML format and supports exporting privileges in XACML format
     */
    @Override
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

    @Override
    public boolean networkMonitorEnabled() {
        if (!hasEntitlementDITs()) {
            return false;
        }

        Set<String> setMigrated = getConfiguration(NETWORK_MONITOR_ENABLED);
        String migrated = ((setMigrated != null) && !setMigrated.isEmpty()) ?
            setMigrated.iterator().next() : null;
        
        return (migrated != null) ? Boolean.parseBoolean(migrated) : false;     
    }

    @Override
    public void setNetworkMonitorEnabled(boolean enabled) {
        Set<String> values = new HashSet<String>();
        values.add(Boolean.toString(enabled));
        setConfiguration(EntitlementUtils.getAdminToken(), NETWORK_MONITOR_ENABLED, values);
    }

    @Override
    public void reindexApplications() {
        Set<Application> appls = getApplications();
        for (Application a : appls) {
            try {
                getApplicationService(subject, realm).saveApplication(a);
            } catch (EntitlementException ex) {
                //ignore
            }
        }
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

    /**
     * Whether the overall monitoring framework is enabled and running.
     *
     * @return true if monitoring is enabled, false otherwise.
     */
    @Override
    public boolean isMonitoringRunning() {
        return MonitoringUtil.isRunning();
    }

    @Override
    public int getPolicyWindowSize() {
        return MonitoringUtil.getPolicyWindowSize();
    }

    private void publishInternalNotifications(String policySetName, String realm) {
        JsonValue notification = json(object(
                field(MESSAGE_ATTR_NAME, policySetName),
                field(MESSAGE_ATTR_REALM, realm),
                field(MESSAGE_ATTR_EVENT_TYPE, PolicyEventType.UPDATE)));
        broker.publish(TOPIC_INTERNAL_POLICYSET, notification);
    }
}
