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

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdType;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.OAuth2Constants;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.openam.oauth2.model.ClientApplication;
import org.forgerock.openam.oauth2.model.impl.ClientApplicationImpl;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.ServerResource;

import java.io.IOException;
import java.security.AccessController;
import java.util.*;

public class ConnectClientRegistration extends ServerResource {

    private ClientApplication oauth2client = null;
    private ServiceSchemaManager serviceSchemaManager = null;
    private ServiceSchema serviceSchema = null;

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
    private static final String REALM = "realm";

    private static final String NOT_USED = null;

    private static final String REGISTRATION_CLIENT_URI = "registration_client_uri";
    private static final String ISSUED_AT = "client_id_issued_at";
    private static final String EXPIRES_AT = "client_secret_expires_at";

    private static final String OAUTH2_CLIENT = "OAuth2Client";
    private static final String AGENT_TYPE = "AgentType";
    private static final String ACTIVE = "Active";
    private static final String SUN_IDENTITY_SERVER_DEVICE_STATUS = "sunIdentityServerDeviceStatus";


    private static final String SCOPES_DEFAULT = "[0]=openid|Using the OpenID Connect Protocol";
    private static final String SUBJECT_TYPE_DEFAULT = "Public";
    private static final String ID_TOKEN_SIGNED_RESPONSE_ALG_DEFAULT = "HmacSsHA256";
    private static final String CLIENT_TYPE_DEFAULT = "Confidential";

    public ConnectClientRegistration () throws SMSException, SSOException {
        this(null,
                new ServiceSchemaManager("AgentService",
                        AccessController.doPrivileged(AdminTokenAction.getInstance())));
        createTranslationMaps();
    }

