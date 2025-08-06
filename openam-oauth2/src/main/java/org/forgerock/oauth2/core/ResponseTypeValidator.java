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

package org.forgerock.oauth2.core;

import static org.forgerock.openam.oauth2.OAuth2Constants.AuthorizationEndpoint.*;
import static org.forgerock.openam.oauth2.OAuth2Constants.*;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.OPENID;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.SCOPE;

import java.util.Map;
import java.util.Set;
import jakarta.inject.Singleton;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnsupportedResponseTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            OAuth2ProviderSettings providerSettings, OAuth2Request request)
            throws InvalidRequestException, UnsupportedResponseTypeException, ServerException {

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

        //requested response type comes in as 'id_token token' (for example) and split into 'id_token' and 'token'
        //provider response types are as 'id_token token', 'token', 'id_token' - those with spaces must be split
        //and compared in any order. A provider must specify all response types for a client in one string.
        final Set<String> clientAllowedResponseTypes = clientRegistration.getAllowedResponseTypes();
        boolean cleared = false;
        for (String clientAllowedResponseType : clientAllowedResponseTypes) {
            if (Utils.splitResponseType(clientAllowedResponseType).containsAll(requestedResponseTypes) &&
                    Utils.splitResponseType(clientAllowedResponseType).size() == requestedResponseTypes.size()) {
                cleared = true;
                break;
            }
        }

        if (!cleared) {
            throw new UnsupportedResponseTypeException("Client does not support this response type.", urlLocation);
        }

        validateForOAuth2(clientRegistration, requestedResponseTypes);

        Set<String> requestedScopes = Utils.splitScope(request.<String>getParameter(SCOPE));
        if (Utils.isOpenIdConnectClient(clientRegistration) && requestedScopes.contains(OPENID)) {
            validateOpenidResponseTypes(clientRegistration, requestedResponseTypes);
        }

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

    /**
      See <a href="http://openid.net/specs/openid-connect-core-1_0.html#Authentication">
        table 'OpenID Connect "response_type" Values'
      </a>
     */
    private void validateOpenidResponseTypes(ClientRegistration clientRegistration, Set<String> requestedResponseTypes)
            throws UnsupportedResponseTypeException {

        if (requestedResponseTypes.contains(TOKEN)
                && !requestedResponseTypes.contains(CODE)
                && !requestedResponseTypes.contains(ID_TOKEN)) {

            logger.debug("Response type is not supported. OpenId Connect client does not support scope=\"token\".");
            throw new UnsupportedResponseTypeException("Response type is not supported.",
                    Utils.getRequiredUrlLocation(requestedResponseTypes, clientRegistration));
        }
    }
}
