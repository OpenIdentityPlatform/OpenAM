/*
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
 * $Id: FMSessionProvider.java,v 1.23 2009/11/20 00:30:40 exu Exp $
 *
 * Portions Copyrighted 2010-2016 ForgeRock AS.
 */

package com.sun.identity.plugin.session.impl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
import java.util.Map;
import java.util.HashSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.ResourceBundle;
import java.security.SecureRandom;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;

import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.plugin.session.SessionListener;
import com.sun.identity.plugin.session.SessionException;

import com.sun.identity.shared.Constants;

import com.iplanet.sso.SSOTokenManager;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOTokenEvent;
import com.iplanet.sso.SSOTokenListener;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.shared.encode.CookieUtils;
import com.sun.identity.shared.debug.Debug;

import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.StringUtils;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.service.AMAuthErrorCode;
import com.sun.identity.authentication.service.AuthUtils;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.SMSException;

/**
 * Used for creating sessions, and for accessing session
 * information.
 */
public class FMSessionProvider implements SessionProvider {

    /**
     * This constant string is used both in this class and
     * in the Federation Auth Module implementationt to specify
     * the name of the random secret call back.
     */
    public static final String RANDOM_SECRET = "randomSecret";

    private static final String AUTH_TYPE = "AuthType";
    private static final String PROPERTY_VALUES_SEPARATOR = "|";
    private static ResourceBundle bundle =
        Locale.getInstallResourceBundle("fmSessionProvider");
    private static Debug debug = Debug.getInstance("libPlugins");;
    private static String cookieName = SystemPropertiesManager.
        get(Constants.AM_COOKIE_NAME);
/**
 *  Commented out since initializing session service statically here brings
 *  in the whole OpenAM which is not desirable for clientsdk.
 *  Can remove this, issue handler probably could do that.
    private static String lbcookieName = Session.lbCookieName;
    private static String lbcookieValue = SessionService.getSessionService().
        getLocalServerID();
*/
    private static String lbcookieName =  null;
    private static String lbcookieValue = null;
    private static boolean urlRewriteEnabled = false;
    private static SecureRandom random = new SecureRandom();
    private static final int SECRET_LENGTH = 20;
    private static Set secretSet = Collections.synchronizedSet(
        new HashSet(1000));
    
    static {
        String urlRewriteEnabledStr = SystemPropertiesManager.
            get(Constants.REWRITE_AS_PATH);
        if (urlRewriteEnabledStr != null &&
            urlRewriteEnabledStr.trim().length() != 0 &&
            urlRewriteEnabledStr.trim().toLowerCase().equals("true")) {
            urlRewriteEnabled = true;
        }
    }    
        
    /**
     * Default Constructor
     */
    public FMSessionProvider() {
    }

    /**
     * Indicates whether a secret originally comes from this class or not
     * @param secret the secret string to be matched
     * @return true if there is a match, false otherwise
     */
    public static boolean matchSecret(String secret) {
        return secretSet.remove(secret);
    }

    private static String generateSecret() {
        byte bytes[] = new byte[SECRET_LENGTH];
        random.nextBytes(bytes);
        return new String(bytes);
    }
    
