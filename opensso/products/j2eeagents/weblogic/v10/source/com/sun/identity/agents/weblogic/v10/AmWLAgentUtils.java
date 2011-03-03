/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AmWLAgentUtils.java,v 1.2 2008/06/25 05:52:22 qcheng Exp $
 *
 */

package com.sun.identity.agents.weblogic.v10;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import com.sun.identity.agents.arch.IModuleAccess;

/**
 * This is utility class to help dynamically load Class and Method.
 */
public class AmWLAgentUtils {
    
    /**
     * This function will generate a Method to be dynamically invoked
     * in runtime.
     *
     * @param modAccess the IModuleAccess used for logging
     * @param className the class name whose Method will be generated
     * @param methodName the name of the Method to be generated
     * @param parameterClasses the array of parameters' types.
     * @return Method generated
     *
     * @exception Exception thrown if anything wrong
     */
    public static Method getClassMethod(
            IModuleAccess modAccess,
            String className,
            String methodName,
            Class[] parameterClasses) throws Exception {
        
        Class clazz = null;
        Method method = null;
        
        if (modAccess.isLogMessageEnabled()) {
            modAccess.logMessage(
                    "AmWLAgentUtils.getClassMethod - " +
                    "load local auth class: " + className);
        }
        clazz = getClass(modAccess, className);
        method = clazz.getMethod(
                methodName,
                parameterClasses);
        
        return method;
    }
    
    /**
     * This function will generate a Constructor to be dynamically 
     * invoked in runtime.
     *
     * @param modAccess the IModuleAccess used for logging
     * @param className the class name whose Constructor will be generated
     * @param parameterClasses the array of parameters' types
     * @return Constructor generated
     * @exception Exception thrown if anything wrong
     */
    public static Constructor getConstructorMethod(
            IModuleAccess modAccess,
            String className,
            Class[] parameterClasses) throws Exception {
        
        Class clazz = null;
        Constructor constructor = null;
        
        clazz = getClass(modAccess, className);
        constructor = clazz.getConstructor(parameterClasses);
        return constructor;
    }

    /**
     * This function will return the Class of the specified class name during
     * runtime.
     *
     * @param modAccess the IModuleAccess used for logging
     * @param className the class name whose Class will be generated
     * @return Class generated
     * @exception Exception thrown if anything wrong
     */
    public static Class getClass(
            IModuleAccess modAccess,
            String className ) throws Exception {
        
        ClassLoader classLoader = null;
        Class clazz = null;
        Constructor constructor = null;
        
        classLoader =
                Thread.currentThread().getContextClassLoader();
        try {
            clazz = classLoader.loadClass(className);
        } catch (ClassNotFoundException ex) {
            if (modAccess.isLogWarningEnabled()) {
                modAccess.logWarning(
                    "AmWLAgentUtils.getClass - " +
                    "Thread.currentThread().getContextClassLoader " +
                    "can not find class: " +  className);
            }
        }
        
        if (clazz == null) {
            classLoader = ClassLoader.getSystemClassLoader();
            clazz = classLoader.loadClass(className);
        }
        
        if (clazz == null) {
            throw new Exception("Can not find Class of name: " + 
                    className);
        }
        
        return clazz;
    }    
    
}
