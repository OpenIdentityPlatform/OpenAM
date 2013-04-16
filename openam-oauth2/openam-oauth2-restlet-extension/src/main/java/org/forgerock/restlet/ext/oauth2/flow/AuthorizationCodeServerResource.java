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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.sun.identity.shared.OAuth2Constants;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.openam.oauth2.model.AccessToken;
import org.forgerock.openam.oauth2.model.AuthorizationCode;
import org.forgerock.openam.oauth2.model.RefreshToken;
import org.restlet.data.Form;
import org.restlet.data.Reference;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.routing.Redirector;

/**
 * Implements the Authorization Code Flow
 *
 * @see <a href="http://tools.ietf.org/html/rfc6749#section-4.1">4.1.  Authorization Code Grant</a>
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
                        OAuth2Constants.Params.REDIRECT_URI, String.class));

        String approval_prompt =
                OAuth2Utils.getRequestParameter(getRequest(), OAuth2Constants.Custom.APPROVAL_PROMPT,
                        String.class);
        String decision = OAuth2Utils.getRequestParameter(getRequest(), OAuth2Constants.Custom.DECISION,
                String.class);

        if (!OAuth2Constants.Custom.ALLOW.equalsIgnoreCase(decision)) {
            /*
             * APPROVAL_PROMPT = true AND NOT (CLIENT.AUTO_GRANT)
             */
            // Build approval page data

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
                        OAuth2Constants.Params.REDIRECT_URI, String.class));
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
                            .getRequestParameter(getRequest(), OAuth2Constants.Params.SCOPE, String.class);
            // Validate the granted scope
            Set<String> checkedScope = executeAccessTokenScopePlugin(scope_after);

            // Generate Token resourceOwner, sessionClient, checkedScope,
            // customParameters
            AuthorizationCode token = createAuthorizationCode(checkedScope);

            Reference location = new Reference(sessionClient.getRedirectUri());
            location.addQueryParameter(OAuth2Constants.Params.CODE, token.getToken());
            String state =
                    OAuth2Utils
                            .getRequestParameter(getRequest(), OAuth2Constants.Params.STATE, String.class);
            if (OAuth2Utils.isNotBlank(state)) {
                location.addQueryParameter(OAuth2Constants.Params.STATE, state);
            }
            if (isScopeChanged()) {
                location.addQueryParameter(OAuth2Constants.Params.SCOPE, OAuth2Utils.join(checkedScope, OAuth2Utils
                        .getScopeDelimiter(getContext())));
            }
            Redirector cb =
                    new Redirector(getContext(), location.toString(), Redirector.MODE_CLIENT_FOUND);
            cb.handle(getRequest(), getResponse());
        } else {
            OAuth2Utils.DEBUG.warning("AuthorizationCodeServerResource::Resource Owner did not authorize the request");
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
                OAuth2Utils.getRequestParameter(getRequest(), OAuth2Constants.Params.CODE, String.class);

        AuthorizationCode code = null;
        try{
            code = getTokenStore().readAuthorizationCode(code_p);
        }
        catch (Exception e ){
            OAuth2Utils.DEBUG.error("AuthorizationCodeServerResource::Authorization code doesn't exist.");
            throw OAuthProblemException.OAuthError.INVALID_GRANT.handle(getRequest());
        }

        if (null == code) {
            OAuth2Utils.DEBUG.error("AuthorizationCodeServerResource::Authorization code doesn't exist.");
            throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(getRequest(),
                    "Authorization code doesn't exist.");
        } else if (code.isTokenIssued()) {
            invalidateTokens(code_p);
            getTokenStore().deleteAuthorizationCode(code_p);
            OAuth2Utils.DEBUG.error("AuthorizationCodeServerResource::Authorization code has been used");
            throw OAuthProblemException.OAuthError.INVALID_GRANT.handle(getRequest());
        } else {
            if (code.isExpired()) {
                OAuth2Utils.DEBUG.error("AuthorizationCodeServerResource::Authorization code expired.");
                throw OAuthProblemException.OAuthError.INVALID_CODE.handle(getRequest(),
                        "Authorization code expired.");
            }

            // Generate Token
            AccessToken token = createAccessToken(code);

            //set access token issued
            code.setIssued(true);
            getTokenStore().updateAuthorizationCode(code_p, code);
            Map<String, Object> response = token.convertToMap();
            if (checkIfRefreshTokenIsRequired(getRequest())){
                response.put(OAuth2Constants.Params.REFRESH_TOKEN, token.getRefreshToken());
            }
            return new JacksonRepresentation<Map>(response);
        }
    }

    // Get the decision [allow,deny]
    protected boolean getDecision(Representation entity) {
        if (!decisionIsAllow) {
            String decision =
                    OAuth2Utils.getRequestParameter(getRequest(), OAuth2Constants.Custom.DECISION,
                            String.class);
            if (OAuth2Constants.Custom.ALLOW.equalsIgnoreCase(decision)) {
                decisionIsAllow = true;
            } else {
                OAuth2Utils.DEBUG.error("AuthorizationCodeServerResource::Resource Owner did not authorize the request");
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
            return new String[] { OAuth2Constants.Params.RESPONSE_TYPE, OAuth2Constants.Params.CLIENT_ID };
        }
        case TOKEN_ENDPOINT: {
            return new String[] { OAuth2Constants.Params.GRANT_TYPE, OAuth2Constants.Params.CODE,
                OAuth2Constants.Params.REDIRECT_URI };
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
                OAuth2Utils.getRealm(getRequest()), resourceOwner.getIdentifier(),
                sessionClient);
    }

    protected RefreshToken createRefreshToken(AuthorizationCode code){
        resourceOwner = getAuthenticatedResourceOwner();
        return getTokenStore().createRefreshToken(code.getScope(),
                                                    OAuth2Utils.getRealm(getRequest()),
                                                    code.getUserID(),
                                                    sessionClient.getClientId(),
                                                    code);
    }

    /**
     * This method is intended to be overridden by subclasses.
     * 
     * @param code
     * @return
     * @throws OAuthProblemException
     */
    protected AccessToken createAccessToken(AuthorizationCode code) {
        if (checkIfRefreshTokenIsRequired(getRequest())){
            //create refresh token
            RefreshToken token = createRefreshToken(code);

            //pass in refresh token as parent of Access Token
            return getTokenStore().createAccessToken(client.getClient().getAccessTokenType(),
                    code.getScope(), token, OAuth2Utils.getRealm(getRequest()));
        } else {
            return getTokenStore().createAccessToken(client.getClient().getAccessTokenType(),
                    code.getScope(), code, OAuth2Utils.getRealm(getRequest()));
        }
    }

    protected Form getFormPost(Representation entity) {
        if (null == formPost) {
            formPost = new Form(entity);
        }
        return formPost;
    }

    private void invalidateTokens(String id){

        JsonValue token = getTokenStore().queryForToken(id);

        Set<HashMap<String,Set<String>>> list = (Set<HashMap<String,Set<String>>>) token.getObject();

        if (list != null && !list.isEmpty() ){
            for (HashMap<String,Set<String>> entry : list){
                if (entry.get("id") != null && !entry.get("id").isEmpty()){
                    String entryID = entry.get("id").iterator().next();
                    invalidateTokens(entry.get("id").iterator().next());
                    String type = null;
                    if (entry.get("type") != null){
                        type = entry.get("type").iterator().next();
                    }
                    deleteToken(type, entryID);
                }
            }
        }
    }

    private void deleteToken(String type, String id){
        if (type.equalsIgnoreCase(OAuth2Constants.Token.OAUTH_ACCESS_TOKEN)){
            getTokenStore().deleteAccessToken(id);
        } else if (type.equalsIgnoreCase(OAuth2Constants.Token.OAUTH_REFRESH_TOKEN)){
            getTokenStore().deleteRefreshToken(id);
        } else if (type.equalsIgnoreCase(OAuth2Constants.Params.CODE)){
            getTokenStore().deleteAuthorizationCode(id);
        } else {
            //shouldnt ever happen
        }
    }
}
