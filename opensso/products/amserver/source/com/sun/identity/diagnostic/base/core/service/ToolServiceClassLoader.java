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
 * $Id: ToolServiceClassLoader.java,v 1.1 2008/11/22 02:19:54 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.core.service;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;

import com.sun.identity.diagnostic.base.core.common.ToolConstants;
import com.sun.identity.shared.debug.Debug;


/**
 * This class represents the class loader to load the services. 
 * <code>ToolServiceManager</code> uses this custom class loader 
 * to load the libraries for every service.
 *
 */
public class ToolServiceClassLoader extends URLClassLoader { 
    private String serviceId = null;
    private ToolServiceManager manager = null;        
    private ClassLoader parent = null;
    
    /** Creates a new instance of ToolServiceClassLoader */
    public ToolServiceClassLoader() {
        super(new URL[]{});
    }
    
    /**
     * Creates a new instance of ToolServiceClassLoader with the 
     * given parent.
     *
     * @param parent Parent of this class loader.
     */
    public ToolServiceClassLoader(ClassLoader parent) {
        super(new URL[]{}, parent);
    }
    
    /**
     * Creates a new instance of ToolServiceClassLoader with the given 
     * service id, service manager and parent.
     *
     * @param id ID of the service for which the loader is created.
     * @param manager Tool Service Manager that manages this class loader.
     * @param parent represents the parent class loader of this class 
     *        loader.
     */
    public ToolServiceClassLoader(
        String id, 
        ToolServiceManager manager, 
        ClassLoader parent
    ) {
        super(new URL[]{}, parent);
        this.serviceId = id;
        this.manager = manager;
        this.parent = parent;
    }
    
    /**
     * Returns the <code>ToolServiceManager</code> that manages this 
     * class loader.
     *
     * @return An instance of <code>ToolServiceManager</code> that 
     *         is managing this classloader.
     */
    public ToolServiceManager getServiceManager() {
        return manager;
    }
    
    /**
     * Returns the ID of the service for which this class loader 
     * is responsible for.
     *
     * @return ID of the service for which this class loader is 
     *         resposible for.
     */
    public String getServiceId() {
        return serviceId;
    }
    
    /**
     * Returns the parent class loader that loaded the application.
     *
     * @return Parent class loader that loaded the application.
     */
    public ClassLoader getParentClassLoader() {
        return parent;
    }
    
    /**
     * This method is resposible for adding URLs to this
     * custom class loader's search path.
     *
     * @param aURL Tool manager that manages this context.
     */
    public void addURLToPath(final URL aURL) {
        super.addURL(aURL);
    }
    
    @Override protected Class < ? > findClass(final String aName) 
        throws ClassNotFoundException 
    {
        return super.findClass(aName);
    }
    
    @Override public URL findResource(final String aName) {
        return super.findResource(aName);
    }
    
    
    @Override public Enumeration < URL > findResources(final String aName) 
        throws IOException 
    {
        return super.findResources(aName);
    }
    
    /**
     * This method takes care of loading the service.
     */
    public Class loadService(String className) {
        try {
            return super.loadClass(className);
        }
        catch (ClassNotFoundException ce) {
            Debug.getInstance(ToolConstants.DEBUG_NAME).error(
                "ToolServiceClassLoader.loadService: " + ce.getMessage());
            return null;
        }
    }
}
