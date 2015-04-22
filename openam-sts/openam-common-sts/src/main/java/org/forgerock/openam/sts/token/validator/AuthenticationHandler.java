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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.token.validator;

import org.forgerock.openam.sts.TokenTypeId;
import org.forgerock.openam.sts.TokenValidationException;

/**
 * Defines the authentication interface which is the foundation for all sts token authentication actions. AuthenticationHandlers
 * for the specified set of input token types supported by the sts instances are plugged into the rest/soap/wss4j token validation
 * context to delegate all token validation to OpenAM.
 * @param <T> The to-be-authenticated token. Note that its type cannot be constrained, as it varies from X509Certificate[],
 *           to classes defined in the org.forgerock.openam.sts.token.model package, to classes defined in Apache wss4j.
 */
public interface AuthenticationHandler<T> {
    /**
     *
     * @param token the to-be-authentication token
     * @param tokenTypeId the type of the to-be-authenticated token. Necessary to obtain the AuthTargetMapping defining
     *                    the rest authN url configured for the sts instance for this particular token type.
     * @return the OpenAM session id corresponding to the successfully authenticated token
     * @throws TokenValidationException if the token could not be successfully validated
     */
    String authenticate(T token, TokenTypeId tokenTypeId) throws TokenValidationException;
}
