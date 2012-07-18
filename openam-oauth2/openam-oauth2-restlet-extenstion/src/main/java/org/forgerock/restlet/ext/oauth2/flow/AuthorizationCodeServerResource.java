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
import org.forgerock.restlet.ext.oauth2.model.AuthorizationCode;
import org.restlet.data.Form;
import org.restlet.data.Reference;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.routing.Redirector;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class AuthorizationCodeServerResource extends AbstractFlow {

    protected boolean decisionIsAllow = false;
    protected Form formPost = null;

    /*
     * If TLS is not available, the authorization server SHOULD warn the
     * resource owner about the insecure endpoint prior to redirection.
     */
    @Get("html")
    public Representation represent() {
        /*
         * The authorization server validates the request to ensure all required
         * parameters are present and valid. If the request is valid, the
         * authorization server authenticates the resource owner and obtains an
         * authorization decision (by asking the resource owner or by
         * establishing approval via other means).
         */
        resourceOwner = getAuthenticatedResourceOwner();
        client = validateRemoteClient();
        // Validate Redirect URI throw exception
        sessionClient =
                client.getClientInstance(OAuth2Utils.getRequestParameter(getRequest(),
                        OAuth2.Params.REDIRECT_URI, String.class));

        String approval_prompt =
                OAuth2Utils.getRequestParameter(getRequest(), OAuth2.Custom.APPROVAL_PROMPT,
                        String.class);
        String decision = OAuth2Utils.getRequestParameter(getRequest(), OAuth2.Custom.DECISION,
                String.class);

        if (!OAuth2.Custom.ALLOW.equalsIgnoreCase(decision)) {
            /*
             * APPROVAL_PROMPT = true AND NOT (CLIENT.AUTO_GRANT)
             */
            // Build approval page data

            // The target contains the state
            String state =
                    OAuth2Utils
                            .getRequestParameter(getRequest(), OAuth2.Params.STATE, String.class);

            // Get the requested scope
            String scope_before =
                    OAuth2Utils
                            .getRequestParameter(getRequest(), OAuth2.Params.SCOPE, String.class);
            // Validate the granted scope
            Set<String> checkedScope =
                    getCheckedScope(scope_before, client.getClient().allowedGrantScopes(), client
                            .getClient().defaultGrantScopes());

            return getPage("authorize.ftl", getDataModel());
        } else {
            decisionIsAllow = true;
            return authorization(getRequest().getEntity());
        }
    }

    @Post("form:json")
    public Representation represent(Representation entity) {
        // Validate the client
        client = validateRemoteClient();
        // Validate Redirect URI throw exception
        sessionClient =
                client.getClientInstance(OAuth2Utils.getRequestParameter(getRequest(),
                        OAuth2.Params.REDIRECT_URI, String.class));
        switch (endpointType) {
        case AUTHORIZATION_ENDPOINT: {
            resourceOwner = getAuthenticatedResourceOwner();

            return authorization(entity);
        }
        case TOKEN_ENDPOINT: {
            return token(entity);
        }
        default: {
            return null;
        }
        }
    }

    public Representation authorization(Representation entity) {
        /*
         * When a decision is established, the authorization server directs the
         * user-agent to the provided client redirection URI using an HTTP
         * redirection response, or by other means available to it via the user-
         * agent.
         */
        if (getDecision(entity)) {
            // Get the granted scope
            String scope_after =
                    OAuth2Utils
                            .getRequestParameter(getRequest(), OAuth2.Params.SCOPE, String.class);
            // Validate the granted scope
            Set<String> checkedScope =
                    getCheckedScope(scope_after, client.getClient().allowedGrantScopes(), client
                            .getClient().defaultGrantScopes());

            // Generate Token resourceOwner, sessionClient, checkedScope,
            // customParameters
            AuthorizationCode token = createAuthorizationCode(checkedScope);

            Reference location = new Reference(sessionClient.getRedirectUri());
            location.addQueryParameter(OAuth2.Params.CODE, token.getToken());
            String state =
                    OAuth2Utils
                            .getRequestParameter(getRequest(), OAuth2.Params.STATE, String.class);
            if (OAuth2Utils.isNotBlank(state)) {
                location.addQueryParameter(OAuth2.Params.STATE, state);
            }
            Redirector cb =
                    new Redirector(getContext(), location.toString(), Redirector.MODE_CLIENT_FOUND);
            cb.handle(getRequest(), getResponse());
        } else {
            throw OAuthProblemException.OAuthError.ACCESS_DENIED.handle(getRequest(),
                    "Resource Owner did not authorize the request");
        }
        return getResponseEntity();
    }

    public Representation token(Representation entity) {
        /*
         * The authorization server MUST:
         * 
         * o require client authentication for confidential clients or for any
         * client that was issued client credentials (or with other
         * authentication requirements), o authenticate the client if client
         * authentication is included and ensure the authorization code was
         * issued to the authenticated client, o verify that the authorization
         * code is valid, and o ensure that the "redirect_uri" parameter is
         * present if the "redirect_uri" parameter was included in the initial
         * authorization request as described in Section 4.1.1, and if included
         * ensure their values are identical.
         */

        // Find code
        String code_p =
                OAuth2Utils.getRequestParameter(getRequest(), OAuth2.Params.CODE, String.class);
        AuthorizationCode code = getTokenStore().readAuthorizationCode(code_p);

        if (null == code) {
            throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(getRequest(),
                    "Authorization code has been user.");
        } else if (code.isTokenIssued()) {
            // TODO throw Exception
            throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(getRequest(),
                    "Authorization code has been user.");
        } else {
            // TODO Token expire check
            if (code.getExpireTime() - System.currentTimeMillis() < 0 || code.isExpired()) {
                // Throw expired code
                throw OAuthProblemException.OAuthError.INVALID_CODE.handle(getRequest(),
                        "Authorization code expired.");
            }
            // TODO validate redirect URI and ClientID
            if (!code.getClient().equals(sessionClient)) {
                // Throw redirect_uri mismatch
            }

            // Generate Token
            AccessToken token = createAccessToken(code);
            Map<String, Object> response = token.convertToMap();
            return new JacksonRepresentation<Map>(response);
        }
    }

    // Get the decision [allow,deny]
    protected boolean getDecision(Representation entity) {
        if (!decisionIsAllow) {
            String decision =
                    OAuth2Utils.getRequestParameter(getRequest(), OAuth2.Custom.DECISION,
                            String.class);
            if (OAuth2.Custom.ALLOW.equalsIgnoreCase(decision)) {
                decisionIsAllow = true;
            } else {
                throw OAuthProblemException.OAuthError.ACCESS_DENIED.handle(getRequest(),
                        "Resource Owner did not authorize the request");
            }
        }
        return decisionIsAllow;
    }

    @Override
    protected String[] getRequiredParameters() {
        Set<String> required = null;
        switch (endpointType) {
        case AUTHORIZATION_ENDPOINT: {
            return new String[] { OAuth2.Params.RESPONSE_TYPE, OAuth2.Params.CLIENT_ID };
        }
        case TOKEN_ENDPOINT: {
            return new String[] { OAuth2.Params.GRANT_TYPE, OAuth2.Params.CODE,
                OAuth2.Params.REDIRECT_URI };
        }
        default: {
            return null;
        }
        }
    }

    /**
     * This method is intended to be overridden by subclasses.
     * 
     * @param checkedScope
     * @return
     * @throws OAuthProblemException
     */
    protected AuthorizationCode createAuthorizationCode(Set<String> checkedScope) {
        return getTokenStore().createAuthorizationCode(checkedScope,
                OAuth2Utils.getContextRealm(getContext()), resourceOwner.getIdentifier(),
                sessionClient);
    }

    /**
     * This method is intended to be overridden by subclasses.
     * 
     * @param code
     * @return
     * @throws OAuthProblemException
     */
    protected AccessToken createAccessToken(AuthorizationCode code) {
        return getTokenStore().createAccessToken(client.getClient().getAccessTokenType(),
                code.getScope(), code);
    }

    protected Form getFormPost(Representation entity) {
        if (null == formPost) {
            formPost = new Form(entity);
        }
        return formPost;
    }
}
