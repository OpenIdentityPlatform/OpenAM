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
 * $Id: FSPreLogin.java,v 1.6 2008/08/19 19:11:04 veiming Exp $
 *
 */

package com.sun.identity.federation.login;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Cookie;

import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.federation.services.util.FSServiceUtils;
import com.sun.identity.federation.services.FSLoginHelper;
import com.sun.identity.federation.services.FSLoginHelperException;
import com.sun.identity.shared.encode.CookieUtils;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.liberty.LibertyManager;


/**
 * This class has methods for pre login operations.
 */
public class FSPreLogin {
    
    private String realm = null;
    private static String postLoginURL = null;
    private static String loginURL = null;
    private static String amserverURI = null;
    private static String fedCookieName =
        SystemConfigurationUtil.getProperty(
            IFSConstants.FEDERATE_COOKIE_NAME);
    private boolean doLocalAuth = false;
    private String errorPage = null;
    private String homePage = null;
    private String commonLoginPage = null;
    private static IDFFMetaManager metaManager = null;
    
    static {
        metaManager = FSUtils.getIDFFMetaManager();
    }
    
    /**
     * Removes metaAlias,goto, and org keys and values
     * from query string if present.
     *
     * @param httpRequest the HttpServletRequest object.
     * @return a query string.
     */
    private String removeMetaGotoOrg(HttpServletRequest httpRequest) {
        Enumeration paramEnum = httpRequest.getParameterNames();
        String returnString = null;
        while (paramEnum.hasMoreElements()) {
            String paramKey = (String)paramEnum.nextElement();
            if(paramKey.equalsIgnoreCase(IFSConstants.META_ALIAS) ||
                    paramKey.equalsIgnoreCase(IFSConstants.GOTOKEY) ||
                    paramKey.equalsIgnoreCase(IFSConstants.ORGKEY)) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSPreLogin::removeMetaGotoOrg "
                        + "found metaAlias or gotoKey or org.");
                }
            } else {
                String paramValue =
                    httpRequest.getParameter(paramKey);
                StringBuffer paramStringBuf= new StringBuffer().append(paramKey)
                    .append(IFSConstants.EQUAL_TO).append(paramValue);
                if (returnString == null || returnString.length() < 1) {
                    returnString = paramStringBuf.toString();
                } else {
                    returnString +=
                        IFSConstants.AMPERSAND + paramStringBuf.toString();
                }
            }
        }
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message(
                "FSPreLogin::removeMetaGotoOrg returning with " + returnString);
        }
        return returnString;
    }
    
    /**
     * Forms the login url and append the required queryString.
     * If showFederatePage is false then the federate page is not shown
     * in postlogin.
     *
     * @param showFederatePage true if request should be redirected to
     *                         Federation Page.
     * @param metaAlias the provider alias.
     * @param request the HttpServletRequest.
     * @return the login URL String.
     */
    private String getLoginURL(boolean showFederatePage,
        String metaAlias,HttpServletRequest request) 
    {
        StringBuffer localLoginURLBuf = new StringBuffer(loginURL)
            .append(IFSConstants.QUESTION_MARK)
            .append(IFSConstants.ARGKEY).append(IFSConstants.EQUAL_TO)
            .append(IFSConstants.NEWSESSION);
        
        String returnURL = new StringBuffer().append(localLoginURLBuf)
            .append(IFSConstants.AMPERSAND)
            .append(getQueryString(showFederatePage, metaAlias,request))
            .toString();
        
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSPreLogin::getLoginURL returning with URL "
                + returnURL );
        }
        return returnURL;
    }
    
    /**
     * Forms the required queryString to be append to login URL.
     *
     * @param showFederatePage true if request should be redirected to
     *                         Federation Page.
     * @param metaAlias alias of the provider.
     * @param httpRequest the HttpServletRequest object.
     * @return the login URL String.
     */
    private String getQueryString(boolean showFederatePage, String metaAlias,
            HttpServletRequest httpRequest) 
    {
        FSUtils.debug.message("FSPreLogin::getQueryString called");
        String lrURL = httpRequest.getParameter(IFSConstants.GOTOKEY);
        String reqQueryString = removeMetaGotoOrg(httpRequest);

        if (lrURL == null || lrURL.length() <= 0) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSPreLogin::getQueryString."
                    + "no goto in queryString.Assinging LRURL = "
                    + homePage);
            }
            lrURL = homePage;
        }
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message(
                "FSPreLogin::getQueryString.RelayState =" + lrURL
                + " Required QueryString =" + reqQueryString);
        }
        
        String gotoURL = new StringBuffer().append(postLoginURL)
            .append(IFSConstants.QUESTION_MARK).append(IFSConstants.META_ALIAS)
            .append(IFSConstants.EQUAL_TO)
            .append(metaAlias).append(IFSConstants.AMPERSAND)
            .append(IFSConstants.LRURL).append(IFSConstants.EQUAL_TO)
            .append(lrURL)
            .toString();
