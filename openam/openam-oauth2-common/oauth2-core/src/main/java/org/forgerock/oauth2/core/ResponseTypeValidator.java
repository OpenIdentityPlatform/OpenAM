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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.Map;
import java.util.Set;

import static org.forgerock.oauth2.core.OAuth2Constants.UrlLocation;
import static org.forgerock.oauth2.core.OAuth2Constants.AuthorizationEndpoint.*;

/**
 * Validates that the requested response types are valid and are allowed by the OAuth2 Provider and client registration.
 *
 * @since 12.0.0
 */
@Singleton
public class ResponseTypeValidator {

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");

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

        final UrlLocation urlLocation = Utils.getRequiredUrlLocation(requestedResponseTypes, clientRegistration);
        final Map<String, ResponseTypeHandler> allowedResponseTypes = providerSettings.getAllowedResponseTypes();

        if (allowedResponseTypes == null || allowedResponseTypes.isEmpty()) {
            throw new InvalidRequestException("Invalid Response Type.", urlLocation);
        }

        if (!allowedResponseTypes.keySet().containsAll(requestedResponseTypes)) {
            throw new UnsupportedResponseTypeException("Response type is not supported.", urlLocation);
        }

        final Set<String> clientAllowedResponseTypes = clientRegistration.getAllowedResponseTypes();
        if (!clientAllowedResponseTypes.containsAll(requestedResponseTypes)) {
            throw new UnsupportedResponseTypeException("Client does not support this response type.", urlLocation);
        }

        validateForOAuth2(clientRegistration, requestedResponseTypes);
        validateForOpenIdConnect(clientRegistration, requestedResponseTypes);

    }

    private void validateForOAuth2(ClientRegistration clientRegistration, Set<String> requestedResponseTypes)
            throws UnsupportedResponseTypeException {

        if (!Utils.isOpenIdConnectClient(clientRegistration)
                && requestedResponseTypes.contains(TOKEN)
                && requestedResponseTypes.contains(CODE)) {

            logger.debug("Response type is not supported. OAuth2 client does not support scope=\"token code\".");
            throw new UnsupportedResponseTypeException("Response type is not supported.",
                    Utils.getRequiredUrlLocation(requestedResponseTypes, clientRegistration));
        }
    }

    private void validateForOpenIdConnect(ClientRegistration clientRegistration, Set<String> requestedResponseTypes)
            throws UnsupportedResponseTypeException {

        if (Utils.isOpenIdConnectClient(clientRegistration)
                && requestedResponseTypes.contains(TOKEN)
                && !requestedResponseTypes.contains(CODE)
                && !requestedResponseTypes.contains(ID_TOKEN)) {

            logger.debug("Response type is not supported. OpenId Connect client does not support scope=\"token\".");
            throw new UnsupportedResponseTypeException("Response type is not supported.",
                    Utils.getRequiredUrlLocation(requestedResponseTypes, clientRegistration));
        }
    }
}
