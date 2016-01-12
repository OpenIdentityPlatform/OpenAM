/*
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
 * $Id: AMAuthLevelManager.java,v 1.3 2008/06/25 05:41:51 qcheng Exp $
 *
 * Portions Copyrighted 2012-2016 ForgeRock AS.
 */

package com.sun.identity.authentication.config;

import static java.util.Collections.singleton;
import static java.util.Collections.synchronizedMap;

import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.service.AuthD;
import com.sun.identity.authentication.service.AuthUtils;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import com.sun.identity.sm.ServiceNotFoundException;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

import javax.security.auth.login.Configuration;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Manager for module authentication level, this class provides methods to 
 * retrieve modules which satisfied specific authentication level requirement.
 * It keeps a cache copy of all module authentication level for all
 * organizations, and implements <code>ServiceListener</code> so the cache
 * could be updated when changes happened.
 */
public class AMAuthLevelManager implements ServiceListener {
    // instance
    private static volatile AMAuthLevelManager instance = null;

    /**
     * listener Map for the auth modules, key is the module name,
     * value is a ListenerMapEntry which contain the <code>ServiceSchemaManager</code>,
     * listener ID, <code>ServiceConfigmanager</code> and listener ID.
     */
    private final Map<String, ListenerMapEntry> listenerMap = synchronizedMap(new HashMap<String, ListenerMapEntry>());

    /**
     * Map to hold authentication level for all organizations. Map of
     * organization DN to a map of authentication module name (String) to
     * module authentication level(Integer).
     */
    private static final ConcurrentMap<String, Map<String, Integer>> authLevelMap = new ConcurrentHashMap<>();

    /**
     * Map from service name to module name.
     */
    private static final ConcurrentMap<String, String> moduleServiceMap = new ConcurrentHashMap<>();

    /**
     * Map from global module name to auth level.
     */
    private static final Map<String, Integer> globalAuthLevelMap = new ConcurrentHashMap<>();

    /**
     * Map of service name to authentication config name. This is the map to
     * register all auth configuration names which are affected by the service
     * changes. Upon notification on the service change, the listened
     * authentication configuration need to be checked.
     */
    private static final Map<String, Set<String>> authConfigListenerMap =
            synchronizedMap(new HashMap<String, Set<String>>());

    private static final String CORE_AUTH = "iPlanetAMAuthService";

    private static final Debug debug = Debug.getInstance("amAuthConfig");

    /**
     * Constructor
     */
    private AMAuthLevelManager() {
        initialize();
    }

    /**
     * Returns manager instance.
     *
     * @return <code>AMAuthLevelManager</code>.
     */
    public static AMAuthLevelManager getInstance() {
        if (instance == null) {
            synchronized (AMAuthLevelManager.class) {
                if (instance == null) {
                    instance = new AMAuthLevelManager();
                }
            }
        }    
        return instance;
    } 

    private void registerListener(String serviceName, Map<String, ListenerMapEntry> newMap) {
        // register listener for the specified service 
        // check if the listener for the service is registered already
        ListenerMapEntry entry = listenerMap.remove(serviceName);
        if (entry != null) {
            if (debug.messageEnabled()) {
                debug.message("initialize, existing " + serviceName);
            }
            newMap.put(serviceName, entry);
        } else {
            // create new listener
            try {
                entry = addServiceListener(serviceName);
                if (entry != null) {
                    newMap.put(serviceName, entry);
                }
            } catch (Exception e) {
                debug.error("can't add listener for " + serviceName, e);
            }
        }
    }

