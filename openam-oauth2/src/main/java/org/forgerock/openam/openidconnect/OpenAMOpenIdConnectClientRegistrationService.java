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
 * Copyright 2014-2015 ForgeRock AS.
 * Portions Copyrighted 2015 Nomura Research Institute, Ltd.
 */

package org.forgerock.openam.openidconnect;

import static org.forgerock.oauth2.core.OAuth2Constants.Params.*;
import static org.forgerock.oauth2.core.OAuth2Constants.ShortClientAttributeNames.*;
import static org.forgerock.oauth2.core.OAuth2Constants.ShortClientAttributeNames.CLIENT_ID;
import static org.forgerock.oauth2.core.OAuth2Constants.ShortClientAttributeNames.CLIENT_SECRET;

import com.sun.identity.shared.validation.ValidationException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.AccessTokenVerifier;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.oauth2.core.exceptions.AccessDeniedException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidTokenException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.forgerock.oauth2.core.exceptions.UnsupportedResponseTypeException;
import org.forgerock.openam.oauth2.validation.OpenIDConnectURLValidator;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.openidconnect.Client;
import org.forgerock.openidconnect.ClientBuilder;
import org.forgerock.openidconnect.ClientDAO;
import org.forgerock.openidconnect.OpenIdConnectClientRegistrationService;
import org.forgerock.openidconnect.exceptions.InvalidClientMetadata;
import org.forgerock.openidconnect.exceptions.InvalidRedirectUri;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final TokenStore tokenStore;
    private final OpenIDConnectURLValidator urlValidator;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Constructs a new OpenAMOpenIdConnectClientRegistrationService.
     *
     * @param clientDAO An instance of the ClientDAO.
     * @param providerSettingsFactory An instance of the OAuth2ProviderSettingsFactory.
     * @param tokenVerifier An instance of the AccessTokenVerifier.
     * @param tokenStore An instance of the TokenStore.
     * @param urlValidator An instance of the URLValidator.
     */
    @Inject
    OpenAMOpenIdConnectClientRegistrationService(ClientDAO clientDAO,
                                                 OAuth2ProviderSettingsFactory providerSettingsFactory,
                                                 AccessTokenVerifier tokenVerifier, TokenStore tokenStore,
                                                 OpenIDConnectURLValidator urlValidator) {
        this.clientDAO = clientDAO;
        this.providerSettingsFactory = providerSettingsFactory;
        this.tokenVerifier = tokenVerifier;
        this.tokenStore = tokenStore;
        this.urlValidator = urlValidator;
    }

    /**
     * {@inheritDoc}
     */
    public JsonValue createRegistration(String accessToken, String deploymentUrl, OAuth2Request request)
            throws InvalidRedirectUri, InvalidClientMetadata, ServerException, UnsupportedResponseTypeException,
            AccessDeniedException, NotFoundException {

        final OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);

        if (!providerSettings.isOpenDynamicClientRegistrationAllowed()) {
            if (!tokenVerifier.verify(request).isValid()) {
                throw new AccessDeniedException("Access Token not valid");
            }
        }

        final JsonValue input = request.getBody();

        //check input to ensure it is valid
        Set<String> inputKeys = input.keys();
        for (String key : inputKeys) {
            OAuth2Constants.ShortClientAttributeNames keyName = fromString(key);
            if (keyName == null) {
                logger.warn("Unknown input given. Key: " + key);
            }
        }

        //create client given input
        ClientBuilder clientBuilder = new ClientBuilder();
        try {

            boolean jwks = false;

            if (input.get(JWKS.getType()).asString() != null) {
                jwks = true;

                try {
                    JsonValueBuilder.toJsonValue(input.get(JWKS.getType()).asString());
                } catch (JsonException e) {
                    throw new InvalidClientMetadata("jwks must be valid JSON.");
                }

                clientBuilder.setJwks(input.get(JWKS.getType()).asString());
                clientBuilder.setPublicKeySelector(Client.PublicKeySelector.JWKS.getType());
            }

            if (input.get(JWKS_URI.getType()).asString() != null) {
                if (jwks) { //allowed to set either jwks or jwks_uri but not both
                    throw new InvalidClientMetadata("Must define either jwks or jwks_uri, not both.");
                }
                jwks = true;

                try {
                    new URL(input.get(JWKS_URI.getType()).asString());
                } catch (MalformedURLException e) {
                    throw new InvalidClientMetadata("jwks_uri must be a valid URL.");
                }

                clientBuilder.setJwksUri(input.get(JWKS_URI.getType()).asString());
                clientBuilder.setPublicKeySelector(Client.PublicKeySelector.JWKS_URI.getType());
            }

            //not spec-defined, this is OpenAM proprietary
            if (input.get(X509.getType()).asString() != null) {
                clientBuilder.setX509(input.get(X509.getType()).asString());
            }

            //drop to this if neither other are set
            if (!jwks) {
                clientBuilder.setPublicKeySelector(Client.PublicKeySelector.X509.getType());
            }

            if (input.get(TOKEN_ENDPOINT_AUTH_METHOD.getType()).asString() != null) {
                if (Client.TokenEndpointAuthMethod.fromString(
                        input.get(TOKEN_ENDPOINT_AUTH_METHOD.getType()).asString()) == null) {
                    logger.error("Invalid token_endpoint_auth_method requested.");
                    throw new InvalidClientMetadata("Invalid token_endpoint_auth_method requested.");
                }
                clientBuilder.setTokenEndpointAuthMethod(input.get(TOKEN_ENDPOINT_AUTH_METHOD.getType()).asString());
            } else {
                clientBuilder.setTokenEndpointAuthMethod(Client.TokenEndpointAuthMethod.CLIENT_SECRET_BASIC.getType());
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
                    logger.error("Invalid client_type requested.");
                    throw new InvalidClientMetadata("Invalid client_type requested");
                }
            } else {
                clientBuilder.setClientType(Client.ClientType.CONFIDENTIAL.getType());
            }

            if (input.get(DEFAULT_MAX_AGE.getType()).asLong() != null) {
                clientBuilder.setDefaultMaxAge(input.get(DEFAULT_MAX_AGE.getType()).asLong());
                clientBuilder.setDefaultMaxAgeEnabled(true);
            } else {
                clientBuilder.setDefaultMaxAge(Client.MIN_DEFAULT_MAX_AGE);
                clientBuilder.setDefaultMaxAgeEnabled(false);
            }

            List<String> redirectUris = new ArrayList<String>();

            if (input.get(REDIRECT_URIS.getType()).asList() != null) {
                redirectUris = input.get(REDIRECT_URIS.getType()).asList(String.class);
                boolean isValidUris = true;
                for (String redirectUri : redirectUris) {
                    try {
                        urlValidator.validate(redirectUri);
                    } catch (ValidationException e) {
                        isValidUris = false;
                        logger.error("The redirectUri: " + redirectUri + " is invalid.");
                    }
                }
                if (!isValidUris) {
                    throw new InvalidRedirectUri();
                }
                clientBuilder.setRedirectionURIs(redirectUris);
            }

            if (input.get(SECTOR_IDENTIFIER_URI.getType()).asString() != null) {
                try {
                    URL sectorIdentifier = new URL(input.get(SECTOR_IDENTIFIER_URI.getType()).asString());
                    List<String> response = mapper.readValue(sectorIdentifier, List.class);

                    if (!response.containsAll(redirectUris)) {
                        logger.error("Request_uris not included in sector_identifier_uri.");
                        throw new InvalidClientMetadata();
                    }
                } catch (Exception e) {
                    logger.error("Invalid sector_identifier_uri requested.");
                    throw new InvalidClientMetadata("Invalid sector_identifier_uri requested.");
                }
                clientBuilder.setSectorIdentifierUri(input.get(SECTOR_IDENTIFIER_URI.getType()).asString());
            }

            List<String> scopes = input.get(SCOPES.getType()).asList(String.class);
            if (scopes != null && !scopes.isEmpty()) {
                if (!containsAllCaseInsensitive(providerSettings.getSupportedScopes(), scopes)) {
                    logger.error("Invalid scopes requested.");
                    throw new InvalidClientMetadata("Invalid scopes requested");
                }
            } else { //if nothing requested, fall back to provider defaults
                scopes = new ArrayList<String>();
                scopes.addAll(providerSettings.getDefaultScopes());
            }

            //regardless, we add openid
            if (!scopes.contains(OPENID)) {
                scopes = new ArrayList<String>(scopes);
                scopes.add(OPENID);
            }

            clientBuilder.setAllowedGrantScopes(scopes);

            List<String> defaultScopes = input.get(DEFAULT_SCOPES.getType()).asList(String.class);
            if (defaultScopes != null) {
                if (containsAllCaseInsensitive(providerSettings.getSupportedScopes(), defaultScopes)) {
                    clientBuilder.setDefaultGrantScopes(defaultScopes);
                } else {
                    throw new InvalidClientMetadata("Invalid default scopes requested.");
                }
            }

            if (input.get(CLIENT_NAME.getType()).asString() != null) {
                clientBuilder.setClientName(input.get(CLIENT_NAME.getType()).asString());
            }

            if (input.get(CLIENT_DESCRIPTION.getType()).asList() != null) {
                clientBuilder.setDisplayDescription(input.get(CLIENT_DESCRIPTION.getType()).asList(String.class));
            }

            if (input.get(SUBJECT_TYPE.getType()).asString() != null) {
                if (providerSettings.getSupportedSubjectTypes().contains(input.get(SUBJECT_TYPE.getType()).asString())) {
                    clientBuilder.setSubjectType(input.get(SUBJECT_TYPE.getType()).asString());
                } else {
                    logger.error("Invalid subject_type requested.");
                    throw new InvalidClientMetadata("Invalid subject_type requested");
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
                    throw new InvalidClientMetadata("Unsupported id_token_response_signed_alg requested.");
                }
            } else {
                clientBuilder.setIdTokenSignedResponseAlgorithm(ID_TOKEN_SIGNED_RESPONSE_ALG_DEFAULT);
            }

            if (input.get(POST_LOGOUT_REDIRECT_URIS.getType()).asList() != null) {
                clientBuilder.setPostLogoutRedirectionURIs(input.get(POST_LOGOUT_REDIRECT_URIS.getType())
                        .asList(String.class));
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
                    throw new InvalidClientMetadata("Invalid application_type requested.");
                }
            } else {
                clientBuilder.setApplicationType(DEFAULT_APPLICATION_TYPE);
            }

            if (input.get(DISPLAY_NAME.getType()).asList() != null) {
                clientBuilder.setDisplayName(input.get(DISPLAY_NAME.getType()).asList(String.class));
            }

            if (input.get(RESPONSE_TYPES.getType()).asList() != null) {
                final List<String> clientResponseTypeList = input.get(RESPONSE_TYPES.getType()).asList(String.class);
                final List<String> typeList = new ArrayList<String>();
                for (String responseType : clientResponseTypeList) {
                    typeList.addAll(Arrays.asList(responseType.split(" ")));
                }
                if (containsAllCaseInsensitive(providerSettings.getAllowedResponseTypes().keySet(), typeList)) {
                    clientBuilder.setResponseTypes(clientResponseTypeList);
                } else {
                    logger.error("Invalid response_types requested.");
                    throw new InvalidClientMetadata("Invalid response_types requested.");
                }
            } else {
                List<String> defaultResponseTypes = new ArrayList<String>();
                defaultResponseTypes.add("code");
                clientBuilder.setResponseTypes(defaultResponseTypes);
            }

            if (input.get(CONTACTS.getType()).asList() != null) {
                clientBuilder.setContacts(input.get(CONTACTS.getType()).asList(String.class));
            }

        } catch (JsonValueException e) {
            logger.error("Unable to build client.", e);
            throw new InvalidClientMetadata();
        }

        Client client = clientBuilder.createClient();

        // If the client registered without an access token, generate one to allow access to the configuration endpoint
        // See OPENAM-3604 and http://openid.net/specs/openid-connect-registration-1_0.html#ClientRegistration
        if (providerSettings.isRegistrationAccessTokenGenerationEnabled() && !client.hasAccessToken()) {
            client.setAccessToken(createRegistrationAccessToken(client, request));
        }

        clientDAO.create(client, request);

        // Ensure client registrations are logged so that in an open dymamic registration environment, the admins can
        // have some visibility on who is registering clients.
        if (logger.isInfoEnabled()) {
            logger.info("Registered OpenID Connect client: " + client.getClientID()
                    + ", name=" + client.getClientName() + ", type=" + client.getClientType());
        }

        Map<String, Object> response = client.asMap();

        response.put(REGISTRATION_CLIENT_URI,
                deploymentUrl + "/oauth2/connect/register?client_id=" + client.getClientID());
        response.put(ISSUED_AT, System.currentTimeMillis() / 1000);

        response.put(EXPIRES_AT, 0);

        return new JsonValue(response);
    }

    /**
     * Creates a fresh registration access token for the case where open dynamic registration is enabled and the client
     * has registered without providing an access token. This allows the client to use the client registration endpoint
     * to manage their registration.
     *
     * @param client the client to issue the access token for.
     * @param request the OAuth2 request.
     * @return the token id of the generated access token.
     * @throws ServerException if an internal error occurs.
     */
    private String createRegistrationAccessToken(Client client, OAuth2Request request) throws ServerException, NotFoundException {
        final AccessToken rat = tokenStore.createAccessToken(
                null,                           // Grant type
                OAuth2Constants.Bearer.BEARER,  // Access Token Type
                null,                           // Authorization Code
                client.getClientID(),           // Resource Owner ID
                client.getClientID(),           // Client ID
                null,                           // Redirect URI
                Collections.<String>emptySet(), // Scopes
                null,                           // Refresh Token
                null,                           // Nonce
                null,                           // Claims
                request);                       // Request
        return rat.getTokenId();
    }

    private boolean containsAllCaseInsensitive(final Set<String> collection, final List<String> values) {
        for (String value : values) {
            boolean foundInCollection = false;
            for (String item : collection) {
                if (item.equalsIgnoreCase(value)) {
                    foundInCollection = true;
                    break;
                }
            }
            if (!foundInCollection) {
                return false;
            }
        }
        return true;
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
            throws InvalidRequestException, InvalidClientMetadata, InvalidTokenException {
        if (clientId != null) {
            final Client client;
            try {
                client = clientDAO.read(clientId, request);
            } catch (UnauthorizedClientException e) {
                logger.error("ConnectClientRegistration.Validate(): Unable to create client", e);
                throw new InvalidClientMetadata();
            }

            if (!client.getAccessToken().equals(accessToken)) {
                //client access token doesn't match the access token supplied in the request
                logger.error("ConnectClientRegistration.getClient(): Invalid accessToken");
                throw new InvalidTokenException();
            }

            //remove the client fields that don't need to be reported.
            client.remove(CLIENT_SECRET.getType());
            client.remove(REGISTRATION_ACCESS_TOKEN.getType());

            return new JsonValue(client.asMap());
        } else {
            logger.error("ConnectClientRegistration.readRequest(): No client id sent");
            throw new InvalidRequestException();
        }

    }
}
