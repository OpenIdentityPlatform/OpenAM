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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openidconnect;

import org.forgerock.oauth2.core.AuthorizeRequestValidator;
import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.ClientRegistrationStore;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.Utils;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidScopeException;
import org.forgerock.util.Reject;

import javax.inject.Inject;

import static org.forgerock.oauth2.core.OAuth2Constants.Params.*;
import static org.forgerock.oauth2.core.OAuth2Constants.UrlLocation.*;

import java.util.Set;

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
            InvalidClientException, InvalidScopeException {

        validateOpenIdScope(request);

        try {
            OpenIdPrompt prompt = new OpenIdPrompt(request);
            Reject.ifFalse(prompt.isValid(), "Prompt parameter " + prompt.getOriginalValue() + " is invalid");
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    private void validateOpenIdScope(OAuth2Request request) throws InvalidClientException, InvalidRequestException,
            InvalidScopeException {
        final ClientRegistration clientRegistration = clientRegistrationStore.get(
                request.<String>getParameter(CLIENT_ID), request);
        if (Utils.isOpenIdConnectClient(clientRegistration)) {
            final boolean openIdConnectRequested = Utils.splitScope(request.<String>getParameter(SCOPE)).contains(OPENID);
            final Set<String> responseTypes = Utils.splitResponseType(request.<String>getParameter(RESPONSE_TYPE));

            if (!openIdConnectRequested) {
                throw new InvalidRequestException("Missing expected scope=openid from request",
                        Utils.isOpenIdConnectFragmentErrorType(responseTypes) ? FRAGMENT : QUERY);
            }
        }
    }
}
