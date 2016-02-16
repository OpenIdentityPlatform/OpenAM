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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openidconnect.restlet;

import java.util.Locale;
import javax.inject.Inject;

import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.exceptions.InvalidJwtException;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2Jwt;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.forgerock.oauth2.restlet.ExceptionHandler;
import org.forgerock.oauth2.restlet.OAuth2RestletException;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.openidconnect.OpenIdConnectClientRegistration;
import org.forgerock.openidconnect.OpenIdConnectClientRegistrationStore;
import org.forgerock.util.annotations.VisibleForTesting;
import org.restlet.Request;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

/**
 * OpenID Connect id_token validation and claim decoding endpoint. This is a non-standard endpoint that allows a
 * client to pass in an id_token and have it validated and the claims returned in a single call. The id_token is
 * validated by looking up the client id (audience) and realm information to resolve the client registration details,
 * and then obtaining the public/symmetric key information from there (via JWK_URI or other mechanism). The signature
 * is verified and then claims are checked as per the OIDC specification. No attempt is made to check if the token
 * has been revoked.
 * <p>
 * This endpoint is primarily intended to serve as a minimal "Stateless OpenID Connect" in conjunction with a custom
 * claims script that bakes all required profile information into the id token at creation time. For most cases, the
 * standard userinfo endpoint should be preferred.
 */
public class IdTokenInfo extends ServerResource {
    private final OpenIdConnectClientRegistrationStore clientRegistrationStore;
    private final OAuth2RequestFactory<?, Request> requestFactory;
    private final ExceptionHandler exceptionHandler;
    private final SigningManager signingManager = new SigningManager();

    /**
     * Constructs the idtokeninfo endpoint with the given client registration store
     *
     * @param clientRegistrationStore the client registration store for this realm.
     * @param requestFactory the OAuth2 request factory.
     * @param exceptionHandler the exception handler for uncaught exceptions.
     */
    @Inject
    public IdTokenInfo(final OpenIdConnectClientRegistrationStore clientRegistrationStore,
            final OAuth2RequestFactory<?, Request> requestFactory,
            final ExceptionHandler exceptionHandler) {
        this.clientRegistrationStore = clientRegistrationStore;
        this.requestFactory = requestFactory;
        this.exceptionHandler = exceptionHandler;
    }

    /**
     * Validates the OpenID Connect id_token passed in the body of the request and returns the claims specified in
     * the claims query parameter.
     *
     * @param body the body of the request.
     * @return a JSON representation of the claims from the id_token.
     * @throws OAuth2RestletException if an error occurs.
     */
    @Post
    public Representation validateIdToken(Representation body) throws OAuth2RestletException {
        try {
            final OAuth2Request request = requestFactory.create(getRequest());
            final OAuth2Jwt idToken = validateIdToken(request);

            return new JsonRepresentation(filterClaims(idToken, request).build());
        } catch (InvalidClientException e) {
            throw new OAuth2RestletException(Status.BAD_REQUEST.getCode(), e.getError(),
                    "no registered client matches audience of id_token", null);
        } catch (OAuth2Exception e) {
            throw new OAuth2RestletException(e.getStatusCode(), e.getError(), e.getMessage(), null);
        }
    }

    /**
     * Validates the id_token provided in the given request and returns it if valid.
     *
     * @param request the request containing an id_token to validate.
     * @return the validated id_token.
     * @throws OAuth2Exception if the token is not valid or an error occurs.
     */
    @VisibleForTesting
    OAuth2Jwt validateIdToken(OAuth2Request request) throws OAuth2Exception {
        final String jwt = request.getParameter(OAuth2Constants.JWTTokenParams.ID_TOKEN);
        if (StringUtils.isBlank(jwt)) {
            throw new BadRequestException("no id_token in request");
        }
        final OAuth2Jwt idToken;
        try {
            idToken = OAuth2Jwt.create(jwt);
        } catch (InvalidJwtException e) {
            throw new BadRequestException("invalid id_token: " + e.getMessage());
        }

        final String clientId = CollectionUtils.getFirstItem(idToken.getSignedJwt().getClaimsSet().getAudience());
        final String realm = idToken.getSignedJwt().getClaimsSet().get(OAuth2Constants.JWTTokenParams.REALM)
                                    .defaultTo("/")
                                    .asString();
        final OpenIdConnectClientRegistration clientRegistration = clientRegistrationStore.get(clientId,
                new ValidateIdTokenRequest(request, realm));

        if (idToken.isExpired()) {
            throw new BadRequestException("id_token has expired");
        }

        if (!clientRegistration.verifyJwtIdentity(idToken)) {
            throw new BadRequestException("invalid id_token");
        }

        return idToken;
    }

    /**
     * Extracts the claims from the given id_token and filters them according to the {@literal claims} parameter in
     * the request. If no claims parameter is present then all of the claims are returned.
     *
     * @param idToken the id token to extract the claims from.
     * @param request the request.
     * @return the filtered claims from the id_token.
     */
    @VisibleForTesting
    JwtClaimsSet filterClaims(OAuth2Jwt idToken, OAuth2Request request) {
        final String requestedClaims = request.getParameter(OAuth2Constants.Custom.CLAIMS);
        final JwtClaimsSet claims = idToken.getSignedJwt().getClaimsSet();
        if (requestedClaims != null) {
            JwtClaimsSet newClaims = new JwtClaimsSet();
            for (String claimName : requestedClaims.split(",")) {
                if (claims.isDefined(claimName)) {
                    newClaims.setClaim(claimName, claims.getClaim(claimName));
                }
            }
            return newClaims;
        } else {
            return claims;
        }
    }

    /**
     * Handles any exception that is thrown when processing a OAuth2 authorization request.
     *
     * @param throwable The throwable.
     */
    @Override
    protected void doCatch(Throwable throwable) {
        exceptionHandler.handle(throwable, getResponse());
    }

    /**
     * Wrapper OAuth2Request that provides the realm information from the id_token realm claim rather than the request.
     */
    private static class ValidateIdTokenRequest extends OAuth2Request {
        private final OAuth2Request delegate;
        private final String realm;

        ValidateIdTokenRequest(final OAuth2Request delegate, final String realm) {
            this.delegate = delegate;
            this.realm = realm;
        }

        @Override
        public <T> T getRequest() {
            return delegate.getRequest();
        }

        @Override
        public <T> T getParameter(final String name) {
            if (OAuth2Constants.JWTTokenParams.REALM.equals(name)) {
                return (T) realm;
            }
            return delegate.getParameter(name);
        }

        @Override
        public JsonValue getBody() {
            return delegate.getBody();
        }

        @Override
        public Locale getLocale() {
            return delegate.getLocale();
        }
    }
}
