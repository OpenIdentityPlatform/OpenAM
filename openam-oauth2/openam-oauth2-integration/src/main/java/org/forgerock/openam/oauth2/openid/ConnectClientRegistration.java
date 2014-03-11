/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2012-2014 ForgeRock AS. All rights reserved.
 */

/*
 * Portions Copyrighted 2013 Nomura Research Institute, Ltd
 */

package org.forgerock.openam.oauth2.openid;

import com.sun.identity.security.AdminTokenAction;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.openam.oauth2.OAuth2ConfigurationFactory;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.oauth2.OAuth2Utils;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.openam.oauth2.model.Client;
import org.forgerock.openam.oauth2.model.ClientBuilder;
import org.forgerock.openam.oauth2.provider.ClientDAO;
import org.forgerock.openam.oauth2.provider.OAuth2ProviderSettings;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import java.io.IOException;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.forgerock.openam.oauth2.OAuth2Constants.ShortClientAttributeNames.APPLICATION_TYPE;
import static org.forgerock.openam.oauth2.OAuth2Constants.ShortClientAttributeNames.CLIENT_DESCRIPTION;
import static org.forgerock.openam.oauth2.OAuth2Constants.ShortClientAttributeNames.CLIENT_ID;
import static org.forgerock.openam.oauth2.OAuth2Constants.ShortClientAttributeNames.CLIENT_NAME;
import static org.forgerock.openam.oauth2.OAuth2Constants.ShortClientAttributeNames.CLIENT_SECRET;
import static org.forgerock.openam.oauth2.OAuth2Constants.ShortClientAttributeNames.CLIENT_SESSION_URI;
import static org.forgerock.openam.oauth2.OAuth2Constants.ShortClientAttributeNames.CLIENT_TYPE;
import static org.forgerock.openam.oauth2.OAuth2Constants.ShortClientAttributeNames.DEFAULT_SCOPES;
import static org.forgerock.openam.oauth2.OAuth2Constants.ShortClientAttributeNames.DISPLAY_NAME;
import static org.forgerock.openam.oauth2.OAuth2Constants.ShortClientAttributeNames.ID_TOKEN_SIGNED_RESPONSE_ALG;
import static org.forgerock.openam.oauth2.OAuth2Constants.ShortClientAttributeNames.POST_LOGOUT_REDIRECT_URIS;
import static org.forgerock.openam.oauth2.OAuth2Constants.ShortClientAttributeNames.REDIRECT_URIS;
import static org.forgerock.openam.oauth2.OAuth2Constants.ShortClientAttributeNames.REGISTRATION_ACCESS_TOKEN;
import static org.forgerock.openam.oauth2.OAuth2Constants.ShortClientAttributeNames.RESPONSE_TYPES;
import static org.forgerock.openam.oauth2.OAuth2Constants.ShortClientAttributeNames.SCOPES;
import static org.forgerock.openam.oauth2.OAuth2Constants.ShortClientAttributeNames.SUBJECT_TYPE;
import static org.forgerock.openam.oauth2.OAuth2Constants.ShortClientAttributeNames.fromString;

public class ConnectClientRegistration extends ServerResource {
    private static final String ID_TOKEN_SIGNED_RESPONSE_ALG_DEFAULT = "HS256";
    private static final String REALM = "realm";
    private static final String DEFAULT_REALM = "/";
    private static final String DEFAULT_APPLICATION_TYPE = "web";

    private static final String REGISTRATION_CLIENT_URI = "registration_client_uri";
    private static final String ISSUED_AT = "client_id_issued_at";
    private static final String EXPIRES_AT = "client_secret_expires_at";

