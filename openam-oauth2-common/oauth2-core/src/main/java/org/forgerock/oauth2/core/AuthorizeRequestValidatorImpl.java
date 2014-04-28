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

package org.forgerock.oauth2.core;

import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnsupportedResponseTypeException;
import org.forgerock.util.Reject;

import javax.inject.Inject;
import javax.inject.Singleton;

import static org.forgerock.oauth2.core.Utils.isEmpty;
import static org.forgerock.oauth2.core.Utils.splitResponseType;

/**
 * Implementation of the request validator for the OAuth2 authorize endpoint.
 *
 * @since 12.0.0
 */
@Singleton
public class AuthorizeRequestValidatorImpl implements AuthorizeRequestValidator {

    private final ClientRegistrationStore clientRegistrationStore;
    private final RedirectUriValidator redirectUriValidator;
    private final OAuth2ProviderSettingsFactory providerSettingsFactory;
    private final ResponseTypeValidator responseTypeValidator;

    /**
     * Constructs a new AuthorizeRequestValidatorImpl instance.
     *
     * @param clientRegistrationStore An instance of the ClientRegistrationStore.
     * @param redirectUriValidator An instance of the RedirectUriValidator.
     * @param providerSettingsFactory An instance of the OAuth2ProviderSettingsFactory.
     * @param responseTypeValidator An instance of the ResponseTypeValidator.
     */
    @Inject
    public AuthorizeRequestValidatorImpl(ClientRegistrationStore clientRegistrationStore,
            RedirectUriValidator redirectUriValidator, OAuth2ProviderSettingsFactory providerSettingsFactory,
            ResponseTypeValidator responseTypeValidator) {
        this.clientRegistrationStore = clientRegistrationStore;
        this.redirectUriValidator = redirectUriValidator;
        this.providerSettingsFactory = providerSettingsFactory;
        this.responseTypeValidator = responseTypeValidator;
    }

    /**
     * {@inheritDoc}
     */
    public void validateRequest(OAuth2Request request) throws InvalidClientException, InvalidRequestException,
            RedirectUriMismatchException, UnsupportedResponseTypeException, ServerException {

        Reject.ifTrue(isEmpty(request.<String>getParameter("client_id")), "Missing parameter, 'client_id'");
        Reject.ifTrue(isEmpty(request.<String>getParameter("response_type")), "Missing parameter, 'response_type'");

        final ClientRegistration clientRegistration = clientRegistrationStore.get(request.<String>getParameter("client_id"),
                request);

        redirectUriValidator.validate(clientRegistration, request.<String>getParameter("redirect_uri"));

        responseTypeValidator.validate(clientRegistration,
                splitResponseType(request.<String>getParameter("response_type")), providerSettingsFactory.get(request));
    }
}
