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


import com.sun.identity.idm.AMIdentity;
import com.sun.identity.shared.OAuth2Constants;
import edu.emory.mathcs.backport.java.util.Arrays;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.openam.oauth2.model.CoreToken;
import org.forgerock.openam.oauth2.provider.ResponseType;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.restlet.data.Form;
import org.restlet.data.Parameter;
import org.restlet.data.Reference;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.routing.Redirector;

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
                OAuth2Utils.getRequestParameter(getRequest(), OAuth2Constants.Params.STATE, String.class);

        // Get the requested scope
        String scope_before =
                OAuth2Utils.getRequestParameter(getRequest(), OAuth2Constants.Params.SCOPE, String.class);

        // Validate the granted scope
        Set<String> checkedScope = executeAuthorizationPageScopePlugin(scope_before);

        String prompt =
                OAuth2Utils.getRequestParameter(getRequest(), OAuth2Constants.Custom.PROMPT, String.class);

        Set<String> promptSet = null;
        if (prompt != null && !prompt.isEmpty()){
            String [] prompts = prompt.split(" ");
            if (prompts != null && prompts.length > 0){
                promptSet = new HashSet<String>(Arrays.asList(prompts));
            }
        }

        //check if there is an invalid response type
        String responseType =
                OAuth2Utils.getRequestParameter(getRequest(), OAuth2Constants.Params.RESPONSE_TYPE, String.class);
        if (responseType != null && !responseType.isEmpty()){
            if (!validResponseTypes(OAuth2Utils.stringToSet(responseType))){
                OAuth2Utils.DEBUG.warning("AuthorizeServerResource.represent(): Requested a response type that is not configured.");
                throw OAuthProblemException.OAuthError.UNSUPPORTED_RESPONSE_TYPE.handle(getRequest(), "Response type is not supported");
            }
        } else {
            OAuth2Utils.DEBUG.warning("AuthorizeServerResource.represent(): Requested a response type that is not configured.");
            throw OAuthProblemException.OAuthError.UNSUPPORTED_RESPONSE_TYPE.handle(getRequest(), "Response type is not supported");
        }

        //check for saved consent
        if (!savedConsent(resourceOwner.getIdentifier(), sessionClient.getClientId(), checkedScope) ||
                (promptSet != null && promptSet.contains("consent"))  ){
            return getPage("authorize.ftl", getDataModel(checkedScope));
        } else {
            //skip consent age if consent is saved
            Map<String, Object> attrs = getRequest().getAttributes();
            attrs.put(OAuth2Constants.Custom.DECISION, OAuth2Constants.Custom.ALLOW);
            getRequest().setAttributes(attrs);
            Representation rep = new JsonRepresentation(new HashMap());
            return represent(rep);
        }
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
            String save_consent =
                    OAuth2Utils.getRequestParameter(getRequest(), OAuth2Constants.Custom.SAVE_CONSENT, String.class);

            String scope_after =
                    OAuth2Utils.getRequestParameter(getRequest(), OAuth2Constants.Params.SCOPE, String.class);

            if (save_consent != null && save_consent.equalsIgnoreCase("on")){
                saveConsent(resourceOwner.getIdentifier(), sessionClient.getClientId(), scope_after);
            }

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
                throw OAuthProblemException.OAuthError.UNSUPPORTED_RESPONSE_TYPE.handle(getRequest(),
                        "Response type is not supported");
            } else {
                try {
                    for(String request: requestedResponseTypes){
                        String responseClass = responseTypes.get(request);
                        if (responseClass == null || responseClass.isEmpty()){
                            OAuth2Utils.DEBUG.warning("AuthorizeServerResource.represent(): Requested a response type that is not configured. response_type=" + request);
                            throw OAuthProblemException.OAuthError.UNSUPPORTED_RESPONSE_TYPE.handle(getRequest(),
                                    "Response type is not supported");
                        }
                        Class clazz = Class.forName(responseClass);
                        ResponseType classObj = (ResponseType) clazz.newInstance();
                        CoreToken token = classObj.createToken(data);
                        String paramName = classObj.getReturnLocation();
                        if (listOfTokens.containsKey(paramName)){
                            OAuth2Utils.DEBUG.error("AuthorizeServerResource.represent(): Returning multiple response types with the same url value");
                            throw OAuthProblemException.OAuthError.UNSUPPORTED_RESPONSE_TYPE.handle(getRequest(),
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
                    throw OAuthProblemException.OAuthError.UNSUPPORTED_RESPONSE_TYPE.handle(getRequest(),
                            "Response type is not supported");
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

    protected boolean savedConsent(String userid, String clientId, Set<String> scopes){
        String attribute = OAuth2Utils.getOAuth2ProviderSetting(OAuth2Constants.OAuth2ProviderService.SAVED_CONSENT_ATTRIBUTE,
                String.class,
                getRequest());

        AMIdentity id = OAuth2Utils.getIdentity(userid, OAuth2Utils.getRealm(getRequest()));
        Set<String> attributeSet = null;

        if (id != null){
            try {
                attributeSet = id.getAttribute(attribute);
            }
            catch (Exception e){
                OAuth2Utils.DEBUG.error("AuthorizeServerResource.saveConsent(): Unable to get profile attribute", e);
                return false;
            }
        }

        //check the values of the attribute set vs the scope and client requested
        //attribute set is in the form of client_id|scope1 scope2 scope3
        for(String consent : attributeSet){
            int loc = consent.indexOf(" ");
            String consentClientId = consent.substring(0, loc);
            String[] scopesArray = consent.substring(loc+1, consent.length()).split(" ");
            Set<String> consentScopes = null;
            if (scopesArray != null && scopesArray.length > 0){
                consentScopes = new HashSet<String>(Arrays.asList(scopesArray));
            } else {
                consentScopes = new HashSet<String>();
            }

            //if both the client and the scopes are identical to the saved consent then approve
            if (clientId.equals(consentClientId) && scopes.equals(consentScopes)){
                return true;
            }
        }

        return false;
    }

    protected void saveConsent(String userId, String clientId, String scopes){
        AMIdentity id = OAuth2Utils.getIdentity(userId, OAuth2Utils.getRealm(getRequest()));
        String consentAttribute =
                OAuth2Utils.getOAuth2ProviderSetting(OAuth2Constants.OAuth2ProviderService.SAVED_CONSENT_ATTRIBUTE,
                        String.class, getRequest());
        try {

            //get the current set of consents and add our new consent to it.
            Set<String> consents = new HashSet<String>(id.getAttribute(consentAttribute));
            StringBuilder sb = new StringBuilder();
            sb.append(clientId.trim()).append(" ").append(scopes.trim());
            consents.add(sb.toString());

            //update the user profile with our new consent settings
            Map<String, Set<String>> attrs = new HashMap<String, Set<String>>();
            attrs.put(consentAttribute, consents);
            id.setAttributes(attrs);
            id.store();
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("AuthorizeServerResource.saveConsent(): Unable to save consent ", e);
        }
    }

    private boolean validResponseTypes(Set<String> responseTypesRequested){

        Map<String, String> allResponseTypes = getResponseTypes(OAuth2Utils.getRealm(getRequest()));
        return  allResponseTypes.keySet().containsAll(responseTypesRequested);
    }

}
