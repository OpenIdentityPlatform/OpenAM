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
 * Default implementation of the OpenIdConnectTokenAuthnContextMapper. Note that the acr claim is optional - see
 * http://openid.net/specs/openid-connect-core-1_0.html#IDToken, and that the set of acr values for OIDC do not
 * seem to be as formalized as for e.g. SAML2. Thus this default Mapper will simply return null.
 */
public class DefaultOpenIdConnectTokenAuthnContextMapper implements OpenIdConnectTokenAuthnContextMapper {
    /**
     *
     * @param inputTokenType The TokenType validated as part of the token transformation
     * @param inputToken The json representation of the validated token, as presented to the REST STS in the
     *                   token transformation invocation. This state can be used by custom implementations of this interface
     *                   to make more elaborate decisions regarding the returned AuthnContext class reference.
     * @return the value corresponding to the acr claim to be included in the OpenIdConnect token. Will simply return null
     * because this value is optional.
     */
    @Override
    public String getAuthnContextClassReference(TokenTypeId inputTokenType, JsonValue inputToken) {
        return null;
    }
}
