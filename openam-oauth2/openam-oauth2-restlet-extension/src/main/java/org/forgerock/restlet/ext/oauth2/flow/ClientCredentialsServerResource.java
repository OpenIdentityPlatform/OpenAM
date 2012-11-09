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
import org.forgerock.openam.oauth2.model.AccessToken;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;

/**
 * Implements the Client Credentials Flow
 * @see <a
 *      href="http://tools.ietf.org/html/rfc6749#section-4.4>4.4.
 *      Client Credentials Grant</a>
 */
public class ClientCredentialsServerResource extends AbstractFlow {

    @Post("form:json")
    public Representation represent(Representation entity) {
        Representation rep = null;
        client = getAuthenticatedClient();

        // Get the requested scope
        String scope_before =
                OAuth2Utils.getRequestParameter(getRequest(), OAuth2Constants.Params.SCOPE, String.class);
        // Validate the granted scope
        Set<String> checkedScope = executeAccessTokenScopePlugin(scope_before);

        AccessToken token = createAccessToken(checkedScope);

        return new JacksonRepresentation<Map>(token.convertToMap());
    }

    @Override
    protected String[] getRequiredParameters() {
        return new String[] { OAuth2Constants.Params.GRANT_TYPE };
    }

    /**
     * This method is intended to be overridden by subclasses.
     * 
     * @param checkedScope
     * @return
     * @throws org.forgerock.openam.oauth2.exceptions.OAuthProblemException
     * 
     */
    private AccessToken createAccessToken(Set<String> checkedScope) {
        return getTokenStore().createAccessToken(client.getClient().getAccessTokenType(),
                checkedScope, OAuth2Utils.getRealm(getRequest()),
                client.getClient().getClientId());
    }
}
