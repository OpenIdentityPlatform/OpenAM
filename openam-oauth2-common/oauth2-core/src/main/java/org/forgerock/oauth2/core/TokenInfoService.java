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

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.ExpiredTokenException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidTokenException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;

/**
 * Service to return the full information of a OAuth2 token.
 *
 * @since 12.0.0
 */
public interface TokenInfoService {

    /**
     * Returns a Json representation of the token's information that is on the OAuth2 request.
     *
     * @param request The OAuth2 request.
     * @return The token's information.
     * @throws InvalidRequestException If the request is missing any required parameters or is otherwise malformed.
     * @throws ServerException If any internal server error occurs.
     * @throws NotFoundException If the token cannot be found or is expired.
     */
    JsonValue getTokenInfo(OAuth2Request request) throws InvalidRequestException, NotFoundException, ServerException;
}
