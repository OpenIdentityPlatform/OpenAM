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
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.ClientAuthenticationFailedException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;

/**
 * A service for introspecting tokens.
 */
public interface TokenIntrospectionService {

    /**
     * Allows introspection of a (refresh or access) token according to the
     * <a href="http://tools.ietf.org/html/draft-ietf-oauth-introspection-04">OAuth 2.0 Token Introspection
     * standard</a>.
     * <p>
     * The request must must contain authorization as the OAuth 2.0 client that the token was issued for, using either
     * credentials or a bearer token.
     * @param request The OAuth 2.0 request
     * @return Details of the specified token.
     */
    JsonValue introspect(OAuth2Request request) throws InvalidClientException, InvalidRequestException, NotFoundException, ClientAuthenticationFailedException, ServerException, BadRequestException, InvalidGrantException;

}