    /** 
     * Meaningful only for SP side, the implementation of this method
     * will create a local session for the local user identified by
     * the information in the map. The underline mechanism of the
     * session creation and management is application specific.
     * For example, it could be cookie setting or url rewriting, which 
     * is expected to be done by the implementation of this method.
     * Note that only the first input parameter is mandatory. Normally,
     * at least one of the last two parameters should not be null
     * 
     * @param info a Map with keys and values being of type String; The
     *             keys will include "principalName" (returned from
     *             SPAccountMapper), "realm", "authLevel", and may
     *             include "resourceOffering" and/or "idpEntityID";
     *             The implementation of this method could choose to set
     *             some of the information contained in the map into the
     *             newly created Session by calling setProperty(), later
     *             the target application may consume the information. 
     * @param request the HttpServletRequest the user made to initiate
     *                the SSO.
     * @param response the HttpServletResponse that will be sent to the
     *                 user (for example it could be used to set a cookie).
     * @param targetApplication the original resource that was requested
     *                          as the target of the SSO by the end user;
     *                          If needed, this String could be modified,
     *                          e.g., by appending query string(s) or by
     *                          url rewriting, hence this is an in/out
     *                          parameter.
     * @return the newly created local user session.
     * @throws SessionException if an error occurred during session
     * creation.
     */ 
    public Object createSession(
        Map info,                       // in
        HttpServletRequest request,     // in
        HttpServletResponse response,   // in/out
        StringBuffer targetApplication  // in/out
    ) throws SessionException {

        String realm = (String)info.get(REALM);
        if (realm == null || realm.length() == 0) {
            throw new SessionException(bundle.getString("nullRealm"));
        }
        String principalName = (String)info.get(PRINCIPAL_NAME);
        if (principalName == null || principalName.length() == 0) {
            throw new SessionException(bundle.getString("nullPrincipal"));
        }

        String authLevel = (String)info.get(AUTH_LEVEL);

        Object oldSession = null;
        
        if (request != null) {
            try {
                oldSession = getSession(request);
                String oldPrincipal = getPrincipalName(oldSession);
                oldPrincipal = oldPrincipal.toLowerCase();
                if ((!oldPrincipal.equals(principalName.toLowerCase())) &&
                    (!oldPrincipal.startsWith(
                    "id=" + principalName.toLowerCase() +",")))
                {
                    invalidateSession(oldSession, request, response);
                    oldSession = null;
                }
            } catch (SessionException se) {
                oldSession = null;
            }
        }

        // Call auth module "Federation"
        AuthContext ac = null;
        try {
            if (oldSession != null) {
                ac = new AuthContext((SSOToken) oldSession);
            } else {
                ac = new AuthContext(realm);
            }
            ac.login(AuthContext.IndexType.MODULE_INSTANCE, "Federation", null, null, request, response);
        } catch (AuthLoginException ale) {
            throw new SessionException(ale);
        }

        Callback[] callbacks = null;
        while (ac.hasMoreRequirements()) {
            callbacks = ac.getRequirements();
            if (callbacks == null || callbacks.length == 0) {
                continue;
            }
            for (int i = 0; i < callbacks.length; i++) {
                if (callbacks[i] instanceof NameCallback) {
                    NameCallback nc = (NameCallback) callbacks[i];
                    if (nc.getPrompt().equals(PRINCIPAL_NAME)) {
                        nc.setName(principalName);
                    } else if (nc.getPrompt().equals(RANDOM_SECRET)) {
                        String randomString = generateSecret();
                        while (secretSet.contains(randomString)) {
                            randomString = generateSecret();
                        }
                        secretSet.add(randomString);
                        nc.setName(randomString);
                    } else if (nc.getPrompt().equals(AUTH_LEVEL)) {
                        nc.setName(authLevel);
                    }
                }
            }
            break;
        }
        ac.submitRequirements(callbacks);

        SSOToken ssoToken = null;
        if (ac.getStatus() == AuthContext.Status.SUCCESS) {
            try {
                ssoToken = ac.getSSOToken();
            } catch (Exception e) {
                throw new SessionException(e.getMessage());
            }                
        } else if (ac.getStatus() == AuthContext.Status.FAILED) {
            // TODO: test again when auth changes are done so the error code
            // is set and passed over
            int failureCode = SessionException.AUTH_ERROR_NOT_DEFINED;
            AuthLoginException ale = ac.getLoginException();
            String authError = null;
            if (ale != null) {
                authError = ale.getErrorCode();
            }
            if (authError == null) {
                failureCode = SessionException.AUTH_ERROR_NOT_DEFINED;
            } else if (authError.equals(AMAuthErrorCode.AUTH_USER_INACTIVE)) {
                failureCode = SessionException.AUTH_USER_INACTIVE;
            } else if(authError.equals(AMAuthErrorCode.AUTH_USER_LOCKED)) {
                failureCode = SessionException.AUTH_USER_LOCKED;
            } else if(authError.equals(
                AMAuthErrorCode.AUTH_ACCOUNT_EXPIRED))
            {
                failureCode = SessionException.AUTH_ACCOUNT_EXPIRED;
            }
            
            SessionException se = null;
            if (ale != null) {
                se = new SessionException(ale);
            } else {
                se = new SessionException(bundle.getString("loginFailed"));
            }
            se.setErrCode(failureCode);
            throw se;
        } else {
            throw new SessionException(bundle.getString("loginFailed"));
        }

        if (response != null) {
            ServiceSchemaManager scm = null;
            try {
                scm = new ServiceSchemaManager(
                    "iPlanetAMPlatformService", ssoToken);
            } catch (Exception e) {
                throw new SessionException(e);
            }
            ServiceSchema platformSchema = null;
            try {
                platformSchema = scm.getGlobalSchema();
            } catch (SMSException se) {
                throw new SessionException(se);
            }
            setLoadBalancerCookie(request, response);
            Set cookieDomains = (Set)platformSchema.getAttributeDefaults().
                get("iplanet-am-platform-cookie-domains");
            String value = ssoToken.getTokenID().toString();
            if (cookieDomains.size() == 0) {
                Cookie cookie =
                    CookieUtils.newCookie(cookieName, value, "/");
		CookieUtils.addCookieToResponse(response, cookie);
            } else {
                Iterator it = cookieDomains.iterator();
                Cookie cookie = null;
                String cookieDomain = null;
                while (it.hasNext()) {
                    cookieDomain = (String) it.next();
                    if (debug.messageEnabled()) {
                        debug.message("cookieName=" + cookieName);
                        debug.message("value=" + value);
                        debug.message("cookieDomain=" + cookieDomain);
                    }
                    cookie = CookieUtils.newCookie(
                        cookieName, value,
                        "/", cookieDomain);
		    CookieUtils.addCookieToResponse(response, cookie);
                }
            }
            if (urlRewriteEnabled && targetApplication != null) {
                int n = targetApplication.length();
                if (n > 0) {
                    String rewrittenURL = rewriteURL(
                        ssoToken, targetApplication.toString());
                    targetApplication.delete(0, n);
                    targetApplication.append(rewrittenURL);
                }
            }   
        }

        // set all properties in the info map to sso token
        try {
            Iterator it = info.keySet().iterator();
            while (it.hasNext()) {
                String keyName = (String) it.next();
                if (keyName.equals(AUTH_LEVEL)) {
                    continue;
                }
                String keyVal = (String) info.get(keyName);
                ssoToken.setProperty(keyName, 
                    StringUtils.getEscapedValue(keyVal));
            }
        } catch (SSOException se) {
            throw new SessionException(se);
        }
        return ssoToken;
    }
    
