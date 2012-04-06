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
 * $Id: ApplicationLogoutHandler.java,v 1.13 2009/10/28 18:50:15 leiming Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.agents.filter;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Manager;

/**
 * <p>
 * This Task handler is the Default HTTP Request Pre-processing handler for an
 * incoming request.
 * </p>
 */
public class ApplicationLogoutHandler extends AmFilterTaskHandler
implements IApplicationLogoutHandler {

    private LogoutHelper helper;

    public ApplicationLogoutHandler(Manager manager){
        super(manager);
    }

    public void initialize(ISSOContext context, AmFilterMode mode)
    throws AgentException {
        super.initialize(context, mode);
        setIntroSpectRequestAllowedFlag(getConfigurationBoolean(
                CONFIG_LOGOUT_INTROSPECT_ENABLE,
                DEFAULT_LOGOUT_INTROSPECT_ENABLE));
        setIsActiveFlag();
        helper = new LogoutHelper(this);
    }

    /**
     * Checks to see if the incoming request is for a logout event and take the
     * necessary steps if a logout event is detected.
     *
     * @param ctx
     *            the <code>AmFilterRequestContext</code> that carries
     *            information about the incoming request and response objects.
     *
     * @return <code>AmFilterResult</code> if the processing of this task
     *         resulted in a particular action to be taken for the incoming
     *         request. The return could be <code>null</code> if no action is
     *         necessary for this request.
     *
     * @throws AgentException
     *             if the processing resulted in an unrecoverable error
     *             condition
     */
    public AmFilterResult process(AmFilterRequestContext ctx)
            throws AgentException {

        AmFilterResult result = null;

        if (detectNeedForLogout(ctx.getHttpServletRequest())) {
            if (isLogMessageEnabled()) {
                logMessage(
                        "ApplicationLogoutHandler: Detected need to logout.");
            }

            helper.doLogout(ctx);

            String logoutURL = getLogoutURL(ctx);
            result = new AmFilterResult(
                    AmFilterResultStatus.STATUS_REDIRECT,
                    logoutURL);
        }

        return result;
    }

    private String getApplicationEntryURL(AmFilterRequestContext ctx) {
        HttpServletRequest request = ctx.getHttpServletRequest();
        String entryURI = getApplicationEntryURI(request);
        if (entryURI == null || entryURI.trim().length() == 0) {
            entryURI = "/";
        }
        return ctx.getBaseURL() + entryURI;
    }

    private String getApplicationEntryURI(HttpServletRequest request) {
        String appName = getApplicationName(request);
        String result = getApplicationConfigurationString(request,
                    CONFIG_LOGOUT_ENTRY_URI_MAP, appName);
        if (result == null) {
            if (isLogMessageEnabled()) {
                logMessage("ApplicationLogoutHandler: no entry URI "
                        + "specified for app: " + appName
                        + ". Using appcontext URI");
            }
            result = request.getContextPath();
        }
        return result;
    }

    /**
     * Detect the need for logout event
     */
    private boolean detectNeedForLogout(HttpServletRequest request)
            throws AgentException {
        String appName = null;
        boolean result = false;

        try {
            appName = getApplicationName(request);

            // Check for logout URI match
            result = matchLogoutURI(request, appName);

            // Check for logout param in the request body and query string
            if (!result) {
                result = searchForLogoutParam(request, appName);
            }
        } catch (Exception ex) {
            throw new AgentException(
                    "ApplicationLogoutHandler.process() failed "
                        + " to process incoming request with exception", ex);
        }

        if (isLogMessageEnabled()) {
            logMessage("ApplicationLogoutHandler : Need to logout = "
                    + result);
        }

        return result;
    }

    /**
     * Returns a boolean value indicating if a match for logout parameter was
     * found
     */
    private boolean searchForLogoutParam(HttpServletRequest request,
            String appName) {
        boolean result = false;
        String logoutParam = getManager().getApplicationConfigurationString(
                CONFIG_LOGOUT_REQUEST_PARAM_MAP, appName);

        if ((logoutParam != null) && (logoutParam.length() > 0)) {

            // First look to see if introspection in the request is allowed
            // If allowed, first look into HTTP request to short circuit the
            // rest
            if (getIntroSpectRequestAllowedFlag()) {
                if (isLogMessageEnabled()) {
                    logMessage("ApplicationLogoutHandler : Looking for "
                            + "request parameter =" + logoutParam
                            + " in request body.");
                }

                String requestParam = request.getParameter(logoutParam);
                if ((requestParam != null) && (requestParam.length() > 0)) {

                    result = true;
                    if (isLogMessageEnabled()) {
                        logMessage("ApplicationLogoutHandler : App Name = "
                                + appName + "has a match for logout Param  ="
                                + requestParam + " as a request parameter."
                                + "Need to logout =" + result);
                    }
                } else {
                    if (isLogMessageEnabled()) {
                        logMessage(
                                "ApplicationLogoutHandler : Request Parameter ="
                                + requestParam
                                + " not found in HTTP request body.");
                    }
                }
            } else { // look into query string
                String queryString = request.getQueryString();
                result = getLogoutParamMatchResult(logoutParam, queryString,
                        appName);

            }
        }

        return result;
    }

    /**
     * Returns a boolean value if logout param is in query string, try the two
     * variations ?param_name=, &param_name=
     */
    private boolean getLogoutParamMatchResult(String logoutParam,
            String queryString, String appName) {
        String firstVariant = logoutParam + "=";
        String secondVariant = "&" + logoutParam + "=";
        boolean result = false;

        if ((queryString != null) && (queryString.length() > 0)) {
            if (queryString.startsWith(firstVariant)
                    || (queryString.indexOf(secondVariant) > 0)) {
                result = true;
                if (isLogMessageEnabled()) {
                    logMessage("ApplicationLogoutHandler : App Name = "
                            + appName + "has a match for logout Param  ="
                            + logoutParam + " in the Request query string ."
                            + "Need to logout =" + result);
                }
            }
        }

        return result;

    }

    /**
     * Returns a boolean value indicating if a match for logout URI was found
     */
    private boolean matchLogoutURI(HttpServletRequest request, String appName) {
        boolean result = false;

        if ((appName != null) && (appName.length() > 0)) {
            String logoutURI = getApplicationConfigurationString(request,
                    CONFIG_LOGOUT_URI_MAP, appName);            
            if ((logoutURI != null) && (logoutURI.length() > 0)) {
                if (request.getRequestURI().equals(logoutURI)) {
                    result = true;

                    if (isLogMessageEnabled()) {
                        logMessage("ApplicationLogoutHandler : App Name = "
                                + appName + "has a match for logout URI ="
                                + logoutURI + " with the request URI."
                                + "Need to logout =" + result);
                    }
                } else {
                    if (isLogMessageEnabled()) {
                        logMessage("ApplicationLogoutHandler : Request URI = "
                                + request.getRequestURI()
                                + " did not match with logout URI = "
                                + logoutURI + " specified in configuration.");
                    }
                }
            }
        }

        return result;
    }

    /**
     * get the property id's value based on possible second context as the key.
     * The key of second context is in the form of appName/path1 from requested 
     * URI /appName/path1/path2/...
     */
    private String getApplicationConfigurationString(HttpServletRequest request,
            String id, String appName) {

        String requestURI = request.getRequestURI();
        int index1 = requestURI.indexOf("/", 1);
        int index2 = -1;
        if (index1 > 0) {
            index2 = requestURI.indexOf("/", index1 + 1);
        }
        String secondContextKey = null;
        if (index2 > 0) {
            secondContextKey = requestURI.substring(1, index2);
        }

        // return if the key is null.
        if (secondContextKey == null || secondContextKey.length() == 0) {
            return getManager().getApplicationConfigurationString(
                    id, appName);
        }

        // return to use appName as the key if second context value is null.
        String secondContextValue = 
                getManager().getApplicationConfigurationString(
                    id, secondContextKey);
        if (secondContextValue == null || secondContextValue.length() == 0) {
            return getManager().getApplicationConfigurationString(
                    id, appName);
        }

        // get default or global value for this property.
        String defaultValue = getManager().getConfigurationString(id);
        if (defaultValue == null || defaultValue.length() == 0) {
            return secondContextValue;
        }

        if (secondContextValue.equals(defaultValue)) {
            return getManager().getApplicationConfigurationString(
                    id, appName);
        } else {
            return secondContextValue;
        }
    }
    
    /**
     * Caches the isActive flag
     */
    private void setIsActiveFlag() {
        Map logoutUrlMap = getManager().getConfigurationMap(
                CONFIG_LOGOUT_URI_MAP);
        String globalLogoutURI = getManager().getConfigurationString(
                CONFIG_LOGOUT_URI_MAP);
        Map requestParamMap = getManager().getConfigurationMap(
                CONFIG_LOGOUT_REQUEST_PARAM_MAP);
        String globalRequestParam = getManager().getConfigurationString(
                CONFIG_LOGOUT_REQUEST_PARAM_MAP);

        if ((logoutUrlMap != null && logoutUrlMap.size() > 0) ||
            (globalLogoutURI != null && globalLogoutURI.trim().length() > 0) ||
            (requestParamMap != null && requestParamMap.size() > 0) ||
            (globalRequestParam != null && globalRequestParam.trim().length() >
0)) {
            _isActiveFlag = true;
        }
    }

    /**
     * Returns the cached the isActive flag
     *
     * @return
     */
    private boolean getIsActiveFlag() {
        return _isActiveFlag;
    }

    /**
     * Detects if the handler is active or not
     *
     * @return boolean true if active, false if inactive
     */
    public boolean isActive() {
        return (!isModeNone()) && getIsActiveFlag();
    }

    /**
     * Returns a String that can be used to identify this task handler
     *
     * @return the name of this task handler
     */
    public String getHandlerName() {
        return AM_APP_LOGOUT_HANDLER_NAME;
    }

    /**
     * Returns the logout URL.
     */
    private String getLogoutURL(AmFilterRequestContext ctx)
    throws AgentException {
        String result = null;
        String logoutURL = ctx.getLogoutURL(getApplicationEntryURL(ctx));
        if (logoutURL != null) {
            // use the available OpenSSO server logout URL
            return logoutURL;
        }

        // OpenSSO server logout URL is not available, try to construct it 
        // based on its login URL
        String loginURL = ctx.getAuthRedirectURL(getApplicationEntryURL(ctx));
        StringBuffer buff = null;
        String loginStr = "Login";
        String logoutStr = "Logout";

        // Replace "Login" by "Logout" in the redirection url
        // to the entry_uri page
        int iEnd = loginURL.indexOf("?");
        if (iEnd > -1) {
            int iStart = iEnd-loginStr.length();
            if (loginStr.compareToIgnoreCase(loginURL.substring(iStart,iEnd)) == 0) {
                buff= new StringBuffer(loginURL.substring(0,iStart));
                buff.append(logoutStr).append(loginURL.substring(iEnd));
                result = buff.toString();
            }
        }
        if (result == null) {
            if (isLogWarningEnabled()) {
                logWarning("ApplicationLogoutHandler: Could not get logout url. "
                + "Redirecting to login page");
            }
            buff = new StringBuffer(loginURL);
            buff.append('&').append(ARG_NEW_SESSION_PARAMETER);
            result = buff.toString();
        }
        if (isLogMessageEnabled()) {
            logMessage("ApplicationLogoutHandler: Logout URL is: " + result);
        }

        return result;
    }

    private boolean getIntroSpectRequestAllowedFlag() {
        return _introSpectRequestAllow;
    }

    private void setIntroSpectRequestAllowedFlag(boolean allowed) {
        _introSpectRequestAllow = allowed;
        if (isLogMessageEnabled()) {
            logMessage("ApplicationLogoutHandler: request introspect: "
                    + allowed);
        }
    }

    private boolean _isActiveFlag;
    private boolean _introSpectRequestAllow;
}

