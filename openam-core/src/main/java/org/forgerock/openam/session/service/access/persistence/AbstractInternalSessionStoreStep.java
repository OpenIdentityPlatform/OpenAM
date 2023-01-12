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
 * An abstract InternalSessionStoreStep will pass through implementation of the InternalSessionStoreStep interfaces.
 *
 * Extend this to quickly make small filter classes without repeating pass through method implementations.
 */
public abstract class AbstractInternalSessionStoreStep implements InternalSessionStoreStep { // public by design

    @Override
    public InternalSession getBySessionID(SessionID sessionID, InternalSessionStore next) throws SessionPersistenceException {
        return next.getBySessionID(sessionID);
    }

    @Override
    public InternalSession getByHandle(String sessionHandle, InternalSessionStore next) throws SessionPersistenceException {
        return next.getByHandle(sessionHandle);
    }

    @Override
    public InternalSession getByRestrictedID(SessionID sessionID, InternalSessionStore next) throws SessionPersistenceException {
        return next.getByRestrictedID(sessionID);
    }

    @Override
    public void create(InternalSession session, InternalSessionStore next) throws SessionPersistenceException {
        next.create(session);
    }

    @Override
    public void update(InternalSession session, InternalSessionStore next) throws SessionPersistenceException {
        next.update(session);
    }

    @Override
    public void remove(InternalSession internalSession, InternalSessionStore next) throws SessionPersistenceException {
        next.remove(internalSession);
    }

}
