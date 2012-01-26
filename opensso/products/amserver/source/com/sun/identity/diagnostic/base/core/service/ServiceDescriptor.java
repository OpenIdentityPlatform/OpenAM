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
 * $Id: ServiceDescriptor.java,v 1.1 2008/11/22 02:19:54 ak138937 Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.diagnostic.base.core.service;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.sun.identity.diagnostic.base.core.jaxbgen.CategoryType;
import com.sun.identity.diagnostic.base.core.jaxbgen.LibraryType;
import com.sun.identity.diagnostic.base.core.jaxbgen.ResourceType;

/**
 * This class is the in-memory representation of service descriptor.
 * (service.xml)
 */
public class ServiceDescriptor {
    
    private URL serviceHome;
    private URL manifestUrl;   
    private static String CORE_SERVICE = "core";
    private static String APP_SERVICE  = "application";
    private com.sun.identity.diagnostic.base.core.jaxbgen.ServiceType 
        serviceType;
    
    public ServiceDescriptor() {
    }
    
    /**
     * Create a new instance of <code>ServiceDescriptor</code> with 
     * service home, service URL and handle to service metadata.
     *
     * @param loc the loction of the service
     * @param manifest the url of the service descriptor file
     * @param s the service data object 
     */
    public ServiceDescriptor(
        URL loc, 
        URL manifest, 
        com.sun.identity.diagnostic.base.core.jaxbgen.ServiceType s
    ) {
        serviceHome = loc;
        manifestUrl = manifest;
        serviceType = s;
    }
    
    /**
     * Returns the path to service home.
     *
     * @return path to service home.
     */
    public URL getServiceHome() {
        return serviceHome;
    }
    
    /**
     * Returns the service descriptor (service.xml) URL.
     *
     * @return URL representing the service descriptor file.
     */
    public URL getServiceManifestURL() {
        return manifestUrl;
    }
    
    /**
     * Returns the service id of this descriptor.
     *
     * @return id of this service descriptor.
     */
    public String getId() {
        return serviceType.getId();
    }
    
    /**
     * Returns the class name represents this service.
     *
     * @return name of the class representing this service.
     */
    public String getServiceClassName() {
        return serviceType.getClazz();
    }
    
    /**
     * Returns the name of the service.
     *
     * @return name of this service.
     */
    public String getServiceName(){
        return serviceType.getName();
    }
    
    /**
     * Returns the type of this service. Either 'core' 
     * or 'application'.
     *
     * @return type of this service.
     */
    public String getServiceType() {
        List sType = serviceType.getTypeofservice().getContent();
        return (String)sType.get(0);
    }
    
    /**
     * Returns true if its core service, false otherwise.
     *
     * @return true if core service, false otherwise.
     */
    public boolean isCoreService() {
        if (getServiceType().equalsIgnoreCase(CORE_SERVICE)) {
            return true;
        }
        return false;
    }
    
    /**
     * Returns all the categories appplicable for this service.
     *
     * @return list of categories for this service.
     */
    public List<CategoryType> getRealizationCategories() {
        return serviceType.getRealization().getCategory();
    }
    
    /**
     * Returns the list of categories appplicable for this service.
     *
     * @return list of categories for this service.
     */
    public List<String> getRealizationCategoriesType() {
        List<String> result = new ArrayList<String> ();
        List<CategoryType> categories = 
            serviceType.getRealization().getCategory();
        for (CategoryType c: categories) {
            result.add(c.getId());
        }
        return result;
    }
    
    /**
     * Returns the operation of the service for the given category.
     *
     * @param category for which the operation is to be found.
     * @return operation of this service for the given category.
     */
    public String getOperationForCategory(String category) {
        List<CategoryType> categories = 
            serviceType.getRealization().getCategory();
        for (CategoryType c: categories) {
            if (category.equals(c.getId())) {
                return c.getOperation();
            }
        }
        return null;
    }
    
    /**
     * Returns the name of the service for the given category.
     *
     * @param category category for which the service name is requested.
     * @return name of the service for the given category.
     */
    public String getNameForCategory(String category) {
        List<CategoryType> categories = 
            serviceType.getRealization().getCategory();
        for (CategoryType c: categories) {
            if (category.equals(c.getId())) {
                return c.getName();
            }
        }
        return null;
    }
    
    /**
     * Returns whether this service is to be loaded on application 
     * start up.
     * true if needs to be loaded on application start-up, false 
     * otherwise.
     *
     * @return true if this needs to be loaded on application start-up, 
     *  false otherwise.
     */
    public boolean isLoadOnStartup() {
        List start = 
            serviceType.getRuntime().getLoadOnStartup().getContent();
        if (((String)start.get(0)).equalsIgnoreCase("true")) {
            return true;
        }
        return false;
    }
    
    /**
     * Returns a list of libraries for this service.
     *
     * @return list of libraries for this service.
     */
    public List<String> getLibraries() {
        List<String> result = new ArrayList<String>();
        List<LibraryType> libs = 
            serviceType.getRuntime().getLibraries().getLibrary();
        for (LibraryType l:  libs) {    
            result.add((String)l.getContent().get(0));
        }
        return result;
    }
    
    /**
     * Returns a list of resources for this service.
     *
     * @return list of resources for this service.
     */
    public List<String> getResources() {
        List<String> result = new ArrayList<String>();
        List<ResourceType> resources = 
            serviceType.getRuntime().getResourceBundles().getResource();
        for (ResourceType r : resources) {
            result.add((String)r.getContent().get(0));
        }
        return result;
    }
}
