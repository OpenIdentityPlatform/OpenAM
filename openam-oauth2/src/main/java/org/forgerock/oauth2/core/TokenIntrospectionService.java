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
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.oauth2.core;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.oauth2.OAuth2Constants.IntrospectionEndpoint.*;

import jakarta.inject.Inject;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.oauth2.OAuth2UrisFactory;

/**
 * A service for introspecting tokens.
 */
public class TokenIntrospectionService {

    private final ClientAuthenticator clientAuthenticator;
    private final Set<TokenIntrospectionHandler> handlers;
    private final OAuth2UrisFactory urisFactory;

    @Inject
    public TokenIntrospectionService(ClientAuthenticator clientAuthenticator,
            Set<TokenIntrospectionHandler> handlers, OAuth2UrisFactory urisFactory) {
        this.clientAuthenticator = clientAuthenticator;
        this.urisFactory = urisFactory;
        this.handlers = new TreeSet<>(new Comparator<TokenIntrospectionHandler>() {
            @Override
            public int compare(TokenIntrospectionHandler t1, TokenIntrospectionHandler t2) {
                int priorityOrder = t2.priority().compareTo(t1.priority());
                return priorityOrder != 0 ? priorityOrder : t1.getClass().getName().compareTo(t2.getClass().getName());
            }
        });
        this.handlers.addAll(handlers);
    }

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
    public JsonValue introspect(OAuth2Request request) throws InvalidClientException, InvalidRequestException,
            NotFoundException, ServerException {
        ClientRegistration clientRegistration = clientAuthenticator.authenticate(request,
                urisFactory.get(request).getIntrospectionEndpoint());
        String tokenType = request.getParameter(TOKEN_TYPE_HINT);
        String tokenId = request.getParameter(TOKEN);

        for (TokenIntrospectionHandler handler : handlers) {
            JsonValue result = handler.introspect(request, clientRegistration.getClientId(), tokenType, tokenId);
            if (result != null) {
                return result;
            }
        }

        return json(object(field(ACTIVE, false)));
    }
}
