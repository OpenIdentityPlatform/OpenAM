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
* Copyright 2015 ForgeRock AS.
*/
package org.forgerock.openidconnect;

import javax.inject.Inject;
import org.forgerock.oauth2.core.AuthorizeRequestValidator;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidScopeException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnsupportedResponseTypeException;
import org.forgerock.util.Reject;

import static org.forgerock.oauth2.core.Utils.isEmpty;

/**
 * Checks whether Proof Key for Code Exchange is enabled and validates accordingly
 *
 * @since 13.0.0
 */
public class CodeVerifierValidator implements AuthorizeRequestValidator {

    private final OAuth2ProviderSettingsFactory providerSettingsFactory;

    @Inject
    public CodeVerifierValidator(OAuth2ProviderSettingsFactory providerSettingsFactory) {
        this.providerSettingsFactory = providerSettingsFactory;
    }

    @Override
    public void validateRequest(OAuth2Request request) throws InvalidClientException, InvalidRequestException,
            RedirectUriMismatchException, UnsupportedResponseTypeException, ServerException, BadRequestException,
            InvalidScopeException, NotFoundException {
        final OAuth2ProviderSettings settings = providerSettingsFactory.get(request);

        if (!settings.isCodeVerifierRequired() || !isAuthCodeRequest(request)) {
            return;
        } else {
            Reject.ifTrue(isEmpty(request.<String>getParameter(OAuth2Constants.Custom.CODE_CHALLENGE)),
                    "Missing parameter, '" + OAuth2Constants.Custom.CODE_CHALLENGE + "'");

            String codeChallengeMethod = request.getParameter(OAuth2Constants.Custom.CODE_CHALLENGE_METHOD);

            if (codeChallengeMethod != null) {
                Reject.ifFalse(codeChallengeMethod.equals(
                                OAuth2Constants.Custom.CODE_CHALLENGE_METHOD_S_256) || codeChallengeMethod.equals(
                                OAuth2Constants.Custom.CODE_CHALLENGE_METHOD_PLAIN),
                        "Invalid value for " + OAuth2Constants.Custom.CODE_CHALLENGE_METHOD);
            }

            return;
        }

    }

    private boolean isAuthCodeRequest(OAuth2Request request) {
        return request.<String>getParameter(
                OAuth2Constants.Params.RESPONSE_TYPE).equals(OAuth2Constants.Params.CODE);
    }
}
