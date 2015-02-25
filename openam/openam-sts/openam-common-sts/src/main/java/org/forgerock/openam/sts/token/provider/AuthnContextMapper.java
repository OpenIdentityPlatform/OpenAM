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
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.token.provider;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.sts.TokenType;

/**
 * Defines the functionality which maps a TokenType to a SAML2 AuthnContext value (see section 2.7.2.2
 * of http://docs.oasis-open.org/security/saml/v2.0/saml-core-2.0-os.pdf for details). This AuthnContext will
 * be sent to the TokenGenerationService for inclusion in the AuthnStatement of the issued assertion. It specifies
 * the manner in which the subject was authenticated. In the context of token transformation, the validated input
 * token will determine the AuthnContext specified in the TokenGenerationService invocation. This interface defines
 * the contract for this mapping.
 *
 */
public interface AuthnContextMapper {
    /**
     * Returns the AuthnContext value corresponding to the TokenType inputToken.
     * @param inputTokenType The TokenType validated as part of the token transformation
     * @param inputToken The json representation of the validated token, as presented to the REST STS in the
     *                   token transformation invocation. This state can be used by custom implementations of this interface
     *                   to make more elaborate decisions regarding the returned AuthnContext class reference.
     * @return A valid AuthnContext value, as defined here:
     * http://docs.oasis-open.org/security/saml/v2.0/saml-authn-context-2.0-os.pdf
     */
    String getAuthnContext(TokenType inputTokenType, JsonValue inputToken);
}
