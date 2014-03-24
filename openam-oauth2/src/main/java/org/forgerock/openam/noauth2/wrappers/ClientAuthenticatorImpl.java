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

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.security.AdminTokenAction;
import org.forgerock.oauth2.core.ClientAuthenticator;
import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.ClientRegistrationStore;
import org.forgerock.oauth2.core.InvalidClientException;
import org.forgerock.openam.oauth2.OAuth2Utils;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @since 12.0.0
 */
public class ClientAuthenticatorImpl implements ClientAuthenticator<OpenAMClientAuthentication> {

    private final ClientRegistrationStore<AMIdentity> clientRegistrationStore;

    @Inject
    public ClientAuthenticatorImpl(final ClientRegistrationStore<AMIdentity> clientRegistrationStore) {
        this.clientRegistrationStore = clientRegistrationStore;
    }

    public ClientRegistration authenticate(final OpenAMClientAuthentication clientAuthentication) throws InvalidClientException {

        final String clientId = clientAuthentication.getClientId();
        String clientSecret = clientAuthentication.getClientSecret();
        if (clientSecret == null) {
            clientSecret = "";
        }
        final String realm = clientAuthentication.getRealm();

        AMIdentity client = getIdentity(clientId, realm);
        final ClientRegistration clientRegistration = clientRegistrationStore.get(client);

        if (!clientRegistration.isConfidential()) {
            return clientRegistration;
        }

        if (!authenticate(clientId, clientSecret.toCharArray(), realm)) {
            OAuth2Utils.DEBUG.error("ClientVerifierImpl::Unable to verify password for: " + clientId);
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

    @SuppressWarnings("unchecked")
    private AMIdentity getIdentity(String uName, String realm) throws OAuthProblemException {
        final SSOToken token = AccessController.doPrivileged(AdminTokenAction.getInstance());
        final AMIdentity theID;

        try {
            final AMIdentityRepository amIdRepo = new AMIdentityRepository(token, realm);

            final IdSearchControl idsc = new IdSearchControl();
            idsc.setRecursive(true);
            idsc.setAllReturnAttributes(true);
            // search for the identity
            Set<AMIdentity> results = Collections.emptySet();
            idsc.setMaxResults(0);
            IdSearchResults searchResults =
                    amIdRepo.searchIdentities(IdType.AGENT, uName, idsc);
            if (searchResults != null) {
                results = searchResults.getSearchResults();
            }

            if (results == null || results.size() != 1) {
                throw OAuthProblemException.OAuthError.INVALID_CLIENT.handle(null, "Client authentication failed");

            }

            theID = results.iterator().next();

            //if the client is deactivated return null
            if (theID.isActive()){
                return theID;
            } else {
                return null;
            }
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("ClientVerifierImpl::Unable to get client AMIdentity: ", e);
            throw OAuthProblemException.OAuthError.INVALID_CLIENT.handle(null, "Client authentication failed");
        }
    }
}
