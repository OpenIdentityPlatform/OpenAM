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
 * $Id: AuthUtils.java,v 1.33 2009/12/15 16:39:47 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted 2010-2013 ForgeRock, Inc.
 */
package com.sun.identity.authentication.service;

import java.net.URL;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.ResourceBundle;

import java.security.AccessController;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.callback.Callback;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Cookie;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;

import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.iplanet.am.util.Misc;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.shared.Constants;

import com.sun.identity.security.AdminTokenAction;

import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.config.AMAuthLevelManager;
import com.sun.identity.authentication.config.AMAuthConfigUtils;
import com.sun.identity.authentication.server.AuthContextLocal;
import com.sun.identity.authentication.server.AuthXMLRequest;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.authentication.util.AMAuthUtils;
import com.sun.identity.authentication.client.AuthClientUtils;

import com.sun.identity.common.ResourceLookup;

import com.sun.identity.shared.encode.CookieUtils;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.DNMapper;

import com.sun.identity.policy.PolicyUtils;
import com.sun.identity.policy.plugins.AuthLevelCondition;
import com.sun.identity.policy.plugins.AuthSchemeCondition;
import com.sun.identity.policy.plugins.AuthenticateToServiceCondition;
import com.sun.identity.policy.plugins.AuthenticateToRealmCondition;

import com.sun.identity.authentication.spi.AMPostAuthProcessInterface;
import com.sun.identity.shared.encode.Base64;
import java.util.StringTokenizer;

public class AuthUtils extends AuthClientUtils {
    
    public static final String BUNDLE_NAME="amAuth";
    
    /**
     * Authentication type for Realm based authentication after
     * Composite Advices
     */
    public static final int REALM = 1;
    
    /**
     * Authentication type for Service based authentication after 
     * Composite Advices
     */
    public static final int SERVICE = 2;
    
    /**
     * Authentication type for Module based authentication after 
     * Composite Advices
     */
    public static final int MODULE = 3;
    
    
    private static ArrayList pureJAASModuleClasses = new ArrayList();
    private static ArrayList ISModuleClasses = new ArrayList();
    private static Hashtable moduleService = new Hashtable();
    private static ResourceBundle bundle;
    static Debug utilDebug = Debug.getInstance("amAuthUtils");    
    private static String serviceURI = SystemProperties.get(
        Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR) + "/UI/Login";

    /*
     * Private constructor to prevent any instances being created
     */
    private AuthUtils() {
    }
    
    /* retrieve session */
    public static com.iplanet.dpro.session.service.InternalSession
    getSession(AuthContextLocal authContext) {
        
        com.iplanet.dpro.session.service.InternalSession sess =
        getLoginState(authContext).getSession();
        if (utilDebug.messageEnabled()) {
            utilDebug.message("returning session : " + sess);
        }
        return sess;
    }
    
    /* this method does the following
     * 1. initializes authService (AuthD) if not already done.
     * 2. parses the request parameters and stores in dataHash
     * 3. Retrieves the AuthContext object from the global table
     * 4. if this is found then updates the loginState request
     *    type to false and updates the parameter hash table in
     *   loginstate object.
     
     * on error throws AuthException
     */
    
    /**
     * Returns the authentication context for a request.
     *
     * @param request HTTP Servlet Request.
     * @param response HTTP Servlet Response.
     * @param sid SessionID for this request.
     * @param isSessionUpgrade <code>true</code> if session upgrade.
     * @param isBackPost <code>true</code> if back posting.
     * @return authentication context.
     */
    public static AuthContextLocal getAuthContext(
        HttpServletRequest request,
        HttpServletResponse response,
        SessionID sid,
        boolean isSessionUpgrade,
        boolean isBackPost) throws AuthException {
        return getAuthContext(request,response,sid,
            isSessionUpgrade,isBackPost,false);
    }
    
