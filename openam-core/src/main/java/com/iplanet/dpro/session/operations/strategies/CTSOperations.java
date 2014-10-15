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
import org.forgerock.openam.cts.api.fields.CoreTokenField;
import org.forgerock.openam.cts.api.filter.TokenFilterBuilder;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.api.tokens.TokenIdFactory;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.exceptions.ReadFailedException;
import org.forgerock.openam.cts.impl.query.PartialToken;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;

/**
 * Responsible for providing an implementation of the Session Operations
 * strategy that is backed by the Core Token Service (CTS).
 *
 * Note that this class now deals ONLY WITH REMOTE SESSIONS.  Local sessions should be
 * handled by the LocalOperations class and if a local session is accidentally passed
 * into a couple of these functions (logout and destroy), you'll get a SessionException.
 *
 * For this reason you'll find that read operations work, write operations should be
 * delegated to the home server (achieved via PLL requests made by the RemoteOperations
 * class).
 */
public class CTSOperations implements SessionOperations {

    private final CTSPersistentStore cts;
    private final SessionAdapter adapter;
    private final TokenIdFactory idFactory;
    private final SessionInfoFactory sessionInfoFactory;
    private final SessionService sessionService;
    private final Debug debug;
    private final RemoteOperations remote;

    /**
     * Guice initialised constructor.
     *
     * @param cts Non null.
     * @param adapter Non null.
     * @param idFactory Non null.
     * @param sessionInfoFactory Non null.
     * @param sessionService Non null.
     * @param remote Non null, failover for refresh operation.
     * @param debug Non null.
     */
    @Inject
    public CTSOperations(CTSPersistentStore cts, SessionAdapter adapter,
                         TokenIdFactory idFactory, SessionInfoFactory sessionInfoFactory,
                         SessionService sessionService, RemoteOperations remote,
                         @Named(SessionConstants.SESSION_DEBUG) Debug debug) {
        this.cts = cts;
        this.adapter = adapter;
        this.idFactory = idFactory;
        this.sessionInfoFactory = sessionInfoFactory;
        this.sessionService = sessionService;
        this.remote = remote;
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
        try {
            InternalSession internalSession = readToken(sessionID);
            // Modifies the Session if required.
            if (reset) {
                internalSession.setLatestAccessTime();
            }

            return sessionInfoFactory.getSessionInfo(internalSession, sessionID);
        } catch (ReadFailedSessionException e) {
            return remote.refresh(session, reset);
        }
    }

    /**
     * Performs a remote logout of the session.
     *
     * @param session Non null Session to use for the delete.
     * @throws SessionException If we somehow passed a local session into this function
     */
    public void logout(Session session) throws SessionException {

        // See OPENAM-4543.  The check for a local session should be removed if it proves to be a performance
        // bottleneck.  As Peter points out, because we "know" this is a remote session, we will force checkSessionLocal
        // to look in three hashtables, then do a couple of string compares... all for peace of mind.
        //
        SessionID sessionID = session.getID();
        if (sessionService.checkSessionLocal(sessionID)) {
            throw new SessionException("CTSOperations received a local session (only remote sessions expected)");
        }
        remote.logout(session);
    }

    /**
     * Perform a remote destroy operation on the SessionID, because we know this is a remote session.
     *
     * @param requester {@inheritDoc}
     * @param session {@inheritDoc}
     * @throws SessionException if we somehow passed a local session into this function
     */
    @Override
    public void destroy(Session requester, Session session) throws SessionException {

        // Comments as for logout.  The check for a local session should be removed if it proves to be a performance
        // bottleneck.
        //
        SessionID sessionID = session.getID();
        if (sessionService.checkSessionLocal(sessionID)) {
            throw new SessionException("CTSOperations received a local session (only remote sessions expected)");
        }
        remote.destroy(requester, session);
    }

    /**
     * Set the property of the remote session.
     *
     * {@inheritDoc}
     */
    public void setProperty(Session session, String name, String value) throws SessionException {
        remote.setProperty(session, name, value);
    }

    /**
     * Reading from CTS should be safe, since it changes to the session will be written to CTS by the remote server.
     *
     * @param sessionID the session id
     * @return the internal session as stored in CTS
     * @throws SessionException
     */
    private InternalSession readToken(SessionID sessionID) throws SessionException {
        String tokenID = idFactory.toSessionTokenId(sessionID);
        try {
            Token token = cts.read(tokenID);
            return adapter.fromToken(token);
        } catch (ReadFailedException e) {
            throw new ReadFailedSessionException(e);
        } catch (CoreTokenException e) {
            debug.error("Failed to read token: " + tokenID, e);
            throw new SessionException(e);
        }
    }

    /**
     * Checks whether the CTS store contains the session.
     * @param session The requested session.
     * @return Whether the session is in the CTS store.
     */
    public boolean hasSession(Session session) throws SessionException {
        String tokenId = idFactory.toSessionTokenId(session.getID());
        boolean found = false;
        try {
            Collection<PartialToken> tokens = cts.attributeQuery(new TokenFilterBuilder()
                    .returnAttribute(CoreTokenField.TOKEN_ID)
                    .withAttribute(CoreTokenField.TOKEN_ID, tokenId)
                    .build());
            found = !tokens.isEmpty();
        } catch (CoreTokenException e) {
            if (debug.messageEnabled()) {
                debug.message("Could not find token: " + tokenId, e);
            }
        }
        return found;
    }

    private static class ReadFailedSessionException extends SessionException {
        ReadFailedSessionException(ReadFailedException e) {
            super(e);
        }
    }
}
