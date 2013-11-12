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
 * $Id: AMConfiguration.java,v 1.9 2009/12/23 20:03:04 mrudul_uchil Exp $
 *
 */



package com.sun.identity.authentication.config;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.service.AuthD;
import com.sun.identity.authentication.service.AuthUtils;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.security.auth.login.ConfigFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

/**
 * OpenSSO JAAS Configuration implementation.
 */
public class AMConfiguration extends Configuration {
    
    /**
     * Holds all JAAS configuration, maps configuration name (String) to
     * array of <code>AppConfigurationEntry</code>.
     * TODO : make this a bounded map
     */
    private static Map jaasConfig = new HashMap();
    
    /**
     * Map to hold listeners for a configuration, maps configuration name
     * to a set of Listener object. this is used to remove listeners
     * when config entry is removed from <code>jaasConfig</code>
     * TODO : make this a bounded map.
     */
    private static Map listenersMap = new HashMap();
    
    private static ConfigFile configFile = null;
    
    private static Debug debug = Debug.getInstance("amAuthConfig");
    
    private Configuration defConfig = null;
    private AMAuthenticationManager amAM = null;
    private static ServiceConfigManager scm = null;
    
    /**
     * Constructor.
     * @param config base authentication configuration.
     */
    public AMConfiguration(Configuration config) {
        this.defConfig = config;
    }

    private static SSOToken getAdminToken() {
        SSOToken adminToken = AuthD.getAuth().getSSOAuthSession();
        return adminToken;
    }
    
    /**
     * Initialize JAAS configuration.
     */
    private void initialize() {
        debug.message("inside AMConfiguration.initialize()");
        // initialize config map, this could also be called to
        // refresh the config map
        synchronized (jaasConfig) {
            jaasConfig = new HashMap();
        }
        synchronized (listenersMap) {
            listenersMap = new HashMap();
        }
    }
    
    /**
     * There is a problem here in JAAS or our framework,
     * AppConfigurationEntry[] could not be reused, Auth will hang.
     * This method is used to create a clone copy of given config entry.
     */
    private AppConfigurationEntry[] cloneConfigurationEntry(
        AppConfigurationEntry[] entries,
        String orgDN) {
        if (debug.messageEnabled()) {
            debug.message("AMConfiguration.cloneConfigurationEntry, orgDN=" +
            orgDN + ", entries=" + entries);
        }
        // clone the entry
        List list = new ArrayList();
        // get supported modules for this org
        Set supportedModules = null;
        if (AuthD.revisionNumber < ISAuthConstants.AUTHSERVICE_REVISION7_0) {
            supportedModules = amAM.getAllowedModuleNames();
            if (supportedModules.isEmpty()) {
                return null;
            }
        }
        synchronized (entries) {
            int len = entries.length;
            for (int i = 0; i < len; i++) {
                String tmp = entries[i].getLoginModuleName();
                if (AuthD.revisionNumber<ISAuthConstants.AUTHSERVICE_REVISION7_0
                && !tmp.equals(ISAuthConstants.APPLICATION_MODULE)
                && !supportedModules.contains(
                AMAuthConfigUtils.getModuleName(tmp))) {
                    if (debug.messageEnabled()) {
                        debug.message("skip module " + tmp);
                    }
                    continue;
                }
                list.add(new AppConfigurationEntry(
                entries[i].getLoginModuleName(),
                entries[i].getControlFlag(),
                entries[i].getOptions()));
            }
        }
        int len = list.size();
        if (len == 0) {
            return null;
        }
        // convert list to AppConfigurationEntry[]
        AppConfigurationEntry[] clone = new AppConfigurationEntry[len];
        for (int i = 0; i < len; i++) {
            clone[i] = (AppConfigurationEntry) list.get(i);
        }
        return clone;
    }
    
    /**
     * Returns organization DN from the authentication configuration name.
     *
     * @param configName Configuration Name.
     * @return organization DN.
     */
    private String getOrganization(String configName) {
        return (new AMAuthConfigType(configName)).getOrganization();
    }
    
