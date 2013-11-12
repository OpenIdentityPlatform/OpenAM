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
 * $Id: AmFilterRequestContext.java,v 1.9 2008/10/07 17:32:31 huacui Exp $
 *
 */
/**
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.agents.filter;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.identity.agents.arch.AgentConfiguration;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.common.IHttpServletRequestHelper;
import com.sun.identity.agents.common.IURLFailoverHelper;
import com.sun.identity.agents.common.SSOValidationResult;
import com.sun.identity.agents.policy.AmWebPolicyResult;
import com.sun.identity.agents.util.CookieUtils;
import com.sun.identity.agents.util.IUtilConstants;
import com.sun.identity.agents.util.NameValuePair;
import com.sun.identity.agents.util.StringUtils;

/**
 * A <code>AmFilterRequestContext</code> encapsulates all request and response
 * related information that is needed for the invocation of various task
 * handlers by the <code>AmFilter</code> in order to be able to process the
 * incoming request.
 */
public class AmFilterRequestContext implements IUtilConstants {

    /**
     * The sole constructor for <code>AmFilterRequestContext</code>.
     * @param request the HttpServletRequest used for filter invocation
     * @param response the HttpServletResponse used for filter invocation
     * @param gotoParameterName the parameter name used for return redirects
     * @param loginURLFailoverHelper the URL failover helper used to select
     * the currently available login URL in cases where necessary.
     * @param logoutURLFailoverHelper the URL failover helper used to select
     * the currently available logout URL in cases where necessary.
     * @param isFormLoginRequest a boolean indicating if the current request is
     * that for a J2EE Form based login.
     * @param accessDeniedURI the URI to be used for denying access to the 
     * requested resource.
     * @param amFilter the instance of <code>AmFilter</code> that is creating
     * this particular request context.
     * @param filterMode the mode in which the <code>AmFilter</code> is 
     * operating during the execution of this request.
     */
    public AmFilterRequestContext(HttpServletRequest request,
                           HttpServletResponse response,
                           String gotoParameterName,
                           IURLFailoverHelper loginURLFailoverHelper,
                           IURLFailoverHelper logoutURLFailoverHelper,
                           boolean isFormLoginRequest,
                           String accessDeniedURI,
                           AmFilter amFilter,
                           AmFilterMode filterMode,
                           String agentHost, int agentPort, 
                           String agentProtocol)
    {
        setRequest(request);
        setResponse(response);
        setGotoParameterName(gotoParameterName);
        setLoginURLFailoverHelper(loginURLFailoverHelper);
        setLogoutURLFailoverHelper(logoutURLFailoverHelper);
        setIsFormLoginRequestFlag(isFormLoginRequest);
        setAccessDeniedURI(accessDeniedURI);
        setAmFilter(amFilter);
        setFilterMode(filterMode);
        setAgentHost(agentHost);
        setAgentPort(agentPort);
        setAgentProtocol(agentProtocol);
    }

    /**
     * Returns the original HttpServletRequst that was used for filter
     * invocation.
     *
     * @return the original HttpServletRequest object
     */
    public HttpServletRequest getHttpServletRequest() {
        return _request;
    }

    /**
     * Returns the original HttpServletResponse that was used for filter
     * invocation.
     *
     * @return the original HttpServletResponse object
     */
    public HttpServletResponse getHttpServletResponse() {
        return _response;
    }

    /**
     * Returns the Filter mode
     *
     * @return the Filter mode
     */
    public AmFilterMode getFilterMode() {
        return _filterMode;
    }


    /**
     * Returns the value of parameter name used for return redirects such
     * as <code>goto</code> as configured in the Identity Server authentication
     * service.
     *
     * @return the value of parameter name used for return redirects
     */
    public String getGotoParameterName() {
        return _gotoParameterName;
    }

    /**
     * Returns the value of the actual canoncialized destination URL that
     * the user intended to access when the request was submitted.
     *
     * @return the canonicalized destination URL as submitted by the user.
     */
    public String getDestinationURL() {
        /**
         * Note: The <code>AmFilterRequestContext</code> need not be
         * thread safe and therefore the following lazy initialization should
         * not cause any problems in the runtime.
         */
        if (_destinationURL == null ) {
            initDestinationURL();
        }
        return _destinationURL;
    }
    
