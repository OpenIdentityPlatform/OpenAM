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
 * $Id: SessionService.java,v 1.37 2010/02/03 03:52:54 bina Exp $
 *
 * Portions Copyrighted 2010-2016 ForgeRock AS.
 */

package com.iplanet.dpro.session.service;


import static org.forgerock.openam.utils.Time.*;

import com.iplanet.dpro.session.SessionID;
import com.sun.identity.common.HttpURLConnectionManager;
import com.sun.identity.security.EncodeAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.URLEncDec;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.AccessController;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.session.SessionCookies;

/**
 * Responsible for creating HTTP connections that identify themselves, via cookies, as belonging to a session.
 *
 * Session-aware HttpConnection management logic extracted from SessionService class
 * as part of first-pass refactoring to improve SessionService adherence to SRP.
 *
 * @since 13.0.0
 */
/*
 * Further refactoring is warranted.
 */
@Singleton
public class HttpConnectionFactory {

    private final Debug sessionDebug;
    private final SessionServiceConfig serviceConfig;
    private final SessionServerConfig serverConfig;
    private final SessionCookies sessionCookies;

    @Inject
    public HttpConnectionFactory(
            @Named(SessionConstants.SESSION_DEBUG) Debug sessionDebug,
                SessionServiceConfig serviceConfig,
                SessionServerConfig serverConfig, SessionCookies sessionCookies) {

        this.sessionDebug = sessionDebug;
        this.serviceConfig = serviceConfig;
        this.serverConfig = serverConfig;
        this.sessionCookies = sessionCookies;
    }

    /**
     * Helper function used for remote invocation over HTTP It constructs
     * HttpURLConnection using url and adding cookies based on sid and returns
     * it to the caller. In order to complete the invocation caller is supposed
     * to open input stream
     *
     * @param url url
     * @param sid SessionID
     */
    public HttpURLConnection createSessionAwareConnection(URL url, SessionID sid, String extraCookies) throws Exception {
        if (!serviceConfig.isSessionFailoverEnabled()) {
            return null;
        }

        HttpURLConnection connection = null;

        try {
            connection = HttpURLConnectionManager.getConnection(url);

            StringBuilder securityCookieValue = new StringBuilder();
            securityCookieValue.append(serverConfig.getLocalServerURL().toString());
            securityCookieValue.append(Constants.AT);
            securityCookieValue.append(currentTimeMillis());

            String securityCookie = AccessController.doPrivileged(new EncodeAction(securityCookieValue.toString()));

            StringBuilder cookie = new StringBuilder();
            cookie.append(serviceConfig.getSecurityCookieName());
            cookie.append(Constants.EQUALS);
            cookie.append(serviceConfig.isCookieEncodingEnabled() ? URLEncDec.encode(securityCookie) : securityCookie);

            if (extraCookies != null) {
                cookie.append(Constants.SEMI_COLON);
                cookie.append(extraCookies);
            }

            if (sid != null) {
                cookie.append(Constants.SEMI_COLON).append(sessionCookies.getCookieName());
                cookie.append(Constants.EQUALS);
                cookie.append(serviceConfig.isCookieEncodingEnabled() ? URLEncDec.encode(sid.toString()) : sid.toString());

                String httpId = sid.getTail();

                if (httpId != null) {
                    cookie.append(Constants.SEMI_COLON);
                    cookie.append(serviceConfig.getHttpSessionTrackingCookieName());
                    cookie.append(Constants.EQUALS);
                    cookie.append(serviceConfig.isCookieEncodingEnabled() ? URLEncDec.encode(httpId) : httpId);
                }

            }

            if (sessionDebug.messageEnabled()) {
                sessionDebug.message("created cookie value: " + cookie.toString());
            }

            connection.setRequestProperty("Cookie", cookie.toString());
            connection.setRequestMethod("GET");
            connection.setDoInput(true);

        } catch (Exception ex) {
            sessionDebug.message("Failed contacting " + url, ex);
            throw ex;
        }
        return connection;
    }

}
