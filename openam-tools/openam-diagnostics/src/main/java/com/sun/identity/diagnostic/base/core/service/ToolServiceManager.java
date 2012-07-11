/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ToolServiceManager.java,v 1.1 2008/11/22 02:19:54 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.core.service;

import java.io.File; 
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.identity.diagnostic.base.core.ToolContext;
import com.sun.identity.diagnostic.base.core.common.ToolConstants;
import com.sun.identity.diagnostic.base.core.utils.IOUtils;
import com.sun.identity.shared.debug.Debug;


/**
 * This class is resposible for managing services. This is called 
 * during the application start-up to discover/load/register the 
 * available services. It takes care of all the interactions happen 
 * among services.
 *
 */
public class ToolServiceManager {
    
    private static final String SERVICE_MANIFEST_FILE = "service.xml";
    private static HashMap<URL,URL> discoveredServicesMap = null;
    private static Object instanceLock = new Object();
    private static ToolServiceManager sm = null;
    private ToolRegistry registry = null;
    private ToolContext tContext = null;
    private Map activeServices = new HashMap(); 
    private Map classLoaders = new HashMap();
    
    private ToolServiceManager() {
    }
    
    /**
     * Creates an instance of ToolServiceManager if it doesn't already 
     * exist. It's implemented as singleton so that there is only one 
     * instance of ToolServiceManager in the application.
     *
     * @return a singleton instance of <code>ToolServiceManager</code>
     */
    public static ToolServiceManager getInstance() {
        if (sm != null) {
            return sm;
        }
        synchronized (instanceLock) {
            if (sm == null) {
                sm = new ToolServiceManager();
            }
        }
        return sm;
    }
    
    /**
     * Returns the registry with which the services are registered.
     *
     * @return  registry that is used to registered the services.
     */
    public ToolRegistry getRegistry() {
        return ToolRegistry.getInstance();
    }
    
    /**
     * Called once by <code>ToolManager</code> during 
     * application start up to set the <code>ToolContext</code> 
     * for this <code>ToolServiceManager</code>.
     * 
     * @param tContext Context in which this service runs.
     */
    public void init(ToolContext tContext) {
        this.tContext = tContext;
        this.registry = getRegistry();
    }

    /**
     * This method is responsible for discovering the services and 
     * register them with the registry. This method just publishes 
     * the services in the registry and it doesn't load or activate 
     * the services.
     */
    public void publishServices() {
        discoverServices();
        registry.registerServices(discoveredServicesMap);
    }
    
    /**
     * This method is responsible of discovering servcies and 
     * collecting all available services. It starts by looking 
     * into default service repository location and then proceeds 
     * to scan the custom service repository locations if any.
     */
    public void discoverServices() {
        discoveredServicesMap = new HashMap <URL, URL> ();     
        ArrayList serviceRepositories = new ArrayList <String> ();
        
        serviceRepositories.add(tContext.getDefaultServiceRepository());
        serviceRepositories.addAll(tContext.getCustomServiceRepositories());
        try {
            discoveredServicesMap = 
                getAllServiceLocations(serviceRepositories);
        }
        catch (Exception e) {
            Debug.getInstance(ToolConstants.DEBUG_NAME).error(
                    "ToolServiceManager.discoverServices : " +
                    "Exception in discovering services", e);
        }
    }

    /**
     * Searches for services in the given repository location and 
     * returns the service locations map 
     * <service location, service descriptor URL>.
     *
     * @return Map of discovered service locations.
     */
    private HashMap <URL, URL> getAllServiceLocations(
        final ArrayList<String> repositories
    ) throws Exception {
        HashMap <URL, URL> result = new HashMap <URL, URL> ();
        for(String repository : repositories) {
            File f = new File(repository).getCanonicalFile();
            if (f.isDirectory()) {
                scanDirectory(f, result, true);
            }
        }
        return result;
    }
    
    private void scanDirectory(
        final File f, 
        HashMap result, 
        boolean recursive
    ) throws Exception {
        if (f.isDirectory()) {
            File manifest = new File(f, SERVICE_MANIFEST_FILE);
            if(manifest.isFile()) {
                result.put(IOUtils.convertFileToURL(f), 
                    IOUtils.convertFileToURL(manifest));
            }
            File[] files = f.listFiles();
            for (int i = 0; i < files.length; ++i) {
                if(recursive && files[i].isDirectory()) {
                    scanDirectory(files[i], result, false);
                }
            }
        }
    }   
    
    /**
     * Returns the all the discovered services 
     * 
     * @return  discovered services.
     */
    private HashMap <URL, URL> getDiscoveredServices() {
        return discoveredServicesMap;
    }
    
    /**
     * Identifies and activates the services that are to be 
     * activated during application start up.
     */
    public synchronized void activateServicesOnStartup() {
        List<String> services = 
            registry.getAllRegisteredServices();
        for (String service: services) {
            if (registry.isCoreService(service) ||
                    registry.isLoadOnStart(service)) {
                activateService(service);
            }
        }
    }
             
    /**
     * This method activates the given service.
     *
     * @param id The service that needs to be activated.
     * @return Instance of a service for the given service id.
     */
    public synchronized ToolService activateService(final String id) {
        if (isServiceActive(id)) {
            return getServiceFromPool(id);
        }       
        if(!registry.isRegistered(id)) {
            return null; 
        }        
        ToolService s = loadService(id);
        // Mark this service as 'active'.
        addServiceToPool(id, s);
        s.init(tContext);
        s.start();
        return getServiceFromPool(id);
    }
    
