/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: IdRepoPluginsCache.java,v 1.8 2009/11/10 01:52:37 hengming Exp $
 *
 * Portions Copyrighted 2015-2016 ForgeRock AS.
 */

package com.sun.identity.idm.server;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.DNUtils;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.idm.IdOperation;
import com.sun.identity.idm.IdRepo;
import com.sun.identity.idm.IdRepoBundle;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdRepoListener;
import com.sun.identity.idm.IdType;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SMSThreadPool;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.sm.ServiceSchemaManager;
import java.security.AccessController;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 *
 * @author aravind
 */
public class IdRepoPluginsCache implements ServiceListener {
    
    static Debug debug = Debug.getInstance("amIdm");
    static boolean initializedListeners;
    static ServiceConfigManager idRepoServiceConfigManager;
    private static int svcRevisionNumber;
    
    // Cache of IdRepo Plugins
    // The Map contains <orgName, MAP<name, IdRepo object>>
    private Map idrepoPlugins = new HashMap();
    // Needs to synchronized for get(), put() and clear()
    private Map readonlyPlugins = new Hashtable();
    
    protected IdRepoPluginsCache() {
        // Initialize listeners
        if (debug.messageEnabled()) {
            debug.message("IdRepoPluginsCache constructor called");
        }
        initializeListeners();
    }