    public ConnectClientRegistration (ClientApplication clientApplication, ServiceSchemaManager serviceSchemaManager) {
        createTranslationMaps();
        try {
            this.oauth2client = clientApplication;
            this.serviceSchemaManager = serviceSchemaManager;
            this.serviceSchema = this.serviceSchemaManager.getOrganizationSchema().getSubSchema(OAUTH2_CLIENT);
        } catch (Exception e) {
            OAuth2Utils.DEBUG.error("Unable to get Client Schema", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(getRequest());
        }
    }

    private void createTranslationMaps() {
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
    public Representation readRequest() {
        String clientId= OAuth2Utils.getRequestParameter(
                getRequest(), OAuth2Constants.OAuth2Client.CLIENT_ID, String.class);
        String accessToken = getRequest().getChallengeResponse().getRawValue();

        if (clientId != null) {
            try {
                AMIdentity client = OAuth2Utils.getClientIdentity(clientId, OAuth2Utils.getRealm(getRequest()));
                ClientApplication oauth2client;
                if (this.oauth2client != null) {
                    oauth2client = this.oauth2client;
                } else {
                    oauth2client = new ClientApplicationImpl(client);
                }
                if (!oauth2client.getAccessToken().equals(accessToken)) {
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

    private void setDefaultValues(Map<String, Set<String>> attrs) {
        Set<String> temp;
        if (!attrs.containsKey(OAuth2Constants.OAuth2Client.SCOPES)) {
            temp = new HashSet<String>();
            temp.add(SCOPES_DEFAULT);
            attrs.put(OAuth2Constants.OAuth2Client.SCOPES, temp);
        }

        if (!attrs.containsKey(OAuth2Constants.OAuth2Client.DEFAULT_SCOPES)) {
            temp = new HashSet<String>();
            temp.add(SCOPES_DEFAULT);
            attrs.put(OAuth2Constants.OAuth2Client.DEFAULT_SCOPES, temp);
        }

        if (!attrs.containsKey(OAuth2Constants.OAuth2Client.IDTOKEN_SIGNED_RESPONSE_ALG)) {
            temp = new HashSet<String>();
            temp.add(ID_TOKEN_SIGNED_RESPONSE_ALG_DEFAULT);
            attrs.put(OAuth2Constants.OAuth2Client.IDTOKEN_SIGNED_RESPONSE_ALG, temp);
        }

        if (!attrs.containsKey(OAuth2Constants.OAuth2Client.CLIENT_TYPE)) {
            temp = new HashSet<String>();
            temp.add(CLIENT_TYPE_DEFAULT);
            attrs.put(OAuth2Constants.OAuth2Client.CLIENT_TYPE, temp);
        }

    }

    private String getValue(JsonValue value) throws OAuthProblemException {
        if (value.isString()) {
            return value.asString();
        } else if (value.isList()) {
            return value.asList().get(0).toString();
        } else {
            OAuth2Utils.DEBUG.error("ConnectClientRegistration.getValue(): Error parsing input");
            throw OAuthProblemException.OAuthError.INVALID_CLIENT_METADATA.handle(getRequest());
        }
    }


    private boolean isSingle(String value) {
        AttributeSchema attributeSchema = serviceSchema.getAttributeSchema(value);
        AttributeSchema.UIType uiType = attributeSchema.getUIType();
        if (uiType != null && (uiType.equals(AttributeSchema.UIType.UNORDEREDLIST) ||
                uiType.equals(AttributeSchema.UIType.ORDEREDLIST))) {
            return false;
        }
        return true;
    }

    private void formatSet(Set<String> set, boolean isSingleValue, Map<String, Set<String>> attrs, String key) {
        if (isSingleValue) {
            attrs.put(translationMap.get(key), set);
        } else {
            Set newset = new HashSet<String>();
            Iterator<String> iter = set.iterator();
            for (int i = 0; iter.hasNext(); i++) {
                String string = iter.next();
                string = ("[" + i + "]=" + string);
                newset.add(string);
            }
            attrs.put(translationMap.get(key), newset);
        }
    }

    private void setAttr(JsonValue jsonValue, boolean isSingleValue, Map<String, Set<String>> attrs) {
        String key = jsonValue.getPointer().leaf();
        if (jsonValue.isList()) {
            Set<String> set = new HashSet<String>(jsonValue.asList(String.class));
            formatSet(set, isSingleValue, attrs, key);
        } else if (jsonValue.isString()) {
            Set<String> set = new HashSet<String>();
            set.add(jsonValue.asString());
            formatSet(set, isSingleValue, attrs, key);
        } else if (jsonValue.isBoolean()) {
            Set<String> set = new HashSet<String>();
            set.add(jsonValue.asBoolean().toString());
            formatSet(set, isSingleValue, attrs, key);
        }
    }

    @Post
    public Representation validate(Representation entity) {

        String accessToken = getRequest().getChallengeResponse().getRawValue();

        Map<String, Set<String>> attrs;
        JsonValue result = null;
        String realm = null, id = null, secret = null;
        try {
            JacksonRepresentation<Map> rep =
                    new JacksonRepresentation<Map>(entity, Map.class);
            result = new JsonValue(rep.getObject());

            JsonValue value = result.get(OAuth2Constants.OAuth2Client.REALM);
            if (result.isDefined(OAuth2Constants.OAuth2Client.REALM)) {
                realm = getValue(value);
                result.remove(OAuth2Constants.OAuth2Client.REALM);
            } else {
                realm = null;
            }

            value = result.get(OAuth2Constants.OAuth2Client.CLIENT_ID);
            if (result.isDefined(OAuth2Constants.OAuth2Client.CLIENT_ID)) {
                id = getValue(value);
                result.remove(OAuth2Constants.OAuth2Client.CLIENT_ID);
            } else {
                id = UUID.randomUUID().toString();
            }

            if (!result.isDefined(OAuth2Constants.OAuth2Client.CLIENT_SECRET)) {
                secret = UUID.randomUUID().toString();
                result.put(OAuth2Constants.OAuth2Client.CLIENT_SECRET, secret);
            }

            if (!result.isDefined(REGISTRATION_ACCESS_TOKEN)) {
                result.put(REGISTRATION_ACCESS_TOKEN, accessToken);
            }

            for (Map.Entry<String, String> entry : translationMap.entrySet()) {
                if (entry.getValue() == NOT_USED && result.isDefined(entry.getKey())) {
                    result.remove(entry.getKey());
                }
            }


            attrs = new HashMap<String, Set<String>>();
            Iterator<JsonValue> iter = result.iterator();
            while (iter.hasNext()) {
                JsonValue jsonValue = iter.next();
                String key = jsonValue.getPointer().leaf();

                if (!translationMap.containsKey(key)) {
                    OAuth2Utils.DEBUG.error("ConnectClientRegistration.Validate(): Input parameter " + key + "unrecognized");
                    throw OAuthProblemException.OAuthError.INVALID_CLIENT_METADATA.handle(getRequest());
                }

                setAttr(jsonValue, isSingle(translationMap.get(key)), attrs);
            }
        } catch (IOException e) {
            OAuth2Utils.DEBUG.error("ConnectClientRegistration.Validate(): Error parsing input", e);
            throw OAuthProblemException.OAuthError.INVALID_CLIENT_METADATA.handle(getRequest());
        }

        Set<String> temp = new HashSet<String>();
        temp.add(OAUTH2_CLIENT);
        attrs.put(AGENT_TYPE, temp);

        temp = new HashSet<String>();
        temp.add(ACTIVE);
        attrs.put(SUN_IDENTITY_SERVER_DEVICE_STATUS, temp);

        setDefaultValues(attrs);

        AMIdentity clientAMIdentity = null;
        try {
            SSOToken token = (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance());
            AMIdentityRepository repo = new AMIdentityRepository(token, realm);
            clientAMIdentity = repo.createIdentity(IdType.AGENTONLY, id, attrs);
        } catch (Exception e) {
            OAuth2Utils.DEBUG.error("ConnectClientRegistration.Validate(): Unable to create client", e);
            throw OAuthProblemException.OAuthError.INVALID_CLIENT_METADATA.handle(getRequest());
        }

        ClientApplication clientApplication = new ClientApplicationImpl(clientAMIdentity);

        Map<String, Object> results = createReadResponse(clientApplication);

        results.put(CLIENT_ID, id);
        results.put(CLIENT_SECRET, secret);

        results.put(REGISTRATION_CLIENT_URI, OAuth2Utils.getDeploymentURL(getRequest()) + "/oauth2/connect/register?client_id=" + id);
        results.put(ISSUED_AT, System.currentTimeMillis() / 1000);

        // TODO add expire time if JWT is used as the secret
        results.put(EXPIRES_AT, 0);

        setStatus(Status.SUCCESS_CREATED);
        return new JsonRepresentation(results);

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
