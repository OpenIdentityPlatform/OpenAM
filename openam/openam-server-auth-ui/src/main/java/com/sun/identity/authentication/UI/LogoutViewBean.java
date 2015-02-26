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
 * $Id: LogoutViewBean.java,v 1.15 2009/11/25 11:59:42 manish_rustagi Exp $
 *
 */

/**
 * Portions Copyrighted 2011-2012 ForgeRock Inc
 */
package com.sun.identity.authentication.UI;

import com.iplanet.am.util.SystemProperties;
import java.io.IOException;
import java.util.ResourceBundle;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.identity.shared.debug.Debug;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.ChildDisplayEvent;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.StaticTextField;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOTokenManager;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.ISLocaleContext;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.service.AuthD;
import com.sun.identity.authentication.service.AuthUtils;
import com.sun.identity.authentication.spi.AMPostAuthProcessInterface;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.locale.L10NMessage;
import com.sun.identity.sm.DNMapper;
import java.util.HashMap;
import java.util.Hashtable;

/**
 * This class is a default implementation of <code>LogoutViewBean</code> auth 
 * Logout UI.
 */
public class LogoutViewBean extends AuthViewBeanBase {
    
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
        SessionID sessionID = null;
        SSOToken token = null;
        InternalSession intSess = null;
        java.util.Locale locale = null;
        logoutDebug.message("In forwardTo()");
        if (requestContext!=null) {
            request = requestContext.getRequest();
            response = requestContext.getResponse();
        }
        
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        
        gotoUrl = request.getParameter("goto");
        if (logoutDebug.messageEnabled()) {
            logoutDebug.message("Goto query param : " + gotoUrl);
        }
        
        try {
            sessionID = new SessionID(request);
            intSess = AuthD.getSession(sessionID);
            if (intSess != null) {
                populateL10NFileAttrs(intSess);
                String localeStr =  intSess.getProperty(ISAuthConstants.LOCALE);
                // I18N get resource bundle
                locale = com.sun.identity.shared.locale.Locale.getLocale(localeStr);
                fallbackLocale = locale;
            } else {
                ISLocaleContext localeContext = new ISLocaleContext();
                localeContext.setLocale(request);
                locale = localeContext.getLocale();
                if (locale == null) {
                    String localeStr = AuthD.getAuth().getPlatformLocale();
                    locale = com.sun.identity.shared.locale.Locale.getLocale(localeStr);
                }
            }
                
            rb = (ResourceBundle)  rbCache.getResBundle("amAuthUI", locale);
            clientType = AuthUtils.getClientType(request);
            if (logoutDebug.messageEnabled()) {
                logoutDebug.message("clienttype is : " + clientType);
            }
            token = SSOTokenManager.getInstance().
                createSSOToken(sessionID.toString());
        } catch (Exception e) {
            ResultVal = getL10NMessage(e, locale);
        }
        
        // Get the Login URL and query map
        if (token != null) {
            try {
                loginURL = token.getProperty(ISAuthConstants.FULL_LOGIN_URL);
            } catch (com.iplanet.sso.SSOException ssoExp) {
                if (logoutDebug.messageEnabled()) {
                    logoutDebug.message("LogoutViewBean.forwardTo: "
                        + " Cannot get Login URL");
                }
            }
        }

        // If there is a gotoUrl value and the orgDN is null do some additional processing
        if (orgDN == null && isGotoSet()) {
            if (logoutDebug.messageEnabled()) {
                logoutDebug.message("OrgDN was null, getting from request for goto validation");
            }
            // First check if there is a org parameter in request, for example realm=/sub-realm
            String orgParm = AuthUtils.getOrgParam(AuthUtils.parseRequestParameters(request));
            if (orgParm == null) {
                if (logoutDebug.messageEnabled()) {
                    logoutDebug.message("Attempting to get orgDN from AuthUtils for serverName " + request.getServerName());
                }
                orgDN = AuthUtils.getOrganizationDN(request.getServerName(), true, request);
            } else {
                if (logoutDebug.messageEnabled()) {
                    logoutDebug.message("Attempting to get orgDN from AuthUtils for orgParm " + orgParm);
                }
                orgDN = AuthUtils.getOrganizationDN(orgParm, true, request);
            }
            if (orgDN == null) {
                // Last resort, get it from the root domain
                orgDN = DNMapper.orgNameToDN("/");
            }
        }
        if (isGotoSet()) {
            gotoUrl = AuthUtils.getValidGotoURL(request, orgDN);
            if (logoutDebug.messageEnabled()) {
                logoutDebug.message("Goto after validation for orgDN: " + orgDN + " gotoUrl: " + gotoUrl);
            }
        }
        
        // set the cookie Value or set the logoutcookie string in
        // the case of URL rewriting otherwise set in the responsed
        // header
        Cookie[] cookieArr = request.getCookies();
        if ((cookieArr != null) && (cookieArr.length != 0)) {
            cookieSupported = true;
        } else {
            cookieSupported = false;
        }
        if (cookieSupported) {
            logoutDebug.message("Cookie is supported");
            AuthUtils.clearAllCookies(request, response);
        } else {
            logoutDebug.message("Cookie is not supported");
            if ( (sessionID != null) && (sessionID.toString().length() != 0)) {
                logoutCookie = AuthUtils.getLogoutCookieString(sessionID);
                if (logoutDebug.messageEnabled()) {
                    logoutDebug.message("Logout Cookie is " + logoutCookie);
                }
            }
        }
        
