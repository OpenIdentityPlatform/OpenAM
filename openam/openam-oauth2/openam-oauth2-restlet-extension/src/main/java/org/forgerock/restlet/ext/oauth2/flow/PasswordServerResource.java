/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock AS. All rights reserved.
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
 * "Portions copyright [year] [name of copyright owner]"
 */

package org.forgerock.restlet.ext.oauth2.flow;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sun.identity.shared.OAuth2Constants;
import org.forgerock.openam.oauth2.model.CoreToken;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.security.SecretVerifier;
import org.restlet.security.User;
import org.restlet.security.Verifier;

/**
 * Implements the Resource Owner Password Credentials Flow
 *
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
                OAuth2Utils.DEBUG.error("Unable to verify user: " + username);
                throw OAuthProblemException.OAuthError.INVALID_GRANT.handle(getRequest());
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

        CoreToken token = null;
        Map<String, Object> result = null;

        if (checkIfRefreshTokenIsRequired(getRequest())) {
            CoreToken refreshToken = createRefreshToken(checkedScope);
            token = createAccessToken(checkedScope, refreshToken);
            result = token.convertToMap();
            result.put(OAuth2Constants.Params.REFRESH_TOKEN, refreshToken.getTokenID());
        } else {
            token = createAccessToken(checkedScope, null);
            result = token.convertToMap();
        }

        //execute post token creation pre return scope plugin for extra return data.
        Map<String, String> data = new HashMap<String, String>();
        result.putAll(executeExtraDataScopePlugin(data, token));

        if (checkedScope != null && !checkedScope.isEmpty()) {
            result.put(OAuth2Constants.Params.SCOPE, OAuth2Utils.join(checkedScope,
                    OAuth2Utils.getScopeDelimiter(getContext())));
        }

        return new JacksonRepresentation<Map>(result);
    }

    @Override
    protected String[] getRequiredParameters() {
        return new String[]{OAuth2Constants.Params.GRANT_TYPE, OAuth2Constants.Params.USERNAME,
                OAuth2Constants.Params.PASSWORD};
    }

    /**
     * This method is intended to be overridden by subclasses.
     *
     * @param checkedScope
     * @return
     * @throws org.forgerock.openam.oauth2.exceptions.OAuthProblemException
     *
     */
    protected CoreToken createAccessToken(Set<String> checkedScope, CoreToken token) {
        if (token == null) {
            return getTokenStore().createAccessToken(client.getClient().getAccessTokenType(),
                    checkedScope, OAuth2Utils.getRealm(getRequest()),
                    resourceOwner.getIdentifier(), client.getClient().getClientId(), null, null, null);
        } else {
            return getTokenStore().createAccessToken(client.getClient().getAccessTokenType(),
                    checkedScope, OAuth2Utils.getRealm(getRequest()),
                    resourceOwner.getIdentifier(), client.getClient().getClientId(), null, null, token.getTokenID());
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
    protected CoreToken createRefreshToken(Set<String> checkedScope) {
        return getTokenStore().createRefreshToken(checkedScope,
                OAuth2Utils.getRealm(getRequest()), resourceOwner.getIdentifier(),
                client.getClient().getClientId(), null);
    }
}