    /**
     * Strip the class from the response type settings.
     *
     * Example:
     *    Format is "type|class" we want only "type".
     *
     * @param responseTypeSet The set of response types that are type|class
     * @return A set of supported response types without the class data.
     */
    private Set<String> formatResponseTypes(Set<String> responseTypeSet) {
        Set<String> responseTypes = new HashSet<String>();
        for (String responseType : responseTypeSet){
            String[] parts = responseType.split("\\|");
            if (parts.length != 2){
                continue;
            }
            responseTypes.add(parts[0]);
        }

        if (responseTypes.contains("code") && responseTypes.contains("token") &&
                responseTypes.contains("id_token")) {
            responseTypes.add("code token id_token");
        }

        if (responseTypes.contains("code") && responseTypes.contains("token")) {
            responseTypes.add("code token");
        }

        if (responseTypes.contains("code") && responseTypes.contains("id_token")) {
            responseTypes.add("code id_token");
        }

        if (responseTypes.contains("token") && responseTypes.contains("id_token")) {
            responseTypes.add("token id_token");
        }
        return responseTypes;
    }

    private boolean containsCaseInsensitive(Set<String> collection1, List<String> values) {
        Iterator<String> valuesIterator = values.iterator();
        while (valuesIterator.hasNext()) {
            boolean containsValue = false;
            String value = valuesIterator.next();
            for (String string : collection1) {
                if (string.equalsIgnoreCase(value)) {
                    containsValue = true;
                    break;
                }
            }
            if (containsValue == false) {
                return false;
            }
        }
        return true;
    }

