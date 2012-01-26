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
 * $Id: SSOTokenValidator.java,v 1.4 2008/07/15 21:21:20 leiming Exp $
 *
 */


package com.sun.identity.agents.common;

import java.net.URLDecoder;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.agents.arch.AgentConfiguration;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Module;
import com.sun.identity.agents.arch.SurrogateBase;
import com.sun.identity.agents.arch.UserMappingMode;
import com.sun.identity.agents.util.StringUtils;
import com.sun.identity.agents.util.TransportToken;

/**
 * The class validates Single Sign-on Token
 */
public class SSOTokenValidator extends SurrogateBase
        implements ISSOTokenValidator {
    
    public SSOTokenValidator(Module module) {
        super(module);
    }
    
    public void initialize()
    throws AgentException {
        try {
            setSSOTokenManager(SSOTokenManager.getInstance());
            setClientIPAddressHeader(
                    AgentConfiguration.getClientIPAddressHeader());
            setClientHostNameHeader(
                    AgentConfiguration.getClientHostNameHeader());
            
            if (getUserMappingMode().equals(
                    UserMappingMode.MODE_PROFILE_ATTRIBUTE)) {
                CommonFactory cf = new CommonFactory(getModule());
                setProfileAttributeHelper(
                        cf.newProfileAttributeHelper());
            }
        } catch (Exception ex) {
            throw new AgentException("SSOTokenValidator init failed", ex);
        }
        
        if (isLogMessageEnabled()) {
            logMessage("SSOTokenValidator: initialized");
        }
    }
    
    /**
     * Validates the given transport string and returns a SSOValidationResult
     * instance indicating the outcome of the validation process.
     *
     * @param transportString that carries all the necessary information such
     * as SSO Token String, remote client address etc.
     *
     * @return a SSOValidationResult object indicating the outcome of the
     * validation process.
     *
     * @throws AgentException in case of an unexpected error condition that
     * hinders the evaluation of the given transport string.
     */
    public SSOValidationResult validate(String transportString)
    throws AgentException {
        SSOValidationResult result = SSOValidationResult.FAILED;
        
        try {
            TransportToken transportToken =
                    new TransportToken(transportString);
            String ssoTokenID = transportToken.getSSOTokenID();
            String clientIP = transportToken.getIPAddress();
            String applicationName =
                    transportToken.getAttribute(ATTRIBUTE_APPLICATION_NAME);
            String headerValue =
                    transportToken.getAttribute(ATTRIBUTE_HEADER_VALUE);
            String clientHost =
                    transportToken.getAttribute(ATTRIBUTE_CLIENT_HOST);
            
            result = validateInternal(ssoTokenID, clientIP, clientHost,
                    applicationName, headerValue);
            
            if (result.isValid()) {
                result.setTransportString(transportString);
            }
        } catch (Exception ex) {
            if (isLogMessageEnabled()) {
                logMessage("SSOTokenValidator: validate failed with exception",
                        ex);
            } else if (isLogWarningEnabled()) {
                logWarning("SSOTokenValidator failed with exception: "
                        + ex.getMessage());
            }
        }
        
        return result;
    }
    
    public SSOValidationResult validate(HttpServletRequest request) {
        String ssoTokenID = getSSOTokenValue(request);
        String clientIP = getClientIPAddress(request);
        String clientHost = getClientHostName(request);
        String appName = getAppName(request);
        String headerValue = request.getHeader(getUserAttributeName());
        
        return validateInternal(ssoTokenID, clientIP, clientHost,
                appName, headerValue);
    }
    
    public SSOValidationResult validate(String ssoTokenID,
            HttpServletRequest request) {
        String clientIP = getClientIPAddress(request);
        String clientHost = getClientHostName(request);
        String appName = getAppName(request);
        String headerValue = request.getHeader(getUserAttributeName());
        
        return validateInternal(ssoTokenID, clientIP, clientHost,
                appName, headerValue);
        
    }
    
    public String getSSOTokenValue(HttpServletRequest request) {
        String rawValue = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (int i=0; i<cookies.length; i++) {
                if (cookies[i].getName().equals(getSSOTokenName())) {
                    rawValue = cookies[i].getValue();
                }
            }
        }
        
        if (rawValue == null) {
            rawValue = StringUtils.getQueryParameter(request.getQueryString(),
                    getSSOTokenName());
            if (rawValue != null) {
                logMessage("SSOTokenValidator: SSO Token read from "
                        + "query string: " + rawValue);
            }
        }
        
        String result = rawValue;
        if (rawValue != null) {
            if (needURLDecode(rawValue)) {
                result = URLDecoder.decode(rawValue);
            }
        }
        return result;
    }
    
    public String getClientIPAddress(HttpServletRequest request) {
        String result = null;
        if (getClientIPAddressHeader() != null) {
            result = request.getHeader(getClientIPAddressHeader());
        } else {
            result = request.getRemoteAddr();
        }
        return result;
    }
    
    public String getClientHostName(HttpServletRequest request) {
        String result = null;
        if (getClientHostNameHeader() != null) {
            result = request.getHeader(getClientHostNameHeader());
        } else {
            result = request.getRemoteHost();
        }
        return result;
    }
    
    /**
     * Method declaration
     *
     *
     * @param ssoTokenID
     * @param ipAddress
     * @param applicationName
     * @param headerValue
     *
     * @return
     *
     * @see
     */
    private SSOValidationResult validateInternal(String ssoTokenID,
            String clientIP, String clientHost, String applicationName,
            String headerValue) {
        SSOValidationResult result = SSOValidationResult.FAILED;
        
        if (getUserMappingMode().equals(UserMappingMode.MODE_HTTP_HEADER)
        && (headerValue == null
                || headerValue.trim().length() == 0)) {
            if (isLogWarningEnabled()) {
                logWarning("SSOTokenValidator: "
                        + "User identification header not found");
            }
        } else {
            try {
                if (ssoTokenID != null && ssoTokenID.length() > 0) {
                    SSOToken ssoToken =
                            getSSOTokenManager().createSSOToken(ssoTokenID,
                            clientIP);
                    boolean  isValid =
                            getSSOTokenManager().isValidToken(ssoToken);
                    
                    if (isValid) {
                        String userPrincipal =
                                ssoToken.getPrincipal().getName();
                        String userId = null;
                        
                        switch (getUserMappingMode().getIntValue()) {
                            case UserMappingMode.INT_MODE_USER_ID:
                                if (isUserPrincipalEnabled()) {
                                    userId = userPrincipal;
                                } else {
                                    userId = ssoToken.getProperty(
                                            AgentConfiguration.
                                            getUserIdPropertyName());
                                }
                                break;
                                
                            case UserMappingMode.INT_MODE_PROFILE_ATTRIBUTE:
                                userId =
                                    getProfileAttributeHelper().getAttribute(
                                    ssoToken, getUserAttributeName());
                                break;
                                
                            case UserMappingMode.INT_MODE_HTTP_HEADER:
                                userId = headerValue;
                                break;
                            case UserMappingMode.INT_MODE_SESSION_PROPERTY:
                                userId = ssoToken.getProperty(
                                        getUserAttributeName());
                                break;
                        }
                        
                        if (userId == null || userId.trim().length() == 0) {
                            throw new AgentException(
                                    "Failed to determine user ID");
                        }
                        
                        SSOValidationResultInitializer initializer =
                                new SSOValidationResultInitializer();
                        
                        initializer.applicationName = applicationName;
                        initializer.headerValue = headerValue;
                        initializer.clientIP = clientIP;
                        initializer.clientHost = clientHost;
                        initializer.ssoToken = ssoToken;
                        initializer.ssoTokenString = ssoTokenID;
                        initializer.userPrincipal = userPrincipal;
                        initializer.userId = userId;
                        result = new SSOValidationResult(true, initializer);
                    }
                }
            } catch (Exception ex) {
                if (isLogMessageEnabled()) {
                    logMessage("SSOTokenValidator.validate(): Exception caught",
                            ex);
                } else if (isLogWarningEnabled()) {
                    logWarning(
                            "SSOTokenValidator.validate(): Exception caught: "
                            + ex.getMessage());
                }
            }
        }
        
        return result;
    }
    
    public String getAppName(HttpServletRequest request) {
        String result = AgentConfiguration.DEFAULT_WEB_APPLICATION_NAME;
        String ctxPath = request.getContextPath();
        if (ctxPath.trim().length() > 0) {
            result = ctxPath.substring(1);
        }
        
        return result;
    }
    
    
    /**
     * Method getSSOTokenManager
     *
     *
     * @return
     *
     */
    private SSOTokenManager getSSOTokenManager() {
        return _ssoTokenMgr;
    }
    
    /**
     * Method setSSOTokenManager
     *
     *
     * @param mgr
     *
     */
    private void setSSOTokenManager(SSOTokenManager mgr) {
        _ssoTokenMgr = mgr;
    }
    
    /**
     * Method useDN
     *
     *
     * @return
     *
     */
    private boolean isUserPrincipalEnabled() {
        return AgentConfiguration.isUserPrincipalEnabled();
    }
    
    /**
     * Method getUserMappingMode
     *
     * @return
     */
    private UserMappingMode getUserMappingMode() {
        return AgentConfiguration.getUserMappingMode();
    }
    
    /**
     * Method getUserAttributeName
     *
     * @return
     */
    private String getUserAttributeName() {
        return AgentConfiguration.getUserAttributeName();
    }
    
    /**
     * Method declaration
     *
     *
     * @param helper
     *
     * @see
     */
    private void setProfileAttributeHelper(IProfileAttributeHelper helper) {
        _profileAttributeHelper = helper;
    }
    
    /**
     * Method declaration
     *
     *
     * @return
     *
     * @see
     */
    private IProfileAttributeHelper getProfileAttributeHelper() {
        return _profileAttributeHelper;
    }
    
    private String getSSOTokenName() {
        return AgentConfiguration.getSSOTokenName();
    }
    
    private boolean needURLDecode(String rawValue) {
        return rawValue.indexOf("%") >= 0;
    }
    
    private void setClientIPAddressHeader(String header) {
        if (header != null && header.trim().length() > 0) {
            _clientIPAddressHeader = header;
        }
        if (isLogMessageEnabled()) {
            if (_clientIPAddressHeader != null) {
                logMessage("SSOTokenValidator: Using header: "
                        + _clientIPAddressHeader
                        + " for client IP address lookup.");
            } else {
                logMessage("SSOTokenValidator: Using request for "
                        + "client IP address lookup.");
            }
        }
    }
    
    private String getClientIPAddressHeader() {
        return _clientIPAddressHeader;
    }
    
    private void setClientHostNameHeader(String header) {
        if (header != null && header.trim().length() > 0) {
            _clientHostNameHeader = header;
        }
        if (isLogMessageEnabled()) {
            if (_clientHostNameHeader != null) {
                logMessage("SSOTokenValidator: Using header: "
                        + _clientHostNameHeader
                        + " for client hostname lookup.");
            } else {
                logMessage("SSOTokenValidator: Using request for "
                        + "client hostname lookup.");
            }
        }
    }
    
    private String getClientHostNameHeader() {
        return _clientHostNameHeader;
    }
    
    private SSOTokenManager _ssoTokenMgr = null;
    private IProfileAttributeHelper _profileAttributeHelper = null;
    private String _clientIPAddressHeader;
    private String _clientHostNameHeader;
}
