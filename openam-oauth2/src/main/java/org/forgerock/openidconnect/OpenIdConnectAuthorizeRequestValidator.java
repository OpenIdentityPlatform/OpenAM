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
 * Copyright 2014-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openidconnect;

import static org.forgerock.openam.oauth2.OAuth2Constants.Custom.*;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.*;
import static org.forgerock.openam.oauth2.OAuth2Constants.UrlLocation.*;
import static org.forgerock.openam.oauth2.OAuth2Constants.AuthorizationEndpoint.ID_TOKEN;

import java.util.Set;

import jakarta.inject.Inject;

import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.oauth2.core.AuthorizeRequestValidator;
import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.ClientRegistrationStore;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.Utils;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidScopeException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.util.Reject;

/**
 * Implementation of the AuthorizeRequestValidator for OpenID Connect request validation.
 *
 * @since 12.0.0
 */
public class OpenIdConnectAuthorizeRequestValidator implements AuthorizeRequestValidator {

    private final ClientRegistrationStore clientRegistrationStore;

    /**
     * Constructs a new OpenIdConnectAuthorizeRequestValidator instance.
     *
     * @param clientRegistrationStore An instance of the ClientRegistrationStore.
     */
    @Inject
    public OpenIdConnectAuthorizeRequestValidator(ClientRegistrationStore clientRegistrationStore) {
        this.clientRegistrationStore = clientRegistrationStore;
    }

    /**
     * {@inheritDoc}
     */
    public void validateRequest(OAuth2Request request) throws BadRequestException, InvalidRequestException,
            InvalidClientException, InvalidScopeException, NotFoundException {

        validateOpenIdScope(request);

        try {
            OpenIdPrompt prompt = new OpenIdPrompt(request);
            Reject.ifFalse(prompt.isValid(), "Prompt parameter " + prompt.getOriginalValue() +
                    " is invalid or unsupported");
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    private void validateNonce(OAuth2Request request, Set<String> responseTypes) throws InvalidRequestException {
        // Core Spec 3.2.2.1, 3.3.2.11, etc
        if (!(responseTypes.size() == 1 && responseTypes.contains(CODE))) {
            if (request.getParameter(NONCE) == null) {
                throw new InvalidRequestException("Missing required parameter nonce from request", FRAGMENT);
            }
        }
    }

    private void validateOpenIdScope(OAuth2Request request) throws InvalidClientException, InvalidRequestException,
            InvalidScopeException, NotFoundException {
        final ClientRegistration clientRegistration = clientRegistrationStore.get(
                request.<String>getParameter(CLIENT_ID), request);
        if (Utils.isOpenIdConnectClient(clientRegistration)) {
            final Set<String> responseTypes = Utils.splitResponseType(request.<String>getParameter(RESPONSE_TYPE));
            Set<String> requestedScopes = Utils.splitScope(request.<String>getParameter(SCOPE));
            if (CollectionUtils.isEmpty(requestedScopes)) {
                requestedScopes = clientRegistration.getDefaultScopes();
            }
            if (!requestedScopes.contains(OPENID) && responseTypes.contains(ID_TOKEN)) {
                throw new InvalidRequestException("Missing expected scope=openid from request",
                        Utils.isOpenIdConnectFragmentErrorType(responseTypes) ? FRAGMENT : QUERY);
            } else if (requestedScopes.contains(OPENID)) {
                validateNonce(request, responseTypes);
            }
        }
    }
}
