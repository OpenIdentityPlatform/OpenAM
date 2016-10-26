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
 * Copyright 2014-2016 ForgeRock AS.
 */
package com.iplanet.dpro.session.operations.strategies;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.forgerock.openam.dpro.session.InvalidSessionIdException;
import org.forgerock.openam.dpro.session.PartialSession;
import org.forgerock.openam.session.SessionPLLSender;
import org.forgerock.openam.session.SessionServiceURLService;
import org.forgerock.openam.session.service.ServicesClusterMonitorHandler;
import org.forgerock.openam.utils.CrestQuery;
import org.forgerock.openam.utils.IOUtils;
import org.forgerock.util.annotations.VisibleForTesting;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.ClientSdkSessionRequests;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.TokenRestriction;
import com.iplanet.dpro.session.TokenRestrictionFactory;
import com.iplanet.dpro.session.operations.SessionOperations;
import com.iplanet.dpro.session.service.GetHttpSession;
import com.iplanet.dpro.session.service.HttpConnectionFactory;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionServerConfig;
import com.iplanet.dpro.session.share.SessionBundle;
import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.dpro.session.share.SessionRequest;
import com.iplanet.dpro.session.share.SessionResponse;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.SearchResults;
import com.sun.identity.session.util.RestrictedTokenContext;
import com.sun.identity.session.util.SessionUtils;
import com.sun.identity.shared.debug.Debug;

/**
 * Responsible for providing ClientSDK implementations of the SessionOperations. These
 * are all moved from {@link Session}. Importantly they use the SessionRequest PLL
 * mechanism for performing these operations.
 */
public class ClientSdkOperations implements SessionOperations {
    protected static final String INVALID_SESSION_STATE = "invalidSessionState";
    protected static final String UNEXPECTED_SESSION = "unexpectedSession";

    private final Debug debug;
    private final ClientSdkSessionRequests clientSdkSessionRequests;
    private ServicesClusterMonitorHandler servicesClusterMonitorHandler;
    private SessionServiceURLService sessionServiceURLService;
    private SessionServerConfig serverConfig;
    private HttpConnectionFactory httpConnectionFactory;

    public ClientSdkOperations(final Debug sessionDebug,
                               final SessionPLLSender sessionPLLSender,
                               final ServicesClusterMonitorHandler servicesClusterMonitorHandler,
                               final SessionServiceURLService sessionServiceURLService,
                               final SessionServerConfig serverConfig,
                               final HttpConnectionFactory httpConnectionFactory) {
        this(sessionDebug, new ClientSdkSessionRequests(sessionDebug, sessionPLLSender), servicesClusterMonitorHandler,
                sessionServiceURLService, serverConfig, httpConnectionFactory);
    }

    @VisibleForTesting
    ClientSdkOperations(final Debug sessionDebug,
                               final ClientSdkSessionRequests clientSdkSessionRequests,
                               final ServicesClusterMonitorHandler servicesClusterMonitorHandler,
                               final SessionServiceURLService sessionServiceURLService,
                               final SessionServerConfig serverConfig,
                               final HttpConnectionFactory httpConnectionFactory) {
        this.debug = sessionDebug;
        this.clientSdkSessionRequests = clientSdkSessionRequests;
        this.servicesClusterMonitorHandler = servicesClusterMonitorHandler;
        this.sessionServiceURLService = sessionServiceURLService;
        this.serverConfig = serverConfig;
        this.httpConnectionFactory = httpConnectionFactory;
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
        SessionResponse sres = clientSdkSessionRequests.sendRequest(session.getSessionServiceURL(), sreq, session);

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
        clientSdkSessionRequests.sendRequest(session.getSessionServiceURL(), sreq, session);
    }

    @Override
    public Session resolveSession(SessionID sessionID) throws SessionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SearchResults<SessionInfo> getValidSessions(Session session, String pattern) throws SessionException {
        SessionRequest sreq =
                new SessionRequest(SessionRequest.GetValidSessions, session.getSessionID().toString(), false);

        if (pattern != null) {
            sreq.setPattern(pattern);
        }

        URL svcurl = sessionServiceURLService.getSessionServiceURL(session.getSessionID());

        SessionResponse sres = clientSdkSessionRequests.sendRequest(svcurl, sreq, session);
        Set<SessionInfo> infos = new HashSet<>(sres.getSessionInfo());

        return new SearchResults<>(infos.size(), infos, sres.getStatus());
    }