    /**
     * Returns the value of the actual destination url in the case 
     * of a form login request. This information is not available
     * for all agents.
     *
     * @return the destination URL in the case of a form login request.
     */
    public String getGotoFormLoginURL() {
        return _gotoFormLoginURL;
    }    
    
    /**
     * Returns the value of actual canocialized destination URL that the
     * user intended to access without the SSO token in the query parameter
     * so that it can be used to evaluate policies where necessary.
     * 
     * @return the canocialized destination URL as submitted by the user 
     * without the SSO token in the query parameter if it was present in 
     * the original URL.
     */
    public String getPolicyDestinationURL() {
        if (_policyDestinationURL == null) {
            initPolicyDestinationURL();
        }
        
        return _policyDestinationURL;
    }

    /**
     * Returns the value of a named cookie from the underlying
     * <code>HttpServletRequest</code> object.
     * @param cookieName the name of the cookie to be looked up.
     * @return the corresponding cookie value, or <code>null</code> if the
     * specified cookie is not present.
     */
    public String getRequestCookieValue(String cookieName) {
        /**
         * Note: The <code>AmFilterRequestContext</code> need not be
         * thread safe and therefore the following lazy initialization should
         * not cause any problems in the runtime.
         */
        if (getCookieMap() == null) {
            initCookieMap();
        }
        return (String) getCookieMap().get(cookieName);
    }

    /**
     * Returns a URL that can be used to redirect the current request which may
     * be lacking sufficient credentials to the Identity Server Authentication
     * service.
     *
     * @return a String representation of a URL that can be used to redirect the
     * current request to the authentication service.
     * @throws AgentException in the event when no login URL for Identity Server
     * authentication service is available.
     */

    public String getAuthRedirectURL() throws AgentException {
        return getAuthRedirectURL(null, null);
    }

   /**
    * Returns the value of the GOTO parameter to be used for post authentication
    * redirect by the authentication service. If the request is a form login
    * request, the return value will point to the application context root. 
    * Otherwise the return value will be a complete URL of the requested 
    * resource.
    * 
    * @return the URL to which the user will be redirected after successful
    * authentication.
    */
    public String populateGotoParameterValue() {
        String       gotoURL  = null;
        String       gotoFormLoginURL = getGotoFormLoginURL();
        if (isFormLoginRequest()) {
            if (gotoFormLoginURL != null) {
                gotoURL = gotoFormLoginURL;
            } else {
                gotoURL = getApplicationContextURL();
            }
        } else {
            gotoURL = getDestinationURL();
        }
        
        return gotoURL;
    }

    /**
     * Returns a URL that can be used to redirect the current request which may
     * be lacking sufficient credentials to the Identity Server Authentication
     * service.
     *
     * @param amWebPolicyResult an optional policy result object that may be
     * carrying any applicable advices.
     * @return a String representation of a URL that can be used to redirect the
     * current request to the authentication service.
     * @throws AgentException in the event when no login URL for Identity Server
     * authentication service is available.
     */
    public String getAuthRedirectURL(AmWebPolicyResult amWebPolicyResult)
        throws AgentException
    {
        return getAuthRedirectURL(amWebPolicyResult, null);
    }
    
    public String getAuthRedirectURL(String gotoURL) throws AgentException {
        return getAuthRedirectURL(null, gotoURL);
    }

