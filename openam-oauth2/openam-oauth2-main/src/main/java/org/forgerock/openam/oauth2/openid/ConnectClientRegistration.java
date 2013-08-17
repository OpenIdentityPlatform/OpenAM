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
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.openam.oauth2.openid;

import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdType;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.OAuth2Constants;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.openam.oauth2.model.ClientApplication;
import org.forgerock.openam.oauth2.model.impl.ClientApplicationImpl;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.ServerResource;

import java.security.AccessController;
import java.util.*;

public class ConnectClientRegistration extends ServerResource {

    ClientApplication oauth2client = null;

    public ConnectClientRegistration (){
    }

    public ConnectClientRegistration (ClientApplication app){
        this.oauth2client = app;
    }

    @Get
    public Representation readRequest(){
        String clientId= OAuth2Utils.getRequestParameter(
                getRequest(), OAuth2Constants.OAuth2Client.CLIENT_ID, String.class);
        String accessToken = getRequest().getChallengeResponse().getRawValue();

        if (clientId != null){
            try {
                AMIdentity client = OAuth2Utils.getClientIdentity(clientId, OAuth2Utils.getRealm(getRequest()));
                ClientApplication oauth2client;
                if (this.oauth2client != null){
                    oauth2client = this.oauth2client;
                } else {
                    oauth2client = new ClientApplicationImpl(client);
                }
                if (!oauth2client.getAccessToken().equals(accessToken)){
                    OAuth2Utils.DEBUG.error("ConnectClientRegistration.readRequest(): Invalid accessToken");
                    throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(getRequest());
                }
                Map<String, Object> responseMap = createReadResponse(oauth2client);
                return new JsonRepresentation(responseMap);
            } catch (Exception e) {
                OAuth2Utils.DEBUG.error("ConnectClientRegistration.Validate(): Unable to create client", e);
                throw OAuthProblemException.OAuthError.INVALID_CLIENT_METADATA.handle(getRequest());
            }
        } else {
            OAuth2Utils.DEBUG.error("ConnectClientRegistration.readRequest(): No client id sent");
            throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(getRequest());
        }
    }

    private Map<String, Object> createReadResponse(ClientApplication oauth2Client){
        Map<String, Object> response = new HashMap<String, Object>();
        response.put(OAuth2Constants.OAuth2Client.REDIRECT_URI, oauth2Client.getRedirectionURIs());
        response.put(OAuth2Constants.OAuth2Client.SCOPES, oauth2Client.getAllowedGrantScopes());
        response.put(OAuth2Constants.OAuth2Client.DEFAULT_SCOPES, oauth2Client.getDefaultGrantScopes());
        response.put(OAuth2Constants.OAuth2Client.NAME, oauth2Client.getClientName());
        response.put(OAuth2Constants.OAuth2Client.DESCRIPTION, oauth2Client.getDisplayDescription());
        //response.put(OAuth2Constants.OAuth2Client.GRANT_TYPES, oauth2Client.getGrantTypes());
        //response.put(OAuth2Constants.OAuth2Client.RESPONSE_TYPES, oauth2Client.getResponseTypes());
        //response.put(OAuth2Constants.OAuth2Client.CONTACTS, oauth2Client.getContacts());
        //response.put(OAuth2Constants.OAuth2Client.LOGO_URI, oauth2Client.getLogoURI());
        //response.put(OAuth2Constants.OAuth2Client.TOKEN_ENDPOINT_AUTH_METHOD, oauth2Client.getTokenEndpointAuthMethod());
        //response.put(OAuth2Constants.OAuth2Client.POLICY_URI, oauth2Client.getPolicyURI());
        //response.put(OAuth2Constants.OAuth2Client.TOS_URI, oauth2Client.getTosURI());
        response.put(OAuth2Constants.OAuth2Client.JKWS_URI, oauth2Client.getJwksURI());
        //response.put(OAuth2Constants.OAuth2Client.SECTOR_IDENTIFIER_URI, oauth2Client.getSectorIdentifierURI());
        response.put(OAuth2Constants.OAuth2Client.SUBJECT_TYPE, oauth2Client.getSubjectType());
        //response.put(OAuth2Constants.OAuth2Client.REQUEST_OBJECT_SIGNING_ALG, oauth2Client.getRequestObjectSigningAlgorithm());
        //response.put(OAuth2Constants.OAuth2Client.USERINFO_SIGNED_RESPONSE_ALG, oauth2Client.getUserInfoSignedResponseAlgorithm());
        //response.put(OAuth2Constants.OAuth2Client.USERINFO_ENCRYPTED_RESPONSE_ALG, oauth2Client.getUserInfoEncryptedResposneAlgorithm());
        //response.put(OAuth2Constants.OAuth2Client.USERINFO_SIGN_AND_ENC_RESPONSE_ALG, oauth2Client.getUserInfoEncryptedResponseEncoding());
        response.put(OAuth2Constants.OAuth2Client.IDTOKEN_SIGNED_RESPONSE_ALG, oauth2Client.getIDTokenSignedResponseAlgorithm());
        //response.put(OAuth2Constants.OAuth2Client.IDTOKEN_ENCRYPTED_RESPONSE_ALG, oauth2Client.getIDTokenEncryptedResposneAlgorithm());
        //response.put(OAuth2Constants.OAuth2Client.IDTOKEN_ENC_AND_SIGNED_RESPONSE_ALG, oauth2Client.getIDTokenEncryptedResponseEncoding());
        //response.put(OAuth2Constants.OAuth2Client.DEFAULT_MAX_AGE, oauth2Client.getDefaultMaxAge());
        //response.put(OAuth2Constants.OAuth2Client.REQUIRE_AUTH_TIME, oauth2Client.getRequireAuthTime());
        //response.put(OAuth2Constants.OAuth2Client.DEFAULT_ACR_VALS, oauth2Client.getDefaultACRValues());
        //response.put(OAuth2Constants.OAuth2Client.INIT_LOGIN_URL, oauth2Client.getinitiateLoginURI());
        response.put(OAuth2Constants.OAuth2Client.POST_LOGOUT_URI, oauth2Client.getPostLogoutRedirectionURI());
        //response.put(OAuth2Constants.OAuth2Client.REQUEST_URLs, oauth2Client.getRequestURIS());
        response.put(OAuth2Constants.OAuth2Client.CLIENT_TYPE, oauth2Client.getClientType());
        response.put(OAuth2Constants.OAuth2Client.CLIENT_ID, oauth2Client.getClientId());

        // remove the entries that are null.
        for(Map.Entry entry : response.entrySet()){
            if (entry.getValue() == null){
                response.remove(entry.getKey());
            }
        }

        return response;
    }