    /**
     * Creates new configuration entry based on the configuration name
     *
     * @param name Configuration name
     * @return Array of <code>AppConfigurationEntry</code> for the
     *         configuration name.
     */
    private AppConfigurationEntry[] newConfiguration(String name) {
        if (debug.messageEnabled()) {
            debug.message("newConfig, name = " + name);
        }
        // parse the config name
        AMAuthConfigType type = new AMAuthConfigType(name);
        AppConfigurationEntry[] entries = null;
        try {
            switch (type.getIndexType()) {
                case AMAuthConfigType.USER :
                    entries = getUserBasedConfig(type.getOrganization(),
                    type.getIndexName(), name);
                    break;
                case AMAuthConfigType.ORGANIZATION:
                    entries = getOrgBasedConfig(type.getOrganization(), name,
                    false);
                    break;
                case AMAuthConfigType.ROLE :
                    entries = getRoleBasedConfig(type.getOrganization(),
                    type.getIndexName(), name);
                    break;
                case AMAuthConfigType.SERVICE :
                    if (type.getIndexName().equals(ISAuthConstants.
                    CONSOLE_SERVICE)) {
                        entries = getOrgBasedConfig(type.getOrganization(),
                        name, true);
                    } else {
                        entries = getServiceBasedConfig(type.getOrganization(),
                        type.getIndexName(), name);
                    }
                    break;
                case AMAuthConfigType.MODULE :
                    entries = getModuleBasedConfig(type.getOrganization(),
                    type.getIndexName(), name);
                    break;
                default :
                    if (debug.messageEnabled()) {
                        debug.message("Unable to find config " + name +
                        " in OpenSSO config");
                    }
                    // check the default configuration
                    debug.message("Getting default configuration.");
                    if (defConfig != null) {
                        entries = defConfig.getAppConfigurationEntry(name);
                    }
                    
                    if (entries == null) {
                        // try to find it in the ConfigFile,
                        // make configFile static? TBD
                        if (configFile == null) {
                            configFile = new ConfigFile();
                        }
                        debug.message("Getting configuration from confFile.");
                        entries = configFile.getAppConfigurationEntry(name);
                    }
                    
                    if (entries == null) {
                        debug.error("newConfiguration, invalid config " +name);
                    }
                    
                    return entries;
            }
        } catch (Exception e) {
            // could be sso, sdk or sm exception
            debug.error("newConfiguration.switch", e);
        }
        
        if (entries == null) {
            // configuration not defined
            if (debug.messageEnabled()) {
                debug.message("newConfig, config not defined " + name);
            }
            return null;
        }
        
        // add the configuration to the jaas config map
        synchronized (jaasConfig) {
            jaasConfig.put(name, entries);
        }
        
        return cloneConfigurationEntry(entries, type.getOrganization());
    }
   
    /**
     * Returns SM service name based on complete class name.
     *
     * @param name Java Class name for the login module
     * @return Service name for the login module e.g.
     *         <code>iPlanetAMAuthLDAPService</code>.
     */
    private String getServiceNameForModule(String name) {
        // there should be definition for mapping between class name
        // and service name, one optioion is to add the mapping in
        // iplanet-am-auth-authenticators (amAuth.xml)
        // for now just return using existing naming comvention
        // first get the module name based on the class name
        int dot = name.lastIndexOf('.');
        String moduleName;
        if (dot != -1) {
            moduleName = name.substring(dot+1);
        } else {
            // no dot in class name
            moduleName = name;
        }
        return AuthUtils.getModuleServiceName(moduleName);
    }
    
    /**
     * Returns Login Module class name, this method should be provided
     * by <code>AuthenticatorManager</code>.
     *
     * @param module Login Module name, e.g. LDAP
     * @return String class name for the module, e.g.
     *         <code>com.sun.identity.authentication.modules.ldap.LDAP</code>.
     */
    private String getLoginModuleClassName(String module) {
        return AuthD.getAuth().getAuthenticatorForName(module);
    }
    
