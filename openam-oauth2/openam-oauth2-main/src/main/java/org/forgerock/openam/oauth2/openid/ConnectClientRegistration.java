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

/*
 * Portions Copyrighted 2013 Nomura Research Institute, Ltd
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

    Map<String, String> translationMap = new HashMap<String, String>();
    Map<String, String> reverseTranslationMap = new HashMap<String, String>();

    private static final String REDIRECT_URIS = "redirect_uris";
    private static final String RESPONSE_TYPES = "response_types";
    private static final String GRANT_TYPES = "grant_types";
    private static final String APPLICATION_TYPE = "application_type";
    private static final String CONTACTS = "contacts";
    private static final String CLIENT_NAME = "client_name";
    private static final String LOGO_URI = "logo_uri";
    private static final String CLIENT_URI = "client_uri";
    private static final String POLICY_URI = "policy_uri";
    private static final String TOS_URI = "tos_uri";
    private static final String JWKS_URI = "jwks_uri";
    private static final String JWKS = "jwks";
    private static final String SECTOR_IDENTIFIER_URI = "sector_identifier_uri";
    private static final String SUBJECT_TYPE = "subject_type";
    private static final String ID_TOKEN_SIGNED_RESPONSE_ALG = "id_token_signed_response_alg";
    private static final String ID_TOKEN_ENCRYPTED_RESPONSE_ALG = "id_token_encrypted_response_alg";
    private static final String ID_TOKEN_ENCRYPTED_RESONSE_ENC = "id_token_encrypted_response_enc";
    private static final String USERINFO_SIGNED_RESPONSE_ALG = "userinfo_signed_response_alg";
    private static final String USERINFO_ENCRYPTED_RESPONSE_ALG = "userinfo_encrypted_response_alg";
    private static final String USERINFO_ENCRYPTED_RESONSE_ENC = "userinfo_encrypted_response_enc";
    private static final String REQUEST_OBJECT_SIGNING_ALG = "request_object_signing_alg";
    private static final String REQUEST_OBJECT_ENCRYPTION_ALG = "request_object_encryption_alg";
    private static final String REQUEST_OBJECT_ENCRYPTION_ENC = "request_object_encryption_enc";
    private static final String TOKEN_ENDPOINT_AUTH_METHOD = "token_endpoint_auth_method";
    private static final String TOKEN_ENDPOINT_AUTH_SIGNING_ALG = "token_endpoint_auth_signing_alg";
    private static final String DEFAULT_MAX_AGE = "default_max_age";
    private static final String REQUIRE_AUTH_TIME = "require_auth_time";
    private static final String DEFAULT_ACR_VALUES = "default_acr_values";
    private static final String INITIATE_LOGIN_URI = "initiate_login_uri";
    private static final String REQUEST_URIS = "request_uris";
    private static final String POST_LOGOUT_REDIRECT_URIS = "post_logout_redirect_uris";
    private static final String REGISTRATION_ACCESS_TOKEN = "registration_access_token";
    private static final String CLIENT_SESSION_URI = "client_session_uri";

    private static final String CLIENT_ID = "client_id";
    private static final String CLIENT_SECRET = "client_secret";
    private static final String CLIENT_TYPE = "client_type";
    private static final String SCOPES = "scopes";
    private static final String DEFAULT_SCOPES = "default_scopes";
    private static final String CLIENT_DESCRIPTION = "client_description";

    private static final String NOT_USED = null;

    private static final String REGISTRATION_CLIENT_URI = "registration_client_uri";
    private static final String ISSUED_AT = "client_id_issued_at";
    private static final String EXPIRES_AT = "client_secret_expires_at";

    private static final String OAUTH2_CLIENT = "OAuth2Client";
    private static final String AGENT_TYPE = "AgentType";
    private static final String ACTIVE = "Active";
    private static final String SUN_IDENTITY_SERVER_DEVICE_STATUS = "sunIdentityServerDeviceStatus";


    public ConnectClientRegistration (){
        createTranslationMaps();
    }

    public ConnectClientRegistration (ClientApplication app){
        createTranslationMaps();
        this.oauth2client = app;
    }

    private void createTranslationMaps(){
        translationMap.put(REDIRECT_URIS, OAuth2Constants.OAuth2Client.REDIRECT_URI);
        translationMap.put(RESPONSE_TYPES, NOT_USED);
        translationMap.put(GRANT_TYPES, NOT_USED);
        translationMap.put(APPLICATION_TYPE, NOT_USED);
        translationMap.put(CONTACTS, NOT_USED);
        translationMap.put(CLIENT_NAME, OAuth2Constants.OAuth2Client.NAME);
        translationMap.put(LOGO_URI, NOT_USED);
        translationMap.put(CLIENT_URI, NOT_USED);
        translationMap.put(POLICY_URI, NOT_USED);
        translationMap.put(TOS_URI, NOT_USED);
        translationMap.put(JWKS_URI, NOT_USED);
        translationMap.put(JWKS, NOT_USED);
        translationMap.put(SECTOR_IDENTIFIER_URI, NOT_USED);
        translationMap.put(SUBJECT_TYPE, NOT_USED);
        translationMap.put(ID_TOKEN_SIGNED_RESPONSE_ALG, OAuth2Constants.OAuth2Client.IDTOKEN_SIGNED_RESPONSE_ALG);
        translationMap.put(ID_TOKEN_ENCRYPTED_RESPONSE_ALG, NOT_USED);
        translationMap.put(ID_TOKEN_ENCRYPTED_RESONSE_ENC, NOT_USED);
        translationMap.put(USERINFO_SIGNED_RESPONSE_ALG, NOT_USED);
        translationMap.put(USERINFO_ENCRYPTED_RESPONSE_ALG, NOT_USED);
        translationMap.put(USERINFO_ENCRYPTED_RESONSE_ENC, NOT_USED);
        translationMap.put(REQUEST_OBJECT_SIGNING_ALG, NOT_USED);
        translationMap.put(REQUEST_OBJECT_ENCRYPTION_ALG, NOT_USED);
        translationMap.put(REQUEST_OBJECT_ENCRYPTION_ENC, NOT_USED);
        translationMap.put(TOKEN_ENDPOINT_AUTH_METHOD, NOT_USED);
        translationMap.put(TOKEN_ENDPOINT_AUTH_SIGNING_ALG, NOT_USED);
        translationMap.put(DEFAULT_MAX_AGE, NOT_USED);
        translationMap.put(REQUIRE_AUTH_TIME, NOT_USED);
        translationMap.put(DEFAULT_ACR_VALUES, NOT_USED);
        translationMap.put(INITIATE_LOGIN_URI, NOT_USED);
        translationMap.put(REQUEST_URIS, NOT_USED);
        translationMap.put(POST_LOGOUT_REDIRECT_URIS, OAuth2Constants.OAuth2Client.POST_LOGOUT_URI);
        translationMap.put(REGISTRATION_ACCESS_TOKEN, OAuth2Constants.OAuth2Client.ACCESS_TOKEN);

        translationMap.put(CLIENT_SECRET, OAuth2Constants.OAuth2Client.USERPASSWORD);
        translationMap.put(CLIENT_TYPE, OAuth2Constants.OAuth2Client.CLIENT_TYPE);
        translationMap.put(SCOPES, OAuth2Constants.OAuth2Client.SCOPES);
        translationMap.put(DEFAULT_SCOPES, OAuth2Constants.OAuth2Client.DEFAULT_SCOPES);
        translationMap.put(CLIENT_DESCRIPTION, OAuth2Constants.OAuth2Client.DESCRIPTION);
        translationMap.put(CLIENT_SESSION_URI, OAuth2Constants.OAuth2Client.CLIENT_SESSION_URI);


        for (Map.Entry<String, String> entry : translationMap.entrySet()) {
            if (entry.getValue() != null) {
                reverseTranslationMap.put(entry.getValue(), entry.getKey());
            }
        }


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

    private Map<String, Object> createReadResponse(ClientApplication oauth2Client) {
        Map<String, Object> response = new HashMap<String, Object>();
        response.put(REDIRECT_URIS, oauth2Client.getRedirectionURIs());
        response.put(SCOPES, oauth2Client.getAllowedGrantScopes());
        response.put(DEFAULT_SCOPES, oauth2Client.getDefaultGrantScopes());
        response.put(CLIENT_NAME, oauth2Client.getClientName());
        response.put(CLIENT_DESCRIPTION, oauth2Client.getDisplayDescription());
        response.put(JWKS_URI, oauth2Client.getJwksURI());
        response.put(SUBJECT_TYPE, oauth2Client.getSubjectType());
        response.put(ID_TOKEN_SIGNED_RESPONSE_ALG, oauth2Client.getIDTokenSignedResponseAlgorithm());
        response.put(POST_LOGOUT_REDIRECT_URIS, oauth2Client.getPostLogoutRedirectionURI());
        response.put(CLIENT_TYPE, oauth2Client.getClientType());
        response.put(CLIENT_ID, oauth2Client.getClientId());
        response.put(REGISTRATION_ACCESS_TOKEN, oauth2Client.getAccessToken());
        response.put(CLIENT_SESSION_URI, oauth2Client.getClientSessionURI());


        for( Iterator<Map.Entry<String,Object>> iter = response.entrySet().iterator() ; iter.hasNext();) {
            Map.Entry<String,Object> entry = iter.next();
            if (entry.getValue() == null) {
                iter.remove();
            } else if (entry.getValue() instanceof Set && ((Set) entry.getValue()).isEmpty()) {
                iter.remove();
            } else if (entry.getValue() instanceof String && ((String) entry.getValue()).isEmpty()) {
                iter.remove();
            } else if (entry.getValue() instanceof ClientApplication.ClientType) {
                //do nothing this will be set
            } else {
                //do nothing has a value
            }
        }

        return response;
    }

    @Post
    public Representation validate(Representation entity) {

        String accessToken = getRequest().getChallengeResponse().getRawValue();

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
                    response.put(CLIENT_SECRET, json.get(CLIENT_SECRET));
                } else {
                    if (!translationMap.containsKey(key)) {
                        OAuth2Utils.DEBUG.error("ConnectClientRegistration.Validate(): Input parameter " + key + "unrecognized");
                        throw OAuthProblemException.OAuthError.INVALID_CLIENT_METADATA.handle(getRequest());
                    }
                    String translation = translationMap.get(key);
                    if (translation != null) {
                        response.put(key, json.get(key));
                    }
                }
            }

        } catch (Exception e) {
            OAuth2Utils.DEBUG.error("ConnectClientRegistration.Validate(): Error parsing input", e);
            throw OAuthProblemException.OAuthError.INVALID_CLIENT_METADATA.handle(getRequest());
        }

        if (!response.containsKey(REGISTRATION_ACCESS_TOKEN)) {
            response.put(REGISTRATION_ACCESS_TOKEN, accessToken);
        }

        if (id == null) {
            id = UUID.randomUUID().toString();
        }

        if (secret == null) {
            secret = UUID.randomUUID().toString();
            try {
                response.put(CLIENT_SECRET, secret);
            } catch (Exception e) {
                OAuth2Utils.DEBUG.error("ConnectClientRegistration.Validate(): Error adding client_secret", e);
                throw OAuthProblemException.OAuthError.INVALID_CLIENT_METADATA.handle(getRequest());
            }
        }

        Map<String, Set<String>> attrs = new HashMap<String, Set<String>>();
        for (Map.Entry mapEntry : response.entrySet()) {
            if (!translationMap.containsKey(mapEntry.getKey()) ||
                    translationMap.get(mapEntry.getKey()) == null) {
                continue;
            }
            if (mapEntry.getValue() instanceof String) {
                Set<String> temp = new HashSet<String>();
                temp.add((String) mapEntry.getValue());
                attrs.put(translationMap.get(mapEntry.getKey()), temp);
            } else if (mapEntry.getValue() instanceof JSONArray) {
                JSONArray temp = (JSONArray)mapEntry.getValue();
                Set<String> set = new HashSet<String>();
                for (int i = 0; i < temp.length(); i++){
                    try {
                        set.add("[" + i + "]=" + (String) temp.get(i));
                    } catch (JSONException e) {
                        OAuth2Utils.DEBUG.error("ConnectClientRegistration.Validate(): Unable to create client", e);
                        throw OAuthProblemException.OAuthError.INVALID_CLIENT_METADATA.handle(getRequest());
                    }
                }
                attrs.put(translationMap.get(mapEntry.getKey()), set);
            } else {
                OAuth2Utils.DEBUG.error("ConnectClientRegistration.Validate(): Unable to create client");
                throw OAuthProblemException.OAuthError.INVALID_CLIENT_METADATA.handle(getRequest());
            }
        }

        Set<String> temp = new HashSet<String>();
        temp.add(OAUTH2_CLIENT);
        attrs.put(AGENT_TYPE, temp);

        temp = new HashSet<String>();
        temp.add(ACTIVE);
        attrs.put(SUN_IDENTITY_SERVER_DEVICE_STATUS, temp);

        try {
            SSOToken token = (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance());
            AMIdentityRepository repo = new AMIdentityRepository(token, realm);
            repo.createIdentity(IdType.AGENTONLY, id, attrs);
        } catch (Exception e) {
            OAuth2Utils.DEBUG.error("ConnectClientRegistration.Validate(): Unable to create client", e);
            throw OAuthProblemException.OAuthError.INVALID_CLIENT_METADATA.handle(getRequest());
        }

        Map<String, Object> map = new HashMap<String, Object>(response);
        map.put(CLIENT_ID, id);

        map.put(REGISTRATION_CLIENT_URI, OAuth2Utils.getDeploymentURL(getRequest()) + "/oauth2/connect/register?client_id=" + id);
        map.put(ISSUED_AT, System.currentTimeMillis() / 1000);

        // TODO add expire time if JWT is used as the secret
        map.put(EXPIRES_AT, 0);
        return new JsonRepresentation(map);

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
