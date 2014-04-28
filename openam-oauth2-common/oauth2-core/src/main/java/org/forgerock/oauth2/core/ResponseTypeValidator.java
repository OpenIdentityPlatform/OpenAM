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

import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnsupportedResponseTypeException;

import javax.inject.Singleton;
import java.util.Map;
import java.util.Set;

/**
 * Validates that the requested response types are valid and are allowed by the OAuth2 Provider and client registration.
 *
 * @since 12.0.0
 */
@Singleton
public class ResponseTypeValidator {

    /**
     * Validates that the requested response types are valid and supported by both the OAuth2 client and provider.
     *
     * @param clientRegistration The client registration.
     * @param requestedResponseTypes The requested response types.
     * @param providerSettings The OAuth2ProviderSettings instance.
     * @throws org.forgerock.oauth2.core.exceptions.InvalidRequestException
     * @throws org.forgerock.oauth2.core.exceptions.UnsupportedResponseTypeException
     * @throws org.forgerock.oauth2.core.exceptions.ServerException
     */
    public void validate(ClientRegistration clientRegistration, Set<String> requestedResponseTypes,
            OAuth2ProviderSettings providerSettings) throws InvalidRequestException, UnsupportedResponseTypeException,
            ServerException {

        if (requestedResponseTypes == null || requestedResponseTypes.isEmpty()) {
            throw new UnsupportedResponseTypeException("Response type is not supported.");
        }

        final Map<String, ResponseTypeHandler> allowedResponseTypes = providerSettings.getAllowedResponseTypes();

        if (allowedResponseTypes == null || allowedResponseTypes.isEmpty()) {
            throw new InvalidRequestException("Invalid Response Type.");
        }

        if (!allowedResponseTypes.keySet().containsAll(requestedResponseTypes)) {
            throw new UnsupportedResponseTypeException("Response type is not supported.");
        }

        final Set<String> clientAllowedResponseTypes = clientRegistration.getAllowedResponseTypes();
        if (!clientAllowedResponseTypes.containsAll(requestedResponseTypes)) {
            throw new UnsupportedResponseTypeException("Client does not support this response type.");
        }
    }
}
