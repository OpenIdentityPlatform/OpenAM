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
 * $Id: LogoutViewBean.java,v 1.10 2009/01/16 06:30:13 hengming Exp $
 *
 */

/*
 * Portions Copyrighted [2010-2011] [ForgeRock AS]
 */

package com.sun.identity.authentication.distUI;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.view.event.ChildDisplayEvent;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.StaticTextField;
import com.iplanet.jato.view.View;
import com.iplanet.sso.SSOToken;

import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.client.AuthClientUtils;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.common.ISLocaleContext;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.L10NMessage;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;


/**
 * This class is a default implementation of <code>LogoutViewBean</code> auth 
 * Logout UI.
 */
public class LogoutViewBean 
extends com.sun.identity.authentication.UI.AuthViewBeanBase {
    
    /**
     * Creates <code>LoginViewBean</code> object.
     */
    public LogoutViewBean() {
        super(PAGE_NAME);
        logoutDebug.message("LogoutViewBean() constructor called");
        registerChildren();
    }
    
    /** registers child views */
    protected void registerChildren() {
        super.registerChildren();
        registerChild(URL_LOGIN, StaticTextField.class);
        registerChild(TXT_LOGOUT, StaticTextField.class);
        registerChild(TXT_GOTO_LOGIN_AFTER_LOGOUT, StaticTextField.class);
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
        logoutDebug.message("In forwardTo()");
        
        if (requestContext != null) {
            request = requestContext.getRequest();
            response = requestContext.getResponse();
            servletContext = requestContext.getServletContext();
            session = request.getSession();
        }
        
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        
        gotoUrl = request.getParameter("goto");
        if (logoutDebug.messageEnabled()) {
            logoutDebug.message("Goto query param : " + gotoUrl);
        }
        
        try {            
            cookieSupported = AuthClientUtils.checkForCookies(request);
            clientType = AuthClientUtils.getClientType(request);
            ISLocaleContext localeContext = new ISLocaleContext();
            localeContext.setLocale(request);
            locale = localeContext.getLocale();
            SessionID sessionID = AuthClientUtils.getSessionIDFromRequest(request);
            SSOToken ssoToken = AuthClientUtils.getExistingValidSSOToken(sessionID);

            if (ssoToken != null) {
                ssoTokenExists = true;
                loginURL = (String)ssoToken.getProperty
                    (ISAuthConstants.DISTAUTH_LOGINURL);
                orgName = (String)ssoToken.getProperty
                    (ISAuthConstants.ORGANIZATION);
                String strIndexType = (String)ssoToken.getProperty
                    (ISAuthConstants.INDEX_TYPE);
                if (strIndexType != null) {
                    indexType = AuthClientUtils.getIndexType(strIndexType);
                    indexName = AuthClientUtils.getIndexName(ssoToken, indexType);
                }
            }

            if (ssoTokenExists) {
                AuthContext ac = new AuthContext(ssoToken);
                ac.logoutUsingTokenID();
                logoutDebug.message("logout successfully");
            }

            rb =  rbCache.getResBundle(bundleName, locale);
            ResultVal = rb.getString("logout.successful");
           
            if (logoutDebug.messageEnabled()) {
                logoutDebug.message("Client Type is: " + clientType);
                logoutDebug.message("JSPLocale = " + locale);
                logoutDebug.message("loginURL : " + loginURL);
            }
            
            fallbackLocale = locale;
            rb =  rbCache.getResBundle(bundleName, locale);
            
        } catch (Exception e) {
            logoutDebug.message("Retrieve AuthContext Error : ", e);
            ResultVal = getL10NMessage(e, locale);
        }
        
        if (cookieSupported) {
            logoutDebug.message("Cookie is supported");
            clearAllCookies();
        } else {
            logoutDebug.message("Cookie is not supported");            
            logoutCookie = LOGOUTCOOKIEVAULE;
            if (logoutDebug.messageEnabled()) {
                logoutDebug.message("Logout Cookie is " + logoutCookie);
            }            
        }
        
        // Invalidate HttpSession
        session.invalidate();

        String logoutJspPage = SystemProperties.get(DEFAULT_LOGOUT_PAGE);

        if (logoutJspPage!=null && logoutJspPage.length()!=0) { //default logout page: set - redirecting to the custom logout page
            jsp_page = appendLogoutCookie(getFileName(logoutJspPage));
            if (!ssoTokenExists) {
                if (!isGotoSet()) {
                    try {
                        logoutDebug.message("super.forwardTo showing Logout page");
                        super.forwardTo(requestContext);
                        return;
                    } catch (Exception e) {
                        ResultVal = getL10NMessage(e, locale);
                    }
                }
            }
        } else {
            // get the Logout JSP page path
            jsp_page = appendLogoutCookie(getFileName(LOGOUT_JSP));

            if (!ssoTokenExists) {
                if (!isGotoSet()) {
                    try {
                        if (logoutDebug.messageEnabled()) {
                            logoutDebug.message("AuthContext is NULL");
                            logoutDebug.message("Goto LOGINURL : "+ LOGINURL);
                        }
                        if (doSendRedirect(LOGINURL)) {
                            response.sendRedirect(appendLogoutCookie(LOGINURL));
                            return;
                        } else {
                            jsp_page = appendLogoutCookie(getFileName(LOGIN_JSP));
                        }
                    } catch (Exception e) {
                        ResultVal = getL10NMessage(e, locale);
                    }
                }
            }
        }

        if (!redirectToGoto(locale)) {
            super.forwardTo(requestContext);
        }
        
    }
    
    private String getFileName(String fileName) {
        String relativeFileName = null;
        if (ssoTokenExists) {
            relativeFileName = AuthClientUtils.getFileName(fileName,locale.toString(),
                orgName,request,servletContext,indexType,indexName);
        } else {
            relativeFileName =
            AuthClientUtils.getDefaultFileName(request,fileName,locale,servletContext);
        }
        if (logoutDebug.messageEnabled()) {
            logoutDebug.message("fileName is : " + fileName);
            logoutDebug.message("relativeFileName is : " + relativeFileName);
        }
        
        return relativeFileName;
    }
    
    private void clearAllCookies() {
        Set cookieDomainSet =  AuthClientUtils.getCookieDomains();

        // No cookie domain specified in profile
        if (cookieDomainSet.isEmpty()) {
            clearAllCookiesByDomain(null);
        } else {
            Iterator iter = cookieDomainSet.iterator();
            while (iter.hasNext()) {
                clearAllCookiesByDomain((String)iter.next());
            }
        }
        AuthClientUtils.clearlbCookie(request, response);
        AuthClientUtils.clearHostUrlCookie(response);
        Map serverCookieMap = null;
        if (storeCookies != null &&
            !storeCookies.isEmpty()) {
            for (Iterator it = storeCookies.iterator();
                it.hasNext();){
                String cookieName = (String)it.next();
                AuthClientUtils.clearServerCookie(cookieName, request,
                        response);
            }
        }
    }
    
    private void clearAllCookiesByDomain(String cookieDomain) {
        Cookie cookie = AuthClientUtils.createCookie(LOGOUTCOOKIEVAULE, cookieDomain);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        if (AuthClientUtils.getAuthCookieValue(request) != null) {
            cookie = AuthClientUtils.createCookie(AuthClientUtils.getAuthCookieName(),
                LOGOUTCOOKIEVAULE, cookieDomain);
            cookie.setMaxAge(0);
            response.addCookie(cookie);
        }
    }
    
    /**
     * Returns display url for auth auth Logout UI
     * 
     * @return display url for auth auth Logout  UI
     */
    public String getDisplayURL() {
        if (logoutDebug.messageEnabled()) {
            logoutDebug.message("In getDisplayURL() jsp_page " + jsp_page);
        }
        return jsp_page;
    }
    
    /**
     *
     *
     */
    protected View createChild(String name) {
        if (logoutDebug.messageEnabled()) {
            logoutDebug.message("In createChild() : child name = " + name);
        }
        
        if (name.equals(TXT_LOGOUT)) {
            return new StaticTextField(this, name, ResultVal);
        } else if (name.equals(TXT_GOTO_LOGIN_AFTER_LOGOUT)) {
            return new StaticTextField(this, name, "");
        } else if (name.equals(URL_LOGIN)) {
            if ((loginURL==null)||(loginURL.length() == 0)) {
                loginURL = LOGINURL;
            }
            loginURL = appendLogoutCookie(loginURL);
            return new StaticTextField(this, name, loginURL);
        } else if (name.equals(HTML_TITLE_LOGOUT)) {
            String logoutTitle = rb.getString("htmlTitle_Logout");
            return new StaticTextField(this, name, logoutTitle);
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
     *         display preparation or execution of auto-retrieving models.
     */
    public void beginDisplay(DisplayEvent event)
    throws ModelControlException {
        logoutDebug.message("In beginDisplay()");
        setPageEncoding(request,response);
    }
    
    
    /* returns the url encoded with the logout cookie string */
    
    private String appendLogoutCookie(String url) {
        return AuthClientUtils.addLogoutCookieToURL(
            url,logoutCookie,cookieSupported);
    }
    
    /* Checks if request should use sendRedirect */
    private boolean doSendRedirect(String redirectURL) {
        return        ((redirectURL != null) && (redirectURL.length() != 0)
        && (AuthClientUtils.isGenericHTMLClient(clientType))) ;
        
    }
    
    // Check whether the 'goto' query parameter value exists or not
    private boolean isGotoSet() {
        if ((gotoUrl != null) && (gotoUrl.length() != 0)) {
            return true;
        } else {
            return false;
        }
    }
    
    // Redirect to the 'goto' query parameter value
    private boolean redirectToGoto(java.util.Locale locale) {
        if (isGotoSet()) {
            if (logoutDebug.messageEnabled()) {
                logoutDebug.message("Redirect to 'goto' URL : " + gotoUrl);
            }
            try {
                URL url = new URL(gotoUrl);
            } catch (MalformedURLException murle) {
                if (logoutDebug.warningEnabled()) {
                    logoutDebug.warning("Invalid gotoURL supplied for LogoutViewBean: "
                            + gotoUrl);
                }
                return false;
            }
            try {
                if (!SystemProperties.getAsBoolean(Constants.IGNORE_GOTO_DURING_LOGOUT)) {
                    if (doSendRedirect(gotoUrl)) {
                        response.sendRedirect(appendLogoutCookie(gotoUrl));
                        return true;
                    }
                }
            } catch (Exception e) {
                if (logoutDebug.messageEnabled()) {
                    logoutDebug.message(
                            "'goto' Redirect failed : " + gotoUrl, e);
                }
                ResultVal = getL10NMessage(e, locale);
            }
        }
        return false;
    }
    
    /**
     * Handles href logout request
     * @param event request invocation event
     * @throws ServletException if it fails to forward logout request
     * @throws IOException  if it fails to forward logout request
     */
    public void handleHrefLogoutRequest(RequestInvocationEvent event)
    throws ServletException, IOException {
        //ViewBean targetView = getViewBean(LoginViewBean.class);
        //targetView.forwardTo(getRequestContext());
        forwardTo();
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
     * Returns if it begins href logout display
     * @param event child display event
     * @return <code>true</code> by default.
     */ 
    public boolean beginHrefLogoutDisplay(ChildDisplayEvent event) {
        return true;
    }
    
    /**
     * Returns if it begins content href logout display
     * @param event child display event
     * @return <code>true</code> by default.
     */ 
    public boolean beginContentHrefLogoutDisplay(ChildDisplayEvent event) {
        setDisplayFieldValue(
        TXT_GOTO_LOGIN_AFTER_LOGOUT,
        rb.getString("gotoLoginAfterLogout"));
        return true;
    }
    
    /**
     * Returns if it begins static text logout display
     * @param event child display event
     * @return <code>true</code> by default.
     */ 
    public boolean beginStaticTextLogoutDisplay(ChildDisplayEvent event) {
        return true;
    }
    
    /**
     * Returns tile Index.
     *
     * @return Tile index.
     */
    public String getTileIndex() {
        return "";
    }
    
    /**
     * Returns <code>true</code> to display static text content.
     *
     * @param event Child display event.
     * @return <code>true</code> to display static text content.
     */
    public boolean beginContentStaticTextLogoutDisplay(ChildDisplayEvent event){
        return true;
    }
    
    private String getL10NMessage(Exception e, java.util.Locale locale) {
        if (e instanceof L10NMessage) {
            return ((L10NMessage)e).getL10NMessage(locale);
        } else {
            return e.getMessage();
        }
    }
    
    
    ////////////////////////////////////////////////////////////////////////////
    // Class variables
    ////////////////////////////////////////////////////////////////////////////
    
    /** Default page name */
    public static final String PAGE_NAME="Logout";
    
    static Debug logoutDebug = Debug.getInstance("amLogoutViewBean");
    
    ////////////////////////////////////////////////////////////////////////////
    // Instance variables
    ////////////////////////////////////////////////////////////////////////////
    
    HttpServletRequest request;
    HttpServletResponse response;
    HttpSession session;
    ServletContext servletContext;
    java.util.Locale locale = null;
    String orgName = "";
    String indexName = "";
    AuthContext.IndexType indexType;
    /** Logout result value */
    public String ResultVal = "";
    /** Goto url */
    public String gotoUrl = "";
    /** JSP page */
    public String jsp_page = "";
    private static String LOGINURL = "";
    private String loginURL = "";
    /** Resource bundle for <code>Locale</code> */
    public ResourceBundle rb = null;
    private static final String LOGOUTCOOKIEVAULE = "LOGOUT";
    private String logoutCookie = null;
    private boolean cookieSupported;
    /** Default parameter name for login url */
    public static final String URL_LOGIN = "urlLogin";
    /** Default parameter name for logout text */
    public static final String TXT_LOGOUT = "txtLogout";
    /** Default parameter name for goto login text after logout */
    public static final String TXT_GOTO_LOGIN_AFTER_LOGOUT =
    "txtGotoLoginAfterLogout";
    /** Default parameter name for logout html title */
    public static final String HTML_TITLE_LOGOUT = "htmlTitle_Logout";
    private String clientType=null;
    private static final String LOGOUT_JSP = "Logout.jsp";
    private static final String LOGIN_JSP = "Login.jsp";
    private static final String bundleName = "amAuthUI";
    private boolean ssoTokenExists = false;
    private final static String DEFAULT_LOGOUT_PAGE = "openam.authentication.distUI.defaultLogoutPage";
    
    static {
        LOGINURL = serviceUri + "/UI/Login";
    }
}

