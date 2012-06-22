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
 * $Id: GetHttpSession.java,v 1.5 2008/08/19 19:08:38 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */

package com.iplanet.dpro.session.service;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.AccessController;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.TokenRestriction;
import com.iplanet.dpro.session.TokenRestrictionFactory;
import com.iplanet.services.naming.WebtopNaming;
import com.sun.identity.shared.encode.CookieUtils;
import com.sun.identity.security.DecodeAction;
import com.sun.identity.shared.encode.URLEncDec;

/**
 * This servlet class is used as a helper to aid SessionService to perform
 * certain session-failover related operations:
 * <ol>
 * <li>Creating a new Http session
 * <li>Recovering an existing Http session in case of server failover
 * <li>Releasing an existing OpenSSO session if it is being
 * relocated to a different "owner" server
 * 
 * Name GetHttpSession is largely a misnomer, but it was kept to minimize
 * changes to other places of code/configuration This class does minimal amount
 * of marshalling/unmarshalling and calls respective methods in the
 * SessionService. Please see descriptions of methods in SessionService for
 * explanation of logic used.
 * 
 */

public final class GetHttpSession extends HttpServlet {

    public static final String OP = "op";

    public static final String NO_OP = "";

    public static final String CREATE_OP = "create";

    public static final String RECOVER_OP = "recover";

    public static final String SAVE_OP = "save";

    public static final String INVALIDATE_OP = "invalidate";

    public static final String RELEASE_OP = "release";

    public static final String GET_RESTRICTED_TOKEN_OP = "get_restricted_token";

    public static final String DEREFERENCE_RESTRICTED_TOKEN_ID =
                                              "dereference_restricted_token_id";

    public static final String DOMAIN = "domain";

    private static final long MAX_TIMESTAMP_DIFF = 10 * 60 * 1000; // 10
                                                                    // Minutes