    /**
     * Returns a URL that can be used to redirect the current request which may
     * be lacking sufficient credentials to the Identity Server Authentication
     * service.
     *
     * @param amWebPolicyResult an optional policy result object that may be
     * carrying any applicable advices.
     * @param gotoURL the goto parameter Value
     * @return a String representation of a URL that can be used to redirect the
     * current request to the authentication service.
     * @throws AgentException in the event when no login URL for Identity Server
     * authentication service is available.
     */
    public String getAuthRedirectURL(AmWebPolicyResult amWebPolicyResult,
                                     String gotoURL)
        throws AgentException
    {


        StringBuffer buff     = new StringBuffer();
        String       loginURL = getLoginURL();
        buff.append(loginURL);

        if(loginURL.indexOf("?") != -1) {
            buff.append("&");
        } else {
            buff.append("?");
        }

        if (gotoURL == null) {
            gotoURL = populateGotoParameterValue();
        }

        buff.append(getGotoParameterName());
        buff.append("=");
        buff.append(URLEncoder.encode(gotoURL));

        if((amWebPolicyResult != null) && amWebPolicyResult.hasNameValuePairs()) {
            NameValuePair[] nvp = amWebPolicyResult.getNameValuePairs();

            buff.append("&");

            for(int i = 0; i < nvp.length; i++) {
                buff.append(URLEncoder.encode(nvp[i].getName()));
                buff.append("=");
                buff.append(URLEncoder.encode(nvp[i].getValue()));

                if(i != nvp.length - 1) {
                    buff.append("&");
                }
            }
        }

        String redirectURL = buff.toString();

        return redirectURL;
    }


    /**
     * Returns a URL that can be used to logout the user from the
     * OpenSSO server and redirect the user to a specific URL. 
     * @param gotoURL the redirect URL after logging out 
     * @return a String representation of a URL that can be used to 
     * logout the user from the OpenSSO server and redirect 
     * the user to the gotoURL. 
     */
    public String getLogoutURL(String gotoURL) {
        String logoutURL = null;
        try {
            logoutURL = getLogoutURL();
        } catch (AgentException ae) {
            return null;
        }
      
        if (logoutURL == null) {
            return null;
        } 

        StringBuffer buff = new StringBuffer();
        buff.append(logoutURL);

        if (logoutURL.indexOf("?") != -1) {
            buff.append("&");
        } else {
            buff.append("?");
        }

        if (gotoURL == null) {
            gotoURL = populateGotoParameterValue();
        }

        buff.append(getGotoParameterName());
        buff.append("=");
        buff.append(URLEncoder.encode(gotoURL));
        return buff.toString();
    }


    /**
     * Returns a redirect result that can be used to redirect the user to
     * the sepcified URL.
     *
     * @param redirectURL the URL to which the user will be redirected to.
     * @return an <code>AmFilter</code> to redirect the user to the specified URL.
     */
    public AmFilterResult getCustomRedirectResult(String redirectURL) {
        return new AmFilterResult(AmFilterResultStatus.STATUS_REDIRECT,
                                   redirectURL);
    }

    /**
     * Returns a redirect result that can be used to authenticate the user
     * by Identity Server Authentication service.
     *
     * @return an <code>AmFilterResult</code> to authenticate the user
     *
     * @throws AgentException if the processing of this request results in an
     * unrecoverable error condition.
     */
    public AmFilterResult getAuthRedirectResult() throws AgentException {
        return getAuthRedirectResult(null);
    }

    /**
     * Returns a redirect result that can be used to authenticate the user
     * by Identity Server Authentication service.
     *
     * @param amWebPolicyResult an optional policy result instance which may
     * contain the necessary advices for authenticating the user
     * @return an <code>AmFilterResult</code> to authenticate the user
     *
     * @throws AgentException if the processing of this request results in an
     * unrecoverable error condition.
     */
    public AmFilterResult getAuthRedirectResult(
        AmWebPolicyResult amWebPolicyResult)
        throws AgentException
    {
        return new AmFilterResult(AmFilterResultStatus.STATUS_REDIRECT,
                                   getAuthRedirectURL(amWebPolicyResult));
    }

    /**
     * Returns a redirect result that can be used to authenticate the user
     * by Identity Server Authentication service.
     *
     * @param amWebPolicyResult an optional policy result instance which may
     * contain the necessary advices for authenticating the user
     * @param gotoURL the goto parameter value
     * @return an <code>AmFilterResult</code> to authenticate the user
     *
     * @throws AgentException if the processing of this request results in an
     * unrecoverable error condition.
     */
    public AmFilterResult getAuthRedirectResult(
        AmWebPolicyResult amWebPolicyResult,
        String gotoURL)
        throws AgentException
    {
        return new AmFilterResult(AmFilterResultStatus.STATUS_REDIRECT,
                                  getAuthRedirectURL(amWebPolicyResult,
                                  gotoURL));
    }

