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
 * $Id: WebServiceAuthenticatorImpl.java,v 1.4 2008/08/06 17:29:25 exu Exp $
 *
 * Portions Copyrighted 2015-2016 ForgeRock AS.
 */

package com.sun.identity.liberty.ws.soapbinding;

import static org.forgerock.openam.utils.Time.*;

import com.iplanet.am.util.Cache;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.security.x509.CertUtils;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import org.forgerock.guice.core.InjectorHolder;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import java.security.AccessController;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

class WebServiceAuthenticatorImpl implements WebServiceAuthenticator {
    private static final String PRINCIPAL_PROP = "Principal";
    private static final String PRINCIPALS_PROP = "Principals";
    private static final String AUTH_TYPE_PROP = "AuthType";
    private static final String AUTH_INSTANT_PROP = "authInstant";
    private static final String ANONYMOUS_PRINCIPAL = "anonymous";
    private static final String SESSION_SERVICE_NAME =
            "iPlanetAMSessionService";
    private static final String MAX_SESSION_TIME =
            "iplanet-am-session-max-session-time";
    private static final String IDLE_TIME =
            "iplanet-am-session-max-idle-time";
    private static final String CACHE_TIME =
            "iplanet-am-session-max-caching-time";
    private static final int DEFAULT_MAX_SESSION_TIME = 120;
    private static final int DEFAULT_IDLE_TIME = 30;
    private static final int DEFAULT_CACHE_TIME = 3;
    private static Cache ssoTokenCache = new Cache(1000);
    private static SSOTokenManager ssoTokenManager = null;
    private static ServiceSchema sessionSchema = null;
    private static String rootSuffix =
            SystemPropertiesManager.get("com.iplanet.am.rootsuffix");
    private static Debug debug = Debug.getInstance("libIDWSF");
    
    static {
        try {
            ssoTokenManager = SSOTokenManager.getInstance();
        } catch (Exception ex) {
            debug.error("WebServiceAuthenticatorImpl.static: " +
                "unable to get SSOTokenManager", ex);
        }
        try {
            SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                    AdminTokenAction.getInstance());
            ServiceSchemaManager scm = new ServiceSchemaManager(
                    SESSION_SERVICE_NAME, adminToken);
            sessionSchema = scm.getDynamicSchema();
        } catch (Exception ex) {
            debug.error("WebServiceAuthenticatorImpl.static: " +
                "unable to get session schema", ex);
        }
    }
    
    /**
     * Authenticates a web service using its certificates.
     *
     * @param message a Message object that needs authentication.
     * @param request the HttpServletRequest object that comes from the web
     *                service
     * @return a SSOToken Object for the valid certificates after
     *         successful authentication or null if authentication fails.
     */
    public Object authenticate(Message message,Subject subject,Map state,
            HttpServletRequest request) {
        List certs = null;
        X509Certificate clientCert = message.getPeerCertificate();
        if (clientCert != null) {
            // SSL client auth certificate
            certs = new ArrayList(2);
            certs.add(clientCert);
        }
        
        X509Certificate messageCert = message.getMessageCertificate();
        if (messageCert != null) {
            if (certs == null) {
                certs = new ArrayList(1);
            }
            certs.add(messageCert);
        }
        
        String principal = null;
        StringBuffer principalsSB = null;
        
        if (certs == null) {
            principal = ANONYMOUS_PRINCIPAL;
        } else {
            Set principalsSet = new HashSet(6);
            
            for(Iterator iter = certs.iterator(); iter.hasNext();) {
                X509Certificate cert = (X509Certificate)iter.next();
                if (debug.messageEnabled()) {
                    debug.message(
                        "WebServiceAuthenticatorImpl.authenticate: cert = " +
                        cert);
                }
                
                String subjectDN = CertUtils.getSubjectName(cert);                                        
                if (principal == null) {
                    principal = subjectDN;
                } else if (!principal.equals(subjectDN)) {
                    principalsSet.add(subjectDN);
                }
                
                String issuerDN = CertUtils.getIssuerName(cert);                        
                principalsSet.add(issuerDN);
            }
            
            principalsSB = new StringBuffer(50);
            for(Iterator iter = principalsSet.iterator(); iter.hasNext();) {
                String str = (String)iter.next();
                if (principalsSB.length() == 0) {
                    principalsSB.append(str);
                } else {
                    principalsSB.append("|").append(str);
                }
            }
        }
        
        if (debug.messageEnabled()) {
            debug.message("WebServiceAuthenticatorImpl.authenticate"+
                ": principal = " + principal +
                ", principals = " + principalsSB);
        }
        
        String authMech = message.getAuthenticationMechanism();
        String cacheKey = authMech + " " + principal;
        if (debug.messageEnabled()) {
            debug.message("WebServiceAuthenticatorImpl.authenticate"+
                ": cacheKey = " + cacheKey);
        }
        
        SSOToken ssoToken = null;
        ssoToken = (SSOToken)ssoTokenCache.get(cacheKey);
        if (ssoToken != null) {
            if (ssoTokenManager.isValidToken(ssoToken)) {
                if (debug.messageEnabled()) {
                    debug.message("WebServiceAuthenticatorImpl." +
                        "authenticate: found ssoToken in cache");
                }
                return ssoToken;
            }
            
            if (debug.messageEnabled()) {
                debug.message("WebServiceAuthenticatorImpl." +
                    "authenticate: ssoToken in cache expired");
            }
            synchronized (ssoTokenCache) {
                ssoTokenCache.remove(cacheKey);
            }
            ssoToken = null;
        }
        
        
        String authInstant = null;
        try {
            InternalSession is = InjectorHolder.getInstance(SessionService.class).newInternalSession(null, null, false);
            is.activate("");
            Map attrs = sessionSchema.getAttributeDefaults();
            is.setMaxSessionTime(CollectionHelper.getIntMapAttr(
                    attrs, MAX_SESSION_TIME, DEFAULT_MAX_SESSION_TIME, debug));
            is.setMaxIdleTime(CollectionHelper.getIntMapAttr(
                attrs, IDLE_TIME, DEFAULT_IDLE_TIME, debug));
            is.setMaxCachingTime(CollectionHelper.getIntMapAttr(
                attrs, CACHE_TIME, DEFAULT_CACHE_TIME, debug));
            is.putProperty(AUTH_TYPE_PROP,
                    message.getAuthenticationMechanism());
            authInstant = DateUtils.toUTCDateFormat(newDate());
            is.putProperty(AUTH_INSTANT_PROP, authInstant);
            
            ssoToken = SSOTokenManager.getInstance()
            .createSSOToken(is.getID().toString());
        } catch (Exception ex) {
            debug.error("WebServiceAuthenticatorImpl.authenticate: " +
                "Unable to get SSOToken", ex);
        }
        
        if (ssoToken == null) {
            return null;
        }
        
        try {
            ssoToken.setProperty(PRINCIPAL_PROP, principal);
            if (principalsSB != null) {
                ssoToken.setProperty(PRINCIPALS_PROP, principalsSB.toString());
            }
            if(authInstant != null) {
                ssoToken.setProperty(AUTH_INSTANT_PROP, authInstant);
            }
            
            ssoToken.setProperty(AUTH_TYPE_PROP,
                    message.getAuthenticationMechanism());
            SSOTokenManager.getInstance().refreshSession(ssoToken);
            ssoTokenCache.put(cacheKey, ssoToken);
            
        } catch (Exception ex) {
            debug.error("WebServiceAuthenticatorImpl.authenticate: " +
                "Unable to set SSOToken property", ex);
            return null;
        }
        
        return ssoToken;
    }
}
