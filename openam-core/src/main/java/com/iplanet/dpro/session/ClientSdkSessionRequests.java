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

package com.iplanet.dpro.session;

import java.net.URL;
import java.security.AccessController;

import org.forgerock.openam.session.SessionPLLSender;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.share.SessionBundle;
import com.iplanet.dpro.session.share.SessionRequest;
import com.iplanet.dpro.session.share.SessionResponse;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.session.util.RestrictedTokenContext;
import com.sun.identity.shared.debug.Debug;

/**
 * Responsible for performing the Session based logic of sending a request.
 *
 * The remote operations performed by this class are driven around a Request/Response
 * model where the generic Request is assigned values which indicate the type of request
 * being made.
 *
 * The response accordingly is generic and needs to be inspected for the required return
 * values from the request.
 *
 */
public class ClientSdkSessionRequests {

    private final SessionPLLSender pllSender;
    private final Debug sessionDebug;

    public ClientSdkSessionRequests(final Debug debug, final SessionPLLSender pllSender) {
//        if (SystemProperties.isServerMode()) {
//            throw new IllegalStateException("Attempted to create ClientSdkSessionRequests in server mode");
//        }
        this.sessionDebug = debug;
        this.pllSender = pllSender;
    }

    /**
     * Sends remote session request without retries.
     *
     * @param svcurl Session Service URL.
     * @param sreq Session Request object.
     * @exception SessionException
     */
    public SessionResponse sendRequest(URL svcurl, SessionRequest sreq, Session session) throws SessionException {
        SessionResponse sres;
        Object context = RestrictedTokenContext.getCurrent();

        SSOToken appSSOToken = null;
        // Client side non-authentication request which does not already have a context
        if (!session.getID().getComingFromAuth() && context == null) {
            appSSOToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
            session.createContext(appSSOToken);
            context = session.getContext();
        }
        
        try {
            if (context != null) {
                sreq.setRequester(RestrictedTokenContext.marshal(context));
            }
            sres = pllSender.sendPLLRequest(svcurl, sreq);
            while (sres.getException() != null) {
                processSessionResponseException(session, sres);
                context = session.getContext();
                if (context != null) {
                    sreq.setRequester(RestrictedTokenContext.marshal(context));
                }
                // send request again
                sres = pllSender.sendPLLRequest(svcurl, sreq);
            }
        } catch (Exception e) {
            throw new SessionException(e);
        }

        return sres;
    }

    /**
     * Handle exception coming back from server in the Sessionresponse
     * @exception SessionException
     * @param sres SessionResponse object holding the exception
     */
    private void processSessionResponseException(Session session, SessionResponse sres) throws SessionException {
        try {
            // Check if this exception was thrown due to Session Time out or not. If so, time out the session.
            String exceptionMessage = sres.getException();

            sessionDebug.message("Session. processSessionResponseException: exception received  from server:{}",
                    sres.getException());

            if (exceptionMessage.contains("SessionTimedOutException")) {
                session.timeout();
            }
            if (exceptionMessage.contains(SessionBundle.getString("appTokenInvalid")))  {
                sessionDebug.message("Requests.processSessionResponseException: AppTokenInvalid = TRUE");

                sessionDebug.message("Requests.processSessionResponseException: Destroying AppToken");

                AdminTokenAction.invalid();
                RestrictedTokenContext.clear();

                sessionDebug.warning("Requests.processSessionResponseException: server responded with app " +
                        "token invalid error, refetching the app sso token");
                SSOToken newAppSSOToken = AccessController.doPrivileged(AdminTokenAction.getInstance());

                sessionDebug.message("Requests.processSessionResponseException: creating New AppToken TokenID = {}",
                        newAppSSOToken);
                session.createContext(newAppSSOToken);
            } else {
                throw new SessionException(sres.getException());
            }
        } catch (Exception ex) {
            throw new SessionException(ex);
        }
    }
}
