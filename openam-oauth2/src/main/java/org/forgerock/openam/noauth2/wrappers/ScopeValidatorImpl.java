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
import org.forgerock.oauth2.core.ScopeValidator;
import org.forgerock.openam.oauth2.provider.Scope;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @since 12.0.0
 */
public class ScopeValidatorImpl implements ScopeValidator {

    private Scope scopeValidator;

    @Inject
    public ScopeValidatorImpl(final Scope scopeValidator) {
        this.scopeValidator = scopeValidator;
    }

    public Set<String> validateAccessTokenScope(ClientRegistration clientRegistration, Set<String> scope) {
        return scopeValidator.scopeRequestedForAccessToken(scope, clientRegistration.getAllowedScopes(),
                clientRegistration.getDefaultScopes());
    }

    public void addAdditionDataToReturnFromTokenEndpoint(AccessToken accessToken) {

        final Map<String,Object> extraData = scopeValidator.extraDataToReturnForTokenEndpoint(
                new HashMap<String, String>(), accessToken.getCoreToken());

        accessToken.addExtraData(extraData);
    }
}
