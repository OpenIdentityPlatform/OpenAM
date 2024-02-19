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
 * Portions Copyrighted 2010-2016 ForgeRock AS.
 * Portions Copyrighted 2023 3A Systems LLC
 */

package com.iplanet.dpro.session.service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.session.SessionCookies;
import org.forgerock.openam.session.service.SessionAccessManager;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.sun.identity.monitoring.Agent;
import com.sun.identity.monitoring.MonitoringUtil;
import com.sun.identity.monitoring.SsoServerSessSvcImpl;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;

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
    private final SessionServerConfig serverConfig;
    private final AuthenticationSessionStore authenticationSessionStore;
    private final SessionAccessManager sessionAccessManager;

    @Inject
    public InternalSessionFactory(
            @Named(SessionConstants.SESSION_DEBUG) Debug sessionDebug,
            SessionServerConfig serverConfig,
            AuthenticationSessionStore authenticationSessionStore,
            SessionAccessManager sessionAccessManager) {

        this.sessionDebug = sessionDebug;
        this.serverConfig = serverConfig;
        this.authenticationSessionStore = authenticationSessionStore;
        this.sessionAccessManager = sessionAccessManager;
    }

    /**
     * Creates a new Internal Session
     *
     * @param domain      Authentication Domain
     * @param stateless   Indicates whether or not this session should be issued as a stateless session.
     */
    public InternalSession newInternalSession(String domain, boolean stateless) {
        return newInternalSession(domain, stateless, true);
    }

    /**
     * Creates a new Internal Session
     *
     * @param domain      Authentication Domain
     * @param stateless   Indicates whether or not this session should be issued as a stateless session.
     * @param checkCts    Indicates whether or not this session exists in cts.
     */
    public InternalSession newInternalSession(String domain, boolean stateless, boolean checkCts) {
        try {
            final SessionID sessionID;
            if(checkCts) {
                sessionID = generateSessionId(domain);
            } else {
                sessionID = SessionID.generateSessionID(serverConfig, domain);
            }
            return generateInternalSession(sessionID, stateless);
        } catch (SessionException e) {
            sessionDebug.error("Error creating new session", e);
            return null;
        }
    }

    private InternalSession generateInternalSession(SessionID sid, boolean stateless) throws SessionException {

        InternalSession session = new InternalSession(sid);

        if (!stateless) {
            String sessionHandle = sid.generateSessionHandle(serverConfig);
            session.setSessionHandle(sessionHandle);

            authenticationSessionStore.addSession(session);

            if (SystemProperties.isServerMode()) {
                if (MonitoringUtil.isRunning()) {
                    SsoServerSessSvcImpl sessImpl =
                            Agent.getSessSvcMBean();
                    sessImpl.incCreatedSessionCount();
                }
            }
        }

        session.setLatestAccessTime();
        String amCtxId = SessionID.generateAmCtxID(serverConfig);
        session.putProperty(Constants.AM_CTX_ID, amCtxId);
        session.putProperty(sessionCookies.getLBCookieName(), serverConfig.getLBCookieValue());
        return session;
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
        } while (sessionAccessManager.getInternalSession(sid) != null);
        return sid;
    }

}