    /**
     * Returns a result for serving some specified data for handling the request.
     *
     * @param data the data to be served to the requestor.
     * @return an <code>AmFilterResult</code> to serve the sepcified data for
     * to the requestor.
     */
    public AmFilterResult getServeDataResult(String data) {
        return new AmFilterResult(AmFilterResultStatus.STATUS_SERVE_DATA,
                                    null, data);
    }

    /**
     * Returns an <code>AmFilterResult</code> instance that represents a continue
     * result for filter processing. If a <code>IHttpServletRequestHelper</code>
     * has been set for this request, it will be automatically added to the result.
     *
     * @return an <code>AmFilterResult</code> to allow the reqeust to continue
     */
    public AmFilterResult getContinueResult() {
        AmFilterResult result = new AmFilterResult(
                AmFilterResultStatus.STATUS_CONTINUE);
        if (hasHttpServletRequestHelper()) {
            result.setHttpServletRequestHelper(getHttpServletRequestHelper());
        }
        return result;
    }

    /**
     * Returns an <code>AmFilterResult</code> instance that represents a server error 
     * result for filter processing.
     *
     * @return an <code>AmFilterResult</code> to indicate a server error
     */
    public AmFilterResult getServerErrorResult() {
        return new AmFilterResult(AmFilterResultStatus.STATUS_SERVER_ERROR);
    }

    /**
     * Returns an <code>AmFilterResult</code> instance that represents a blocked
     * result for filter processing.
     *
     * @return an <code>AmFilterResult</code> to block the current request
     */
    public AmFilterResult getBlockAccessResult() {
        return getBlockAccessResult(false);
    }

    /**
     * Returns an <code>AmFilterResult</code> instance that represents a blocked
     * result for filter processing.
     *
     * @param forceForbidden when set to true, will force the result to require
     * the use of HTTP status code 403 in order to block the current request.
     * @return an <code>AmFilterResult</code> to block the current request
     */
    public AmFilterResult getBlockAccessResult(boolean forceForbidden) {

        AmFilterResult result = null;

        if (forceForbidden) {
            result = new AmFilterResult(AmFilterResultStatus.STATUS_FORBIDDEN);
        } else {
            String accessDeniedURI =
                getAccessDeniedURI();

            if (accessDeniedURI != null) {
                String url = getAccessDeniedURL();
                String gotoURL = populateGotoParameterValue();
                if(gotoURL != null){
                   StringBuffer buff     = new StringBuffer(url);
                   buff.append("?");
                   buff.append(getGotoParameterName());
                   buff.append("=");
                   buff.append(URLEncoder.encode(populateGotoParameterValue()));
                   url = buff.toString();
                }

                result =
                    new AmFilterResult(AmFilterResultStatus.STATUS_REDIRECT,
                                       url);
            } else {

                result =
                    new AmFilterResult(AmFilterResultStatus.STATUS_FORBIDDEN);
            }
        }
        result.markBlocked();

        return result;
    }

    /**
     * Returns an <code>AmFilterResult</code> instance that can be used to
     * redirect the incoming request back to itself. This may be necessary in
     * cases such as where the Task Handler has set certain cookies and these
     * cookies must be made available to the downstream application.
     *
     * @return an <code>AmFilterResult</code> that can be used to redirect the
     * request back to its destination thereby making a roundtrip before passing
     * the request to the downstream application.
     */
    public AmFilterResult getRedirectToSelfResult() {
        return getAmFilter().redirectToSelf(this);
    }

    /**
     * Adds a cookie with age set to 0 for the given name, thereby expiring the
     * cookie on the client.
     * @param cookieName the name of the cookie to be expired.
     */
    public void expireCookie(String cookieName) {
        expireCookie(cookieName, null, null);
    }

