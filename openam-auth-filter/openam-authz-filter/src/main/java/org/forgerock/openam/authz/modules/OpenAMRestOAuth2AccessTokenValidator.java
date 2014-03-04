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

package org.forgerock.openam.authz.modules;

import org.forgerock.authz.modules.oauth2.RestOAuth2AccessTokenValidator;
import org.forgerock.json.fluent.JsonValue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Access Token Validator for validating OAuth2 tokens issued by OpenAM via REST.
 * <br/>
 * This validator requires the configuration given at construction to contain the following entries:
 * <ul>
 *     <li>token-info-endpoint - the URI of OpenAM's tokeninfo endpoint (not including the access_token query
 *     parameter</li>
 *     <li>user-info-endpoint - the URI of OpenAM's userinfo endpoint</li>
 * </ul>
 *
 * @since 12.0.0
 */
public class OpenAMRestOAuth2AccessTokenValidator extends RestOAuth2AccessTokenValidator {

    /**
     * Creates a new instance of the RestOAuth2AccessTokenValidator.
     *
     * @param config The configuration for the validator.
     */
    public OpenAMRestOAuth2AccessTokenValidator(JsonValue config) {
        super(config);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Set<String> getScope(JsonValue tokenInfo) {
        return new HashSet<String>(tokenInfo.get("scope").defaultTo(Collections.emptyList()).asList(String.class));
    }
}
