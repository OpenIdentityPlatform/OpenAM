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
 * $Id: WSSUsernameTokenAuthenticator.java,v 1.5 2008/10/07 17:32:31 huacui Exp $
 *
 */
/**
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.agents.filter;

import java.io.StringReader;
import java.util.Hashtable;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.SAXParser;

import org.xml.sax.InputSource;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenEvent;
import com.iplanet.sso.SSOTokenListener;
import com.sun.identity.agents.arch.AgentConfiguration;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.ISystemAccess;
import com.sun.identity.agents.common.CommonFactory;
import com.sun.identity.agents.common.ISSOTokenValidator;
import com.sun.identity.agents.common.SSOValidationResult;
import com.sun.identity.agents.util.TransportToken;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.shared.xml.XMLUtils;
/**
 * @author 
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class WSSUsernameTokenAuthenticator implements IWebServiceAuthenticator {
    
    public WSSUsernameTokenAuthenticator() throws AgentException {        
        ISystemAccess systemAccess = AmFilterManager.getSystemAccess();
        setSystemAccess(systemAccess);
        
        CommonFactory cf = new CommonFactory(systemAccess.getModule());
        setSSOTokenValidator(cf.newSSOTokenValidator());
    }

    public SSOToken getUserToken(HttpServletRequest request, 
            String requestMessage, String remoteAddress, String remoteHost,
            AmFilterRequestContext ctx) 
    {
        SSOToken result = null;
        
        try {            
            WSSUserNameTokenContentHandler handler = 
                new WSSUserNameTokenContentHandler();
            SAXParser parser = XMLUtils.getSafeSAXParser(false);
            StringReader reader = new StringReader(requestMessage);
            InputSource inputSource = new InputSource(reader);            
            parser.parse(inputSource, handler);
            
            String userName = handler.getUsername();
            String password = handler.getPassword();
            
            if (getSystemAccess().isLogMessageEnabled()) {
                getSystemAccess().logMessage("WSSUsernameTokenAuthenticator: " 
                                + "user: " + userName);
            }
            
            String key = userName + ":" + password;
            
            result = getSSOTokenFromCache(key);
            
            if (result == null) {
                if (getSystemAccess().isLogMessageEnabled()) {
                    getSystemAccess().logMessage(
                       "WSSUsernameTokenAuthenticator: Attempting to "
                            + "authenticate");
                }
                
                AuthContext authContext = new AuthContext(
                        AgentConfiguration.getOrganizationName());
                
                authContext.login();
                if(authContext.hasMoreRequirements()) {
                    Callback[] callbacks = authContext.getRequirements();
                    if(callbacks != null) {
                        addLoginCallbackMessage(callbacks, 
                                userName, password);
                        authContext.submitRequirements(callbacks);
                    }
                }
                if(authContext.getStatus() == AuthContext.Status.SUCCESS) {
                    result = authContext.getSSOToken();
                    cacheSSOToken(key, result, request, remoteAddress);
                }

                if (getSystemAccess().isLogMessageEnabled()) {
                    getSystemAccess().logMessage(
                            "WSSUsernameTokenAuthenticator: AC-Status: "
                            + authContext.getStatus() 
                            + ", sso token: " + result);
                }                
            } else {
                if (getSystemAccess().isLogMessageEnabled()) {
                    getSystemAccess().logMessage(
                         "WSSUsernameTokenAuthenticator: SSO found from cache");
                }
            }
            
        } catch (Exception ex) {
            getSystemAccess().logError("WSSUsernameTokenAuthenticator: "
                            + "Auth failed with exception", ex);
            result = null;
        }

        return result;
    }  
    
    
    private void addLoginCallbackMessage(
            Callback[] callbacks, String appUserName, String password)
                throws UnsupportedCallbackException {

        for(int i = 0; i < callbacks.length; i++) {
            if(callbacks[i] instanceof NameCallback) {
                NameCallback nameCallback = (NameCallback) callbacks[i];

                nameCallback.setName(appUserName);
            } else if(callbacks[i] instanceof PasswordCallback) {
                PasswordCallback pwdCallback =
                    (PasswordCallback) callbacks[i];

                pwdCallback.setPassword(password.toCharArray());
            }
        }
    }
    
    private void cacheSSOToken(String key, SSOToken token, 
            HttpServletRequest request, String remoteAddress) 
    throws Exception 
    {
        try {
            token.addSSOTokenListener(new AmSSOTokenListener(key));
            SSOValidationResult ssoValidationResult = 
                getSSOTokenValidator().validate(
                        token.getTokenID().toString(), request);
            TransportToken transportToken = 
                ssoValidationResult.getTransportToken();
            
            synchronized(LOCK) {
                getSSOCache().put(key, transportToken.getTransportString());

                if(getSystemAccess().isLogMessageEnabled()) {
                    getSystemAccess().logMessage(
                            "WSSUsernameTokenAuthenticator: cache size = "
                               + getSSOCache().size());
                }
            }
        } catch(SSOException ex) {
            getSystemAccess().logError(
                    "WSSUsernameTokenAuthenticator: Failed to cache entry", ex);
        }
    }
    
    private SSOToken getSSOTokenFromCache(String key)
    {
        SSOToken result = null;
        String transportTokenString = null;         
        synchronized (LOCK) {
            transportTokenString = (String) getSSOCache().get(key);
        }
        
        if (transportTokenString != null) {
            try {
                SSOValidationResult ssoValidationResult =
                            getSSOTokenValidator().validate(transportTokenString);

                if(ssoValidationResult.isValid()) {
                    result = ssoValidationResult.getSSOToken();
                } else {
                    removeCacheEntry(key);
                }
            } catch(Exception ex) {
                removeCacheEntry(key);
            }
        }
        
        return result;
    }
    
    private void removeCacheEntry(String key) {
        synchronized(LOCK) {
            getSSOCache().remove(key);
            if (getSystemAccess().isLogMessageEnabled()) {
                getSystemAccess().logMessage("WSSUsernameTokenAuthenticator: "
                                + "removed cached entry, cache size=" 
                                + getSSOCache().size());

            }
        }
    }    
    
    private ISystemAccess getSystemAccess() {
        return _systemAccess;
    }
    
    private void setSystemAccess(ISystemAccess systemAccess) {
        _systemAccess = systemAccess;
    }
    
    private ISSOTokenValidator getSSOTokenValidator() {
        return _ssoTokenValidator;
    }
    
    private void setSSOTokenValidator(ISSOTokenValidator validator) {
        _ssoTokenValidator = validator;
    }
        
    private static Hashtable getSSOCache() {
        return _ssoCache;
    }

    private ISystemAccess _systemAccess;
    private ISSOTokenValidator _ssoTokenValidator;
    
    private static Hashtable _ssoCache = new Hashtable();   
    private static final String LOCK      = "amWSCache_Lock";
    
    class AmSSOTokenListener implements SSOTokenListener {

        AmSSOTokenListener(String key) {
            setKey(key);
        }

        /**
         * Method ssoTokenChanged
         *
         *
         * @param ssoTokenEvent
         *
         */
        public void ssoTokenChanged(SSOTokenEvent ssoTokenEvent) {

            removeCacheEntry(getKey());

            if(getSystemAccess().isLogMessageEnabled()) {
                try {
                    int    type    = ssoTokenEvent.getType();
                    String typeStr = null;

                    switch(type) {

                    case SSOTokenEvent.SSO_TOKEN_DESTROY :
                        typeStr = "SSO_TOKEN_DESTROY";
                        break;

                    case SSOTokenEvent.SSO_TOKEN_IDLE_TIMEOUT :
                        typeStr = "SSO_TOKEN_IDLE_TIMEOUT";
                        break;

                    case SSOTokenEvent.SSO_TOKEN_MAX_TIMEOUT :
                        typeStr = "SSO_TOKEN_MAX_TIMEOUT";
                        break;

                    default :
                        typeStr = "UNKNOWN TYPE EVENT = " + type;
                        break;
                    }

                } catch(SSOException ssoEx) {
                    if(getSystemAccess().isLogWarningEnabled()) {
                        getSystemAccess().logWarning(
                                "WSSUsernameTokenAuthenticator.Listener: "
                                + "Exception caught", ssoEx);
                    }
                }
            }
        }

        private void setKey(String key) {
            _key = key;
        }

        private String getKey() {
            return _key;
        }

        private String _key;
    }
}
