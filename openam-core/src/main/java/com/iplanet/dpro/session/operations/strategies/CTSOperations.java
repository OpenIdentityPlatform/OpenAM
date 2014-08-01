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
 * Copyright 2014 ForgeRock AS.
 */
package com.iplanet.dpro.session.operations.strategies;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.operations.SessionOperations;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionConstants;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.dpro.session.utils.SessionInfoFactory;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.adapters.SessionAdapter;
import org.forgerock.openam.cts.api.fields.SessionTokenField;
import org.forgerock.openam.cts.api.filter.TokenFilterBuilder;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.api.tokens.TokenIdFactory;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.query.PartialToken;

import java.util.Collection;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Responsible for providing an implementation of the Session Operations
 * strategy that is backed by the Core Token Service (CTS).
 *
 * The operations themselves depend on the presence of an InternalSession which
 * the CTS can provide by deserialising the token stored in the CTS.
 *
 * Note: This implementation is only appropriate in the situation where the
 * remote site that is typically responsible for servicing a Session is down.
 */
public class CTSOperations implements SessionOperations {
    private final CTSPersistentStore cts;
    private final SessionAdapter adapter;
    private final TokenIdFactory idFactory;
    private final SessionInfoFactory sessionInfoFactory;
    private final SessionService sessionService;
    private final Debug debug;

    /**
     * Guice initialised constructor.
     *
     * @param cts Non null.
     * @param adapter Non null.
     * @param idFactory Non null.
     * @param sessionInfoFactory Non null.
     * @param sessionService Non null.
     * @param debug Non null.
     */
    @Inject
    public CTSOperations(CTSPersistentStore cts, SessionAdapter adapter,
                         TokenIdFactory idFactory, SessionInfoFactory sessionInfoFactory,
                         SessionService sessionService,
                         @Named(SessionConstants.SESSION_DEBUG) Debug debug) {
        this.cts = cts;
        this.adapter = adapter;
        this.idFactory = idFactory;
        this.sessionInfoFactory = sessionInfoFactory;
        this.sessionService = sessionService;
        this.debug = debug;
    }

    /**
     * Fetch the SessionInfo for the Session ID.
     *
     * The SessionID is used to recall the token from the CTS, which is then
     * converted to a SessionInfo object. The token is not modified as part of
     * this operation.
     *
     * @param session Non null SessionID to update.
     * @param reset If true, then update the last modified timestamp of the Session.
     * @return A non null SessionInfo
     * @throws SessionException If there was a problem locating the Session in the CTS.
     */
    public SessionInfo refresh(Session session, boolean reset) throws SessionException {
        SessionID sessionID = session.getID();
        InternalSession internalSession = readToken(sessionID);

        // Modifies the Session if required.
        if (reset) {
            internalSession.setLatestAccessTime();
        }

        return sessionInfoFactory.getSessionInfo(internalSession, sessionID);
    }

    /**
     * Perform a logout based on the SessionID.
     *
     * Locates the token in the CTS and performs a delete.
     *
     * @param session Non null SessionID to use for the delete.
     * @throws SessionException If there was a problem deleting the token from the CTS.
     */
    public void logout(Session session) throws SessionException {
        SessionID sessionID = session.getID();
        removeToken(sessionID);
        sessionService.logoutInternalSession(sessionID);
    }

    /**
     * Perform a destroy operation on the SessionID.
     *
     * This will, like logout, perform a delete on the token.
     *
     * @param requester {@inheritDoc}
     * @param session {@inheritDoc}
     * @throws SessionException {@inheritDoc}
     */
    @Override
    public void destroy(Session requester, Session session) throws SessionException {
        SessionID sessionID = session.getID();
        String sid = sessionID.toString();
        if (sid.startsWith(SessionService.SHANDLE_SCHEME_PREFIX)) {
            try {
                Collection<PartialToken> matches = cts.attributeQuery(new TokenFilterBuilder()
                        .returnAttribute(SessionTokenField.SESSION_ID.getField())
                        .withAttribute(SessionTokenField.SESSION_HANDLE.getField(), sid)
                        .build());
                for (PartialToken match : matches) {
                    //There should be always only one match, so this should be safe
                    sessionID = new SessionID(match.<String>getValue(SessionTokenField.SESSION_ID.getField()));
                }
            } catch (CoreTokenException cte) {
                debug.error("Failed to query/delete token based on session handle: " + sid, cte);
                throw new SessionException(cte);
            }
        }

        sessionService.checkPermissionToDestroySession(requester, sessionID);
        sessionService.destroyInternalSession(sessionID);
        removeToken(sessionID);
    }

    /**
     * Sets the property using the {@link InternalSession#putProperty} method.
     *
     * {@inheritDoc}
     */
    public void setProperty(Session session, String name, String value) throws SessionException {
        InternalSession internalSession = readToken(session.getID());
        internalSession.putProperty(name, value);

        Token token = adapter.toToken(internalSession);
        try {
            cts.update(token);
        } catch (CoreTokenException e) {
            debug.error("Failed to update token: " + token.getTokenId(), e);
            throw new SessionException(e);
        }
    }

    private void removeToken(SessionID sessionID) throws SessionException {
        String tokenId = idFactory.toSessionTokenId(sessionID);
        try {
            cts.delete(tokenId);
        } catch (CoreTokenException e) {
            debug.error("Failed to delete Token: " + tokenId, e);
            throw new SessionException(e);
        }
    }

    private InternalSession readToken(SessionID sessionID) throws SessionException {
        String tokenID = idFactory.toSessionTokenId(sessionID);
        try {
            Token token = cts.read(tokenID);
            return adapter.fromToken(token);
        } catch (CoreTokenException e) {
            debug.error("Failed to read token: " + tokenID, e);
            throw new SessionException(e);
        }
    }
}