    /**
     * Returns organization based authentication configuration. This method
     * will read the authenticatin configuration XML from the organization,
     * parse the XML to return the <code>AppConfigurationEntry[]</code>.
     *
     * @param orgDN Organization DN.
     * @param name Authentication configuration name.
     * @param isConsole <code>true</code> if this is for console service.
     * @return Array of <code>AppConfigurationEntry</code>.
     */
    private AppConfigurationEntry[] getOrgBasedConfig(
        String orgDN,
        String name,
        boolean isConsole) {
        if (debug.messageEnabled()) {
            debug.message("getOrgBasedConfig,  START " + orgDN);
        }
        try {
            if (scm == null) {
                synchronized(jaasConfig) {
                    scm = new ServiceConfigManager(
                    ISAuthConstants.AUTH_SERVICE_NAME, getAdminToken());
                }
            }
            ServiceConfig service = scm.getOrganizationConfig(orgDN, null);
            Map attrs = service.getAttributes();
            Set configValues;
            if (isConsole) {
                configValues = (Set)attrs.get(ISAuthConstants.AUTHCONFIG_ADMIN);
            } else {
                configValues = (Set)attrs.get(ISAuthConstants.AUTHCONFIG_ORG);
            }
            String configName = null;
            if (configValues != null) {
                configName = (String)configValues.iterator().next();
            }
            if (debug.messageEnabled()) {
                debug.message("org auth config = " + configName);
            }
            AppConfigurationEntry[] ret =
            parseInstanceConfiguration(orgDN, configName, name);
            addServiceListener("iPlanetAMAuthService", name);
            return ret;
        } catch (Exception e) {
            // got exception, return null config
            debug.error("getOrgBasedConfig org=" + orgDN, e);
            return null;
        }
    }
    
    private AppConfigurationEntry[] parseInstanceConfiguration(
        String orgDN,
        String config,
        String name
    ) throws SMSException, SSOException {
	AppConfigurationEntry[] entries = null;
        String configName = config.trim();
        if (configName == null || configName.length() == 0 ||
        configName.equals(ISAuthConstants.BLANK)) {
            return null;
        }

        if (configName.indexOf("<") != -1) {
            if (debug.messageEnabled()) {
                debug.message("Old DIT with chain config");
            }
            entries = parseXMLConfig(configName, name);
        }
	if ((entries == null) || (entries != null && entries.length == 0)) {
            if (debug.messageEnabled()) {
                debug.message("New DIT with named service config");
            }
            entries = getServiceBasedConfig(orgDN, configName, name);
        }
        return entries;
    }
    
    private AppConfigurationEntry[] parseXMLConfig(
        String xmlConfig,
        String name
    ) throws SMSException, SSOException {
        // parse the auth configuration
        AppConfigurationEntry[] entries =
        AMAuthConfigUtils.parseValues(xmlConfig);
        if (entries == null) {
            return null;
        }
        int len = entries.length;
        // App config entry to return
        AppConfigurationEntry[] ret= new AppConfigurationEntry[len];
        // iterate through each config entry, read corresponding
        // module parameters for the organization
        for (int i = 0; i < len; i++) {
            String className = entries[i].getLoginModuleName();
            int dot = className.lastIndexOf('.');
            String moduleName = className;
            if (dot != -1) {
                moduleName = className.substring(dot + 1);
            }
            AMAuthenticationInstance instance =
            amAM.getAuthenticationInstance(moduleName);
            if (instance == null) {
                return null;
            }
            
            // retrieve all attributes
            Map attribs = instance.getAttributeValues();
            if (attribs == null) {
                return null;
            }
            if (dot == -1) { // className is only an instance name here.
                String type = instance.getType();
                className = getLoginModuleClassName(type);
            }
            // add those user defined options.
            // NOTE : user defined options are key/String value
            //       but our attributes are key/Set of String value
            attribs.putAll(entries[i].getOptions());
            attribs.put(ISAuthConstants.MODULE_INSTANCE_NAME, moduleName);
            // construct AppConfigurationEntry
            ret[i] = new AppConfigurationEntry(className,
            entries[i].getControlFlag(), attribs);
            
            // add listener for this Login module
            addServiceListener(AuthUtils.getModuleServiceName(
            instance.getType()),name);
        }
        
        return ret;
    }
    
