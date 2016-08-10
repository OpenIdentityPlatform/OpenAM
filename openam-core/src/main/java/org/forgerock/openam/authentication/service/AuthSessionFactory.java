/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.authentication.service;

import static org.forgerock.openam.session.SessionConstants.APPLICATION_SESSION;
import static org.forgerock.openam.session.SessionConstants.VALID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.session.service.SessionAccessManager;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.service.*;
import com.sun.identity.shared.debug.Debug;

/**
 * Factory for creating and caching an Authentication Session which is used by the authentication framework.
 */
@Singleton
public class AuthSessionFactory {

    private InternalSession authSession; // cached auth session
    private final Debug sessionDebug;
    private final SessionAccessManager sessionAccessManager;
    private final MonitoringOperations monitoringOperations;
    private final InternalSessionFactory internalSessionFactory;
    private final DsameAdminTokenProvider dsameAdminTokenProvider;

    @Inject
    public AuthSessionFactory(final @Named(SessionConstants.SESSION_DEBUG) Debug sessionDebug,
                              final SessionAccessManager sessionAccessManager,
                              final MonitoringOperations monitoringOperations,
                              final InternalSessionFactory internalSessionFactory,
                              final DsameAdminTokenProvider dsameAdminTokenProvider) {
        this.sessionDebug = sessionDebug;
        this.sessionAccessManager = sessionAccessManager;
        this.monitoringOperations = monitoringOperations;
        this.internalSessionFactory = internalSessionFactory;
        this.dsameAdminTokenProvider = dsameAdminTokenProvider;
    }

    /**
     * Returns the Internal Session used by the Auth Services.
     *
     * @param domain      Authentication Domain
     */
    public Session getAuthenticationSession(String domain) {
        try {
            if (authSession == null) {
                // Create a special InternalSession for Authentication Service
                authSession = getServiceSession(domain);
            }
            return authSession != null ? sessionAccessManager.getSession(authSession.getID()) : null;
        } catch (Exception e) {
            sessionDebug.error("Error creating service session", e);
            return null;
        }
    }

    /**
     * Returns a non-expiring application token which can be used by services.
     *
     * @param domain Authentication Domain
     */
    private InternalSession getServiceSession(String domain) {
        try {
            InternalSession session = internalSessionFactory.newInternalSession(domain, false);
            session.setType(APPLICATION_SESSION);
            session.setClientID(dsameAdminTokenProvider.getDsameAdminDN());
            session.setClientDomain(domain);
            session.setNonExpiring();
            session.setState(VALID);
            monitoringOperations.incrementActiveSessions();
            return session;
        } catch (Exception e) {
            sessionDebug.error("Error creating service session", e);
            return null;
        }
    }
}
