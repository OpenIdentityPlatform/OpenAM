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
 * Copyright 2014-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.oauth2.core;

import static com.sun.identity.shared.Constants.*;
import static org.forgerock.oauth2.core.Utils.*;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import jakarta.servlet.http.HttpServletRequest;

import org.forgerock.oauth2.core.exceptions.ClientAuthenticationFailureFactory;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.openam.oauth2.ClientCredentials;
import org.forgerock.openam.oauth2.ClientCredentialsReader;
import org.forgerock.openam.oauth2.OAuth2AuditLogger;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.utils.RealmNormaliser;
import org.forgerock.util.Reject;
import org.restlet.Request;
import org.restlet.Response;
import org.forgerock.openam.rest.jakarta.servlet.ServletUtils;

import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.debug.Debug;

/**
 * Authenticates OAuth2 clients by extracting the client's identifier and secret from the request.
 *
 * @since 12.0.0
 */
@Singleton
public class ClientAuthenticator {

    private final Debug logger = Debug.getInstance("OAuth2Provider");
    private final ClientRegistrationStore clientRegistrationStore;
    private final OAuth2AuditLogger auditLogger;
    private final RealmNormaliser realmNormaliser;
    private final ClientCredentialsReader clientCredentialsReader;
    private final ClientAuthenticationFailureFactory failureFactory;

    /**
     * Constructs a new ClientAuthenticatorImpl.
     * @param clientRegistrationStore An instance of the ClientRegistrationStore.
     * @param auditLogger An instance of the OAuth2AuditLogger.
     * @param realmNormaliser An instance of the RealmNormaliser.
     * @param clientCredentialsReader An instance of the ClientCredentialsReader.
     * @param failureFactory
     */
    @Inject
    public ClientAuthenticator(ClientRegistrationStore clientRegistrationStore, OAuth2AuditLogger auditLogger,
            RealmNormaliser realmNormaliser, ClientCredentialsReader clientCredentialsReader, ClientAuthenticationFailureFactory failureFactory) {
        this.clientRegistrationStore = clientRegistrationStore;
        this.auditLogger = auditLogger;
        this.realmNormaliser = realmNormaliser;
        this.clientCredentialsReader = clientCredentialsReader;
        this.failureFactory = failureFactory;
    }

    /**
     * Authenticates the client making the OAuth2 request by extracting the client's id and secret from the request
     * and authenticating against the OAuth2 providers client registrations.
     *
     * @param request The OAuth2Request. Must not be {@code null}.
     * @param endpoint The endpoint being authenticated for.
     * @return The client's registration.
     * @throws InvalidClientException If either the request does not contain the client's id or the client fails to be
     *          authenticated.
     * @throws InvalidRequestException If the request is missing any required parameters or is otherwise malformed.
     * @throws NotFoundException If the realm does not have an OAuth 2.0 provider service.
     */
    public ClientRegistration authenticate(OAuth2Request request, String endpoint) throws InvalidClientException,
            InvalidRequestException, NotFoundException {

        final ClientCredentials clientCredentials =
                clientCredentialsReader.extractCredentials(request, endpoint);
        Reject.ifTrue(isEmpty(clientCredentials.getClientId()), "Missing parameter, 'client_id'");

        boolean authenticated = false;
        try {
            final String realm = realmNormaliser.normalise(request.<String>getParameter(OAuth2Constants.Custom.REALM));

            final ClientRegistration clientRegistration = clientRegistrationStore.get(clientCredentials.getClientId(),
                    request);
            // Do not need to authenticate public clients
            if (!clientRegistration.isConfidential()) {
                return clientRegistration;
            }

            if (!clientCredentials.isAuthenticated() &&
                    !authenticate(request, clientCredentials.getClientId(), clientCredentials.getClientSecret(), realm)) {
                logger.error("ClientVerifierImpl::Unable to verify password for: " + clientCredentials.getClientId());
                throw failureFactory.getException(request, "Client authentication failed");
            }

            authenticated = true;

            return clientRegistration;

        } catch (org.forgerock.json.resource.NotFoundException e) {
            throw new NotFoundException(e.getMessage());
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
    private boolean authenticate(OAuth2Request request, String clientId, char[] clientSecret, String realm)
            throws InvalidClientException {
        try {
            AuthContext lc = new AuthContext(realm);
            HttpServletRequest httpRequest = ServletUtils.getRequest(Request.getCurrent());
            httpRequest.setAttribute(ISAuthConstants.NO_SESSION_REQUEST_ATTR, "true");
            lc.login(AuthContext.IndexType.MODULE_INSTANCE, "Application", null, httpRequest,
                    ServletUtils.getResponse(Response.getCurrent()));

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
                    throw failureFactory.getException(request, "Missing requirements");
                }
                lc.submitRequirements(callbacks);
            }

            // validate the password..
            if (lc.getStatus() == AuthContext.Status.SUCCESS) {
                request.<Request>getRequest().getAttributes().put(AM_CTX_ID,
                        lc.getAuthContextLocal().getLoginState().getActivatedSessionTrackingId());
                return true;
            } else {
                throw failureFactory.getException(request, "Client authentication failed");
            }
        } catch (AuthLoginException le) {
            logger.error("ClientVerifierImpl::authContext AuthException", le);
            throw failureFactory.getException(request, "Client authentication failed");
        }
    }
}
