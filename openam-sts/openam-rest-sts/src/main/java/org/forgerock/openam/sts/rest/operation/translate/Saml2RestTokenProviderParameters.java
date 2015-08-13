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
import org.forgerock.openam.sts.rest.token.provider.RestTokenProviderParameters;
import org.forgerock.openam.sts.rest.token.provider.saml.Saml2TokenCreationState;

/**
 * An implementation of the RestTokenProviderParameters to support the creation of Saml2 tokens.
 */
public class Saml2RestTokenProviderParameters implements RestTokenProviderParameters<Saml2TokenCreationState> {
    private final Saml2TokenCreationState saml2TokenCreationState;
    private final TokenTypeId inputTokenType;
    private final JsonValue inputToken;

    public Saml2RestTokenProviderParameters(Saml2TokenCreationState saml2TokenCreationState, TokenTypeId inputTokenType, JsonValue inputToken) {
        this.saml2TokenCreationState = saml2TokenCreationState;
        this.inputTokenType = inputTokenType;
        this.inputToken = inputToken;
    }

    @Override
    public Saml2TokenCreationState getTokenCreationState() {
        return saml2TokenCreationState;
    }

    @Override
    public TokenTypeId getInputTokenType() {
        return inputTokenType;
    }

    @Override
    public JsonValue getInputToken() {
        return inputToken;
    }
}
