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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.oauth2.validation;

import jakarta.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.AuthorizationCodeRequestValidator;
import org.forgerock.oauth2.core.AuthorizeRequestValidator;
import org.forgerock.oauth2.core.ClientCredentialsRequestValidator;
import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.PasswordCredentialsRequestValidator;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.oauth2.OAuth2Utils;

/**
 * Validates the confirmation key parameter if it is present.
 */
public class ConfirmationKeyValidator implements AuthorizeRequestValidator, AuthorizationCodeRequestValidator,
        ClientCredentialsRequestValidator, PasswordCredentialsRequestValidator {

    private static final String JWK_KEY = "jwk";

    private final OAuth2Utils utils;

    @Inject
    public ConfirmationKeyValidator(OAuth2Utils utils) {
        this.utils = utils;
    }

    @Override
    public void validateRequest(OAuth2Request request, ClientRegistration clientRegistration)
            throws InvalidRequestException {
        validateRequest(request);
    }

    @Override
    public void validateRequest(OAuth2Request request) throws InvalidRequestException {
        boolean valid;
        try {
            JsonValue confirmationKey = utils.getConfirmationKey(request);
            valid = confirmationKey == null
                    || (confirmationKey.isDefined(JWK_KEY) && confirmationKey.get(JWK_KEY).isMap());
        } catch (Exception e) {
            valid = false;
        }

        if (!valid) {
            throw new InvalidRequestException("Invalid " + OAuth2Constants.ProofOfPossession.CNF_KEY + " parameter");
        }
    }
}
