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

/*
 * Portions Copyrighted 2014 Nomura Research Institute, Ltd
 */

package org.forgerock.openam.oauth2;

import static org.forgerock.oauth2.core.OAuth2Constants.JwtProfile.CLIENT_ASSERTION;
import static org.forgerock.oauth2.core.OAuth2Constants.JwtProfile.CLIENT_ASSERTION_TYPE;
import static org.forgerock.oauth2.core.OAuth2Constants.JwtProfile.JWT_PROFILE_CLIENT_ASSERTION_TYPE;
import static org.forgerock.oauth2.core.Utils.isEmpty;

import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.oauth2.core.ClientAuthenticator;
import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.ClientRegistrationStore;
import org.forgerock.oauth2.core.OAuth2Jwt;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.ClientAuthenticationFailedException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.util.Reject;
import org.restlet.Request;
import org.restlet.data.ChallengeResponse;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Authenticates OAuth2 clients by extracting the client's identifier and secret from the request.
 *
 * @since 12.0.0
 */
@Singleton
public class ClientAuthenticatorImpl implements ClientAuthenticator {

    private final Debug logger = Debug.getInstance("OAuth2Provider");
    private final ClientRegistrationStore clientRegistrationStore;
    private final OAuth2AuditLogger auditLogger;
    private final RealmNormaliser realmNormaliser;
    private final OAuth2ProviderSettingsFactory providerSettingsFactory;

    /**
     * Constructs a new ClientAuthenticatorImpl.
     *
     * @param clientRegistrationStore An instance of the ClientRegistrationStore.
     * @param auditLogger An instance of the OAuth2AuditLogger.
     * @param realmNormaliser An instance of the RealmNormaliser.
     * @param providerSettingsFactory An instance of the OAuth2ProviderSettingsFactory.
     */
    @Inject
    public ClientAuthenticatorImpl(ClientRegistrationStore clientRegistrationStore, OAuth2AuditLogger auditLogger,
            RealmNormaliser realmNormaliser, OAuth2ProviderSettingsFactory providerSettingsFactory) {
        this.clientRegistrationStore = clientRegistrationStore;
        this.auditLogger = auditLogger;
        this.realmNormaliser = realmNormaliser;
        this.providerSettingsFactory = providerSettingsFactory;
    }

    /**
     * {@inheritDoc}
     */
    public ClientRegistration authenticate(OAuth2Request request) throws InvalidClientException,
            InvalidRequestException, ClientAuthenticationFailedException {

        final ClientCredentials clientCredentials = extractCredentials(request);
        Reject.ifTrue(isEmpty(clientCredentials.clientId), "Missing parameter, 'client_id'");

        final String realm = realmNormaliser.normalise(request.<String>getParameter("realm"));

        boolean authenticated = false;
        try {
            final ClientRegistration clientRegistration = clientRegistrationStore.get(clientCredentials.clientId,
                    request);
            // Do not need to authenticate public clients
            if (!clientRegistration.isConfidential()) {
                return clientRegistration;
            }

            if (!clientCredentials.isAuthenticated &&
                    !authenticate(clientCredentials.clientId, clientCredentials.clientSecret, realm)) {
                logger.error("ClientVerifierImpl::Unable to verify password for: " + clientCredentials.clientId);
                throw new InvalidClientException("Client authentication failed");
            }

            authenticated = true;

            return clientRegistration;
        } catch (InvalidClientException e) {
            if (clientCredentials.basicAuth) {
                throw new ClientAuthenticationFailedException("Client authentication failed", "WWW-Authenticate",
                        "Basic realm=\"" + realm + "\"");
            }
            throw e;
        } finally {
            if (auditLogger.isAuditLogEnabled()) {
                if (authenticated) {
                    String[] obs = {clientCredentials.clientId};
                    auditLogger.logAccessMessage("AUTHENTICATED_CLIENT", obs, null);
                } else {
                    String[] obs = {clientCredentials.clientId};
                    auditLogger.logErrorMessage("FAILED_AUTHENTICATE_CLIENT", obs, null);
                }
            }
        }
    }