    @Override
    public Collection<PartialSession> getMatchingSessions(CrestQuery crestQuery) {
        throw new UnsupportedOperationException("Querying sessions is currently not supported with the ClientSDK");
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
        clientSdkSessionRequests.sendRequest(session.getSessionServiceURL(), sreq, session);
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
        if (SystemProperties.isServerMode() && InternalSession.isProtectedProperty(name) ) {
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
        clientSdkSessionRequests.sendRequest(session.getSessionServiceURL(), sreq, session);
    }

    @Override
    public SessionInfo getSessionInfo(SessionID sid, boolean reset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addSessionListener(Session session, String url) throws SessionException {
        SessionRequest sreq = new SessionRequest(
                SessionRequest.AddSessionListener,
                session.getSessionID().toString(), false);
        sreq.setNotificationURL(url);
        clientSdkSessionRequests.sendRequest(sessionServiceURLService.getSessionServiceURL(session.getSessionID()), sreq, session);
    }

    @Override
    public boolean checkSessionExists(SessionID sessionId) throws SessionException {
        throw new UnsupportedOperationException("This should not be used in ClientSDK mode");
    }

    @Override
    public String getRestrictedTokenId(SessionID masterSid, TokenRestriction restriction) throws SessionException {

        String token = getRestrictedTokenIdRemotely(
                sessionServiceURLService.getSessionServiceURL(servicesClusterMonitorHandler.getCurrentHostServer(masterSid)), masterSid, restriction);
        if (token == null) {
            // TODO consider one retry attempt
            throw new InvalidSessionIdException(masterSid);
        } else {
            return token;
        }
    }

    @Override
    public String deferenceRestrictedID(Session session, SessionID restrictedID) throws SessionException {

        //first try
        String hostServerID = servicesClusterMonitorHandler.getCurrentHostServer(restrictedID);

        String masterID = deferenceRestrictedIDRemotely(
                session, sessionServiceURLService.getSessionServiceURL(hostServerID), restrictedID);

        if (masterID == null) {
            //TODO consider one retry attempt
            throw new SessionException("unable to get master id remotely " + restrictedID);
        } else {
            return masterID;
        }
    }

    @Override
    public void setExternalProperty(SSOToken clientToken,
                                    SessionID sessionId,
                                    String name,
                                    String value) throws SessionException {
        throw new UnsupportedOperationException();
    }

    // sjf bug 6797573
    private String deferenceRestrictedIDRemotely(Session s, URL hostServerID, SessionID sessionID) {
        DataInputStream in = null;
        DataOutputStream out = null;

        try {
            String query = "?" + GetHttpSession.OP + "=" + GetHttpSession.DEREFERENCE_RESTRICTED_TOKEN_ID;

            URL url = serverConfig.createServerURL(hostServerID, "GetHttpSession" + query);

            HttpURLConnection conn = httpConnectionFactory.createSessionAwareConnection(url, s.getID(), null);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/octet-stream");

            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            DataOutputStream ds = new DataOutputStream(bs);

            ds.writeUTF(sessionID.toString());
            ds.flush();
            ds.close();

            byte[] getRemotePropertyString = bs.toByteArray();

            conn.setRequestProperty("Content-Length",
                    Integer.toString(getRemotePropertyString.length));

            out = new DataOutputStream(conn.getOutputStream());

            out.write(getRemotePropertyString);
            out.close();
            out = null;

            in = new DataInputStream(conn.getInputStream());

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }

            return in.readUTF();

        } catch (Exception ex) {
            debug.error("Failed to dereference the master token remotely", ex);
        } finally {
            IOUtils.closeIfNotNull(in);
            IOUtils.closeIfNotNull(out);
        }

        return null;
    }

    /**
     * This method is used to create restricted token
     *
     * @param owner       server instance URL
     * @param masterSid   SessionID
     * @param restriction restriction
     */
    private String getRestrictedTokenIdRemotely(URL owner, SessionID masterSid, TokenRestriction restriction) {

        DataInputStream in = null;
        DataOutputStream out = null;

        try {
            String query = "?" + GetHttpSession.OP + "=" + GetHttpSession.GET_RESTRICTED_TOKEN_OP;

            URL url = serverConfig.createServerURL(owner, "GetHttpSession" + query);

            HttpURLConnection conn = httpConnectionFactory.createSessionAwareConnection(url, masterSid, null);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/octet-stream");

            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            DataOutputStream ds = new DataOutputStream(bs);

            ds.writeUTF(TokenRestrictionFactory.marshal(restriction));
            ds.flush();
            ds.close();

            byte[] marshalledRestriction = bs.toByteArray();

            conn.setRequestProperty("Content-Length", Integer.toString(marshalledRestriction.length));

            out = new DataOutputStream(conn.getOutputStream());

            out.write(marshalledRestriction);
            out.close();
            out = null;

            in = new DataInputStream(conn.getInputStream());

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }

            return in.readUTF();

        } catch (Exception ex) {
            debug.error("Failed to create restricted token remotely", ex);
        } finally {
            IOUtils.closeIfNotNull(in);
            IOUtils.closeIfNotNull(out);
        }

        return null;
    }

}