    /**
     * Sets a load balancer cookie in the suppled HTTP response. The load
     * balancer cookie's value is set per server instance and is used to
     * support sticky load balancing.
     *
     * @param request The HTTP request.
     * @param response the <code>HttpServletResponse</code> that will be sent
     *        to the user.
     */
    public void setLoadBalancerCookie(HttpServletRequest request, HttpServletResponse response) {
        FSUtils.setlbCookie(request, response);
    }

    /**
     * May be used by both SP and IDP side for getting an existing
     * session given an session ID.
     * @param sessionID the unique session handle.
     * @return the corresponding session object.
     * @throws SessionException if an error occurred during session
     * retrieval.
     */
    public Object getSession(String sessionID)
        throws SessionException {
        try {
            SSOToken session = SSOTokenManager.getInstance().createSSOToken(
                sessionID);
            SSOTokenManager.getInstance().refreshSession(session);
            return session;
        } catch (Throwable e) {
            throw new SessionException(e);
        }
    }
    
    /**
     * May be used by both SP and IDP side for getting an existing
     * session given a browser initiated HTTP request.
     * @param request the browser initiated HTTP request.
     * @return the corresponding session object.
     * @throws SessionException if an error occurred during session
     * retrieval.
     */
    public Object getSession(HttpServletRequest request)
        throws SessionException {
        try {
            SSOToken session = SSOTokenManager.getInstance().createSSOToken(
                request);
            SSOTokenManager.getInstance().refreshSession(session);
            return session;
        } catch (Exception ex) {
            debug.message("FMSessionProvider.getSession: Could not get the session" +
                    " from the HTTP request: " + ex.getMessage());
            throw new SessionException(ex);
        }
    }
    
