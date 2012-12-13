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
 * $Id: ToolRegistry.java,v 1.2 2009/07/24 22:13:53 ak138937 Exp $
 *
 */
/**
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.diagnostic.base.core.service;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.sun.identity.diagnostic.base.core.common.ToolConstants;
import com.sun.identity.diagnostic.base.core.jaxbgen.*;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import org.xml.sax.InputSource;

/**
 * This class represents the registry where all the services
 * in the application are registered. There is only one instance
 * of registry in the application.
 *
 */

public class ToolRegistry {
    private static ToolRegistry registry = null;
    private HashMap<String, ServiceDescriptor>  registeredServices = new HashMap();
    private HashMap<String, HashMap> serviceCategories = new HashMap();
    private static Object lock = new Object();
    private ResourceBundle rb = ResourceBundle.getBundle(
        ToolConstants.RESOURCE_BUNDLE_NAME);
  
    private ToolRegistry() {
    }
    
    /**
     * Creates an instance of <code>ToolRegistry</code> if it doesn't
     * already exist. It's implemented as singleton so that there is only one
     * instance of <code>ToolRegistry</code> in the application. This is
     * created during application start up by <code>ToolServiceManager</code>.
     */
    public static ToolRegistry getInstance() {
        if (registry != null) {
            return registry;
        }
        synchronized (lock) {
            if (registry == null) {
                registry = new ToolRegistry();
            }
        }
        return registry;
    }
    
    /**
     * This method takes care of registering all the applicable services.
     * Given a list of service locations <location, manifest>, it reads the
     * service manifest and checks for the validatity and applicabilty of each
     * service and registers only the valid and applicable services with the
     * registry.
     *
     * @param serviceLocations Map of service locations.
     */
    public void registerServices(HashMap<URL, URL> serviceLocations) {
        URL manifestUrl;
        URL manifestLocation;
        try {
            com.sun.identity.diagnostic.base.core.jaxbgen.ServiceType s;
            JAXBContext jxbcon = JAXBContext.newInstance(
                "com.sun.identity.diagnostic.base.core.jaxbgen");
            Unmarshaller u = jxbcon.createUnmarshaller();
            for (Iterator<URL> it = serviceLocations.keySet().iterator();
                it.hasNext();) {
                manifestLocation = it.next();
                manifestUrl = serviceLocations.get(manifestLocation);
                // Load the service manifest.
                s = (com.sun.identity.diagnostic.base.core.jaxbgen.ServiceType)
                    u.unmarshal(XMLUtils.createSAXSource(
                        new InputSource(manifestUrl.toExternalForm())));
                ServiceDescriptor sd = createServiceDescriptor(
                    manifestLocation, manifestUrl, s);
                registeredServices.put(sd.getId().toLowerCase(), sd);
                //Add the operation and Service ID in a HashMap
                List<CategoryType> categories = sd.getRealizationCategories();
                for (CategoryType c: categories) {
                    if (serviceCategories.containsKey(rb.getString(c.getId()))) {
                        HashMap nameToOpcode = serviceCategories.get(
                            rb.getString(c.getId()));
                        HashMap OpcodeToService = new HashMap();
                        OpcodeToService.put(c.getOperation(), sd.getId());
                        nameToOpcode.put(rb.getString(c.getName()), OpcodeToService);
                        serviceCategories.put(rb.getString(c.getId()), nameToOpcode);
                    } else {
                        HashMap nameToOpcode = new HashMap();
                        HashMap serviceMap = new HashMap();
                        serviceMap.put(c.getOperation(), sd.getId());
                        nameToOpcode.put(rb.getString(c.getName()), serviceMap);
                        serviceCategories.put(rb.getString(c.getId()), nameToOpcode);
                    }
                }
            }
        } catch(JAXBException je) {
            Debug.getInstance(ToolConstants.DEBUG_NAME).error(
                "ToolRegistry.registerServices : " +
                "Exception in registering services", je);
        }
    }
    
    /**
     * Return the list of service categories that are registered with this
     * registry.
     *
     * @return HashMap of service operations and categories registered with
     *         this registry.
     */
    public HashMap<String, HashMap> getAllServiceCategories() {
        return (!serviceCategories.isEmpty()) ? serviceCategories : null;
    }
    
