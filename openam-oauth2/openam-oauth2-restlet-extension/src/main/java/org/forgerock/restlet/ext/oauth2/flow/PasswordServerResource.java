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
 * "Portions Copyrighted [2012] [ForgeRock Inc]"
 */

package org.forgerock.restlet.ext.oauth2.flow;

import java.util.Map;
import java.util.Set;

import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.openam.oauth2.model.AccessToken;
import org.forgerock.openam.oauth2.model.RefreshToken;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.security.SecretVerifier;
import org.restlet.security.User;
import org.restlet.security.Verifier;

/**
 * Implements the Resource Owner Password Credentials Flow
 * @see <a
 *      href="http://tools.ietf.org/html/rfc6749#section-4.3>4.3.
 *      Resource Owner Password Credentials Grant</a>
 */
public class PasswordServerResource extends AbstractFlow {

    @Post("form:json")
    public Representation represent(Representation entity) {
        Representation rep = null;
        client = getAuthenticatedClient();

        String username =
                OAuth2Utils.getRequestParameter(getRequest(), OAuth2Constants.Params.USERNAME, String.class);
        String password =
                OAuth2Utils.getRequestParameter(getRequest(), OAuth2Constants.Params.PASSWORD, String.class);

        // Authenticate ResourceOwner
        if (getContext().getDefaultVerifier() instanceof SecretVerifier) {
            if (Verifier.RESULT_VALID == ((SecretVerifier) getContext().getDefaultVerifier())
                    .verify(getRequest(), getResponse())) {
                resourceOwner = new User(username, password.toCharArray());
            } else {
                OAuth2Utils.DEBUG.error("Unable to verify user: " + username + "password: " + password);
                throw OAuthProblemException.OAuthError.ACCESS_DENIED.handle(getRequest());
            }
        } else {
            OAuth2Utils.DEBUG.error("SecretVerifier is not set in the Context");
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(getRequest(),
                    "SecretVerifier is not set in the Context");
        }

        // Get the requested scope
        String scope_before =
                OAuth2Utils.getRequestParameter(getRequest(), OAuth2Constants.Params.SCOPE, String.class);
        // Validate the granted scope
        Set<String> checkedScope = executeAccessTokenScopePlugin(scope_before);

        AccessToken token = null;
        Map<String, Object> result = null;

        if (checkIfRefreshTokenIsRequired(getRequest())){
            RefreshToken refreshToken = createRefreshToken(checkedScope);
            token = createAccessToken(checkedScope, refreshToken);
            result = token.convertToMap();
            result.put(OAuth2Constants.Params.REFRESH_TOKEN, refreshToken.getToken());
        } else {
            token = createAccessToken(checkedScope, null);
            result = token.convertToMap();
        }

        return new JacksonRepresentation<Map>(result);
    }

    @Override
    protected String[] getRequiredParameters() {
        return new String[] { OAuth2Constants.Params.GRANT_TYPE, OAuth2Constants.Params.USERNAME,
            OAuth2Constants.Params.PASSWORD };
    }

    /**
     * This method is intended to be overridden by subclasses.
     * 
     * @param checkedScope
     * @return
     * @throws org.forgerock.openam.oauth2.exceptions.OAuthProblemException
     * 
     */
    protected AccessToken createAccessToken(Set<String> checkedScope, RefreshToken token) {
        if (token == null){
            return getTokenStore().createAccessToken(client.getClient().getAccessTokenType(),
                    checkedScope, OAuth2Utils.getRealm(getRequest()),
                    resourceOwner.getIdentifier(), client.getClient().getClientId(), null);
        } else {
            return getTokenStore().createAccessToken(client.getClient().getAccessTokenType(),
                    checkedScope, OAuth2Utils.getRealm(getRequest()),
                    resourceOwner.getIdentifier(), client.getClient().getClientId(), token);
        }
    }

    /**
     * This method is intended to be overridden by subclasses.
     * 
     * @param checkedScope
     * @return
     * @throws org.forgerock.openam.oauth2.exceptions.OAuthProblemException
     * 
     */
    protected RefreshToken createRefreshToken(Set<String> checkedScope) {
        return getTokenStore().createRefreshToken(checkedScope,
                OAuth2Utils.getRealm(getRequest()), resourceOwner.getIdentifier(),
                client.getClient().getClientId());
    }
}
