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
 * Copyright 2014-2015 ForgeRock AS.
 */

/*
 * Portions Copyrighted 2014 Nomura Research Institute, Ltd
 */

package org.forgerock.openam.oauth2;

import static org.forgerock.oauth2.core.Utils.*;

import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.shared.debug.Debug;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import org.forgerock.oauth2.core.ClientAuthenticator;
import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.ClientRegistrationStore;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.ClientAuthenticationFailedException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.openam.utils.RealmNormaliser;
import org.forgerock.util.Reject;

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
    private final ClientCredentialsReader clientCredentialsReader;

    /**
     * Constructs a new ClientAuthenticatorImpl.
     *
     * @param clientRegistrationStore An instance of the ClientRegistrationStore.
     * @param auditLogger An instance of the OAuth2AuditLogger.
     * @param realmNormaliser An instance of the RealmNormaliser.
     * @param clientCredentialsReader An instance of the ClientCredentialsReader.
     */
    @Inject
    public ClientAuthenticatorImpl(ClientRegistrationStore clientRegistrationStore, OAuth2AuditLogger auditLogger,
            RealmNormaliser realmNormaliser, ClientCredentialsReader clientCredentialsReader) {
        this.clientRegistrationStore = clientRegistrationStore;
        this.auditLogger = auditLogger;
        this.realmNormaliser = realmNormaliser;
        this.clientCredentialsReader = clientCredentialsReader;
    }

    /**
     * {@inheritDoc}
     */
    public ClientRegistration authenticate(OAuth2Request request, String endpoint) throws InvalidClientException,
            InvalidRequestException, ClientAuthenticationFailedException, NotFoundException {

        final ClientCredentials clientCredentials =
                clientCredentialsReader.extractCredentials(request, endpoint);
        Reject.ifTrue(isEmpty(clientCredentials.getClientId()), "Missing parameter, 'client_id'");

        final String realm = realmNormaliser.normalise(request.<String>getParameter(OAuth2Constants.Custom.REALM));

        boolean authenticated = false;
        try {
            final ClientRegistration clientRegistration = clientRegistrationStore.get(clientCredentials.getClientId(),
                    request);
            // Do not need to authenticate public clients
            if (!clientRegistration.isConfidential()) {
                return clientRegistration;
            }

            if (!clientCredentials.isAuthenticated() &&
                    !authenticate(clientCredentials.getClientId(), clientCredentials.getClientSecret(), realm)) {
                logger.error("ClientVerifierImpl::Unable to verify password for: " + clientCredentials.getClientId());
                throw new InvalidClientException("Client authentication failed");
            }

            authenticated = true;

            return clientRegistration;
        } catch (InvalidClientException e) {
            if (clientCredentials.usesBasicAuth()) {
                throw new ClientAuthenticationFailedException("Client authentication failed", "WWW-Authenticate",
                        "Basic realm=\"" + realm + "\"");
            }
            throw e;
        } finally {
            if (auditLogger.isAuditLogEnabled()) {
                if (authenticated) {
                    String[] obs = {clientCredentials.getClientId()};
                    auditLogger.logAccessMessage("AUTHENTICATED_CLIENT", obs, null);
                } else {
                    String[] obs = {clientCredentials.getClientId()};
                    auditLogger.logErrorMessage("FAILED_AUTHENTICATE_CLIENT", obs, null);
                }
            }
        }
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
}