    /**
     * Adds a cookie with age set to 0 for the given name, thereby expiring the
     * cookie on the client.
     * @param cookieName the name of the cookie to be expired.
     * @param domain the domain of the cookie to be expired
     */
    public void expireCookie(String cookieName, String domain) {
        expireCookie(cookieName, domain, null);
    }

    /**
     * Adds a cookie with age set to 0 for the given name, thereby expiring the
     * cookie on the client.
     * @param cookieName the name of the cookie to be expired.
     * @param domain the domain of the cookie to be expired
     * @param path the path of the cookie to be expired
     */
    public void expireCookie(String cookieName, String domain, String path) {
        Cookie expiredCookie =
            CookieUtils.getExpiredCookie(cookieName, domain, path);
        getHttpServletResponse().addCookie(expiredCookie);
    }

    /**
     * Returns the DN of the currently authenticated user. If the task handler
     * specific to SSO validation has not been invoked yet, the user is considered
     * unauthenticated and therefore this method will return a <code>null</code>
     * value.
     *
     * @return the DN of the currently authenticated user or <code>null</code> if
     * the current request has not been subjected to SSO validation yet.
     */
    public String getUserPrincipal() {
        return (getSSOValidationResult() != null) ?
            getSSOValidationResult().getUserPrincipal():null;
    }

    /**
     * Returns the user name for the currently authenticated user. If the property
     * use dn is set to true, the name will be the user DN. If set to false, this
     * value will be the UID of the user. This method will return <code>null</code>
     * if the current user has not been authenticated yet.
     *
     * @return the user name of the currently authenticated user, or <code>null</code>
     * otherwise.
     */
    public String getUserId() {
        return (getSSOValidationResult() != null) ?
            getSSOValidationResult().getUserId(): null;

    }

    /**
     * Returns the instance of <code>IHttpServletRequestHelper</code> that can
     * be used to wrap this request.
     * @return the instnace of <code>IHttpServletRequestHelper</code> if available
     * or <code>null</code> if no such instnace is available.
     */
    public IHttpServletRequestHelper getHttpServletRequestHelper() {
        return _httpServletRequestHelper;
    }

    /**
     * Returns true if the current request context has an instnace of
     * <code>IHttpServletRequestHelper</code> available for wrapping the reqeust.
     *
     * @return true if an instance of <code>IHttpServletRequestHelper</code> is
     * available, false otherwise.
     */
    public boolean hasHttpServletRequestHelper() {
        return getHttpServletRequestHelper() != null;
    }

    /**
     * Returns a boolean indicating if the current request is that for a J2EE
     * form login.
     * @return true if the current request is that for a form login, false
     * otherwise.
     */
    public boolean isFormLoginRequest() {
        return _isFormLoginRequestFlag;
    }

    /**
     * Returns the <code>SSOValidationResult</code> object for the current
     * request if available. This instance is created and made available only
     * after the SSO task handler has operated on this request. If the SSO task
     * handler was not invoked prior to this method call, or if the SSO validation
     * failed for the current user, the return from this method will be <code>null</code>.
     * @return the <code>SSOValidationResult</code> object for the current request
     * if available, <code>null</code> otherwise.
     */
    public SSOValidationResult getSSOValidationResult() {
        return _ssoValidationResult;
    }

    /**
     * Returns the access denied URL to be used for blocking this request
     * if necessary. If the access denied URI is not specified, the return
     * will be a <code>null</code> value.
     *
     * @return the access denied URL or <code>null</code> if the access denied
     * URI is not specified.
     */
    public String getAccessDeniedURL() {
        if (getAccessDeniedURI() != null) {
            if (_accessDeniedURL == null) {
                initAccessDeniedURL();
            }
        }

        return _accessDeniedURL;
    }
    
    /**
     * Returns the base URL for this request. The base URL simply specifies the
     * protocol, host and port information and does not contain any URI or
     * query strings.
     *
     * @return the base URL tbe used for this request
     */
    public String getBaseURL() {
    	if (_baseURL == null) {
    		initBaseURL();
    	}
    	return _baseURL;
    }    

