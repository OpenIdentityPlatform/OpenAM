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

import static org.forgerock.json.fluent.JsonValue.*;
import static org.forgerock.oauth2.core.OAuth2Constants.IntrospectionEndpoint.*;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.oauth2.core.exceptions.ClientAuthenticationFailedException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;

/**
 * {@inheritDoc}
 */
public class TokenIntrospectionServiceImpl implements TokenIntrospectionService {

    private final ClientAuthenticator clientAuthenticator;
    private final OAuth2ProviderSettingsFactory providerSettingsFactory;
    private final Set<TokenIntrospectionHandler> handlers;

    @Inject
    public TokenIntrospectionServiceImpl(ClientAuthenticator clientAuthenticator,
            OAuth2ProviderSettingsFactory providerSettingsFactory, Set<TokenIntrospectionHandler> handlers) {
        this.clientAuthenticator = clientAuthenticator;
        this.providerSettingsFactory = providerSettingsFactory;
        this.handlers = new TreeSet<TokenIntrospectionHandler>(new Comparator<TokenIntrospectionHandler>() {
            @Override
            public int compare(TokenIntrospectionHandler t1, TokenIntrospectionHandler t2) {
                int priorityOrder = t2.priority().compareTo(t1.priority());
                return priorityOrder != 0 ? priorityOrder : t1.getClass().getName().compareTo(t2.getClass().getName());
            }
        });
        this.handlers.addAll(handlers);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonValue introspect(OAuth2Request request) throws InvalidClientException, InvalidRequestException,
            NotFoundException, ClientAuthenticationFailedException, ServerException {
        OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);
        ClientRegistration clientRegistration = clientAuthenticator.authenticate(request,
                providerSettings.getIntrospectionEndpoint());
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
