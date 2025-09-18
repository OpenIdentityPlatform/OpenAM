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
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openidconnect;

import static org.forgerock.openam.oauth2.OAuth2Constants.Params.REALM;
import static org.forgerock.openam.utils.CollectionUtils.getFirstItem;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.adapters.TokenAdapter;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.oauth2.IdentityManager;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider for OpenId Connect for managing OpenId Connect sessions.
 *
 * @since 12.0.0
 */
@Singleton
public class OpenIDConnectProvider {

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");
    private final SSOTokenManager tokenManager;
    private final IdentityManager identityManager;
    private final CTSPersistentStore cts;
    private final TokenAdapter<JsonValue> tokenAdapter;

    /**
     * Constructs a new OpenAMOpenIDConnectProvider.
     *
     * @param tokenManager An instance of the SSOTokenManager.
     * @param identityManager An instance of the IdentityManager.
     * @param cts An instance of the CTSPersistentStore.
     * @param tokenAdapter An instance of the TokenAdapter to convert CTS tokens into JsonValue.
     */
    @Inject
    public OpenIDConnectProvider(SSOTokenManager tokenManager, IdentityManager identityManager,
            CTSPersistentStore cts, @Named(OAuth2Constants.CoreTokenParams.OAUTH_TOKEN_ADAPTER)
            TokenAdapter<JsonValue> tokenAdapter) {
        this.tokenManager = tokenManager;
        this.identityManager = identityManager;
        this.cts = cts;
        this.tokenAdapter = tokenAdapter;
    }

    /**
     * Determines whether a user has a valid session.
     *
     * @param userId The user's id.
     * @param request The OAuth2 request.
     * @return {@code true} if the user is valid.
     */
    public boolean isUserValid(String userId, OAuth2Request request) {
        try {
            identityManager.getResourceOwnerIdentity(userId,
                    request.<String>getParameter(REALM));
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    /**
     * Destroys a users session.
     *
     * @param opsId The key id of the id token JWT
     * @throws ServerException If any internal server error occurs.
     */
    public void destroySession(String opsId) throws ServerException {
        try {
            final Token opsToken = cts.read(opsId);

            if (opsToken == null) {
                throw new CoreTokenException("Unable to find id_token");
            }

            JsonValue idTokenUserSessionToken = tokenAdapter.fromToken(opsToken);
            cts.delete(opsId);
            String sessionId = getFirstItem(idTokenUserSessionToken.get(OAuth2Constants.JWTTokenParams.LEGACY_OPS)
                    .asCollection(String.class));

            // for some grant type, there is no OpenAM session associated with a id_token
            if (sessionId != null) {
                final SSOToken token = tokenManager.createSSOToken(sessionId);
                tokenManager.destroyToken(token);
            }
        } catch (CoreTokenException e) {
            logger.warn("Unable to get id_token meta data", e);
            throw new ServerException("Unable to get id_token meta data");
        } catch (Exception e) {
            logger.warn("Unable to get SsoTokenManager", e);
            throw new ServerException("Unable to get SsoTokenManager");
        }
    }
}