    /**
     * Sets the <code>IHttpServletRequestHelper</code> instance where needed.
     * @param httpServletRequestHelper the <code>IHttpServletRequestHelper</code>
     * instance that will be used for wrapping this request.
     */
    public void addHttpServletRequestHelper(IHttpServletRequestHelper helper) {
        if (_httpServletRequestHelper == null) {
            _httpServletRequestHelper = helper;
        } else {
            _httpServletRequestHelper.addUserAttributes(
                    helper.getUserAttributes());
        }
    }
    
   /**
    * Sets a <code>Map</code> that contains the response attributes as evaluated
    * by the remote policy.
    * @param responseAttributes
    */
    public void setPolicyResponseAttributes(Map responseAttributes) {
        _policyResponseAttributes = responseAttributes;
    }
    
   /**
    * Returns the response attributes associated with the current request as
    * determined by remote policy evaluation. This method can return 
    * <code>null</code> if no attributes have been set.
    * 
    * @return
    */
    public Map getPolicyResponseAttributes() {
        return _policyResponseAttributes;
    }

    /**
     * Sets the <code>SSOValidationResult</code> instance where needed. This
     * is done by the SSO Task Handler on successful validation of the user
     * session.
     *
     * @param ssoValidationResult the <code>SSOValidationResult</code> instance
     * that may be used by various task handlers downstream to facilitate the
     * further processing of the request.
     */
    public void setSSOValidationResult(SSOValidationResult ssoValidationResult) 
    {
        _ssoValidationResult = ssoValidationResult;
    }
    
    public boolean isAuthenticated() {
    	boolean result = false;
    	if (_ssoValidationResult != null && _ssoValidationResult.isValid()) {
    		result = true;
    	}
    	
    	return result;
    }

    private void setAccessDeniedURL(String url) {
        _accessDeniedURL = url;
    }

    /**
     * Initializes the access denied URL. The access denied URL is constructed
     * using the current request's base URL followed by the access denied URI
     * as specified in the agent configuration.
     *
     */
    private void initAccessDeniedURL() {
        setAccessDeniedURL(getBaseURL() + getAccessDeniedURI());
    }

    private Map getCookieMap() {
        return _cookieMap;
    }

    private void setCookieMap(Map cookieMap) {
        _cookieMap = cookieMap;
    }

    private void initCookieMap() {
        setCookieMap(
            CookieUtils.getRequestCookies(getHttpServletRequest()));
    }

    private void setDestinationURL(String url) {
        _destinationURL = url;
    }
    
    public void setGotoFormLoginURL(String url) throws AgentException {
        try {
            URL u = new URL(url);
            _gotoFormLoginURL = getBaseURL() + u.getFile();
        } catch (MalformedURLException me) {
            throw new AgentException("Invalid form login url" + url, me);
        }
    }    
    
    private void setPolicyDestinationURL(String url) {
        _policyDestinationURL = url;
    }

    /**
     * Initializes the destination URL. The destination URL is the same as
     * the URL requested by the client browser including the necessary
     * request URI and query strings if applicable.
     */
    private void initDestinationURL() {
        String       queryString = getHttpServletRequest().getQueryString();
        StringBuffer buff        = new StringBuffer();
        buff.append(getBaseURL());
        buff.append(getHttpServletRequest().getRequestURI());

        if(queryString != null) {
            if (!queryString.startsWith("?")) {
                buff.append("?");
            }
            buff.append(queryString);
        }

        setDestinationURL(buff.toString());
    }
    
    private void initPolicyDestinationURL() {
        String queryString = StringUtils.removeQueryParameter(
                getHttpServletRequest().getQueryString(), 
                AgentConfiguration.getSSOTokenName());
        StringBuffer buff = new StringBuffer();
        buff.append(getBaseURL());
        buff.append(getHttpServletRequest().getRequestURI());

        if(queryString != null) {
            if (!queryString.startsWith("?")) {
                buff.append("?");
            }
            buff.append(queryString);
        }

        setPolicyDestinationURL(buff.toString());
    }

    void setRequest(HttpServletRequest request) {
        _request = request;
    }

    void setResponse(HttpServletResponse response) {
        _response = response;
    }

