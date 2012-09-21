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
 * $Id: BaseModule.java,v 1.2 2008/06/25 05:51:35 qcheng Exp $
 *
 */

package com.sun.identity.agents.arch;

/**
 * The <code>BaseModule</code> is a special type of <code>Module</code> which
 * represents the core infrastructure of the Agent runtime. Unlike other 
 * <code>Module</code>s which have to be registered at runtime, the 
 * <code>BaseModule</code> is always registered and available.
 */
public class BaseModule implements IBaseModuleConstants {

   /**
    * This method registers the <code>BaseModule</code> in the Agent runtime.
    * This method is required by <code>ModuleList</code> in order to automate
    * the registration of all applicable <code>Module</code>s in the Agent
    * runtime.
    */
    public static void init() {
        ModuleList.addRegisteredModule(getModule());
    }

   /**
    * Returns an instance of <code>Module</code> which has been initialized
    * as the representing the <code>BaseModule</code>.
    * 
    * @return the <code>Module</code> which represents the 
    * <code>BaseModule</code>.
    */
    public static Module getModule() {
        return _module;
    }

   /**
    * Returns the module code associated with the <code>BaseModule</code>.
    * 
    * @return the module code associated with the <code>BaseModule</code>.
    */
    public static byte getModuleCode() {
        return BASE_MODULE_CODE;
    }


    private static Module _module = new Module(getModuleCode(), STR_BASE_MODULE,
                                         BASE_RESOURCE);    
}
