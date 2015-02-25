/**
 * Copyright 2013 ForgeRock, Inc.
 *
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
 */
package org.forgerock.openam.forgerockrest.session.query.impl;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.dpro.session.share.SessionRequest;
import com.iplanet.dpro.session.share.SessionResponse;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.session.util.RestrictedTokenContext;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.forgerockrest.session.query.SessionQueryType;

import java.net.URL;
import java.security.AccessController;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Provides an implementation of the SessionQueryType that is suitable for use against Remote OpenAM instances.
 * Also appears suitable against local servers.
 *
 * @author robert.wapshott@forgerock.com
 */
public class RemoteSessionQuery implements SessionQueryType {

    private String serverId;
    private static Debug debug = SessionService.sessionDebug;

    /**
     * Creates an instance which is configured to query the given server.
     *
     * @param serverId  Non null Server Id as defined against the result of calling
     *                  {@link com.iplanet.services.naming.WebtopNaming#getAllServerIDs()}.
     */
    public RemoteSessionQuery(String serverId) {
        this.serverId = serverId;
    }

    /**
     * Generates a SessionRequest and uses this to query the remote server.
     *
     * @return  Non null but possibly empty collection of Sessions. If the server is down, then this will
     *          also return no sessions.
     */
    public Collection<SessionInfo> getAllSessions() {
        List<SessionInfo> sessions = new LinkedList<SessionInfo>();

        try {
            URL svcurl = Session.getSessionServiceURL(serverId);
            SSOToken adminToken = getAdminToken();
            String sid = adminToken.getTokenID().toString();

            SessionRequest sreq = new SessionRequest(SessionRequest.GetValidSessions, sid, false);
            SessionResponse sres = getSessionResponse(svcurl, sreq);

            List<SessionInfo> infoList = sres.getSessionInfo();

            if (debug.messageEnabled()) {
                debug.message(MessageFormat.format(
                        "Query returned {0} SessionInfos.",
                        infoList.size()));
            }

            sessions.addAll(infoList);
        } catch (SessionException e) {
            debug.warning("Failed to fetch sessions from " + serverId, e);
        }

        return sessions;
    }


    /**
     * Performs the Session Request and waits for the response.
     *
     * @param svcurl URL Non null to perform the request against.
     *
     * @param sreq Non null Session Request.
     *
     * @return A SessionResponse containing the response from the remote server.
     *
     * @throws SessionException
     */
    private SessionResponse getSessionResponse(URL svcurl, SessionRequest sreq) throws SessionException {
        try {
            Object context = RestrictedTokenContext.getCurrent();
            if (context != null) {
                sreq.setRequester(RestrictedTokenContext.marshal(context));
            }

            SessionResponse sres = Session.sendPLLRequest(svcurl, sreq);
            if (sres.getException() != null) {
                throw new SessionException(sres.getException());
            }
            return sres;
        } catch (SessionException se) {
            throw se;
        } catch (Exception e) {
            throw new SessionException(e);
        }
    }

    /**
     * @return Non null AdminToken.
     */
    private SSOToken getAdminToken() {
        return AccessController.doPrivileged(AdminTokenAction.getInstance());
    }
}
