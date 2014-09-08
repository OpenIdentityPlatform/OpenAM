/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AMAuthenticationManager.java,v 1.9 2009/08/05 19:57:27 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted 2011-2014 ForgeRock AS
 * Portions Copyrighted 2014 Nomura Research Institute, Ltd
 */
package com.sun.identity.authentication.config;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.am.util.SystemProperties;
import com.sun.identity.authentication.service.AuthUtils;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.SMSException;

import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This class provides interfaces to manage authentication module instances. 
 */
public class AMAuthenticationManager {

    private static final String BUNDLE_NAME = "amAuthConfig";
    private static final Debug DEBUG = Debug.getInstance(BUNDLE_NAME);
    private static final Set<String> AUTH_TYPES = new HashSet<String>();
    private static final Map<String, String> MODULE_SERVICE_NAMES = new ConcurrentHashMap<String, String>();
    private static final Set<String> GLOBAL_MODULE_NAMES = new HashSet<String>();
    private static final Map<String, Map<String, Set<String>>> MODULE_INSTANCE_TABLE = Collections.synchronizedMap(
            new HashMap<String, Map<String, Set<String>>>());
    private SSOToken token;
    private String realm;
    private ServiceConfig orgServiceConfig;

    static {
        initAuthenticationService();
    }

    /**
     * Constructs an instance of <code>AMAuthenticationManager</code> for the specified realm to manage the
     * authentication module instances available to this realm.
     *
     * @param token Single sign on token of the user identity on whose behalf the operations are performed.
     * @param org The realm in which the module instance management is performed.
     * @throws AMConfigurationException if Service Management related error occurs.
     */
    public AMAuthenticationManager(SSOToken token, String org) throws AMConfigurationException {
        try {
            SMSEntry.validateToken(token);
            this.token = token;
            this.realm = com.sun.identity.sm.DNMapper.orgNameToDN(org);
            if ((this.realm != null) && ((this.realm).length() != 0)) {
                this.realm = (this.realm).toLowerCase();
            }
            orgServiceConfig = getOrgServiceConfig();
            if (orgServiceConfig == null) {
                throw new AMConfigurationException(BUNDLE_NAME, "badRealm",
                new Object[]{realm});
            }
            synchronized (AMAuthenticationManager.class) {
                if (MODULE_INSTANCE_TABLE.get(realm) == null) {
                    buildModuleInstanceTable(token, realm);
                }
            }
        } catch (SMSException e) {
            throw new AMConfigurationException(e);
        } catch (Exception ee) {
            String installTime = SystemProperties.get(
                AdminTokenAction.AMADMIN_MODE);
            if ((installTime != null) && installTime.equalsIgnoreCase("false")){
                DEBUG.error("Token is invalid." , ee);
            }
        }
    }

    /**
     * Re-initializes the module services.
     * This method is meant for global authentication configuration change.
     */
    public static synchronized void reInitializeAuthServices() {
        AUTH_TYPES.clear();
        GLOBAL_MODULE_NAMES.clear();
        initAuthenticationService();
    }

    /**
     * Returns a Set contains all the authentication types that are plugged in
     * this server. 
     * @return Set of String values of the authentication types available on 
     *         this server.
     */
    public static Set<String> getAuthenticationTypes() {
        return AUTH_TYPES;
    }

    /**
     * Returns a Set contains all the module service names that are plugged in
     * this server. 
     * @return Set of String values of the module service names available on 
     *         this server.
     */
    public static Set<String> getAuthenticationServiceNames() {
        Set<String> names = new HashSet<String>(MODULE_SERVICE_NAMES.values());

        if (DEBUG.messageEnabled()) {
            DEBUG.message("Authenticator serviceNames: " + names);
        }
        return names;
    }

    /**
     * Returns authentication service name of a module.
     *
     * @param moduleName Name of authentication module.
     * @return authentication service name of a module.
     */
    public static String getAuthenticationServiceName(String moduleName) {
        return MODULE_SERVICE_NAMES.get(moduleName);
    }

