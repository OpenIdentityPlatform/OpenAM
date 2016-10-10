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

package org.forgerock.openam.session.service;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.adapters.SessionAdapter;
import org.forgerock.openam.cts.api.fields.SessionTokenField;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.filter.TokenFilterBuilder;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.api.tokens.TokenIdFactory;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.utils.CollectionUtils;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionAuditor;
import com.iplanet.dpro.session.service.SessionLogging;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.dpro.session.service.SessionServiceConfig;
import com.sun.identity.shared.debug.Debug;

/**
 * This class is responsible for loading sessions from the CTS.
 */
public class SessionPersistentStore {
    private final Debug debug;
    private final CTSPersistentStore coreTokenService;
    private final SessionAdapter tokenAdapter;
    private final TokenIdFactory tokenIdFactory;

    @Inject
    public SessionPersistentStore(@Named(SessionConstants.SESSION_DEBUG) final Debug debug,
                                  final CTSPersistentStore coreTokenService,
                                  final SessionAdapter tokenAdapter,
                                  final TokenIdFactory tokenIdFactory) {

        this.debug = debug;
        this.coreTokenService = coreTokenService;
        this.tokenAdapter = tokenAdapter;
        this.tokenIdFactory = tokenIdFactory;
    }

    /**
     * Persist the provided session to the CTS, or update it if it is already there.
     * @param session The session to persist.
     * @throws CoreTokenException If the operation fails.
     */
    public void save(InternalSession session) throws CoreTokenException {
        coreTokenService.update(tokenAdapter.toToken(session));
    }

    /**
     * Remove the provided session from the CTS.
     * @param session The session to delete from the CTS.
     * @throws CoreTokenException If the operation fails.
     */
    public void delete(InternalSession session) throws CoreTokenException {
        String tokenId = tokenIdFactory.toSessionTokenId(session.getID());
        coreTokenService.delete(tokenId);
    }

    /**
     * This will recover the specified session from the repository based on the provided session id.
     * Returns null if no session was recovered.
     * @param sessionID Session ID
     */
    public InternalSession recoverSession(SessionID sessionID) {

        String tokenId = tokenIdFactory.toSessionTokenId(sessionID);
        Token token = null;

        try {
            token = coreTokenService.read(tokenId);
        } catch (CoreTokenException e) {
            debug.error("Failed to retrieve session by its handle", e);
        }
        if (token == null) {
            return null;
        }

        return getInternalSessionFromToken(token);
    }

    /**
     * This will recover the specified session from the repository based on the provided session handle.
     * Returns null if no session was recovered.
     * @param sessionHandle Session Handle
     */
    public InternalSession recoverSessionByHandle(String sessionHandle) {

        final TokenFilter tokenFilter = new TokenFilterBuilder()
                .withAttribute(SessionTokenField.SESSION_HANDLE.getField(), sessionHandle)
                .build();

        Token token = null;

        try {
            final Collection<Token> results = coreTokenService.query(tokenFilter);
            if (results.isEmpty()) {
                return null;
            }
            if (results.size() != 1) {
                debug.error("Duplicate session handle found in Core Token Service");
                return null;
            }
            token = CollectionUtils.getFirstItem(results);
        } catch (CoreTokenException e) {
            debug.error("Failed to retrieve session by its handle", e);
        }
        if (token == null) {
            return null;
        }
        return getInternalSessionFromToken(token);
    }

    private InternalSession getInternalSessionFromToken(Token token) {

        /*
         * As a side effect of deserialising an InternalSession, we must trigger
         * the InternalSession to reschedule its timing task to ensure it
         * maintains the session expiry function.
         */
        InternalSession session = tokenAdapter.fromToken(token);
        session.setSessionServiceDependencies(InjectorHolder.getInstance(SessionService.class),
                InjectorHolder.getInstance(SessionServiceConfig.class),
                InjectorHolder.getInstance(SessionLogging.class),
                InjectorHolder.getInstance(SessionAuditor.class),
                debug);
        session.scheduleExpiry();

        return session;
    }
}
