/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: SessionRequestHandler.java,v 1.9 2009/04/02 04:11:44 ericow Exp $
 *
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 * Portions Copyrighted 2025 3A Systems LLC.
 */
package com.iplanet.dpro.session.service;

import static org.forgerock.openam.audit.AuditConstants.Component.*;
import static org.forgerock.openam.session.SessionConstants.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.dpro.session.InvalidSessionIdException;
import org.forgerock.openam.session.SessionCache;
import org.forgerock.openam.session.SessionPLLSender;
import org.forgerock.openam.session.service.access.SessionQueryManager;
import org.forgerock.openam.sso.providers.stateless.StatelessSessionManager;

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.share.SessionBundle;
import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.dpro.session.share.SessionRequest;
import com.iplanet.dpro.session.share.SessionResponse;
import com.iplanet.services.comm.server.PLLAuditor;
import com.iplanet.services.comm.server.RequestHandler;
import com.iplanet.services.comm.share.Request;
import com.iplanet.services.comm.share.Response;
import com.iplanet.services.comm.share.ResponseSet;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.common.SearchResults;
import com.sun.identity.session.util.RestrictedTokenAction;
import com.sun.identity.session.util.RestrictedTokenContext;
import com.sun.identity.session.util.SessionUtils;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;

/**
 * Responsible for processing a PLL request and routing it to the appropriate handler which will respond to the caller
 * the results of the operation.
 *
 * The operations available from this handler split into two broad categories:
 *
 * In the first group, the request is targeting either all LOCAL sessions or a single local session identified by another
 * request parameter. The session ID in this case is only used to authenticate the operation. That session is not
 * expected to be local to this server (although it might). These operations are:
 * <ul>
 *   <li>GetValidSessions</li>
 *   <li>GetSessionCount</li>
 * </ul>
 *
 * In the second group, the request is targeting a single session identified by a session ID, which is supposed to be
 * hosted by this server instance. The session ID is used both as an id for the target session and to authenticate the
 * operation (i.e. operations are performed on the callers own session). The operations in this group are:
 * <ul>
 *   <li>GetSession</li>
 *   <li>Logout</li>
 *   <li>AddSessionListener</li>
 *   <li>SetProperty</li>
 *   <li>DestroySession</li>
 * </ul>
 */
public class SessionRequestHandler implements RequestHandler {

    private final SessionService sessionService;
    private final Debug sessionDebug;
    private final StatelessSessionManager statelessSessionManager;
    private final SessionQueryManager sessionQueryManager;

    private SSOToken clientToken = null;

    private static final SessionCache sessionCache = InjectorHolder.getInstance(SessionCache.class);
    private static final SessionPLLSender sessionPLLSender = InjectorHolder.getInstance(SessionPLLSender.class);

    public SessionRequestHandler() {
        sessionService = InjectorHolder.getInstance(SessionService.class);
        sessionDebug =  InjectorHolder.getInstance(Key.get(Debug.class, Names.named(SESSION_DEBUG)));
        statelessSessionManager = InjectorHolder.getInstance(StatelessSessionManager.class);
        sessionQueryManager = InjectorHolder.getInstance(SessionQueryManager.class);
    }

    /**
     * Understands how to resolve a Token based on its SessionID.
     *
     * Stateless Sessions by their very nature do not need to be stored in memory, and so
     * can be resolved in a different way to Stateful Sessions.
     *
     * @param sessionID Non null Session ID.
     *
     * @return Null if no matching Session could be found, otherwise a non null
     * Session instance.
     *
     * @throws SessionException If there was an error resolving the Session.
     */
    private Session resolveSession(SessionID sessionID) throws SessionException {
        if (statelessSessionManager.containsJwt(sessionID)) {
            return statelessSessionManager.generate(sessionID);
        }
        return sessionCache.getSession(sessionID);
    }

    @Override
    public ResponseSet process(PLLAuditor auditor,
                               List<Request> requests,
                               HttpServletRequest servletRequest,
                               HttpServletResponse servletResponse,
                               ServletContext servletContext) {
        ResponseSet rset = new ResponseSet(SessionService.SESSION_SERVICE);

        auditor.setComponent(SESSION);
        for (Request req : requests) {
            Response res = processRequest(auditor, req, servletRequest);
            rset.addResponse(res);
        }

        return rset;
    }