    /**
     * Returns user based authentication configuration. This method will read
     * the authentication configuration XML for the user, parse the XML to
     * return the <code>AppConfigurationEntry[]</code>.
     *
     * @param orgDN Organization DN.
     * @param universalId User Universal ID.
     * @param name Authentication configuration name.
     * @return Array of <code>AppConfigurationEntry</code>.
     */
    private AppConfigurationEntry[] getUserBasedConfig(
        String orgDN,
        String universalId,
        String name) {
        if (debug.messageEnabled()) {
            debug.message(
                "getUserBasedConfig,  START " + orgDN + "|" + universalId);
        }
        try {
            AMIdentity identity = IdUtils.getIdentity(getAdminToken(),universalId);
            if (identity != null) {
                Set configNames = identity.getAttribute(
                ISAuthConstants.AUTHCONFIG_USER);
                if ((configNames == null)||(configNames.isEmpty())) {
                    return null;
                }
                String configName = (String)configNames.iterator().next();
                if (debug.messageEnabled()) {
                    debug.message(
                        "Named config for user " + universalId + " = " +
                        configName);
                }
                AppConfigurationEntry[] ret =
                parseInstanceConfiguration(orgDN, configName, name);
                // TODO add user listener for
                return ret;
            } else {
                // user does not exists, return null config
                if (debug.warningEnabled()) {
                    debug.warning(
                        "User Based Config, user not exist " + universalId);
                }
                return null;
            }
        } catch (Exception e) {
            // got exception, return null config
            debug.error("getUserBasedConfig " + universalId + "|" + orgDN, e);
            return null;
        }
    }
    
    /**
     * Returns service based authentication configuration. This method will
     * read the authentication configuration XML for the service, parse the
     * XML to return the <code>AppConfigurationEntry[]</code>.
     *
     * @param orgDN Organization DN.
     * @param service Service name.
     * @param name Authentication configuration name.
     * @return Array of <code>AppConfigurationEntry</code>.
     */
    private AppConfigurationEntry[] getServiceBasedConfig(
        String orgDN,
        String service,
        String name) {
        if (debug.messageEnabled()) {
            debug.message("ServiceBasedConfig,  START " + orgDN +"|"+ service+
            ", name = " + name);
        }
        if (service == null ) {
            return null;
        }
        try {
            Map attributeDataMap = AMAuthConfigUtils.getNamedConfig(
            service, orgDN, getAdminToken());
            
            Set xmlConfigValue = (Set) attributeDataMap.get(
            AMAuthConfigUtils.ATTR_NAME);
            String xmlConfig = null;
            if (xmlConfigValue != null && !xmlConfigValue.isEmpty()) {
                xmlConfig = (String) xmlConfigValue.iterator().next();
            }
            if (xmlConfig == null) {
                // service auth config not defined
                // retrieve organization auth config (??)
                //return getOrgBasedConfig(orgDN);
                // return null now for security concern
                return null;
            }
            
            AppConfigurationEntry[] ret= parseXMLConfig(xmlConfig, name);
            
            if (debug.messageEnabled()) {
                debug.message("serviceBased, add SM listener on " +service);
            }
            addServiceListener("iPlanetAMAuthConfiguration", name);
            
            if (debug.messageEnabled()) {
                debug.message("ServiceBasedConfig, return config " + service +
                ", org=" + orgDN);
            }
            return ret;
        } catch (Exception e) {
            // got exception, return null config
            debug.error("getServiceBasedConfig " + service + "|" + orgDN, e);
            return null;
        }
    }
    