    /**
     * Get the given service from the pool only if its already active.
     *
     * @return instance of the requested service if it is active, null
     * otherwise.
     */
    private ToolService getServiceFromPool(String id) {
        if (isServiceActive(id)) {
            return (ToolService)activeServices.get(id);
        }
        return null;
    }
    
    /**
     * Adds the given service to active pool.
     *
     * @param id the id of the service that is to be added to the pool.
     * @param sClass instance of the service.
     */
    private void addServiceToPool(String id, ToolService sClass) {
        activeServices.put(id, sClass);
    }
    
    /**
     * Loads the given service. It uses the custom class loader to 
     * load the libraries classes for the given service.
     *
     * @param id the id of the service that is to be loaded.
     * @return an instance of the given service.
     */
    private ToolService loadService(final String id) {      
        String className = registry.getServiceClassName(id);
        if (className == null || "".equals(className)) {
            return null;
        }
        ToolServiceClassLoader cl = getServiceClassLoader(id);
        List<String> libs = registry.getLibraries(id);
        try {
            for (String library: libs) {
                URL path = resolvePath(library, id);
                cl.addURLToPath(path);
                
            }
        } catch (MalformedURLException e) {
            Debug.getInstance(ToolConstants.DEBUG_NAME).error(
                "ToolServiceManager.loadService : " +
                "Exception in loading service. URL is malformed", e);
        } catch (IOException ie) {
            Debug.getInstance(ToolConstants.DEBUG_NAME).error(
                "ToolServiceManager.loadService : " +
                "Exception in loading service. I/O error occured", ie);
        }
        loadResources(id);
        Class serviceClass = cl.loadService(className);
        try {
            return (ToolService) serviceClass.newInstance();
        } catch (InstantiationException ie) {
            Debug.getInstance(ToolConstants.DEBUG_NAME).error(
                "ToolServiceManager.loadService : " +
                "Exception in instantiating service", ie);
            return null;
        } catch (IllegalAccessException ile) {
            Debug.getInstance(ToolConstants.DEBUG_NAME).error(
                "ToolServiceManager.loadService : " +
                "Illegal access exception", ile);
            return null;
        }
    }

   /**
    * Sets the given resource path(s).
    */
    private void loadResources(final String id) {
        ToolServiceClassLoader cl = getServiceClassLoader(id);
        try {
            List<String> resBundles = registry.getResourceBundles(id);
            String path = registry.getServiceHome(id).getFile();
            for (String resource: resBundles) {
                String urlPath = path + resource;
                File f = new File(urlPath);
                URL rpath = IOUtils.convertFileToURL(f);
                cl.addURLToPath(rpath);
            }
        } catch (MalformedURLException re) {
            Debug.getInstance(ToolConstants.DEBUG_NAME).error(
                "ToolServiceManager.loadResources : " +
                "Malformed url exception", re);
        } catch (IOException rie) {
            Debug.getInstance(ToolConstants.DEBUG_NAME).error(
                "ToolServiceManager.loadResources : " +
                "URL access i/o error", rie);
        } catch (Exception ex) {
            Debug.getInstance(ToolConstants.DEBUG_NAME).error(
                "ToolServiceManager.loadResources : " +
                "Exception occured in loading resources", ex);
        }
    }

    /**
     * Resolves the library paths. If its relative, resolve it to 
     * the service home directory.
     *
     * @param library the path that needs to be resolved.
     * @id id of the service 
     */   
    private URL resolvePath(String library, String id) 
        throws IOException, MalformedURLException 
    {
        URL result = null;  
        if (library.startsWith("http://")){
            result = new URL(library);
        } else {
            File f = new File(library);
            if (f.isAbsolute()) {
                result = IOUtils.convertFileToURL(f);
            } else {
                String path = registry.getServiceHome(id).getFile() + library;
                f = new File(path);
                result = IOUtils.convertFileToURL(f);
            }
        }
        return result;
    }
    
    /**
     * Returns the service active status. Returns true if the 
     * service is active. False otherwise.
     *
     * @param id id of the service
     */
    public boolean isServiceActive(final String id) {
        if (activeServices.containsKey(id)) {
            return true;
        }
        return false;
    }
    
    /**
     * This is a convenient method to gain access to a service.
     *
     * @param id id of the service
     * @return instance of the requested service.
     */
    
    public ToolService getService(final String id) {
        return activateService(id);
    }
    
    /**
     * Returns the class loader for the given service.
     *
     * @param id id of the service for which the class loader is needed.
     */
    
    private ToolServiceClassLoader getServiceClassLoader(String id) {
        if (classLoaders.containsKey(id)){
            return (ToolServiceClassLoader) classLoaders.get(id);
        }
        synchronized (this) {
            ToolServiceClassLoader cl = null;
            if (!classLoaders.containsKey(id)){
                cl = new ToolServiceClassLoader(id, this, 
                    getClass().getClassLoader());
                classLoaders.put(id, cl);
            }
            return cl;
        }
    }
    
    /**
     * Returns the list of all active services in the application.
     *
     * @return List of active services.
     */
    public List<String> getAllActiveServices() {
        return new ArrayList(activeServices.keySet());
    }
}
