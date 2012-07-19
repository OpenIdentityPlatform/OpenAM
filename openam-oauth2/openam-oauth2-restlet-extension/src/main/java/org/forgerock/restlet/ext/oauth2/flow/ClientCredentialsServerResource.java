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
import org.forgerock.restlet.ext.oauth2.model.AccessToken;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;

/**
 * @see <a
 *      href="http://tools.ietf.org/html/draft-ietf-oauth-v2-24#section-4.4>4.4.
 *      Client Credentials Grant</a>
 */
public class ClientCredentialsServerResource extends AbstractFlow {

    @Post("form:json")
    public Representation represent(Representation entity) {
        Representation rep = null;
        client = getAuthenticatedClient();

        // Get the requested scope
        String scope_before =
                OAuth2Utils.getRequestParameter(getRequest(), OAuth2.Params.SCOPE, String.class);
        // Validate the granted scope
        Set<String> checkedScope =
                getCheckedScope(scope_before, client.getClient().allowedGrantScopes(), client
                        .getClient().defaultGrantScopes());

        AccessToken token = createAccessToken(checkedScope);

        return new JacksonRepresentation<Map>(token.convertToMap());
    }

    @Override
    protected String[] getRequiredParameters() {
        return new String[] { OAuth2.Params.GRANT_TYPE };
    }

    /**
     * This method is intended to be overridden by subclasses.
     * 
     * @param checkedScope
     * @return
     * @throws org.forgerock.restlet.ext.oauth2.OAuthProblemException
     * 
     */
    private AccessToken createAccessToken(Set<String> checkedScope) {
        return getTokenStore().createAccessToken(client.getClient().getAccessTokenType(),
                checkedScope, OAuth2Utils.getContextRealm(getContext()),
                client.getClient().getClientId());
    }
}
