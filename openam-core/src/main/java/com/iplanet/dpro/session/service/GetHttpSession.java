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
 * $Id: GetHttpSession.java,v 1.5 2008/08/19 19:08:38 veiming Exp $
 *
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 */

package com.iplanet.dpro.session.service;

import static org.forgerock.openam.session.SessionConstants.*;
import static org.forgerock.openam.utils.Time.*;

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.TokenRestriction;
import com.iplanet.dpro.session.TokenRestrictionFactory;
import com.iplanet.services.naming.WebtopNaming;
import com.sun.identity.security.DecodeAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.CookieUtils;
import com.sun.identity.shared.encode.URLEncDec;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.session.SessionCache;
import org.forgerock.openam.utils.IOUtils;

import javax.inject.Named;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.AccessController;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

/**
 * This servlet class is used as a helper to aid SessionService to perform
 * certain session-failover related operations:
 * <ol>
 * <li>Creating a new Http session
 * <li>Recovering an existing Http session in case of server failover
 * <li>Releasing an existing OpenAM session if it is being
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

    private static final long MAX_TIMESTAMP_DIFF = TimeUnit.MINUTES.toMillis(10); // 10 Minutes

    private final Debug sessionDebug;
    private final SessionService sessionService;
    private final SessionServiceConfig serviceConfig;
    private final SessionCache sessionCache;

    public GetHttpSession(SessionService sessionService, @Named(SESSION_DEBUG) Debug debug,
                          SessionServiceConfig serviceConfig, SessionCache sessionCache) {
        this.sessionService = sessionService;
        this.sessionDebug = debug;
        this.serviceConfig = serviceConfig;
        this.sessionCache = sessionCache;

    }

    public GetHttpSession() {
        sessionService = InjectorHolder.getInstance(SessionService.class);
        sessionDebug =  InjectorHolder.getInstance(Key.get(Debug.class, Names.named(SESSION_DEBUG)));
        serviceConfig = InjectorHolder.getInstance(SessionServiceConfig.class);
        sessionCache = InjectorHolder.getInstance(SessionCache.class);
    }


    private boolean validateRequest(HttpServletRequest servletRequest) {
        try {
            String encryptedCookie = CookieUtils.getCookieValueFromReq(
                    servletRequest, serviceConfig.getSecurityCookieName());
            if (encryptedCookie == null) {
                sessionDebug.error("GetHttpSession.validateRequest: no Security Cookie in the request");
                return false;
            }
            String decryptedCookie = AccessController.doPrivileged(new DecodeAction(encryptedCookie));
            StringTokenizer st = new StringTokenizer(decryptedCookie, "@");
            String serverURL = st.nextToken();
            long requestTimeStamp = Long.parseLong(st.nextToken());
            long currentTime = currentTimeMillis();
            if (Math.abs(currentTime - requestTimeStamp) > MAX_TIMESTAMP_DIFF) {
                sessionDebug.error("GetHttpSession.validateRequest: Max time elapsed for the Request");
                return false;
            }
            Set<String> platformServerList = WebtopNaming.getPlatformServerList();

            if (!platformServerList.contains(serverURL)) {
                sessionDebug.error("GetHttpSession.validateRequest: request host :" + serverURL
                        + "was not part of the platformServerList");
            }
            return true;

        } catch (Exception e) {
            sessionDebug.error("GetHttpSession.validateRequest: Exception while validating the request ", e);
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
                if (sessionDebug.messageEnabled()) {
                    sessionDebug.message("GetHttpSession.recover: Old HttpSession is obtained");
                }
                SessionID sid = new SessionID(request);
                if (!sid.isNull()) {
                    sessionService.retrieveSession(sid, httpSession);
                }
            } else {
                sessionDebug.error("GetHttpSession.recover: Old  HttpSession is not obtained");
            }
        } else if (op.equals(SAVE_OP)) {
            HttpSession httpSession = request.getSession(false);
            if (httpSession != null) {
                if (sessionDebug.messageEnabled()) {
                    sessionDebug.message("GetHttpSession.save: HttpSession is obtained");
                }
                SessionID sid = new SessionID(request);
                if (!sid.isNull()) {
                    int status = sessionService.handleSaveSession(sid, httpSession);
                    response.setStatus(status);
                }
            } else {
                sessionDebug.error("GetHttpSession.save: HttpSession is not obtained");
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        } else if (op.equals(CREATE_OP)) {
            HttpSession httpSession = request.getSession(true);
            String domain = request.getParameter(DOMAIN);

            InternalSession is = sessionService.newInternalSession(domain, httpSession, false);

            if (sessionDebug.messageEnabled()) {
                sessionDebug.message("GetHttpSession.create: Created new session=" + is.getID());
            }
            DataOutputStream out = new DataOutputStream(response
                    .getOutputStream());
            out.writeUTF(is.getID().toString());
            out.flush();
            out.close();
        } else if (op.equals(INVALIDATE_OP)) {

            HttpSession httpSession = request.getSession(false);
            if (httpSession != null) {
                if (sessionDebug.messageEnabled()) {
                    sessionDebug.message("GetHttpSession.invalidate: HttpSession is obtained");
                }

                try {
                    httpSession.invalidate();
                } catch (IllegalStateException ise) {
                    if (sessionDebug.messageEnabled()) {
                        sessionDebug.message("Exception:invalidateSession: the web containers session timeout could be "
                                + "shorter than the OpenSSO session timeout", ise);
                    }
                }
            } else {
                if (sessionDebug.warningEnabled()) {
                    sessionDebug.warning("GetHttpSession.invalidate: session is not obtained");
                }
            }

        } else if (op.equals(RELEASE_OP)) {
            SessionID sid = new SessionID(request);
            if (!sid.isNull()) {
                if (sessionDebug.messageEnabled()) {
                    sessionDebug.message("GetHttpSession.release: releasing session=" + sid);
                }

                int status = sessionService.handleReleaseSession(sid);
                response.setStatus(status);
            } else {
                if (sessionDebug.messageEnabled()) {
                    sessionDebug.message("GetHttpSession.release: missing session id");
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
                String token = sessionService.handleGetRestrictedTokenIdRemotely(sid, restriction);

                if (token != null) {
                    if (sessionDebug.messageEnabled()) {
                        sessionDebug.message("GetHttpSession.get_restricted_token: Created new session=" + token);
                    }
                    response.setStatus(HttpServletResponse.SC_OK);
                    out = new DataOutputStream(response.getOutputStream());
                    out.writeUTF(token);
                    out.flush();
                } else {
                    sessionDebug.error("GetHttpSession.get_restricted_token: failed to create token");
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                }
            } catch (Exception ex) {
                sessionDebug.error("GetHttpSession.get_restricted_token: exception occured while create token", ex);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            } finally {
                IOUtils.closeIfNotNull(in);
                IOUtils.closeIfNotNull(out);
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
                    String masterSID = sessionService.deferenceRestrictedID(sessionCache.getSession(sid), restrictedID);

                    response.setStatus(HttpServletResponse.SC_OK);
                    out = new DataOutputStream(response.getOutputStream());
                    out.writeUTF(masterSID);
                    out.flush();

                    if (sessionDebug.messageEnabled()) {
                        sessionDebug.message("GetHttpSession.dereference_restricted_token_id: master sid=" + masterSID);
                    }
                } catch (SessionException se) {
                    sessionDebug.message(
                            "GetHttpSession.dereference_restricted_token_id: unable to find master sid", se);
                    response.setStatus(HttpServletResponse.SC_OK);
                    out = new DataOutputStream(response.getOutputStream());
                    out.writeUTF("ERROR");
                    out.flush();
                }
            } catch (Exception ex) {
                sessionDebug.error(
                    "GetHttpSession.dereference_restricted_token_id: exception occured while finding master sid", ex);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            } finally {
                IOUtils.closeIfNotNull(in);
                IOUtils.closeIfNotNull(out);
            }
        } else {
            sessionDebug.error("GetHttpSession: unknown operation requested");
            response.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doGet(request, response);
    }

}
