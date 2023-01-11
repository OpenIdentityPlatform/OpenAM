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

package org.forgerock.openam.session.service.access.persistence;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;

/**
 * Responsible for reading and storing InternalSessions.
 */
public interface InternalSessionStore {
    /**
     * Gets a session from a given SessionID.
     * @param sessionID the ID of the session to retrieve
     * @exception SessionPersistenceException If the storage operation failed.
     * @return an Internal Session
     */
    InternalSession getBySessionID(SessionID sessionID) throws SessionPersistenceException;

    /**
     * Gets a session from a given session handle.
     * @param sessionHandle the handle of the session to retrieve
     * @exception SessionPersistenceException If the storage operation failed.
     * @return an Internal Session
     */
    InternalSession getByHandle(String sessionHandle) throws SessionPersistenceException;

    /**
     * Gets a restricted session from a given SessionID.
     * @param sessionID the ID of the restricted session to retrieve
     * @exception SessionPersistenceException If the storage operation failed.
     * @return a restricted Internal Session
     */
    InternalSession getByRestrictedID(SessionID sessionID) throws SessionPersistenceException;

    /**
     * Stores the InternalSession in the cache. This will also store any associated references
     * which have been stored on the Session:
     *
     * <ul>
     *   <li>Session ID</li>
     *   <li>Session Handle</li>
     *   <li>Restricted Tokens</li>
     * </ul>
     *
     * @param session Non null InternalSession to store.
     * @exception SessionPersistenceException If the storage operation failed.
     */
    void create(InternalSession session) throws SessionPersistenceException;

    void update(InternalSession session) throws SessionPersistenceException;

    /**
     * Remove the Session from the cache.
     *
     * @param session Non null SessionID.
     * @exception SessionPersistenceException If the storage operation failed.
     * @return The InternalSession that was removed from the cache.
     */
    void remove(InternalSession session) throws SessionPersistenceException;

}
