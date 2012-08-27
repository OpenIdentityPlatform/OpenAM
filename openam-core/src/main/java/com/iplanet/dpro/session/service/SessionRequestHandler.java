/**
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
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */

package com.iplanet.dpro.session.service;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.share.SessionBundle;
import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.dpro.session.share.SessionRequest;
import com.iplanet.dpro.session.share.SessionResponse;
import com.iplanet.services.comm.server.RequestHandler;
import com.iplanet.services.comm.share.Request;
import com.iplanet.services.comm.share.Response;
import com.iplanet.services.comm.share.ResponseSet;
import com.sun.identity.session.util.RestrictedTokenAction;
import com.sun.identity.session.util.RestrictedTokenContext;
import com.sun.identity.session.util.SessionUtils;
import com.sun.identity.shared.encode.CookieUtils;
import com.sun.identity.shared.Constants;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import java.net.URL;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SessionRequestHandler implements RequestHandler {
    private SessionService sessionService = null;

    /*
     * Added this property to block registration of the global notification
     * listener (AddListenerOnAllSessions);
     */
    private static Boolean enableAddListenerOnAllSessions = null;
    private SSOToken clientToken = null;
    
    public SessionRequestHandler() {
        sessionService = SessionService.getSessionService();
    }

    public ResponseSet process(List<Request> requests,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse, ServletContext servletContext) {
        ResponseSet rset = new ResponseSet(SessionService.SESSION_SERVICE);
        
        for (Request req : requests) {
            Response res = processRequest(req, servletRequest, servletResponse);
            rset.addResponse(res);
        }
        
        return rset;
    }

    private Response processRequest(Request req,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        String content = req.getContent();
        SessionRequest sreq = SessionRequest.parseXML(content);
        SessionResponse sres = new SessionResponse(sreq.getRequestID(), sreq.getMethodID());

        try {
            // use remote client IP as default RestrictedToken context
            Object context = SessionUtils.getClientAddress(servletRequest);
            this.clientToken = null;
            String requester = sreq.getRequester();
            
            if (requester != null) {
                try {
                    context = RestrictedTokenContext.unmarshal(requester);
                    
                    if (context instanceof SSOToken) {
                        SSOTokenManager ssoTokenManager = SSOTokenManager.getInstance();
                        SSOToken adminToken = (SSOToken)context;
                        
                        if (!ssoTokenManager.isValidToken(adminToken)) {
                            sres.setException(SessionBundle.getString(
                                    "appTokenInvalid") + requester);
                            return new Response(sres.toXMLString());
                        }
                        
                        this.clientToken = (SSOToken)context;
                    }
                } catch (Exception ex) {
                    if (SessionService.sessionDebug.warningEnabled()) {
                         SessionService.sessionDebug.warning(
                             "SessionRequestHandler.processRequest:"
                             + "app token invalid, sending Session response"
                             +" with Exception");
                     }
                     sres.setException(SessionBundle.getString(
                             "appTokenInvalid") + requester);
                     return new Response(sres.toXMLString());
                }
            }
            
            final HttpServletRequest httpReq = servletRequest;
            final HttpServletResponse httpResp = servletResponse;
            final SessionRequest fsreq = sreq;
            sres = (SessionResponse) RestrictedTokenContext.doUsing(context,
                    new RestrictedTokenAction() {
                        public Object run() throws Exception {
                            return processSessionRequest(fsreq, httpReq,
                                    httpResp);
                        }
                    });
        } catch (Exception ex) {
            SessionService.sessionDebug.error(
                    "SessionRequestHandler encounterd exception", ex);
            sres.setException(ex.getMessage());
        }
        
        return new Response(sres.toXMLString());
    }

    private SessionResponse processSessionRequest(SessionRequest req,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        SessionResponse res = new SessionResponse(req.getRequestID(), req
                .getMethodID());
        SessionID sid = new SessionID(req.getSessionID());
        Session requesterSession = null;

        try {
            /* common processing by groups of methods */
            switch (req.getMethodID()) {
            /*
             * in this group of methods the request is targeting either all
             * LOCAL sessions or a single local session identified by another
             * request parameter sid in this case is only used to authenticate
             * the operation Session pointed by sid is not expected to be local
             * to this server (although it might)
             */
            case SessionRequest.GetValidSessions:
            case SessionRequest.AddSessionListenerOnAllSessions:
            case SessionRequest.GetSessionCount:
                /*
                 * note that the purpose of the following is just to check the
                 * authentication of the caller (which can also be used as a
                 * filter for the operation scope!)
                 */
                requesterSession = Session.getSession(sid);
                /*
                 * also check that sid is not a restricted token
                 */
                if (requesterSession.getProperty(Session.TOKEN_RESTRICTION_PROP) != null) {
                    res.setException(sid + " " + SessionBundle.getString("noPrivilege"));
                    return res;
                }
                
                break;

            /*
             * In this group request is targeting a single session identified by
             * sid which is supposed to be hosted by this server instance sid is
             * used both as an id of a session and to authenticate the operation
             * (performed on own session)
             */
            case SessionRequest.GetSession:
            case SessionRequest.Logout:
            case SessionRequest.AddSessionListener:
            case SessionRequest.SetProperty:
            case SessionRequest.DestroySession:                 
                if (req.getMethodID() == SessionRequest.DestroySession) {
                    /*
                     * note that the purpose of the following is just to check
                     * the authentication of the caller (which can also be used
                     * as a filter for the operation scope!)
                     */                  
                    requesterSession = Session.getSession(sid);            
                    /*
                     * also check that sid is not a restricted token
                     */
                    if (requesterSession.getProperty(
                        Session.TOKEN_RESTRICTION_PROP)!= null) { 
                        res.setException(sid + " " + SessionBundle.getString("noPrivilege"));
                        return res;
                    }
                    sid = new SessionID(req.getDestroySessionID());
                } else if (req.getMethodID() == SessionRequest.SetProperty) {
                    /*
                     * This fix is to avoid clients sneaking in to set
                     * protected properties in server-2 or so through
                     * server-1. Short circuit this operation without
                     * forwrading it further.
                     */
                    try {
                        SessionUtils.checkPermissionToSetProperty(
                                    this.clientToken, req.getPropertyName(),
                                    req.getPropertyValue());
                    } catch (SessionException se) {
                        if (SessionService.sessionDebug.warningEnabled()) {
                            SessionService.sessionDebug.warning(
                                "SessionRequestHandler.processRequest:"
                                + "Client does not have permission to set"
                                + " - property key = " + req.getPropertyName()
                                + " : property value = " + req.getPropertyValue());
                        }

                        res.setException(sid + " " + SessionBundle.getString("noPrivilege"));
                        return res;
                    }
                }
                
                if (!sessionService.isSessionFailoverEnabled()) {
                    // TODO check how this behaves in non-session failover case
                    URL originService = Session.getSessionServiceURL(sid);
                    
                    if (!sessionService.isLocalSessionService(originService)) {                        
                        if (!sessionService.isSiteEnabled()) {
                            String siteID = sid.getExtension(SessionID.SITE_ID);
                            if (siteID != null) {
                                String primaryID = sid.getExtension(SessionID.PRIMARY_ID);
                                String localServerID = sessionService.getLocalServerID();
                                if ( (primaryID != null) && (localServerID != null) )
                                {
                                    if (primaryID.equals(localServerID)) {
                                        throw new SessionException("invalid session id");
                                    }
                                }
                            }
                        } else {
                            return forward(originService, req);
                        }
                    }
                } else {
                    if (SessionService.getUseInternalRequestRouting()) {
                        // first try
                        String hostServerID = sessionService
                                .getCurrentHostServer(sid);

                        if (!sessionService.isLocalServer(hostServerID)) {
                            try {
                                return forward(Session.getSessionServiceURL(hostServerID), req);
                            } catch (SessionException se) {
                                // attempt retry
                                if (!sessionService.checkServerUp(hostServerID)) {
                                    // proceed with failover
                                    String retryHostServerID = sessionService
                                            .getCurrentHostServer(sid);
                                    if (retryHostServerID.equals(hostServerID)) {
                                        throw se;
                                    } else {
                                        // we have a shot at retrying here
                                        // if it is remote, forward it
                                        // otherwise treat it as a case of local
                                        // case
                                        if (!sessionService.isLocalServer(retryHostServerID)) {
                                            return forward(Session.getSessionServiceURL(retryHostServerID), req);
                                        }
                                    }
                                } else {
                                    throw se;
                                }
                            }
                        }
                    } else {
                        // use LB-dependent routing
                        // if session is not found at this instance we check that both OpenSSO session and
                        // HTTP session cookies were enclosed in the request. If they were then LB must have 
                        // routed to the proper server instance and we must treat it as a session recovery
                        // case. If any of the cookies missing or do not match the sid in the message we 
                        // assume that request was misrouted and correct it by forwarding via LB with all 
                        // cookies enclosed
                        String isSessionCookie = 
                                CookieUtils.getCookieValueFromReq(servletRequest, Session.getCookieName());
                        String httpCookie = 
                                CookieUtils.getCookieValueFromReq(servletRequest, SessionService.getHttpSessionTrackingCookieName());

                        if (!sessionService.isSessionPresent(sid) 
                                && (isSessionCookie == null
                                || !isSessionCookie.equals(sid.toString())
                                || httpCookie == null 
                                || !httpCookie.equals(sid.getTail()))) {
                            return forward(Session.getSessionServiceURL(sid), req);
                        }
                    }
                    
                    /*
                     * We determined that this server is the host and the
                     * session must be found(or recovered) locally
                     */

                    /*
                     * if session is not already present locally attempt to
                     * recover session if in failover mode
                     */
                    if (!sessionService.isSessionPresent(sid)) {
                        if (sessionService.recoverSession(sid) == null) {
                            /*
                             * if not in failover mode or recovery was not
                             * successful return an exception
                             */

                            /*
                             * !!!!! IMPORTANT !!!!! DO NOT REMOVE "sid" FROM
                             * EXCEPTIONMESSAGE Logic kludge in legacy Agent 2.0
                             * code will break If it can not find SID value in
                             * the exception message returned by Session
                             * Service. This dependency should be eventually
                             * removed once we migrate customers to a newer
                             * agent code base or switch to a new version of
                             * Session Service interface
                             */
                            res.setException(sid + " " + SessionBundle.getString("sessionNotObtained"));
                            return res;
                        }
                    }
                }

                break;
            default:
                res.setException(sid + " " + SessionBundle.getString("unknownRequestMethod"));
                return res;
            }

            /*
             * request method-specific processing
             */
            switch (req.getMethodID()) {
            case SessionRequest.GetSession:
                res.addSessionInfo(sessionService.getSessionInfo(sid, req.getResetFlag()));
                break;

            case SessionRequest.GetValidSessions:
                String pattern = req.getPattern();
                List<SessionInfo> infos = null;
                int status[] = { 0 };
                infos = sessionService.getValidSessions(requesterSession, pattern, status);
                res.setStatus(status[0]);
                res.setSessionInfo(infos);
                break;

            case SessionRequest.DestroySession:
                sessionService.destroySession(requesterSession, new SessionID(req.getDestroySessionID()));
                break;

            case SessionRequest.Logout:
                sessionService.logout(sid);
                break;

            case SessionRequest.AddSessionListener:
                sessionService.addSessionListener(sid, req.getNotificationURL());
                break;

            case SessionRequest.AddSessionListenerOnAllSessions:
                /**
                 * Cookie Hijacking fix to disable adding of Notification
                 * Listener for ALL the sessions over the network to the server
                 * instance specified by Notification URL This property can be
                 * added and set in the AMConfig.properties file should there be
                 * a need to add Notification Listener to ALL the sessions. The
                 * default value of this property is FALSE
                 */
                if (getEnableAddListenerOnAllSessions()) {
                    sessionService.addSessionListenerOnAllSessions(requesterSession, req.getNotificationURL());
                }
                break;

            case SessionRequest.SetProperty:
                sessionService.setExternalProperty(this.clientToken, sid, 
                        req.getPropertyName(), req.getPropertyValue());
                break;

            case SessionRequest.GetSessionCount:
                String uuid = req.getUUID();
                Object sessions = SessionCount.getSessionsFromLocalServer(uuid);
                
                if (sessions != null) {
                    res.setSessionsForGivenUUID((Map) sessions);
                }
                
                break;

            default:
                res.setException(sid + " " + SessionBundle.getString("unknownRequestMethod"));
                break;
            }
        } catch (SessionException se) {
            res.setException(sid + " " + se.getMessage());
        }
        return res;
    }

    private SessionResponse forward(URL svcurl, SessionRequest sreq)
    throws SessionException {
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
        } catch (Exception ex) {
            throw new SessionException(ex);
        }
    }

    private static boolean getEnableAddListenerOnAllSessions() {
        if (enableAddListenerOnAllSessions == null) {
            enableAddListenerOnAllSessions = Boolean.valueOf(SystemProperties
                    .get(Constants.ENABLE_ADD_LISTENER_ON_ALL_SESSIONS));
        }
        
        return enableAddListenerOnAllSessions.booleanValue();
    }
}
