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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.oauth2;

import org.forgerock.common.ClientStore;
import org.forgerock.oauth2.core.ClientAuthenticator;
import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.ClientRegistrationStore;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.ClientAuthenticationFailedException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.util.Reject;
import org.restlet.Request;
import org.restlet.data.ChallengeResponse;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;

import static org.forgerock.oauth2.core.Utils.isEmpty;

/**
 * @since 12.0.0
 */
@Singleton
public class ClientAuthenticatorImpl implements ClientAuthenticator {

    private final ClientRegistrationStore clientRegistrationStore;
    private final ClientStore clientStore;

    @Inject
    public ClientAuthenticatorImpl(final ClientRegistrationStore clientRegistrationStore, final ClientStore clientStore) {
        this.clientRegistrationStore = clientRegistrationStore;
        this.clientStore = clientStore;
    }

    public ClientRegistration authenticate(OAuth2Request request) throws InvalidClientException, InvalidRequestException, ClientAuthenticationFailedException {

        final ClientCredentials clientCredentials = extractCredentials(request);
        Reject.ifTrue(isEmpty(clientCredentials.clientId), "Missing parameter, 'client_id'");

        try {
            final ClientRegistration clientRegistration = clientRegistrationStore.get(clientCredentials.clientId, request);
            // Do not need to authenticate public clients
            if (!clientRegistration.isConfidential()) {
                return clientRegistration;
            }

            if (!authenticate(clientCredentials.clientId, clientCredentials.clientSecret)) {
                throw new InvalidClientException("Client authentication failed");
            }

            return clientRegistration;
        } catch (InvalidClientException e) {
            if (clientCredentials.basicAuth) {
                throw new ClientAuthenticationFailedException("Client authentication failed", "WWW-Authenticate",
                        "Basic");
            }
            throw e;
        }
    }

    private boolean authenticate(final String clientId, final char[] clientSecret) throws InvalidClientException {
        if (clientSecret == null) {
            return false;
        }
        return clientStore.get(clientId).getClientSecret().equals(new String(clientSecret));
    }

    private ClientCredentials extractCredentials(final OAuth2Request request) throws InvalidRequestException, InvalidClientException {

        final Request req = request.getRequest();

        String clientId = request.getParameter("client_id");
        String clientSecret = request.getParameter("client_secret");

        if (req.getChallengeResponse() != null && clientId != null) {
            throw new InvalidRequestException("Client authentication failed");
        }

        boolean basicAuth = false;
        if (req.getChallengeResponse() != null) {
            basicAuth = true;
            final ChallengeResponse challengeResponse = req.getChallengeResponse();

            clientId = challengeResponse.getIdentifier();
            clientSecret = "";
            if (challengeResponse.getSecret() != null && challengeResponse.getSecret().length > 0) {
                clientSecret = String.valueOf(req.getChallengeResponse().getSecret());
            }
        }

        if (clientId == null || clientId.isEmpty()) {
            throw new InvalidClientException("Client authentication failed");
        }

        return new ClientCredentials(clientId, clientSecret == null ? null : clientSecret.toCharArray(), basicAuth);
    }

    private static final class ClientCredentials {

        private final String clientId;
        private final char[] clientSecret;
        private final boolean basicAuth;

        /**
         * Constructs a new ClientCredentials instance.
         *
         * @param clientId The client's identifier.
         * @param clientSecret The client's secret.
         * @param basicAuth Whether the Client's credentials where sent using the Basic Auth header.
         */
        private ClientCredentials(final String clientId, final char[] clientSecret, final boolean basicAuth) {
            this.clientId = clientId;
            this.clientSecret = clientSecret;
            this.basicAuth = basicAuth;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ClientCredentials that = (ClientCredentials) o;

            if (basicAuth != that.basicAuth) return false;
            if (!clientId.equals(that.clientId)) return false;
            if (!Arrays.equals(clientSecret, that.clientSecret)) return false;

            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            int result = clientId.hashCode();
            result = 31 * result + Arrays.hashCode(clientSecret);
            result = 31 * result + (basicAuth ? 1 : 0);
            return result;
        }
    }
}
