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
 * $Id: SupportedAPIDoclet.java,v 1.2 2008/06/25 05:48:04 qcheng Exp $
 *
 */

package com.sun.identity.tools.javadocs;

import com.sun.javadoc.Doc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.RootDoc;
import com.sun.tools.doclets.standard.Standard;
import com.sun.tools.javadoc.Main;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
 
public class SupportedAPIDoclet {
    private static String SUPPORTED_ALL_API = "supported.all.api";
    private static String SUPPORTED_API = "supported.api";

    private static Set includeAll = new HashSet();

    public static void main(String[] args) {
        String name = SupportedAPIDoclet.class.getName();
        Main.execute(name, name, args);
    }
 
    public static boolean validOptions(
        String[][] options,
        DocErrorReporter reporter
    ) throws java.io.IOException {
        return Standard.validOptions(options, reporter);
    }
     
    public static int optionLength(String option) {
        return Standard.optionLength(option);
    }
         
    public static boolean start(RootDoc root)
        throws java.io.IOException {
        return Standard.start((RootDoc)process(root, RootDoc.class));
    }
 
    private static void setIncludeAll(Object[] array) {
        for (int i = 0; i < array.length; i++) {
            Object entry = array[i];
            if (entry instanceof Doc) {
                Doc doc = (Doc)entry;
                if (doc.isClass() || doc.isInterface()) {
                    if (doc.tags(SUPPORTED_ALL_API).length > 0) {
                        includeAll.add(entry.toString());
                    }
                }
            }
        }
    }

    private static boolean toIncludeMe(Doc doc) {
        boolean includeMe = (doc.tags(SUPPORTED_ALL_API).length > 0) ||
            (doc.tags(SUPPORTED_API).length > 0);

        if (!includeMe) {
            String name = doc.toString();
            int idx = name.indexOf('(');
            String className = (idx != -1) ?
                name.substring(0, name.indexOf('(')) : name;

            if (!doc.isConstructor()) {
                className = className.substring(0, className.lastIndexOf('.'));
            }

            includeMe = includeAll.contains(className);
        }

        return includeMe;
    }
 
    private static Object process(Object obj, Class expect) {
        Object retObj = obj;

        if (obj != null) {
            Class cls = obj.getClass();
            if (cls.getName().startsWith("com.sun.")) {
                retObj = Proxy.newProxyInstance(cls.getClassLoader(),
                    cls.getInterfaces(), new StandardHandler(obj));
            } else if (obj instanceof Object[]) {
                Class componentType = expect.getComponentType();
                Object[] array = (Object[])obj;
                setIncludeAll(array);
                List list = new ArrayList(array.length);

                for (int i = 0; i < array.length; i++) {
                    Object entry = array[i];
                    if ((entry instanceof Doc) && !toIncludeMe((Doc)entry)) {
                        continue;
                    }
                    list.add(process(entry, componentType));
                }
                retObj = list.toArray(
                    (Object[])Array.newInstance(componentType, list.size()));
            }
        }
        
        return retObj;
    }

    private static class StandardHandler
        implements InvocationHandler {
        private Object target;
                                                                                
        public StandardHandler(Object target) {
            this.target = target;
        }
                                                                                
        public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
            if (args != null) {
                String methodName = method.getName();
                if (methodName.equals("compareTo") ||
                    methodName.equals("equals") ||
                    methodName.equals("overrides") ||
                    methodName.equals("subclassOf")) {
                    args[0] = unwrap(args[0]);
                }
            }
            try {
                return process(method.invoke(target, args),
                    method.getReturnType());
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }
                                                                                
        private Object unwrap(Object proxy) {
            return (proxy instanceof Proxy) ?
                ((StandardHandler)Proxy.getInvocationHandler(proxy)).target :
                proxy;
        }
    }
}

