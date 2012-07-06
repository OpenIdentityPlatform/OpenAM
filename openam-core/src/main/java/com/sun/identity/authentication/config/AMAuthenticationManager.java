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
 * Portions Copyrighted [2011] [ForgeRock AS]
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This class provides interfaces to manage authentication module instances. 
 */
public class AMAuthenticationManager {
    private static String bundleName;
    private static Debug debug;
    private static Set authTypes;
    private static Map moduleServiceNames;
    private static Set globalModuleNames;

    private SSOToken token;
    private String realm;
    private ServiceConfig orgServiceConfig;
    private static Hashtable moduleInstanceTable;
    private static SSOToken adminToken;

    static {
        adminToken = (SSOToken)AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        authTypes = new HashSet();
        moduleServiceNames = new HashMap();
        bundleName = "amAuthConfig";
        debug = Debug.getInstance(bundleName);
        moduleInstanceTable = new Hashtable();
        globalModuleNames = Collections.EMPTY_SET;
        initAuthenticationService(adminToken);
    }

    /**
     * Constructs an instance of <code>AMAuthenticationManager</code> for the
     * specified realm to manage the authentication module instances available 
     * to this realm. 
     *
     * @param token Single sign on token of the user identity on whose behalf
     *        the operations are performed.
     * @param org The realm in which the module instance management is 
     *              performed.
     * @throws AMConfigrationException if Service Management related error
     *         occurs.
     */
    public AMAuthenticationManager(SSOToken token, String org)
        throws AMConfigurationException {
        try {
            SMSEntry.validateToken(token);
            this.token = token;
            this.realm = com.sun.identity.sm.DNMapper.orgNameToDN(org);
            if ((this.realm != null) && ((this.realm).length() != 0)) {
                this.realm = (this.realm).toLowerCase();
            }
            orgServiceConfig = getOrgServiceConfig();
            if (orgServiceConfig == null) {
                throw new AMConfigurationException(bundleName, "badRealm",
                new Object[]{realm});
            }
            synchronized (AMAuthenticationManager.class) {
                if (moduleInstanceTable.get(realm) == null) {
                           buildModuleInstanceTable(token, realm);
                }
            }
        } catch (SMSException e) {
            throw new AMConfigurationException(e);
        } catch (Exception ee) {
            String installTime = SystemProperties.get(
                AdminTokenAction.AMADMIN_MODE);
            if ((installTime != null) && installTime.equalsIgnoreCase("false")){
                debug.error("Token is invalid." , ee);
            }
        }
    }

    /**
     * Re-initializes the module services.
     * This method is meant for global authentication configuration change.
     */
    public static synchronized void reInitializeAuthServices() {
        authTypes.clear();
        if (globalModuleNames != Collections.EMPTY_SET) {
            globalModuleNames.clear();
        }
        initAuthenticationService(adminToken);
    }

    /**
     * Returns a Set contains all the authentication types that are plugged in
     * this server. 
     * @return Set of String values of the authentication types available on 
     *         this server.
     */
    public static Set getAuthenticationTypes() {
        return authTypes;
    }

