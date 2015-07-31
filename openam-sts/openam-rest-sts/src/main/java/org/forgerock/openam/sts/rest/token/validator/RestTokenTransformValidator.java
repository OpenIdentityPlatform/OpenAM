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

package org.forgerock.openam.sts.rest.token.validator;

import org.forgerock.openam.sts.TokenValidationException;

/**
 * Defines the contract for token validators deployed in the context of token transformation.
 *
 * The generic type T corresponds to the type of the to-be-validated token. These types are currently limited to:
 * 1. java.security.cert.X509Certificate[]
 * 2. the classes in the org.forgerock.openam.sts.token.model package of the openam-sts-client package: RestUsernameToken,
 * OpenAMSessionToken, OpenIdConnectIdToken.
 * 3. JsonValue, which is the type common to all custom token validators.
 */
public interface RestTokenTransformValidator<T> {
    /**
     *
     * @param restTokenTransformValidatorParameters The token validation parameters which provide access to the to-be-validated
     *                                     token
     * @return The RestTokenTransformValidatorResult encapsulating the Principal and OpenAM session id corresponding to a
     * successfully-validated token
     * @throws TokenValidationException If the token could not be successfully validated.
     */
    RestTokenTransformValidatorResult validateToken(RestTokenTransformValidatorParameters<T> restTokenTransformValidatorParameters) throws TokenValidationException;

}
