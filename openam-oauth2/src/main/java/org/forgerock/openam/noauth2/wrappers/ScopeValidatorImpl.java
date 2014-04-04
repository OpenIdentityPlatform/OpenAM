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
import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.CoreToken;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.ScopeFactory;
import org.forgerock.oauth2.core.ScopeValidator;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @since 12.0.0
 */
public class ScopeValidatorImpl implements ScopeValidator {

    private ScopeFactory scopeFactory;

    @Inject
    public ScopeValidatorImpl(final ScopeFactory scopeFactory) {
        this.scopeFactory = scopeFactory;
    }

    public Set<String> validateAccessTokenScope(ClientRegistration clientRegistration, Set<String> scope, final Map<String, Object> context) {
        return scopeFactory.create(context).scopeRequestedForAccessToken(scope, clientRegistration.getAllowedScopes(),
                clientRegistration.getDefaultScopes());
    }

    public void addAdditionalDataToReturnFromTokenEndpoint(AccessToken accessToken, final Map<String, Object> context) {
        accessToken.add(OAuth2Constants.Custom.SSO_TOKEN_ID, (String) context.get("ssoTokenId"));
        final Map<String, String> data = new HashMap<String, String>();
        for (final Map.Entry<String, Object> entry : accessToken.toMap().entrySet()) {
            if (entry.getValue() instanceof String) {
                data.put(entry.getKey(), (String) entry.getValue());
            }
        }

        final Map<String,Object> extraData = scopeFactory.create(
                Collections.<String, Object>singletonMap("realm", accessToken.getCoreToken().getRealm()))
                .extraDataToReturnForTokenEndpoint(data, accessToken.getCoreToken());

        accessToken.addExtraData(extraData);
    }

    public Set<String> validateAuthorizationScope(ClientRegistration clientRegistration, Set<String> scope) {

        if (scope == null || scope.isEmpty()) {
            return clientRegistration.getDefaultScopes();
        }

        Set<String> scopes = new HashSet<String>(clientRegistration.getAllowedScopes());
        scopes.retainAll(scope);
        return scopes;
    }
                                                                                                 //TODO document that is unmodifiable map
    public Map<String, String> addAdditionalDataToReturnFromAuthorizeEndpoint(Map<String, CoreToken> tokens) {
        return new HashMap<String, String>();
    }

    public Map<String, Object> getUserInfo(final AccessToken token) {
        return scopeFactory.create(Collections.<String, Object>singletonMap("realm", token.getCoreToken().getRealm())).getUserInfo(token.getCoreToken());
    }

    public Set<String> validateRefreshTokenScope(ClientRegistration clientRegistration, Set<String> requestedScope, Set<String> tokenScope, final Map<String, Object> context) {
        return scopeFactory.create(context).scopeRequestedForRefreshToken(requestedScope,
                tokenScope, clientRegistration.getAllowedScopes(), clientRegistration.getDefaultScopes());
    }
}
