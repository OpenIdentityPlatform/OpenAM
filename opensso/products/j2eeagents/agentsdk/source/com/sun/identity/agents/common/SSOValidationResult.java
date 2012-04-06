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
 * $Id: SSOValidationResult.java,v 1.3 2008/06/25 05:51:41 qcheng Exp $
 *
 */


package com.sun.identity.agents.common;

import java.io.Serializable;

import com.iplanet.sso.SSOToken;
import com.sun.identity.agents.arch.AgentConfiguration;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.util.TransportToken;
import com.sun.identity.agents.util.IUtilConstants;

/**
 * The class represents SSO Token validation result
 */
public final class SSOValidationResult implements Serializable {
    
    SSOValidationResult(boolean isValid,
            SSOValidationResultInitializer initializer) {
        setValid(isValid);
        
        if (initializer != null) {
            setSSOTokenString(initializer.ssoTokenString);
            setClientIPAddress(initializer.clientIP);
            setClientHostName(initializer.clientHost);
            setApplicationName(initializer.applicationName);
            setHeaderValue(initializer.headerValue);
            setUserPrincipal(initializer.userPrincipal);
            setUserId(initializer.userId);
            setSSOToken(initializer.ssoToken);
        }
    }
    
    SSOValidationResult() {
        this(false, null);
    }
    
    public String getSSOTokenString() {
        return _ssoTokenString;
    }
    
    public String getClientIPAddress() {
        return _clientIPAddress;
    }
    
    public String getClientHostName() {
        return _clientHostName;
    }
    
    public boolean isValid() {
        return _isValid;
    }
    
    public String getApplicationName() {
        return _applicationName;
    }
    
    public String getHeaderValue() {
        return _headerValue;
    }
     
    public String getUserPrincipal() {
        String result = _userPrincipal;
        if (result == null) {
            result = IUtilConstants.ANONYMOUS_USER_NAME;
        }
        return result;
    }
    
    public String getUserId() {
        String result = _userId;
        if (result == null) {
            result = IUtilConstants.ANONYMOUS_USER_NAME;
        }
        return result;
    }
    
    public SSOToken getSSOToken() {
        return _ssoToken;
    }
    
    public String getTransportString() throws AgentException {
        if (_transportString == null) {
            _transportString = constructTransportString();
        }
        
        return _transportString;
    }
    
    public String getEncryptedTransportString() throws AgentException {
        if (_encryptedTransportString == null) {
            _encryptedTransportString =
                    constructEncryptedTransportString();
        }
        
        return _encryptedTransportString;
    }
    
    void setTransportString(String transportString) {
        _transportString = transportString;
    }
    
    public TransportToken getTransportToken() throws AgentException {
        if (_transportToken == null) {
            _transportToken = constructTransportToken();
        }
        
        return _transportToken;
    }
    
    private TransportToken constructTransportToken() throws AgentException {
        TransportToken result = new TransportToken(getSSOTokenString(),
                getClientIPAddress());
        
        result.setAttribute(ISSOTokenValidator.ATTRIBUTE_APPLICATION_NAME,
                getApplicationName());
        
        result.setAttribute(ISSOTokenValidator.ATTRIBUTE_CLIENT_HOST,
                getClientHostName());
        
        if (getHeaderValue() != null
                && getHeaderValue().trim().length() > 0) {
            result.setAttribute(ISSOTokenValidator.ATTRIBUTE_HEADER_VALUE,
                    getHeaderValue());
        }
        
        return result;
    }
    
    private String constructTransportString() throws AgentException {
        return getTransportToken().getTransportString();
    }
    
    private String constructEncryptedTransportString() throws AgentException {
        return getTransportToken().getEncryptedTransportString();
    }
    
    private void setSSOToken(SSOToken ssoToken) {
        _ssoToken = ssoToken;
    }
    
    private void setUserId(String userId) {
        _userId = userId;
    }
    
    private void setUserPrincipal(String userDN) {
        _userPrincipal = userDN;
    }
    
    private void setHeaderValue(String headerValue) {
        _headerValue = headerValue;
    }
    
    private void setApplicationName(String applicationName) {
        _applicationName = applicationName;
    }
    
    private void setValid(boolean isValid) {
        _isValid = isValid;
    }
    
    private void setClientIPAddress(String clientIP) {
        _clientIPAddress = clientIP;
    }
    
    private void setClientHostName(String clientHostName) {
        _clientHostName = clientHostName;
    }
    
    private void setSSOTokenString(String ssoTokenString) {
        _ssoTokenString = ssoTokenString;
    }
    
    private boolean _isValid;
    private String _ssoTokenString;
    private String _clientIPAddress;
    private String _clientHostName;
    private String _applicationName;
    private String _headerValue;
    private String _userPrincipal;
    private String _userId;
    private SSOToken _ssoToken;
    private String _transportString;
    private String _encryptedTransportString;
    private TransportToken _transportToken;
    
    public static final SSOValidationResult FAILED =
            new SSOValidationResult();
}

