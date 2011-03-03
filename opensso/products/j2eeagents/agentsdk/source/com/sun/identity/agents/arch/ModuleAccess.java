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
 * $Id: ModuleAccess.java,v 1.2 2008/06/25 05:51:37 qcheng Exp $
 *
 */

package com.sun.identity.agents.arch;

import java.util.Locale;

/**
 * This class can be used as a base class for a 
 * subsystem of classes where the Module specific functionality is needed.
 */
public class ModuleAccess implements IModuleAccess {
    
   /**
    * Creats a <code>ModuleAccess</code> instance that allows the access to
    * various Module related functionality offered by the given 
    * <code>module</code>. 
    * 
    * @param module whoes services will be exposed.
    */
    public ModuleAccess(Module module) {
        setModule(module);
    }

   /* (non-Javadoc)
    * @see com.sun.identity.agents.arch.IModuleAccess#getResource(int)
    */
    public String getResource(int id) {
        return getModule().getResource(id);
    }

   /* (non-Javadoc)
    * @see IModuleAccess#getResource(int, java.util.Locale)
    */
    public String getResource(int id, Locale locale) {
        return getModule().getResource(id, locale);
    }

   /* (non-Javadoc)
    * @see IModuleAccess#makeLocalizableString(int)
    */
    public LocalizedMessage makeLocalizableString(int id) {
        return getModule().makeLocalizableString(id);
    }

   /* (non-Javadoc)
    * @see IModuleAccess#makeLocalizableString(int, java.lang.Object[])
    */
    public LocalizedMessage makeLocalizableString(int id, Object[] params) {
        return getModule().makeLocalizableString(id, params);
    }

   /* (non-Javadoc)
    * @see com.sun.identity.agents.arch.IDebugAccess#isLogMessageEnabled()
    */
    public boolean isLogMessageEnabled() {
        return getModule().isLogMessageEnabled();
    }

   /* (non-Javadoc)
    * @see IDebugAccess#isLogWarningEnabled()
    */
    public boolean isLogWarningEnabled() {
        return getModule().isLogWarningEnabled();
    }

   /* (non-Javadoc)
    * @see IDebugAccess#logMessage(java.lang.String)
    */
    public void logMessage(String msg) {
        getModule().logMessage(msg);
    }

   /* (non-Javadoc)
    * @see IDebugAccess#logMessage(java.lang.String, java.lang.Throwable)
    */
    public void logMessage(String msg, Throwable th) {
        getModule().logMessage(msg, th);
    }

   /* (non-Javadoc)
    * @see IDebugAccess#logWarning(java.lang.String)
    */
    public void logWarning(String msg) {
        getModule().logWarning(msg);
    }

   /* (non-Javadoc)
    * @see IDebugAccess#logWarning(java.lang.String, java.lang.Throwable)
    */
    public void logWarning(String msg, Throwable th) {
        getModule().logWarning(msg, th);
    }

   /* (non-Javadoc)
    * @see IDebugAccess#logError(java.lang.String)
    */
    public void logError(String msg) {
        getModule().logError(msg);
    }

    /* (non-Javadoc)
     * @see IDebugAccess#logError(java.lang.String, java.lang.Throwable)
     */
    public void logError(String msg, Throwable th) {
        getModule().logError(msg, th);
    }
    
   /**
    * Allows subclasses to retrieve the associated <code>Module</code> instance.
    * 
    * @return the associated <code>Module</code> instance.
    */
    public Module getModule() {
        return _module;
    }
    
    private void setModule(Module module) {
        _module = module;
    }
    
    private Module _module;

}
