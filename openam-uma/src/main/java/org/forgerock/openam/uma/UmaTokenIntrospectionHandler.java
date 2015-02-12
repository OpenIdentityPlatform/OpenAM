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

package org.forgerock.openam.uma;

import static org.forgerock.json.fluent.JsonValue.*;
import static org.forgerock.oauth2.core.OAuth2Constants.IntrospectionEndpoint.*;

import javax.inject.Inject;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.TokenIntrospectionHandler;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.restlet.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An introspection handler for UMA RPT tokens.
 */
public class UmaTokenIntrospectionHandler implements TokenIntrospectionHandler {

    private final Logger logger = LoggerFactory.getLogger("UmaProvider");
    private final UmaProviderSettingsFactory providerSettingsFactory;

    @Inject
    public UmaTokenIntrospectionHandler(UmaProviderSettingsFactory providerSettingsFactory) {
        this.providerSettingsFactory = providerSettingsFactory;
    }

    @Override
    public JsonValue introspect(OAuth2Request request, String clientId, String tokenType, String tokenId)
            throws ServerException {
        try {
            UmaProviderSettings providerSettings = providerSettingsFactory.get(request.<Request>getRequest());
            if (tokenType == null || RPT_TYPE.equals(tokenType)) {
                RequestingPartyToken token = providerSettings.getUmaTokenStore().readRPT(tokenId);
                if (token != null && token.getResourceServerClientId().equals(clientId) &&
                        token.getRealm().equals(request.<String>getParameter(OAuth2Constants.Params.REALM))) {
                    return renderRPT(providerSettings, token);
                } else {
                    logger.warn("Token {} was requested by client {} but wasn't intended for it",
                            request.getParameter(TOKEN), clientId);
                }
            }
        } catch (NotFoundException e) {
            // OK, we'll return not active.
            logger.debug("Couldn't find RPT with ID {}", tokenId, e);
        }
        return null;
    }

    /**
     * Render the RPT as a JsonValue according to the specification for introspection of RPTs.
     * @see <a href="https://docs.kantarainitiative.org/uma/draft-uma-core.html#uma-bearer-token-profile">UMA
     * specification - RPT Profile: Bearer</a>
     * @param providerSettings The provider that is connecting.
     * @param token The RPT.
     * @return A JSON representation of the token attributes.
     * @throws org.forgerock.oauth2.core.exceptions.ServerException
     */
    private JsonValue renderRPT(UmaProviderSettings providerSettings, RequestingPartyToken token) throws ServerException {
        JsonValue permissions = new JsonValue(array());
        for (Permission p : token.getPermissions()) {
            JsonValue permission = json(object(
                    field(UmaConstants.Introspection.RESOURCE_SET_ID, p.getResourceSetId()),
                    field(UmaConstants.Introspection.SCOPES, p.getScopes())));
            if (p.getExpiryTime() != null) {
                permission.add(OAuth2Constants.JWTTokenParams.EXP, p.getExpiryTime());
            }
            permissions.add(permission.getObject());
        }
        return json(object(
                field(ACTIVE, true),
                field(OAuth2Constants.JWTTokenParams.EXP, token.getExpiryTime() / 1000),
                field(OAuth2Constants.JWTTokenParams.ISS, providerSettings.getIssuer()),
                field(TOKEN_TYPE, RPT_TYPE),
                field(UmaConstants.Introspection.PERMISSIONS, permissions.getObject())
        ));
    }

    @Override
    public Integer priority() {
        return 0;
    }

}
