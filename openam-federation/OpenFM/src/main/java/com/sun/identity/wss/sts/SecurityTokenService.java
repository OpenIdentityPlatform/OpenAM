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
 * $Id: SecurityTokenService.java,v 1.4 2008/06/25 05:50:13 qcheng Exp $
 *
 */

package com.sun.identity.wss.sts;

import java.io.*;
import java.net.URLClassLoader;

import javax.servlet.*;
import javax.servlet.http.*;
import com.sun.identity.classloader.FAMClassLoader;
import java.lang.reflect.Method;

public class SecurityTokenService extends HttpServlet {
    
   private static Class jaxwsServlet;
   private static Method doGetMethod;
   private static Method doPostMethod;
   private static Method initMethod;
   private Object wsServlet;
   private static ClassLoader cls;
    
    public void init(ServletConfig config) throws ServletException {
         ClassLoader oldcc = Thread.currentThread().getContextClassLoader();
         super.init(config);
         try {
             if(jaxwsServlet == null) {
                ServletContext context = config.getServletContext();
                cls = FAMClassLoader.getFAMClassLoader(context,null);
                Thread.currentThread().setContextClassLoader(cls);
                jaxwsServlet = cls.loadClass(
                        "com.sun.xml.ws.transport.http.servlet.WSServlet");
           
                Class clsa[] = new Class[2];
                clsa[0] = Class.forName(
                        "javax.servlet.http.HttpServletRequest");
                clsa[1] = Class.forName(
                        "javax.servlet.http.HttpServletResponse");
                doGetMethod = jaxwsServlet.getDeclaredMethod("doGet", clsa);
                doPostMethod = jaxwsServlet.getDeclaredMethod("doPost", clsa);
                clsa = new Class[1];
                clsa[0] = Class.forName("javax.servlet.ServletConfig");                
                initMethod = jaxwsServlet.getDeclaredMethod("init", clsa);
                wsServlet = jaxwsServlet.newInstance();
             }
             Object args[] = new Object[1];
             args[0] = config;
             initMethod.invoke(wsServlet, args);
         } catch (Exception ex) {
             throw new ServletException(ex);
         } catch (Throwable ex) {
             ex.printStackTrace();
         } finally {
             Thread.currentThread().setContextClassLoader(oldcc);
         }
    }        
        
    /**
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        ClassLoader oldcc = Thread.currentThread().getContextClassLoader();
        Object args[] = new Object[2];
        args[0] = request;
        args[1] = response;
        try {
            Thread.currentThread().setContextClassLoader(cls);
            doGetMethod.setAccessible(true);
            doGetMethod.invoke(wsServlet, args);
            doGetMethod.setAccessible(false);
        } catch (Exception ex) {
            throw new ServletException(ex);
        } catch (Throwable ex) {
            ex.printStackTrace();
        } finally {
            Thread.currentThread().setContextClassLoader(oldcc);
        }
        
    }
    
    /**
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        ClassLoader oldcc = Thread.currentThread().getContextClassLoader();
        Object args[] = new Object[2];
        args[0] = request;
        args[1] = response;
        try {
            Thread.currentThread().setContextClassLoader(cls);
            doPostMethod.setAccessible(true);
            doPostMethod.invoke(wsServlet, args);
            doPostMethod.setAccessible(false);
        } catch (Exception ex) {
            throw new ServletException(ex);
        } catch (Throwable ex) {
            ex.printStackTrace();
        } finally {
            Thread.currentThread().setContextClassLoader(oldcc);
        }
    }
    
    /** Returns a short description of the servlet.
     */
    public String getServletInfo() {
        return "Security Token Service Servlet";
    }
    // </editor-fold>
}
