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

import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.AccessDeniedException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnsupportedResponseTypeException;
import org.forgerock.openidconnect.exceptions.InvalidClientMetadata;
import org.forgerock.json.fluent.JsonValue;

/**
 * Service for registering OpenId Connect clients.
 *
 * @since 12.0.0
 */
public interface OpenIdConnectClientRegistrationService {

    /**
     * Creates an OpenId Connect client registration in the OAuth2 provider.
     *
     * @param accessToken The access token for making the registration call.
     * @param deploymentURL THe deployment url of the OAuth2 provider.
     * @param request The OAuth2 request.
     * @return JsonValue representation of the client registration.
     * @throws InvalidClientMetadata
     * @throws ServerException If any internal server error occurs.
     * @throws UnsupportedResponseTypeException If the requested response type is not supported by either the client
     *          or the OAuth2 provider.
     */
    JsonValue createRegistration(String accessToken, String deploymentURL, OAuth2Request request)
            throws InvalidClientMetadata, ServerException, UnsupportedResponseTypeException, AccessDeniedException;

    /**
     * Gets an OpenId Connect client registration from the OAuth2 provider.
     *
     * @param clientId The client's id.
     * @param accessToken The access token used to register the client.
     * @param request The OAuth2 request.
     * @return JsonValue representation of the client registration.
     * @throws InvalidRequestException If either the request does not contain the client's id or the client fails to be
     *          authenticated.
     * @throws InvalidClientMetadata
     */
    JsonValue getRegistration(String clientId, String accessToken, OAuth2Request request)
            throws InvalidRequestException, InvalidClientMetadata;
}