    /**
     * Reads the <code>iplanet-am-auth-authenticators</code> attribute.
     * Adds listener to <code>iPlanetAMAuthService</code>,
     * <code>iPlanetAMAuthConfiguration</code> and all login modules.
     */
    private synchronized void initialize() {
        final Map<String, ListenerMapEntry> newMap = new HashMap<>();
        // register listener for iPlanetAMAuthService
        registerListener(CORE_AUTH, newMap);

        // register listener for iPlanetAMAuthConfiguration
        registerListener(AMAuthConfigUtils.SERVICE_NAME, newMap);
 
        // get All auth modules
        Iterator it = AuthD.getAuth().getAuthenticators(); 

        // register all listeners from it
        if (it != null) {
            while (it.hasNext()) {
                String moduleName = (String) it.next();
                String moduleServiceName = 
                    AuthUtils.getModuleServiceName(moduleName);
                
                // check if the listener for the module is registered already
                ListenerMapEntry entry = listenerMap.remove(moduleName);
                if (entry != null) {
                    if (debug.messageEnabled()) {
                        debug.message("initialize, existing " + moduleName);
                    }
                    newMap.put(moduleName, entry);
                } else {
                    // create new listener
                    try {
                        entry = addServiceListener(moduleServiceName);
                        if (entry != null) {
                            newMap.put(moduleName, entry);
                        }
                    } catch (Exception e) {
                        // this is OK since some modules might not have
                        // xml config defined
                        if (debug.messageEnabled()) {
                            debug.message("authlevel, add service listener," +
                                e.getMessage());
                        }
                    }
                }
                moduleServiceMap.putIfAbsent(moduleServiceName, moduleName);
                // get organization schema auth level for module
                updateGlobalAuthLevelMap(moduleServiceName);
            }

        }

        // remove listeners remains in listenerMap : module removed
        if (!listenerMap.isEmpty()) {
            for (ListenerMapEntry entry : listenerMap.values()) {
                entry.removeListeners();
            }
        }

        // reassign map
        synchronized (listenerMap) {
            listenerMap.clear();
            listenerMap.putAll(newMap);
        }
    }

