/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock Inc. All rights reserved.
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


import com.sun.identity.shared.OAuth2Constants;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.openam.oauth2.model.CoreToken;
import org.forgerock.openam.oauth2.provider.ResponseType;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.restlet.data.Form;
import org.restlet.data.Parameter;
import org.restlet.data.Reference;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.routing.Redirector;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AuthorizeServerResource extends AbstractFlow {

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

        String decision = OAuth2Utils.getRequestParameter(getRequest(), OAuth2Constants.Custom.DECISION,
                String.class);

        if (OAuth2Constants.Custom.ALLOW.equalsIgnoreCase(decision)) {
            String scope_after =
                    OAuth2Utils
                            .getRequestParameter(getRequest(), OAuth2Constants.Params.SCOPE, String.class);

            String state =
                    OAuth2Utils
                            .getRequestParameter(getRequest(), OAuth2Constants.Params.STATE, String.class);

            Set<String> checkedScope = executeAccessTokenScopePlugin(scope_after);


            Map<String, String> responseTypes = null;
            responseTypes = getResponseTypes(OAuth2Utils.getRealm(getRequest()));

            Set<String> requestedResponseTypes = OAuth2Utils.stringToSet(OAuth2Utils.getRequestParameter(getRequest(), OAuth2Constants.Params.RESPONSE_TYPE, String.class));
            Map<String, CoreToken> listOfTokens = new HashMap<String, CoreToken>();
            Map<String, Object> data = new HashMap<String, Object>();
            data.put(OAuth2Constants.CoreTokenParams.TOKEN_TYPE, client.getClient().getAccessTokenType());
            data.put(OAuth2Constants.CoreTokenParams.SCOPE, checkedScope);
            data.put(OAuth2Constants.CoreTokenParams.REALM, OAuth2Utils.getRealm(getRequest()));
            data.put(OAuth2Constants.CoreTokenParams.USERNAME, resourceOwner.getIdentifier());
            data.put(OAuth2Constants.CoreTokenParams.CLIENT_ID, sessionClient.getClientId());
            data.put(OAuth2Constants.CoreTokenParams.REDIRECT_URI, sessionClient.getRedirectUri());


            if (requestedResponseTypes == null || requestedResponseTypes.isEmpty()){
                OAuth2Utils.DEBUG.error("AuthorizeServerResource.represent(): Error response_type not set");
                OAuthProblemException.OAuthError.INVALID_REQUEST.handle(getRequest(),
                        "No response type set");
            } else {
                try {
                    for(String request: requestedResponseTypes){
                        String responseClass = responseTypes.get(request);
                        if (responseClass == null || responseClass.isEmpty()){
                            OAuth2Utils.DEBUG.warning("AuthorizeServerResource.represent(): Requested a response type that is not configured. response_type=" + request);
                            continue;
                        }
                        Class clazz = Class.forName(responseClass);
                        ResponseType classObj = (ResponseType) clazz.newInstance();
                        CoreToken token = classObj.createToken(data);
                        String paramName = classObj.getReturnLocation();
                        if (listOfTokens.containsKey(paramName)){
                            OAuth2Utils.DEBUG.error("AuthorizeServerResource.represent(): Error response_type not set");
                            OAuthProblemException.OAuthError.INVALID_REQUEST.handle(getRequest(),
                                    "Returning multiple response types with the same url value");
                        }
                        listOfTokens.put(classObj.URIParamValue(), token);

                        if (fragment == false){
                            String location = classObj.getReturnLocation();
                            if (location.equalsIgnoreCase("FRAGMENT")){
                                fragment = true;
                            }
                        }

                    }
                } catch (Exception e){
                    OAuth2Utils.DEBUG.error("AuthorizeServerResource.represent(): Error invoking classes for response_type", e);
                    OAuthProblemException.OAuthError.UNSUPPORTED_RESPONSE_TYPE.handle(getRequest(),
                            "Error invoking classes for response types");
                }
            }

            Form tokenForm = tokensToForm(listOfTokens);

            //execute post token creation pre return scope plugin for extra return data.
            Map<String, String> extraData = new HashMap<String, String>();
            String nonce = OAuth2Utils.getRequestParameter(getRequest(), OAuth2Constants.Custom.NONCE, String.class);
            extraData.put(OAuth2Constants.Custom.NONCE, nonce);
            extraData.put(OAuth2Constants.Params.RESPONSE_TYPE, OAuth2Utils.getRequestParameter(getRequest(), OAuth2Constants.Params.RESPONSE_TYPE, String.class));
            Map<String, String> valuesToAdd = executeAuthorizationExtraDataScopePlugin(extraData, listOfTokens);
            if (valuesToAdd != null && !valuesToAdd.isEmpty()){
                String returnType = valuesToAdd.remove("returnType");
                if(returnType != null && !returnType.isEmpty()){
                    if (returnType.equalsIgnoreCase("FRAGMENT")){
                        fragment = true;
                    }
                }
            }
            for(Map.Entry<String, String> entry : valuesToAdd.entrySet()){
                    tokenForm.add(entry.getKey(), entry.getValue().toString());
            }

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

            if (fragment){
                redirectReference.setFragment(tokenForm.getQueryString());
            } else {
                redirectReference.setQuery(tokenForm.getQueryString());
            }

            Redirector dispatcher =
                    new Redirector(getContext(), redirectReference.toString(),
                            Redirector.MODE_CLIENT_FOUND);
            dispatcher.handle(getRequest(), getResponse());
        } else {
            OAuth2Utils.DEBUG.warning("AuthorizeServerResource::Resource Owner did not authorize the request");
            throw OAuthProblemException.OAuthError.ACCESS_DENIED.handle(getRequest(),
                    "Resource Owner did not authorize the request");
        }
        return getResponseEntity();
    }

    @Override
    protected String[] getRequiredParameters() {
        return new String[] { OAuth2Constants.Params.RESPONSE_TYPE, OAuth2Constants.Params.CLIENT_ID };
    }

    protected Form tokensToForm(Map<String, CoreToken> tokens) {

        Form result = new Form();
        for (Map.Entry<String, CoreToken> entry : tokens.entrySet()){
            Map<String,Object> token = entry.getValue().convertToMap();
            Parameter p = new Parameter(entry.getKey(), entry.getValue().getTokenID());
            if (!result.contains(p)){
                result.add(p);
            }
            //if access token add extra fields
            if (entry.getValue().getTokenName().equalsIgnoreCase(OAuth2Constants.Params.ACCESS_TOKEN)){
                for (Map.Entry<String, Object> entryInMap : token.entrySet()) {
                    p = new Parameter(entryInMap.getKey(), entryInMap.getValue().toString());
                    if (!result.contains(p)){
                        result.add(p);
                    }
                }
            }

        }
        return result;
    }

}
