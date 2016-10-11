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

package org.forgerock.openam.session.service.caching;

import java.util.Collection;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;

/**
 * Responsible for caching and providing access to {@link com.iplanet.dpro.session.service.InternalSession} objects.
 *
 * This cache will provide access to the sessions via a number of concepts:
 *
 * <ul>
 *   <li>SessionID of the InternalSession</li>
 *   <li>Session handle of the InternalSession</li>
 *   <li>Any of the restricted SessionIDs of the InternalSession</li>
 * </ul>
 */
public interface InternalSessionCache {

    /**
     * Gets a session from a given SessionID.
     * @param sessionID the ID of the session to retrieve
     * @return an Internal Session
     */
    InternalSession getBySessionID(SessionID sessionID);

    /**
     * Gets a session from a given session handle.
     * @param sessionHandle the handle of the session to retrieve
     * @return an Internal Session
     */
    InternalSession getByHandle(String sessionHandle);

    /**
     * Gets a restricted session from a given SessionID.
     * @param sessionID the ID of the restricted session to retrieve
     * @return a restricted Internal Session
     */
    InternalSession getByRestrictedID(SessionID sessionID);

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
     */
    void put(InternalSession session);

    /**
     * Remove the Session from the cache.
     *
     * @param sessionID Non null SessionID.
     *
     * @return The InternalSession that was removed from the cache.
     */
    InternalSession remove(SessionID sessionID);

    /**
     * Currently used for stats gathering about the cache.
     * @return Current number of sessions stored in the cache.
     */
    int size();

    /**
     * @return <tt>true</tt> if this cache is empty.
     */
    boolean isEmpty();

    /**
     * Currently used by getValidSessions.
     * @return Unmodifiable collection of all Sessions that are stored in the cache.
     */
    Collection<InternalSession> getAllSessions();
}
