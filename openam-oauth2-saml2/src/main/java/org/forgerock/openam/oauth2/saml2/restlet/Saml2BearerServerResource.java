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
 * Copyright 2012-2014 ForgeRock AS.
 */

/**
 * Portions copyright 2012-2013 ForgeRock AS
 */

package org.forgerock.openam.oauth2.saml2.restlet;

import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.AccessTokenRequest;
import org.forgerock.oauth2.core.AccessTokenService;
import org.forgerock.oauth2.core.ClientCredentials;
import org.forgerock.oauth2.core.ContextHandler;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.forgerock.oauth2.reslet.ClientCredentialsExtractor;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.openam.oauth2.saml2.core.Saml2AccessTokenRequest;
import org.forgerock.restlet.ext.oauth2.flow.AbstractFlow;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.ext.servlet.ServletUtils;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;

import static org.forgerock.openam.oauth2.saml2.core.Saml2AccessTokenRequest.createSaml2AccessTokenRequestBuilder;

/**
 * Implements a SAML 2.0 Flow. This is an Extension grant.
 *
 * @see <a href="http://tools.ietf.org/html/rfc6749#section-4.5">4.5.  Extension Grants</a>
 */
public class Saml2BearerServerResource extends AbstractFlow {

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
    private final AccessTokenService accessTokenService;
    private final ClientCredentialsExtractor clientCredentialsExtractor;
    private final ContextHandler contextHandler;

    @Inject
    public Saml2BearerServerResource(final AccessTokenService accessTokenService,
            final ClientCredentialsExtractor clientCredentialsExtractor, final ContextHandler contextHandler) {
        this.accessTokenService = accessTokenService;
        this.clientCredentialsExtractor = clientCredentialsExtractor;
        this.contextHandler = contextHandler;
    }

    @Post
    public Representation token(final Representation entity) {

        final String assertion = getAttribute(OAuth2Constants.SAML20.ASSERTION);
        final String scope = getAttribute("scope");

        final ClientCredentials clientCredentials;
        try {
            clientCredentials = clientCredentialsExtractor.extract(getRequest());
        } catch (InvalidClientException e) {
            throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(getRequest(), e.getMessage());
        } catch (InvalidRequestException e) {
            throw OAuthProblemException.OAuthError.INVALID_CLIENT.handle(getRequest(), e.getMessage());
        }

        try {
            final AccessTokenRequest accessTokenRequest = createSaml2AccessTokenRequestBuilder()
                    .clientCredentials(clientCredentials)
                    .assertion(assertion)
                    .scope(scope)
                    .context(contextHandler.createContext(ServletUtils.getRequest(getRequest())))
                    .build();

            final AccessToken accessToken = accessTokenService.requestAccessToken(accessTokenRequest);

            return new JacksonRepresentation<Map<String, Object>>(accessToken.toMap());

        } catch (IllegalArgumentException e) {
            logger.error("Invalid parameters in request, " + e.getMessage());
            throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(getRequest(), e.getMessage());
        } catch (InvalidGrantException e) {
            throw OAuthProblemException.OAuthError.INVALID_GRANT.handle(getRequest(), "Assertion is invalid.");
        } catch (InvalidClientException e) {
            throw OAuthProblemException.OAuthError.INVALID_CLIENT.handle(getRequest(), e.getMessage());
        } catch (OAuth2Exception e) {
            //CATCH ALL
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(getRequest(), e.getMessage());
        }
    }
}