    private Response processRequest(
            final PLLAuditor auditor,
            final Request req,
            final HttpServletRequest servletRequest) {

        final SessionRequest sreq = SessionRequest.parseXML(req.getContent());
        auditor.setMethod(sreq.getMethodName());
        SessionResponse sres = new SessionResponse(sreq.getRequestID(), sreq.getMethodID());

        Object context;
        try {
            // use remote client IP as default RestrictedToken context
            context = SessionUtils.getClientAddress(servletRequest);
            this.clientToken = null;
        } catch (Exception ex) {
            sessionDebug.error("SessionRequestHandler encountered exception", ex);
            sres.setException(ex.getMessage());
            return auditedExceptionResponse(auditor, sres);
        }

        String requester = sreq.getRequester();
        if (requester != null) {
            try {
                context = RestrictedTokenContext.unmarshal(requester);

                if (context instanceof SSOToken) {
                    SSOTokenManager ssoTokenManager = SSOTokenManager.getInstance();
                    SSOToken adminToken = (SSOToken)context;

                    if (!ssoTokenManager.isValidToken(adminToken)) {
                        sres.setException(SessionBundle.getString("appTokenInvalid") + requester);
                        return auditedExceptionResponse(auditor, sres);
                    }

                    this.clientToken = (SSOToken)context;
                }
            } catch (Exception ex) {
                if (sessionDebug.warningEnabled()) {
                    sessionDebug.warning(
                            "SessionRequestHandler.processRequest:"
                                    + "app token invalid, sending Session response"
                                    +" with Exception");
                }
                sres.setException(SessionBundle.getString("appTokenInvalid") + requester);
                return auditedExceptionResponse(auditor, sres);
            }
        }

        try {
            sres = (SessionResponse) RestrictedTokenContext.doUsing(context,
                    new RestrictedTokenAction() {
                        public Object run() throws Exception {
                            try {
                                return processSessionRequest(auditor, sreq);
                            } catch (SessionException se) {
                                sessionDebug.message("processSessionRequest caught exception: {}", se.getMessage(), se);
                                return handleException(sreq, new SessionID(sreq.getSessionID()), se.getMessage());
                            } catch (SessionRequestException se) {
                                sessionDebug.message("processSessionRequest caught exception: {}", se.getResponseMessage(), se);
                                return handleException(sreq, se.getSid(), se.getResponseMessage());
                            }
                        }
                    });
        } catch (Exception ex) {
            sessionDebug.error("SessionRequestHandler encountered exception", ex);
            sres.setException(ex.getMessage());
        }

        if (sres.getException() == null) {
            auditor.auditAccessSuccess();
        } else {
            auditor.auditAccessFailure(sres.getException());
        }

        return new Response(sres.toXMLString());
    }

    private Response auditedExceptionResponse(PLLAuditor auditor, SessionResponse sres) {
        auditor.auditAccessAttempt();
        auditor.auditAccessFailure(sres.getException());
        return new Response(sres.toXMLString());
    }

    private SessionResponse processSessionRequest(PLLAuditor auditor, SessionRequest req) throws SessionException,
            SessionRequestException {
        SessionID sid = new SessionID(req.getSessionID());

        Session requesterSession = null;

        try {
            requesterSession = resolveSession(sid);
            auditAccessAttempt(auditor, requesterSession);
        } catch (SessionException se) {
            // Log the access attempt without session properties, then continue.
            auditor.auditAccessAttempt();
            throw se;
        }

        verifyValidRequest(req, requesterSession);
        return processMethod(req, requesterSession);
    }

    private void verifyRequestingSessionIsNotRestrictedToken(Session requesterSession)
            throws SessionException, SessionRequestException {
        if (requesterSession.getProperty(TOKEN_RESTRICTION_PROP) != null) {
            throw new SessionRequestException(requesterSession.getSessionID(), SessionBundle.getString("noPrivilege"));
        }
    }

    private void verifyValidRequest(SessionRequest req, Session requesterSession) throws SessionException,
            SessionRequestException {
        SessionID targetSid = requesterSession.getSessionID();
        if (req.getMethodID() == SessionRequest.DestroySession) {
            targetSid = new SessionID(req.getDestroySessionID());
            verifyRequestingSessionIsNotRestrictedToken(requesterSession);
        } else if (req.getMethodID() == SessionRequest.SetProperty) {
            try {
                SessionUtils.checkPermissionToSetProperty(
                        this.clientToken, req.getPropertyName(),
                        req.getPropertyValue());
            } catch (SessionException se) {
                if (sessionDebug.warningEnabled()) {
                    sessionDebug.warning("SessionRequestHandler.processRequest: Client does not have permission to set"
                                    + " - property key = " + req.getPropertyName()
                                    + " : property value = " + req.getPropertyValue());
                }
                throw new SessionRequestException(requesterSession.getSessionID(), SessionBundle.getString("noPrivilege"));
            }
        }

        switch (req.getMethodID()) {
            case SessionRequest.GetValidSessions:
            case SessionRequest.GetSessionCount:
                verifyRequestingSessionIsNotRestrictedToken(requesterSession);
                break;

            case SessionRequest.GetSession:
            case SessionRequest.Logout:
            case SessionRequest.AddSessionListener:
            case SessionRequest.SetProperty:
            case SessionRequest.DestroySession:
                verifyTargetSessionExists(targetSid);
                break;
            default:
                throw new SessionRequestException(requesterSession.getSessionID(), SessionBundle.getString("unknownRequestMethod"));
        }
    }

