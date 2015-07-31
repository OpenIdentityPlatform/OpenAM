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

package org.forgerock.openam.sts.token;

import org.forgerock.openam.sts.TokenIdGenerationException;
import org.forgerock.openam.sts.TokenType;

/**
 * STS-issued tokens which are persisted to the CTS require an identifier. For WS-Trust support in the soap-sts, the
 * to-be-canceled/validated token is specified in the request, and thus the token identifier must be derived from the actual
 * token. This interface defines the concern of generating an id given a token string. The identifier encapsulated in OIDC and
 * SAML2 tokens cannot be used when the token is encrypted. This interface will be consumed
 * by both the soap and rest sts, in the implementation of the validate and cancel operations, and in the TokenService,
 * when persisting generated tokens in the CTS.
 */
public interface CTSTokenIdGenerator {
    /**
     *
     * @param tokenType the type of token for which an id must be generated
     * @param tokenString the string representation of the token
     * @return the token id for CTS persistence
     * @throws TokenIdGenerationException if the token id cannot be generated.
     */
    String generateTokenId(TokenType tokenType, String tokenString) throws TokenIdGenerationException;
}