    /**
     * Extracts the client's credentials from the OAuth2 request.
     *
     * @param request The OAuth2 request.
     * @return The client's credentials
     * @throws InvalidRequestException If the request contains multiple client credentials.
     * @throws InvalidClientException If the request does not contain the client's id.
     */
    private ClientCredentials extractCredentials(OAuth2Request request) throws InvalidRequestException,
            InvalidClientException {

        final Request req = request.getRequest();
        boolean basicAuth = false;
        if (req.getChallengeResponse() != null) {
            basicAuth = true;
        }

        if (JWT_PROFILE_CLIENT_ASSERTION_TYPE.equalsIgnoreCase(request.<String>getParameter(CLIENT_ASSERTION_TYPE))) {
            return verifyJwtBearer(request, basicAuth);
        }

        String clientId = request.getParameter("client_id");
        String clientSecret = request.getParameter("client_secret");

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
        }

        if (clientId == null || clientId.isEmpty()) {
            logger.error("Client Id is not set");
            throw new InvalidClientException("Client authentication failed");
        }

        return new ClientCredentials(clientId, clientSecret == null ? null : clientSecret.toCharArray(), false,
                basicAuth);
    }

    private ClientCredentials verifyJwtBearer(OAuth2Request request, boolean basicAuth) throws InvalidClientException,
            InvalidRequestException {

        OAuth2Jwt jwt = OAuth2Jwt.create(request.<String>getParameter(CLIENT_ASSERTION));

        ClientRegistration clientRegistration = clientRegistrationStore.get(jwt.getSubject(), request);

        OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);
        SigningHandler signingHandler = clientRegistration.getClientJwtSigningHandler();

        if (!jwt.isValid(signingHandler)) {
            throw new InvalidClientException("JWT is has expired or is not valid");
        }

        if (basicAuth && jwt.getSubject() != null) {
            logger.error("Client (" + jwt.getSubject() + ") using multiple authentication methods");
            throw new InvalidRequestException("Client authentication failed");
        }

        if (!jwt.isIntendedForAudience(providerSettings.getTokenEndpoint())) {
            throw new InvalidClientException("Audience validation failed");
        }

        return new ClientCredentials(jwt.getSubject(), null, true, false);
    }

    /**
     * Perform the authentication of the client using the specified client credentials.
     *
     * @param clientId The client's id.
     * @param clientSecret The client's secret.
     * @param realm The realm the client exists in.
     * @return {@code true} if the client was authenticated successfully.
     * @throws InvalidClientException If the authentication configured for the client is not completed by the
     *          specified client credentials.
     */
    private boolean authenticate(String clientId, char[] clientSecret, String realm) throws InvalidClientException {

        try {
            AuthContext lc = new AuthContext(realm);
            lc.login(AuthContext.IndexType.MODULE_INSTANCE, "Application");
            while (lc.hasMoreRequirements()) {
                Callback[] callbacks = lc.getRequirements();
                List<Callback> missing = new ArrayList<Callback>();
                // loop through the requires setting the needs..
                for (final Callback callback : callbacks) {
                    if (callback instanceof NameCallback) {
                        NameCallback nc = (NameCallback) callback;
                        nc.setName(clientId);
                    } else if (callback instanceof PasswordCallback) {
                        PasswordCallback pc = (PasswordCallback) callback;
                        pc.setPassword(clientSecret);
                    } else {
                        missing.add(callback);
                    }
                }
                // there's missing requirements not filled by this
                if (missing.size() > 0) {
                    lc.logout();
                    throw new InvalidClientException("Missing requirements");
                }
                lc.submitRequirements(callbacks);
            }

            // validate the password..
            if (lc.getStatus() == AuthContext.Status.SUCCESS) {
                lc.logout();
                return true;
            } else {
                throw new InvalidClientException("Client authentication failed");
            }
        } catch (AuthLoginException le) {
            logger.error("ClientVerifierImpl::authContext AuthException", le);
            throw new InvalidClientException("Client authentication failed");
        }
    }

    /**
     * Models the client's credentials
     *
     * @since 12.0.0
     */
    private static final class ClientCredentials {

        private final String clientId;
        private final char[] clientSecret;
        private final boolean isAuthenticated;
        private final boolean basicAuth;

        /**
         * Constructs a new ClientCredentials instance.
         *
         * @param clientId The client's identifier.
         * @param clientSecret The client's secret.
         * @param isAuthenticated If the process of getting the client credentials has authenticated the client. i.e.
         *                        Jwt assertion.
         * @param basicAuth Whether the Client's credentials where sent using the Basic Auth header.
         */
        private ClientCredentials(final String clientId, final char[] clientSecret, final boolean isAuthenticated,
                final boolean basicAuth) {
            this.clientId = clientId;
            this.clientSecret = clientSecret;
            this.isAuthenticated = isAuthenticated;
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
