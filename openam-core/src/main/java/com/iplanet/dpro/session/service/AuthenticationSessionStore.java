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
 * Portions copyright 2025 3A Systems LLC.
 */

package com.iplanet.dpro.session.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.forgerock.openam.session.service.SessionAccessManager;
import org.forgerock.util.Reject;

import com.iplanet.dpro.session.SessionID;

/**
 * Stores sessions used for authentication which must not yet be written to the store.
 * This comprises both sessions which shouldn't be written to the store (e.g. stateless, authentication),
 * as well as sessions which have yet to be added to the store.
 *
 * Authentication sessions will be stored for the duration of their {@link InternalSession#getTimeLeft()}
 * value, after which they will be removed from this store.
 */
@Singleton
public class AuthenticationSessionStore {
    private final ConcurrentHashMap<SessionID, InternalSession> store = new ConcurrentHashMap<>();

    private final SessionAccessManager sessionAccessManager;

    final ScheduledExecutorService timer = Executors.newScheduledThreadPool(1);
    
    @Inject
    public AuthenticationSessionStore(SessionAccessManager sessionAccessManager) {
        this.sessionAccessManager = sessionAccessManager;
        
        timer.scheduleAtFixedRate(new Runnable() {
            public void run() { 
            	cullExpiredSessions(); 
            }
        }, 1,1, TimeUnit.MINUTES);
    }

    public void addSession(InternalSession session) {
        //cullExpiredSessions(); CPU consuming method

        if (session.isStored()) {
            throw new IllegalStateException("Session was added to temporary store when it is already persisted.");
        }

        if (store.containsKey(session.getSessionID())) {
            throw new IllegalStateException("Session was added to temporary store twice.");
        }

        store.put(session.getSessionID(), session);
    }

    /**
     * Lookup the Session based on its Session ID.
     *
     * Side effect: Calling this method will cull any expired Sessions from this store.
     *
     * @param sessionID Looks up the InternalSession by its SessionID.
     * @return Null if no Session was found otherwise a non null Session.
     */
    public InternalSession getSession(SessionID sessionID) {
        if (sessionID == null) {
            return null;
        }

        InternalSession session = store.get(sessionID);
        if (cullSessionIfNecessary(session)) {
            return null;
        } else {
            return session;
        }
    }

    /**
     * Moves the specified session out of this store and into the persistent store.
     *
     * @param sessionID Non null sessionID for the session to be promoted.
     * @throws IllegalStateException if session not found in the store.
     */
    public void promoteSession(SessionID sessionID) {
        InternalSession session = removeSession(sessionID);
//        if (session == null) {
//            throw new IllegalStateException("Attempted to promote non existent session");
//        }
        if(session != null)
        		sessionAccessManager.persistInternalSession(session);
    }

    private void cullExpiredSessions() {
        for (InternalSession session : store.values()) {
            cullSessionIfNecessary(session);
        }
    }

    private boolean cullSessionIfNecessary(InternalSession session) {
        if (shouldRemove(session)) {
            removeSession(session.getSessionID());
            return true;
        } else {
            return false;
        }
    }

    private boolean shouldRemove(InternalSession session) {
        return session != null && session.getTimeLeft() == 0;
    }

    /**
     * There are cases where an authentication session is created but never used. In these cases
     * we should remove it from the store.
     *
     * @param sessionID Non null SessionID to remove from the store.
     * @return The {@link InternalSession} that was removed.
     * @throws IllegalStateException If the session was not found in the store.
     */
    public InternalSession removeSession(SessionID sessionID) {
        Reject.ifNull(sessionID);
        return store.remove(sessionID);
    }
}