    /**
     * This code makes the authentication type list static. In case the list
     * is expanded or shrinked, the server needs to be restarted.
     */
    private static void initAuthenticationService() {
        SSOToken token = getAdminToken();
        try {
            ServiceSchemaManager scm = new ServiceSchemaManager(ISAuthConstants.AUTH_SERVICE_NAME, token);
            ServiceSchema schema = scm.getGlobalSchema();
            Set<String> authenticators = (Set<String>) schema.getAttributeDefaults().get(
                    ISAuthConstants.AUTHENTICATORS);
            for (String module : authenticators) {
                int index = module.lastIndexOf(".");
                if (index != -1) {
                    module = module.substring(index + 1);
                }
                // Application is not one of the selectable instance type.
                if (!module.equals(ISAuthConstants.APPLICATION_MODULE)) { 
                    AUTH_TYPES.add(module);
                }
                String serviceName = MODULE_SERVICE_NAMES.get(module);
                if (serviceName == null) {
                    serviceName = AuthUtils.getModuleServiceName(module);
                    try {
                        new ServiceSchemaManager(serviceName, token);
                        MODULE_SERVICE_NAMES.put(module, serviceName);
                    } catch (Exception e) {
                        GLOBAL_MODULE_NAMES.add(module);
                        AUTH_TYPES.remove(module);
                    }
                }
            }
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Global module names: " + GLOBAL_MODULE_NAMES);
                DEBUG.message("moduleServiceNames: " + MODULE_SERVICE_NAMES);
            }
       } catch (Exception smse) {
            String installTime = SystemProperties.get(
                AdminTokenAction.AMADMIN_MODE);
            if ((installTime != null) && installTime.equalsIgnoreCase("false")){
                DEBUG.error("Failed to get module types", smse);
            }
        }
    }
    
    /**
     * build the module instance table for the realm.
     * format of this table:
     * Table:  key = realm, value = module Map for the realm.
     * module Map for the realm: 
     *   key = module type, value = Set of module instances
     */
    private static void buildModuleInstanceTable(SSOToken token, String realm) {
        try {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("AMAuthenticationManager." +
                    "buildModuleInstanceTable: realm = " + realm);
            }
            for (String service : MODULE_SERVICE_NAMES.values()) {
                buildModuleInstanceForService(realm, service);
            }
        } catch (Exception e) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("building module instance table error", e);
            }
        }
    }

    /**
     * Updates the static module instance table for the specified service in
     * the realm.
     *
     * @param realm The realm in which the operation is processed.
     * @param serviceName the service for which the table is built.
     */
    public static synchronized void buildModuleInstanceForService(
        String realm,
        String serviceName) {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("start moduleInstanceTable : " + MODULE_INSTANCE_TABLE +
            " for realm : " + realm + " and service : " + serviceName);
        }
        try {
            String moduleName = getModuleName(serviceName);
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Module name : " + moduleName);
            }
            if ((moduleName != null) && (moduleName.length() != 0)) {
                ServiceConfigManager scm = new ServiceConfigManager(serviceName, getAdminToken());
                ServiceConfig config = scm.getOrganizationConfig(realm, null);
                if (config == null) {
                    if (DEBUG.messageEnabled()) {
                        DEBUG.message("AMAuthenticationManager." +
                            "buildModuleInstanceForService: Service="
                            + serviceName + " not configured in realm="+realm);
                    }
                }
                realm = com.sun.identity.sm.DNMapper.orgNameToDN(realm);
                synchronized (MODULE_INSTANCE_TABLE) {
                    Map<String, Set<String>> moduleMap = MODULE_INSTANCE_TABLE.remove(realm);
                    if (moduleMap != null) {
                    /*
                     * this code is to not manipulate the hashmap that might
                     * be in iteration by other threads
                     */
                        Map<String, Set<String>> newMap = new HashMap<String, Set<String>>(moduleMap);
                        newMap.remove(moduleName);
                        moduleMap = newMap;
                    }
                    Set<String> instanceSet = new HashSet<String>();
                    Map<String, Set<String>> defaultAttrs = null;
                    if (config != null) {
                        defaultAttrs = config.getAttributesWithoutDefaults();
                    }
                    if (defaultAttrs != null && !defaultAttrs.isEmpty()) {
                        instanceSet.add(moduleName);
                    }
                    Set<String> instances = null;
                    if (config != null) {
                        instances = config.getSubConfigNames();
                    }
                    if (instances != null) {
                        instanceSet.addAll(instances);
                    }
                    if (!instanceSet.isEmpty()){
                        if (moduleMap == null) {
                            moduleMap = new HashMap<String, Set<String>>();
                        }
                        /*
                         * this operation is safe as moduleMap is a local object
                         * now.
                         */
                        moduleMap.put(moduleName, instanceSet);
                    }
                    if (moduleMap != null && !moduleMap.isEmpty()) {
                        MODULE_INSTANCE_TABLE.put(realm, moduleMap);
                    }
                }
            }
        } catch (Exception e) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("build module instance for service error: " , e);
            }
        }
        if (DEBUG.messageEnabled()) {
            DEBUG.message("return moduleInstanceTable: " + MODULE_INSTANCE_TABLE);
        }
    }
    
    // get the module name from its service name.
    private static String getModuleName(String serviceName) {
        for (String moduleName : MODULE_SERVICE_NAMES.keySet()) {
            if (MODULE_SERVICE_NAMES.get(moduleName).equals(serviceName)) {
                return moduleName;
            }
        }
        return null;
    }

    /**
     * Returns an <code>AMAuthenticationSchema</code> object for the specified
     * authentication type. 
     *
     * @param authType Type of the authentication module instance.
     * @return <code>AMAuthenticationSchema</code> object of the specified
     *         authentication type.
     * @throws AMConfigurationException if error occurred during retrieving
     *         the service schema.
     */
    public AMAuthenticationSchema getAuthenticationSchema(String authType) 
            throws AMConfigurationException {
        return getAuthenticationSchema(authType, token);
    }

    private static AMAuthenticationSchema getAuthenticationSchema(
        String authType, SSOToken token) throws AMConfigurationException {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("getting auth schema for " + authType);
        }
        try {
            ServiceSchema serviceSchema;
            String serviceName = getServiceName(authType);
            ServiceSchemaManager scm = new ServiceSchemaManager(serviceName, 
                token);
            ServiceSchema orgSchema = scm.getOrganizationSchema();
            ServiceSchema subSchema = orgSchema.getSubSchema(
                ISAuthConstants.SERVER_SUBSCHEMA);
            if (subSchema != null) {
                  // using the sub schema in new auth config.
                serviceSchema = subSchema;
            } else {
                // fall back to the org schema if the DIT is old.
                serviceSchema = orgSchema;
            }
            AMAuthenticationSchema amschema = 
                new AMAuthenticationSchema(serviceSchema);
            return amschema;
        } catch (Exception e) {
            throw new AMConfigurationException(e);
        }
    }

    /**
     * Returns the <code>AMAuthenticationInstance</code> object whose name is
     * as specified.
     * Name uniqueness is required for the instances among the same realm, as 
     * well as the instances that are available to this realm.
     *
     * @param authName Authentication instance name.
     * @return The <code>AMAuthenticationInstance</code> object that is
     *         associated with the authentication instance name.
     */
    public AMAuthenticationInstance getAuthenticationInstance(String authName) {
        String type = getAuthInstanceType(authName);
        if (type == null) {
            return null;
        }
        return getAuthenticationInstance(authName, type);
    }

    /**
     * Returns an <code>AMAuthenticationInstance</code> object with the give
     * authentication name and type.
     */
    private AMAuthenticationInstance getAuthenticationInstance(
        String authName,
        String type){
        // for global authentication modules
        if (GLOBAL_MODULE_NAMES.contains(authName)) {
            return new AMAuthenticationInstance(authName, type, null, null);
        }
        String serviceName = getServiceName(type);
        AMAuthenticationInstance instance = null;
        ServiceConfigManager scm = null;
        ServiceSchemaManager ssm = null;
        try {
            ssm = new ServiceSchemaManager( serviceName, token);
        } catch (SMSException e) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Instance type does not exist: " + type);
            }
            return null;
        } catch (SSOException ee) {
            DEBUG.error("SSO token is invalid", ee);
            return null;
        }
        Map globalAttrs = null;
        ServiceSchema schema = null;
        try {
            schema = ssm.getSchema(SchemaType.GLOBAL);
            if (schema != null) {
                globalAttrs = schema.getAttributeDefaults();
            }
        } catch (SMSException e) {
            // normal exception for some schemas without global configuration.
            // no need to log anything.
        }

        Map orgAttrs = null;
        ServiceConfig service = null;
        try {
            scm = new ServiceConfigManager(serviceName, token);
            service = scm.getOrganizationConfig(realm, null);
                    
            if (service != null) {
                if (authName.equals(type)) {
                    orgAttrs = service.getAttributesWithoutDefaults();
                } else {
                    service = service.getSubConfig(authName);
                    if (service != null) {
                          orgAttrs = service.getAttributes();
                    }
                }
            }
        } catch (SSOException e) {
            if (DEBUG.warningEnabled()) {
                DEBUG.warning("Token doesn't have access to service: " +
                   token + " :: " + serviceName); 
            }
        } catch (SMSException e) {
            // normal exception for global service configuration.
            // no need to log anything. 
        }

        if (DEBUG.messageEnabled()) {
            DEBUG.message("global attrs = " + globalAttrs);
            DEBUG.message("org attrs = ");
            if (orgAttrs != null) {
                for (Iterator it=orgAttrs.entrySet().iterator(); it.hasNext();){
                    Map.Entry e = (Map.Entry) it.next();
                    if ((((String)e.getKey()).endsWith("passwd")) ||
                       (((String)e.getKey()).endsWith("Passwd")) ||
                       (((String)e.getKey()).endsWith("password")) ||
                       (((String)e.getKey()).endsWith("Password")) ||
                       (((String)e.getKey()).endsWith("secret"))) {
                        DEBUG.message(e.getKey() + ": " + "<BLOCKED>");
                    }
                    else {
                        DEBUG.message(e.getKey() + ": " + e.getValue());
                    }
                }
            }
        }

        if ((globalAttrs != null && ! globalAttrs.isEmpty()) ||
            (orgAttrs != null && ! orgAttrs.isEmpty())) {
            return new AMAuthenticationInstance(authName, type,service,schema);
        } else {
            return null;
        }
    }

    /**
     * Returns the type of the authentication module instance with the 
     * specified instance name.
     */
    public String getAuthInstanceType(String authName) {
        String returnValue = null;
        if (GLOBAL_MODULE_NAMES.contains(authName)) {
            returnValue = authName;
        } else {
            Map<String, Set<String>> moduleMap = MODULE_INSTANCE_TABLE.get(realm);
            if (moduleMap != null) {
                for (String type : moduleMap.keySet()) {
                    Set<String> instanceNames = moduleMap.get(type);
                    if (instanceNames.contains(authName)) {
                        returnValue = type;
                        break;
                    }
                }
            }
        }
        return returnValue;
    }
    /**
     * Returns a Set of all registered module instance names for a module type,
     * including both the old instances from 6.3 DIT and the new instances
     * in 7.0.
     */
    public Set<String> getModuleInstanceNames(String aModuleType) {
        Set<String> instances = Collections.EMPTY_SET;
        Map<String, Set<String>> moduleMap = MODULE_INSTANCE_TABLE.get(realm);
        if (moduleMap != null || !GLOBAL_MODULE_NAMES.isEmpty()) {
        instances = new HashSet<String>();
            if (moduleMap != null) {
            for (String key : moduleMap.keySet()) {
                if (key.equals(aModuleType)) {
                    instances.addAll(moduleMap.get(key));
                }
            }
            }
        }
        if (DEBUG.messageEnabled()) {
            DEBUG.message("Registered module names: " + instances);
        }
        return instances;
    }


    /**
     * Returns a Set of all registered module instance names, including 
     * both the old instances from 6.3 DIT and the new instances in 7.0. 
     */
    private Set<String> getRegisteredModuleNames() {
        Set<String> instances = Collections.EMPTY_SET;
        Map<String, Set<String>> moduleMap = MODULE_INSTANCE_TABLE.get(realm);
        if (moduleMap != null || !GLOBAL_MODULE_NAMES.isEmpty()) {
            instances = new HashSet<String>();
            if (moduleMap != null) {
                for (String key : moduleMap.keySet()) {
                    instances.addAll(moduleMap.get(key));
                }
            }
            if (!GLOBAL_MODULE_NAMES.isEmpty()){
                instances.addAll(GLOBAL_MODULE_NAMES);
            }
        }
        if (DEBUG.messageEnabled()) {
            DEBUG.message("Registered module names: " + instances);
        }
        return instances;
    }

    /**
     * Returns a Set of module instance names that is allowed for this
     * organization.
     * Since this is only needed for 6.3 and earlier, for 7.0 it returns an
     * empty set.
     * @return a Set of String values for module instance names.
     */
    public Set<String> getAllowedModuleNames() {
        Set<String> retVal;
        if (AuthUtils.getAuthRevisionNumber() >= ISAuthConstants.AUTHSERVICE_REVISION7_0) {
            retVal = getRegisteredModuleNames();
        } else {
            Map<String, Set<String>> attrMap = orgServiceConfig.getAttributes();
            Set<String> defaultModuleNames = attrMap.get(ISAuthConstants.AUTH_ALLOWED_MODULES);
            Set<String> returnSet = Collections.EMPTY_SET;
            if (defaultModuleNames != null && !GLOBAL_MODULE_NAMES.isEmpty()) {
                   returnSet = new HashSet<String>();
                returnSet.addAll(GLOBAL_MODULE_NAMES);
                returnSet.addAll(defaultModuleNames);
            }
            retVal = returnSet;
        }
        if (retVal != null) {
            retVal.remove(ISAuthConstants.APPLICATION_MODULE);
        }
        return retVal;
    }

    /* return true if this module is from 6.3 DIT */
    private boolean isInheritedAuthInstance(String name) {
        Map attrMap = orgServiceConfig.getAttributes();
        Set defaultModuleNames = (Set)attrMap.get(
            ISAuthConstants.AUTH_ALLOWED_MODULES);
        if (defaultModuleNames != null && defaultModuleNames.contains(name)) {
            return true;
        } 
        return false;
    }

    private ServiceConfig getOrgServiceConfig() {
        try {
            ServiceConfigManager scm = new ServiceConfigManager(
                ISAuthConstants.AUTH_SERVICE_NAME, token);
            return scm.getOrganizationConfig(realm, null);
        } catch (Exception e) {
            String installTime = SystemProperties.get(
                AdminTokenAction.AMADMIN_MODE);
            if ((installTime != null) && installTime.equalsIgnoreCase("false")){
                DEBUG.error("Service config for " + realm + " is null." +
                    e.getMessage());
            }
            return null;
        }
    }

    /**
     * Returns the authentication module instances that are available to this 
     * realm except the Application instance which is for internal use only.
     *
     * @return A Set of <code>AMAuthenticationInstance</code> objects that are 
     *         available to this realm.
     */
    public Set<AMAuthenticationInstance> getAuthenticationInstances() {
        Set<AMAuthenticationInstance> instanceSet = Collections.EMPTY_SET;
        Map<String, Set<String>> moduleMap = MODULE_INSTANCE_TABLE.get(realm);
        if (moduleMap != null || !GLOBAL_MODULE_NAMES.isEmpty()) {
            instanceSet = new HashSet<AMAuthenticationInstance>();
            if (!GLOBAL_MODULE_NAMES.isEmpty()) {
                for (String name : GLOBAL_MODULE_NAMES) {
                    if (name.equals(ISAuthConstants.APPLICATION_MODULE)) {
                        continue;
                    }
                    AMAuthenticationInstance instance = getAuthenticationInstance(name, name);
                    if (instance != null) {
                        instanceSet.add(instance);
                    }
                }
            }
            if (moduleMap != null) {
                for (String type : moduleMap.keySet()) {
                    Set<String> instanceNameSet = moduleMap.get(type);
                    for (String name : instanceNameSet) {
                        AMAuthenticationInstance instance = getAuthenticationInstance(name, type);
                        if (instance != null) {
                            instanceSet.add(instance);
                        }
                    }
                }
            }
        }
        return instanceSet;
    }

    /**
     * Creates an <code>AMAuthenticationInstance</code> instance with the
     * specified parameters.
     *
     * @param name Name of the authentication module instance.
     * @param type Type of the authentication module instance.
     * @param attributes A Map of parameters for this module instance.
     * @return <code>AMAuthenticationInstance</code> object is newly created.
     * @throws AMConfigurationException if error occurred during the 
     *         authentication creation.
     */
    public AMAuthenticationInstance createAuthenticationInstance(
        String name, 
        String type,
        Map attributes
    ) throws AMConfigurationException {
        if (name.indexOf(' ') != -1) {
            throw new AMConfigurationException(BUNDLE_NAME,
                "invalidAuthenticationInstanceName", null);
        }
        Set moduleTypes = getAuthenticationTypes();
        if (!moduleTypes.contains(type)) {
            throw new AMConfigurationException(BUNDLE_NAME, "wrongType",
                new Object[]{type} );
        }
        AMAuthenticationInstance instance = getAuthenticationInstance(name);
        if (instance != null) {
            if (instance.getServiceConfig() != null) {
                throw new AMConfigurationException(BUNDLE_NAME,
                    "authInstanceExist", new Object[]{name});
            } else {
                throw new AMConfigurationException(BUNDLE_NAME,
                    "authInstanceIsGlobal", new Object[]{name});
            }
        }

        String serviceName = getServiceName(type);

        ServiceSchema schema = null;
        try {
            ServiceSchemaManager ssm = 
                new ServiceSchemaManager(serviceName, token);
            schema = ssm.getSchema(SchemaType.GLOBAL);
        } catch (SSOException e) {
            if (DEBUG.warningEnabled()) {
                DEBUG.warning("Token doesn't have access to service: " +
                   token + " -> " + serviceName); 
            }
        } catch (SMSException e) {
            // normal exception for service without global configuration.
            // no need to log anything.
        }

        try {
            OrganizationConfigManager ocm = new OrganizationConfigManager(
                token, realm);
            // Check if service is assigned
            if (!ocm.getAssignedServices().contains(serviceName)) {
                ocm.assignService(serviceName, null);
            }
            ServiceConfig orgConfig = ocm.getServiceConfig(serviceName);
            if (orgConfig == null) {
                orgConfig = ocm.addServiceConfig(serviceName, null);
            }
            ServiceConfig subConfig = orgConfig;
            if (!name.equals(type)) {
                orgConfig.addSubConfig(name, ISAuthConstants.SERVER_SUBSCHEMA, 
                    0, attributes);
                subConfig = orgConfig.getSubConfig(name);
            } else {
                // if the module instance name equals to its type, set the
                // the attributes in its organization config, not sub config.
                subConfig.setAttributes(attributes);
            }

            //In case of server mode AMAuthLevelManager will update AMAuthenticationManager about the change, and
            //there is no need to reinitialize the configuration twice. In client mode it is less likely that
            //AMAuthLevelManager listeners are in place, so let's reinitialize to be on the safe side.
            if (!SystemProperties.isServerMode()) {
                buildModuleInstanceForService(realm, serviceName);
            }
            return new AMAuthenticationInstance(name, type, subConfig, schema);
        } catch (Exception e) {
            throw new AMConfigurationException(e);
        }
    }


    /**
     * Deletes a specified authentication module instance.
     * @param name Name of the authentication module instance going to be 
     *                deleted.
     * @throws AMConfigurationException if it fails to delete the 
     *         authentication instance.
     */
    public void deleteAuthenticationInstance(String name) 
        throws AMConfigurationException {
        AMAuthenticationInstance instance = getAuthenticationInstance(name);
        if (instance == null) {
            throw new AMConfigurationException(BUNDLE_NAME,
                "authInstanceNotExist", new Object[] {name});
        }

        if (isModuleInstanceInUse(name)) {
            throw new AMConfigurationException(BUNDLE_NAME,
                "authInstanceInUse", new Object[]{name});
        }
        String type = getAuthInstanceType(name);
        ServiceConfig serviceConfig = instance.getServiceConfig();
        if (serviceConfig == null) {
            throw new AMConfigurationException(BUNDLE_NAME,
                "authInstanceIsGlobal", new Object[] {type});
             }
        try {
            if (name.equals(type)) {
                // no subconfig
                Map attrs = serviceConfig.getAttributesWithoutDefaults();
                if (attrs != null) {
                    serviceConfig.removeAttributes(attrs.keySet());
                }
            } else {
                // remove sub config
                String serviceName = serviceConfig.getServiceName();
                ServiceConfigManager scm = new ServiceConfigManager(
                    serviceName, token);
                ServiceConfig orgConfig = scm.getOrganizationConfig(realm,null);
                orgConfig.removeSubConfig(name);
            }
            if (isInheritedAuthInstance(name)) {
                Set<String> moduleNames = new HashSet<String>();
                moduleNames.add(name);
                orgServiceConfig.removeAttributeValues(
                    ISAuthConstants.AUTH_ALLOWED_MODULES, moduleNames);
            }

            //In case of server mode AMAuthLevelManager will update AMAuthenticationManager about the change, and
            //there is no need to reinitialize the configuration twice. In client mode it is less likely that
            //AMAuthLevelManager listeners are in place, so let's reinitialize to be on the safe side.
            if (!SystemProperties.isServerMode()) {
                buildModuleInstanceForService(realm, serviceConfig.getServiceName());
            }
        } catch (Exception e) {
            throw new AMConfigurationException(e);
        }
    }

    /**
     * Returns <code>true</code> if this authentication module instance editable
     * by this user and/or in this realm.
     *
     * @param instance The authentication module instance.
     * @return <code>true</code> if editable.
     */
    public boolean isEditable(AMAuthenticationInstance instance) {
        return true;
    }

    private static String getServiceName(String module) {
        return MODULE_SERVICE_NAMES.get(module);
    }

    /**
     * Returns <code>true</code> if the module instance with the specified
     * name is being used by any named configurations or not.
     *
     * @param moduleInstance Name of the module instance.
     * @return <code>true</code> if the module instance in use.
     */ 
    private boolean isModuleInstanceInUse(String moduleInstance) {
        Set services = Collections.EMPTY_SET;
        boolean returnValue = false;

        try {
            ServiceConfigManager scm = new ServiceConfigManager(
                ISAuthConstants.AUTHCONFIG_SERVICE_NAME, token);
            ServiceConfig oConfig = scm.getOrganizationConfig(realm, null);

            if (oConfig != null) {
                ServiceConfig namedConfig = 
                    oConfig.getSubConfig("Configurations");
                if (namedConfig != null) { 
                        services = namedConfig.getSubConfigNames("*");
                }
            }
           } catch (Exception e) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Failed to get named sub configurations.");
            }
        }

        for (Iterator it = services.iterator(); it.hasNext(); ) {
            String service = (String)it.next();
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Checking " + service + " ...");
            }
            if (serviceContains(service, moduleInstance)) {
                if (DEBUG.messageEnabled()) {
                    DEBUG.message(moduleInstance + " is used in " + service);
                }
                returnValue = true;
                break;
            } 
        }
        return returnValue;
    }

    /**
     * Checks if the module instance name appears in the named configuration
     * definition.
     * @param serviceName String value for the name of the named configuration.
     * @param moduleInstance String value for the name of the module instance.
     * @return <code>true</code> if the module instance is in the service.
     */
    private boolean serviceContains(String serviceName, String moduleInstance) {
        boolean returnValue = false;
        Map dataMap = null;

        if (serviceName != null) {
            try {
                dataMap = AMAuthConfigUtils.getNamedConfig(serviceName,
                    realm, this.token);
            } catch (Exception e) {
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("Failed to get named sub config attrs.");
                }
            }
        }

        if (dataMap != null) {
            Set xmlConfigValues = (Set)dataMap.get(AMAuthConfigUtils.ATTR_NAME);
            if (xmlConfigValues != null && !xmlConfigValues.isEmpty()) {
                String xmlConfig = (String)xmlConfigValues.iterator().next();
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("service config for " + serviceName + "  = "
                        + xmlConfig);
                } 

                if (xmlConfig != null && xmlConfig.length() != 0) {
                        Document doc = XMLUtils.toDOMDocument(xmlConfig, DEBUG);
                    if (doc != null) {
                        Element vPair = doc.getDocumentElement();
                        Set values = XMLUtils.getAttributeValuePair(vPair);
                        for (Iterator it=values.iterator(); it.hasNext(); ) {
                            String value = (String)it.next();
                            String[] moduleInfo = value.split(" ");
                            if (moduleInfo.length > 0 && moduleInfo[0].equals(moduleInstance)) {
                                returnValue = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return returnValue;
    }

    private static SSOToken getAdminToken() {
        return AccessController.doPrivileged(AdminTokenAction.getInstance());
    }
}
