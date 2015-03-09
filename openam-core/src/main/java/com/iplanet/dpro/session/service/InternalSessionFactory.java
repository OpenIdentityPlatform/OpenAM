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
 * $Id: SessionService.java,v 1.37 2010/02/03 03:52:54 bina Exp $
 *
 * Portions Copyrighted 2010-2015 ForgeRock AS.
 */

package com.iplanet.dpro.session.service;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.sun.identity.monitoring.Agent;
import com.sun.identity.monitoring.MonitoringUtil;
import com.sun.identity.monitoring.SsoServerSessSvcImpl;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.URLEncDec;
import java.io.DataInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpSession;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.session.SessionCookies;
import org.forgerock.openam.utils.IOUtils;

/**
 * Responsible for creating InternalSession objects.
 *
 * InternalSession creation logic extracted from SessionService class
 * as part of first-pass refactoring to improve SessionService adherence to SRP.
 *
 * @since 13.0.0
 */
/*
 * Further refactoring is warranted.
 */
@Singleton
public class InternalSessionFactory {

    private final SessionCookies sessionCookies = InjectorHolder.getInstance(SessionCookies.class);

    private final Debug sessionDebug;
    private final SessionServiceConfig serviceConfig;
    private final SessionServerConfig serverConfig;
    private final HttpConnectionFactory httpConnectionFactory;
    private final InternalSessionCache cache;

    @Inject
    public InternalSessionFactory(
            @Named(SessionConstants.SESSION_DEBUG) Debug sessionDebug,
            SessionServiceConfig serviceConfig,
            SessionServerConfig serverConfig,
            HttpConnectionFactory httpConnectionFactory,
            InternalSessionCache cache) {

        this.sessionDebug = sessionDebug;
        this.serviceConfig = serviceConfig;
        this.serverConfig = serverConfig;
        this.httpConnectionFactory = httpConnectionFactory;
        this.cache = cache;
    }

    /**
     * Creates a new Internal Session
     *
     * @param domain      Authentication Domain
     * @param httpSession Http Session
     */
    public InternalSession newInternalSession(String domain, HttpSession httpSession) {
        try {
            return newInternalSession(domain, httpSession, true);
        } catch (SessionException e) {
            sessionDebug.error("Error creating new session", e);
            return null;
        }
    }

    /**
     * Creates a new Internal Session
     *
     * @param domain                   Authentication Domain
     * @param httpSession              Http Session
     * @param forceHttpSessionCreation in session failover mode if this parameter is true and
     *                                 httpSession is null, it will cause SessionService to create a
     *                                 new Http session for failover purposes
     */
    InternalSession newInternalSession(String domain, HttpSession httpSession, boolean forceHttpSessionCreation)
            throws SessionException {

        if (serviceConfig.isSessionFailoverEnabled() && !serviceConfig.isUseInternalRequestRoutingEnabled()
                && httpSession == null && forceHttpSessionCreation) {
            return createSession(domain);
        }

        InternalSession session = null;
        SessionID sid = null;

        // generate primary id
        sid = generateSessionId(domain);

        // generate session handle which looks like normal session id
        // except it is not a valid session id
        // and can not be used for anything other than destroySession
        // TODO consider unifying RestrictedTokens and session handle

        String sessionHandle = sid.generateSessionHandle(serverConfig);

        session = new InternalSession(sid);
        session.setSessionHandle(sessionHandle);
        session.setHttpSession(httpSession);
        cache.put(session);
        if (SystemProperties.isServerMode()) {
            if (MonitoringUtil.isRunning()) {
                SsoServerSessSvcImpl sessImpl =
                        Agent.getSessSvcMBean();
                sessImpl.incCreatedSessionCount();
            }
        }

        session.setCreationTime();
        session.setLatestAccessTime();
        String amCtxId = SessionID.generateAmCtxID(serverConfig);
        session.putProperty(Constants.AM_CTX_ID, amCtxId);
        session.putProperty(sessionCookies.getLBCookieName(), serverConfig.getLBCookieValue());
        session.reschedule();
        return session;
    }

    /**
     * Creates InternalSession which is always coupled with Http session This is
     * only used in session failover mode to ensure that every internal session
     * is associated with Http session used as fail-over store
     *
     * @param domain authentication domain passed to newInternalSession
     */

    private InternalSession createSession(String domain) {

        DataInputStream in = null;

        try {
            String query = "?" + GetHttpSession.OP + "=" + GetHttpSession.CREATE_OP;

            if (domain != null) {
                query += "&" + GetHttpSession.DOMAIN + "=" + URLEncDec.encode(domain);
            }

            String routingCookie = null;

            URL url = serverConfig.createLocalServerURL("GetHttpSession" + query);
            HttpURLConnection conn = httpConnectionFactory.createSessionAwareConnection(url, null, routingCookie);
            in = new DataInputStream(conn.getInputStream());

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }

            SessionID sid = new SessionID(in.readUTF());
            return cache.getBySessionID(sid);

        } catch (Exception ex) {
            sessionDebug.error("Failed to retrieve new session", ex);
        } finally {
            IOUtils.closeIfNotNull(in);
        }

        return null;
    }

    /**
     * Generates new SessionID
     *
     * @param domain      session domain
     * @return newly generated session id
     * @throws SessionException
     */
    private SessionID generateSessionId(String domain) throws SessionException {
        SessionID sid;
        do {
            sid = SessionID.generateSessionID(serverConfig, domain);
        } while (cache.getBySessionID(sid) != null || cache.getByHandle(sid.toString()) != null);
        return sid;
    }

}
