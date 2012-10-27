/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AuthExceptionViewBean.java,v 1.8 2008/12/24 01:42:25 ericow Exp $
 *
 */



package com.sun.identity.authentication.distUI;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.ViewBean;
import com.iplanet.jato.view.event.ChildDisplayEvent;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.StaticTextField;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.common.ISLocaleContext;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.L10NMessage;
import com.sun.identity.authentication.client.AuthClientUtils;
import java.io.IOException;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * A default implementation of <code>ViewBean</code> auth exception UI.
 */
public class AuthExceptionViewBean extends
    com.sun.identity.authentication.UI.AuthViewBeanBase
{
    /**
     * Creates <code>AuthExceptionViewBean</code> object.
     */
    public AuthExceptionViewBean() {
        super(PAGE_NAME);
        exDebug.message("AuthExceptionViewBean() constructor called");
        registerChildren();
    }
    
    /** registers child views */
    protected void registerChildren() {
        super.registerChildren();
        registerChild(URL_LOGIN, StaticTextField.class);
        registerChild(TXT_EXCEPTION, StaticTextField.class);
        registerChild(TXT_GOTO_LOGIN_AFTER_FAIL, StaticTextField.class);
    }
    
    /**
     * Forwards the request to this view bean, displaying the page. This
     * method is the equivalent of <code>RequestDispatcher.forward()</code>,
     * meaning that the same semantics apply to the use of this method.
     * This method makes implicit use of the display URL returned
     * by the <code>getDisplayURL()</code> method.
     * @param requestContext servlet context for auth request
     */
    public void forwardTo(RequestContext requestContext) {
        exDebug.message("In forwardTo()");
        
        if (requestContext != null) {
            request = requestContext.getRequest();
            response = requestContext.getResponse();
            servletContext = requestContext.getServletContext();
            session = request.getSession();
        }
        
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        
        try {
            if (session.isNew()) {
                exDebug.message("New Request");
                ISLocaleContext localeContext = new ISLocaleContext();
                localeContext.setLocale(request);
                locale = localeContext.getLocale();
            } else {
                exDebug.message("Existing request");
                locale = (java.util.Locale) session.getAttribute("Locale");
                ac = (AuthContext) session.getAttribute("AuthContext");
                orgName = (String) session.getAttribute("OrgName");
                indexType =
                    AuthClientUtils.getIndexType((String) session.getAttribute("IndexType"));
                indexName = (String) session.getAttribute("IndexName");
            }
            
            if (exDebug.messageEnabled()) {
                exDebug.message("JSPLocale = " + locale);
                exDebug.message("AuthContext : " + ac);
            }
            session.invalidate();
        } catch (Exception e) {
            if (e instanceof L10NMessage) {
                ResultVal = ((L10NMessage)e).getL10NMessage(locale);
            } else {
                ResultVal = e.getMessage();
            }
        }
        if (ac == null) {
            try {
                if (exDebug.messageEnabled()) {
                    exDebug.message("Goto Login URL : " + LOGINURL);
                }
                response.sendRedirect(LOGINURL);
            } catch (Exception e) {}
        } else {
            super.forwardTo(requestContext);
        }
    }
    
    /**
     * Returns display url for auth exception UI
     * 
     * @return display url for auth exception UI
     */
    public String getDisplayURL() {
        exDebug.message("In getDisplayURL()");
        
        // I18N get resource bundle
        rb = (ResourceBundle)  rbCache.getResBundle("amAuthUI", locale);
        if (rb == null) {
            return AuthClientUtils.getFileName("Exception.jsp",locale.toString(),orgName,
            request,servletContext,indexType,indexName);
        } else {
            return AuthClientUtils.getFileName("authException.jsp",locale.toString(),orgName,
            request,servletContext,indexType,indexName);
        }
    }
    
    
    /**
     *
     *
     */
    protected View createChild(String name) {
        if (exDebug.messageEnabled()) {
            exDebug.message("In createChild() : child name = " + name);
        }
        
        if (name.equals(TXT_EXCEPTION)) {
            return new StaticTextField(this, name, ResultVal);
        } else if (name.equals(TXT_GOTO_LOGIN_AFTER_FAIL)) {
            return new StaticTextField(this, name, "");
        } else if (name.equals(URL_LOGIN)) { // non-cookie support
            String loginURL = AuthClientUtils.encodeURL(LOGINURL, request, ac);
            return new StaticTextField(this, name, loginURL);
        } else if (name.equals(HTML_TITLE_AUTH_EXCEPTION)) {
            String exceptionTitle = rb.getString("htmlTitle_AuthException");
            return new StaticTextField(this, name, exceptionTitle);
        } else {
            return super.createChild(name);
        }
    }
    
    /**
     * Called as notification that the JSP has begun its display 
     * processing. In addition to performing the default behavior in the 
     * superclass's version, this method executes any auto-retrieving or auto-
     * executing models associated with this view unless auto-retrieval is
     * disabled.
     *
     * @param event Display Event.
     * @throws ModelControlException if manipulation of a model fails during
     *         display preparation or execution of auto-retrieving models
     */
    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        SessionID sessionID = AuthClientUtils.getSessionIDFromRequest(request);
        try {
            String cookieDomain = null;
            Set cookieDomainSet = 
                    AuthClientUtils.getCookieDomainsForReq(request);
            Cookie cookie;
            setPageEncoding(request,response);

            // No cookie domain specified in profile
            if (cookieDomainSet.isEmpty()) {
                cookie = AuthClientUtils.getLogoutCookie(sessionID, null);
                response.addCookie(cookie);
            } else {
                Iterator iter = cookieDomainSet.iterator();
                while (iter.hasNext()) {
                    cookieDomain = (String)iter.next();
                    cookie = AuthClientUtils.getLogoutCookie(sessionID, cookieDomain);
                    response.addCookie(cookie);
                }
            }
            AuthClientUtils.clearlbCookie(request, response);
            ResultVal = rb.getString("uncaught_exception");
            
        }catch (Exception e) {
            e.printStackTrace();
            if (exDebug.messageEnabled()) {
                exDebug.message(
                    "error in getting Exception : " + e.getMessage());
            }
            
            ResultVal = rb.getString("uncaught_exception") + " : " +
                e.getMessage();
        }
        
    }
    
    /**
     * Handles href exception request
     * @param event request invocation event.
     * @throws ServletException if it fails to foward request
     * @throws IOException if it fails to foward request
     */
    public void handleHrefExceptionRequest(RequestInvocationEvent event)
    throws ServletException, IOException {
        ViewBean targetView = getViewBean(LoginViewBean.class);
        targetView.forwardTo(getRequestContext());
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Display cycle events:
    // If the fireDisplayEvents attribute in a display field tag is set to true,
    // then the begin/endDisplay events will fire for that display field.
    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * Using the display cycle event to adjust the value of a given field
     *
     */
    /**
     * Returns if it begins href exception display
     * @param event child display event
     * @return <code>true</code> by default
     */ 
    public boolean beginHrefExceptionDisplay(ChildDisplayEvent event) {
        return true;
    }
    
    /**
     * Returns if it begins content href exception display
     * @param event child display event
     * @return <code>true</code> by default.
     */ 
    public boolean beginContentHrefExceptionDisplay(ChildDisplayEvent event) {
        setDisplayFieldValue(
        TXT_GOTO_LOGIN_AFTER_FAIL,
        rb.getString("gotoLoginAfterFail"));
        return true;
    }
    
    /**
     * Returns if it begins static text exception display
     * @param event child display event
     * @return <code>true</code> by default.
     */ 
    public boolean beginStaticTextExceptionDisplay(ChildDisplayEvent event) {
        return true;
    }
    
    /**
     * Returns tile index for auth exception UI.
     * @return default empty string 
     */
    public String getTileIndex() {
        return "";
    }
    
    /**
     * Return <code>true</code> if it begins content static text exception 
     * display.
     * @param event The DisplayEvent
     * @return <code>true</code> if it begins content static text exception 
     * display.
     */
    public boolean beginContentStaticTextExceptionDisplay(
        ChildDisplayEvent event
    ) {
        return true;
    }
    
    
    ////////////////////////////////////////////////////////////////////////////
    // Class variables
    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * Page name for auth exception.
     */
    public static final String PAGE_NAME="AuthException";
    
    static Debug exDebug = Debug.getInstance("amAuthExceptionViewBean");
    
    ////////////////////////////////////////////////////////////////////////////
    // Instance variables
    ////////////////////////////////////////////////////////////////////////////
    
    HttpServletRequest request;
    HttpServletResponse response;
    HttpSession session;
    ServletContext servletContext;
    AuthContext ac = null;
    /**
     * Result value
     */
    public String ResultVal = "";
    private static String LOGINURL = "";
    /**
     * Resource bundle
     */
    public ResourceBundle rb = null;
    private java.util.Locale locale = null;
    String orgName = "";    
    String indexName = "";
    AuthContext.IndexType indexType;
    
    /**
     * Property name for url login.
     */
    public static final String URL_LOGIN = "urlLogin";
    /**
     * Property name for text exception
     */
    public static final String TXT_EXCEPTION = "StaticTextException";
    /**
     * Property name for goto login after failure.
     */
    public static final String TXT_GOTO_LOGIN_AFTER_FAIL =
        "txtGotoLoginAfterFail";
    /**
     * Property name for auth exception UI html title.
     */
    public static final String HTML_TITLE_AUTH_EXCEPTION =
        "htmlTitle_AuthException";
    static {
        LOGINURL = serviceUri + "/UI/Login";
    }
}

