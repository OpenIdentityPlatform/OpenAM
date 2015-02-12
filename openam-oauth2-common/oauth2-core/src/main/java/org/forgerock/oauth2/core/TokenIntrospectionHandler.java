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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.oauth2.core;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;

/**
 * Handles token introspection for given types of tokens.
 */
public interface TokenIntrospectionHandler {

    /**
     * Look for a token of a particular type, with a given ID, and return its introspected representation.
     * @param tokenType The type of the token being introspected - could be null, in which case try and find any token
     *                  with that ID.
     * @param tokenId The ID of the token.
     * @param request The OAuth 2.0 request.
     * @param clientId The OAuth 2.0 client making the request.
     * @return The introspected represenation.
     */
    JsonValue introspect(OAuth2Request request, String clientId, String tokenType, String tokenId) throws ServerException, NotFoundException;

    /**
     * The priority of the handler. A lower value will be used first.
     */
    Integer priority();
}
