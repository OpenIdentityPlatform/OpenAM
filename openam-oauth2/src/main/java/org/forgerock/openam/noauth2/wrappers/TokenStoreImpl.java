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

import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.ClientAuthentication;
import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.GrantType;
import org.forgerock.oauth2.core.RefreshToken;
import org.forgerock.oauth2.core.ResourceOwner;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.oauth2.core.CoreToken;
import org.forgerock.openam.oauth2.provider.OAuth2TokenStore;

import javax.inject.Inject;
import java.util.Set;

/**
 * @since 12.0.0
 */
public class TokenStoreImpl implements TokenStore {

    private final OAuth2TokenStore tokenStore;

    @Inject
    public TokenStoreImpl(final OAuth2TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    //TODO pull out realm
    public AccessToken createAccessToken(final GrantType grantType, final String resourceOwnerId,
            final ClientRegistration clientRegistration, final ClientAuthentication clientAuthentication,
            final Set<String> scope, final RefreshToken refreshToken) {

        final String clientId = clientRegistration.getClientId();
        final String realm = ((OpenAMClientAuthentication) clientAuthentication).getRealm(); //TODO fix so can remove casting
        final String refreshTokenId = refreshToken != null ? refreshToken.getTokenId() : null;

        final CoreToken token = tokenStore.createAccessToken("Bearer", scope, realm, resourceOwnerId, clientId, null,
                null, refreshTokenId, grantType.toString());

        return new AccessToken(token);
    }

    public RefreshToken createRefreshToken(final GrantType grantType, final ClientRegistration clientRegistration,
            final ClientAuthentication clientAuthentication, final ResourceOwner resourceOwner,
            final Set<String> scope) {

        final String clientId = clientRegistration.getClientId();
        final String realm = ((OpenAMClientAuthentication) clientAuthentication).getRealm(); //TODO fix so can remove casting

        final CoreToken token = tokenStore.createRefreshToken(scope, realm, resourceOwner.getId(), clientId, null,
                grantType.toString());

        return new RefreshToken(token);
    }
}