    /**
     * May be used by both SP and IDP side to invalidate a session.
     * In case of SLO with SOAP, the last two input parameters
     * would have to be null
     * @param session the session to be invalidated
     * @param request the browser initiated HTTP request.
     * @param response the HTTP response going back to browser.
     * @throws SessionException if an error occurred during session
     * retrieval.     
     */
    public void invalidateSession(
        Object session,
        HttpServletRequest request,   // optional input
        HttpServletResponse response  // optional input
    ) throws SessionException {
        try {
            SSOToken token = (SSOToken)session;
            AuthUtils.logout(token.getTokenID().toString(), request, response);
            if ((request != null) && (response != null)) {
                AuthUtils.clearAllCookies(request, response);
            }
        } catch (SSOException se) {
            throw new SessionException(se);
        }
    }

    /**
     * Indicates whether the session is still valid.
     * This is useful for toolkit clean-up thread.
     * @param session Session object
     * @return boolean value indicating whether the session
     * is still valid
     */
    public boolean isValid(Object session)
        throws SessionException {
        try {
            return SSOTokenManager.getInstance().isValidToken(
                (SSOToken)session);
        } catch (SSOException se) {
            throw new SessionException(se);
        }
    }   

    /**
     * The returned session ID should be unique and not 
     * change during the lifetime of this session.
     */
    public String getSessionID(Object session) {
        return ((SSOToken)session).getTokenID().toString();
    }

    /**
     * Returns princiapl name, or user name given the session
     * object. 
     * @param session Session object.
     * @return principal name, or user name. 
     * @throws SessionException if this operation causes an error.
     */
    public String getPrincipalName(Object session)
        throws SessionException {    
        try {        
            return ((SSOToken)session).getProperty(
                Constants.UNIVERSAL_IDENTIFIER);
        } catch (SSOException se) {
            throw new SessionException(se);
        }
    }
    
    
    public long getTimeLeft(Object session)
        throws SessionException {
        try {        
            return ((SSOToken)session).getTimeLeft();
        } catch (SSOException se) {
            throw new SessionException(se);
        }
    }
        
    /**
     * Stores a property in the session object.
     * @param session the session object.
     * @param name the property name.
     * @param values the property values.
     * @throws SessionException if setting the property causes an error.
     */
    public void setProperty(
        Object session,
        String name,
        String[] values
    ) throws SessionException {
        if (name == null || values == null ||
            values.length == 0) {
            return;
        }
        String propValue = null;
        if (values.length == 1) {
            propValue = StringUtils.getEscapedValue(values[0]);
        } else {
            StringBuffer buffer = new StringBuffer(
                StringUtils.getEscapedValue(values[0]));
            for (int i=1; i<values.length; i++) {
                buffer.append(PROPERTY_VALUES_SEPARATOR).
                    append(StringUtils.getEscapedValue(values[i]));
            }
            propValue = buffer.toString();
        }
        try {
            ((SSOToken)session).setProperty(name, propValue);
        } catch (SSOException se) {
            throw new SessionException(se);
        }
    }

