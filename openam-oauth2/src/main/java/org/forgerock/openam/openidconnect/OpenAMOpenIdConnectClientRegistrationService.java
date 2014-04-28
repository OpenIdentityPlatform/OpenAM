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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.openidconnect;

import org.forgerock.oauth2.core.AccessTokenVerifier;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnsupportedResponseTypeException;
import org.forgerock.openidconnect.Client;
import org.forgerock.openidconnect.ClientBuilder;
import org.forgerock.openidconnect.ClientDAO;
import org.forgerock.openidconnect.OpenIdConnectClientRegistrationService;
import org.forgerock.openidconnect.exceptions.InvalidClientMetadata;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.forgerock.oauth2.core.OAuth2Constants.ShortClientAttributeNames.APPLICATION_TYPE;
import static org.forgerock.oauth2.core.OAuth2Constants.ShortClientAttributeNames.CLIENT_DESCRIPTION;
import static org.forgerock.oauth2.core.OAuth2Constants.ShortClientAttributeNames.CLIENT_ID;
import static org.forgerock.oauth2.core.OAuth2Constants.ShortClientAttributeNames.CLIENT_NAME;
import static org.forgerock.oauth2.core.OAuth2Constants.ShortClientAttributeNames.CLIENT_SECRET;
import static org.forgerock.oauth2.core.OAuth2Constants.ShortClientAttributeNames.CLIENT_SESSION_URI;
import static org.forgerock.oauth2.core.OAuth2Constants.ShortClientAttributeNames.CLIENT_TYPE;
import static org.forgerock.oauth2.core.OAuth2Constants.ShortClientAttributeNames.DEFAULT_SCOPES;
import static org.forgerock.oauth2.core.OAuth2Constants.ShortClientAttributeNames.DISPLAY_NAME;
import static org.forgerock.oauth2.core.OAuth2Constants.ShortClientAttributeNames.ID_TOKEN_SIGNED_RESPONSE_ALG;
import static org.forgerock.oauth2.core.OAuth2Constants.ShortClientAttributeNames.POST_LOGOUT_REDIRECT_URIS;
import static org.forgerock.oauth2.core.OAuth2Constants.ShortClientAttributeNames.REDIRECT_URIS;
import static org.forgerock.oauth2.core.OAuth2Constants.ShortClientAttributeNames.REGISTRATION_ACCESS_TOKEN;
import static org.forgerock.oauth2.core.OAuth2Constants.ShortClientAttributeNames.RESPONSE_TYPES;
import static org.forgerock.oauth2.core.OAuth2Constants.ShortClientAttributeNames.SCOPES;
import static org.forgerock.oauth2.core.OAuth2Constants.ShortClientAttributeNames.SUBJECT_TYPE;
import static org.forgerock.oauth2.core.OAuth2Constants.ShortClientAttributeNames.fromString;

/**
 * Service for registering OpenId Connect clients in OpenAM.
 *
 * @since 12.0.0
 */
@Singleton
public class OpenAMOpenIdConnectClientRegistrationService implements OpenIdConnectClientRegistrationService {

    private static final String ID_TOKEN_SIGNED_RESPONSE_ALG_DEFAULT = "HS256";
    private static final String DEFAULT_APPLICATION_TYPE = "web";

    private static final String REGISTRATION_CLIENT_URI = "registration_client_uri";
    private static final String ISSUED_AT = "client_id_issued_at";
    private static final String EXPIRES_AT = "client_secret_expires_at";

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");
    private final ClientDAO clientDAO;
    private final OAuth2ProviderSettingsFactory providerSettingsFactory;
    private final AccessTokenVerifier tokenVerifier;

    /**
     * Constructs a new OpenAMOpenIdConnectClientRegistrationService.
     *
     * @param clientDAO An instance of the ClientDAO.
     * @param providerSettingsFactory An instance of the OAuth2ProviderSettingsFactory.
     * @param tokenVerifier An instance of the AccessTokenVerifier.
     */
    @Inject
    OpenAMOpenIdConnectClientRegistrationService(ClientDAO clientDAO,
            OAuth2ProviderSettingsFactory providerSettingsFactory, AccessTokenVerifier tokenVerifier) {
        this.clientDAO = clientDAO;
        this.providerSettingsFactory = providerSettingsFactory;
        this.tokenVerifier = tokenVerifier;
    }

    /**
     * {@inheritDoc}
     */
    public JsonValue createRegistration(String accessToken, String deploymentUrl, OAuth2Request request)
            throws InvalidClientMetadata, ServerException, UnsupportedResponseTypeException {

        if (!tokenVerifier.verify(request)) {
            throw new ServerException("Access Token not valid");
        }

        final OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);
        final JsonValue input = request.getBody();

