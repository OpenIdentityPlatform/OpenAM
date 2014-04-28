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

/**
 * Request validator for the OAuth2 authorization code grant.
 * <br/>
 * Request validators validate that the specified OAuth2 request is valid be checking that the request contains all of
 * the required parameters.
 *
 * @since 12.0.0
 */
public interface AuthorizationCodeRequestValidator {

    /**
     * Validates that the OAuth2 request contains the valid parameters for the OAuth2 authorization code grant.
     *
     * @param request The OAuth2 request.  Must not be {@code null}.
     * @param clientRegistration The registration of the client making the request.
     * @throws InvalidRequestException If the request is missing any required parameters or is otherwise malformed.
     * @throws RedirectUriMismatchException If the redirect uri on the request does not match the redirect uri
     *          registered for the client.
     * @throws InvalidClientException If either the request does not contain the client's id or the client fails to be
     *          authenticated.
     * @throws IllegalArgumentException If the request is missing any required parameters.
     */
    void validateRequest(OAuth2Request request, ClientRegistration clientRegistration) throws InvalidRequestException,
            RedirectUriMismatchException, InvalidClientException;
}
