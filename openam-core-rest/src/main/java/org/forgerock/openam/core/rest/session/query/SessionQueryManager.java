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
 * Copyright 2013-2015 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.core.rest.session.query;

import com.iplanet.dpro.session.share.SessionInfo;
import com.sun.identity.shared.debug.Debug;

import jakarta.inject.Inject;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Provides the ability to query a collection of OpenAM servers for Session information. Uses the
 * SessionQueryFactory to determine the most appropriate mechanism for performing the query and handles any
 * complexity around querying Sessions.
 *
 * This manager should easily be expanded to support new functions like 'Session Count' or 'Get Sessions for User'.
 *
 * @author robert.wapshott@forgerock.com
 */
public class SessionQueryManager {

    private static Debug debug = Debug.getInstance("frRest");

    private SessionQueryFactory queryFactory;

    /**
     * Intialise the SessionQueryManager and provide the OpenAM server ids that it should apply to.
     *
     * @param queryFactory Non null instance.
     */
    @Inject
    public SessionQueryManager(SessionQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    /**
     * Query all servers allocated to this SessionQueryManager for their Sessions.
     *
     * @param serverIds One or more server id's. Typically this value can be generated using
     *                  {@link com.iplanet.services.naming.WebtopNaming#getAllServerIDs()} which will provide all
     *                  server id's known to OpenAM.
     *
     * @return Returns all sessions across all servers.
     */
    public Collection<SessionInfo> getAllSessions(Collection<String> serverIds) {
        // impl note, this could be a Map of Server -> Sessions

        List<SessionInfo> sessions = new LinkedList<SessionInfo>();

        for (String server : serverIds) {
            SessionQueryType queryType = queryFactory.getSessionQueryType(server);

            Collection<SessionInfo> queriedSessions = queryType.getAllSessions();

            if (debug.messageEnabled()) {
                debug.message(MessageFormat.format(
                        "SessionQueryManager#getAllSessions() :: Queried {0} from: {1}",
                        queriedSessions.size(),
                        server));
            }

            sessions.addAll(queriedSessions);
        }

        return sessions;
    }
}