/*
        String gotoURL = new StringBuffer().append(postLoginURL)
            .append(IFSConstants.QUESTION_MARK).append(IFSConstants.META_ALIAS)
            .append(IFSConstants.EQUAL_TO)
            .append(metaAlias)
            .toString();
*/
        if (showFederatePage) {
            gotoURL = new StringBuffer().append(gotoURL)
                .append(IFSConstants.AMPERSAND)
                .append(IFSConstants.FEDERATEKEY).append(IFSConstants.EQUAL_TO)
                .append(IFSConstants.FEDERATEVALUE).toString() ;
        }
        
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSPreLogin::getQueryString.gotoURL ="
                + gotoURL);
        }
        gotoURL = URLEncDec.encode(gotoURL);
        StringBuffer returnURLBuf =
            new StringBuffer().append(IFSConstants.ORGKEY)
                .append(IFSConstants.EQUAL_TO)
                .append(realm);
        if (reqQueryString != null && reqQueryString.length() > 0) {
            returnURLBuf.append(IFSConstants.AMPERSAND).append(reqQueryString);
        }
        returnURLBuf.append(IFSConstants.AMPERSAND).append(IFSConstants.GOTOKEY)
            .append(IFSConstants.EQUAL_TO).append(gotoURL);
        
        return returnURLBuf.toString();
    }

    /**
     * Retrieves hosted provider and local configuration attributes.
     *
     * @param metaAlias the provider alias.
     * @param httpRequest the HttpServletRequest object.
     * @exception FSPreLoginException on error.
     */
    private void setMetaInfo(String metaAlias,
        HttpServletRequest httpRequest)
        throws FSPreLoginException 
    {
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message(
                "FSPreLogin::setMetaInfo called with metaAlias =" +
                metaAlias );
        }
        String authType = null;
        try {
            String hostedEntityID = null;
            String hostedProviderRole = IFSConstants.SP;
            if (metaManager != null) {
                hostedProviderRole =
                    metaManager.getProviderRoleByMetaAlias(metaAlias);
                hostedEntityID =
                    metaManager.getEntityIDByMetaAlias(metaAlias);
            } else {
                FSUtils.debug.error("FSPreLogin::setMetaInfo "
                    + "meta manager is null. "
                    + "Cannot proceed so throwing error page");
                throw new FSPreLoginException(
                    "FSPreLogin:: could not get meta manager handle.");
            }
            
            realm = IDFFMetaUtils.getRealmByMetaAlias(metaAlias);
            BaseConfigType hostedConfig = null;
            if (hostedProviderRole != null) {
                if (hostedProviderRole.equals(IFSConstants.SP)) {
                    hostedConfig =
                        metaManager.getSPDescriptorConfig(
                            realm, hostedEntityID);
                } else if (hostedProviderRole.equals(IFSConstants.IDP)) {
                    hostedConfig =
                        metaManager.getIDPDescriptorConfig(
                            realm, hostedEntityID);
                }
            }

            if (hostedConfig != null) {
                Map attributes = IDFFMetaUtils.getAttributes(hostedConfig);
                homePage = IDFFMetaUtils.getFirstAttributeValue(
                    attributes, IFSConstants.PROVIDER_HOME_PAGE_URL);
                authType= IDFFMetaUtils.getFirstAttributeValue(
                    attributes, IFSConstants.AUTH_TYPE);
                commonLoginPage = FSServiceUtils.getCommonLoginPageURL(
                    httpRequest, hostedConfig);
                errorPage = FSServiceUtils.getErrorPageURL(
                    httpRequest, hostedConfig, metaAlias);
            } else {
                FSUtils.debug.error("FSPreLogin::setMetaInfo "
                    + "getDescriptorConfig retured null. "
                    + "Cannot proceed so throwing error page");
                throw new FSPreLoginException(
                    "FSPreLogin:: could not get sp config.");
            }
           
            if (authType != null &&
                authType.equalsIgnoreCase(IFSConstants.AUTH_LOCAL)) 
            {
                doLocalAuth = true;
            } else {
                FSUtils.debug.message("FSPreLogin::setMetaInfo "
                    + "authType=" + authType
                    + "Setting authType to default false.");
            }
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSPreLogin::setMetaInfo.realm = " + realm
                    + " doLocalAuth = " + doLocalAuth);
            }
        } catch (IDFFMetaException allianExp) {
            FSUtils.debug.error("FSPreLogin::setMetaInfo."
                + " IDFFMetaException caught. ", allianExp);
            throw new FSPreLoginException(
                "FSPreLogin::IDFFMetaException. ");
        } catch (Exception exp) {
            FSUtils.debug.error("FSPreLogin::setMetaInfo."
                + " General Exception caught. " , exp);
            throw new FSPreLoginException("Prelogin exception");
        }
    }
   
    /**
     * Returns a map of cookies from the cookie array.
     *
     * @param cookieArray Array of cookies.
     * @return a Map of Cookies with cookieName as key and
     *         value the cookie value.
     */
    private Map getCookieMap(Cookie[] cookieArray) {
        Map cookieMap = new HashMap();
        if (cookieArray != null) {
            for(int i = 0; i < cookieArray.length; i++) {
                cookieMap.put(cookieArray[i].getName(),
                    cookieArray[i].getValue());
                if(FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSPreLogin::getCookieMap with key "
                        + cookieArray[i].getName() + " and value "
                        + cookieArray[i].getValue());
                }
            }
        }
        return cookieMap;
    }
   
    /**
     * Sets the required headers in the HTTP response.
     *
     * @param response the HttpServletResponse object.
     * @param retMap Map of key/value attribute pairs to
     *        be set in the request.
     */
    private void setResponse(HttpServletResponse response ,Map retMap){
        FSUtils.debug.message("FSPreLogin::setResponse called.");
        Map headerMap = (Map)retMap.get(IFSConstants.HEADER_KEY);
        Iterator hdrNames = headerMap.keySet().iterator();
        while (hdrNames.hasNext()) {
            String name = hdrNames.next().toString();
            String value = (String)headerMap.get(name);
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSPreLogin::setResponse with header with name " + name
                    + " with value " + value);
            }
            response.addHeader(name, value);
        }
       
        if (!headerMap.containsKey("Cache-Control")){
            response.addHeader("Cache-Control", "no-cache");
        }
        
        if (!headerMap.containsKey("Pragma")){
            response.addHeader("Pragma", "no-cache");
        }
    }
   
    /**
     * Forwards request to an error page .
     *
     * @param request the HttpServletRequest object.
     * @param response the HttpServletResponse object.
     */
    private void sendError(HttpServletRequest request,
        HttpServletResponse response) 
    {
        try {
            FSUtils.forwardRequest(request, response, errorPage);
        } catch (Exception exp) {
            FSUtils.debug.error("FSPreLogin:: sendError "
                + "Error during sending error page");
        }
    }

    /**
     * Validates the OpenSSO Session Token String.
     *
     * @param token the Session Token String.
     * @return a boolean value true if valid otherwise false.
     */
    private boolean validateISCookie(HttpServletRequest request) {
        boolean isValidToken = false;
        FSUtils.debug.message("FSPreLogin::validateISCookie.Called ");
        try {
            SessionProvider sessionProvider = SessionManager.getProvider();
            Object ssoToken = sessionProvider.getSession(request);
            isValidToken = sessionProvider.isValid(ssoToken);
        } catch (SessionException ssoe) {
            FSUtils.debug.message("FSPreLogin::validateISCookie():", ssoe);
        } catch (Exception ex) {
            FSUtils.debug.message("FSPreLogin::validateISCookie():", ex);
        }
        return isValidToken;
    }

    /**
     * Returns a Map of header contained in the HTTP Request.
     *
     * @param httpRequest the HttpServletRequest object.
     * @return Map of request headers key/value pairs.
     */
    private Map setHeaderMap(HttpServletRequest httpRequest) {
        Map headerMap = new HashMap();
        Enumeration headerNames = httpRequest.getHeaderNames();
        while(headerNames.hasMoreElements()) {
            String hn = headerNames.nextElement().toString();
            String hv = httpRequest.getHeader(hn);
            headerMap.put(hn, hv);
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSLoginHelper.setHeaderMap() : name :: "
                    + hn + " :: value :: " + hv);
            }
        }
        return headerMap;
    }

   /**
    * Initiates call to create Authentication Request.
    * Returns a Map of request headers/attributes key/values pairs,
    * where key is the attribute name and value is the attribute
    * value.
    *
    * @param authLevel the AuthLevel parameter value.
    * @param metaAlias the provider alias.
    * @param httpRequest the HttpServletRequest object.
    * @param httpResponse the HttpServletResponse object.
    * @return Map of request headers key/value pairs.
    * @exception FSPreLoginException on error.
    */
    private Map createSSOMap(String authLevel, String metaAlias,
        boolean isFedCookiePresent,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse)
        throws FSPreLoginException 
    {
        try {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSPreLogin::createSSOMap called with authLevel =" +
                    authLevel + " metaAlias =" + metaAlias);
            }
           
            if (!isFedCookiePresent) {
                String actionOnNoFedCookie = httpRequest.getParameter(
                    IFSConstants.ACTION_ON_NO_FED_COOKIE);
                if (actionOnNoFedCookie != null &&
                    actionOnNoFedCookie.equals(IFSConstants.LOCAL_LOGIN)) 
                {
                    FSUtils.forwardRequest(httpRequest, httpResponse,
                        getLoginURL(false, metaAlias,httpRequest));
                    return null;
                }
            }
           
            Map headerMap = setHeaderMap(httpRequest);
            FSLoginHelper loginHelper = new FSLoginHelper(httpRequest);
            String targetURL = httpRequest.getParameter(IFSConstants.GOTOKEY);
            if (targetURL == null || targetURL.length() <= 0 ) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSPreLogin::createSSOMap."
                        + "no goto in queryString.Assinging targetURL = "
                        + homePage);
                }
                targetURL = homePage;
            }
            Map retMap = loginHelper.createAuthnRequest(headerMap,
                targetURL,
                authLevel,
                metaAlias,
                null,
                isFedCookiePresent);
            String requestID = null;
            String responseData =
                (String) retMap.get(IFSConstants.RESPONSE_DATA_KEY);
            if (responseData != null &&  responseData.length() != 0) {
                return retMap;
            }
            requestID = (String) retMap.get(IFSConstants.AUTH_REQUEST_ID);
            String URL = (String) retMap.get(IFSConstants.URL_KEY);
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSPreLogin::createSSOMap requestID"
                    + requestID + " URL " + URL);
            }
            if (requestID != null && URL == null) {
                //show list page
                String queryString =
                    getQueryString(true, metaAlias,httpRequest);
                String returnURL = new StringBuffer().append(commonLoginPage)
                    .append(IFSConstants.QUESTION_MARK).append(queryString)
                    .append(IFSConstants.AMPERSAND)
                    .append(IFSConstants.AUTH_REQUEST_ID)
                    .append(IFSConstants.EQUAL_TO)
                    .append(URLEncDec.encode(requestID))
                    .append(IFSConstants.AMPERSAND)
                    .append(IFSConstants.META_ALIAS)
                    .append(IFSConstants.EQUAL_TO)
                    .append(httpRequest.getParameter(IFSConstants.META_ALIAS))
                    .toString();
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "FSPreLogin::createSSOMap framedlogin url " +
                        returnURL);
                }
                retMap.put(IFSConstants.URL_KEY,returnURL);
            }
            return retMap;
        } catch (FSLoginHelperException exp) {
            FSUtils.debug.error("FSPreLogin::setMetaInfo."
                + " FSLoginHelperException Exception caught. ", exp);
            throw new FSPreLoginException("FSPreLogin::FSLoginHelperException");
        }
    }


    /**
     * Redirects request to URL based on whether a Single Sign-On needs to
     * be done or local Authentication. The decision is based on AuthFlag
     * which is set in LocalConfiguration of the provider, presence/absense
     * of Federation Cookie and the presence/absence of OpenSSO
     * Session Cookie or/and its validity.
     *
     * @param request the <code>HttpServletRequest</code> object.
     * @param response the <code>HttpServletResponse</code> object.
     */
    public void doPreLogin(HttpServletRequest request,
        HttpServletResponse response) 
    {
        try {
            FSUtils.debug.message("FSPreLogin::Entered doPreLogin");
            Cookie cookieArray[] = CookieUtils.getCookieArrayFromReq(request);
            boolean isTokenValid = false;
            boolean isFedCookie = true;
            amserverURI = FSServiceUtils.getBaseURL(request);
            postLoginURL = amserverURI + IFSConstants.POST_LOGIN_PAGE;
            loginURL = amserverURI + IFSConstants.LOGIN_PAGE;
            String gotoOnFedCookieNoURL = request.getParameter(
                IFSConstants.GOTO_ON_FEDCOOKIE_NO_URL);
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSPreLogin::gotoOnFedCookieNoURL="
                    + gotoOnFedCookieNoURL);
            }
            String returnURL = null;
            Map retMap = new HashMap();
            String metaAlias = request.getParameter(IFSConstants.META_ALIAS);
            Map cookieMap = getCookieMap(cookieArray);
            setMetaInfo(metaAlias,request);
           
            if (LibertyManager.isLECPProfile(request)) {
                String headerName = LibertyManager.getLECPHeaderName();
                String headerValue = request.getHeader(headerName);
                response.setHeader(headerName, headerValue);
            }
           
            if (doLocalAuth) {
                FSUtils.debug.message(
                    "FSPreLogin::doPreLogin. do local auth is true ");
                returnURL = getLoginURL(false,metaAlias,request);
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "FSPreLogin::doPreLogin. returning with url " +
                        returnURL);
                }
                response.sendRedirect(returnURL);
                return;
            }
           
            if (cookieMap.containsKey(request)) {
                isTokenValid = true;
                FSUtils.debug.message(
                    "FSPreLogin::doPreLogin. OpenSSO Token is valid");
            }
            boolean isFedCookiePresent = false;
            if (cookieMap.containsKey(fedCookieName)) {
                isFedCookiePresent = true;
                FSUtils.debug.message(
                    "FSPreLogin::doPreLogin. fedCookie is present");
                if (((String)cookieMap.get(fedCookieName))
                    .equalsIgnoreCase("no")) 
                {
                    isFedCookie = false;
                    FSUtils.debug.message(
                        "FSPreLogin::doPreLogin. fedCookie is no");
                }
            }
           
            String authLevelParam =
                request.getParameter(IFSConstants.AUTH_LEVEL_KEY);
            /**
             * This authlevel in http session will be used while
             * redirecting to local login from the common login page
             */
            if (authLevelParam != null) {
                HttpSession httpSession = request.getSession();
                httpSession.setAttribute(IFSConstants.AUTH_LEVEL_KEY,
                    authLevelParam);
            }
           
            if (!isFedCookie) {
                // no FedCookie
                // redirect to local login page no post login
                // Also, Check if there's any no liberty URL
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSPreLogin::doPreLogin."
                        + "In case where ISToken invalid and"
                        + "fedcookie no");
                }
                if (gotoOnFedCookieNoURL != null) {
                    response.sendRedirect(gotoOnFedCookieNoURL);
                } else {
                    returnURL = getLoginURL(false,metaAlias,request);
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message(
                            "FSPreLogin::doPreLogin. returning with url " +
                            returnURL);
                    }
                    FSUtils.forwardRequest(request, response, returnURL);
                }
                return;
            } else { // fedCookie is present
                if (isTokenValid) {
                    // this is the case where token is valid and
                    // fedCookie is present
                    //do sso if auth level present in queryString
                    //else append queryString and send to localLogin
                    if (authLevelParam != null) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("FSPreLogin::doPreLogin."
                                + "In case where ISToken valid and "
                                + "fedcookie yes and dolocalauth false and "
                                + "authLevel present");
                        }
                        retMap = createSSOMap(authLevelParam,metaAlias,
                            isFedCookiePresent,request,response);
                        if (retMap != null) {
                            setResponse(response,retMap);
                            String responseData = (String) retMap.get(
                                IFSConstants.RESPONSE_DATA_KEY);
                            if (responseData != null &&
                                responseData.length() != 0) 
                            {
                                response.getOutputStream().write(
                                    responseData.getBytes());
                                response.flushBuffer();
                            } else {
                                returnURL = (String) retMap.get(
                                    IFSConstants.URL_KEY);
                                if (FSUtils.debug.messageEnabled()) {
                                    FSUtils.debug.message(
                                        "FSPreLogin::doPreLogin."
                                        + "returning with url "
                                        + returnURL);
                                }
                                FSUtils.forwardRequest(request,
                                    response, returnURL);
                            }
                        }
                        return;
                    } else {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("FSPreLogin::doPreLogin."
                                 + "In case where ISToken valid and"
                                 + "fedcookie yes and dolocalauth false and "
                                 + "authLevel not present");
                        }
                        returnURL = getLoginURL(true,metaAlias,request);
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("FSPreLogin::doPreLogin."
                                + "returning with url " + returnURL);
                        }
                        FSUtils.forwardRequest(request, response, returnURL);
                        return;
                    }
                } else { // Token not valid
                    // do sso with gettin authlevel from request or default auth
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("FSPreLogin::doPreLogin."
                            + "In case where ISToken invalid and"
                            + "fedcookie yes");
                    }
                    retMap = createSSOMap(authLevelParam,metaAlias,
                        isFedCookiePresent,request,response);
                    if (retMap != null) {
                        setResponse(response,retMap);
                        String responseData =
                            (String) retMap.get(IFSConstants.RESPONSE_DATA_KEY);
                        if (responseData != null && responseData.length() != 0)
                        {
                            response.getOutputStream().write(
                                responseData.getBytes());
                            response.flushBuffer();
                        } else {
                            returnURL = (String)retMap.get(
                                IFSConstants.URL_KEY);
                            if (FSUtils.debug.messageEnabled()) {
                                FSUtils.debug.message("FSPreLogin::doPreLogin."
                                    + "returning with url " + returnURL);
                            }
                            FSUtils.forwardRequest(
                                request, response, returnURL);
                        }
                    }
                    return;
                }
            }
        } catch (FSPreLoginException preLoginExp) {
            FSUtils.debug.error("FSPreLogin::Exception in doPrelogin. ",
                preLoginExp);
            sendError(request, response);
        } catch (Exception exp) {
            FSUtils.debug.error("FSPreLogin::Exception in doPrelogin. ", exp);
            //redirect to error page
            sendError(request, response);
        }
    }
}
