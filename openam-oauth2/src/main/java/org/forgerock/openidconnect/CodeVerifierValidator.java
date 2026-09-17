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
* Copyright 2015-2016 ForgeRock AS.
* Portions copyright 2025-2026 3A Systems LLC.
*/
package org.forgerock.openidconnect;

import java.util.Set;

import jakarta.inject.Inject;
import org.forgerock.oauth2.core.AuthorizeRequestValidator;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.oauth2.OAuth2Constants.UrlLocation;
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

import static org.forgerock.oauth2.core.Utils.isEmpty;
import static org.forgerock.oauth2.core.Utils.isOpenIdConnectFragmentErrorType;
import static org.forgerock.oauth2.core.Utils.splitResponseType;
import static org.forgerock.openam.oauth2.OAuth2Constants.UrlLocation.FRAGMENT;
import static org.forgerock.openam.oauth2.OAuth2Constants.UrlLocation.QUERY;

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

        // response_type is a space delimited set: hybrid flows ("code token", "code id_token",
        // "code id_token token") issue an authorization code too, so PKCE must apply to them.
        final Set<String> responseTypes =
                splitResponseType(request.<String>getParameter(OAuth2Constants.Params.RESPONSE_TYPE));

        if (!settings.isCodeVerifierRequired() || !responseTypes.contains(OAuth2Constants.Params.CODE)) {
            return;
        }

        // OpenID Connect Core 3.3.2.6: for response types carrying id_token or token the error
        // must be returned in the fragment, otherwise the client never sees it.
        final UrlLocation errorLocation = isOpenIdConnectFragmentErrorType(responseTypes) ? FRAGMENT : QUERY;

        if (isEmpty(request.<String>getParameter(OAuth2Constants.Custom.CODE_CHALLENGE))) {
            throw new InvalidRequestException(
                    "Missing parameter, '" + OAuth2Constants.Custom.CODE_CHALLENGE + "'", errorLocation);
        }

        final String codeChallengeMethod = request.getParameter(OAuth2Constants.Custom.CODE_CHALLENGE_METHOD);

        if (codeChallengeMethod != null
                && !OAuth2Constants.Custom.CODE_CHALLENGE_METHOD_S_256.equals(codeChallengeMethod)
                && !OAuth2Constants.Custom.CODE_CHALLENGE_METHOD_PLAIN.equals(codeChallengeMethod)) {
            throw new InvalidRequestException(
                    "Invalid value for " + OAuth2Constants.Custom.CODE_CHALLENGE_METHOD, errorLocation);
        }
    }
}
