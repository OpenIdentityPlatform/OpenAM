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
 * Copyright 2015 ForgeRock AS.
 */

package com.iplanet.dpro.session.operations.strategies;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.operations.SessionOperations;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.dpro.session.share.SessionInfo;
import org.forgerock.openam.session.blacklist.SessionBlacklist;
import org.forgerock.openam.sso.providers.stateless.StatelessSessionFactory;

import javax.inject.Inject;

/**
 * Handles client-side sessions.
 *
 * @since 13.0.0
 */
public class StatelessOperations implements SessionOperations {
    private final SessionOperations localOperations;
    private final SessionService sessionService;
    private final StatelessSessionFactory statelessSessionFactory;
    private final SessionBlacklist sessionBlacklist;

    @Inject
    public StatelessOperations(final LocalOperations localOperations,
                               final SessionService sessionService,
                               final StatelessSessionFactory statelessSessionFactory,
                               final SessionBlacklist sessionBlacklist) {
        this.localOperations = localOperations;
        this.sessionService = sessionService;
        this.statelessSessionFactory = statelessSessionFactory;
        this.sessionBlacklist = sessionBlacklist;
    }

    @Override
    public SessionInfo refresh(final Session session, final boolean reset) throws SessionException {
        return statelessSessionFactory.getSessionInfo(session.getID());
    }

    @Override
    public void logout(final Session session) throws SessionException {
        sessionBlacklist.blacklist(session);
    }

    @Override
    public void destroy(final Session requester, final Session session) throws SessionException {
        sessionService.checkPermissionToDestroySession(requester, session.getID());
        sessionBlacklist.blacklist(session);
    }

    @Override
    public void setProperty(final Session session, final String name, final String value) throws SessionException {
        localOperations.setProperty(session, name, value);
    }

}