    /**
     * Processes role based authentication configuration. This method will
     * read the auth config xml string for the role, parse the XML string to
     * return the <code>AppConfigurationEntry[]</code>.
     *
     * @param orgDN Organization DN.
     * @param roleUniversalId Universal Id of Role.
     * @param name Auth config name.
     * @return Array of <code>AppConfigurationEntry</code>.
     */
    private AppConfigurationEntry[] getRoleBasedConfig(
        String orgDN,
        String roleUniversalId,
        String name) {
        if (debug.messageEnabled()) {
            debug.message(
                "RoleBasedConfig,  START " + orgDN +"|"+ roleUniversalId);
        }
        try {
            AMIdentity identity =
            IdUtils.getIdentity(getAdminToken(),roleUniversalId);
            if (identity != null) {
                Set configNames = (Set)identity.getServiceAttributes(
                ISAuthConstants.AUTHCONFIG_SERVICE_NAME).get(
                ISAuthConstants.AUTHCONFIG_ROLE);
                if (configNames == null) {
                    return null;
                }
                String configName = (String)configNames.iterator().next();
                if (debug.messageEnabled()) {
                    debug.message(
                        "Named config for role " + roleUniversalId + " = " +
                    configName);
                }
                AppConfigurationEntry[] ret =
                parseInstanceConfiguration(orgDN, configName, name);
                //TODO add listener for role
                return ret;
            } else {
                // role does not exists, return null config
                if (debug.warningEnabled()) {
                    debug.warning(
                        "RoleBaseConfig, role not exist " + roleUniversalId);
                }
                return null;
            }
        } catch (Exception e) {
            // got exception, return null config
            debug.error(
                "getRoleBasedConfig " + orgDN + "|" + roleUniversalId, e);
            return null;
        }
    }
    
    /**
     * Returns module based authentication configuration.
     * This method will read the auth config xml string for the module
     * defined in the specified organization,
     * parse the xml string to return the AppConfigurationEntry[].
     *
     * @param orgDN Organization DN.
     * @param module auth module name.
     * @param name Authentication configuration name.
     * @return module based authentication configuration.
     */
    private AppConfigurationEntry[] getModuleBasedConfig(
        String orgDN,
        String module,
        String name) {
        if (debug.messageEnabled()) {
            debug.message("ModuleBasedConfig,  START " + orgDN +"|"+ module +
            ", name = " + name);
        }
        try {
            AMAuthenticationInstance instance =
            amAM.getAuthenticationInstance(module);
            if (instance == null) {
                return null;
            }
            Map attribs = instance.getAttributeValues();
            attribs.put(ISAuthConstants.MODULE_INSTANCE_NAME, module);
            String type = instance.getType();
            // construct AppConfigurationEntry
            AppConfigurationEntry[] ret = new AppConfigurationEntry[1];
            ret[0] = new AppConfigurationEntry(getLoginModuleClassName(type),
            AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, attribs);
            
            // add SM ServiceListener on module
            addServiceListener(AuthUtils.getModuleServiceName(type), name);
            
            if (debug.messageEnabled()) {
                debug.message("ModuleBaseConfig, return config " + module +
                ", " + orgDN);
            }
            return ret;
        } catch (Exception e) {
            // got exception, return null config
            debug.error("getModuleBasedConfig " + orgDN + "|" + module, e);
            return null;
        }
    }
    