    private ListenerMapEntry addServiceListener(String service) throws SMSException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("addServiceListener for " + service);
        }
        // add Service Schema Listener 
        ServiceSchemaManager ssm = null;
        try {
            ssm = new ServiceSchemaManager(service, AuthD.getAuth().getSSOAuthSession());
        } catch (ServiceNotFoundException e) {
            // service not defined, this is OK, since Application/Cert
            // module does not define any xml file
            return null;
        }
        String schemaListenerId = ssm.addListener(this);
        // add Service Config Manager
        ServiceConfigManager scm = null;
        try {
            scm = new ServiceConfigManager(service,
                AuthD.getAuth().getSSOAuthSession());
        } catch (ServiceNotFoundException e) {
            // service not defined, this is OK, since Application/Cert
            // module does not define any xml file
            return null;
        }
        String configListenerId = scm.addListener(this);

        return new ListenerMapEntry(ssm, schemaListenerId, scm, configListenerId);
    }

    /**
     * Returns modules whose authentication level is equals or bigger than
     * the authentication level specified, am empty set will be returned
     * if organization does not exist, or no matching authentication level
     * could be found.
     *
     * @param level Authentication level.
     * @param orgDN Organization DN.
     * @param clientType Client Type.
     * @return Set which contains module names, e.g. <code>LDAP, Cert,
     *         RADIUS</code>.
     */
    public Set<String> getModulesForLevel(int level, String orgDN, String clientType) {
        Map<String, Integer> map = authLevelMap.get(orgDN);
        if (map == null) {
            map = initOrgAuthLevel(orgDN);
        }
        if (map == null || map.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> set = getModuleForLevel(level, map);
        if (debug.messageEnabled()) {
            debug.message("getModuleForLevel " + level + ", org=" + orgDN +
                ", modules=" + set);
        }
        
        if (debug.messageEnabled()) {
            debug.message("getModuleForLevel, modules=" + set);
        }
        return set;
    }

    private Map<String, Integer> initOrgAuthLevel(String orgDN) {
        // new map contains the module to auth level mapping
        Map<String, Integer> map = new HashMap<>();
        Set<String> allowedModules;
        AMAuthenticationManager manager = null;
        try {
            // get all enabled auth modules for this org
            manager = new AMAuthenticationManager(AuthD.getAuth().getSSOAuthSession(), orgDN);
            allowedModules = manager.getAllowedModuleNames();
        } catch (Exception e) {
            debug.error("initOrgAuthLevel " + orgDN, e);
            return map;
        }

        if (!allowedModules.isEmpty()) {
            for (final String module : allowedModules) {
                if (debug.messageEnabled()) {
                    debug.message("initOrgAuthLevel process " + module);
                }
                final AMAuthenticationInstance instance = manager.getAuthenticationInstance(module);

                if (instance == null) {
                    continue;
                }
                // get the auth level attribute
                Map attrs = instance.getAttributeValues();
                String attrName = AMAuthConfigUtils.getAuthLevelAttribute(attrs, instance.getType());
                String authLevel = CollectionHelper.getMapAttr(attrs, attrName);
                Integer level = null;
                if (authLevel != null && authLevel.length() != 0) {
                    try {
                        level = Integer.valueOf(authLevel);
                    } catch (Exception e) {
                        debug.error("initOrgAuthLevel, invalid level", e);
                    }
                }

                if (debug.messageEnabled()) {
                    debug.message("globalAuthLevel MAP " + globalAuthLevelMap);
                    debug.message("initOrgAuthLevel add " + module);
                    debug.message("level is... " + level);
                }

                // add the mapping to the map
                if (level != null) {
                    map.put(module, level);
                }
            }
        }

        // add to the authLevelMap
        Map<String, Integer> previousMap = authLevelMap.putIfAbsent(orgDN, map);
        if (previousMap != null) {
            // We lost the race
            map = previousMap;
        }

        return map;
    }

    private Set<String> getModuleForLevel(int level, Map<String, Integer> map) {
        Set<String> set = new HashSet<>();

        for (final Map.Entry<String, Integer> entry : map.entrySet()) {
            if (entry.getValue() >= level) {
                set.add(entry.getKey());
            }
        }
        return set;
    } 

    /**
     * Implements methods in <code>com.sun.identity.sm.ServiceListener</code>
     *
     * @param serviceName
     * @param version
     * @param groupName
     * @param serviceComponent
     * @param type
     */
    public void globalConfigChanged(
        String serviceName,
        String version,
        String groupName,
        String serviceComponent,
        int type) {
        if (debug.messageEnabled()) {
            debug.message("authlevel : globalConfigChanged " + serviceName + 
                ", ver=" + version + ", group=" + groupName + 
                ", componnet=" + serviceComponent +
                ", type=" + type);
        }
        if (serviceName.equals(ISAuthConstants.AUTH_SERVICE_NAME)) {
            AMAuthenticationManager.reInitializeAuthServices();
        }
    }

    /**
     * Implements methods in <code>com.sun.identity.sm.ServiceListener</code>.
     *
     * @param serviceName
     * @param version
     * @param orgName
     * @param groupName
     * @param serviceComponent
     * @param type
     */
    public void organizationConfigChanged(
        String serviceName,
        String version,
        String orgName,
        String groupName,
        String serviceComponent,
        int type) {
        if (debug.messageEnabled()) {
            debug.message("authlevel : orgConfigChanged " + serviceName + 
                ", ver=" + version + ", org=" + orgName + ", group=" +
                groupName + ", componnet=" + serviceComponent + 
                ", type=" + type);
        }

        // update auth level map for the org
        authLevelMap.remove(orgName);

        // this listener event should be conditioned only for ADDED and REMOVED. SM will provide special MODIFIED type
        // for removal of all attributes(for the default instance)
        AMAuthenticationManager.updateModuleInstanceTable(orgName, serviceName);
        // process auth config updates
        updateAuthConfiguration(serviceName, orgName, serviceComponent); 
    }


    /**
     * Implements methods in <code>com.sun.identity.sm.ServiceListener</code>.
     *
     * @param serviceName
     * @param version
     */
    public void schemaChanged(String serviceName, String version) {
        if (debug.messageEnabled()) {
            debug.message("authlevel : schemaChanged " + serviceName + 
                ", ver=" + version);
        }
        // if it is iPlanetAMAuthService, initialize listeners
        // since new modules might be added or old modules removed
        if (serviceName.equals(CORE_AUTH)) {
            initialize();
        } else {
        	//HashMap will replace if there is existing one already
        	//this is necessary because ServiceSchemaManagerImpl will 
        	//be cleared and therefore will be stale
        	String moduleName = moduleServiceMap.get(serviceName);
    		if ( !listenerMap.isEmpty() ) {
	        	try {
	                // just in case ssm or scm already has AMAuthLevelManager registered
	        		// will remove existing one and replace it with new one.
                    ListenerMapEntry entry = listenerMap.remove(moduleName);
                    if (entry != null) {
                        entry.removeListeners();
                        entry = addServiceListener(serviceName);
                        if (entry != null) {
                            listenerMap.put(moduleName, entry);
                        }
                    }
	            } catch (Exception e) {
	                debug.error("can't add listener for " + serviceName, e);
	                return;
	            }
    		}
        }
        // process auth configuration updates
        updateAuthConfiguration(serviceName, "", "");
        updateGlobalAuthLevelMap(serviceName);
    }
    
    public int getLevelForModule(
        String moduleName,
        String orgDN,
        String defaultAuthLevel) {
        if (debug.messageEnabled()) {
            debug.message("moduleName : " + moduleName);
            debug.message("orgDN : " + orgDN);
            debug.message("defaultAuthLevel: " + defaultAuthLevel);
        }
        Map<String, Integer> map = authLevelMap.get(orgDN);
        if (map == null) {
            map = initOrgAuthLevel(orgDN);
        }
        if (debug.messageEnabled()) {
            debug.message("Map is : " + map);
        }

        Integer authLevel = null;
        if (map != null && !map.isEmpty()) {
            authLevel = map.get(moduleName);
        }
        //same fix needed for 6.3 too.
        if (authLevel == null) {
            authLevel = globalAuthLevelMap.get(moduleName);
            if (authLevel == null) {
                authLevel = Integer.valueOf(defaultAuthLevel);
            }
        }

        if (debug.messageEnabled()) {
            debug.message("authLevel : " + authLevel);
        }

        return authLevel;
    }

    /**
     * Removes all service listeners for the specified authentication
     * configuration.
     *
     * @param configName Name of authentication configuration.
     */
    protected void removeAuthConfigListener(String configName) {
       removeConfigListenerEntry(singleton(configName));
    }

    /**
     * Register auth config listener for a auth modules or auth 
     * configuration service. 
     * @param service   Service name,  e.g. iPlanetAMAuthLDAPService
     * @param name      Auth config name
     */
    protected void addAuthConfigListener(String service, String name) {
        Set<String> set = authConfigListenerMap.get(service);
        if (set == null) {
            set = new CopyOnWriteArraySet<>();
            set.add(name);
            authConfigListenerMap.put(service, set);
        } else {
            set.add(name);
        }
    }

    /**
     * Processes authentication configuration update upon service change
     * notification.
     *
     * @param serviceName Name of the service which was changed.
     * @param orgName Organization DN.
     * @param componentName Name of the component changed.
     */
    private synchronized void updateAuthConfiguration(
        String serviceName, 
        String orgName,
        String componentName) {
        Set<String> set = authConfigListenerMap.get(serviceName);
        if (set == null || set.isEmpty()) {
            // no auth config listener for this service
            return;
        }
        // new set to hold entries which will be updated
        // need to remove them from other entries in the authConfigListenerMap
        Set<String> updatedEntries = null;
        for (final String configName : set) {
            if (processAuthConfigEntry(serviceName, orgName,
                    componentName, configName)) {
                if (updatedEntries == null) {
                    updatedEntries = new HashSet<>();
                }
                updatedEntries.add(configName);
            }
        }

        if (updatedEntries == null) {
            return;
        }

        if (debug.messageEnabled()) {
            debug.message("updateAuthConfiguration, updated=" + updatedEntries);
        }

        // now we need to remove the update auth config entries from
        // other entries in authConfigListenerMap
        removeConfigListenerEntry(updatedEntries);
    }

    private void removeConfigListenerEntry(Set<String> updatedEntries) {
        synchronized (authConfigListenerMap) {
            for (final Map.Entry<String, Set<String>> entry : authConfigListenerMap.entrySet()) {
                String service = entry.getKey();
                Set<String> entries = entry.getValue();
                if (debug.messageEnabled()) {
                    debug.message("updateAuthConfiguration, check " + service + ", entries=" + entries);
                }
                if (entries != null && !entries.isEmpty()) {
                    entries.removeAll(updatedEntries);
                }
            }
        }
    }

    /**
     * Processes one authentication configuration entry upon service change
     * notification. Check if this entry need to be updated based on the
     * notification information, if so, call AMAuthConfiguration to update
     * auth config for this entry.
     *
     * @param serviceName Name of the service which was changed
     * @param orgName Organization DN.
     * @param componentName Name of the component changed.
     * @param configName Authentication configuration name.
     * @return true if the auth config is updated.
     */
    private boolean processAuthConfigEntry(
        String serviceName, 
        String orgName,
        String componentName,
        String configName) {
        // check if we need to update config based on service names
        boolean needUpdate = false;
        if (componentName.length() == 0) {
            // always update for schema changes
            needUpdate = true;
        } else if (serviceName.equals(CORE_AUTH) || 
            ((serviceName.startsWith("iPlanetAMAuth") || 
              serviceName.startsWith(ISAuthConstants.AUTH_ATTR_PREFIX_NEW)) &&
              serviceName.endsWith("Service"))) {
            // Login Module or Core auth changed, 
            // module name looks like following
            // iPlanetAMAuth<Module_Name>Service 
            // check if it is for this org
            AMAuthConfigType type = new AMAuthConfigType(configName);
            if (type.getOrganization().equals(orgName)) {
                needUpdate = true;
            }
        } else if (serviceName.equals(AMAuthConfigUtils.SERVICE_NAME)) { 
            // configuration service changed. 
            // find out subconfig name 
            int i = componentName.lastIndexOf("/");
            // hold the service name
            String temp = "";
            if (i != -1) {
                temp = componentName.substring(i + 1); 
            } else {
                temp = componentName;
            }
            // convert name to AMAuthConfigType
            AMAuthConfigType type = new AMAuthConfigType(configName);
            if (type.getOrganization().equals(orgName) && 
                (AuthD.revisionNumber >= ISAuthConstants.AUTHSERVICE_REVISION7_0
                   || (type.getIndexType() == AMAuthConfigType.SERVICE &&
                       type.getIndexName().equalsIgnoreCase(temp))
                )) {
                // match index type, service name & orgnanization DN
                if (debug.messageEnabled()) {
                    debug.message(configName + " matches " + temp);
                }
                needUpdate = true;
            }
        }

        if (needUpdate) {
            if (debug.messageEnabled()) {
                debug.message("processSMNotification, name=" + configName);
            }
            ((AMConfiguration)Configuration.getConfiguration())
                .processListenerEvent(configName);
        } 

        return needUpdate;
    }

    /**
     * Retreives and updates the service organization schema's global 
     * authentication level map with the changed authentication level. 
     */
    private void updateGlobalAuthLevelMap(String serviceName) {
        if (debug.messageEnabled()) {
            debug.message("updateGlobalAuthLevelMap for " + serviceName);
        }
        try {
            ServiceSchemaManager ssm = new ServiceSchemaManager(serviceName, 
                                        AuthD.getAuth().getSSOAuthSession());
            ServiceSchema schema = ssm.getOrganizationSchema();
            Map attrs = null;
            if (schema != null) {
                attrs = schema.getAttributeDefaults();
            }
            String module = moduleServiceMap.get(serviceName);
            if ( (module != null) && module.length() > 0 ) {
                String attrName = 
                    AMAuthConfigUtils.getAuthLevelAttribute(attrs, module);
                String authLevel = CollectionHelper.getMapAttr(attrs, attrName);
                if ((authLevel != null) && (authLevel.length() > 0)) {
                    Integer level = Integer.valueOf(authLevel);
                    globalAuthLevelMap.put(module, level);
                    debug.message("authLevel is : {}", authLevel);
                    debug.message("globalAuthLevelMap is : {}", globalAuthLevelMap);
                } else {
                    debug.warning("No auth level for module {}", module);
                }
            }
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("Error retrieving service schema " , e);
            }
        }
    }

    /**
     * Holds information on registered service and config listeners so that they can be de-registered when no longer
     * needed.
     */
    private static class ListenerMapEntry {
        private final ServiceSchemaManager serviceSchemaManager;
        private final String schemaListenerId;
        private final ServiceConfigManager serviceConfigManager;
        private final String configListenerId;

        ListenerMapEntry(final ServiceSchemaManager serviceSchemaManager, final String schemaListenerId,
                         final ServiceConfigManager serviceConfigManager, final String configListenerId) {
            this.serviceSchemaManager = serviceSchemaManager;
            this.schemaListenerId = schemaListenerId;
            this.serviceConfigManager = serviceConfigManager;
            this.configListenerId = configListenerId;
        }

        /**
         * Removes the registered listeners. Any errors that occur will be logged and swallowed.
         */
        void removeListeners() {
            try {
                serviceSchemaManager.removeListener(schemaListenerId);
                serviceConfigManager.removeListener(configListenerId);
            } catch (Exception e) {
                debug.error("AMAuthLevelManager: removeListeners", e);
            }
        }
    }
}
