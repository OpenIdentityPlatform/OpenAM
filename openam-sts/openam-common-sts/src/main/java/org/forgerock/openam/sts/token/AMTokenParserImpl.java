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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2013-2015 ForgeRock AS.
 * Portions Copyrighted 2025-2026 3A Systems, LLC.
 */

package org.forgerock.openam.sts.token;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.forgerock.json.JsonException;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.HttpURLConnectionWrapper;
import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.slf4j.Logger;

/**
 * AMTokenParser implementation. Responsible for parsing out the OpenAM session id from all successful authentication
 * requests.
 */
public class AMTokenParserImpl implements AMTokenParser {
    private static final String TOKEN_ID = "tokenId";
    private final Logger logger;
    private final String amSessionCookieName;

    @Inject
    AMTokenParserImpl(Logger logger, @Named(AMSTSConstants.AM_SESSION_COOKIE_NAME) String amSessionCookieName) {
        this.logger = logger;
        this.amSessionCookieName = amSessionCookieName;
    }

    @Override
    public String getSessionFromAuthNResponse(HttpURLConnectionWrapper.ConnectionResult authNResponse)
            throws TokenValidationException {
        JsonValue responseJson;
        try {
            responseJson = JsonValueBuilder.toJsonValue(authNResponse.getResult());
        } catch (JsonException e) {
            String message = "Exception caught getting the text of the json authN response: " + e;
            throw new TokenValidationException(ResourceException.INTERNAL_ERROR, message, e);
        }
        JsonValue sessionIdJsonValue = responseJson.get(TOKEN_ID);
        if (sessionIdJsonValue.isString()) {
            return sessionIdJsonValue.asString();
        }
        /*
        With an HttpOnly session cookie (the default) OpenAM leaves the tokenId out of the body unless
        org.openidentityplatform.openam.httponly.allowTokenInBody is set, and delivers the session only in the cookie.
         */
        String sessionId = getSessionFromCookies(authNResponse);
        if (sessionId == null) {
            String message = "REST authN response contains neither a " + TOKEN_ID + " string entry nor a non-empty "
                    + amSessionCookieName + " cookie. The obtained entry: " + sessionIdJsonValue.toString()
                    + "; The response: " + responseJson.toString();
            throw new TokenValidationException(ResourceException.INTERNAL_ERROR, message);
        }
        return sessionId;
    }

    /*
    Of several session cookies the last non-empty one wins, so a clearing (empty) cookie cannot replace the session.
    A URL-encoded value (com.iplanet.am.cookie.encode=true) is decoded; a raw session id never contains '%'.
     */
    private String getSessionFromCookies(HttpURLConnectionWrapper.ConnectionResult authNResponse)
            throws TokenValidationException {
        String sessionId = null;
        for (String setCookie : authNResponse.getSetCookieHeaders()) {
            String pair = setCookie.split(";", 2)[0];
            int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).trim().equals(amSessionCookieName)) {
                String value = pair.substring(eq + 1).trim();
                if (value.indexOf('%') >= 0) {
                    try {
                        value = URLDecoder.decode(value, "UTF-8");
                    } catch (UnsupportedEncodingException | IllegalArgumentException e) {
                        throw new TokenValidationException(ResourceException.INTERNAL_ERROR,
                                "Could not decode the " + amSessionCookieName + " cookie of the REST authN response", e);
                    }
                }
                if (!value.isEmpty()) {
                    sessionId = value;
                }
            }
        }
        return sessionId;
    }
}
