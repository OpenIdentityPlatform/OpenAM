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
 * Copyright 2012-2016 ForgeRock AS.
 */

/**
 * Portions copyright 2012-2013 ForgeRock AS
 */

package org.forgerock.openam.oauth2.saml2.restlet;

import static java.util.Collections.*;
import static org.forgerock.oauth2.restlet.RestletConstants.*;

import java.util.Map;

import javax.inject.Inject;

import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.AccessTokenService;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.exceptions.InvalidClientAuthZHeaderException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.forgerock.oauth2.restlet.OAuth2RestletException;
import org.forgerock.openam.rest.representations.JacksonRepresentationFactory;
import org.restlet.Request;
import org.restlet.data.ChallengeRequest;
import org.restlet.data.ChallengeScheme;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements a SAML 2.0 Flow. This is an Extension grant.
 *
 * @see <a href="http://tools.ietf.org/html/rfc6749#section-4.5">4.5.  Extension Grants</a>
 */
public class Saml2BearerServerResource extends ServerResource {

    /*
     * 2.1. Using Saml2BearerServerResource Assertions as Authorization Grants
     * 
     * To use a Saml2BearerServerResource Bearer Assertion as an authorization
     * grant, use the following parameter values and encodings.
     * 
     * The value of "grant_type" parameter MUST be
     * "urn:ietf:params:oauth:grant-type:saml2-bearer"
     * 
     * The value of the "assertion" parameter MUST contain a single
     * Saml2BearerServerResource 2.0 Assertion. The Saml2BearerServerResource
     * Assertion XML data MUST be encoded using base64url, where the encoding
     * adheres to the definition in Section 5 of RFC4648 [RFC4648] and where the
     * padding bits are set to zero. To avoid the need for subsequent encoding
     * steps (by "application/ x-www-form-urlencoded"
     * [W3C.REC-html401-19991224], for example), the base64url encoded data
     * SHOULD NOT be line wrapped and pad characters ("=") SHOULD NOT be
     * included.
     */

    /*
     * 2.2. Using Saml2BearerServerResource Assertions for Client
     * Authentication
     * 
     * To use a Saml2BearerServerResource Bearer Assertion for client
     * authentication grant, use the following parameter values and encodings.
     * 
     * 
     * The value of "client_assertion_type" parameter MUST be
     * "urn:ietf:params:oauth:client-assertion-type:saml2-bearer"
     * 
     * The value of the "client_assertion" parameter MUST contain a single
     * Saml2BearerServerResource 2.0 Assertion. The Saml2BearerServerResource
     * Assertion XML data MUST be encoded using base64url, where the encoding
     * adheres to the definition in Section 5 of RFC4648 [RFC4648] and where the
     * padding bits are set to zero. To avoid the need for subsequent encoding
     * steps (by "application/x-www-form-urlencoded" [W3C.REC-html401-19991224],
     * for example), the base64url encoded data SHOULD NOT be line wrapped and
     * pad characters ("=") SHOULD NOT be included.
     */

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");

    private final OAuth2RequestFactory<?, Request> requestFactory;
    private final AccessTokenService accessTokenService;
    private final JacksonRepresentationFactory jacksonRepresentationFactory;

    @Inject
    public Saml2BearerServerResource(final OAuth2RequestFactory<?, Request> requestFactory,
            final AccessTokenService accessTokenService, JacksonRepresentationFactory jacksonRepresentationFactory) {
        this.requestFactory = requestFactory;
        this.accessTokenService = accessTokenService;
        this.jacksonRepresentationFactory = jacksonRepresentationFactory;
    }

    @Post
    public Representation token(final Representation entity) throws OAuth2RestletException {

        final OAuth2Request request = requestFactory.create(getRequest());

        try {
            final AccessToken accessToken = accessTokenService.requestAccessToken(request);
            return jacksonRepresentationFactory.create(accessToken.toMap());
        } catch (InvalidGrantException e) {
            throw new OAuth2RestletException(e.getStatusCode(), e.getError(), "Assertion is invalid.", request.<String>getParameter("redirect_uri"), request.<String>getParameter("state"));
        } catch (InvalidClientAuthZHeaderException e) {
            getResponse().setChallengeRequests(singletonList(
                    new ChallengeRequest(
                            ChallengeScheme.valueOf(SUPPORTED_RESTLET_CHALLENGE_SCHEMES.get(e.getChallengeScheme())),
                            e.getChallengeRealm())));
            throw new OAuth2RestletException(e.getStatusCode(), e.getError(), e.getMessage(), request.<String>getParameter("state"));
        } catch (OAuth2Exception e) {
            throw new OAuth2RestletException(e.getStatusCode(), e.getError(), e.getMessage(), request.<String>getParameter("redirect_uri"), request.<String>getParameter("state"));
        }
    }
}