    /**
     * Returns the authentication context for a request.
     *
     * @param request HTTP Servlet Request.
     * @param response HTTP Servlet Response.
     * @param sid SessionID for this request.
     * @param isSessionUpgrade <code>true</code> if session upgrade.
     * @param isBackPost <code>true</code> if back posting.
     * @param isLogout <code>true</code> for logout.
     * @return authentication context.
     */
    public static AuthContextLocal getAuthContext(
        HttpServletRequest request,
        HttpServletResponse response,
        SessionID sid,
        boolean isSessionUpgrade,
        boolean isBackPost,
        boolean isLogout) throws AuthException {
        utilDebug.message("In AuthUtils:getAuthContext");
        Hashtable dataHash;
        AuthContextLocal authContext = null;
        LoginState loginState = null;
        // initialize auth service.
        AuthD ad = AuthD.getAuth();
        
        try {
            dataHash = parseRequestParameters(request);
            // commented this since it debug file
            // has too many messages and making the file large
            // in size.
            authContext = retrieveAuthContext(request, sid);
            if (utilDebug.messageEnabled()) {
                utilDebug.message("AuthUtil:getAuthContext:sid is.. .: " 
                                  + sid);
                utilDebug.message("AuthUtil:getAuthContext:authContext is..: "
                + authContext);
            }

            if(!sid.isNull() && authContext == null && !isSessionUpgrade) {
                String authCookieValue = getAuthCookieValue(request);
                if ((authCookieValue != null) && (authCookieValue.length() != 0)
                        && (!authCookieValue.equalsIgnoreCase("LOGOUT"))) {
                    String cookieURL = null;
                    try {
                        SessionID sessionID = new SessionID(authCookieValue);
                        URL sessionServerURL = 
                            Session.getSessionServiceURL(sessionID);
                        cookieURL = sessionServerURL.getProtocol()
                            + "://" + sessionServerURL.getHost() + ":"
                            + Integer.toString(sessionServerURL.getPort()) 
                            + serviceURI;
                    } catch (SessionException e) {
                        if (utilDebug.messageEnabled()) {
                        	utilDebug.message("AuthUtils:getAuthContext():"
                                + e.toString());
                        }
                    }
                    if (utilDebug.messageEnabled()) {
                    	utilDebug.message("AuthUtils:getAuthContext():"
                            + "cookieURL : " + cookieURL);
                    }
                    if ((cookieURL != null) && (cookieURL.length() != 0) &&
                        (isLocalServer(cookieURL,true))) {
                        utilDebug.error("AuthUtils:getAuthContext(): "
                            + "Invalid Session Timed out");
                        clearAllCookies(request, response);
                        throw new AuthException(
                            AMAuthErrorCode.AUTH_TIMEOUT, null);
                    }            	
                }
            }
            
            if (utilDebug.messageEnabled()) {
                utilDebug.message("isSessionUpgrade  :" + isSessionUpgrade);
                utilDebug.message("BACK with Request method POST : " 
                                  + isBackPost);
            }
            
            if ((authContext == null)  && (isLogout)) {
                return null;
            }
            
            if ((authContext == null) || (isSessionUpgrade) || (isBackPost)) {
                try {
                    loginState = new LoginState();
                    InternalSession oldSession = null;
                    if (sid != null) {
                        oldSession = AuthD.getSession(sid);
                        loginState.setOldSession(oldSession);
                    }
                    if (isSessionUpgrade) {
                        loginState.setOldSession(oldSession);
                        loginState.setSessionUpgrade(isSessionUpgrade);
                    } else if (isBackPost) {
                        loginState.setOldSession(oldSession);
                    }
                    authContext =
                    loginState.createAuthContext(request,response,sid,dataHash);
                    authContext.setLoginState(loginState);
                    String queryOrg =
                    getQueryOrgName(request,getOrgParam(dataHash));
                    if (utilDebug.messageEnabled()) {
                        utilDebug.message("query org is .. : "+ queryOrg);
                    }
                    loginState.setQueryOrg(queryOrg);
                } catch (AuthException ae) {
                    utilDebug.message("Error creating AuthContextLocal : ");
                    if (utilDebug.messageEnabled()) {
                        utilDebug.message("Exception " , ae);
                    }
                    throw new AuthException(ae);
                }
            } else {
                utilDebug.message("getAuthContext: found existing request.");
                
                authContext = processAuthContext(authContext,request,
				response,dataHash,sid);
				loginState = getLoginState(authContext);
				loginState.setRequestType(false);
            }
            
        } catch (Exception ee) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Error creating AuthContextLocal : " 
                                  + ee.getMessage());
            }
            
            throw new AuthException(ee);
        }
        return authContext;
        
    }
    
    
    // processAuthContext checks for arg=newsession in the HttpServletRequest
    // if request has arg=newsession then destroy session and create a new
    // AuthContextLocal object.
    
    static AuthContextLocal processAuthContext(AuthContextLocal authContext,
    HttpServletRequest request,
    HttpServletResponse response,
    Hashtable dataHash,
    SessionID sid) throws AuthException {
        // initialize auth service.
        AuthD ad = AuthD.getAuth();
        
        LoginState loginState = getLoginState(authContext);
        com.iplanet.dpro.session.service.InternalSession sess = null;
        
        if (utilDebug.messageEnabled()) {
            utilDebug.message("in processAuthContext authcontext : " 
                + authContext );
            utilDebug.message("in processAuthContext request : " + request);
            utilDebug.message("in processAuthContext response : " + response);
            utilDebug.message("in processAuthContext sid : " + sid);
        }
        
        if (newSessionArgExists(dataHash, sid) &&
        (loginState.getLoginStatus() == LoginStatus.AUTH_SUCCESS)) {
            // destroy auth context and create new one.
            utilDebug.message("newSession arg exists");
            destroySession(loginState);
            try{
                loginState = new LoginState();
                authContext = loginState.createAuthContext(request,response,
                sid,dataHash);
                authContext.setLoginState(loginState);
                String queryOrg =
                getQueryOrgName(request,getOrgParam(dataHash));
                if (utilDebug.messageEnabled()) {
                    utilDebug.message("query org is .. : "+ queryOrg);
                }
                loginState.setQueryOrg(queryOrg);
            } catch (AuthException ae) {
                utilDebug.message("Error creating AuthContextLocal");
                if (utilDebug.messageEnabled()) {
                    utilDebug.message("Exception " , ae);
                }
                throw new AuthException(AMAuthErrorCode.AUTH_ERROR, null);
            }
        } else {
            boolean multipleTabsUsed =
                Boolean.valueOf(SystemPropertiesManager.get(
                     Constants.MULTIPLE_TABS_USED, "false")).booleanValue();
            if (ad.debug.messageEnabled()) {
                ad.debug.message("AuthUtils .processAuthContext()."
                    + Constants.MULTIPLE_TABS_USED+"="+multipleTabsUsed);
            }
            /**
             * This flag indicates that the same user is running the auth login
             * process in mutiple tabs of the same browser and if the auth
             * is zero user intervention custom auth module using Redirect
             * Callback, then there would be a situation that the same 
             * authContext is being used by mutiple threads running the
             * auth process, so avoid this mutiple thread interference keep 
             * the process in this while loop until all the submit requirements
             * have been met. This is a specific customer use case.
             */
            if (multipleTabsUsed) {
                 while (authContext.submittedRequirements()) {
                     ad.debug.error("Currently processing submit Requirements");
                     if (ad.debug.messageEnabled()) {
                        ad.debug.message("watiting for submittedRequirements() "
                        +"to complete.");
                     }
                 }
            } else {
                if (authContext.submittedRequirements()) {
                    ad.debug.error("Currently processing submit Requirements");
                    throw new AuthException(
                             AMAuthErrorCode.AUTH_TOO_MANY_ATTEMPTS, null);
                }
            }
            // update loginState - requestHash , sess
            utilDebug.message("new session arg does not exist");
            loginState.setHttpServletRequest(request);
            loginState.setHttpServletResponse(response);
            loginState.setParamHash(dataHash);
            sess = ad.getSession(sid);
            if (utilDebug.messageEnabled()) {
                utilDebug.message("AuthUtil :Session is .. : " + sess);
            }
            loginState.setSession(sess);
            loginState.persistentCookieArgExists();
            loginState.setRequestLocale(request);
            if (checkForCookies(request)) {
                loginState.setCookieDetect(false);
            }
        }
        return authContext;
    }
    
    public static LoginState getLoginState(AuthContextLocal authContext) {
        
        LoginState loginState = null;
        if (authContext != null) {
            loginState = authContext.getLoginState();
        }
        return loginState;
    }       
   
    public static Hashtable getRequestParameters(AuthContextLocal authContext) {
        LoginState loginState = getLoginState(authContext);
        if (loginState != null) {
            return loginState.getRequestParamHash();
        } else {
            return new Hashtable();
        }
    }
    
    // retrieve the sid from the LoginState object
    public static String getSidString(AuthContextLocal authContext)
    throws AuthException {
        com.iplanet.dpro.session.service.InternalSession sess = null;
        String sidString = null;
        try {
            if (authContext != null) {
                LoginState loginState = authContext.getLoginState();
                if (loginState != null) {
                    SessionID sid = loginState.getSid();
                    if (sid != null) {
                        sidString = sid.toString();
                    }
                }
            }
        } catch (Exception  e) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Error retreiving sid.. :" + e.getMessage());
            }
            // no need to have error code since the method where this is called
            // generates AUTH_ERROR
            throw new AuthException("noSid", new Object[] {e.getMessage()});
        }
        return sidString;
    }
    
    /**
     * Returns the Cookie object created based on the cookie name,
     * Session ID and cookie domain. If Session is in invalid State then
     * cookie is created with authentication cookie name , if
     * Active/Inactive Session state AM Cookie Name will be used to create
     * cookie.
     *
     * @param ac the AuthContext object
     *@param cookieDomain the cookie domain for creating cookie
     * @return Cookie object.
     */
    public static Cookie getCookieString(AuthContextLocal ac,String cookieDomain) {
        
        Cookie cookie=null;
        String cookieName = getCookieName();
        try {
            String sidString= getSidString(ac);
            LoginState loginState = getLoginState(ac);
            if (loginState != null && loginState.isSessionInvalid()) {
                cookieName = getAuthCookieName();
                utilDebug.message("Create AM AUTH cookie");
            }
            cookie = createCookie(cookieName,sidString,cookieDomain);
            if (CookieUtils.isCookieSecure()) {
                cookie.setSecure(true);
            }
        } catch (Exception e) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Error getting sid : " + e.getMessage());
            }
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("Cookie is : " + cookie);
        }
        return cookie;
    }
    
    /**
     * Returns the Logout cookie.
     *
     * @param ac the AuthContextLocal object
     * @param cookieDomain the cookieDomain
     * @return Logout cookie .
     */
    public static Cookie getLogoutCookie(AuthContextLocal ac, String cookieDomain) {
        LoginState loginState = getLoginState(ac);
        SessionID sid = loginState.getSid();
        String logoutCookieString = getLogoutCookieString(sid);
        Cookie logoutCookie = createCookie(logoutCookieString,cookieDomain);
        logoutCookie.setMaxAge(0);
        return logoutCookie;
    }
    
     /*
     * Return logout url from LoginState object.
     * Caller should check for possible null value returned.
     */
    public String getLogoutURL(AuthContextLocal authContext) {

        try {
            LoginState loginState = getLoginState(authContext);
            if (loginState == null) {
                // No default URL in case of logout. Taken care by LogoutBean.
                return null;
            }

            String logoutURL = loginState.getLogoutURL();

            if (utilDebug.messageEnabled()) {
                if (logoutURL != null)
                    utilDebug.message("AuthUtils: getLogoutURL : " + logoutURL);
                else
                    utilDebug.message("AuthUtils: getLogoutURL : null");
            }

            return logoutURL;

        } catch (Exception e) {
            utilDebug.message("Exception " , e);
            return null;
        }
    }
    // returns true if request is new else false.    
    public static boolean isNewRequest(AuthContextLocal ac) {
        
        LoginState loginState = getLoginState(ac);
        if (loginState.isNewRequest()) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("this is a newRequest");
            }
            return true;
        } else {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("this is an existing request");
            }
            return false;
        }
    }

    public static String constructOrigURL(HttpServletRequest request) {
        String requestURI = (String) request.getAttribute("javax.servlet.forward.request_uri");
        if (requestURI != null) {
            String queryString = (String) request.getAttribute("javax.servlet.forward.query_string");
            return Base64.encode((requestURI + '?' + queryString).getBytes());
        } else {
            return Base64.encode((request.getRequestURI() + '?' + request.getQueryString()).getBytes());
        }
    }

    /* return the successful login url */
    public static String getLoginSuccessURL(AuthContextLocal authContext) {
        String successURL = null;
        LoginState loginState = getLoginState(authContext);
        if (loginState == null) {
            successURL = AuthD.getAuth().defaultSuccessURL;
        } else {
            successURL = getLoginState(authContext).getSuccessLoginURL();
        }
        return successURL;
    }
    
    /* return the failed login url */
    public static String getLoginFailedURL(AuthContextLocal authContext) {
        
        try {
            LoginState loginState = getLoginState(authContext);
            if (loginState == null) {
                return AuthD.getAuth().defaultFailureURL;
            }
            String loginFailedURL=loginState.getFailureLoginURL();
            if (utilDebug.messageEnabled()) {
                utilDebug.message("AuthUtils: getLoginFailedURL "
                                  + loginFailedURL);
            }
            
            // remove the loginstate/authContext from the hashtable
            //removeLoginStateFromHash(authContext);
            //	destroySession(authContext);
            return loginFailedURL;
        } catch (Exception e) {
            utilDebug.message("Exception " , e);
            return null;
        }
    }
    
    
    /* return filename  - will use FileLookUp API
     * for UI only - this returns the relative path
     */
    public static String getFileName(AuthContextLocal authContext,String fileName) {
        
        LoginState loginState = getLoginState(authContext);
        String relFileName = null;
        if (loginState != null) {
            relFileName =
            getLoginState(authContext).getFileName(fileName);
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("getFileName:AuthUtilsFile name is :"
            + relFileName);
        }
        return relFileName;
    }
    
    public static boolean getInetDomainStatus(AuthContextLocal authContext) {
        return getLoginState(authContext).getInetDomainStatus();
    }
    
    public static boolean newSessionArgExists(
        Hashtable dataHash, SessionID sid) {

        String arg = (String)dataHash.get("arg");
        if (arg != null && arg.equals("newsession")) {
            if (retrieveAuthContext(sid) != null) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }
    
    public static String encodeURL(String url,
    AuthContextLocal authContext,
    HttpServletResponse response) {
        if (utilDebug.messageEnabled()) {
            utilDebug.message("AuthUtils:input url is :"+ url);
        }
        LoginState loginState = getLoginState(authContext);
        String encodedURL;
        
        if (loginState==null) {
            encodedURL = url;
        } else {
            encodedURL = loginState.encodeURL(url,response);
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("AuthUtils:encoded url is :"+encodedURL);
        }
        
        return encodedURL;
    }
    
    // return the locale
    public static String getLocale(AuthContextLocal authContext) {
        // initialize auth service.
        AuthD ad = AuthD.getAuth();
        
        if (authContext == null) {
            return  ad.getPlatformLocale();
        }
        
        LoginState loginState = getLoginState(authContext);
        if (loginState == null) {
            return ad.getPlatformLocale();
        }
        
        return loginState.getLocale();
    }   
   
    static void destroySession(LoginState loginState) {
        try {
            if (loginState != null) {
                loginState.destroySession();
            }
        } catch (Exception e)  {
            utilDebug.message("Error destroySEssion : " , e);
        }
    }
    
    public static void destroySession(AuthContextLocal authContext) {
        if (authContext != null) {
            LoginState loginState = getLoginState(authContext);
            destroySession(loginState);
        }
    }    
   
    /**
     * Returns <code>true</code> if the session has timed out or the page has
     * timed out.
     *
     * @param authContext the authentication context object for the request.
     * @return <code>true</code> if timed out else false.
     */
    public static boolean sessionTimedOut(AuthContextLocal authContext) {
        boolean timedOut = false;
        
        LoginState loginState = getLoginState(authContext);
        
        if (loginState != null) {
            timedOut = loginState.isTimedOut();
            
            if (!timedOut) {
                InternalSession sess = loginState.getSession();
                if (sess != null) {
                    timedOut = sess.isTimedOut();
                }
                loginState.setTimedOut(timedOut);
            }
            
            if (utilDebug.messageEnabled()) {
                utilDebug.message("AuthUtils.sessionTimedOut: " + timedOut);
            }
        }
        return timedOut;
    }    
   
    /* return the value of argument iPSPCookie entered on the URL */
    public static boolean isPersistentCookieOn(AuthContextLocal authContext) {
        return getLoginState(authContext).isPersistentCookieOn();
    }
    
    /* retrieve persistent cookie setting from core auth profile */
    public static boolean getPersistentCookieMode(AuthContextLocal authContext) {
        return getLoginState(authContext).getPersistentCookieMode();
    }

    public static boolean isZeroPageLoginEnabled(AuthContextLocal authContext) {
        return getLoginState(authContext).isZeroPageLoginEnabled();
    }
    
    /* return persistent cookie */
    public static Cookie getPersistentCookieString(AuthContextLocal authContext,
    String cookieDomain ) {
        return null;
    }
    
    /* returns the username from the persistent cookie */
    public static String searchPersistentCookie(AuthContextLocal authContext) {
        LoginState loginState = getLoginState(authContext);
        return loginState.searchPersistentCookie();
    }
    
    public static Cookie createPersistentCookie(AuthContextLocal authContext,
    String cookieDomain) throws AuthException {
        Cookie pCookie=null;
        try {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("cookieDomain : " + cookieDomain);
            }
            LoginState loginState = getLoginState(authContext);
            pCookie = loginState.setPersistentCookie(cookieDomain);
            return pCookie;
        } catch (Exception e) {
            utilDebug.message("Unable to create persistent Cookie");
            throw new AuthException(AMAuthErrorCode.AUTH_ERROR, null);
        }
    }
    
    public static Cookie createlbCookie(AuthContextLocal authContext,
    String cookieDomain, boolean persist) throws AuthException {
        Cookie lbCookie=null;
        try {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("cookieDomain : " + cookieDomain);
            }
            LoginState loginState = getLoginState(authContext);
            lbCookie = loginState.setlbCookie(cookieDomain, persist);
            return lbCookie;
        } catch (Exception e) {
            utilDebug.message("Unable to create Load Balance Cookie");
            throw new AuthException(AMAuthErrorCode.AUTH_ERROR, null);
        }
        
    }
    
    public static void setlbCookie(AuthContextLocal authContext,
            HttpServletRequest request, HttpServletResponse response)
            throws AuthException {
        String cookieName = getlbCookieName();
        if (cookieName != null && cookieName.length() != 0) {
            Set domains = getCookieDomainsForReq(request);
            if (!domains.isEmpty()) {
                for (Iterator it = domains.iterator(); it.hasNext(); ) {
                    String domain = (String)it.next();
                    Cookie cookie = createlbCookie(authContext, domain, false);
                    CookieUtils.addCookieToResponse(response, cookie);
                }
            } else {
                CookieUtils.addCookieToResponse(response, 
                        createlbCookie(authContext, null, false));
            }
        }
    }     
  
    /**
     * called by UI if the username returned by
     * searchPersistentCookie is null
     * clear persistent cookie  in the request
     */
    public static Cookie clearPersistentCookie(String cookieDomain,
    AuthContextLocal authContext) {
        String pCookieValue = LoginState.encodePCookie();
        int maxAge = 0;
        
        Cookie clearPCookie = createPersistentCookie(getPersistentCookieName(),
        pCookieValue,maxAge,cookieDomain);
        
        return clearPCookie;
    }    
   
    /* return the indexType for this request */
    public static int getCompositeAdviceType(AuthContextLocal authContext) {
        int type = 0;
        try {            
            LoginState loginState = getLoginState(authContext);            
            if (loginState != null) {
                type = loginState.getCompositeAdviceType();
            }
            if (utilDebug.messageEnabled()) {
                utilDebug.message("in getCompositeAdviceType, type : " + type);
            }            
        } catch (Exception e) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Error in getCompositeAdviceType : " 
                    + e.toString());
            }
        }
        return type;
    }
    
    /* return the indexType for this request */
    public static AuthContext.IndexType getIndexType(AuthContextLocal authContext) {
        
        try {
            AuthContext.IndexType indexType = null;
            LoginState loginState = getLoginState(authContext);
            
            if (loginState != null) {
                indexType = loginState.getIndexType();
            }
            if (utilDebug.messageEnabled()) {
                utilDebug.message("in getIndexType, index type : " + indexType);
            }
            return indexType;
        } catch (Exception e) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Error in getIndexType : " + e.toString());
            }
            return null;
        }
    }
    
    /* return the indexName for this request */
    public static String getIndexName(AuthContextLocal authContext) {
        
        try {
            String indexName = null;
            LoginState loginState = getLoginState(authContext);
            
            if (loginState != null) {
                indexName = loginState.getIndexName();
            }
            if (utilDebug.messageEnabled()) {
                utilDebug.message("in getIndexName, index Name : " + indexName);
            }
            return indexName;
        } catch (Exception e) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Error in getIndexName : " + e.toString());
            }
            return null;
        }
    }
    
    public static Callback[] getRecdCallback(AuthContextLocal authContext) {
        LoginState loginState = getLoginState(authContext);
        Callback[] recdCallback = null;
        if (loginState != null) {
            recdCallback = loginState.getRecdCallback();
        }
        
        if ( recdCallback != null ) {
            if (utilDebug.messageEnabled()) {
                for (int i = 0; i < recdCallback.length; i++) {
                    utilDebug.message("in getRecdCallback, recdCallback[" 
                                      + i + "] :" + recdCallback[i]);
                }
            }
        }
        else {
            utilDebug.message("in getRecdCallback, recdCallback is null");
        }
        
        return recdCallback;
    }    
    
    /**
     * Returns the resource based on the default values.
     *
     * @param request HTTP Servlet Request.
     * @param fileName name of the file
     * @return Path to the resource.
     */
    public static String getDefaultFileName(
        HttpServletRequest request,
        String fileName) {
        // initialize auth service.
        AuthD ad = AuthD.getAuth();
        
        String locale = ad.getPlatformLocale();
        String filePath = getFilePath(getClientType(request));
        String fileRoot = getFileRoot();
        String orgDN;
        try {
            orgDN = getDomainNameByRequest(request, parseRequestParameters(request));
        } catch (Exception ex) {
            //in case we are unable to determine the realm from the incoming
            //requests, let's fallback to top level realm
            orgDN = getOrganizationDN("/", false, request);
        }
        String orgFilePath = getOrgFilePath(orgDN);
        String templateFile = null;
        try {
            templateFile = ResourceLookup.getFirstExisting(
            ad.getServletContext(),
            fileRoot,locale,orgFilePath,filePath,fileName,
            templatePath,true);
        } catch (Exception e) {
            templateFile = new StringBuffer().append(templatePath)
            .append(fileRoot).append(Constants.FILE_SEPARATOR)
            .append(fileName).toString();
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("getDefaultFileName:templateFile is :" +
            templateFile);
        }
        return templateFile;
    }
    
    /* returns the orgDN for the request */
    public static String getOrgDN(AuthContextLocal authContext) {
        String orgDN = null;
        LoginState loginState = getLoginState(authContext);
        if (loginState != null) {
            orgDN = loginState.getOrgDN();
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("orgDN is : " + orgDN);
        }
        return orgDN;
    }
    
    /* create auth context for org */
    public static AuthContextLocal getAuthContext(String orgName)
    throws AuthException {
        return getAuthContext(orgName,"0",false, null);
    }
    
    public static AuthContextLocal getAuthContext(String orgName,
    String sessionID) throws AuthException {
        return getAuthContext(orgName,sessionID,false, null);
    }
    
    public static AuthContextLocal getAuthContext(String orgName,
    HttpServletRequest req) throws AuthException {
        return getAuthContext(orgName, "0", false, req);
    }
    
    public static AuthContextLocal getAuthContext(String orgName,
    String sessionID, boolean logout) throws AuthException {
        return getAuthContext(orgName, sessionID, logout, null);
    }
    
    public static AuthContextLocal getAuthContext(HttpServletRequest req,
    String sessionID) throws AuthException {
        return getAuthContext(null, sessionID, false, req);
    }
    
    /** Returns the AuthContext Handle for the Request.
     *  @param orgName OrganizationName in request
     *  @param sessionID Session ID for this request
     *  @param isLogout a boolean which is true if it is a Logout request
     *  @param req HttpServletRequest
     *  @return AuthContextLocal object
     */
    public static AuthContextLocal getAuthContext(String orgName,
    String sessionID, boolean isLogout, HttpServletRequest req)
    throws AuthException {
        return getAuthContext(orgName, sessionID, isLogout, req, null, null);
    }
    
    /* create auth context for org  and sid, if sessionupgrade then
     * save the previous authcontext and create new authcontext
     * orgName - organization name to login to
     * sessionId - sessionID of the request - "0" if new request
     * isLogout - is this a logout request 
     *  @param orgName OrganizationName in request
     *  @param sessionID Session ID for this request
     *  @param isLogout a boolean which is true if it is a Logout request
     *  @param req HttpServletRequest
     *  @param indexType Index Type
     *  @param indexName Index Name
     *  @return AuthContextLocal object
     */
    public static AuthContextLocal getAuthContext(String orgName,
        String sessionID, boolean isLogout, HttpServletRequest req,
        String indexType, AuthXMLRequest xmlReq)
        throws AuthException {
        return getAuthContext(orgName, sessionID, isLogout, req,indexType,
            xmlReq,false);
    }

    /* create auth context for org  and sid, if sessionupgrade then
     * save the previous authcontext and create new authcontext
     * orgName - organization name to login too
     * sessionId - sessionID of the request - "0" if new request
     * isLogout - is this a logout request - if yes then no session
     * upgrade  - this is the case where session is VALID so need
     * to use this flag to determine if session upgrade is needed.
     * this is used mainly for Logout/Abort.
     *  @param orgName OrganizationName in request
     *  @param sessionID Session ID for this request
     *  @param isLogout a boolean which is true if it is a Logout request
     *  @param req HttpServletRequest
     *  @param indexType Index Type
     *  @param indexName Index Name
     *  @param forceAuth force auth flag
     *  @return AuthContextLocal object
     */
    public static AuthContextLocal getAuthContext(String orgName,
        String sessionID, boolean isLogout, HttpServletRequest req,
        String indexType,  AuthXMLRequest xmlReq, boolean forceAuth)
        throws AuthException {
        AuthContextLocal authContext = null;
        SessionID sid = null;
        com.iplanet.dpro.session.service.InternalSession sess = null;
        LoginState loginState = null;
        boolean sessionUpgrade = false;
        AuthD ad = AuthD.getAuth();
        int sessionState = -1;
        SSOToken ssot = null;
        String indexName = null;
        if (xmlReq != null) {
            indexName = xmlReq.getIndexName();
        }
        
        if (utilDebug.messageEnabled()) {
            utilDebug.message("orgName : " + orgName);
            utilDebug.message("sessionID is " + sessionID);
            utilDebug.message("sessionID is " + sessionID.length());
            utilDebug.message("isLogout : " + isLogout);
        }
        try {
            if ((sessionID != null) && (!sessionID.equals("0"))) {
                sid = new SessionID(sessionID);
                authContext = retrieveAuthContext(req, sid);
                
                // check if this sesson id is active, if yes then it
                // is a session upgrade case.
                loginState = getLoginState(authContext);
                if (loginState != null) {
                    sess = loginState.getSession();
                } else {
                    sess = AuthD.getSession(sessionID);
                }
                if (sess == null) {
                    sessionUpgrade = false;
                } else {
                    sessionState = sess.getState();
                    if (utilDebug.messageEnabled()) {
                        utilDebug.message("sid from sess is : " + sess.getID());
                        utilDebug.message("sess is : " + sessionState);
                    }
                    if (!((sessionState == Session.INVALID)  || (isLogout))) {
                        ssot = AuthUtils.
                            getExistingValidSSOToken(sid);
                        if ((indexType != null) && (indexName != null)) {
                            Hashtable indexTable = new Hashtable();
                            indexTable.put(indexType, indexName);
                            if (forceAuth) {
                                sessionUpgrade = true;
                            } else {
                                sessionUpgrade = checkSessionUpgrade(ssot,
                                    indexTable);
                            }
                        } else {
                            sessionUpgrade = true;
                        }
                    }
                    if (utilDebug.messageEnabled()) {
                        utilDebug.message("session upgrade is : "+ sessionUpgrade);
                    }
                }
            }
            
            if (utilDebug.messageEnabled()) {
                utilDebug.message("AuthUtil:getAuthContext:sid is.. .: " + sid);
                utilDebug.message("AuthUtil:getAuthContext:authContext is.. .: "
                + authContext);
                utilDebug.message("AuthUtil:getAuthContext:sessionUpgrade is.. .: "
                + sessionUpgrade);
                utilDebug.message("AuthUtil:getAuthContext:ForceAuth is.. .: "
                    + forceAuth);
            }
            
            if ((orgName == null) && (sess == null)) {
                utilDebug.error("Cannot create authcontext with null org " );
                throw new AuthException(AMAuthErrorCode.AUTH_TIMEOUT, null);
            } else if (orgName == null) {
                orgName = sess.getClientDomain();
            }
            if ((ssot != null) && !(sessionUpgrade)) {
                xmlReq.setValidSessionNoUpgrade(true);
                return null;
            }
            
            if (((ssot == null) && (loginState == null)) || 
                (sessionUpgrade)) {
                try {
                    loginState = new LoginState();
                    InternalSession oldSession = null;
                    if (sid != null) {
                        oldSession = AuthD.getSession(sid);
                        loginState.setOldSession(oldSession);
                    }
                    if (sessionUpgrade) {
                        loginState.setOldSession(oldSession);
                        loginState.setSessionUpgrade(sessionUpgrade);
                    }
                    authContext = loginState.createAuthContext(sid,orgName,req);
                    authContext.setLoginState(loginState);
                    String queryOrg = getQueryOrgName(null,orgName);
                    if (utilDebug.messageEnabled()) {
                        utilDebug.message("query org is .. : "+ queryOrg);
                    }
                    loginState.setQueryOrg(queryOrg);
                } catch (AuthException ae) {
                    utilDebug.message("Error creating AuthContextLocal 2: ");
                    if (utilDebug.messageEnabled()) {
                        utilDebug.message("Exception " , ae);
                    }
                    throw new AuthException(ae);
                }
            } else {
                // update loginState
                try {
                    com.iplanet.dpro.session.service.InternalSession
                    requestSess = ad.getSession(sessionID);
                    if (utilDebug.messageEnabled()) {
                        utilDebug.message("AuthUtil :Session is .. : " + requestSess);
                    }
                    loginState = getLoginState(authContext);
                    if (loginState != null) {
                        loginState.setSession(requestSess);
                        loginState.setRequestType(false);
                    }
                } catch (Exception ae) {
                    utilDebug.message("Error Retrieving AuthContextLocal" );
                    if (utilDebug.messageEnabled()) {
                        utilDebug.message("Exception " , ae);
                    }
                    throw new AuthException(AMAuthErrorCode.AUTH_ERROR, null);
                }
                
            }
            if (forceAuth){ 
                loginState.setForceAuth(forceAuth);
            }
            
            
        } catch (Exception ee) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Creating AuthContextLocal 2: ", ee);
            }
            
            throw new AuthException(ee);
        }
        return authContext;
    }
    
    /**
     * Returns a set of authentication modules whose authentication
     * level equals to or greater than the specified authLevel. If no such
     * module exists, an empty set will be returned.
     *
     * @param authLevel authentication level.
     * @param organizationDN DN for the organization.
     * @param clientType  Client type, e.g. "genericHTML".
     * @return Set of authentication modules whose authentication level
     *         equals to or greater that the specified authentication level.
     */
    public static Set getAuthModules(
        int authLevel,
        String organizationDN,
        String clientType) {
        return AMAuthLevelManager.getInstance().getModulesForLevel(authLevel,
        organizationDN, clientType);
    }
 
    /* return the previous Internal Session */
    public static InternalSession getOldSession(AuthContextLocal authContext) {
     	LoginState loginState = getLoginState(authContext);
	InternalSession oldSession = loginState.getOldSession();
	return oldSession;
    }

    
    /* retreive the authcontext based on the req */
    public static AuthContextLocal getOrigAuthContext(SessionID sid)
    throws AuthException {
        AuthContextLocal authContext = null;
        // initialize auth service.
        AuthD ad = AuthD.getAuth();
        try {
            authContext = retrieveAuthContext(sid);
            if (utilDebug.messageEnabled()) {
                utilDebug.message("AuthUtil:getOrigAuthContext:sid is.:"+sid);
                utilDebug.message("AuthUtil:getOrigAuthContext:authContext is:"
                + authContext);
            }
            com.iplanet.dpro.session.service.InternalSession sess =
            getLoginState(authContext).getSession();
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Session is : "+ sess);
                if (sess != null) {
                    utilDebug.message("Session State is : "+ sess.getState());
                }
                utilDebug.message("Returning Orig AuthContext:"+authContext);
            }
            
            if (sess == null) {
                return null;
            } else {
                int status = sess.getState();
                if (status == Session.INVALID){
                    return null;
                }
                return authContext;
            }
            
        } catch (Exception e) {
            return null;
        }
    }
    
    /* check if the session is active */
    public static boolean isSessionActive(AuthContextLocal oldAuthContext) {
        try {
            com.iplanet.dpro.session.service.InternalSession sess =
            getSession(oldAuthContext);
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Sess is : " + sess);
            }
            boolean sessionValid = false;
            if (sess != null) {
                if (sess.getState() == Session.VALID) {
                    sessionValid = true;
                }
                if (utilDebug.messageEnabled()) {
                    utilDebug.message("Sess State is : " + sess.getState());
                    utilDebug.message("Is Session Active : " + sessionValid);
                }
            }
            return sessionValid;
        } catch (Exception e) {
            return false;
        }
    }
    
    /* retreive session property */
    public static String getSessionProperty(String property,
    AuthContextLocal oldAuthContext) {
        String value = null;
        try {
            com.iplanet.dpro.session.service.InternalSession sess =
            getSession(oldAuthContext);
            if (sess != null) {
                value = sess.getProperty(property);
            }
        } catch (Exception e) {
            utilDebug.message("Error : " ,e);
        }
        return value;
    }
    
    /* return session upgrade - true or false */
    public static boolean isSessionUpgrade(AuthContextLocal authContext) {
        boolean isSessionUpgrade = false;
        LoginState loginState =  getLoginState(authContext);
        if (loginState != null) {
            isSessionUpgrade = loginState.isSessionUpgrade();
        }
        return isSessionUpgrade;
    }
    
    public static void setCookieSupported(AuthContextLocal ac, boolean flag) {
        LoginState loginState =  getLoginState(ac);
        if (loginState==null) {
            return;
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("set cookieSupported to : " + flag);
            utilDebug.message("set cookieDetect to false");
        }
        loginState.setCookieSupported(flag);
    }
    
    public static boolean isCookieSupported(AuthContextLocal ac) {
        LoginState loginState =  getLoginState(ac);
        if (loginState==null) {
            return false;
        }
        return loginState.isCookieSupported();
    }
    
    public static boolean isCookieSet(AuthContextLocal ac) {
        LoginState loginState =  getLoginState(ac);
        if (loginState==null) {
            return false;
        }
        return loginState.isCookieSet();
    }
    
    /**
     * Returns true if cookies found in the request.
     *
     * @param req  HTTP Servlet Request.
     * @param ac authentication context.
     * @return <code>true</code> if cookies found in request.
     */
    public static boolean checkForCookies(HttpServletRequest req, AuthContextLocal ac){
        LoginState loginState =  getLoginState(ac);
        if (loginState!=null) {
            utilDebug.message("set cookieSet to false.");
            loginState.setCookieSet(false);
            loginState.setCookieDetect(false);
        }
        // came here if cookie not found , return false
        return (
        (CookieUtils.getCookieValueFromReq(req,getAuthCookieName()) != null)
        ||
        (CookieUtils.getCookieValueFromReq(req,getCookieName()) !=null));
    }    
   
    public static String getLoginURL(AuthContextLocal authContext) {
        LoginState loginState =  getLoginState(authContext);
        if (loginState==null) {
            return null;
        }
        return loginState.getLoginURL();
    }
    
    public static AuthContextLocal getAuthContextFromHash(SessionID sid) {
        AuthContextLocal authContext = null;
        if (sid != null) {
            authContext = retrieveAuthContext(sid);
        }
        return authContext;
    }  
    
    // Gets Callbacks per Page state
    public static Callback[] getCallbacksPerState(AuthContextLocal authContext, 
                                           String pageState) {
        LoginState loginState = getLoginState(authContext);
        Callback[] recdCallback = null;
        if (loginState != null) {
            recdCallback = loginState.getCallbacksPerState(pageState);
        }
        if ( recdCallback != null ) {
            if (utilDebug.messageEnabled()) {
                for (int i = 0; i < recdCallback.length; i++) {
                    utilDebug.message("in getCallbacksPerState, recdCallback[" 
                                      + i + "] :" + recdCallback[i]);
                }
            }
        }
        else {
            utilDebug.message("in getCallbacksPerState, recdCallback is null");
        }
        return recdCallback;
    }
    
    // Sets (saves) Callbacks per Page state
    public static void setCallbacksPerState(AuthContextLocal authContext,
    String pageState, Callback[] callbacks) {
        LoginState loginState = getLoginState(authContext);
        
        if (loginState != null) {
            loginState.setCallbacksPerState(pageState, callbacks);
        }
        if ( callbacks != null ) {
            if (utilDebug.messageEnabled()) {
                for (int i = 0; i < callbacks.length; i++) {
                    utilDebug.message("in setCallbacksPerState, callbacks[" 
                                      + i + "] :" + callbacks[i]);
                }
            }
        }
        else {
            utilDebug.message("in setCallbacksPerState, callbacks is null");
        }
    }    
    
    /**
     * Returns the SessionID . This is required to added the
     * session server , port , protocol info to the Logout Cookie.
     * SessionID is retrieved from Auth service if a handle on
     * the authcontext object is there otherwise retrieve from
     * the request object.
     *
     * @param authContext  is the AuthContext which is
     * 	    handle to the auth service
     * @param request is the HttpServletRequest object
     * @return returns the SessionID
     */
    public static SessionID getSidValue(AuthContextLocal authContext,
    HttpServletRequest request) {
        SessionID sessionId = null;
        if (authContext != null)  {
            utilDebug.message("AuthContext is not null");
            try {
                String sid = getSidString(authContext);
                if (sid != null) {
                    sessionId = new SessionID(sid);
                }
            } catch (Exception e) {
                utilDebug.message("Exception getting sid",e);
            }
        }
        
        if (sessionId == null) {
            utilDebug.message("Sid from AuthContext is null");
            sessionId = new SessionID(request);
        }
        
        if (utilDebug.messageEnabled()) {
            utilDebug.message("sid is : " + sessionId);
        }
        return sessionId;
    }
    
    /**
     * Returns true if cookie is supported otherwise false.
     * the value is retrieved from the auth service if a
     * handle on the auth context object is there otherwise
     * check the HttpServletRequest object to see if the
     * OpenSSO cookie is in the request header
     *
     * @param authContext is the handle to the auth service
     *	                  for the request
     * @param request is the HttpServletRequest Object for the
     *	              request
     *
     * @return boolean value indicating whether cookie is supported
     *	       or not.
     */
    public static boolean isCookieSupported(AuthContextLocal authContext,
    HttpServletRequest request) {
        boolean cookieSupported;
        if (authContext != null)  {
            utilDebug.message("AuthContext is not null");
            cookieSupported = isCookieSupported(authContext);
        } else {
            cookieSupported = checkForCookies(request,null);
        }
        
        if (utilDebug.messageEnabled()) {
            utilDebug.message("Cookie supported" + cookieSupported);
        }
        return cookieSupported;
    }
    
    /**
     * Returns the previous index type after module is selected in authlevel
     * or composite advices.
     * @param ac the is the AuthContextLocal instance.
     * @return AuthContext.IndexType.
     */
    public static AuthContext.IndexType getPrevIndexType(AuthContextLocal ac) {
        LoginState loginState = getLoginState(ac);
        if (loginState != null) {
            return loginState.getPreviousIndexType();
        } else {
            return null;
        }
    }
    
    /**
     * Returns whether the auth module is or the auth chain contains pure JAAS
     * module(s).
     * @param configName a string of the configuratoin name.
     * @return 1 for pure JAAS module; -1 for module(s) provided by IS only.
     */
    public static int isPureJAASModulePresent(
    String configName, AMLoginContext amlc)
    throws AuthLoginException {
        
        if (AuthD.enforceJAASThread) {
            return 1;
        }
        int returnValue = -1;
        
        Configuration ISConfiguration = null;
        try {
            ISConfiguration = Configuration.getConfiguration();
        } catch (Exception e) {
            return 1;
        }
        
        AppConfigurationEntry[] entries =
        ISConfiguration.getAppConfigurationEntry(configName);
        if (entries == null) {
            throw new AuthLoginException("amAuth",
            AMAuthErrorCode.AUTH_CONFIG_NOT_FOUND, null);
        }
        // re-use the obtained configuration
        amlc.setConfigEntries(entries);
        
        for (int i = 0; i < entries.length; i++) {
            String className = entries[i].getLoginModuleName();
            if (utilDebug.messageEnabled()) {
                utilDebug.message("config entry: " + className);
            }
            if (pureJAASModuleClasses.contains(className)) {
                returnValue = 1;
                break;
            } else if (ISModuleClasses.contains(className)) {
                continue;
            }
            
            try {
                Object classObject = Class.forName(className,true,
                    Thread.currentThread().getContextClassLoader()
                    ).newInstance();
                if (classObject instanceof AMLoginModule) {
                    if (utilDebug.messageEnabled()) {
                        utilDebug.message(className +
                        " is instance of AMLoginModule");
                    }
                    synchronized(ISModuleClasses) {
                        if (! ISModuleClasses.contains(className)) {
                            ISModuleClasses.add(className);
                        }
                    }
                } else {
                    if (utilDebug.messageEnabled()) {
                        utilDebug.message(className + " is a pure jaas module");
                    }
                    synchronized(pureJAASModuleClasses) {
                        if (! pureJAASModuleClasses.contains(className)) {
                            pureJAASModuleClasses.add(className);
                        }
                    }
                    returnValue = 1;
                    break;
                }
            } catch (Exception e) {
                if (utilDebug.messageEnabled()) {
                    utilDebug.message("fail to instantiate class for " +
                    className);
                }
                synchronized(pureJAASModuleClasses) {
                    if (! pureJAASModuleClasses.contains(className)) {
                        pureJAASModuleClasses.add(className);
                    }
                }
                returnValue = 1;
                break;
            }
        }
        return returnValue;
    }
    
    /**
     * Get the module service name in either
     * iplanet-am-auth format<module.toLowerCase()>Service(old) or
     * sunAMAuth<module>Service format(new).
     */
    public static String getModuleServiceName(String moduleName) {
        String serviceName = (String) moduleService.get(moduleName);
        if (serviceName == null) {
            serviceName = AMAuthConfigUtils.getModuleServiceName(moduleName);
            try {
                SSOToken token = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
                new ServiceSchemaManager(serviceName, token);
            } catch (Exception e) {
                serviceName = AMAuthConfigUtils.getNewModuleServiceName(
                moduleName);
            }
            moduleService.put(moduleName, serviceName);
        }
        return serviceName;
    }
    
    public static int getAuthRevisionNumber(){
        try {
            SSOToken token = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
            ServiceSchemaManager scm = new ServiceSchemaManager(
            ISAuthConstants.AUTH_SERVICE_NAME, token);
            return scm.getRevisionNumber();
        } catch (Exception e) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("getAuthRevisionNumber error", e);
            }
        }
        return 0;
    }       
   
    /**
     * Returns success URL for this request. If <code>goto</code> parameter is
     * in the current request then returns the <code>goto</code> parameter
     * else returns the success URL set in the valid session.
     *
     * @param request HTTP Servlet Request.
     * @param authContext authentication context for this request.
     * @return success URL.
     */
    public static String getSuccessURL(
        HttpServletRequest request,
        AuthContextLocal authContext) {
        String orgDN = authContext.getOrgDN();
        String successURL = getValidGotoURL(request, orgDN);
        if ((successURL == null) || (successURL.length() == 0) ||
        (successURL.equalsIgnoreCase("null")) ) {
            LoginState loginState = getLoginState(authContext);
            if (loginState == null) {
                successURL = getSessionProperty("successURL",authContext);
            } else {
                successURL =
                getLoginState(authContext).getConfiguredSuccessLoginURL();
            }
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("getSuccessURL : " + successURL);
        }
        return successURL;
    }              
    
    // Returns the set of Module instances resulting from a 'composite advice'
    public static Map processCompositeAdviceXML(String xmlCompositeAdvice,
    String orgDN, String clientType) {
        Map returnAuthInstances = null;
        Set returnModuleInstances = null;
        try {
            String decodedAdviceXML = URLEncDec.decode(xmlCompositeAdvice);
            Map adviceMap = PolicyUtils.parseAdvicesXML(decodedAdviceXML);
            if (utilDebug.messageEnabled()) {
                utilDebug.message("processCompositeAdviceXML - decoded XML : "
                + decodedAdviceXML);
                utilDebug.message("processCompositeAdviceXML - result Map : "
                + adviceMap);
            }
            if ((adviceMap != null) && (!adviceMap.isEmpty())) {
                returnAuthInstances = new HashMap();
                returnModuleInstances = new HashSet();
                Set keySet = adviceMap.keySet();
                Iterator keyIter = keySet.iterator();
                while (keyIter.hasNext()) {
                    String name = (String)keyIter.next();
                    Set values = (Set)adviceMap.get(name);
                    if (name.equals(AuthenticateToRealmCondition.
                        AUTHENTICATE_TO_REALM_CONDITION_ADVICE)) {
                        //returnAuthInstances = Collections.EMPTY_MAP;
                        returnAuthInstances.put(name, values);
                        break;
                    } else if (name.equals(AuthenticateToServiceCondition.
                        AUTHENTICATE_TO_SERVICE_CONDITION_ADVICE)) {
                        returnAuthInstances.put(name, values);                        
                    } else if (name.equals(AuthSchemeCondition.
                        AUTH_SCHEME_CONDITION_ADVICE)) {
                        returnModuleInstances.addAll(values);
                    } else if (name.equals(AuthLevelCondition.
                        AUTH_LEVEL_CONDITION_ADVICE)) {
                        Set newAuthLevelModules =
                            processAuthLevelCondition(values,orgDN,clientType);
                        returnModuleInstances.addAll(newAuthLevelModules);
                    }                    
                }
                if (returnAuthInstances.isEmpty()) {
                    returnAuthInstances.put(
                        AuthSchemeCondition.AUTH_SCHEME_CONDITION_ADVICE, 
                            returnModuleInstances);
                }
            }
        } catch (Exception e) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Error in processCompositeAdviceXML : "
                , e);
            }
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("processCompositeAdviceXML - " + 
                "returnAuthInstances : " + returnAuthInstances);
        }
        return returnAuthInstances;
    }
    
    // Returns the set of module instances having lowest auth level from a
    // given set of auth level values
    private static Set processAuthLevelCondition(Set authLevelvalues,
    String orgDN, String clientType) {
        if (utilDebug.messageEnabled()) {
            utilDebug.message("processAuthLevelCondition - authLevelvalues : "
            + authLevelvalues);
        }
        Set returnModuleInstances = Collections.EMPTY_SET;
        try {
            if ((authLevelvalues != null) && (!authLevelvalues.isEmpty())) {
                // First get the lowest auth level value from a given set
                int minAuthlevel = Integer.MAX_VALUE;
                String qualifiedRealm = null;
                String qualifiedOrgDN = null;
                Iterator iter = authLevelvalues.iterator();
                while (iter.hasNext()) {
                    //get the Realm qualified Auth Level value
                    String realmQualifiedAuthLevel = (String) iter.next();
                    String strAuthLevel = 
                        AMAuthUtils.getDataFromRealmQualifiedData(
                            realmQualifiedAuthLevel);                    
                    try {
                        int authLevel = Integer.parseInt(strAuthLevel);                        
                        if (authLevel < minAuthlevel) {
                            minAuthlevel = authLevel;
                            qualifiedRealm = 
                                AMAuthUtils.getRealmFromRealmQualifiedData(
                                    realmQualifiedAuthLevel);
                            qualifiedOrgDN = null;
                            if ((qualifiedRealm != null) && 
                                (qualifiedRealm.length() != 0)) {
                                qualifiedOrgDN = DNMapper.orgNameToDN(
                                    qualifiedRealm);
                            }
                            if (utilDebug.messageEnabled()) {
                                utilDebug.message("qualifiedRealm : " 
                                    + qualifiedRealm);
                                utilDebug.message("qualifiedOrgDN : " 
                                    + qualifiedOrgDN);
                            }
                        }
                    } catch (Exception nex) {
                        continue;
                    }
                }

                if ((qualifiedOrgDN != null) && (qualifiedOrgDN.length() != 0)) {
                    Set moduleInstances = 
                        getAuthModules(minAuthlevel,qualifiedOrgDN,clientType);
                    if (utilDebug.messageEnabled()) {
                        utilDebug.message("moduleInstances : " 
                            + moduleInstances);
                    }
                    if ((moduleInstances != null) && 
                        (!moduleInstances.isEmpty())) {

                        returnModuleInstances = new HashSet();
                        Iterator iterInstances = moduleInstances.iterator();
                        while (iterInstances.hasNext()) {
                            //get the module instance value
                            String moduleInstance = 
                                (String) iterInstances.next();                            
                            String realmQualifiedModuleInstance = 
                                AMAuthUtils.toRealmQualifiedAuthnData(
                                    qualifiedRealm,moduleInstance);                            
                            returnModuleInstances.add(
                                realmQualifiedModuleInstance);                            
                        }
                    }
                } else {
                    returnModuleInstances = 
                        getAuthModules(minAuthlevel,orgDN,clientType);
                }

                if (utilDebug.messageEnabled()) {
                    utilDebug.message("processAuthLevelCondition - " + 
                        "returnModuleInstances : " + returnModuleInstances + 
                            " for auth level : " + minAuthlevel);
                }
            }
        } catch (Exception e) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Error in processAuthLevelCondition : "
                , e);
            }
        }
        return returnModuleInstances;
    }                                                                                                        
      
    // returns AuthContextLocal object from Session object identified by 'sid'.
    // if not found then check it in the HttpSession.
    private static AuthContextLocal retrieveAuthContext(
    HttpServletRequest req, SessionID sid) {
        AuthContextLocal acLocal = null;        
        if (sid != null) {
            acLocal = retrieveAuthContext(sid);
        }

        return acLocal;
    }
    
    // retrieve the AuthContextLocal object from the Session object.
    private static AuthContextLocal retrieveAuthContext(SessionID sid) {
        com.iplanet.dpro.session.service.InternalSession is =
            AuthD.getSession(sid);        
        AuthContextLocal localAC = null;
        if (is != null) {
            localAC = (AuthContextLocal)
            is.getObject(ISAuthConstants.AUTH_CONTEXT_OBJ);
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("retrieveAuthContext - InternalSession = " + is);
            utilDebug.message("retrieveAuthContext - aclocal = " + localAC);
        }
        return localAC;
    }
    
    /**
     * Removes the AuthContextLocal object in the Session object identified
     * by the SessionID object parameter 'sid'.
     */
    public static void removeAuthContext(SessionID sid) {
        com.iplanet.dpro.session.service.InternalSession is =
        AuthD.getSession(sid);
        if (is != null) {
            is.removeObject(ISAuthConstants.AUTH_CONTEXT_OBJ);
        }
    }           
    
    /**
     * Returns the authentication service or chain configured for the
     * given organization.
     *
     * @param orgDN organization DN.
     * @return the authentication service or chain configured for the
     * given organization.
     */
    public static String getOrgConfiguredAuthenticationChain(String orgDN) {
        AuthD ad = AuthD.getAuth();
        return ad.getOrgConfiguredAuthenticationChain(orgDN);
    }

    /**
     * Returns true if remote Auth security is enabled and false otherwise
     *
     * @return the value of sunRemoteAuthSecurityEnabled attribute
     */
     public static String getRemoteSecurityEnabled() throws AuthException {
         ServiceSchema schema = null;
         try {
             SSOToken dUserToken = (SSOToken) AccessController.doPrivileged (
                 AdminTokenAction.getInstance());
             ServiceSchemaManager scm = new ServiceSchemaManager(
                 "iPlanetAMAuthService", dUserToken);
             schema = scm.getGlobalSchema();
         } catch ( Exception exp) {
             utilDebug.error("Cannot get global schema",exp);
             throw new AuthException(AMAuthErrorCode.AUTH_ERROR, null);
         }
         Map attrs = null;
         if (schema != null) {
             attrs = schema.getAttributeDefaults();
         }
         String securityEnabled = (String)Misc.getMapAttr(attrs,
             ISAuthConstants.REMOTE_AUTH_APP_TOKEN_ENABLED);
         if (utilDebug.messageEnabled()) {
             utilDebug.message("Security Enabled = " + securityEnabled);
         }
         return securityEnabled;   
     }

     /**
      * Returns the flag indicating a request "forward" after
      * successful authentication.
      *
      * @param authContext AuthContextLocal object
      * @param req HttpServletRequest object
      * @return the boolean flag.
      */
     public static boolean isForwardSuccess(AuthContextLocal authContext,
         HttpServletRequest req) {
         boolean isForward = forwardSuccessExists(req);
         if (!isForward) {
             LoginState loginState = getLoginState(authContext);
             if (loginState != null) {
                 isForward = loginState.isForwardSuccess();
             }
         }
         return isForward;
     }

     /**
      * Returns <code>true</code> if the request has the
      * <code>forward=true</code> query parameter.
      *
      * @param req HttpServletRequest object
      * @return <code>true</code> if this parameter is present.
      */
     public static boolean forwardSuccessExists(HttpServletRequest req) {
         String forward = req.getParameter("forward");
         boolean isForward =
             (forward != null) && forward.equals("true");
         if (utilDebug.messageEnabled()) {
             utilDebug.message("forwardSuccessExists : "+ isForward);
         }
         return isForward;
     }
     
     /**
      * Returns <code>Map</code> attributes
      *
      * @param serviceName Service Name
      * @return <code>Map</code> of global attributes.
      */
     public static Map getGlobalAttributes(String serviceName) {
         Map attrs = null;
         try {
             SSOToken dUserToken = (SSOToken) AccessController.doPrivileged (
                 AdminTokenAction.getInstance());
             ServiceSchemaManager scm = new ServiceSchemaManager(
                 serviceName, dUserToken);
             ServiceSchema schema = scm.getGlobalSchema();
             if (schema != null) {
                 attrs = schema.getAttributeDefaults();
             }
         } catch (SMSException smsExp) {
             utilDebug.error("AuthUtils.getGlobalAttributes: SMS Error", smsExp
                 );
         } catch (SSOException ssoExp) {
             utilDebug.error("AuthUtils.getGlobalAttributes: SSO Error", ssoExp
                 );
         }
         if (utilDebug.messageEnabled()) {
             utilDebug.message("AuthUtils.getGlobalAttributes: attrs=" + attrs);
         }
         return attrs;
     }
     
    public static void clearAllCookies(HttpServletRequest request, 
        HttpServletResponse response) {

        SessionID sid = new SessionID(request);
        Set cookieDomainSet = getCookieDomainsForReq(request);
        if (cookieDomainSet.isEmpty()) {
            clearAllCookiesByDomain(sid, null, request, response);
        } else {
            Iterator iter = cookieDomainSet.iterator();
            while (iter.hasNext()) {
                clearAllCookiesByDomain(sid, (String)iter.next(), request,
                    response);
            }
        }
        clearlbCookie(request, response);
        clearHostUrlCookie(response);
    }
    
    public static void clearAllCookiesByDomain(SessionID sid,
        String cookieDomain, HttpServletRequest request,
        HttpServletResponse response) {

        Cookie cookie = getLogoutCookie(sid, cookieDomain);
        response.addCookie(cookie);
        String pCookieName = getPersistentCookieName();
        String cookieValue = CookieUtils.getCookieValueFromReq(request,
            pCookieName);
        if (cookieValue != null) {
            // clear Persistent Cookie
            cookie = clearPersistentCookie(cookieDomain, null);
            if (utilDebug.messageEnabled()) {
                utilDebug.message("AuthUtils.clearAllCookiesByDomain: " +
                    "Clearing persistent cookie = " + cookie + ", domain = " +
                    cookieDomain);
            }
            response.addCookie(cookie);
        }
    }

             /*
     * Get URL set by Post Process Plugin in HttpServletRequest.
     * Caller should check for null return value.
     */
    public static String getPostProcessURL(HttpServletRequest servletRequest, String attrName)
    {
        if (attrName == null) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("URL name is null");
            }
            return null;
        }

        String url = null;

        if (servletRequest != null) {
            url = (String) servletRequest.getAttribute(attrName);
        }

        if (utilDebug.messageEnabled()) {
            if ( (url != null) && (url.length() > 0) ) {
                utilDebug.message("URL name : " + attrName +
                    " Value : " + url);
            }
            else {
               utilDebug.message("URL name : " + attrName +
                    " Value : Not set - null or empty string");
            }
        }

        if ( (url != null) && (url.length() <= 0) )
           url = null;

        return url;
    }

    /* Helper method to reset HttpServletRequest object before it is sent to
     * Post Process Plugin so that it can set new values.
     */

    public static void resetPostProcessURLs(HttpServletRequest servletRequest)
    {
        if (servletRequest != null) {
            servletRequest.removeAttribute(
               AMPostAuthProcessInterface.POST_PROCESS_LOGIN_SUCCESS_URL);
            servletRequest.removeAttribute(
               AMPostAuthProcessInterface.POST_PROCESS_LOGIN_FAILURE_URL);
            servletRequest.removeAttribute(
               AMPostAuthProcessInterface.POST_PROCESS_LOGOUT_URL);
        }
    }
    
    /**
     * Returns valid goto parameter for this request.	 
     * Validate goto parameter set in the current request, then returns it	 
     * if valid	 
     * @param request the HttpServletRequest	 
     * @param authContext authentication context for this request.	 
     * @return successURL a String	 
     */	 
    public static String getValidGotoURL(HttpServletRequest request,	 
            AuthContextLocal authContext) {	 
        String orgDN = authContext.getOrgDN();	 
        return getValidGotoURL(request, orgDN);	 
    }	 
	 
    /**	 
     * Returns valid goto parameter for this request.	 
     * Validate goto parameter set in the current request, then returns it	 
     * if valid	 
     * @param request the HttpServletRequest	 
     * @param orgDN organization DN	 
     * @return successURL a String	 
     */	 
    public static String getValidGotoURL(HttpServletRequest request,	 
            String orgDN) {	 
        String gotoUrl = null;	 
        if (request != null) {	 
            gotoUrl = request.getParameter("goto");	 
        }	 
	 
        if ((gotoUrl != null) && (gotoUrl.length() != 0) &&	 
                (!gotoUrl.equalsIgnoreCase("null")) ) {	 
            String encoded = request.getParameter("encoded");	 
            if (encoded != null && encoded.equals("true")) {	 
                gotoUrl = getBase64DecodedValue(gotoUrl);	 
            }	 
	 
            AuthD authD = AuthD.getAuth();	 
            if (!authD.isGotoUrlValid(gotoUrl, orgDN)) {
                if (utilDebug.messageEnabled()) {
                    utilDebug.message("AuthUtils.getValidGotoURL():" +
                        "Original goto URL is " + gotoUrl + " which is " +
                        "invalid");                   	
                }            	
                return null;                
            }	 
        }	 
        return gotoUrl;	 
    }    

    /**
     * Performs a logout on a given token ensuring the post auth classes are called
     * 
     * @param sessionID The token id to logout
     * @param request The HTTP request
     * @param response The HTTP response
     * @return true if the token was still valid before logout was called
     * @throws SSOException If token is null or other SSO exceptions
     */
    public static boolean logout(String sessionID, HttpServletRequest request, HttpServletResponse response)
    throws SSOException {
        return logout(AuthD.getSession(sessionID), 
                SSOTokenManager.getInstance().createSSOToken(sessionID), request, response);
    }
    
    /**
     * Performs a logout on a given token ensuring the post auth classes are called
     * 
     * @param intSession The <code>InternalSession</code> to logout
     * @param token The <code>SSOToken</code> to logout
     * @param request The HTTP request
     * @param response The HTTP response
     * @return true if the token was still valid before logout was called
     * @throws SSOException If token is null or other SSO exceptions
     */
    public static boolean logout(InternalSession intSession, 
                                 SSOToken token, 
                                 HttpServletRequest request, 
                                 HttpServletResponse response) 
    throws SSOException {
        if (token == null) {
            throw new SSOException("token cannot be null");
        }
        Object loginContext = null;
        
        if (intSession != null) {
            loginContext = intSession.getObject(ISAuthConstants.LOGIN_CONTEXT);
        }
        
        try {
            if (loginContext != null) {
                if (loginContext instanceof 
                    javax.security.auth.login.LoginContext) {
                    javax.security.auth.login.LoginContext lc = 
                        (javax.security.auth.login.LoginContext) 
                         loginContext;
                    lc.logout();
                } else {
                    com.sun.identity.authentication.jaas.LoginContext 
                        jlc = (com.sun.identity.authentication.jaas.
                        LoginContext) loginContext;
                    jlc.logout();
                }
            }
        } catch (javax.security.auth.login.LoginException loginExp) {
            utilDebug.error("AuthUtils.logout: Cannot Execute module Logout", loginExp);
        }
        
        Set<AMPostAuthProcessInterface> postAuthSet = null;
        
        if (intSession != null) {
            postAuthSet = (Set<AMPostAuthProcessInterface>) intSession.getObject(ISAuthConstants.
                POSTPROCESS_INSTANCE_SET);
        }
        
        if ((postAuthSet != null) && !(postAuthSet.isEmpty())) {            
            for (AMPostAuthProcessInterface postLoginInstance : postAuthSet) {
                try {
                    postLoginInstance.onLogout(request, response, token);
                } catch (Exception exp) {
                   utilDebug.error("AuthUtils.logout: Failed in post logout.", exp);
                }
            }
        } else {
            String plis = null;
            
            if (intSession != null) {
                plis = intSession.getProperty(ISAuthConstants.POST_AUTH_PROCESS_INSTANCE);
            }
            if (plis != null && plis.length() > 0) {
                StringTokenizer st = new StringTokenizer(plis, "|");
                
                if (token != null) {
                    while (st.hasMoreTokens()) {
                        String pli = (String)st.nextToken();
                        
                        try {
                            AMPostAuthProcessInterface postProcess = 
                                    (AMPostAuthProcessInterface)
                                    Thread.currentThread().
                                    getContextClassLoader().
                                    loadClass(pli).newInstance();
                            postProcess.onLogout(request, response, token);
                        } catch (Exception ex) {
                            utilDebug.error("AuthUtils.logout:" + pli, ex);
                        }
                    }
                }
            }
        }
        
        boolean isTokenValid = false;
        
        try {
            isTokenValid = SSOTokenManager.getInstance().isValidToken(token);
            
            if ((token != null) && isTokenValid) {
                AuthD.getAuth().logLogout(token);
                Session session = Session.getSession(new SessionID(token.getTokenID().toString()));
                session.logout();
                
                if (utilDebug.messageEnabled()) {
                    utilDebug.message("AuthUtils.logout: logout successful.");
                }
            }
        } catch (SessionException se) {
            if (utilDebug.warningEnabled()) {
                utilDebug.warning("AuthUtils.logout: SessionException"
                    + " checking validity of SSO Token", se);
            }
        }
        
        return isTokenValid;
    }
}
