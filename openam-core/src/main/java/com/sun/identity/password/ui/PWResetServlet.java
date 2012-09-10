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
 * $Id: PWResetServlet.java,v 1.2 2008/06/25 05:43:42 qcheng Exp $
 *
 */
/**
 * Portions Copyrighted 2012 ForgeRock Inc
 * Portions Copyrighted 2012 Open Source Solution Technology Corporation
 */
package com.sun.identity.password.ui;

import com.iplanet.jato.ApplicationServletBase;
import com.iplanet.jato.CompleteRequestException;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestContextImpl;
import com.iplanet.jato.ViewBeanManager;
import com.iplanet.jato.view.ViewBean;
import com.sun.identity.common.ISLocaleContext;
import com.sun.identity.password.ui.model.PWResetModelImpl;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * <code>PWResetServlet</code> is the controller Servlet for the
 * Password Reset.
 */
public class PWResetServlet extends ApplicationServletBase implements Constants
{

    /**
     *  Default module URL
     */
    public static final String DEFAULT_MODULE_URL="../ui";

    // All module servlets use this debug object.
    private static Debug debug = Debug.getInstance("amPassword"); 
    private static final String URL_LOCALE = "locale";

    /** 
     * Package name 
     */
    public static String PACKAGE_NAME = getPackageName(
        PWResetServlet.class.getName());

    /**
     * Ignores HTTP session time out.  
     *
     * @param requestContext  The JATO request context.
     */
    protected void onSessionTimeout(RequestContext requestContext)
	throws ServletException {
	// do nothing
    }    

    /** 
     * Constructs a module Servlet for password reset URI 
     */
    public PWResetServlet() {
        super();
    }

    /**
     * Using this callback the character set to be used for
     * decoding POST/GET data will be set.
     * @param requestContext - request context
     * @throws ServletException 
     */
    protected void onBeforeRequest(RequestContext requestContext)
        throws ServletException
    {
        try {
            HttpServletRequest req = requestContext.getRequest();
            HttpSession session = req.getSession(false);
            String sessLocale = null;
            if (session != null) {
                sessLocale = (String) session.getAttribute(URL_LOCALE);
            }
            ISLocaleContext lc = new ISLocaleContext();
            if (sessLocale != null && sessLocale.length() > 0) {
                lc.setLocale(ISLocaleContext.URL_LOCALE, sessLocale);
            }
            lc.setLocale(req);
            String reqCharset = lc.getMIMECharset();
            String reqLocale = lc.getLocale().toString();
            req.setCharacterEncoding(reqCharset);
            if (req.getParameter(URL_LOCALE) != null) {
                if (session == null) {
                    session = req.getSession(true);
                }
                session.setAttribute(URL_LOCALE, reqLocale);
            }
        } catch (UnsupportedEncodingException ex) {
            debug.error("ampassword:encoding error", ex);
        }
    }
    /**
     * Gets the modules URL
     *
     * @return modules URL
     */
    public String getModuleURL(){
        return DEFAULT_MODULE_URL;
    }

    /**
     * Initializes request context
     *
     * @param requestContext  request context
     */
    protected void initializeRequestContext(RequestContext requestContext){
        super.initializeRequestContext(requestContext);
        // Set a view bean manager in the request context.  This must be
        // done at the module level because the view bean manager is

        ViewBeanManager viewBeanManager=
            new ViewBeanManager(requestContext,PACKAGE_NAME);
        ((RequestContextImpl)requestContext).setViewBeanManager(
            viewBeanManager);
    }

    /**
     * Forwards to invalid URL view bean, in case of an invalid target
     * request handler (page).
     *
     * @param requestContext  request context
     * @param handlerName  name of handler
     * @throws ServletException
     */
    protected void onRequestHandlerNotFound(
        RequestContext requestContext,
        String handlerName)
        throws ServletException
    {
        ViewBeanManager viewBeanManager = requestContext.getViewBeanManager();
        ViewBean targetView = viewBeanManager.getViewBean(
            PWResetInvalidURLViewBean.class);
        targetView.forwardTo(requestContext);
        throw new CompleteRequestException();
    }

    /**
     * Forwards to invalid URL view bean, in case of no handler specified
     *
     * @param requestContext  request context
     * @throws ServletException
     */
    protected void onRequestHandlerNotSpecified(RequestContext requestContext)
        throws ServletException
    {
        ViewBeanManager viewBeanManager = requestContext.getViewBeanManager();
        ViewBean targetView = viewBeanManager.getViewBean(
            PWResetInvalidURLViewBean.class);
        targetView.forwardTo(requestContext);
        throw new CompleteRequestException();
    }

    /**
     * Forwards to uncaught exception view bean, to respond to uncaught
     * application error messages.
     *
     * @param requestContext  request context
     * @param e Exception that was not handled by the application.
     * @throws ServletException
     * @throws IOException
     */
    protected void onUncaughtException(
        RequestContext requestContext,
        Exception e)
        throws ServletException, IOException
    {
        PWResetModelImpl.debug.error("PWResetServlet.onUncaughtException", e);
        ViewBeanManager viewBeanManager = requestContext.getViewBeanManager();
        ViewBean targetView = viewBeanManager.getViewBean(
            PWResetUncaughtExceptionViewBean.class);
        targetView.forwardTo(requestContext);
        throw new CompleteRequestException();
    }

    @Override
    protected void onPageSessionDeserializationException(
            RequestContext requestContext,
            ViewBean viewBean,
            Exception e)
            throws ServletException, IOException {
        ViewBeanManager viewBeanManager = requestContext.getViewBeanManager();
        ViewBean targetView = viewBeanManager.getViewBean(
                PWResetInvalidURLViewBean.class);
        targetView.forwardTo(requestContext);
        throw new CompleteRequestException();
    }
}

