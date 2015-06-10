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

/**
 * OpenIdConnect tokens can include an Authentication Context Class Reference (acr) claim which indicates how the subject
 * asserted by the OIDC token was authenticated. For the rest-sts, this will ultimately be a function of the input token
 * in the token transformation invocation. A default implementation of this interface will be provided, but if users wish
 * to customize the default mappings, or support a specific acr value for a custom token implementation, then they
 * can implement this interface with a classpath-resident class, and specify the name of this class in the OpenIdConnectTokenConfig
 * state associated with the published sts, and that class will be consulted to provide the value of the acr claim corresponding
 * to the input token state.
 */
public interface OpenIdConnectTokenAuthnContextMapper {
    /**
     * Returns the AuthnContext value corresponding to the TokenType inputToken.
     * @param inputTokenType The TokenType validated as part of the token transformation
     * @param inputToken The json representation of the validated token, as presented to the REST STS in the
     *                   token transformation invocation. This state can be used by custom implementations of this interface
     *                   to make more elaborate decisions regarding the returned AuthnContext class reference.
     * @return A valid AuthnContext value, as specified in the acr claim here:
     * http://openid.net/specs/openid-connect-core-1_0.html#IDToken
     */
    String getAuthnContextClassReference(TokenTypeId inputTokenType, JsonValue inputToken);

}
