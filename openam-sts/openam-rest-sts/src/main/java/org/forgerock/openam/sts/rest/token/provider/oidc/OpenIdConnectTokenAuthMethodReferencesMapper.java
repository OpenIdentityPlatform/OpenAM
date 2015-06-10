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

package org.forgerock.openam.sts.rest.token.provider.oidc;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.sts.TokenTypeId;

import java.util.Set;

/**
 * An interface which allows soap-sts publishers to generate the amr claim for issued OpenIdConnect tokens on the basis
 * of the validated input token. See the amr claim here: http://openid.net/specs/openid-connect-core-1_0.html#IDToken.
 */
public interface OpenIdConnectTokenAuthMethodReferencesMapper {
    /**
     * Returns the Set of authentication methods references corresponding to the TokenType inputToken.
     * @param inputTokenType The input token type passed as input to the token transformation operation
     * @param inputToken The input token passed as input to the token transformation operation
     * @return A set of authentication method references appropriate to the validation of the input token type.
     */
    Set<String> getAuthnMethodsReferences(TokenTypeId inputTokenType, JsonValue inputToken);
}
