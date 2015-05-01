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
* Copyright 2015 ForgeRock AS.
*/
package org.forgerock.openam.oauth2;

import static org.forgerock.oauth2.core.OAuth2Constants.JwtProfile.*;
import static org.forgerock.openidconnect.Client.TokenEndpointAuthMethod.*;

import com.sun.identity.shared.debug.Debug;
import java.util.Set;
import javax.inject.Inject;
import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2Jwt;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.openidconnect.Client;
import org.forgerock.openidconnect.OpenIdConnectClientRegistration;
import org.forgerock.openidconnect.OpenIdConnectClientRegistrationStore;
import org.restlet.Request;
import org.restlet.data.ChallengeResponse;

/**
 * Used to extract an OAuth2 client's credentials from its OAuth2 Request.
 * Attempts auth methods in appropriate order, and applies token_endpoint_auth_method
 * if appropriate.
 */
public class ClientCredentialsReader {

    private final Debug logger = Debug.getInstance("OAuth2Provider");
    private final OpenIdConnectClientRegistrationStore clientRegistrationStore;

    @Inject
    public ClientCredentialsReader(OpenIdConnectClientRegistrationStore clientRegistrationStore) {
        this.clientRegistrationStore = clientRegistrationStore;
    }

    /**
     * Extracts the client's credentials from the OAuth2 request.
     *
     * @param request The OAuth2 request.
     * @param endpoint The endpoint this request should be for, or null to disable audience verification.
     * @return The client's credentials.
     * @throws InvalidRequestException If the request contains multiple client credentials.
     * @throws InvalidClientException If the request does not contain the client's id.
     */
    public ClientCredentials extractCredentials(OAuth2Request request, String endpoint) throws InvalidRequestException,
            InvalidClientException, NotFoundException {

        final Request req = request.getRequest();
        boolean basicAuth = false;
        if (req.getChallengeResponse() != null) {
            basicAuth = true;
        }

        final ClientCredentials client;
        Client.TokenEndpointAuthMethod method = CLIENT_SECRET_POST;

        //jwt type first
        if (JWT_PROFILE_CLIENT_ASSERTION_TYPE.equalsIgnoreCase(request.<String>getParameter(CLIENT_ASSERTION_TYPE))) {
            client = verifyJwtBearer(request, basicAuth, endpoint);
            method = PRIVATE_KEY_JWT;
        } else {
            String clientId = request.getParameter(OAuth2Constants.Params.CLIENT_ID);
            String clientSecret = request.getParameter(OAuth2Constants.Params.CLIENT_SECRET);

            if (basicAuth && clientId != null) {
                logger.error("Client (" + clientId + ") using multiple authentication methods");
                throw new InvalidRequestException("Client authentication failed");
            }

            if (req.getChallengeResponse() != null) {
                final ChallengeResponse challengeResponse = req.getChallengeResponse();

                clientId = challengeResponse.getIdentifier();
                clientSecret = "";
                if (challengeResponse.getSecret() != null && challengeResponse.getSecret().length > 0) {
                    clientSecret = String.valueOf(req.getChallengeResponse().getSecret());
                }

                method = CLIENT_SECRET_BASIC;
            }

            if (clientId == null || clientId.isEmpty()) {
                logger.error("Client Id is not set");
                throw new InvalidClientException("Client authentication failed");
            }

            client = new ClientCredentials(clientId, clientSecret == null ? null : clientSecret.toCharArray(), false,
                    basicAuth);
        }

        final OpenIdConnectClientRegistration cr = clientRegistrationStore.get(client.getClientId(), request);
        final Set<String> scopes = cr.getAllowedScopes();

        //if we're accessing the token endpoint, check we're authenticating using the appropriate method
        if (scopes.contains(OAuth2Constants.Params.OPENID)
                && req.getResourceRef().getLastSegment().equals(OAuth2Constants.Params.ACCESS_TOKEN)
                && !cr.getTokenEndpointAuthMethod().equals(method.getType())) {
                    throw new InvalidClientException("Invalid authentication method for accessing this endpoint.");
        }

        return client;
    }

    private ClientCredentials verifyJwtBearer(OAuth2Request request, boolean basicAuth, String endpoint)
            throws InvalidClientException, InvalidRequestException, NotFoundException {

        final OAuth2Jwt jwt = OAuth2Jwt.create(request.<String>getParameter(CLIENT_ASSERTION));

        final ClientRegistration clientRegistration = clientRegistrationStore.get(jwt.getSubject(), request);

        if (!clientRegistration.verifyJwtIdentity(jwt)) {
            throw new InvalidClientException("JWT has expired or is not valid");
        }

        if (basicAuth && jwt.getSubject() != null) {
            logger.error("Client (" + jwt.getSubject() + ") using multiple authentication methods");
            throw new InvalidRequestException("Client authentication failed");
        }

        if (endpoint != null && !jwt.isIntendedForAudience(endpoint)) {
            throw new InvalidClientException("Audience validation failed");
        }

        return new ClientCredentials(jwt.getSubject(), null, true, false);
    }

}