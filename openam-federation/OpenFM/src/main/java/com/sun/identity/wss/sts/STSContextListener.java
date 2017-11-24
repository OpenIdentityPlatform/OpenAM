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
 * $Id: STSContextListener.java,v 1.5 2008/06/25 05:50:12 qcheng Exp $
 *
 */

package com.sun.identity.wss.sts;

import javax.servlet.ServletContextListener;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContext;
import java.net.URLClassLoader;
import com.sun.identity.classloader.FAMClassLoader;
import java.lang.reflect.Method;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

public class STSContextListener
        implements ServletContextAttributeListener, ServletContextListener {
    
    private static Class stsContextListener;
    private static Method ctxDestroyed;
    private static Method ctxInitialized;
    
    
    /** Creates a new instance of STSContextListener */
    public STSContextListener() {       
    }
    
    public void attributeAdded(ServletContextAttributeEvent event) {
    }
 	
    public void attributeRemoved(ServletContextAttributeEvent event) {
    }

    public void attributeReplaced(ServletContextAttributeEvent event) {
    }
    
    public void contextDestroyed(ServletContextEvent event) {
        ServletContext context = event.getServletContext();
        ClassLoader oldcc = Thread.currentThread().getContextClassLoader();
        try {
            ClassLoader cls = FAMClassLoader.getFAMClassLoader(context,null);
            Thread.currentThread().setContextClassLoader(cls);
            stsContextListener = cls.loadClass(
              "com.sun.xml.ws.transport.http.servlet.WSServletContextListener");
            Class clsa[] = new Class[1];
            clsa[0] = Class.forName("javax.servlet.ServletContextEvent");
            ctxDestroyed = 
                 stsContextListener.getDeclaredMethod("contextDestroyed", clsa);
            
            Object contextListener = stsContextListener.newInstance();
            Object args[] = new Object[1];
            args[0] = event;
            ctxDestroyed.invoke(contextListener, args);
        } catch (Throwable ex) {
            ex.printStackTrace();
        } finally {
            Thread.currentThread().setContextClassLoader(oldcc);
        }
        
    }
    
    public void contextInitialized(ServletContextEvent event) {
        ServletContext context = event.getServletContext();
        
        ClassLoader oldcc = Thread.currentThread().getContextClassLoader();
        try {
            ClassLoader cls = FAMClassLoader.getFAMClassLoader(context,null);
            Thread.currentThread().setContextClassLoader(cls);
            stsContextListener = cls.loadClass(
              "com.sun.xml.ws.transport.http.servlet.WSServletContextListener");
            Class clsa[] = new Class[1];
            clsa[0] = Class.forName("javax.servlet.ServletContextEvent");
            ctxInitialized = 
               stsContextListener.getDeclaredMethod("contextInitialized", clsa);
            
            Object contextListener = stsContextListener.newInstance();
            Object args[] = new Object[1];
            args[0] = event;
            ctxInitialized.invoke(contextListener, args);
        } catch (Throwable ex) {
            ex.printStackTrace();
        } finally {
            Thread.currentThread().setContextClassLoader(oldcc);
        }
    }
    
}
