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

package org.forgerock.oauth2.core;

import java.util.Map;
import java.util.Set;

/**
 * Interface for performing validation on the requested scope at both the OAuth2 authorize and token endpoints.
 *
 * @since 12.0.0
 */
public interface ScopeValidator {

    /**
     * Validates at the 'token' endpoint that the scope requested is valid and allowed.
     *
     * @param clientRegistration The client's registration.
     * @param scope The requested scope.
     * @param context A {@code Map<String, Object>} containing OAuth2 Provider implementation specific context
     *                information.
     * @return A {@code Set<String>} of the validated and allowed scopes.
     */
    Set<String> validateAccessTokenScope(final ClientRegistration clientRegistration, final Set<String> scope,
            final Map<String, Object> context);

    /**
     * Extension point to allow additional data to be added to the access token before it is sent back to the client.
     *
     * @param accessToken The access token.
     * @param context A {@code Map<String, Object>} containing OAuth2 Provider implementation specific context
     *                information.
     */
    void addAdditionalDataToReturnFromTokenEndpoint(final AccessToken accessToken, final Map<String, Object> context);

    /**
     * Validates at the 'authorize' endpoint that the scope requested is valid and allowed.
     *
     * @param clientRegistration The client's registration.
     * @param scope The requested scope.
     * @return A {@code Set<String>} of the validated and allowed scopes.
     */
    Set<String> validateAuthorizationScope(final ClientRegistration clientRegistration, final Set<String> scope);

    /**
     * Extension point to allow additional data to be returned so that it is added to the response from the 'authorize'
     * endpoint.
     *
     * @param tokens The tokens that will be returned.
     * @return A {@code Map<String, String>} of additional data to be returned from the 'authorize' endpoint.
     */
    Map<String, String> addAdditionalDataToReturnFromAuthorizeEndpoint(final Map<String, CoreToken> tokens);

    /**
     * Gets resource owner's, for whom the access token was issued on behalf of, information.
     *
     * @param token The access token.
     * @return A {@code Map<String, Object>} of the resource owner's information.
     */
    Map<String, Object> getUserInfo(final AccessToken token);

    /**
     * Validates that the refresh token request's requested scope requested is valid and allowed.
     *
     * @param clientRegistration The client's registration.
     * @param requestedScope The requested scope.
     * @param tokenScope The scope on the refresh token.
     * @param context A {@code Map<String, Object>} containing OAuth2 Provider implementation specific context
     *                information.
     * @return A {@code Set<String>} of the validated and allowed scopes.
     */
    Set<String> validateRefreshTokenScope(final ClientRegistration clientRegistration, final Set<String> requestedScope,
            final Set<String> tokenScope, final Map<String, Object> context);
}
