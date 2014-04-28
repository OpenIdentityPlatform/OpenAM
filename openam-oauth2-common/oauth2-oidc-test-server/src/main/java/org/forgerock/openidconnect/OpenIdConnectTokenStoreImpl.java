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

package org.forgerock.openidconnect;

import org.forgerock.oauth2.TokenStoreImpl;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.restlet.Request;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.security.PrivateKey;

/**
 * @since 12.0.0
 */
@Singleton
public class OpenIdConnectTokenStoreImpl extends TokenStoreImpl implements OpenIdConnectTokenStore {

    private final OAuth2ProviderSettingsFactory providerSettingsFactory;
    private final OpenIdConnectClientRegistrationStore clientRegistrationStore;

    @Inject
    public OpenIdConnectTokenStoreImpl(final OAuth2ProviderSettingsFactory providerSettingsFactory,
            final OpenIdConnectClientRegistrationStore clientRegistrationStore) {
        super(providerSettingsFactory);
        this.providerSettingsFactory = providerSettingsFactory;
        this.clientRegistrationStore = clientRegistrationStore;
    }

    public OpenIdConnectToken createOpenIDToken(String resourceOwnerId, String clientId, String authorizationParty, String nonce, String ops, OAuth2Request request) throws ServerException, InvalidClientException {

        final OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);
        final OpenIdConnectClientRegistration clientRegistration = clientRegistrationStore.get(clientId, request);
        final PrivateKey privateKey = providerSettings.getServerKeyPair().getPrivate();
        final String algorithm = clientRegistration.getIDTokenSignedResponseAlgorithm();

        final long timeInSeconds = System.currentTimeMillis()/1000;
        final long tokenLifetime = providerSettings.getOpenIdTokenLifetime();
        final long exp = timeInSeconds + tokenLifetime;

        final long iat = timeInSeconds;
        final long ath = timeInSeconds;

        final Request req = request.getRequest();
        final String iss = req.getHostRef().toString() + "/" + req.getResourceRef().getSegments().get(0);

        return new OpenIdConnectToken(privateKey, algorithm, iss, resourceOwnerId, clientId, authorizationParty, exp, iat, ath, nonce, ops);
    }
}
