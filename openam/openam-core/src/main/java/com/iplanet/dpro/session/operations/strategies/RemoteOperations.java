/**
 * Copyright 2014 ForgeRock AS.
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
package com.iplanet.dpro.session.operations.strategies;

import com.iplanet.dpro.session.Requests;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.operations.SessionOperations;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionConstants;
import com.iplanet.dpro.session.share.SessionBundle;
import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.dpro.session.share.SessionRequest;
import com.iplanet.dpro.session.share.SessionResponse;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.session.util.RestrictedTokenContext;
import com.sun.identity.session.util.SessionUtils;
import com.sun.identity.shared.debug.Debug;

import javax.inject.Inject;
import javax.inject.Named;
import java.text.MessageFormat;
import java.util.List;

/**
 * Responsible for providing remote implementations of the SessionOperations. These
 * are all moved from {@link Session}. Importantly they use the SessionRequest PLL
 * mechanism for performing these operations.
 */
public class RemoteOperations implements SessionOperations {
    protected static final String INVALID_SESSION_STATE = "invalidSessionState";
    protected static final String UNEXPECTED_SESSION = "unexpectedSession";

    private final Debug debug;
    private final Requests requests;

    @Inject
    public RemoteOperations(@Named(SessionConstants.SESSION_DEBUG) Debug debug, Requests requests) {
        this.debug = debug;
        this.requests = requests;
    }

    /**
     *
     * @param session The Session to update.
     * @param reset If true, then update the last modified timestamp of the Session.
     * @return
     * @throws SessionException
     */
    public SessionInfo refresh(Session session, boolean reset) throws SessionException {
        SessionID sessionID = session.getID();
        if (debug.messageEnabled()) {
            debug.message(MessageFormat.format(
                    "Remote fetch SessionInfo for {0}\n" +
                    "Reset: {1}",
                    sessionID,
                    reset));
        }

        SessionRequest sreq = new SessionRequest(SessionRequest.GetSession, sessionID.toString(), reset);
        SessionResponse sres = requests.sendRequestWithRetry(session.getSessionServiceURL(), sreq, session);

        if (sres.getException() != null) {
            throw new SessionException(SessionBundle.rbName,
                    INVALID_SESSION_STATE, null);
        }

        List<SessionInfo> infos = sres.getSessionInfo();
        if (infos.size() != 1) {
            throw new SessionException(SessionBundle.rbName,
                    UNEXPECTED_SESSION, null);
        }
        return infos.get(0);
    }

    /**
     * Performs a logout operation by making a remote request based
     * on the Sessions service URL.
     *
     * @param session Session to logout.
     */
    public void logout(Session session) throws SessionException {
        if (debug.messageEnabled()) {
            debug.message(MessageFormat.format(
                    "Remote logout {0}",
                    session.getID().toString()));
        }

        SessionRequest sreq = new SessionRequest(SessionRequest.Logout,
                session.getID().toString(), false);
        requests.sendRequestWithRetry(session.getSessionServiceURL(), sreq, session);
    }

    /**
     * Destroys the Session via the Session remote service URL.
     *
     * @param requester {@inheritDoc}
     * @param session {@inheritDoc}
     * @throws SessionException {@inheritDoc}
     */
    public void destroy(Session requester, Session session) throws SessionException {
        if (debug.messageEnabled()) {
            debug.message(MessageFormat.format(
                    "Remote destroy {0}",
                    session));
        }

        SessionRequest sreq = new SessionRequest(SessionRequest.DestroySession, requester.getID().toString(), false);
        sreq.setDestroySessionID(session.getID().toString());
        requests.sendRequestWithRetry(session.getSessionServiceURL(), sreq, session);
    }

    /**
     * Perform a remote setProperty on the Session using the remote Service URL.
     *
     * {@inheritDoc}
     */
    public void setProperty(Session session, String name, String value) throws SessionException {
        if (debug.messageEnabled()) {
            debug.message(MessageFormat.format(
                    "Remote setProperty {0} {1}={2}",
                    session,
                    name,
                    value));
        }

        SessionID sessionID = session.getID();
        SessionRequest sreq = new SessionRequest(
                SessionRequest.SetProperty, sessionID.toString(), false);
        sreq.setPropertyName(name);
        sreq.setPropertyValue(value);
        if ( Session.isServerMode() && InternalSession.isProtectedProperty(name) ) {
            try {
                SSOToken admSSOToken = SessionUtils.getAdminToken();
                sreq.setRequester(RestrictedTokenContext.marshal(admSSOToken));
            } catch (SSOException e) {
                throw new SessionException(e);
            } catch (Exception e) {
                throw new SessionException(e);
            }

            if (debug.messageEnabled()) {
                debug.message("Session.setProperty: "
                        + "added admSSOToken in sreq to set "
                        + "externalProtectedProperty in remote server");
            }
        }
        requests.sendRequestWithRetry(session.getSessionServiceURL(), sreq, session);
    }
}