    /**
     * Returns a Set contains all the module service names that are plugged in
     * this server. 
     * @return Set of String values of the module service names available on 
     *         this server.
     */
    public static Set getAuthenticationServiceNames() {
        Collection keys = moduleServiceNames.values();
        Set names = (keys != null) ? new HashSet(keys) : Collections.EMPTY_SET;

        if (debug.messageEnabled()) {
            debug.message("Authenticator serviceNames: " + names);
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
        return (String)moduleServiceNames.get(moduleName);
    }

    /**
     * This code makes the authentication type list static. In case the list
     * is expanded or shrinked, the server needs to be restarted.
     */
    private static void initAuthenticationService(SSOToken token) {
        try {
            ServiceSchemaManager scm = new ServiceSchemaManager(
                ISAuthConstants.AUTH_SERVICE_NAME, token);
            ServiceSchema schema = scm.getGlobalSchema();
            Set authenticators = (Set)schema.getAttributeDefaults().get(
                ISAuthConstants.AUTHENTICATORS);
            for (Iterator it = authenticators.iterator(); it.hasNext(); ) {
                String module = (String)it.next();
                int index = module.lastIndexOf(".");
                if (index != -1) {
                    module = module.substring(index + 1);
                }
                   // Application is not one of the selectable instance type.
                if (!module.equals(ISAuthConstants.APPLICATION_MODULE)) { 
                    authTypes.add(module);
                }
                String serviceName = (String)moduleServiceNames.get(module);
                if (serviceName == null) {
                    serviceName = AuthUtils.getModuleServiceName(module);
                    try {
                        new ServiceSchemaManager(serviceName, token);
                        synchronized(moduleServiceNames) {
                            Map newMap = new HashMap(moduleServiceNames);
                            newMap.put(module, serviceName);
                            moduleServiceNames = newMap;
                        }
                    } catch (Exception e) {
                        if (globalModuleNames == Collections.EMPTY_SET) {
                            globalModuleNames = new HashSet();
                        }
                        globalModuleNames.add(module);
                        authTypes.remove(module);
                        continue;
                    }
                }
            }
            if (debug.messageEnabled()) {
                debug.message("Global module names: " + globalModuleNames);
                debug.message("moduleServiceNames: " + moduleServiceNames);
            }
       } catch (Exception smse) {
            String installTime = SystemProperties.get(
                AdminTokenAction.AMADMIN_MODE);
            if ((installTime != null) && installTime.equalsIgnoreCase("false")){
                debug.error("Failed to get module types", smse);
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
            if (debug.messageEnabled()) {
                debug.message("AMAuthenticationManager." +
                    "buildModuleInstanceTable: realm = " + realm);
            }
            Collection authServiceNames = moduleServiceNames.values();
            for (Iterator it = authServiceNames.iterator(); it.hasNext(); ) {
                String service = (String) it.next();
                buildModuleInstanceForService(realm, service);
            }
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("building module instance table error", e);
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
        if (debug.messageEnabled()) {
            debug.message("start moduleInstanceTable : " + moduleInstanceTable +
            " for realm : " + realm + " and service : " + serviceName);
        }
        try {
            String moduleName = getModuleName(serviceName);
            if (debug.messageEnabled()) {
                debug.message("Module name : " + moduleName);
            }
            if ((moduleName != null) && (moduleName.length() != 0)) {
                ServiceConfigManager scm = new ServiceConfigManager(
                    serviceName, adminToken);
                ServiceConfig config = scm.getOrganizationConfig(realm, null);
                if (config == null) {
                    if (debug.messageEnabled()) {
                        debug.message("AMAuthenticationManager." +
                            "buildModuleInstanceForService: Service="
                            + serviceName + " not configured in realm="+realm);
                    }
                }
                realm = com.sun.identity.sm.DNMapper.orgNameToDN(realm);
                synchronized (moduleInstanceTable) {
                    Map moduleMap = (Map)moduleInstanceTable.remove(realm);
                    if (moduleMap != null) {
                    /*
                     * this code is to not manipulate the hashmap that might
                     * be in iteration by other threads
                     */
                        Map newMap = new HashMap(moduleMap);
                        newMap.remove(moduleName);
                        moduleMap = newMap;
                    }
                    Set instanceSet = new HashSet();
                    Map defaultAttrs = null;
                    if (config != null) {
                        defaultAttrs = config.getAttributesWithoutDefaults();
                    }
                    if (defaultAttrs != null && !defaultAttrs.isEmpty()) {
                        instanceSet.add(moduleName);
                    }
                    Set instances = null;
                    if (config != null) {
                        instances = config.getSubConfigNames();
                    }
                    if (instances != null) {
                        instanceSet.addAll(instances);
                    }
                    if (!instanceSet.isEmpty()){
                        if (moduleMap == null) {
                            moduleMap = new HashMap();
                        }
                        /*
                         * this operation is safe as moduleMap is a local object
                         * now.
                         */
                        moduleMap.put(moduleName, instanceSet);
                    }
                    if (moduleMap != null && !moduleMap.isEmpty()) {
                        moduleInstanceTable.put(realm, moduleMap);
                    }
                }
            }
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("build module instance for service error: " , e);
            }
        }
        if (debug.messageEnabled()) {
            debug.message("return moduleInstanceTable: " + moduleInstanceTable);
        }
    }
    
    // get the module name from its service name.
    private static String getModuleName(String serviceName) {
        for (Iterator it=moduleServiceNames.keySet().iterator();it.hasNext();){
            String moduleName = (String) it.next();
            if (moduleServiceNames.get(moduleName).equals(serviceName)) {
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
        if (debug.messageEnabled()) {
            debug.message("getting auth schema for " + authType);
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
        if (globalModuleNames.contains(authName)) {
            return new AMAuthenticationInstance(authName, type, null, null);
        }
        String serviceName = getServiceName(type);
        AMAuthenticationInstance instance = null;
        ServiceConfigManager scm = null;
        ServiceSchemaManager ssm = null;
        try {
            ssm = new ServiceSchemaManager( serviceName, token);
        } catch (SMSException e) {
            if (debug.messageEnabled()) {
                debug.message("Instance type does not exist: " + type);
            }
            return null;
        } catch (SSOException ee) {
            debug.error("SSO token is invalid", ee);
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
            if (debug.warningEnabled()) {
                debug.warning("Token doesn't have access to service: " +
                   token + " :: " + serviceName); 
            }
        } catch (SMSException e) {
            // normal exception for global service configuration.
            // no need to log anything. 
        }

        if (debug.messageEnabled()) {
            debug.message("global attrs = " + globalAttrs);
            debug.message("org attrs = ");
            if (orgAttrs != null) {
                for (Iterator it=orgAttrs.entrySet().iterator(); it.hasNext();){
                    Map.Entry e = (Map.Entry) it.next();
                    if ((((String)e.getKey()).endsWith("passwd")) ||
                       (((String)e.getKey()).endsWith("Passwd")) ||
                       (((String)e.getKey()).endsWith("password")) ||
                       (((String)e.getKey()).endsWith("Password")) ||
                       (((String)e.getKey()).endsWith("secret"))) {
                        debug.message(e.getKey() + ": " + "<BLOCKED>");
                    }
                    else {
                        debug.message(e.getKey() + ": " + e.getValue());
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
    private String getAuthInstanceType(String authName) {
        String returnValue = null;
        if (globalModuleNames.contains(authName)) {
            returnValue = authName;
        } else {
            Map moduleMap = (Map)moduleInstanceTable.get(realm);
            if (moduleMap != null) {
                for (Iterator types = moduleMap.keySet().iterator(); 
                    types.hasNext();) {
                        String type = (String) types.next();
                        Set instanceNames = (Set)moduleMap.get(type);
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
    public Set getModuleInstanceNames(String aModuleType) {
        Set instances = Collections.EMPTY_SET;
        Map moduleMap = (Map)moduleInstanceTable.get(realm);
        if (moduleMap != null || !globalModuleNames.isEmpty()) {
        instances = new HashSet();
            if (moduleMap != null) {
                for (Iterator keys = moduleMap.keySet().iterator();
                    keys.hasNext(); ) {
                    String key = (String)keys.next();
                    if (key.equals(aModuleType)) {
                        instances.addAll((Set)moduleMap.get(key));
                    }
                }
            }
        }
        if (debug.messageEnabled()) {
            debug.message("Registered module names: " + instances);
        }
        return instances;
    }


    /**
     * Returns a Set of all registered module instance names, including 
     * both the old instances from 6.3 DIT and the new instances in 7.0. 
     */
    private Set getRegisteredModuleNames() {
        Set instances = Collections.EMPTY_SET;
        Map moduleMap = (Map)moduleInstanceTable.get(realm);
        if (moduleMap != null || !globalModuleNames.isEmpty()) {
            instances = new HashSet();
            if (moduleMap != null) {
                for (Iterator keys = moduleMap.keySet().iterator(); 
                    keys.hasNext(); ) {
                    String key = (String)keys.next();
                    instances.addAll((Set)moduleMap.get(key));
                }
            }
            if (!globalModuleNames.isEmpty()){
                instances.addAll(globalModuleNames);
            }
        }
        if (debug.messageEnabled()) {
            debug.message("Registered module names: " + instances);
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
    public Set getAllowedModuleNames() {
        Set retVal = null;
        if (AuthUtils.getAuthRevisionNumber() 
            >= ISAuthConstants.AUTHSERVICE_REVISION7_0) {
            retVal = getRegisteredModuleNames();
        } else {
            Map attrMap = orgServiceConfig.getAttributes();
            Set defaultModuleNames = (Set)attrMap.get(
                ISAuthConstants.AUTH_ALLOWED_MODULES);
            Set returnSet = Collections.EMPTY_SET;
            if (defaultModuleNames != null && !globalModuleNames.isEmpty()) {
                   returnSet = new HashSet();
                returnSet.addAll(globalModuleNames);
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
                debug.error("Service config for " + realm + " is null." + 
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
    public Set getAuthenticationInstances() {
        Set instanceSet = Collections.EMPTY_SET;
        Map moduleMap = (Map)moduleInstanceTable.get(realm);
        if (moduleMap != null || !globalModuleNames.isEmpty()) {
            instanceSet = new HashSet();
            if (!globalModuleNames.isEmpty()) {
                for (Iterator names = globalModuleNames.iterator(); 
                    names.hasNext();) {
                    String name = (String)names.next();
                        if (name.equals(ISAuthConstants.APPLICATION_MODULE)) {
                        continue;
                        }
                    AMAuthenticationInstance instance = 
                        getAuthenticationInstance(name, name);
                    if (instance != null) {
                        instanceSet.add(instance);
                    }
                }
            }
            if (moduleMap != null) {
                for (Iterator types = moduleMap.keySet().iterator(); 
                    types.hasNext(); ) {
                        String type = (String) types.next();
                        Set instanceNameSet = (Set)moduleMap.get(type);
                        for (Iterator names = instanceNameSet.iterator(); 
                        names.hasNext(); ){
                          String name = (String)names.next();
                        AMAuthenticationInstance instance = 
                            getAuthenticationInstance(name, type);
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
            throw new AMConfigurationException(bundleName,
                "invalidAuthenticationInstanceName", null);
        }
        Set moduleTypes = getAuthenticationTypes();
        if (!moduleTypes.contains(type)) {
            throw new AMConfigurationException(bundleName, "wrongType",
                new Object[]{type} );
        }
        AMAuthenticationInstance instance = getAuthenticationInstance(name);
        if (instance != null) {
            if (instance.getServiceConfig() != null) {
                throw new AMConfigurationException(bundleName, 
                    "authInstanceExist", new Object[]{name});
            } else {
                throw new AMConfigurationException(bundleName, 
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
            if (debug.warningEnabled()) {
                debug.warning("Token doesn't have access to service: " +
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
            throw new AMConfigurationException(bundleName, 
                "authInstanceNotExist", new Object[] {name});
        }

        if (isModuleInstanceInUse(name)) {
            throw new AMConfigurationException(bundleName,
                "authInstanceInUse", new Object[]{name});
        }
        String type = getAuthInstanceType(name);
        ServiceConfig serviceConfig = instance.getServiceConfig();
        if (serviceConfig == null) {
            throw new AMConfigurationException(bundleName, 
                "authInstanceIsGloal", new Object[] {type});
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
                Set moduleNames = new HashSet();
                moduleNames.add(name);
                orgServiceConfig.removeAttributeValues(
                    ISAuthConstants.AUTH_ALLOWED_MODULES, moduleNames);
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
        return (String)moduleServiceNames.get(module);
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
            if (debug.messageEnabled()) {
                debug.message("Failed to get named sub configurations.");
            }
        }

        for (Iterator it = services.iterator(); it.hasNext(); ) {
            String service = (String)it.next();
            if (debug.messageEnabled()) {
                debug.message("Checking " + service + " ...");
            }
            if (serviceContains(service, moduleInstance)) {
                if (debug.messageEnabled()) {
                    debug.message(moduleInstance + " is used in " + service);
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
     */
    private boolean serviceContains(String serviceName, String moduleInstance) {
        boolean returnValue = false;
        Map dataMap = null;

        if (serviceName != null) {
            try {
                dataMap = AMAuthConfigUtils.getNamedConfig(serviceName,
                    realm, this.token);
            } catch (Exception e) {
                if (debug.messageEnabled()) {
                    debug.message("Failed to get named sub config attrs.");
                }
            }
        }

        if (dataMap != null) {
            Set xmlConfigValues = (Set)dataMap.get(AMAuthConfigUtils.ATTR_NAME);
            if (xmlConfigValues != null && !xmlConfigValues.isEmpty()) {
                String xmlConfig = (String)xmlConfigValues.iterator().next();
                if (debug.messageEnabled()) {
                    debug.message("service config for " + serviceName + "  = "
                        + xmlConfig);
                } 

                if (xmlConfig != null && xmlConfig.length() != 0) {
                        Document doc = XMLUtils.toDOMDocument(xmlConfig, debug);
                    if (doc != null) {
                        Element vPair = doc.getDocumentElement();
                        Set values = XMLUtils.getAttributeValuePair(vPair);
                        for (Iterator it=values.iterator(); it.hasNext(); ) {
                            String value = (String)it.next();
                            if (value.startsWith(moduleInstance)) {
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
}
