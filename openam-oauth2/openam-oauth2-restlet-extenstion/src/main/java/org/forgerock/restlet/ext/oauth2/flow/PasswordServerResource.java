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

import org.forgerock.restlet.ext.oauth2.OAuth2;
import org.forgerock.restlet.ext.oauth2.OAuth2Utils;
import org.forgerock.restlet.ext.oauth2.OAuthProblemException;
import org.forgerock.restlet.ext.oauth2.model.AccessToken;
import org.forgerock.restlet.ext.oauth2.model.RefreshToken;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.security.SecretVerifier;
import org.restlet.security.User;
import org.restlet.security.Verifier;

/**
 * @see <a
 *      href="http://tools.ietf.org/html/draft-ietf-oauth-v2-24#section-4.3>4.3.
 *      Resource Owner Password Credentials Grant</a>
 */
public class PasswordServerResource extends AbstractFlow {

    @Post("form:json")
    public Representation represent(Representation entity) {
        Representation rep = null;
        client = getAuthenticatedClient();
        String username =
                OAuth2Utils.getRequestParameter(getRequest(), OAuth2.Params.USERNAME, String.class);
        String password =
                OAuth2Utils.getRequestParameter(getRequest(), OAuth2.Params.PASSWORD, String.class);

        // Authenticate ResourceOwner
        if (getContext().getDefaultVerifier() instanceof SecretVerifier) {
            if (Verifier.RESULT_VALID == ((SecretVerifier) getContext().getDefaultVerifier())
                    .verify(username, password.toCharArray())) {
                resourceOwner = new User(username, password.toCharArray());
            } else {
                throw OAuthProblemException.OAuthError.ACCESS_DENIED.handle(getRequest());
            }
        } else {
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(getRequest(),
                    "SecretVerifier is not set in the Context");
        }

        // Get the requested scope
        String scope_before =
                OAuth2Utils.getRequestParameter(getRequest(), OAuth2.Params.SCOPE, String.class);
        // Validate the granted scope
        Set<String> checkedScope =
                getCheckedScope(scope_before, client.getClient().allowedGrantScopes(), client
                        .getClient().defaultGrantScopes());

        AccessToken token = createAccessToken(checkedScope);
        Map<String, Object> result = token.convertToMap();

        // TODO Conditional
        RefreshToken refreshToken = createRefreshToken(checkedScope);
        result.put(OAuth2.Params.REFRESH_TOKEN, refreshToken.getToken());

        return new JacksonRepresentation<Map>(result);
    }

    @Override
    protected String[] getRequiredParameters() {
        return new String[] { OAuth2.Params.GRANT_TYPE, OAuth2.Params.USERNAME,
            OAuth2.Params.PASSWORD };
    }

    /**
     * This method is intended to be overridden by subclasses.
     * 
     * @param checkedScope
     * @return
     * @throws org.forgerock.restlet.ext.oauth2.OAuthProblemException
     * 
     */
    protected AccessToken createAccessToken(Set<String> checkedScope) {
        return getTokenStore().createAccessToken(client.getClient().getAccessTokenType(),
                checkedScope, OAuth2Utils.getContextRealm(getContext()),
                resourceOwner.getIdentifier(), client.getClient().getClientId());
    }

    /**
     * This method is intended to be overridden by subclasses.
     * 
     * @param checkedScope
     * @return
     * @throws org.forgerock.restlet.ext.oauth2.OAuthProblemException
     * 
     */
    protected RefreshToken createRefreshToken(Set<String> checkedScope) {
        return getTokenStore().createRefreshToken(checkedScope,
                OAuth2Utils.getContextRealm(getContext()), resourceOwner.getIdentifier(),
                client.getClient().getClientId());
    }
}