    @SuppressWarnings("unchecked")
    protected Set<IdRepo> getIdRepoPlugins(String orgName)
        throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("IdRepoPluginsCache.getIdRepoPlugins orgName: " +
                orgName);
        }
        // Check the cache
        Map orgRepos = null;
        orgName = DNUtils.normalizeDN(orgName);
        Set readOrgRepos = (Set) readonlyPlugins.get(orgName);
        if ((readOrgRepos != null) && !readOrgRepos.isEmpty()) {
            return (readOrgRepos);
        }
        synchronized (idrepoPlugins) {
            orgRepos = (Map) idrepoPlugins.get(orgName);
            if (orgRepos == null) {
                try {
                    if (debug.messageEnabled()) {
                        debug.message("IdRepoPluginsCache.getIdRepoPlugins " +
                            "Not in cache for: " + orgName);
                    }
                    // Initialize the plugins
                    orgRepos = new LinkedHashMap();
                    ServiceConfig sc = idRepoServiceConfigManager
                        .getOrganizationConfig(orgName, null);
                    if (sc == null) {
                        // Organization does not exist. Error condition
                        debug.error("IdRepoPluginsCache.getIdRepoPlugins " +
                            "Org does not exisit: " + orgName);
                        Object[] args = { orgName };
                        throw new IdRepoException(
                            IdRepoBundle.BUNDLE_NAME, "312", args);
                    }
                    Set subConfigNames = sc.getSubConfigNames();
                    if (debug.messageEnabled()) {
                        debug.message("IdRepoPluginsCache.getIdRepoPlugins " +
                            "Loading plugins: " + subConfigNames);
                    }
                    if (subConfigNames != null && !subConfigNames.isEmpty()) {
                        for (Iterator items = subConfigNames.iterator();
                            items.hasNext();) {
                            String idRepoName = (String) items.next();
                            ServiceConfig reposc = sc.getSubConfig(idRepoName);
                            if (reposc == null) {
                                debug.error("IdRepoPluginsCache." +
                                    "getIdRepoPlugins SubConfig is null for" +
                                    " orgName: " + orgName +
                                    " subConfig Name: " + idRepoName);
                            }
                            IdRepo repo = constructIdRepoPlugin(orgName,
                                reposc.getAttributesForRead(), idRepoName);
                            // Add to cache
                            orgRepos.put(idRepoName, repo);
                        }
                    }
                    // Add internal repos
                    addInternalRepo(orgRepos, orgName);
                    idrepoPlugins.put(orgName, orgRepos);
                } catch (SMSException ex) {
                    debug.error("IdRepoPluginsCache.getIdRepoPlugins " +
                            "SMS Exception for orgName: " + orgName, ex);
                }
            }
            // Cache a readonly copy
            if (orgRepos != null) {
                readOrgRepos = new OrderedSet();
                readOrgRepos.addAll(orgRepos.values());
                readonlyPlugins.put(orgName, readOrgRepos);
            }
        }
        if (debug.messageEnabled() && (readOrgRepos != null)) {
            Set ps = new HashSet();
            for (Iterator items = readOrgRepos.iterator(); items.hasNext();) {
                ps.add(items.next().getClass().getName());
            }
            debug.message("IdRepoPluginsCache.getIdRepoPlugins retuned for" +
                " OrgName: " + orgName + " Plugins: " + ps);
        }
        return (readOrgRepos);
    }

    @SuppressWarnings("unchecked")
    protected Set<IdRepo> getIdRepoPlugins(String orgName, IdOperation op,
        IdType type) throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("IdRepoPluginsCache.getIdRepoPlugins for " +
                "OrgName: " + orgName + " Op: " + op + " Type: " + type);
        }
        String cacheName = DNUtils.normalizeDN(orgName) + op.toString() +
            type.toString();
        Set answer = (Set) readonlyPlugins.get(cacheName);
        if ((answer != null) && !answer.isEmpty()) {
            return (answer);
        }
        answer = new OrderedSet();
        Set plugins = getIdRepoPlugins(orgName);
        if ((plugins != null) && !plugins.isEmpty()) {
            for (Iterator items = plugins.iterator(); items.hasNext();) {
                IdRepo repo = (IdRepo) items.next();
                if (repo.getSupportedTypes().contains(type)) {
                    Set ops = repo.getSupportedOperations(type);
                    if (ops.contains(op)) {
                        answer.add(repo);
                    }
                }
            }
        }
        if (debug.messageEnabled()) {
            Set ps = new HashSet();
            for (Iterator items = answer.iterator(); items.hasNext();) {
                IdRepo repo = (IdRepo) items.next();
                ps.add(repo.getClass().getName());
            }
            debug.message("IdRepoPluginsCache.getIdRepoPlugins retuned for" +
                " OrgName: " + orgName + " Op: " + op + " Type: " + type +
                " Plugins: " + ps);
        }
        synchronized (idrepoPlugins) {
            if (answer != null) {
                readonlyPlugins.put(cacheName, answer);
            }
        }
        return (answer);
    }
    
    /**
     * Delete an IdRepo plugin
     */
    private void removeIdRepo(String orgName, String name,
        boolean reinitialize) throws IdRepoException, SSOException {
        orgName = DNUtils.normalizeDN(orgName);
        synchronized (idrepoPlugins) {
            // Clear IdRepo plugins first since other threads should
            // not access it during shutdown
            clearReadOnlyPlugins(orgName);
            Map idrepos = (Map) idrepoPlugins.get(orgName);
            if (idrepos != null && !idrepos.isEmpty()) {
                // Iterate through the plugins
                for (Iterator items = idrepos.keySet().iterator();
                    items.hasNext();) {
                    String iname = items.next().toString();
                    if (iname.equalsIgnoreCase(name)) {
                        IdRepo repo = (IdRepo) idrepos.get(iname);
                        // Shutting down idrepo
                        if (debug.messageEnabled()) {
                            debug.message("IdRepoPluginsCache.removeIdRepo" +
                                " for OrgName: " + orgName + " Repo Name: " +
                                name);
                        }
                        // Remove from cache first
                        idrepos.remove(iname);
                        ShutdownIdRepoPlugin shutdownrepo =
                            new ShutdownIdRepoPlugin(repo);
                        // Provide a delay of 500ms for existing operations
                        // to complete. the delay is in the forked thread.
                        SMSThreadPool.scheduleTask(shutdownrepo);
                        break;
                    }
                }
                if (reinitialize) {
                    // Adding plugin back provides the atomic operation
                    // for the caller. Else, client will get No-plugins
                    // configured exception.
                    // Add the plugin back to the cache
                    addIdRepo(orgName, name);
                }
            }
        }
    }
    
    private void clearReadOnlyPlugins(String orgName) {
        // clear a readonly copy for the org Name
        for (Iterator items = readonlyPlugins.keySet().iterator();
            items.hasNext();) {
            String name = items.next().toString();
            if (name.startsWith(orgName)) {
                items.remove();
            }
        }
    }
    
    /**
     * Delete all IdRepo plugin for the organization
     */
    private void removeIdRepo(String orgName) {
        orgName = DNUtils.normalizeDN(orgName);
        Map idrepos = null;
        synchronized (idrepoPlugins) {
            // Clear IdRepo plugins first
            clearReadOnlyPlugins(orgName);
            idrepos = (Map) idrepoPlugins.remove(orgName);
        }
        if (debug.messageEnabled()) {
            debug.message("IdRepoPluginsCache.removeIdRepo for " +
                "OrgName: " + orgName + " Repo Names: " + idrepos.keySet());
        }
        ShutdownIdRepoPlugin shutdownrepos = new ShutdownIdRepoPlugin(idrepos);
        // Provide a delay of 500ms for existing operations
        // to complete. the delay is in the forked thread.
        SMSThreadPool.scheduleTask(shutdownrepos);
    }
    
    /**
     * Clears the IdRepo plugin cache
     */
    public void clearIdRepoPluginsCache() {
        Map cache = null;
        synchronized (idrepoPlugins) {
            // Clear readonly cache first.
            // Don't want other theads to get plugins that are
            // shutdown.
            readonlyPlugins.clear();
            cache = new HashMap(idrepoPlugins);
            idrepoPlugins.clear();
            readonlyPlugins.clear();
        }
        // Iterate throught the orgName and shutdown the repos
        for (Iterator onames = cache.keySet().iterator(); onames.hasNext();) {
            Map repos = (Map) cache.get(onames.next());
            for (Iterator items = repos.keySet().iterator(); items.hasNext();) {
                String name = items.next().toString();
                IdRepo repo = (IdRepo) repos.get(name);
                repo.removeListener();
                repo.shutdown();
            }
        }
    }
    
    /**
     * Adds an IdRepo plugin to an organization given the configuration
     * @param orgName organization to which IdRepo would be added
     * @param configMap configuration of the IdRepo
     */
    private void addIdRepo(String orgName, String name)
        throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("IdRepoPluginsCache.addIdRepo called for orgName: " +
                orgName + " IdRepo Name: " + name);
        }
        Map configMap = null;
        try {
            ServiceConfig sc = idRepoServiceConfigManager
                .getOrganizationConfig(orgName, null);
            if (sc == null) {
                debug.error("IdRepoPluginsCache.addIdRepo orgName: " +
                    orgName + " does not exisit");
                Object[] args = {orgName};
                throw new IdRepoException(
                    IdRepoBundle.BUNDLE_NAME, "312", args);
            }
            sc = sc.getSubConfig(name);
            if (sc == null) {
                debug.error("IdRepoPluginsCache.addIdRepo orgName: " +
                    orgName + " subConfig does not exisit: " + name);
                Object[] args = {orgName + ":" + name};
                throw new IdRepoException(
                    IdRepoBundle.BUNDLE_NAME, "312", args);
            }
            configMap = sc.getAttributes();
        } catch (SMSException smse) {
            if (debug.warningEnabled()) {
                debug.warning("IdRepoPluginsCache.addIdRepo SMSException " +
                    "for orgName: " + orgName + " sc name: " + name, smse);
            }
            return;
        }
        IdRepo repo = constructIdRepoPlugin(orgName, configMap, name);
        // Add to cache
        orgName = DNUtils.normalizeDN(orgName);
        synchronized (idrepoPlugins) {
            // Clear the readonly plugins first.
            // Other threads have to wait for the initialization to complete
            // Will get updated when getPlugins gets called
            clearReadOnlyPlugins(orgName);
            Map repos = (Map) idrepoPlugins.get(orgName);
            boolean addInternalRepos = false;
            if (repos == null) {
                repos = new LinkedHashMap();
                idrepoPlugins.put(orgName, repos);
                addInternalRepos = true;
            }
            repos.put(name, repo);
            if (addInternalRepos) {
                addInternalRepo(repos, orgName);
            }
        }
    }
    
    private void addInternalRepo(Map orgRepos, String orgName)
        throws SSOException, IdRepoException {
        // Check if AMSDK plugin needs to be added
        if (ServiceManager.isCoexistenceMode()) {
            orgRepos.put(IdConstants.AMSDK_PLUGIN_NAME,
                getAMRepoPlugin(orgName));
        }
        // Check if AgentsRepos needs to be added
        if (svcRevisionNumber >= 30) {
            orgRepos.put(IdConstants.AGENTREPO_PLUGIN,
                getAgentRepoPlugin(orgName));
        }
        // Check if SpecialRepo needs to be added
        if (ServiceManager.isConfigMigratedTo70() &&
            ServiceManager.getBaseDN().equalsIgnoreCase(orgName)) {
            orgRepos.put(IdConstants.SPECIAL_PLUGIN,
                getSpecialRepoPlugin());
        }
    }
    
    protected void initializeListeners() {
        synchronized (debug) {
            if (!initializedListeners) {
                // Add listeners to Service Schema and Config Managers
                if (debug.messageEnabled()) {
                    debug.message("IdRepoPluginsCache.initializeListeners: " +
                        "setting up ServiceListener");
                }
                SSOToken token = getAdminToken();

                try {
                    // Initialize configuration objects
                    idRepoServiceConfigManager = new ServiceConfigManager(token,
                        IdConstants.REPO_SERVICE, "1.0");
                    idRepoServiceConfigManager.addListener(this);

                    // Initialize schema objects
                    ServiceSchemaManager idRepoServiceSchemaManager =
                        new ServiceSchemaManager(token,
                        IdConstants.REPO_SERVICE, "1.0");
                    idRepoServiceSchemaManager.addListener(this);

                    // Get the version number
                    svcRevisionNumber = idRepoServiceSchemaManager
                        .getRevisionNumber();
                    
                    // Initialize listener for JAXRPCObject
                    IdRepoListener.addRemoteListener(
                        new JAXRPCObjectImplEventListener());
                    
                    initializedListeners = true;
                } catch (SMSException smse) {
                    // Exceptions will be throws during install and config
                    // when these services will not be loaded
                    String installTime = SystemProperties.get(
                        Constants.SYS_PROPERTY_INSTALL_TIME, "false");
                    if (!installTime.equals("true")) {
                        debug.error("IdRepoPluginsCache.initializeListeners: " +
                            "Unable to set up a service listener for IdRepo",
                            smse);
                    }
                } catch (SSOException ssoe) {
                    debug.error("IdRepoPluginsCache.initializeListeners: " +
                        "Unable to set up a service listener for IdRepo.",
                        ssoe);
                }
            }
        }
    }

    /**
     * Constructs IdRepo plugin object and returns.
     */
    private IdRepo constructIdRepoPlugin(String orgName, Map configMap,
        String name) throws IdRepoException, SSOException {
        IdRepo answer = null;
        if (debug.messageEnabled()) {
            debug.message("IdRepoPluginsCache.constructIdRepoPlugin: config=" +
                configMap.get("sunIdRepoClass"));
        }
        if (configMap == null || configMap.isEmpty()) {
            if (debug.warningEnabled()) {
                debug.warning("IdRepoPluginsCache.constructIdRepoPlugin: " +
                    "Cannot construct with empty config data");
            }
            return (null);
        }
        Set vals = (Set) configMap.get(IdConstants.ID_REPO);
        if ((vals != null) && !vals.isEmpty()) {
            String className = (String) vals.iterator().next();
            Class thisClass;
            try {
                thisClass = Thread.currentThread().getContextClassLoader()
                    .loadClass(className);
                answer = (IdRepo) thisClass.newInstance();
            } catch (Throwable ex) {
                debug.error("IdRepoPluginsCached.constructIdRepoPlugin " +
                    " OrgName: " + orgName + " ConfigMap: " + configMap, ex);
                throw (new IdRepoException(ex.getMessage()));
            }
            answer.initialize(configMap);

            // Add listener to this plugin class!
            Map listenerConfig = new HashMap();
            listenerConfig.put("realm", orgName);
            listenerConfig.put("plugin-name", name);
            if (className.equals(
                IdConstants.AMSDK_PLUGIN)) {
                listenerConfig.put("amsdk", "true");
            }
            IdRepoListener listener = new IdRepoListener();
            listener.setConfigMap(listenerConfig);
            answer.addListener(getAdminToken(), listener);
        }
        return (answer);
    }
    
    private static SSOToken getAdminToken() {
        return ((SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance()));
    }
    
    // Internal repos
    private IdRepo getSpecialRepoPlugin() 
        throws SSOException, IdRepoException  {
        // Valid only for root realm
        IdRepo pluginClass = null;
        try {
            if (debug.messageEnabled()) {
                debug.message("Special repo being initialized");
            }
            Class thisClass = Thread.currentThread().
                getContextClassLoader().loadClass(IdConstants.SPECIAL_PLUGIN);
            pluginClass = (IdRepo) thisClass.newInstance();
            HashMap config = new HashMap(2);
            config.put("realm", ServiceManager.getBaseDN());
            pluginClass.initialize(config);
            IdRepoListener lter = new IdRepoListener();
            lter.setConfigMap(config);
            pluginClass.addListener(getAdminToken(), lter);
        } catch (Exception e) {
            debug.error("IdRepoPluginsCache.getSpecialRepoPlugin: " +
                "Unable to init plugin: " + IdConstants.SPECIAL_PLUGIN, e);
        }
        return pluginClass;
    }

    protected IdRepo getAgentRepoPlugin(String orgName) 
        throws SSOException, IdRepoException  {
        IdRepo pluginClass = null;
        try {
            if (debug.messageEnabled()) {
                debug.message("Agents repo being initialized");
            }
            Class thisClass = Thread.currentThread().
                getContextClassLoader().loadClass(IdConstants.AGENTREPO_PLUGIN);
            pluginClass = (IdRepo) thisClass.newInstance();
            HashMap config = new HashMap(2);
            HashSet realmName = new HashSet();
            realmName.add(orgName);
            config.put("agentsRepoRealmName", realmName);
            pluginClass.initialize(config);
        } catch (Exception e) {
            debug.error("IdRepoPluginsCache.getAgentRepoPlugin: " +
                "Unable to init plugin: " + IdConstants.AGENTREPO_PLUGIN, e);
        }
        // Add listener
        if (pluginClass != null) {
            Map listenerConfig = new HashMap();
            listenerConfig.put("realm", orgName);
            IdRepoListener lter = new IdRepoListener();
            lter.setConfigMap(listenerConfig);
            pluginClass.addListener(getAdminToken(), lter);                
        }
        // Retuns the plugin class
        return pluginClass;
    }
    
    protected IdRepo getAMRepoPlugin(String orgName) 
        throws SSOException, IdRepoException {  
        IdRepo pluginClass = null;
        try {
            if (debug.messageEnabled()) {
                debug.message("AMSDK repo being initialized");
            }
            Class thisClass = Thread.currentThread().
                getContextClassLoader().loadClass(IdConstants.AMSDK_PLUGIN);
            pluginClass = (IdRepo) thisClass.newInstance();
            Map amsdkConfig = new HashMap();
            Set vals = new HashSet();
            vals.add(DNMapper.realmNameToAMSDKName(orgName));
            amsdkConfig.put("amSDKOrgName", vals);
            pluginClass.initialize(amsdkConfig);
        } catch (Exception e) {
            debug.error("IdRepoPluginsCache.getAMRepoPlugin: " +
                "Unable to instantiate plugin for Org: " + orgName, e);
        }

        if (pluginClass != null) {
            // Add listener to this plugin class
            Map listenerConfig = new HashMap();
            listenerConfig.put("realm", orgName);
            listenerConfig.put("amsdk", "true");
            IdRepoListener lter = new IdRepoListener();
            lter.setConfigMap(listenerConfig);
            pluginClass.addListener(getAdminToken(), lter);
        }

        
        return pluginClass;
    }

    /**
     * Notification for global config changes to IdRepoService
     */
    public void globalConfigChanged(String serviceName, String version,
            String groupName, String serviceComponent, int type) {
        if (debug.messageEnabled()) {
            debug.message("IdRepoPluginsCache: Global Config changed called " +
                "ServiceName: " + serviceName + " groupName: " + groupName +
                " serviceComp: " + serviceComponent + " Type: " + type);
        }
        if (serviceComponent.equals("") || serviceComponent.equals("/")) {
            return;
        }
        // FIXME: Clients don't have to call this !!
        if (!serviceComponent.startsWith("/users/") && 
            !serviceComponent.startsWith("/roles/")) {
            if (type != 1) {
                clearIdRepoPluginsCache();
            }
        } else {
            // Special identities have changed, clear the cache
            ((IdServicesImpl) IdServicesImpl.getInstance())
                .clearSpecialIdentityCache();
        }
    }

    /**
     * Notification for organization config changes to IdRepoService
     */
    public void organizationConfigChanged(String serviceName, String version,
            String orgName, String groupName, String serviceComponent, int type)
    {
        if (debug.messageEnabled()) {
            debug.message("IdRepoPluginsCache: Org Config changed called " +
                "ServiceName: " + serviceName + " orgName: " + orgName +
                " groupName: " + groupName + " serviceComp: " +
                serviceComponent + " Type: " + type);
        }
        // ignore componenet is "". 
        // if component is /" and type is 2(delete), need to remove hidden
        // plugins.
        if ((type == ServiceListener.REMOVED) &&
            (serviceComponent.length() == 0)) {
            // Organization has been deleted
            removeIdRepo(orgName);
        } else if ((serviceComponent.length() != 0) && 
            !serviceComponent.equals("/") && !serviceComponent.equals("")) {
            // IdRepo has been either added, removed or modified
            String idRepoName = null;
            StringTokenizer st = new StringTokenizer(serviceComponent, "/");
            if (st.hasMoreTokens()) {
                idRepoName = st.nextToken();
            }
            try {
                if (type == ServiceListener.ADDED) {
                    addIdRepo(orgName, idRepoName);
                } else if (type == ServiceListener.MODIFIED) {
                    if (!IdServicesImpl.isShutdownCalled()) {
                        // Reinitialize the plugin after shutdown
                        removeIdRepo(orgName, idRepoName, true);
                    } else {
                        removeIdRepo(orgName, idRepoName, false);
                    }
                } else if (type == ServiceListener.REMOVED) {
                    removeIdRepo(orgName, idRepoName, false);
                }
            } catch (Exception e) {
                debug.error("IdRepoPluginsCached.organizationConfigChanged " +
                    "ServiceName: " + serviceName + " orgName: " + orgName +
                    " groupName: " + groupName + " serviceComp: " +
                    serviceComponent + " Type: " + type, e);
            }
        }
    }

    /**
     * Notification for schema changes to IdRepoService
     */
    public void schemaChanged(String serviceName, String version) {
        if (debug.messageEnabled()) {
            debug.message("IdRepoPluginsCache: Schema changed called" +
                " Service name: " + serviceName);
        }
        clearIdRepoPluginsCache();
    }
    
    // Timer task to shutdown IdRepo plugins
     private class ShutdownIdRepoPlugin implements Runnable {
         
        IdRepo plugin;
        Map idrepos;

        public ShutdownIdRepoPlugin(IdRepo plugin) {
            this.plugin = plugin;
        }
        
        public ShutdownIdRepoPlugin(Map idrepos) {
            this.idrepos = idrepos;
        }

        public void run() {
            // Shutdown the repo
            try {
                // Provide a delay of 500ms for caller operations
                // to complete
                Thread.sleep(500);
            } catch (InterruptedException e) {
                if (debug.messageEnabled()) {
                   debug.message("IdRepoPluginsCache.ShutdownIdRepoPlugin: " + e );
                }
            }
            if (plugin != null) {
                plugin.removeListener();
                plugin.shutdown();
                plugin = null;
            }
            if (idrepos != null && !idrepos.isEmpty()) {
                // Iterate through the plugins
                for (Iterator items = idrepos.keySet().iterator();
                    items.hasNext();) {
                    String name = items.next().toString();
                    IdRepo repo = (IdRepo) idrepos.get(name);
                    // Shutting down idrepo
                    repo.removeListener();
                    repo.shutdown();
                }
                idrepos = null;
            }
        }
     }
}