        // get the Logout JSP page path
        jsp_page = appendLogoutCookie(getFileName(LOGOUT_JSP));
        if ((intSess != null) && intSess.isTimedOut()) {
            try {
                if (logoutDebug.messageEnabled()) {
                    logoutDebug.message("Goto Login URL : " + loginURL);
                }
                
                if (doSendRedirect(loginURL)) {
                    response.sendRedirect(appendLogoutCookie(loginURL));
                    return;
                } else {
                    int queryIndex = loginURL.indexOf("?");
                    String qString = null;
                    if (queryIndex != -1) {
                        qString = loginURL.substring(queryIndex);
                    }
                    if (qString != null) {
                        jsp_page = appendLogoutCookie(
                            getFileName(LOGIN_JSP)+qString);
                    } else {
                        jsp_page = appendLogoutCookie(getFileName(LOGIN_JSP));
                    }
                }
            } catch (Exception e) {
                if (logoutDebug.messageEnabled()) {
                    logoutDebug.message("Redirect failed : " + loginURL, e);
                }
                ResultVal = getL10NMessage(e, locale);
            }
            super.forwardTo(requestContext);
            return;
        }
        boolean wasTokenValid = false;
        
        try {
            wasTokenValid = AuthUtils.logout(intSess, token, request, response);
            ResultVal = rb.getString("logout.successful");
                                         
            String postProcessURL =
                AuthUtils.getPostProcessURL(request, AMPostAuthProcessInterface.POST_PROCESS_LOGOUT_URL);

            if (postProcessURL != null) {
                gotoUrl = postProcessURL;
            }
        } catch (SSOException ssoe) {
            try {
                if (logoutDebug.messageEnabled()) {
                    logoutDebug.message("Exception during logout", ssoe);
                    logoutDebug.message("Goto Login URL : " + LOGINURL);
                }
                
                if (doSendRedirect(LOGINURL)) {
                    response.sendRedirect(appendLogoutCookie(LOGINURL));
                    return;
                } else {
                    jsp_page = appendLogoutCookie(getFileName(LOGIN_JSP));
                }
            } catch (Exception ex) {
                if (logoutDebug.messageEnabled()) {
                    logoutDebug.message("Redirect failed:" + LOGINURL, ex);
                }
                
                ResultVal = ex.getMessage();
            }
            
            super.forwardTo(requestContext);
            return;
        }

        if (!wasTokenValid) {
            if (!isGotoSet()) {
                String originalRedirectURL = AuthUtils.getOrigRedirectURL(
                    request,sessionID);
                if (originalRedirectURL != null) {
                    try {
                        if (logoutDebug.messageEnabled()) {
                            logoutDebug.message("Original Redirect URL: " +
                            originalRedirectURL);
                        }
                        int index = originalRedirectURL.indexOf("/Login");
                        if (index != -1) {
                            originalRedirectURL =
                                originalRedirectURL.substring(0,index)
                                + "/Logout";
                        }
                        if (logoutDebug.messageEnabled()) {
                            logoutDebug.message(
                                "Redirect to Original Redirect URL :"
                                + originalRedirectURL);
                        }
                        if (doSendRedirect(originalRedirectURL)) {
                            response.sendRedirect(
                                appendLogoutCookie(originalRedirectURL));
                            return;
                        }
                    } catch (Exception e) {
                        ResultVal = getL10NMessage(e, locale);
                    }
                } else {
                    try {
                        if (logoutDebug.messageEnabled()) {
                            logoutDebug.message(
                                "Goto LOGINURL : "+ LOGINURL);
                        }
                        if (doSendRedirect(LOGINURL)) {
                            response.sendRedirect(
                                appendLogoutCookie(LOGINURL));
                            return;
                        } else {
                            jsp_page = appendLogoutCookie(
                                getFileName(LOGIN_JSP));
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
    
    private void populateL10NFileAttrs(InternalSession intSess) {
        if (intSess != null){
            localeName = intSess.getProperty(ISAuthConstants.LOCALE);
            orgDN = intSess.getClientDomain();
            String serviceName = intSess.getProperty(ISAuthConstants.SERVICE);
            if ((serviceName != null) && (serviceName.length() != 0)) {
                indexType = AuthContext.IndexType.SERVICE;
                indexName = serviceName;
            }
        }
    }
    
    private String getFileName(String fileName) {
        String relativeFileName = null;
        if (orgDN != null) {
            relativeFileName = AuthUtils.getFileName(fileName, localeName,
                orgDN, request, AuthD.getAuth().getServletContext(), 
                indexType, indexName);
        } else {
            relativeFileName = AuthUtils.getDefaultFileName(request,fileName);
        }
        if (logoutDebug.messageEnabled()) {
            logoutDebug.message("fileName is : " + fileName);
            logoutDebug.message("relativeFileName is : " + relativeFileName);
        }
        
        return relativeFileName;
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
        return AuthUtils.addLogoutCookieToURL(url,
        logoutCookie,
        cookieSupported
        );
    }
    
    /* Checks if request should use sendRedirect */
    private boolean doSendRedirect(String redirectURL) {
        return        ((redirectURL != null) && (redirectURL.length() != 0)
        && (AuthUtils.isGenericHTMLClient(clientType))) ;
        
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
    
    String localeName = null;
    String orgDN = null;
    AuthContext.IndexType indexType = null;
    String indexName = null;
    HttpServletRequest request;
    HttpServletResponse response;
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
    
    static {
        LOGINURL = serviceUri + "/UI/Login";
    }
}

