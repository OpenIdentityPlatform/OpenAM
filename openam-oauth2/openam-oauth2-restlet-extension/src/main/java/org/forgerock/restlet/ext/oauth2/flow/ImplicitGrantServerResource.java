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

import com.sun.identity.shared.OAuth2Constants;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.forgerock.openam.oauth2.model.AccessToken;
import org.restlet.data.Form;
import org.restlet.data.Reference;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.routing.Redirector;

/**
 *
 * Implements the Implicit Flow
 *
 * @see <a href="http://tools.ietf.org/html/rfc6749#section-4.2">4.2.  Implicit Grant</a>
 */
public class ImplicitGrantServerResource extends AbstractFlow {

    protected boolean decision_allow = false;

    /**
     * Developers should note that some user-agents do not support the inclusion
     * of a fragment component in the HTTP "Location" response header field.
     * Such clients will require using other methods for redirecting the client
     * than a 3xx redirection response. For example, returning an HTML page
     * which includes a 'continue' button with an action linked to the
     * redirection URI.
     * <p/>
     * If TLS is not available, the authorization server SHOULD warn the
     * resource owner about the insecure endpoint prior to redirection.
     * 
     * @return
     */
    @Get("html")
    @Post("html")
    public Representation represent() {
        resourceOwner = getAuthenticatedResourceOwner();

        // Validate the client
        client = validateRemoteClient();
        // Validate Redirect URI throw exception
        sessionClient =
                client.getClientInstance(OAuth2Utils.getRequestParameter(getRequest(),
                        OAuth2Constants.Params.REDIRECT_URI, String.class));

        // The target contains the state
        String state =
                OAuth2Utils
                        .getRequestParameter(getRequest(), OAuth2Constants.Params.STATE, String.class);

        // Get the requested scope
        String scope_before =
                OAuth2Utils
                        .getRequestParameter(getRequest(), OAuth2Constants.Params.SCOPE, String.class);

        // Validate the granted scope
        Set<String> checkedScope = executeAuthorizationPageScopePlugin(scope_before);

        return getPage("authorize.ftl", getDataModel(checkedScope));
    }

    @Post("form:json")
    public Representation represent(Representation entity) {
        resourceOwner = getAuthenticatedResourceOwner();
        client = validateRemoteClient();
        sessionClient =
                client.getClientInstance(OAuth2Utils.getRequestParameter(getRequest(),
                        OAuth2Constants.Params.REDIRECT_URI, String.class));
        String scope_after =
            OAuth2Utils
                .getRequestParameter(getRequest(), OAuth2Constants.Params.SCOPE, String.class);

        String state =
                OAuth2Utils
                        .getRequestParameter(getRequest(), OAuth2Constants.Params.STATE, String.class);

        Set<String> checkedScope = executeAccessTokenScopePlugin(scope_after);

        AccessToken token = createAccessToken(checkedScope);
        Form tokenForm = tokenToForm(token.convertToMap());

        /*
         * scope OPTIONAL, if identical to the scope requested by the
         * client, otherwise REQUIRED. The scope of the access token as
         * described by Section 3.3.
         */
        if (isScopeChanged()) {
            tokenForm.add(OAuth2Constants.Params.SCOPE, OAuth2Utils.join(checkedScope, OAuth2Utils
                    .getScopeDelimiter(getContext())));
        }
        if (null != state) {
            tokenForm.add(OAuth2Constants.Params.STATE, state);
        }

        Reference redirectReference = new Reference(sessionClient.getRedirectUri());
        redirectReference.setFragment(tokenForm.getQueryString());

        Redirector dispatcher =
                new Redirector(getContext(), redirectReference.toString(),
                        Redirector.MODE_CLIENT_FOUND);
        dispatcher.handle(getRequest(), getResponse());
        return getResponseEntity();
    }

    @Override
    protected String[] getRequiredParameters() {
        return new String[] { OAuth2Constants.Params.RESPONSE_TYPE, OAuth2Constants.Params.CLIENT_ID };
    }

    /**
     * This method is intended to be overridden by subclasses.
     * 
     * @param checkedScope
     * @return
     * @throws org.forgerock.openam.oauth2.exceptions.OAuthProblemException
     * 
     */
    protected AccessToken createAccessToken(Set<String> checkedScope) {
        return getTokenStore().createAccessToken(client.getClient().getAccessTokenType(),
                checkedScope, OAuth2Utils.getRealm(getRequest()),
                resourceOwner.getIdentifier(), sessionClient);
    }

    protected Form tokenToForm(Map<String, Object> token) {
        Form result = new Form();
        for (Map.Entry<String, Object> entry : token.entrySet()) {
            result.add(entry.getKey(), entry.getValue().toString());
        }
        return result;
    }

}
