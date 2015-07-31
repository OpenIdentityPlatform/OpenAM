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

import org.forgerock.openam.sts.TokenTypeId;
import org.forgerock.openam.sts.TokenValidationException;

/**
 * Inteface defining the concerns of validating tokens issued by the rest-sts. The generic type T corresponds to the
 * specific token type. Note that in the OpenAM 13 release, the validation of tokens issued by the sts will involve only
 * consulting the TokenService to determine whether the token was persisted in the CTS (and thus issued by the sts, and not
 * expired).
 */
public interface RestIssuedTokenValidator<T>  {
    /**
     *
     * @param tokenType the type of the to-be-validated token
     * @return true if this RestIssuedTokenValidator instance can validate tokens of the specified type
     */
    boolean canValidateToken(TokenTypeId tokenType);

    /**
     *
     * @param validatorParameters Encapsulation of the state necessary to validate the token type - in this case, simply
     *                            the token itself
     * @return true if the token is valid - false otherwise
     * @throws TokenValidationException if an exception prevented token validation from occurring
     */
    boolean validateToken(RestIssuedTokenValidatorParameters<T> validatorParameters) throws TokenValidationException;
}
