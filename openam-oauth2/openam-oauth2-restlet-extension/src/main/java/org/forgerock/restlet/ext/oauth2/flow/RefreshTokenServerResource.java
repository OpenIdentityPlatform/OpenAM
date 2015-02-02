/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012-2015 ForgeRock AS.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of company]"
 */

package org.forgerock.restlet.ext.oauth2.flow;

import java.util.*;

import com.sun.identity.shared.OAuth2Constants;
import org.forgerock.openam.oauth2.model.CoreToken;
import org.forgerock.openam.oauth2.model.SessionClient;
import org.forgerock.openam.oauth2.model.SessionClientImpl;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;

/**
 * Implements the Refresh Token Flow
 *
 * @see <a href="http://tools.ietf.org/html/rfc6749#section-6">6. Refreshing an Access Token</a>
 */
public class RefreshTokenServerResource extends AbstractFlow {

    @Post("form:json")
    public Representation represent(Representation entity) {
        /*
         * o require client authentication for confidential clients or for any
         * client that was issued client credentials (or with other
         * authentication requirements), o authenticate the client if client
         * authentication is included and ensure the refresh token was issued to
         * the authenticated client, and o validate the refresh token.
         */

        client = getAuthenticatedClient();
        String refresh_token =
                OAuth2Utils.getRequestParameter(getRequest(), OAuth2Constants.Params.REFRESH_TOKEN,
                        String.class);
        // Find Token
        CoreToken refreshToken = getTokenStore().readRefreshToken(refresh_token);

        SessionClient refreshTokenClient = new SessionClientImpl(refreshToken.getClientID(),
                refreshToken.getRedirectURI());

        if (null == refreshToken) {
            OAuth2Utils.DEBUG.error("Refresh token does not exist for id: " + refresh_token);
            throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(getRequest(),
                    "RefreshToken does not exist");
        } else if (!refreshTokenClient.getClientId().equalsIgnoreCase(client.getClient().getClientId())) {
            OAuth2Utils.DEBUG.error("Refresh Token was issued to a different client id: " + refreshTokenClient.getClientId());
            throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(getRequest(),
                    "Token was issued to a different client");
        } else {
            if (refreshToken.isExpired()) {
                OAuth2Utils.DEBUG.warning("Refresh Token is expired for id: " + refresh_token);
                throw OAuthProblemException.OAuthError.EXPIRED_TOKEN.handle(getRequest());
            }

            // Get the requested scope
            String scope_before =
                    OAuth2Utils
                            .getRequestParameter(getRequest(), OAuth2Constants.Params.SCOPE, String.class);

            Set<String> granted_after = null;
            // Get the granted scope
            if (null != refreshToken.getScope()) {
                granted_after = new TreeSet<String>(refreshToken.getScope());
            } else {
                granted_after = new TreeSet<String>();
            }

            // Validate the granted scope
            Set<String> checkedScope = executeRefreshTokenScopePlugin(scope_before, granted_after);

            // Generate Token
            CoreToken token = createAccessToken(refreshToken, checkedScope);
            Map<String, Object> response = token.convertToMap();

            if (checkIfRefreshTokenIsRequiredOnRefreshingToken(getRequest())) {
                response.put(OAuth2Constants.Params.REFRESH_TOKEN, token.getRefreshToken());
            }

            //execute post token creation pre return scope plugin for extra return data.
            Map<String, String> data = new HashMap<String, String>();
            response.putAll(executeExtraDataScopePlugin(data, token));

            if (checkedScope != null && !checkedScope.isEmpty()) {
                response.put(OAuth2Constants.Params.SCOPE, OAuth2Utils.join(checkedScope,
                        OAuth2Utils.getScopeDelimiter(getContext())));
            }

            return new JacksonRepresentation<Map>(response);
        }
    }

    @Override
    protected String[] getRequiredParameters() {
        return new String[]{OAuth2Constants.Params.GRANT_TYPE, OAuth2Constants.Params.REFRESH_TOKEN};
    }

    /**
     * Creates a new refresh token with the same scope as the previous refresh token.
     *
     * @param refreshToken The current refresh token.
     * @return A new refresh token.
     */
    private CoreToken createRefreshToken(CoreToken refreshToken) {
        return getTokenStore().createRefreshToken(refreshToken.getScope(), OAuth2Utils.getRealm(getRequest()),
                refreshToken.getUserID(), refreshToken.getClientID(), refreshToken.getRedirectURI(),
                refreshToken.getGrantType());
    }

    /**
     * Creates a new access token for the given refresh token and checked scope.
     * <br/>
     * If refresh tokens are configured to be sent the given refresh token will be replaced and the old one will be
     * deleted from the token store.
     *
     * @param refreshToken The given refresh token.
     * @param checkedScope The checked scope.
     * @return A new access token.
     * @throws OAuthProblemException If there is any problem issuing the refresh token or access token.
     */
    protected CoreToken createAccessToken(CoreToken refreshToken, Set<String> checkedScope) {
        if (checkIfRefreshTokenIsRequired(getRequest())) {
            final String oldRefreshTokenId = refreshToken.getTokenID();
            refreshToken = createRefreshToken(refreshToken);

            getTokenStore().deleteRefreshToken(oldRefreshTokenId);
        }

        return getTokenStore().createAccessToken(client.getClient().getAccessTokenType(),
                checkedScope, OAuth2Utils.getRealm(getRequest()), refreshToken.getUserID(),
                refreshToken.getClientID(), refreshToken.getRedirectURI(), null, refreshToken.getTokenID(),
                getGrantType());
    }
}
