/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.restlet.ext.oauth2.flow;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.forgerock.restlet.ext.oauth2.OAuth2;
import org.forgerock.restlet.ext.oauth2.OAuth2Utils;
import org.forgerock.restlet.ext.oauth2.OAuthProblemException;
import org.forgerock.restlet.ext.oauth2.model.AccessToken;
import org.forgerock.restlet.ext.oauth2.model.RefreshToken;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class RefreshTokenServerResource extends AbstractFlow {

    @Post("form:json")
    public Representation represent(Representation entity) {
        /*
         * o require client authentication for confidential clients or for any
         * client that was issued client credentials (or with other
         * authentication requirements), o authenticate the client if client
         * authentication is included and ensure the refresh token was issued to
         * the authenticated client, and o validate the refresh token.
         */

        client = getAuthenticatedClient();
        String refresh_token =
                OAuth2Utils.getRequestParameter(getRequest(), OAuth2.Params.REFRESH_TOKEN,
                        String.class);
        // Find Token
        RefreshToken refreshToken = getTokenStore().readRefreshToken(refresh_token);

        if (null == refreshToken) {
            throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(getRequest(),
                    "RefreshToken does not exist");
        } else if (!refreshToken.getClient().getClientId().equals(client.getClient().getClientId())) {
            // TODO throw Exception
            throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(getRequest(),
                    "Token was issued to a different client");
        } else {
            // TODO validate the refresh token.
            if (refreshToken.getExpireTime() - System.currentTimeMillis() < 0
                    || refreshToken.isExpired()) {
                throw OAuthProblemException.OAuthError.EXPIRED_TOKEN.handle(getRequest());
            }

            // Get the requested scope
            String scope_before =
                    OAuth2Utils
                            .getRequestParameter(getRequest(), OAuth2.Params.SCOPE, String.class);

            // Get the granted scope
            Set<String> granted_after = new TreeSet<String>(refreshToken.getScope());
            granted_after.retainAll(client.getClient().allowedGrantScopes());

            // Validate the granted scope
            Set<String> checkedScope = getCheckedScope(scope_before, granted_after, granted_after);

            // Generate Token
            AccessToken token = createAccessToken(refreshToken, checkedScope);
            Map<String, Object> response = token.convertToMap();
            return new JacksonRepresentation<Map>(response);
        }
    }

    @Override
    protected String[] getRequiredParameters() {
        return new String[] { OAuth2.Params.GRANT_TYPE, OAuth2.Params.REFRESH_TOKEN };
    }

    /**
     * This method is intended to be overridden by subclasses.
     * 
     * @param checkedScope
     * @return
     * @throws org.forgerock.restlet.ext.oauth2.OAuthProblemException
     * 
     */
    protected AccessToken createAccessToken(RefreshToken refreshToken, Set<String> checkedScope) {
        return getTokenStore().createAccessToken(client.getClient().getAccessTokenType(),
                checkedScope, refreshToken);
    }

}
