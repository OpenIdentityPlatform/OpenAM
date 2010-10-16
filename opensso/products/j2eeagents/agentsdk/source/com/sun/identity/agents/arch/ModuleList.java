/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ModuleList.java,v 1.2 2008/06/25 05:51:37 qcheng Exp $
 *
 */

package com.sun.identity.agents.arch;

import java.util.HashMap;

/**
 * Represents a list of registered <code>Module</code>s at runtime for reference
 * purposes. The <code>ModuleList</code> class initializes and registers all
 * the configured <code>Module</code>s during static initialization and makes
 * them available for reference by other classes within the system at a later
 * time.
 */
public class ModuleList {
    
    /**
     * Adds a <code>Module</code> in the list of registered 
     * <code>Module</code>s.
     * Note that if a <code>Module</code> is registered with the same module
     * code as an already registered <code>Module</code>, an
     * <code>IllegalStateException</code> is thrown by this method.
     *
     * @param module to be registered.
     * @throws <code>IllegalStateException</code> if there is an attempt to
     * register a <code>Module</code> with the same module code as an already
     * registered <code>Module</code>.
     */
    public static void addRegisteredModule(Module module) {
        Object oldModule = getModuleMap().put(
                new Integer(module.getModuleCode()), module);
        
        if (oldModule != null) {
            throw new IllegalStateException(
                    "Attempt to re-register the Module: "
                    + module);
        }
        if(getBaseModule().isLogMessageEnabled()) {
            getBaseModule().logMessage("Registered Module: "
                    + module.toString());
        }
    }
    
    /**
     * Retrieves a registered <code>Module</code> that was registered during
     * the Agent runtime initialization.
     *
     * @param code the module code associated with the <code>Module</code> to
     * be retrieved.
     *
     * @return the associated <code>Module</code> with the given module code, or
     * <code>null</code> if no such module was registered.
     */
    public static Module getRegisteredModule(byte code) {
        return(Module) getModuleMap().get(new Integer(code));
    }
    
    /**
     * Initializes the registered <code>Module</code>s in the system. This 
     * method is automatically executed by the static initializer of
     * <code>ModuleList</code>.
     */
    private synchronized static void initializeModules() {
        
        if( !isInitialized()) {
            ModuleList.addRegisteredModule(getBaseModule());
            String[] moduleList =
                    AgentConfiguration.getServiceResolver().getModuleList();
            
            if((moduleList != null) && (moduleList.length > 0)) {
                for(int i = 0; i < moduleList.length; i++) {
                    String nextModuleLoader = moduleList[i];
                    
                    try {
                        if(getBaseModule().isLogMessageEnabled()) {
                            getBaseModule().logMessage(
                                    "Loading Module: " + nextModuleLoader);
                        }
                        
                        Class moduleLoader = Class.forName(nextModuleLoader);
                        
                        moduleLoader.getMethod("init",
                                new Class[]{}).invoke(null,
                                new Object[]{});
                    } catch(Exception ex) {
                        getBaseModule().logError(
                                "Exception while registering module: "
                                + nextModuleLoader, ex);
                    }
                }
            }
        }
        markInitialized();
    }
    
    private static boolean isInitialized() {
        return _initialized;
    }
    
    private static void markInitialized() {
        _initialized = true;
    }
    
    private static Module getBaseModule() {
        return _baseModule;
    }
    
    private static HashMap getModuleMap() {
        return _moduleMap;
    }
    
    private static HashMap _moduleMap   = new HashMap();
    private static boolean _initialized = false;
    private static Module  _baseModule  = BaseModule.getModule();
    
    static {
        AgentConfiguration.initialize();
        initializeModules();
    }
}
