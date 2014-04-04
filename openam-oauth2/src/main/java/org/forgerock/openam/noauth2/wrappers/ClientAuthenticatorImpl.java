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

package org.forgerock.openam.noauth2.wrappers;

import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.spi.AuthLoginException;
import org.forgerock.oauth2.core.ClientAuthenticator;
import org.forgerock.oauth2.core.ClientCredentials;
import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.ClientRegistrationStore;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.openam.oauth2.OAuth2Utils;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @since 12.0.0
 */
public class ClientAuthenticatorImpl implements ClientAuthenticator {

    private final ClientRegistrationStore clientRegistrationStore;

    @Inject
    public ClientAuthenticatorImpl(final ClientRegistrationStore clientRegistrationStore) {
        this.clientRegistrationStore = clientRegistrationStore;
    }

    @Override
    public ClientRegistration authenticate(final ClientCredentials clientCredentials, final Map<String, Object> context)
            throws InvalidClientException {

        final String realm = (String) context.get("realm");

        final ClientRegistration clientRegistration = clientRegistrationStore.get(clientCredentials.getClientId(),
                context);

        if (!clientRegistration.isConfidential()) {
            return clientRegistration;
        }

        if (!authenticate(clientCredentials.getClientId(), clientCredentials.getClientSecret(), realm)) {
            OAuth2Utils.DEBUG.error("ClientVerifierImpl::Unable to verify password for: " + clientCredentials.getClientId());
            throw new InvalidClientException("Client authentication failed");
        }

        return clientRegistration;
    }

    private boolean authenticate(final String clientId, final char[] clientSecret, final String realm)
            throws InvalidClientException {

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
                lc.logout();
                throw new InvalidClientException();
            }
        } catch (AuthLoginException le) {
            OAuth2Utils.DEBUG.error("ClientVerifierImpl::authContext AuthException", le);
            throw new InvalidClientException(le);
        }
    }
}
