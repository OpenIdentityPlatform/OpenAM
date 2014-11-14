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
import org.forgerock.util.Reject;

import javax.inject.Inject;

import static org.forgerock.oauth2.core.Utils.isEmpty;

/**
 * Implementation of the AuthorizationCodeRequestValidator for OAuth2 request validation.
 *
 * @since 12.0.0
 */
public class AuthorizationCodeRequestValidatorImpl implements AuthorizationCodeRequestValidator {

    private final RedirectUriValidator redirectUriValidator;

    /**
     * Constructs a new AuthorizationCodeRequestValidatorImpl.
     *
     * @param redirectUriValidator An instance of the RedirectUriValidator.
     */
    @Inject
    public AuthorizationCodeRequestValidatorImpl(final RedirectUriValidator redirectUriValidator) {
        this.redirectUriValidator = redirectUriValidator;
    }

    /**
     * {@inheritDoc}
     */
    public void validateRequest(OAuth2Request request, ClientRegistration clientRegistration)
            throws InvalidRequestException, RedirectUriMismatchException, InvalidClientException {

        if  (request.getParameter("scope") != null) {
            throw new InvalidRequestException("Scope parameter is not supported on an authorization code access_token "+
                    "exchange request. Scope parameter should be supplied to the authorize request.");
        }

        Reject.ifTrue(Utils.isEmpty(request.<String>getParameter("code")), "Missing parameter, 'code'");
        Reject.ifTrue(Utils.isEmpty(request.<String>getParameter("redirect_uri")), "Missing parameter, 'redirect_uri'");

        redirectUriValidator.validate(clientRegistration, request.<String>getParameter("redirect_uri"));
    }
}