    @Post
    public Representation validate(Representation entity) {

        JSONObject json = null;
        Map<String, Object> response = new HashMap<String, Object>();
        String realm = null, id = null, secret = null;
        try {
            json = new JSONObject(entity.getText());
            Iterator i = json.keys();
            while (i.hasNext()) {
                String key = i.next().toString();
                if (key.equalsIgnoreCase(OAuth2Constants.OAuth2Client.REALM)) {
                    Object o = json.get(OAuth2Constants.OAuth2Client.REALM);
                    if (o instanceof JSONArray) {
                        realm = (String) ((List) o).get(0);
                    } else if (o instanceof String) {
                        realm = (String) o;
                    } else {
                        OAuth2Utils.DEBUG.error("ConnectClientRegistration.Validate(): Input parameter " + key + "unrecognized");
                        throw OAuthProblemException.OAuthError.INVALID_CLIENT_METADATA.handle(getRequest());
                    }
                } else if (key.equalsIgnoreCase(OAuth2Constants.OAuth2Client.CLIENT_ID)) {
                    Object o = json.get(OAuth2Constants.OAuth2Client.CLIENT_ID);
                    if (o instanceof JSONArray) {
                        id = (String) ((List) o).get(0);
                    } else if (o instanceof String) {
                        id = (String) o;
                    } else {
                        OAuth2Utils.DEBUG.error("ConnectClientRegistration.Validate(): Input parameter " + key + "unrecognized");
                        throw OAuthProblemException.OAuthError.INVALID_CLIENT_METADATA.handle(getRequest());
                    }
                } else if (key.equalsIgnoreCase(OAuth2Constants.OAuth2Client.CLIENT_SECRET)) {
                    Object o = json.get(OAuth2Constants.OAuth2Client.CLIENT_SECRET);
                    if (o instanceof JSONArray) {
                        secret = (String) ((List) o).get(0);
                    } else if (o instanceof String) {
                        secret = (String) o;
                    } else {
                        OAuth2Utils.DEBUG.error("ConnectClientRegistration.Validate(): Input parameter " + key + "unrecognized");
                        throw OAuthProblemException.OAuthError.INVALID_CLIENT_METADATA.handle(getRequest());
                    }
                    response.put(OAuth2Constants.OAuth2Client.USERPASSWORD, json.get(OAuth2Constants.OAuth2Client.CLIENT_SECRET));
                } else {
                    response.put(translate(key), json.get(key));
                }
            }

        } catch (Exception e) {
            OAuth2Utils.DEBUG.error("ConnectClientRegistration.Validate(): Error parsing input", e);
            throw OAuthProblemException.OAuthError.INVALID_CLIENT_METADATA.handle(getRequest());
        }

        if (id == null) {
            id = UUID.randomUUID().toString();
        }

        if (secret == null) {
            secret = UUID.randomUUID().toString();
            try {
                response.put(OAuth2Constants.OAuth2Client.CLIENT_SECRET, secret);
            } catch (Exception e) {
                OAuth2Utils.DEBUG.error("ConnectClientRegistration.Validate(): Error adding client_secret", e);
                throw OAuthProblemException.OAuthError.INVALID_CLIENT_METADATA.handle(getRequest());
            }
        }

        Map<String, Set<String>> attrs = new HashMap<String, Set<String>>();
        for (Map.Entry mapEntry : response.entrySet()) {
            if (mapEntry.getValue() instanceof String) {
                Set<String> temp = new HashSet<String>();
                temp.add((String) mapEntry.getValue());
                attrs.put(translate((String) mapEntry.getKey()), temp);
            } else if (mapEntry.getValue() instanceof JSONArray) {
                JSONArray temp = (JSONArray)mapEntry.getValue();
                Set<String> set = new HashSet<String>();
                for (int i = 0; i < temp.length(); i++){
                    try {
                        set.add((String) temp.get(i));
                    } catch (JSONException e) {
                        OAuth2Utils.DEBUG.error("ConnectClientRegistration.Validate(): Unable to create client", e);
                        throw OAuthProblemException.OAuthError.INVALID_CLIENT_METADATA.handle(getRequest());
                    }
                }
                attrs.put(translate((String) mapEntry.getKey()), set);
            } else {
                OAuth2Utils.DEBUG.error("ConnectClientRegistration.Validate(): Unable to create client");
                throw OAuthProblemException.OAuthError.INVALID_CLIENT_METADATA.handle(getRequest());
            }
        }

        Set<String> temp = new HashSet<String>();
        temp.add("OAuth2Client");
        attrs.put("AgentType", temp);

        temp = new HashSet<String>();
        temp.add("Active");
        attrs.put("sunIdentityServerDeviceStatus", temp);

        try {
            SSOToken token = (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance());
            AMIdentityRepository repo = new AMIdentityRepository(token, realm);
            repo.createIdentity(IdType.AGENTONLY, id, attrs);
        } catch (Exception e) {
            OAuth2Utils.DEBUG.error("ConnectClientRegistration.Validate(): Unable to create client", e);
            throw OAuthProblemException.OAuthError.INVALID_CLIENT_METADATA.handle(getRequest());
        }

        Map<String, Object> map = new HashMap<String, Object>(response);
        map.put(OAuth2Constants.OAuth2Client.CLIENT_ID, id);

        map.put("registration_client_uri", OAuth2Utils.getDeploymentURL(getRequest()) + "/oauth2/connect/register?client_id=" + id);
        map.put("issued_at", System.currentTimeMillis() / 1000);

        // TODO add expire time if JWT is used as the secret
        map.put("expires_at", 0);
        return new JsonRepresentation(map);

    }