    /**
     * Return the operation code and service class for a given category
     * and name
     *
     * @return HashMap of operation code and service class registered with
     *         this registry.
     */
    public HashMap<String, String> getServiceDetails(
        String category,
        String serviceName
    ) {
        HashMap<String, String> operationToService = null;
        HashMap<String, HashMap> categoryToName =
            serviceCategories.get(category);
        operationToService = categoryToName.get(serviceName);
        return (!operationToService.isEmpty()) ? operationToService : null;
    }
    
    /**
     * Return the list of service ID's that are registered with this
     * registry.
     *
     * @return List of service ID's registered with this registry.
     */
    public List<String> getAllRegisteredServices() {
        List<String> result = new ArrayList<String> ();
        for (Iterator<String> it = registeredServices.keySet().iterator();
            it.hasNext();) {
            result.add(it.next());
        }
        return result;
    }
    
    /**
     * Returns the name of the given service.
     *
     * @param id id of the service for which the name is requested.
     * @return name of the given service.
     */
    public String getServiceName(String id) {
        if (isRegistered(id)) {
            return registeredServices.get(id).getServiceName();
        }
        return null;
    }
    
    /**
     * Returns the service home path for the given service id.
     *
     * @param id id of the service for which home is to be obtained.
     * @return Path that represents the service home of the given service.
     */
    public URL getServiceHome(String id) {
        if (isRegistered(id)) {
            return registeredServices.get(id).getServiceHome();
        }
        return null;
    }
    
    /**
     * This method returns the list of libraries that are to be loaded
     * by the class loader for the given service.
     *
     * @param id The service for which the list of libraries are to be
     *         loaded.
     * @return list of libraries for the given service.
     */
    public List<String> getLibraries(String id) {
        if (isRegistered(id)) {
            return registeredServices.get(id).getLibraries();
        }
        return null;
    }
    
    /**
     * This method returns the list of resource bundles to be used for
     * this service as defined by its manifest.
     *
     * @param id The service for which the list of resource bundles
     *        are to be obtained.
     * @return list of resource bundles for the given service.
     */
    public List<String> getResourceBundles(String id) {
        if (isRegistered(id)) {
            return registeredServices.get(id).getResources();
        }
        return null;
    }
    
    /**
     * This method returns the type of this service. ie, whether its
     * a 'core' or 'application' service.
     *
     * @param id The service for which the type is requested.
     * @return type of the given service.
     */
    public String getServiceType(String id) {
        if (isRegistered(id)) {
            return registeredServices.get(id).getServiceType();
        }
        return null;
    }
    
    /**
     * This method returns whether this service is to be loaded on
     * application start-up.
     *
     * @param id id of the service.
     * @return True if the given service should be loaded on start-up.
     * False otherwise.
     */
    public boolean isLoadOnStart(String id) {
        if (isRegistered(id)) {
            registeredServices.get(id).isLoadOnStartup();
        }
        return false;
    }
    
    /**
     * Returns true if its a core service.
     *
     * @param id id of the service
     * @return true if its a core service. false otherwise.
     */
    public boolean isCoreService(String id) {
        ServiceDescriptor desc = registeredServices.get(id);
        if (desc != null) {
            return desc.isCoreService();
        }
        return false;
    }
    
    /**
     * This method returns whether this service is registered with
     * the registry.
     *
     * @param id id of the service
     * @return True if its registered. False otherwise.
     */
    public boolean isRegistered(String id) {
        ServiceDescriptor desc = registeredServices.get(id);
        if (desc != null) {
            return true;
        }
        return false;
    }
    
    /**
     * This method returns the service class name for the given sevice.
     *
     * @param id The service for which the service class name is requested.
     * @return name of the service class for the given service.
     */
    public String getServiceClassName(String id) {
        if (isRegistered(id)) {
            return registeredServices.get(id).getServiceClassName();
        }
        return null;
    }
    
    /**
     * Creates a service descriptor for the given service for registration.
     *
     * @param loc path representing service home.
     * @param manifest URL path of the service descriptor.(service.xml)
     * @param s Handle to service descriptor.
     *
     * @return An instance of the service descriptor.
     */
    public ServiceDescriptor createServiceDescriptor(
        URL loc, 
        URL manifest,
        com.sun.identity.diagnostic.base.core.jaxbgen.ServiceType s
    ) {
        ServiceDescriptor sd = new ServiceDescriptor(loc, manifest, s);
        return sd;
    }
}