    private boolean containsCaseInsensitive(Set<String> collection, String value) {
        Iterator<String> iteratorCollection1 = collection.iterator();
        while (iteratorCollection1.hasNext()) {
            if (iteratorCollection1.next().equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    @Post
    public Representation createClient(Representation entity) throws OAuthProblemException {
        String accessToken = getRequest().getChallengeResponse().getRawValue();
        String realm = OAuth2Utils.getRealm(getRequest());
        OAuth2ProviderSettings settings = OAuth2Utils.getSettingsProvider(getRequest());
        JsonValue input = null;
        //read input
        try {
            JacksonRepresentation<Map> rep = new JacksonRepresentation<Map>(entity, Map.class);
            input = new JsonValue(rep.getObject());
        } catch (IOException e) {
            OAuth2Utils.DEBUG.error("ConnectClientRegistration.createClient(): Unable to parse json input.", e);
            throw OAuthProblemException.OAuthError.INVALID_CLIENT_METADATA.handle(null);
        }

        //check input to ensure it is valid
        Set<String> inputKeys = input.keys();
        for (String key : inputKeys) {
            if (fromString(key) == null) {
                //input in unknown
                OAuth2Utils.DEBUG.error("ConnectClientRegistration.createClient(): Unknown input given. Key: " + key);
                throw OAuthProblemException.OAuthError.INVALID_CLIENT_METADATA.handle(null);
            }
        }
        //create client given input
        ClientBuilder clientBuilder = new ClientBuilder();
        try {
            if ((realm == null || realm.isEmpty()) && input.get(REALM).asString() != null) {
                realm = input.get(REALM).asString();
            } else {
                realm = DEFAULT_REALM;
            }

            if (input.get(CLIENT_ID.getType()).asString() != null) {
                clientBuilder.setClientID(input.get(CLIENT_ID.getType()).asString());
            } else {
                clientBuilder.setClientID(UUID.randomUUID().toString());
            }

            if (input.get(CLIENT_SECRET.getType()).asString() != null) {
                clientBuilder.setClientSecret(input.get(CLIENT_SECRET.getType()).asString());
            } else {
                clientBuilder.setClientSecret(UUID.randomUUID().toString());
            }

            if (input.get(CLIENT_TYPE.getType()).asString() != null) {
                if (Client.ClientType.fromString(input.get(CLIENT_TYPE.getType()).asString()) != null) {
                    clientBuilder.setClientType(input.get(CLIENT_TYPE.getType()).asString());
                } else {
                    OAuth2Utils.DEBUG.error("ConnectClientRegistration.createClient(): Invalid client_type requested.");
                    throw OAuthProblemException.OAuthError.INVALID_CLIENT_METADATA.handle(null);
                }
            } else {
                clientBuilder.setClientType(Client.ClientType.CONFIDENTIAL.getType());
            }

            if (input.get(REDIRECT_URIS.getType()).asList() != null) {
                clientBuilder.setRedirectionURIs(input.get(REDIRECT_URIS.getType()).asList(String.class));
            }

            if (input.get(SCOPES.getType()).asList() != null) {
                if (containsCaseInsensitive(settings.getSupportedClaims(), input.get(SCOPES.getType()).asList(String.class))) {
                    clientBuilder.setAllowedGrantScopes(input.get(SCOPES.getType()).asList(String.class));
                } else {
                    OAuth2Utils.DEBUG.error("ConnectClientRegistration.createClient(): Invalid scopes requested.");
                    throw OAuthProblemException.OAuthError.INVALID_CLIENT_METADATA.handle(null);
                }
            }

            if (input.get(DEFAULT_SCOPES.getType()).asList() != null) {
                if (containsCaseInsensitive(settings.getSupportedClaims(), input.get(DEFAULT_SCOPES.getType()).asList(String.class))) {
                    clientBuilder.setDefaultGrantScopes(input.get(DEFAULT_SCOPES.getType()).asList(String.class));
                } else {
                    OAuth2Utils.DEBUG.error("ConnectClientRegistration.createClient(): Invalid default_scopes requested.");
                    throw OAuthProblemException.OAuthError.INVALID_CLIENT_METADATA.handle(null);
                }
            }

            if (input.get(CLIENT_NAME.getType()).asString() != null) {
                clientBuilder.setClientName(input.get(CLIENT_NAME.getType()).asString());
            }

            if (input.get(CLIENT_DESCRIPTION.getType()).asList() != null) {
                clientBuilder.setDisplayDescription(input.get(CLIENT_DESCRIPTION.getType()).asList(String.class));
            }

            if (input.get(SUBJECT_TYPE.getType()).asString() != null) {
                if (Client.SubjectType.fromString(input.get(SUBJECT_TYPE.getType()).asString()) != null) {
                    clientBuilder.setSubjectType(Client.SubjectType.PUBLIC.getType());
                } else {
                    OAuth2Utils.DEBUG.error("ConnectClientRegistration.createClient(): Invalid subject_type requested.");
                    throw OAuthProblemException.OAuthError.INVALID_CLIENT_METADATA.handle(null);
                }
            } else {
                clientBuilder.setSubjectType(Client.SubjectType.PUBLIC.getType());
            }

            if (input.get(ID_TOKEN_SIGNED_RESPONSE_ALG.getType()).asString() != null) {
                if (containsCaseInsensitive(settings.getTheIDTokenSigningAlgorithmsSupported(), input.get(ID_TOKEN_SIGNED_RESPONSE_ALG.getType()).asString())) {
                    clientBuilder.setIdTokenSignedResponseAlgorithm(input.get(ID_TOKEN_SIGNED_RESPONSE_ALG.getType()).asString());
                } else {
                    OAuth2Utils.DEBUG.error("ConnectClientRegistration.createClient(): Unsupported id_token_response_signed_alg requested.");
                    throw OAuthProblemException.OAuthError.INVALID_CLIENT_METADATA.handle(null);
                }
            } else {
                clientBuilder.setIdTokenSignedResponseAlgorithm(ID_TOKEN_SIGNED_RESPONSE_ALG_DEFAULT);
            }

            if (input.get(POST_LOGOUT_REDIRECT_URIS.getType()).asString() != null) {
                clientBuilder.setPostLogoutRedirectionURI(input.get(POST_LOGOUT_REDIRECT_URIS.getType()).asString());
            }

            if (input.get(REGISTRATION_ACCESS_TOKEN.getType()).asString() != null) {
                clientBuilder.setAccessToken(input.get(REGISTRATION_ACCESS_TOKEN.getType()).asString());
            } else {
                clientBuilder.setAccessToken(accessToken);
            }

            if (input.get(CLIENT_SESSION_URI.getType()).asString() != null) {
                clientBuilder.setClientSessionURI(input.get(CLIENT_SESSION_URI.getType()).asString());
            }

            if (input.get(APPLICATION_TYPE.getType()).asString() != null) {
                if (Client.ApplicationType.fromString(input.get(APPLICATION_TYPE.getType()).asString()) != null) {
                    clientBuilder.setApplicationType(Client.ApplicationType.WEB.getType());
                } else {
                    OAuth2Utils.DEBUG.error("ConnectClientRegistration.createClient(): Invalid application_type requested.");
                    throw OAuthProblemException.OAuthError.INVALID_CLIENT_METADATA.handle(null);
                }
            } else {
                clientBuilder.setApplicationType(DEFAULT_APPLICATION_TYPE);
            }

            if (input.get(DISPLAY_NAME.getType()).asList() != null) {
                clientBuilder.setDisplayName(input.get(DISPLAY_NAME.getType()).asList(String.class));
            }

            if (input.get(RESPONSE_TYPES.getType()).asList() != null) {
                if (containsCaseInsensitive(formatResponseTypes(settings.getResponseTypes()), input.get(RESPONSE_TYPES.getType()).asList(String.class))) {
                    clientBuilder.setResponseTypes(input.get(RESPONSE_TYPES.getType()).asList(String.class));
                } else {
                    OAuth2Utils.DEBUG.error("ConnectClientRegistration.createClient(): Invalid response_types requested.");
                    throw OAuthProblemException.OAuthError.INVALID_CLIENT_METADATA.handle(null);
                }
            } else {
                List<String> defaultResponseTypes = new ArrayList<String>();
                defaultResponseTypes.add("code");
                clientBuilder.setResponseTypes(defaultResponseTypes);
            }

        } catch (JsonValueException e) {
            OAuth2Utils.DEBUG.error("ConnectClientRegistration.createClient(): Unable to build client.", e);
            throw OAuthProblemException.OAuthError.INVALID_CLIENT_METADATA.handle(null);
        }

        //build the client
        Client client = clientBuilder.createClient();

        //get the ClientDAO
        ClientDAO clientDAO = OAuth2ConfigurationFactory.Holder.getConfigurationFactory().newClientDAO(realm,
                getRequest(), AccessController.doPrivileged(AdminTokenAction.getInstance()));

        //write the client to the storage
        clientDAO.create(client);

        //return the response json data
        Map<String, Object> response = client.asMap();

        response.put(REGISTRATION_CLIENT_URI,
                OAuth2Utils.getDeploymentURL(getRequest()) + "/oauth2/connect/register?client_id=" + client.getClientID());
        response.put(ISSUED_AT, System.currentTimeMillis() / 1000);

        // TODO add expire time if JWT is used as the secret
        response.put(EXPIRES_AT, 0);

        setStatus(Status.SUCCESS_CREATED);
        return new JsonRepresentation(response);

    }

    @Get
    public Representation getClient() throws OAuthProblemException {
        String realm = OAuth2Utils.getRealm(getRequest());
        String clientId = OAuth2Utils.getRequestParameter(getRequest(),
                OAuth2Constants.OAuth2Client.CLIENT_ID,
                String.class);
        String accessToken = getRequest().getChallengeResponse().getRawValue();

        if (clientId != null) {
            try {
                //read the client from storage
                ClientDAO clientDAO = OAuth2ConfigurationFactory.Holder.getConfigurationFactory().newClientDAO(realm,
                        getRequest(), AccessController.doPrivileged(AdminTokenAction.getInstance()));
                Client client = clientDAO.read(clientId);

                if (!client.getAccessToken().equals(accessToken)) {
                    //client access token doesn't match the access token supplied in the request
                    OAuth2Utils.DEBUG.error("ConnectClientRegistration.getClient(): Invalid accessToken");
                    throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(null);
                }

                //remove the client fields that don't need to be reported.
                client.remove(CLIENT_SECRET.getType());
                client.remove(REGISTRATION_ACCESS_TOKEN.getType());

                //return the client attributes as json
                return new JsonRepresentation(client.toString());
            } catch (Exception e) {
                OAuth2Utils.DEBUG.error("ConnectClientRegistration.Validate(): Unable to create client", e);
                throw OAuthProblemException.OAuthError.INVALID_CLIENT_METADATA.handle(null);
            }
        } else {
            OAuth2Utils.DEBUG.error("ConnectClientRegistration.readRequest(): No client id sent");
            throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(null);
        }
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