    private boolean validateRequest(HttpServletRequest servletRequest) {
        try {
            String encryptedCookie = CookieUtils.getCookieValueFromReq(
                    servletRequest, SessionService.securityCookieName);
            if (encryptedCookie == null) {
                SessionService.sessionDebug
                        .error("GetHttpSession.validateRequest: "
                                + "no Security Cookie in the request");
                return false;
            }
            String decryptedCookie = (String) AccessController
                    .doPrivileged(new DecodeAction(encryptedCookie));
            StringTokenizer st = new StringTokenizer(decryptedCookie, "@");
            String serverURL = st.nextToken();
            long requestTimeStamp = Long.parseLong(st.nextToken());
            long currentTime = System.currentTimeMillis();
            if (Math.abs(currentTime - requestTimeStamp) > MAX_TIMESTAMP_DIFF) {
                SessionService.sessionDebug
                        .error("GetHttpSession.validateRequest: "
                                + "Max time elapsed for the Request");
                return false;
            }
            Vector platformServerList = WebtopNaming.getPlatformServerList();

            if (!platformServerList.contains(serverURL)) {
                SessionService.sessionDebug
                        .error("GetHttpSession.validateRequest: "
                                + "request host :" + serverURL
                                + "was not part of the platformServerList");
            }
            return true;

        } catch (Exception e) {
            SessionService.sessionDebug.error(
                    "GetHttpSession.validateRequest: "
                            + "Exception while validating the request ", e);
            return false;
        }

    }

    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        if (!validateRequest(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String op = request.getParameter(OP);
        if (op.equals(RECOVER_OP)) {
            HttpSession httpSession = request.getSession(false);
            if (httpSession != null) {
                if (SessionService.sessionDebug.messageEnabled()) {
                    SessionService.sessionDebug.message(
                            "GetHttpSession.recover: " +
                            "Old HttpSession is obtained");
                }
                SessionID sid = new SessionID(request);
                if (!sid.isNull()) {
                    SessionService.getSessionService().retrieveSession(sid,
                            httpSession);
                }
            } else {
                SessionService.sessionDebug.error(
                        "GetHttpSession.recover: " +
                        "Old  HttpSession is not obtained");
            }
        } else if (op.equals(SAVE_OP)) {
            HttpSession httpSession = request.getSession(false);
            if (httpSession != null) {
                if (SessionService.sessionDebug.messageEnabled()) {
                    SessionService.sessionDebug.message(
                            "GetHttpSession.save: HttpSession is obtained");
                }
                SessionID sid = new SessionID(request);
                if (!sid.isNull()) {
                    int status = SessionService.getSessionService()
                            .handleSaveSession(sid, httpSession);
                    response.setStatus(status);
                }
            } else {
                SessionService.sessionDebug.error(
                        "GetHttpSession.save: HttpSession is not obtained");
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        } else if (op.equals(CREATE_OP)) {
            HttpSession httpSession = request.getSession(true);
            String domain = request.getParameter(DOMAIN);
            InternalSession is = SessionService.getSessionService()
                    .newInternalSession(domain, httpSession);
            if (SessionService.sessionDebug.messageEnabled()) {
                SessionService.sessionDebug
                        .message("GetHttpSession.create: Created new session="
                                + is.getID());
            }
            DataOutputStream out = new DataOutputStream(response
                    .getOutputStream());
            out.writeUTF(is.getID().toString());
            out.flush();
            out.close();
        } else if (op.equals(INVALIDATE_OP)) {

            HttpSession httpSession = request.getSession(false);
            if (httpSession != null) {
                if (SessionService.sessionDebug.messageEnabled()) {
                    SessionService.sessionDebug.message(
                            "GetHttpSession.invalidate: " +
                            "HttpSession is obtained");
                }

                try {
                    httpSession.invalidate();
                } catch (IllegalStateException ise) {
                    if (SessionService.sessionDebug.messageEnabled()) {
                        SessionService.sessionDebug.message(
                                "Exception:invalidateSession: the web " +
                                "containers session timeout could be " +
                                "shorter than the OpenSSO session " +
                                "timeout", ise);
                    }
                }
            } else {
                if (SessionService.sessionDebug.warningEnabled()) {
                    SessionService.sessionDebug.warning(
                            "GetHttpSession.invalidate: session is " +
                            "not obtained");
                }
            }

        } else if (op.equals(RELEASE_OP)) {
            SessionID sid = new SessionID(request);
            if (!sid.isNull()) {
                if (SessionService.sessionDebug.messageEnabled()) {
                    SessionService.sessionDebug.message(
                            "GetHttpSession.release: releasing session="
                                    + sid);
                }
                int status = SessionService.getSessionService()
                        .handleReleaseSession(sid);
                response.setStatus(status);
            } else {
                if (SessionService.sessionDebug.messageEnabled()) {
                    SessionService.sessionDebug.message(
                            "GetHttpSession.release: missing session id");
                }
            }
        } else if (op.equals(GET_RESTRICTED_TOKEN_OP)) {
            DataInputStream in = null;
            DataOutputStream out = null;
            SessionID sid = new SessionID(request);
            try {
                in = new DataInputStream(request.getInputStream());

                TokenRestriction restriction = TokenRestrictionFactory
                        .unmarshal(in.readUTF());
                String token = SessionService.getSessionService()
                        .handleGetRestrictedTokenIdRemotely(sid, restriction);

                if (token != null) {
                    if (SessionService.sessionDebug.messageEnabled()) {
                        SessionService.sessionDebug.message(
                                "GetHttpSession.get_restricted_token: " +
                                "Created new session="
                                        + token);
                    }
                    response.setStatus(HttpServletResponse.SC_OK);
                    out = new DataOutputStream(response.getOutputStream());
                    out.writeUTF(token);
                    out.flush();
                } else {
                    SessionService.sessionDebug.error(
                            "GetHttpSession.get_restricted_token: " +
                            "failed to create token");
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                }
            } catch (Exception ex) {
                SessionService.sessionDebug.error(
                        "GetHttpSession.get_restricted_token: " +
                        "exception occured while create token",
                                ex);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            } finally {
                SessionService.closeStream(in);
                SessionService.closeStream(out);
            }
        } else if (op.equals(DEREFERENCE_RESTRICTED_TOKEN_ID)) {
            DataInputStream in = null;
            DataOutputStream out = null;

            String cookieValue = CookieUtils.getCookieValueFromReq(
                    request, CookieUtils.getAmCookieName());
            if((cookieValue != null) && (cookieValue.indexOf("%") != -1)) {
                cookieValue = URLEncDec.decode(cookieValue);
            }

            SessionID sid = new SessionID(cookieValue);

            try {
                in = new DataInputStream(request.getInputStream());

                String restrictedID = in.readUTF();

                try {
                    String masterSID = SessionService.getSessionService().deferenceRestrictedID(Session.getSession(sid), restrictedID);

                    response.setStatus(HttpServletResponse.SC_OK);
                    out = new DataOutputStream(response.getOutputStream());
                    out.writeUTF(masterSID);
                    out.flush();

                    if (SessionService.sessionDebug.messageEnabled()) {
                        SessionService.sessionDebug.message(
                            "GetHttpSession.dereference_restricted_token_id: master sid=" + masterSID);
                    }
                } catch (SessionException se) {
                    SessionService.sessionDebug.message(
                            "GetHttpSession.dereference_restricted_token_id: unable to find master sid", se);
                    response.setStatus(HttpServletResponse.SC_OK);
                    out = new DataOutputStream(response.getOutputStream());
                    out.writeUTF("ERROR");
                    out.flush();
                }
            } catch (Exception ex) {
                SessionService.sessionDebug.error(
                    "GetHttpSession.dereference_restricted_token_id: exception occured while finding master sid",
                    ex);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            } finally {
                SessionService.closeStream(in);
                SessionService.closeStream(out);
            }
        } else {
            SessionService.sessionDebug
                    .error("GetHttpSession: unknown operation requested");
            response.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        doGet(request, response);
    }

}