    private String translate(String key) {
        if (key.equalsIgnoreCase("redirect_uris")) {
            return OAuth2Constants.OAuth2Client.REDIRECT_URI;
        } else if (key.equalsIgnoreCase("response_types")) {
            //return OAuth2Constants.OAuth2Client.RESPONSE_TYPES;
        } else if (key.equalsIgnoreCase("grant_types")) {
            //return OAuth2Constants.OAuth2Client.GRANT_TYPES;
        } else if (key.equalsIgnoreCase("contacts")) {
            //return OAuth2Constants.OAuth2Client.CONTACTS;
        } else if (key.equalsIgnoreCase("client_name")) {
            return OAuth2Constants.OAuth2Client.NAME;
        } else if (key.equalsIgnoreCase("logo_uri")) {
            //return OAuth2Constants.OAuth2Client.LOGO_URI;
        } else if (key.equalsIgnoreCase("token_endpoint_auth_method")) {
            //return OAuth2Constants.OAuth2Client.TOKEN_ENDPOINT_AUTH_METHOD;
        } else if (key.equalsIgnoreCase("policy_uri")) {
            //return OAuth2Constants.OAuth2Client.POLICY_URI;
        } else if (key.equalsIgnoreCase("tos_uri")) {
            //return OAuth2Constants.OAuth2Client.TOS_URI;
        } else if (key.equalsIgnoreCase("jwks_uri")) {
            return OAuth2Constants.OAuth2Client.JKWS_URI;
        } else if (key.equalsIgnoreCase("sector_identifier_uri")) {
            //return OAuth2Constants.OAuth2Client.SECTOR_IDENTIFIER_URI;
        } else if (key.equalsIgnoreCase("subject_type")) {
            return OAuth2Constants.OAuth2Client.SUBJECT_TYPE;
        } else if (key.equalsIgnoreCase("request_object_signing_alg")) {
            //return OAuth2Constants.OAuth2Client.REQUEST_OBJECT_SIGNING_ALG;
        } else if (key.equalsIgnoreCase("userinfo_signed_response_alg")) {
            //return OAuth2Constants.OAuth2Client.USERINFO_SIGNED_RESPONSE_ALG;
        } else if (key.equalsIgnoreCase("userinfo_encrypted_response_alg")) {
            //return OAuth2Constants.OAuth2Client.USERINFO_ENCRYPTED_RESPONSE_ALG;
        } else if (key.equalsIgnoreCase("userinfo_encrypted_response_enc")) {
            //return OAuth2Constants.OAuth2Client.USERINFO_SIGN_AND_ENC_RESPONSE_ALG;
        } else if (key.equalsIgnoreCase("id_token_signed_response_alg")) {
            return OAuth2Constants.OAuth2Client.IDTOKEN_SIGNED_RESPONSE_ALG;
        } else if (key.equalsIgnoreCase("id_token_encrypted_response_alg")) {
            //return OAuth2Constants.OAuth2Client.IDTOKEN_ENCRYPTED_RESPONSE_ALG;
        } else if (key.equalsIgnoreCase("id_token_encrypted_response_enc")) {
            //return OAuth2Constants.OAuth2Client.IDTOKEN_ENC_AND_SIGNED_RESPONSE_ALG;
        } else if (key.equalsIgnoreCase("default_max_age")) {
            //return OAuth2Constants.OAuth2Client.DEFAULT_MAX_AGE;
        } else if (key.equalsIgnoreCase("require_auth_time")) {
            //return OAuth2Constants.OAuth2Client.REQUIRE_AUTH_TIME;
        } else if (key.equalsIgnoreCase("default_acr_values")) {
            //return OAuth2Constants.OAuth2Client.DEFAULT_ACR_VALS;
        } else if (key.equalsIgnoreCase("initiate_login_uri")) {
            //return OAuth2Constants.OAuth2Client.INIT_LOGIN_URL;
        } else if (key.equalsIgnoreCase("post_logout_redirect_uri")) {
            return OAuth2Constants.OAuth2Client.POST_LOGOUT_URI;
        } else if (key.equalsIgnoreCase("request_uris")) {
            //return OAuth2Constants.OAuth2Client.REQUEST_URLs;
        } else if (key.equalsIgnoreCase("registration_access_token")){
            return OAuth2Constants.OAuth2Client.ACCESS_TOKEN;
        } else if (key.equalsIgnoreCase(OAuth2Constants.OAuth2Client.USERPASSWORD)) {
            return OAuth2Constants.OAuth2Client.USERPASSWORD;
        } else if (key.equalsIgnoreCase(OAuth2Constants.OAuth2Client.CLIENT_TYPE)) {
            return OAuth2Constants.OAuth2Client.CLIENT_TYPE;
        } else if (key.equalsIgnoreCase(OAuth2Constants.OAuth2Client.SCOPES)) {
            return OAuth2Constants.OAuth2Client.SCOPES;
        } else if (key.equalsIgnoreCase(OAuth2Constants.OAuth2Client.DEFAULT_SCOPES)) {
            return OAuth2Constants.OAuth2Client.DEFAULT_SCOPES;
        } else if (key.equalsIgnoreCase(OAuth2Constants.OAuth2Client.REALM)) {
            return OAuth2Constants.OAuth2Client.REALM;
        } else if (key.equalsIgnoreCase(OAuth2Constants.OAuth2Client.CLIENT_SECRET)) {
            return OAuth2Constants.OAuth2Client.USERPASSWORD;
        } else if (key.equalsIgnoreCase(OAuth2Constants.OAuth2Client.CLIENT_SESSION_URI)) {
            return OAuth2Constants.OAuth2Client.CLIENT_SESSION_URI;
        } else {
            return key;
        }
        return key;
    }

    @Override
    protected void doCatch(Throwable throwable) {
        if (throwable instanceof OAuthProblemException) {
            OAuthProblemException exception = (OAuthProblemException) throwable;
            getResponse().setEntity(new JacksonRepresentation<Map>(exception.getErrorMessage()));
            getResponse().setStatus(exception.getStatus());
        }
    }
}