    /**
     *  Verify that this server is the correct host for the session and the session can be found(or recovered) locally.
     *  This function will become much simpler with removal of home servers, or possibly no longer be required.
     */
    private void verifyTargetSessionExists(SessionID sid) throws SessionException, SessionRequestException {
        if (!sessionService.checkSessionExists(sid)) {
            throw new SessionRequestException(sid, SessionBundle.getString("sessionNotObtained"));
        }
    }

    /**
     * Request method-specific processing
     */
    private SessionResponse processMethod(SessionRequest req, Session requesterSession) throws SessionException {
        SessionResponse res = new SessionResponse(req.getRequestID(), req.getMethodID());

        switch (req.getMethodID()) {
            case SessionRequest.GetSession:
                try {
                    if (statelessSessionManager.containsJwt(requesterSession.getSessionID())) {
                        // We need to validate the session before creating the sessioninfo to ensure that the
                        // stateless session hasn't timed out yet, and hasn't  been blacklisted either.
                        SSOTokenManager tokenManager = SSOTokenManager.getInstance();
                        final SSOToken statelessToken = tokenManager.createSSOToken(req.getSessionID());
                        if (!tokenManager.isValidToken(statelessToken)) {
                            throw new InvalidSessionIdException(req.getSessionID());
                        }
                    }
                    res.addSessionInfo(sessionService.getSessionInfo(requesterSession.getSessionID(), req.getResetFlag()));
                } catch (SSOException ssoe) {
                    return handleException(req, requesterSession.getSessionID(), SessionBundle.getString("invalidSessionID"));
                }
                break;

            case SessionRequest.GetValidSessions:
                String pattern = req.getPattern();
                SearchResults<SessionInfo> infoSearchResults = sessionService.getValidSessions(requesterSession, pattern);
                res.setStatus(infoSearchResults.getErrorCode());
                List<SessionInfo> sessionInfo = new ArrayList<>();
                sessionInfo.addAll(infoSearchResults.getSearchResults());
                res.setSessionInfo(sessionInfo);
                break;

            case SessionRequest.DestroySession:
                sessionService.destroySession(requesterSession, new SessionID(req.getDestroySessionID()));
                break;

            case SessionRequest.Logout:
                sessionService.logout(requesterSession);
                break;

            case SessionRequest.AddSessionListener:
                sessionService.addSessionListener(requesterSession, req.getNotificationURL());
                break;

            case SessionRequest.SetProperty:
                sessionService.setExternalProperty(this.clientToken, requesterSession.getSessionID(), req.getPropertyName(), req.getPropertyValue());
                break;

            case SessionRequest.GetSessionCount:
                String uuid = req.getUUID();
                Map sessions =  sessionQueryManager.getAllSessionsByUUID(uuid);
                if (sessions != null) {
                    res.setSessionsForGivenUUID(sessions);
                }

                break;

            default:
                return handleException(req, requesterSession.getSessionID(), SessionBundle.getString("unknownRequestMethod"));
        }
        return res;
    }

    private void auditAccessAttempt(PLLAuditor auditor, Session session) {
        try {
            auditor.setUserId(session.getClientID());
            auditor.setTrackingId(session.getProperty(Constants.AM_CTX_ID));
            auditor.setRealm(session.getProperty(Constants.ORGANIZATION));
        } catch (SessionException ignored) {
            // Don't audit with session information.
        }
        auditor.auditAccessAttempt();
    }

    /**
     * !!!!! IMPORTANT !!!!! DO NOT REMOVE "sid" FROM
     * EXCEPTIONMESSAGE Logic kludge in legacy Agent 2.0
     * code will break If it can not find SID value in
     * the exception message returned by Session
     * Service. This dependency should be eventually
     * removed once we migrate customers to a newer
     * agent code base or switch to a new version of
     * Session Service interface
     */
    private SessionResponse handleException(SessionRequest req, SessionID sid, String error) {
        SessionResponse response = new SessionResponse(req.getRequestID(), req.getMethodID());
        response.setException(sid + " " + error);
        return response;
    }

    private class SessionRequestException extends Exception {
        private final SessionID sid;
        private final String responseMessage;

        public SessionRequestException(SessionID sid, String responseMessage) {

            this.sid = sid;
            this.responseMessage = responseMessage;
        }

        public SessionID getSid() {
            return sid;
        }

        public String getResponseMessage() {
            return responseMessage;
        }
    }
}