    /**
     * Retrieves a property from the session object.
     * @param session the session object.
     * @param name the property name.
     * @return the property values.
     * @throws SessionException if getting the property causes an error.
     */
    public String[] getProperty(Object session, String name)
        throws SessionException {
        if (session == null || name == null || name.length() == 0) {
            return null;
        }
        if (name.equals(AUTH_METHOD)) {
            name = AUTH_TYPE;
        }
        String values = null;
        try {
            if (SAML2Constants.IDP_SESSION_INDEX.equals(name)) {
                // get session property by ignoring session state
                // this propperty could be retrieve when session idle timed out
                // need to be able to get value without exception
                values = ((SSOToken)session).getProperty(name, true);
            } else {
                values = ((SSOToken)session).getProperty(name);
            }
        } catch (SSOException se) {
            throw new SessionException(se);
        }
        if (values == null || values.length() == 0) {
            return null;
        }
        if (name.equals(AUTH_TYPE)) {
            String[] retValues = new String[1];
            if (values.equalsIgnoreCase(SAMLConstants.AUTH_METHOD_CERT)) {
                retValues[0] = SAMLConstants.AUTH_METHOD_CERT_URI;
            }
            if (values.equalsIgnoreCase(SAMLConstants.AUTH_METHOD_KERBEROS)) {
                retValues[0] = SAMLConstants.AUTH_METHOD_KERBEROS_URI;
            }
            if (SAMLConstants.passwordAuthMethods.contains(
                values.toLowerCase())) {
                retValues[0] = SAMLConstants.AUTH_METHOD_PASSWORD_URI;
            }
            if (SAMLConstants.tokenAuthMethods.contains(
                values.toLowerCase())) {
                retValues[0] = SAMLConstants.AUTH_METHOD_HARDWARE_TOKEN_URI;
            } else {
                retValues[0] = SAMLConstants.AUTH_METHOD_URI_PREFIX+values;
            }
            return retValues;
        }
        
        if (name.equals(SAML2Constants.ORGANIZATION)) {
            String[] retValues = new String[1];
            retValues[0] =  DNMapper.orgNameToRealmName(values);
            return retValues;
        }
        
        String[] returnVals = values.split("\\"+PROPERTY_VALUES_SEPARATOR);
        for (int i= 0; i < returnVals.length; i++) {
            returnVals[i] = StringUtils.getUnescapedValue(returnVals[i]);
        }
        return returnVals; 
    }

    /*
     * Rewrites an URL with session information in case
     * cookie setting is not supported.
     * @param session the session object.
     * @param URL the URL to be rewritten.
     * @return the rewritten URL.
     * @throws SessionException if rewritting the URL
     * causes an error.
     */
    public String rewriteURL(Object session, String URL)
        throws SessionException {
            
        if (urlRewriteEnabled) {
            try {
                return ((SSOToken)session).encodeURL(URL);
            } catch (SSOException se) {
                throw new SessionException(se);
            }
        }
        return URL;
    }

    /**
     * Registers a listener for the session.
     *
     * If the provided session does not support listeners, calling this method will throw <code>SessionException</code>.
     *
     * @param session the session object.
     * @param listener listener for the session invalidation event.
     * 
     * @throws SessionException if adding the listener caused an error.
     */
    public void addListener(Object session, SessionListener listener) throws SessionException {
        try {
            ((SSOToken)session).addSSOTokenListener(new SSOTokenListenerImpl(session, listener));
        } catch (SSOException se) {
            throw new SessionException(se);
        }
    }

    class SSOTokenListenerImpl implements SSOTokenListener {
        private Object session = null;
        private SessionListener listener = null;
        
        public SSOTokenListenerImpl(
            Object session, SessionListener listener
        ) {
            this.session = session;
            this.listener = listener;
        }

        public void ssoTokenChanged(SSOTokenEvent evt) {
            int eventType = -1;

            try {
                eventType = evt.getType();
            } catch (SSOException se) {
            }
            if (eventType==SSOTokenEvent.SSO_TOKEN_IDLE_TIMEOUT ||
                eventType==SSOTokenEvent.SSO_TOKEN_MAX_TIMEOUT ||
                eventType==SSOTokenEvent.SSO_TOKEN_DESTROY) {

                listener.sessionInvalidated(session);
            }
        }
    }
}
