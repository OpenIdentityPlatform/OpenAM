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

import org.forgerock.oauth2.core.exceptions.ClientAuthenticationFailedException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;

/**
 * Authenticates OAuth2 clients by extracting the client's identifier and secret from the request.
 *
 * @since 12.0.0
 */
public interface ClientAuthenticator {

    /**
     * Authenticates the client making the OAuth2 request by extracting the client's id and secret from the request
     * and authenticating against the OAuth2 providers client registrations.
     *
     * @param request The OAuth2Request. Must not be {@code null}.
     * @return The client's registration.
     * @throws InvalidClientException If either the request does not contain the client's id or the client fails to be
     *          authenticated.
     * @throws InvalidRequestException If the request is missing any required parameters or is otherwise malformed.
     * @throws ClientAuthenticationFailedException If client authentication fails.
     */
    ClientRegistration authenticate(OAuth2Request request) throws InvalidClientException, InvalidRequestException,
            ClientAuthenticationFailedException;
}
