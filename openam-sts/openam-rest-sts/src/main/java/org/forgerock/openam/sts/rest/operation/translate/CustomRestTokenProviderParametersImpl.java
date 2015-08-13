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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.rest.operation.translate;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.sts.TokenTypeId;
import org.forgerock.openam.sts.rest.token.provider.CustomRestTokenProviderParameters;

import java.security.Principal;

/**
 * This is an implementation of the RestTokenProviderParameters specific for custom RestTokenProvider implementations.
 * It exposes the setter methods necessary to set the extra state added to the RestTokenProviderParameters interface
 * to support custom providers.
 */
public class CustomRestTokenProviderParametersImpl implements CustomRestTokenProviderParameters {
    private final JsonValue tokenCreationState;
    private final TokenTypeId inputTokenType;
    private final JsonValue inputToken;
    private String amSessionId;
    private Principal principal;
    private JsonValue additionalState;

    public CustomRestTokenProviderParametersImpl(JsonValue tokenCreationState, TokenTypeId inputTokenType, JsonValue inputToken) {
        this.tokenCreationState = tokenCreationState;
        this.inputTokenType = inputTokenType;
        this.inputToken = inputToken;
    }
    @Override
    public JsonValue getTokenCreationState() {
        return tokenCreationState;
    }

    @Override
    public TokenTypeId getInputTokenType() {
        return inputTokenType;
    }

    @Override
    public JsonValue getInputToken() {
        return inputToken;
    }


    @Override
    public Principal getPrincipal() {
        return principal;
    }

    @Override
    public JsonValue getAdditionalState() {
        return additionalState;
    }

    @Override
    public String getAMSessionIdFromTokenValidation() {
        return amSessionId;
    }

    void setPrincipal(Principal principal) {
        this.principal = principal;
    }

    void setAdditionalState(JsonValue additionalState) {
        this.additionalState = additionalState;
    }

    void setAMSessionIdFromTokenValidation(String amSessionId) {
        this.amSessionId = amSessionId;
    }
}
