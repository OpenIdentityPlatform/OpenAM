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
 * $Id: WebServiceTaskHandler.java,v 1.5 2008/10/07 17:32:31 huacui Exp $
 *
 */

package com.sun.identity.agents.filter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.iplanet.sso.SSOToken;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.common.CommonFactory;
import com.sun.identity.agents.common.IHttpServletRequestHelper;
import com.sun.identity.agents.common.IProfileAttributeHelper;
import com.sun.identity.agents.common.SSOValidationResult;
import com.sun.identity.agents.policy.AmWebPolicyManager;
import com.sun.identity.agents.policy.AmWebPolicyResult;
import com.sun.identity.agents.policy.AmWebPolicyResultStatus;
import com.sun.identity.agents.policy.IAmWebPolicy;
import com.sun.identity.agents.util.CommonAttributeUtils;
import com.sun.identity.agents.util.StringUtils;

public class WebServiceTaskHandler extends LocalAuthTaskHandler implements
        IWebServiceTaskHandler {
    
    /**
     * The constructor that takes a <code>Manager</code> intance in order
     * to gain access to the infrastructure services such as configuration
     * and log access.
     *
     * @param manager the <code>Manager</code> for the <code>filter</code>
     * subsystem.
     */
    public WebServiceTaskHandler(Manager manager) {
        super(manager);
    }
    
    public void initialize(ISSOContext context, AmFilterMode mode)
    throws AgentException {
        super.initialize(context, mode);
        initWebServicesEnabledFlag();
        
        if (isActive()) {
            setCommonFactory(new CommonFactory(getModule()));
            initAttributeDateFormatString();
            initProcessGetRequestsFlag();
            initWebServiceAuthenticator();
            initWebServiceInternalErrorContent();
            initWebServiceAuthErrorContent();
            if (mode.equals(AmFilterMode.MODE_URL_POLICY)
            || mode.equals(AmFilterMode.MODE_ALL)) {
                initAmWebPolicy();
                initFetchPolicyResponseAttributeFlag();
            }
            initFetchProfileAttributeFlag();
            initFetchSessionAttributeFlag();
        }
    }
    
    /**
     * <p>
     * Processes the request to see if it is a Web Service invocation request.
     * This task handler uses a list of configured URIs in Agent Configuration
     * to determine if a particular URI can be identified as a Web Service
     * frontend. If the request URI matches with any of the configured URIs,
     * this task handler initiates its processing to identify the appropriate
     * response for this request.
     * </p><p>
     * A web service request is treated differently depending upon whether the
     * HTTP method used for invoking the request is <code>GET</code> or
     * <code>POST</code>. If the method is <code>GET</code>, the request is
     * considered a normal web-client request and passed down for further
     * processing. This behavior can be changed so that this request is
     * effectively treated as not-enforced request by setting a flag in the
     * Agent Configuration to disable processing of <code>GET</code> requests.
     * </p><p>
     * If the request uses <code>POST</code> method of invocation, this task
     * handler extracts the request message and passes it on to the configured
     * <code>IWebServiceAuthenticator</code> implementation. For a valid request
     * the authenticator produces a <code>SSOToken</code> which is then used
     * to enforce further checks such as URL policy and J2EE local
     * authentication checks depending upon the filter operation mode. If
     * all checks are successful, the task handler populates the necessary
     * request attributes and generates the continue decision for the request.
     * </p><p>
     * If any of the authorization checks fail, a auth-error fault is generated
     * by the task handler. If any other exception occurs, an internal-error
     * fault is generated instead.
     * </p>
     *
     * @param ctx - the context of the current request
     * @return a <code>AmFilterResult</code> to indicate the necessary action
     * for handling the request, or <code>null</code> if no action is required
     * for this request by this task handler.
     * @throws AgentException if an unrecoverable error condition occurs during
     * the processing of this request.
     */
    public AmFilterResult process(AmFilterRequestContext ctx)
    throws AgentException {
        AmFilterResult result = null;
        String requestURL = null;
        if (isLogMessageEnabled()) {
           logMessage("WebServiceTaskHandler.process: now processing");
        }
        try {
            if (isActive()) {
                requestURL = ctx.getDestinationURL();
                HttpServletRequest request = ctx.getHttpServletRequest();
                if (getWebServiceEndpoints().contains(
                        request.getRequestURI())) {
                    String method = request.getMethod();
                    if (method.equalsIgnoreCase(HTTP_METHOD_GET)) {
                        if (!isGetProcesssingEnabled()) {
                            result = ctx.getContinueResult();
                            if (isLogMessageEnabled()) {
                                logMessage(
                                        "WebServiceTaskHandler: no processing"
                                        + "for GET request of WS endpoint: "
                                        + request.getRequestURI());
                            }
                        } else {
                            if (isLogMessageEnabled()) {
                                logMessage("WebServiceTaskHandler: regular "
                                        + "processing for GET request of WS "
                                        + "endpoint: "
                                        + request.getRequestURI());
                            }
                        }
                    } else if (method.equals(HTTP_METHOD_POST)) {
                        result = processWebServiceRequest(ctx, requestURL);
                        if (result != null) {
                            result.setProcessResponseFlag(true);
                        }
                    } else {
                        logError(
                            "WebServiceTaskHandler: Unknown request method: "
                            + method);
                        result = getInternalErrorResult(requestURL);
                        result.setProcessResponseFlag(true);
                    }
                }
            }
        } catch (Exception ex) {
            logError("WebServiceTaskHandler: Exception caught", ex);
            result = getInternalErrorResult(requestURL);
            result.setProcessResponseFlag(true);
        }
        return result;
    }
    
    public boolean isActive() {
        return isWebServicesEnabled();
    }
    
    public String getHandlerName() {
        return AM_FILTER_WEBSERVICE_TASK_HANDLER_NAME;
    }
    
    private AmFilterResult processWebServiceRequest(AmFilterRequestContext ctx,
            String requestURL) throws Exception {
        AmFilterResult result = null;
        HttpServletRequest request = ctx.getHttpServletRequest();
        String requestBody = getRequestBody(request);
        Map responseAttributes = null;
        if (isLogMessageEnabled()) {
            logMessage("WebServiceTaskHandler: Request Body: \n"
                    + requestBody);
        }
        SSOToken ssoToken = getWebServiceAuthenticator().getUserToken(
                request, requestBody, getClientIPAddress(request),
                getClientHostName(request), ctx);
        if (ssoToken == null) {
            if (isLogWarningEnabled()) {
                logWarning("WebServiceTaskHandler: authentication failed for "
                        + "webservice endpoint: " + request.getRequestURI());
            }
            result = getAuthorizationErrorResult(requestURL);
        } else {
            
            SSOValidationResult ssoValidationResult =
                    getSSOContext().getSSOTokenValidator().validate(
                    ssoToken.getTokenID().toString(),
                    ctx.getHttpServletRequest());
            
            ctx.setSSOValidationResult(ssoValidationResult);
            
            if (ssoValidationResult.isValid()) {
                if (isLogMessageEnabled()) {
                    logMessage(
                            "WebServiceTaskHandler: authentication successful "
                            + "for webservice endpoint: "
                            + request.getRequestURI()
                            + ", principal: "
                            + ssoValidationResult.getUserPrincipal());
                }
                
                if (isModeURLPolicyActive()) {
                    AmWebPolicyResult amWebPolicyResult = getAmWebPolicy()
                    .checkPolicyForResource(ssoToken, requestURL,
                            request.getMethod(),
                            getClientIPAddress(request),
                            getClientHostName(request),
                            request);
                    
                    if (amWebPolicyResult.getPolicyResultStatus().equals(
                            AmWebPolicyResultStatus.STATUS_ALLOW)) {
                        if (isLogMessageEnabled()) {
                            logMessage(
                                    "WebServiceTaskHandler: Access allowed by "
                                    + "Web Policy");
                        }
                        
                        if (isResponseAttributeFetchEnabled()) {
                            responseAttributes =
                                    amWebPolicyResult.getResponseAttributes();
                        }
                        
                    } else {
                        if (isLogMessageEnabled()) {
                            logMessage(
                                    "WebServiceTaskHandler: Access denied by "
                                    + "Web Policy");
                        }
                        result = getAuthorizationErrorResult(requestURL);
                    }
                }
                
                if (result == null && isModeJ2EEPolicyActive()) {
                    boolean authenticationSuccessful = authenticate(ctx);
                    if (!authenticationSuccessful) {
                        result = getAuthorizationErrorResult(requestURL);
                        if (isLogMessageEnabled()) {
                            logMessage("WebServiceTaskHandler: local auth "
                                    + "failed");
                        }
                    } else {
                        if (isLogMessageEnabled()) {
                            logMessage("WebServiceTaskHandler: local auth "
                                    + "successful");
                        }
                    }
                }
                if (result == null) {
                    request = ctx.getHttpServletRequest();
                    requestBody = getRequestBody(ctx.getHttpServletRequest());  
                    if (isLogMessageEnabled()) {
                        logMessage("WebServiceTaskHandler: requestBody=" + 
                                   requestBody);
                    }
                    result = getAllowResult(ssoValidationResult,
                            responseAttributes, requestBody,
                            requestURL, request);
                }
                
            } else {
                if (isLogWarningEnabled()) {
                    logWarning("WebServiceTaskHandler: authentication failed "
                            + "for webservice endpoint: "
                            + request.getRequestURI());
                }
                result = getAuthorizationErrorResult(requestURL);
            }
        }
        
        return result;
    }
    
    private AmFilterResult getAllowResult(
            SSOValidationResult ssoValidationResult, Map allResponseAttributes,
            String requestBody, String requestURL, HttpServletRequest request)
            throws Exception {
        AmFilterResult result = null;
        
        Map headerAttributes = new HashMap();
        Map requestAttributes = new HashMap();
        
        if (isResponseAttributeFetchEnabled()) {
            Map responseAttributes =
                    getResponseAttributes(allResponseAttributes);
            if (getResponseAttributeFetchMode().equals(
                    AttributeFetchMode.MODE_HTTP_HEADER)) {
                headerAttributes.putAll(responseAttributes);
            } else if (getResponseAttributeFetchMode().equals(
                    AttributeFetchMode.MODE_REQUEST_ATTRIBUTE)) {
                requestAttributes.putAll(responseAttributes);
            }
        }
        
        if (isProfileAttributeFetchEnabled()) {
            SSOToken ssoToken = ssoValidationResult.getSSOToken();
            Map profileAttributes =
                    getProfileAttributeHelper().getAttributeMap(
                    ssoToken, getProfileAttributeQueryMap());
            
            if (profileAttributes != null) {
                AttributeFetchMode profileAttrMode =
                        getProfileAttributeFetchMode();
                if (profileAttrMode.equals(
                        AttributeFetchMode.MODE_HTTP_HEADER)) {
                    CommonAttributeUtils.mergeAttributes(
                            headerAttributes,
                            profileAttributes);
                } else if (profileAttrMode.equals(
                        AttributeFetchMode.MODE_REQUEST_ATTRIBUTE)) {
                    CommonAttributeUtils.mergeAttributes(
                            requestAttributes,
                            profileAttributes);
                }
            }
        }
        
        if (isSessionAttributeFetchEnabled()) {
            Map sessionAttributes =
                    getSessionAttributes(ssoValidationResult);
            
            if (sessionAttributes != null) {
                AttributeFetchMode sessionAttrMode =
                        getSessionAttributeFetchMode();
                if (sessionAttrMode.equals(
                        AttributeFetchMode.MODE_HTTP_HEADER)) {
                    CommonAttributeUtils.mergeAttributes(
                            headerAttributes,
                            sessionAttributes);
                } else if (sessionAttrMode.equals(
                        AttributeFetchMode.MODE_REQUEST_ATTRIBUTE)){
                    CommonAttributeUtils.mergeAttributes(
                            requestAttributes,
                            sessionAttributes);
                }
                
            }
        }
        
        if (isLogMessageEnabled()) {
            logMessage("WebServiceTaskHandler: headerAttributes: "
                    + headerAttributes);
            logMessage("WebServiceTaskHandler: requestAttributes "
                    + requestAttributes);
        }
        
        IHttpServletRequestHelper helper =
                getCommonFactory().newServletRequestHelper(
                getAttributeDateFomratString(),
                headerAttributes,
                new WebServiceRequestInputStream(requestBody,
                request.getCharacterEncoding()));
        
        if (requestAttributes.size() > 0) {
            Iterator it = requestAttributes.keySet().iterator();
            while (it.hasNext()) {
                String nextKey = (String) it.next();
                Set nextValueSet = (Set)
                requestAttributes.get(nextKey);
                request.setAttribute(nextKey, nextValueSet);
            }
        }
        
        result = new AmFilterResult(
                AmFilterResultStatus.STATUS_CONTINUE);
        result.setHttpServletRequestHelper(helper);
        
        if (isLogMessageEnabled()) {
            logMessage("WebServiceTaskHandler: Allowing access to "
                    + "request " + requestURL);
        }
        return result;
    }
    
    private Map getResponseAttributes(Map allResponseAttributes) {
        Map result = new HashMap();
        Map queryMap = getResponseAttributeQueryMap();
        Iterator it = queryMap.keySet().iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            Set value = (Set) allResponseAttributes.get(name);
            if (value == null) {
                value = Collections.EMPTY_SET;
            }
            String mappedName = (String) queryMap.get(name);
            result.put(mappedName, value);
        }
        return result;
    }
    
    private Map getSessionAttributes(SSOValidationResult ssoValidationResult)
    throws AgentException {
        Map result = new HashMap();
        try {
            SSOToken token = ssoValidationResult.getSSOToken();
            if (token != null) {
                Map queryMap = getSessionAttributeQueryMap();
                
                Iterator it = queryMap.keySet().iterator();
                while (it.hasNext()) {
                    String sessionPropName = (String) it.next();
                    String userAttributeName = (String)
                    queryMap.get(sessionPropName);
                    Set valueSet = new HashSet();
                    String value = token.getProperty(sessionPropName);
                    if (value != null) {
                        valueSet.add(value);
                    }
                    result.put(userAttributeName, valueSet);
                }
            }
        } catch (Exception ex) {
            throw new AgentException("Unable to get session attributes", ex);
        }
        return result;
    }
    
    private AmFilterResult getAuthorizationErrorResult(String requestURL)
    throws AgentException {
        AmFilterResult result = new AmFilterResult(
                AmFilterResultStatus.STATUS_SERVE_DATA,
                null, getAuthErrorContentForURL(requestURL));
        result.setRequestURL(requestURL);
        result.markBlocked();
        return result;
    }
    
    private AmFilterResult getInternalErrorResult(String requestURL)
    throws AgentException {
        AmFilterResult result = new AmFilterResult(
                AmFilterResultStatus.STATUS_SERVE_DATA,
                null,  getInternalErrorContentForURL(requestURL));
        result.setRequestURL(requestURL);
        result.markBlocked();
        return result;
    }
    
    private String getAuthErrorContentForURL(String requestURL)
    throws AgentException {
        return getErrorContentForURL(getAuthErrorContent(), requestURL);
    }
    
    private String getInternalErrorContentForURL(String requestURL)
    throws AgentException {
        return  getErrorContentForURL(getInternalErrorContent(),
                requestURL);
    }
    
    private String getErrorContentForURL(String errorTemplate, String url)
    throws AgentException {
        String result = null;
        try {
            StringBuffer buff = new StringBuffer(errorTemplate);
            StringUtils.replaceString(buff, AM_FILTER_WS_END_POINT, url);
            result = buff.toString();
        } catch (Exception ex) {
            throw new AgentException("Unable to process content", ex);
        }
        return result;
    }
    
    private void initAmWebPolicy() throws AgentException {
        setAmWebPolicy(AmWebPolicyManager.getAmWebPolicyInstance());
    }
    
    private void initWebServiceInternalErrorContent() throws AgentException {
        setInternalErrorContent(getContentString(
                CONFIG_WEBSERVICE_INTERNAL_ERROR_FILE,
                DEFAULT_WEBSERVICE_INTERNAL_ERROR_FILE));
    }
    
    private void initWebServiceAuthErrorContent() throws AgentException {
        setAuthErrorContent(getContentString(
                CONFIG_WEBSERVICE_AUTH_ERROR_FILE,
                DEFAULT_WEBSERVICE_AUTH_ERROR_FILE));
    }
    
    private void initWebServiceAuthenticator() throws AgentException {
        try {
            IWebServiceAuthenticator wsAuthenticator = null;
            String wsAuthenticatorImpl = getConfiguration(
                    CONFIG_WEBSERVICE_AUTHENTICATOR_IMPL,
                    getResolver().getDefaultWebServiceAuthenticatorImpl());
            if (wsAuthenticatorImpl != null
                    && wsAuthenticatorImpl.trim().length() > 0) {
                if (isLogMessageEnabled()) {
                    logMessage("WebServiceTaskHandler: WS Authenticator impl:"
                            + wsAuthenticatorImpl);
                }
                
                wsAuthenticator = (IWebServiceAuthenticator)
                Class.forName(wsAuthenticatorImpl).newInstance();
                
                setWsAuthenticator(wsAuthenticator);
                
            } else {
                throw new AgentException("No WebServiceAuthenticator found");
            }
            
        } catch (Exception ex) {
            logError("WebServiceTaskHandler: Initialization failed", ex);
            throw new AgentException("Initialization failed", ex);
        }
    }
    
    private void initProcessGetRequestsFlag() {
        setProcessGetRequestFlag(getConfigurationBoolean(
                CONFIG_WEBSERVICE_PROCESS_GET,
                DEFAULT_WEBSERVICE_PROCESS_GET));
    }
    
    private void initWebServicesEnabledFlag() {
        boolean wsEnabled = getConfigurationBoolean(
                CONFIG_WEBSERVICE_ENABLE_FLAG,
                DEFAULT_WEBSERVICE_ENABLE_FLAG);
        if (wsEnabled) {
            String[] endPoints = getConfigurationStrings(
                    CONFIG_WEBSERVICE_END_POINT);
            if (endPoints != null && endPoints.length > 0) {
                HashSet set = new HashSet();
                for (int i=0; i<endPoints.length; i++) {
                    if (endPoints[i] != null 
                            && endPoints[i].trim().length()>0) {
                        set.add(endPoints[i]);
                    }
                    if (set.size() > 0) {
                        setWebServicesEnabledFlag(true);
                        getWebServiceEndpoints().addAll(set);
                        if (isLogMessageEnabled()) {
                            logMessage("WebServiceTaskHandler: "
                                    + "The following end points are configured:"
                                    + set);
                        }
                    } else {
                        if (isLogMessageEnabled()) {
                            logMessage(
                                "WebServiceTaskHandler: no WS endpoints found");
                        }
                    }
                }
            } else {
                if (isLogMessageEnabled()) {
                    logMessage("WebServiceTaskHandler: no WS endpoints set");
                }
            }
        } else {
            if (isLogMessageEnabled()) {
                logMessage("WebServiceTaskHandler: disabled by configuration");
            }
        }
    }
    
    private String getContentString(String configKey, String defaultFileName)
    throws AgentException {
        String result = null;
        String fileName = getConfigurationString(configKey, defaultFileName);
        if (isLogMessageEnabled()) {
            logMessage("WebServiceTaskHandler: " + configKey + " file is: "
                    + fileName);
        }
        InputStream inStream = null;
        try {
            inStream = ClassLoader.getSystemResourceAsStream(fileName);
        } catch (Exception ex) {
            if (isLogWarningEnabled()) {
                logWarning(
                        "WebServiceTaskHandler: Exception while trying to get "
                        + "file " + fileName, ex);
            }
        }

        if( inStream == null ) {
            try {
                ClassLoader cl = 
                    Thread.currentThread().getContextClassLoader();
                if( cl != null ) 
                    inStream = cl.getResourceAsStream(fileName);

            } catch(Exception ex) {
                if (isLogWarningEnabled()) {
                    logWarning(
                     "WebServiceTaskHandler: Exception while trying to get "
                     + "for file " + fileName, ex);
                }
            }
        }
        
        if (inStream == null) {
            try {
                File contentFile = new File(fileName);
                if (!contentFile.exists() || !contentFile.canRead()) {
                    throw new AgentException("Unable to read content "
                            + fileName);
                }
                inStream = new FileInputStream(contentFile);
            } catch (Exception ex) {
                logError("WebServiceTaskHandler: Unable to read  content "
                        + fileName, ex);
                throw new AgentException("Unable to read port content "
                        + fileName, ex);
            }
        }
        
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inStream));
            StringBuffer contentBuffer = new StringBuffer();
            String       nextLine      = null;
            
            while ((nextLine = reader.readLine()) != null) {
                contentBuffer.append(nextLine);
                contentBuffer.append(NEW_LINE);
            }
            
            result = contentBuffer.toString();
            if (isLogMessageEnabled()) {
                logMessage("WebServiceTaskHandler: content for " + configKey
                        + " is: " + NEW_LINE + result);
            }
        } catch (Exception ex) {
            throw new AgentException("Unable to read content file: "
                    + fileName, ex);
        } finally {
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (Exception ex) {
                    logError("Exception while trying to close input stream "
                            + "for content file: " + fileName, ex);
                }
            }
        }
        
        if (result == null || result.trim().length() == 0) {
            throw new AgentException("Unable to get content for " + configKey);
        }
        
        return result;
    }
    
    private boolean isWebServicesEnabled() {
        return _webServicesEnabled;
    }
    
    private void setWebServicesEnabledFlag(boolean flag) {
        _webServicesEnabled = flag;
        if (isLogMessageEnabled()) {
            logMessage("WebServiceTaskHandler: enabled flag set to: " +
                    _webServicesEnabled);
        }
    }
    
    private HashSet getWebServiceEndpoints() {
        return _webServiceEndpoints;
    }
    
    private IWebServiceAuthenticator getWebServiceAuthenticator() {
        return _webServiceAuthenticator;
    }
    
    private void setWsAuthenticator(IWebServiceAuthenticator wsAuthenticator) {
        _webServiceAuthenticator = wsAuthenticator;
    }
    
    private String getInternalErrorContent() {
        return _internalErrorContent;
    }
    
    private void setInternalErrorContent(String content) {
        _internalErrorContent = content;
    }
    
    private String getAuthErrorContent() {
        return _authErrorContent;
    }
    
    private void setAuthErrorContent(String content) {
        _authErrorContent = content;
    }
    
    private IAmWebPolicy getAmWebPolicy() {
        return _amWebPolicy;
    }
    
    private void setAmWebPolicy(IAmWebPolicy amWebPolicy) {
        _amWebPolicy = amWebPolicy;
    }
    
    private boolean isGetProcesssingEnabled() {
        return _processGetRequests;
    }
    
    private void setProcessGetRequestFlag(boolean flag) {
        _processGetRequests = flag;
        if (isLogMessageEnabled()) {
            logMessage("WebServiceTaskHandler: GET processing enabled: "
                    + _processGetRequests);
        }
    }
    
    private void initAttributeDateFormatString() {
        setAttributeDateFormatString(getConfigurationString(
                CONFIG_ATTRIBUTE_DATE_FORMAT,
                DEFAULT_DATE_FORMAT_STRING));
    }
    
    private String getAttributeDateFomratString() {
        return _attributeDateFormatString;
    }
    
    private void setAttributeDateFormatString(String dateFormatString) {
        _attributeDateFormatString = dateFormatString;
        if (isLogMessageEnabled()) {
            logMessage("WebServiceTaskHandler: attribute date format string: "
                    + _attributeDateFormatString);
        }
    }
    
    private CommonFactory getCommonFactory() {
        return _commonFactory;
    }
    
    private void setCommonFactory(CommonFactory cf) {
        _commonFactory = cf;
    }
    
    private boolean isResponseAttributeFetchEnabled() {
        return _fetchResponseAttributes;
    }
    
    private void initFetchPolicyResponseAttributeFlag() {
        AttributeFetchMode mode = AttributeFetchMode.get(
                getConfigurationString(CONFIG_RESPONSE_ATTRIBUTE_FETCH_MODE));
        
        if (isValidFetchMode(mode)) {
            Map queryMap = getConfigurationMap(CONFIG_RESPONSE_ATTRIBUTE_MAP);
            if (queryMap != null && queryMap.size() > 0) {
                setFetchPolicyResponseAttributesFlag(true);
                setResponseAttributeQueryMap(queryMap);
                setResponseAttributeFetchMode(mode);
            }
        }
    }
    
    private boolean isValidFetchMode(AttributeFetchMode mode) {
        boolean result = false;
        if (mode != null) {
            if (mode.equals(AttributeFetchMode.MODE_HTTP_HEADER)
            || mode.equals(AttributeFetchMode.MODE_REQUEST_ATTRIBUTE)) {
                result = true;
            }
        }
        
        return result;
    }
    
    private void setFetchPolicyResponseAttributesFlag(boolean flag) {
        _fetchResponseAttributes = flag;
        if (isLogMessageEnabled()) {
            logMessage("WebServiceTaskHandler: policy response attribute fetch"
                    + " enabled: " + _fetchResponseAttributes);
        }
    }
    
    private AttributeFetchMode getResponseAttributeFetchMode() {
        return _responseAttributeFetchMode;
    }
    
    private void setResponseAttributeFetchMode(AttributeFetchMode mode){
        _responseAttributeFetchMode = mode;
        if (isLogMessageEnabled()) {
            logMessage("WebServiceTaskHandler: response attribute " +
                    "fetch mode: " + _responseAttributeFetchMode);
        }
    }
    
    private void initFetchProfileAttributeFlag() throws AgentException {
        AttributeFetchMode mode = AttributeFetchMode.get(
                getConfigurationString(CONFIG_PROFILE_ATTRIBUTE_FETCH_MODE));
        if (isValidFetchMode(mode)) {
            Map queryMap = getConfigurationMap(CONFIG_PROFILE_ATTRIBUTE_MAP);
            if (queryMap != null && queryMap.size() > 0) {
                setFetchProfileAttributeFlag(true);
                setProfileAttributeHelper(
                        getCommonFactory().newProfileAttributeHelper());
                setProfileAttributeQueryMap(queryMap);
                setPorfileAttributeFetchMode(mode);
            }
        }
    }
    
    private void setProfileAttributeHelper(IProfileAttributeHelper helper) {
        _profileAttributeHelper = helper;
    }
    
    private IProfileAttributeHelper getProfileAttributeHelper() {
        return _profileAttributeHelper;
    }
    
    private Map getProfileAttributeQueryMap() {
        return _profileAttributeQueryMap;
    }
    
    private void setProfileAttributeQueryMap(Map map) {
        _profileAttributeQueryMap = map;
        if (isLogMessageEnabled()) {
            logMessage("WebServiceTaskHandler: profile attribute query map: "
                    + _profileAttributeQueryMap);
        }
    }
    
    private boolean isProfileAttributeFetchEnabled() {
        return _fetchProfileAttributes;
    }
    
    private void setFetchProfileAttributeFlag(boolean flag) {
        _fetchProfileAttributes = flag;
        if (isLogMessageEnabled()) {
            logMessage("WebServiceTaskHandler: profile attribute fetch "
                    + "enabled: " + _fetchProfileAttributes);
        }
    }
    
    private AttributeFetchMode getProfileAttributeFetchMode() {
        return _profileAttributeFetchMode;
    }
    
    private void setPorfileAttributeFetchMode(AttributeFetchMode mode) {
        _profileAttributeFetchMode = mode;
        if (isLogMessageEnabled()) {
            logMessage("WebServiceTaskHandler: profile attribute fetch mode: "
                    + _profileAttributeFetchMode);
        }
    }
    
    private void initFetchSessionAttributeFlag() throws AgentException {
        AttributeFetchMode mode = AttributeFetchMode.get(
                getConfigurationString(CONFIG_SESSION_ATTRIBUTE_FETCH_MODE));
        if (isValidFetchMode(mode)) {
            Map queryMap = getConfigurationMap(CONFIG_SESSION_ATTRIBUTE_MAP);
            if (queryMap != null && queryMap.size() > 0) {
                setFetchSessionAttributeFlag(true);
                setSessionAttributeQueryMap(queryMap);
                setSessionAttributeFetchMode(mode);
            }
        }
    }
    
    private boolean isSessionAttributeFetchEnabled() {
        return _fetchSessionAttributes;
    }
    
    private void setFetchSessionAttributeFlag(boolean flag) {
        _fetchSessionAttributes = flag;
        if (isLogMessageEnabled()) {
            logMessage("WebServiceTaskHandler: session attribute fetch "
                    + "enabled: " + _fetchSessionAttributes);
        }
    }
    
    private Map getSessionAttributeQueryMap() {
        return _sessionAttributeQueryMap;
    }
    
    private Map getResponseAttributeQueryMap() {
        return _responseAttributeQueryMap;
    }
    
    private void setSessionAttributeQueryMap(Map map) {
        _sessionAttributeQueryMap = map;
        if (isLogMessageEnabled()) {
            logMessage("WebServiceTaskHandler: session attribute query map: "
                    + _sessionAttributeQueryMap);
        }
    }
    
    private void setResponseAttributeQueryMap(Map map) {
        _responseAttributeQueryMap = map;
        if (isLogMessageEnabled()) {
            logMessage("WebServiceTaskHandler: response attribute query map: "
                    + _responseAttributeQueryMap);
        }
    }
    
    private AttributeFetchMode getSessionAttributeFetchMode() {
        return _sessionAttributeFetchMode;
    }
    
    private void setSessionAttributeFetchMode(AttributeFetchMode mode) {
        _sessionAttributeFetchMode = mode;
        if (isLogMessageEnabled()) {
            logMessage("WebServiceTaskHandler: session attribute fetch mode: "
                    + _sessionAttributeFetchMode);
        }
    }
    
    private boolean _webServicesEnabled;
    private HashSet _webServiceEndpoints = new HashSet();
    private boolean _processGetRequests;
    private IWebServiceAuthenticator _webServiceAuthenticator;
    private String _internalErrorContent;
    private String _authErrorContent;
    private IAmWebPolicy _amWebPolicy;
    private String _attributeDateFormatString;
    private CommonFactory _commonFactory;
    private boolean _fetchResponseAttributes;
    private AttributeFetchMode _responseAttributeFetchMode
            = AttributeFetchMode.MODE_NONE;
    private boolean _fetchProfileAttributes;
    private AttributeFetchMode _profileAttributeFetchMode
            = AttributeFetchMode.MODE_NONE;
    private Map _profileAttributeQueryMap = new HashMap();
    private IProfileAttributeHelper _profileAttributeHelper;
    private boolean _fetchSessionAttributes;
    private AttributeFetchMode _sessionAttributeFetchMode =
            AttributeFetchMode.MODE_NONE;
    private Map _sessionAttributeQueryMap = new HashMap();
    private Map _responseAttributeQueryMap = new HashMap();
}