        //check input to ensure it is valid
        Set<String> inputKeys = input.keys();
        for (String key : inputKeys) {
            if (fromString(key) == null) {
                //input in unknown
                logger.error("Unknown input given. Key: " + key);
                throw new InvalidClientMetadata();
            }
        }
        //create client given input
        ClientBuilder clientBuilder = new ClientBuilder();
        try {

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
                    logger.error("Invalid client_type requested.");
                    throw new InvalidClientMetadata();
                }
            } else {
                clientBuilder.setClientType(Client.ClientType.CONFIDENTIAL.getType());
            }

            if (input.get(REDIRECT_URIS.getType()).asList() != null) {
                clientBuilder.setRedirectionURIs(input.get(REDIRECT_URIS.getType()).asList(String.class));
            }

            if (input.get(SCOPES.getType()).asList() != null) {
                if (containsCaseInsensitive(providerSettings.getSupportedClaims(),
                        input.get(SCOPES.getType()).asList(String.class))) {
                    clientBuilder.setAllowedGrantScopes(input.get(SCOPES.getType()).asList(String.class));
                } else {
                    logger.error("Invalid scopes requested.");
                    throw new InvalidClientMetadata();
                }
            }

            if (input.get(DEFAULT_SCOPES.getType()).asList() != null) {
                if (containsCaseInsensitive(providerSettings.getSupportedClaims(),
                        input.get(DEFAULT_SCOPES.getType()).asList(String.class))) {
                    clientBuilder.setDefaultGrantScopes(input.get(DEFAULT_SCOPES.getType()).asList(String.class));
                } else {
                    logger.error("Invalid default_scopes requested.");
                    throw new InvalidClientMetadata();
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
                    logger.error("Invalid subject_type requested.");
                    throw new InvalidClientMetadata();
                }
            } else {
                clientBuilder.setSubjectType(Client.SubjectType.PUBLIC.getType());
            }

            if (input.get(ID_TOKEN_SIGNED_RESPONSE_ALG.getType()).asString() != null) {
                if (containsCaseInsensitive(providerSettings.getSupportedIDTokenSigningAlgorithms(),
                        input.get(ID_TOKEN_SIGNED_RESPONSE_ALG.getType()).asString())) {
                    clientBuilder.setIdTokenSignedResponseAlgorithm(input.get(ID_TOKEN_SIGNED_RESPONSE_ALG.getType())
                            .asString());
                } else {
                    logger.error("Unsupported id_token_response_signed_alg requested.");
                    throw new InvalidClientMetadata();
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
                    logger.error("Invalid application_type requested.");
                    throw new InvalidClientMetadata();
                }
            } else {
                clientBuilder.setApplicationType(DEFAULT_APPLICATION_TYPE);
            }

            if (input.get(DISPLAY_NAME.getType()).asList() != null) {
                clientBuilder.setDisplayName(input.get(DISPLAY_NAME.getType()).asList(String.class));
            }

            if (input.get(RESPONSE_TYPES.getType()).asList() != null) {
                if (containsCaseInsensitive(providerSettings.getAllowedResponseTypes().keySet(),
                        input.get(RESPONSE_TYPES.getType()).asList(String.class))) {
                    clientBuilder.setResponseTypes(input.get(RESPONSE_TYPES.getType()).asList(String.class));
                } else {
                    logger.error("Invalid response_types requested.");
                    throw new InvalidClientMetadata();
                }
            } else {
                List<String> defaultResponseTypes = new ArrayList<String>();
                defaultResponseTypes.add("code");
                clientBuilder.setResponseTypes(defaultResponseTypes);
            }

        } catch (JsonValueException e) {
            logger.error("Unable to build client.", e);
            throw new InvalidClientMetadata();
        }

        Client client = clientBuilder.createClient();

        clientDAO.create(client, request);

        Map<String, Object> response = client.asMap();

        response.put(REGISTRATION_CLIENT_URI,
                deploymentUrl + "/oauth2/connect/register?client_id=" + client.getClientID());
        response.put(ISSUED_AT, System.currentTimeMillis() / 1000);

        response.put(EXPIRES_AT, 0);

        return new JsonValue(response);
    }

    private boolean containsCaseInsensitive(final Set<String> collection, final List<String> values) {
        final Iterator<String> valuesIterator = values.iterator();
        while (valuesIterator.hasNext()) {
            final String value = valuesIterator.next();
            for (final String string : collection) {
                if (string.equalsIgnoreCase(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean containsCaseInsensitive(final Set<String> collection, final String value) {
        final Iterator<String> iter = collection.iterator();
        while (iter.hasNext()) {
            if (iter.next().equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public JsonValue getRegistration(String clientId, String accessToken, OAuth2Request request)
            throws InvalidRequestException, InvalidClientMetadata {
        if (clientId != null) {
            try {
                final Client client = clientDAO.read(clientId, request);

                if (!client.getAccessToken().equals(accessToken)) {
                    //client access token doesn't match the access token supplied in the request
                    logger.error("ConnectClientRegistration.getClient(): Invalid accessToken");
                    throw new InvalidRequestException();
                }

                //remove the client fields that don't need to be reported.
                client.remove(CLIENT_SECRET.getType());
                client.remove(REGISTRATION_ACCESS_TOKEN.getType());

                return new JsonValue(client.asMap());
            } catch (Exception e) {
                logger.error("ConnectClientRegistration.Validate(): Unable to create client", e);
                throw new InvalidClientMetadata();
            }
        } else {
            logger.error("ConnectClientRegistration.readRequest(): No client id sent");
            throw new InvalidRequestException();
        }

    }
}