    /**
     * Retrieve an array of <code>AppConfigurationEntries</code> which
     * corresponds to the configuration of <code>LoginModules</code> for this
     * application.
     *
     * @param configName Configuration name used to index the Configuration.
     * @return Array of <code>AppConfigurationEntries</code> which
     *         corresponds to the configuration of <code>LoginModules</code>
     *         for this application, or null if this application has no
     *         configured <code>LoginModules</code>.
     */
    public synchronized AppConfigurationEntry[] getAppConfigurationEntry(String configName) {
        // this function will read corresponding auth configuration for the
        // specified  configName, and retrieve corresponding module instance
        // attributes for the module instance defined in the options field of
        // the auth configuration,  and return those attributes in the
        //getOptions() call of the AppConfigurationEntry instance.
        if (debug.messageEnabled()) {
            debug.message("retrieving configuration: " + configName);
            debug.message("cached configs " + jaasConfig);
        }
        if (configName == null) {
            return null;
        }

        AMAuthConfigType type = new AMAuthConfigType(configName);
        String orgDN = type.getOrganization();

        try {
            amAM = new AMAuthenticationManager(getAdminToken(), orgDN);
        } catch (Exception e) {
            debug.error("Failed to obtain AMAuthenticationManager: " +
                e.getMessage());
            if (debug.messageEnabled()) {
                debug.message("Stack trace: ", e);
            }
            return null;
        }
        
        AppConfigurationEntry[] entry =
            (AppConfigurationEntry[])jaasConfig.get(configName);

        if (entry != null) {
            // already exists in the map
            if (debug.messageEnabled()) {
                debug.message("getAppConfigurationEntry[], found "+configName);
            }
            return cloneConfigurationEntry(entry, getOrganization(configName));
        } else {
            // new configuration
            if (debug.messageEnabled()) {
                debug.message("getAppConfigurationEntry[], new " + configName);
            }
            return newConfiguration(configName);
        }
    }
    
    /**
     * Refreshes and reloads the Configuration.
     */
    public void refresh() {
        this.initialize();
    }
    
    /**
     * Processes listener event, this method will remove configuration from
     * the configuration cache, also remove the listener from the listened
     * object, such as <code>AMUser</code>, <code>AMRole</code>, or SM Service.
     *
     * @param name Configuration name.
     */
    public void processListenerEvent(String name) {
        synchronized (jaasConfig) {
            if (debug.messageEnabled()) {
                debug.message("pLE, remove config " + name);
            }
            jaasConfig.remove(name);
        }
        
        // TODO IdRepo does not have listener support yet.
        //removeListenersMap(name);
    }
    
    /**
     * Removes listeners from the listened object.
     *
     * @param name Configuration name.
     */
    private void removeListenersMap(String name) {
        synchronized (listenersMap) {
            Set set = (Set) listenersMap.get(name);
            if (set == null) {
                if (debug.messageEnabled()) {
                    debug.message("remove, no listeners for " + name);
                }
                return;
            } else {
                Iterator it = set.iterator();
                while (it.hasNext()) {
                    AMSDKEventListener l = (AMSDKEventListener) it.next();
                    if (debug.messageEnabled()) {
                        debug.message("remove SDK listener on " + name +
                        " for dn=" + l.getListenedObject().getDN());
                    }
                    
                    // remove SDK listener for User/Role
                    l.getListenedObject().removeEventListener(l);
                    // clear listened object
                    l.setListenedObject(null);
                } // while
                // remove entry from listeners map
                listenersMap.remove(name);
            } //else
        }
        
        // remove this auth config entry from all the listened services
        AMAuthLevelManager.getInstance().removeAuthConfigListener(name);
    }
    
    /**
     * Adds Service listener for a service.
     *
     * @param service Service name, e.g. <code>iPlanetAMAuthLDAPService</code>.
     * @param name Authentication config name.
     * @throws SMSException
     * @throws SSOException
     */
    private void addServiceListener(String service, String name)
            throws SMSException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("addServiceListener for " + service+", name=" + name);
        }
        AMAuthLevelManager.getInstance().addAuthConfigListener(service, name);
    }
    
    /**
     * Adds listener to listeners Map.
     *
     * @param name Configuration name.
     * @param listener Listener object.
     */
    public void addToListenersMap(String name, Object listener) {
        // put into the sdk listener map
        synchronized (listenersMap) {
            Set set = (Set) listenersMap.get(name);
            if (set == null) {
                set = new HashSet();
                set.add(listener);
                listenersMap.put(name, set);
            } else {
                set.add(listener);
            }
        }
    }
}
