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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.oauth2.core;

import static com.sun.identity.shared.DateUtils.stringToDate;

import javax.inject.Inject;
import java.text.ParseException;
import java.util.Set;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.utils.StringUtils;

/**
 *
 */
public class GrantTypeAccessTokenGenerator {

    private final TokenStore tokenStore;
    private static final Debug logger = Debug.getInstance("OAuth2Provider");

    @Inject
    public GrantTypeAccessTokenGenerator(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    public AccessToken generateAccessToken(OAuth2ProviderSettings providerSettings, String grantType, String clientId,
            String resourceOwnerId, String redirectUri, Set<String> scope, String validatedClaims,
            String authorizationCode, String nonce, OAuth2Request request)
            throws ServerException, NotFoundException {
        //retrieve end user's original authenticated time from session
        long authTime = 0;
        AuthorizationCode authCode = request.getToken(AuthorizationCode.class);
        DeviceCode deviceCode = request.getToken(DeviceCode.class);
        if (authCode != null) {
            String sessionId = authCode.getSessionId();
            if (StringUtils.isNotBlank(sessionId)) {
                try {
                    final SSOTokenManager ssoTokenManager = SSOTokenManager.getInstance();
                    final SSOToken token = ssoTokenManager.createSSOToken(sessionId);
                    authTime = stringToDate(token.getProperty(ISAuthConstants.AUTH_INSTANT)).getTime();
                } catch (SSOException | ParseException e) {
                    logger.error("Error retrieving session from AuthorizationCode", e);
                }
            }
        } else if (deviceCode != null) {
            try {
                authTime = stringToDate(deviceCode.getStringProperty(ISAuthConstants.AUTH_INSTANT)).getTime();
            } catch (ParseException e) {
                logger.error("Error retrieving session from DeviceCode", e);
            }
        }

        RefreshToken refreshToken = null;
        if (providerSettings.issueRefreshTokens()) {
            refreshToken = tokenStore.createRefreshToken(grantType, clientId,
                    resourceOwnerId, redirectUri, scope, request, validatedClaims, authTime);
        }

        AccessToken accessToken = tokenStore.createAccessToken(grantType, OAuth2Constants.Bearer.BEARER,
                authorizationCode, resourceOwnerId, clientId, redirectUri, scope, refreshToken, nonce, validatedClaims,
                request);

        if (refreshToken != null) {
            accessToken.addExtraData(OAuth2Constants.Params.REFRESH_TOKEN, refreshToken.toString());
        }

        return accessToken;
    }
}