    private void setGotoParameterName(String gotoPrameterName) {
        _gotoParameterName = gotoPrameterName;
    }

    private String getLoginURL() throws AgentException {
        return getLoginURLFailoverHelper().getAvailableURL(this);
    }

    private String getLogoutURL() throws AgentException {
        return getLogoutURLFailoverHelper().getAvailableURL(this);
    }

    private IURLFailoverHelper getLoginURLFailoverHelper() {
        return _loginURLFailoverHelper;
    }

    private IURLFailoverHelper getLogoutURLFailoverHelper() {
        return _logoutURLFailoverHelper;
    }

    private void setLoginURLFailoverHelper(IURLFailoverHelper loginURLFailoverHelper) {
        _loginURLFailoverHelper = loginURLFailoverHelper;
    }

    private void setLogoutURLFailoverHelper(IURLFailoverHelper logoutURLFailoverHelper) {
        _logoutURLFailoverHelper = logoutURLFailoverHelper;
    }

    private void setAccessDeniedURI(String accessDeniedURI) {
        _accessDeniedURI = accessDeniedURI;
    }

    private void setFilterMode(AmFilterMode filterMode) {
        _filterMode = filterMode;
    }

    String getAccessDeniedURI() {
        return _accessDeniedURI;
    }

    private void setAmFilter(IAmFilter amFilter) {
        _amFilter = amFilter;
    }

    private IAmFilter getAmFilter() {
        return _amFilter;
    }

    private void setIsFormLoginRequestFlag(boolean isFormLoginRequest) {
        _isFormLoginRequestFlag = isFormLoginRequest;
    }

    /**
     * Initializes the base URL to be used for this request. The base URL simply
     * specifies the protocol, host and port information and does nto contain any
     * URI or query strings.
     *
     */
    private void initBaseURL() {
        //FIXME use entry point definitions
		StringBuffer buff = new StringBuffer();
		buff.append(getAgentProtocol());
		buff.append("://");
		buff.append(getAgentHost());
		buff.append(":");
		buff.append(getAgentPort());
		_baseURL = buff.toString();
    }

    public String getApplicationContextURL() {
        if (_applicationContextURL == null) {
            initApplicationContextURL();
        }
        return _applicationContextURL;
    }

    private void initApplicationContextURL() {
        _applicationContextURL = getBaseURL() +
            getHttpServletRequest().getContextPath();
    }
    
    /**
     * Sets the base URL for this request. The base URL simply specifies the
     * protocol, host and port information and does not contain any URI or
     * query strings.
     *
     * @param baseURL the base URL to be set for this request
     */
    private void setBaseURL(String baseURL) {
    	_baseURL = baseURL;
    }
    
    private void setAgentHost(String agentHost) {
        _agentHost = agentHost;
    }
    
    public String getAgentHost() {
        return _agentHost;
    }
    
    private void setAgentPort(int agentPort) {
        _agentPort = agentPort;
    }
    
    public int getAgentPort() {
        return _agentPort;
    }
    
    private void setAgentProtocol(String agentProtocol) {
        _agentProtocol = agentProtocol;
    }
    
    public String getAgentProtocol() {
        return _agentProtocol;
    }

    private HttpServletRequest _request;
    private HttpServletResponse _response;
    private String _gotoParameterName;
    private String _destinationURL;
    private String _gotoFormLoginURL;
    private String _policyDestinationURL;
    private Map _cookieMap;
    private IURLFailoverHelper _loginURLFailoverHelper;
    private IURLFailoverHelper _logoutURLFailoverHelper;
    private boolean _isFormLoginRequestFlag;
    private String _accessDeniedURI;
    private String _accessDeniedURL;
    private IAmFilter _amFilter;
    private IHttpServletRequestHelper _httpServletRequestHelper;
    private SSOValidationResult _ssoValidationResult;
    private String _baseURL;
    private String _applicationContextURL;
    private AmFilterMode _filterMode;
    private Map _policyResponseAttributes;
    private String _agentHost;
    private int _agentPort;
    private String _agentProtocol;
}
