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
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.token.validator;

import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.HttpURLConnectionWrapper;
import org.forgerock.openam.sts.HttpURLConnectionWrapperFactory;
import org.forgerock.openam.sts.STSPrincipal;
import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.sts.token.UrlConstituentCatenator;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

/**
 * @see org.forgerock.openam.sts.token.validator.PrincipalFromSession
 */
public class PrincipalFromSessionImpl implements PrincipalFromSession {
    private static final String ID = "id";

    private final UrlConstituentCatenator urlConstituentCatenator;
    private final String amDeploymentUrl;
    private final String amJsonRestBase;
    private final String realm;
    private final String amRestIdFromSessionUriElement;
    private final String amSessionCookieName;
    private final String crestVersionUsersService;
    private final HttpURLConnectionWrapperFactory httpURLConnectionWrapperFactory;
    private final Logger logger;

    @Inject
    public PrincipalFromSessionImpl(
            @Named(AMSTSConstants.AM_DEPLOYMENT_URL) String amDeploymentUrl,
            @Named(AMSTSConstants.AM_REST_AUTHN_JSON_ROOT) String jsonRestBase,
            @Named(AMSTSConstants.REST_ID_FROM_SESSION_URI_ELEMENT) String idFromSessionUriElement,
            @Named(AMSTSConstants.AM_SESSION_COOKIE_NAME) String amSessionCookieName,
            @Named(AMSTSConstants.REALM) String realm,
            @Named(AMSTSConstants.CREST_VERSION_USERS_SERVICE) String crestVersionUsersService,
            UrlConstituentCatenator urlConstituentCatenator,
            HttpURLConnectionWrapperFactory httpURLConnectionWrapperFactory,
            Logger logger) {
        this.amDeploymentUrl = amDeploymentUrl;
        this.amJsonRestBase = jsonRestBase;
        this.amRestIdFromSessionUriElement = idFromSessionUriElement;
        this.amSessionCookieName = amSessionCookieName;
        this.realm = realm;
        this.crestVersionUsersService = crestVersionUsersService;
        this.urlConstituentCatenator = urlConstituentCatenator;
        this.httpURLConnectionWrapperFactory = httpURLConnectionWrapperFactory;
        this.logger = logger;
    }
    @Override
    public Principal getPrincipalFromSession(String sessionId) throws TokenValidationException {
        return obtainPrincipalFromSession(constitutePrincipalFromSessionUrl(), sessionId);
    }

    /**
     * Creates the String representing the url at which the principal id from session token functionality can be
     * consumed.
     * @return A String representing the url of OpenAM's Restful principal from session id service
     */
    private String constitutePrincipalFromSessionUrl() {
        StringBuilder sb = new StringBuilder(urlConstituentCatenator.catenateUrlConstituents(amDeploymentUrl, amJsonRestBase));
        if (!AMSTSConstants.ROOT_REALM.equals(realm)) {
            sb = urlConstituentCatenator.catentateUrlConstituent(sb, realm);
        }
        sb = urlConstituentCatenator.catentateUrlConstituent(sb, amRestIdFromSessionUriElement);
        return sb.toString();
    }

    private Principal obtainPrincipalFromSession(String sessionToUsernameUrl, String sessionId) throws TokenValidationException {
        if ((sessionId == null) || sessionId.isEmpty()) {
            throw new TokenValidationException(ResourceException.INTERNAL_ERROR,
                    "the sessionId passed to PrincipalFromSession is null or empty.");
        }
        try {
            Map<String, String> headerMap = new HashMap<String, String>();
            headerMap.put(AMSTSConstants.COOKIE, amSessionCookieName + AMSTSConstants.EQUALS + sessionId);
            headerMap.put(AMSTSConstants.CONTENT_TYPE, AMSTSConstants.APPLICATION_JSON);
            headerMap.put(AMSTSConstants.CREST_VERSION_HEADER_KEY, crestVersionUsersService);
            HttpURLConnectionWrapper.ConnectionResult connectionResult =  httpURLConnectionWrapperFactory
                    .httpURLConnectionWrapper(new URL(sessionToUsernameUrl))
                    .setRequestHeaders(headerMap)
                    .setRequestMethod(AMSTSConstants.POST)
                    .makeInvocation();
            final int responseCode = connectionResult.getStatusCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new TokenValidationException(responseCode, "Non-200 response from posting principal from session request.");
            } else {
                return parsePrincipalFromResponse(connectionResult.getResult());
            }
        } catch (IOException e) {
            throw new TokenValidationException(ResourceException.INTERNAL_ERROR,
                    "Exception caught making principal from session invocation: " + e, e);
        }
    }

    private Principal parsePrincipalFromResponse(String response) throws TokenValidationException {
        JsonValue responseJson;
        try {
            responseJson = JsonValueBuilder.toJsonValue(response);
        } catch (JsonException e) {
            String message = "Exception caught getting the text of the json principal from session response: " + e;
            throw new TokenValidationException(ResourceException.INTERNAL_ERROR, message, e);
        }
        JsonValue principalIdJsonValue = responseJson.get(ID);
        if (!principalIdJsonValue.isString()) {
            String message = "Principal from session response does not contain " + ID + " string entry. The obtained entry: "
                    + principalIdJsonValue.toString() + "; The response: " + responseJson.toString();
            throw new TokenValidationException(ResourceException.INTERNAL_ERROR, message);
        }
        return new STSPrincipal(principalIdJsonValue.asString());
    }
}
