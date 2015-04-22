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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openidconnect;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Models a OAuth2 Client.
 * 
 * @since 12.0.0
 */
public class Client extends JsonValue {

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");

    public static final long MIN_DEFAULT_MAX_AGE = 1;
    //set param to this after max_age check, necessary in case default_max_age is 1.
    public static final String CONFIRMED_MAX_AGE = "60";

    /**
     * Stores the possible OAuth2Client types.
     * 
     * @since 12.0.0
     */
    public enum ClientType {
        /** Confidential Client Type. */
        CONFIDENTIAL("Confidential"), 
        /** Public Client Type. */
        PUBLIC("Public");

        private String type;

        /**
         * Constructs a new ClientType.
         * 
         * @param type The client type.
         */
        ClientType(String type) {
            this.type = type;
        }

        /**
         * Gets the client type.
         * 
         * @return The client type.
         */
        public String getType() {
            return this.type;
        }

        /**
         * Gets the client type from a String.
         * 
         * @param type The String to translate into a client type.
         * @return A client type if found, else null.
         */
        public static ClientType fromString(String type) {
            if (type != null) {
                for (ClientType clientType : ClientType.values()) {
                    if (type.equalsIgnoreCase(clientType.type)) {
                        return clientType;
                    }
                }
            }
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return type;
        }
    }

    /**
     * Stores a Client SubjectType.
     * 
     * @since 13.0.0
     */
    public enum SubjectType {
        /** Pairwise Subject Type. */
        PAIRWISE("Pairwise"), 
        /** Public Subject Type. */
        PUBLIC("Public");

        private String type;

        /**
         * Constructs a new SubjectType.
         * 
         * @param type The subject type.
         */
        SubjectType(String type) {
            this.type = type;
        }

        /**
         * Gets the subject type.
         * 
         * @return The client subject type.
         */
        public String getType() {
            return this.type;
        }

        /**
         * Translates a String type into a subject type.
         * 
         * @param type The string to translate into a subject type.
         * @return A subject type if one is found, else {@code null}.
         */
        public static SubjectType fromString(String type) {
            if (type != null) {
                for (SubjectType subjectType : SubjectType.values()) {
                    if (type.equalsIgnoreCase(subjectType.type)) {
                        return subjectType;
                    }
                }
            }
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return type;
        }
    }

    /**
     * Stores a client application type.
     * 
     * @since 12.0.0
     */
    public enum ApplicationType {
        /** Web Application Type. */
        WEB("web"),
        /** Native Application Type. */
        NATIVE("native");

        private String type;

        /**
         * Constructs a new Application Type.
         * 
         * @param type The type of application.
         */
        ApplicationType(String type) {
            this.type = type;
        }

        /**
         * Gets the client application type.
         * 
         * @return The client application type.
         */
        public String getType() {
            return this.type;
        }

        /**
         * Translates a string into an application type.
         * 
         * @param type The string to translate into a application type.
         * @return An application type if found, else {@code null}.
         */
        public static ApplicationType fromString(String type) {
            if (type != null) {
                for (ApplicationType applicationType : ApplicationType.values()) {
                    if (type.equalsIgnoreCase(applicationType.type)) {
                        return applicationType;
                    }
                }
            }
            return null;
        }
    }

    /**
     * Stores a token endpoint auth method type.
     *
     * @since 13.0.0
     */
    public enum TokenEndpointAuthMethod {
        /** Client secret post type. */
        CLIENT_SECRET_POST("client_secret_post"),
        /** Client secret basic type. */
        CLIENT_SECRET_BASIC("client_secret_basic"),
        /** Client secret post type. */
     //   CLIENT_SECRET_JWT("client_secret_jwt"), todo uncomment as we add suppot
        /** Client secret basic type. */
        PRIVATE_KEY_JWT("private_key_jwt");
        /** None type. */
     //   NONE("none");

        private String type;

        /**
         * Constructs a new Token Endpoint Auth Method type.
         *
         * @param type The type of token endpoint auth method.
         */
        TokenEndpointAuthMethod(String type) {
            this.type = type;
        }

        /**
         * Gets the client token endpoint auth method type.
         *
         * @return The client application type.
         */
        public String getType() {
            return this.type;
        }

        /**
         * Translates a string into a token endpoint auth method type.
         *
         * @param type The string to translate into a token endpoint auth method.
         * @return An application type if found, else {@code null}.
         */
        public static TokenEndpointAuthMethod fromString(String type) {
            if (type != null) {
                for (TokenEndpointAuthMethod authMethod : TokenEndpointAuthMethod.values()) {
                    if (type.equalsIgnoreCase(authMethod.type)) {
                        return authMethod;
                    }
                }
            }
            return null;
        }
    }

    /**
     * Stores the selected public key locator for this client.
     *
     * @since 13.0.0
     */
    public enum PublicKeySelector {
        /** JWKs URI */
        JWKS_URI("jwks_uri"),
        /** JWKs */
        JWKS("jwks"),
        /** X509 */
        X509("x509");

        private String type;

        /**
         * Constructs a new Token Endpoint Auth Method type.
         *
         * @param type The type of token endpoint auth method.
         */
        PublicKeySelector(String type) {
            this.type = type;
        }

        /**
         * Gets the client token endpoint auth method type.
         *
         * @return The client application type.
         */
        public String getType() {
            return this.type;
        }

        /**
         * Translates a string into a token endpoint auth method type.
         *
         * @param type The string to translate into a token endpoint auth method.
         * @return An application type if found, else {@code null}.
         */
        public static PublicKeySelector fromString(String type) {
            if (type != null) {
                for (PublicKeySelector keySelector : PublicKeySelector.values()) {
                    if (type.equalsIgnoreCase(keySelector.type)) {
                        return keySelector;
                    }
                }
            }
            return null;
        }
    }

    /**
     * Creates a OAuth2Client.
     *
     * @param clientID                       The client id of the client.
     * @param clientType                     The client type of the client.
     * @param redirectionURIs                The redirection uris of the client.
     * @param allowedGrantScopes             The allowed scopes of the client.
     * @param defaultGrantScopes             The default scopes of the client.
     * @param displayName                    The display name of the client
     * @param displayDescription             The client description of the client.
     * @param clientName                     The client name of the client.
     * @param subjectType                    The subject type of the client.
     * @param idTokenSignedResponseAlgorithm The id token signed response algorithm of the client.
     * @param postLogoutRedirectionURIs      The post logout redirect URIs of the client.
     * @param accessToken                    The registration access token of the client.
     * @param clientSessionURI               The client session uri of the client.
     * @param applicationType                The application type of the client.
     * @param clientSecret                   The client secret of the client.
     * @param responseTypes                  The response types of the client.
     * @param contacts                       The contact information for the client (can be null).
     * @param defaultMaxAge                  The default maximum age a session can be when authorizing, in seconds.
     * @param defaultMaxAgeEnabled           Whether we enforce the defaultMaxAge parameter or not.
     * @param tokenEndpointAuthMethod        The token endpoint's authentication method.
     * @param jwks                           The client's public key jwks.
     * @param jwksUri                        The client's public key jwks_uri.
     * @param x509                           The client's public key x509.
     * @param selector                       Selector for the public key format.
     * @param sectorIdUri                    Client's sector ID Uri.
     */
    public Client(String clientID, String clientType, List<String> redirectionURIs, List<String> allowedGrantScopes,
                  List<String> defaultGrantScopes, List<String> displayName, List<String> displayDescription,
                  String clientName, String subjectType, String idTokenSignedResponseAlgorithm,
                  List<String> postLogoutRedirectionURIs, String accessToken, String clientSessionURI, String applicationType,
                  String clientSecret, List<String> responseTypes, List<String> contacts, Long defaultMaxAge,
                  Boolean defaultMaxAgeEnabled, String tokenEndpointAuthMethod, String jwks,
                  String jwksUri, String x509, String selector, String sectorIdUri) {
        super(new HashMap<String, Object>());
        setAccessToken(accessToken);
        setAllowedGrantScopes(allowedGrantScopes);
        setClientID(clientID);
        setClientSessionURI(clientSessionURI);
        setClientType(clientType);
        setDefaultGrantScopes(defaultGrantScopes);
        setDisplayDescription(displayDescription);
        setDisplayName(displayName);
        setClientName(clientName);
        setSubjectType(subjectType);
        setIdTokenSignedResponseAlgorithm(idTokenSignedResponseAlgorithm);
        setPostLogoutRedirectionURIs(postLogoutRedirectionURIs);
        setRedirectionURIs(redirectionURIs);
        setApplicationType(applicationType);
        setClientSecret(clientSecret);
        setResponseTypes(responseTypes);
        setContacts(contacts);
        setDefaultMaxAge(defaultMaxAge);
        setDefaultMaxAgeEnabled(defaultMaxAgeEnabled);
        setTokenEndpointAuthMethod(tokenEndpointAuthMethod);
        setJwks(jwks);
        setJwksUri(jwksUri);
        setX509(x509);
        setKeySelector(selector);
        setSectorIdUri(sectorIdUri);
    }

    /**
     * Sets JWKs for this client.
     */
    private void setJwks(String jwks) {
        if (!StringUtils.isBlank(jwks)) {
            put(OAuth2Constants.ShortClientAttributeNames.JWKS.getType(), jwks);
        }
    }

    /**
     * Gets JWKs for this client.
     */
    public String getJwks() {
        return get(OAuth2Constants.ShortClientAttributeNames.JWKS.getType()).asString();
    }

    /**
     * Gets JWKs_URI for this client.
     */
    public String getJwksUri() {
        return get(OAuth2Constants.ShortClientAttributeNames.JWKS_URI.getType()).asString();
    }

    /**
     * Gets X509 certificate for this client.
     */
    public String getX509() {
        return get(OAuth2Constants.ShortClientAttributeNames.X509.getType()).asString();
    }

    /**
     * Returns the key selector for this client.
     */
    public String getKeySelector() {
        return get(OAuth2Constants.ShortClientAttributeNames.PUBLIC_KEY_SELECTOR.getType()).asString();
    }

    /**
     * JWKs_URI for this client.
     */
    private void setJwksUri(String jwksUri) {
        if (!StringUtils.isBlank(jwksUri)) {
            put(OAuth2Constants.ShortClientAttributeNames.JWKS_URI.getType(), jwksUri);
        }
    }

    /**
     * X590 certificate w/ public key for this client.
     */
    private void setX509(String x509) {
        if (!StringUtils.isBlank(x509)) {
            put(OAuth2Constants.ShortClientAttributeNames.X509.getType(), x509);
        }
    }

    /**
     * Sets which of the public key types this client uses.
     */
    private void setKeySelector(String selector) {
        if (!StringUtils.isBlank(selector)) {
            put(OAuth2Constants.ShortClientAttributeNames.PUBLIC_KEY_SELECTOR.getType(), selector);
        }
    }

    /**
     * Gets the client id of the OAuth2Client.
     *
     * @return The client id.
     */
    public String getClientID() {
        return get(OAuth2Constants.ShortClientAttributeNames.CLIENT_ID.getType()).asString();
    }

    /**
     * Gets the default max age of the OAuth2Client.
     *
     * @return the default max age.
     */
    public Long getDefaultMaxAge() {
        return get(OAuth2Constants.ShortClientAttributeNames.DEFAULT_MAX_AGE.getType()).asLong();
    }

    /**
     * Gets whether the max age value is enforced.
     *
     * @return true if enforced, false otherwise.
     */
    public Boolean getDefaultMaxAgeEnabled() {
        return get(OAuth2Constants.ShortClientAttributeNames.DEFAULT_MAX_AGE_ENABLED.getType()).asBoolean();
    }

    /**
     * Sets the client id of the OAuth2Client.
     *
     * @param clientID The client id.
     */
    public void setClientID(String clientID) {
        if (!StringUtils.isBlank(clientID)) {
            put(OAuth2Constants.ShortClientAttributeNames.CLIENT_ID.getType(), clientID);
        }
    }

    /**
     * Gets the client type of the OAuth2Client. If the client type is {@code null}, {@code null} is returned.
     *
     * @return The client type.
     */
    public ClientType getClientType() {
        return ClientType.fromString(get(OAuth2Constants.ShortClientAttributeNames.CLIENT_TYPE.getType()).asString());
    }

    /**
     * Sets the client type of the OAuth2Client.
     *
     * @param clientType The client type.
     */
    public void setClientType(String clientType) {
        if (clientType != null && !clientType.isEmpty()) {
            put(OAuth2Constants.ShortClientAttributeNames.CLIENT_TYPE.getType(), clientType);
        }
    }

    /**
     * Gets the redirection uris of the OAuth2Client. If the redirection uris is {@code null}, {@code null} is 
     * returned.
     *
     * @return The redirection uris as URIs.
     */
    public Set<URI> getRedirectionURIs() {
        List<String> listOfRedirectURIs =
                get(OAuth2Constants.ShortClientAttributeNames.REDIRECT_URIS.getType()).asList(String.class);
        if (listOfRedirectURIs == null) {
            return null;
        }
        Set<URI> setOfURIs = new HashSet<URI>();
        try {
            for (String stringURI : listOfRedirectURIs) {
                URI uri = new URI(stringURI);
                setOfURIs.add(uri);
            }
        } catch (Exception e) {
            logger.error("Client.getRedirectionURIs(): Unable to parse uri", e);
        }
        return setOfURIs;
    }

    /**
     * Gets the redirection uris of the OAuth2Client. If the redirection uris is {@code null}, {@code null} is returned.
     *
     * @return The redirection uris as Strings.
     */
    public Set<String> getRedirectionURIsAsString() {
        if (get(OAuth2Constants.ShortClientAttributeNames.REDIRECT_URIS.getType()).asList(String.class) != null) {
            return new HashSet<String>(get(OAuth2Constants.ShortClientAttributeNames.REDIRECT_URIS.getType())
                    .asList(String.class));
        } else {
            return null;
        }
    }

    /**
     * Sets the redirection uris of the OAuth2Client.
     *
     * @param redirectionURIs The redirection uris.
     */
    public void setRedirectionURIs(List<String> redirectionURIs) {
        if (redirectionURIs != null && !redirectionURIs.isEmpty()) {
            put(OAuth2Constants.ShortClientAttributeNames.REDIRECT_URIS.getType(), redirectionURIs);
        }
    }

    /**
     * Sets the sector id uri.
     */
    public void setSectorIdUri(String sectorIdUri) {
        if (!StringUtils.isBlank(sectorIdUri)) {
            put(OAuth2Constants.ShortClientAttributeNames.SECTOR_IDENTIFIER_URI.getType(), sectorIdUri);
        }
    }

    /**
     * Returns the sector id uri for this client if set.
     */
    public String getSectorIdUri() {
        if (get(OAuth2Constants.ShortClientAttributeNames.SECTOR_IDENTIFIER_URI.getType()).asString() != null) {
            return get(OAuth2Constants.ShortClientAttributeNames.SECTOR_IDENTIFIER_URI.getType()).asString();
        } else {
            return null;
        }
    }

    /**
     * Gets the allowed scopes of the OAuth2Client. If the allowed scopes is {@code null}, {@code null} is returned.
     *
     * @return The allowed scopes.
     */
    public Set<String> getAllowedGrantScopes() {
        if (get(OAuth2Constants.ShortClientAttributeNames.SCOPES.getType()).asList(String.class) != null) {
            return new HashSet<String>(get(OAuth2Constants.ShortClientAttributeNames.SCOPES.getType())
                    .asList(String.class));
        } else {
            return null;
        }
    }

    /**
     * Sets the allowed scopes of the OAuth2Client.
     *
     * @param allowedGrantScopes The allowed scopes.
     */
    public void setAllowedGrantScopes(List<String> allowedGrantScopes) {
        if (allowedGrantScopes != null && !allowedGrantScopes.isEmpty()) {
            put(OAuth2Constants.ShortClientAttributeNames.SCOPES.getType(), allowedGrantScopes);
        }
    }

    /**
     * Gets the default scopes of the OAuth2Client. If the default scopes is {@code null}, {@code null}, is returned.
     *
     * @return The default scopes.
     */
    public Set<String> getDefaultGrantScopes() {
        if (get(OAuth2Constants.ShortClientAttributeNames.DEFAULT_SCOPES.getType()).asList(String.class) != null) {
            return new HashSet<String>(get(OAuth2Constants.ShortClientAttributeNames.DEFAULT_SCOPES.getType())
                    .asList(String.class));
        } else {
            return new HashSet<String>();
        }
    }

    /**
     * Sets the default scopes of the OAuth2Client.
     *
     * @param defaultGrantScopes The default scopes.
     */
    public void setDefaultGrantScopes(List<String> defaultGrantScopes) {
        if (defaultGrantScopes != null && !defaultGrantScopes.isEmpty()) {
            put(OAuth2Constants.ShortClientAttributeNames.DEFAULT_SCOPES.getType(), defaultGrantScopes);
        }
    }

    /**
     * Gets the display names of the OAuth2Client. If the display names is {@code null}, {@code null} is returned.
     *
     * @return The display names.
     */
    public Set<String> getDisplayName() {
        if (get(OAuth2Constants.ShortClientAttributeNames.DISPLAY_NAME.getType()).asList(String.class) != null) {
            return new HashSet<String>(get(OAuth2Constants.ShortClientAttributeNames.DISPLAY_NAME.getType())
                    .asList(String.class));
        } else {
            return null;
        }
    }

    /**
     * Sets the display names of the OAuth2Client.
     *
     * @param displayName The display names.
     */
    public void setDisplayName(List<String> displayName) {
        if (displayName != null && !displayName.isEmpty()) {
            put(OAuth2Constants.ShortClientAttributeNames.DISPLAY_NAME.getType(), displayName);
        }
    }

    /**
     * Gets the client descriptions of the OAuth2Client. If the client descriptions is {@code null}, {@code null} is
     * returned.
     *
     * @return The client descriptions.
     */
    public Set<String> getDisplayDescription() {
        if (get(OAuth2Constants.ShortClientAttributeNames.CLIENT_DESCRIPTION.getType()).asList(String.class) != null) {
            return new HashSet<String>(get(OAuth2Constants.ShortClientAttributeNames.CLIENT_DESCRIPTION.getType())
                    .asList(String.class));
        } else {
            return null;
        }
    }

    /**
     * Sets the client descriptions of the OAuth2Client.
     *
     * @param displayDescription The client descriptions.
     */
    public void setDisplayDescription(List<String> displayDescription) {
        if (displayDescription != null && !displayDescription.isEmpty()) {
            put(OAuth2Constants.ShortClientAttributeNames.CLIENT_DESCRIPTION.getType(), displayDescription);
        }
    }

    /**
     * Gets the client name of the OAuth2Client. If the client name is {@code null}, {@code null} is returned.
     *
     * @return The client name.
     */
    public String getClientName() {
        if (get(OAuth2Constants.ShortClientAttributeNames.CLIENT_NAME.getType()).asString() != null) {
            return get(OAuth2Constants.ShortClientAttributeNames.CLIENT_NAME.getType()).asString();
        } else {
            return null;
        }
    }

    /**
     * Sets the client name of the OAuth2Client.
     *
     * @param clientName The client name.
     */
    public void setClientName(String clientName) {
        if (StringUtils.isBlank(clientName)) {
            put(OAuth2Constants.ShortClientAttributeNames.CLIENT_NAME.getType(), clientName);
        }
    }

    /**
     * Gets the subject type of the OAuth2Client.
     *
     * @return The subject type.
     */
    public SubjectType getSubjectType() {
        return SubjectType.fromString(get(OAuth2Constants.ShortClientAttributeNames.SUBJECT_TYPE.getType()).asString());
    }

    /**
     * Sets the subject type of the OAuth2Client.
     *
     * @param subjectType The subject type.
     */
    public void setSubjectType(String subjectType) {
        if (!StringUtils.isBlank(subjectType)) {
            put(OAuth2Constants.ShortClientAttributeNames.SUBJECT_TYPE.getType(), subjectType);
        }
    }

    /**
     * Gets the id token signed response alg of the OAuth2Client. 
     *
     * @return The id token signed response alg.
     */
    public String getIdTokenSignedResponseAlgorithm() {
        return get(OAuth2Constants.ShortClientAttributeNames.ID_TOKEN_SIGNED_RESPONSE_ALG.getType()).asString();
    }

    /**
     * Sets the id token signed response alg of the OAuth2Client.
     * <br/>
     * If the specified idTokenSignedResponseAlgorithm is {@code null} the value is defaulted to 'HS256'.
     *
     * @param idTokenSignedResponseAlgorithm The id token signed response alg.
     */
    public void setIdTokenSignedResponseAlgorithm(String idTokenSignedResponseAlgorithm) {
        if (idTokenSignedResponseAlgorithm != null && !idTokenSignedResponseAlgorithm.isEmpty()) {
            put(OAuth2Constants.ShortClientAttributeNames.ID_TOKEN_SIGNED_RESPONSE_ALG.getType(), 
                    idTokenSignedResponseAlgorithm);
        } else {
            put(OAuth2Constants.ShortClientAttributeNames.ID_TOKEN_SIGNED_RESPONSE_ALG.getType(), "HS256");
        }
    }

    /**
     * Gets the post logout redirection uri of the OAuth2Client. If the logout redirection uri is {@code null},
     * {@code null} is returned.
     *
     * @return The post logout redirection uri.
     */
    public List<String> getPostLogoutRedirectionURIs() {
        return get(OAuth2Constants.ShortClientAttributeNames.POST_LOGOUT_REDIRECT_URIS.getType()).asList(String.class);
    }

    /**
     * Sets the post logout redirection uri of the OAuth2Client.
     *
     * @param postLogoutRedirectionURIs The post logout redirection uri.
     */
    public void setPostLogoutRedirectionURIs(List<String> postLogoutRedirectionURIs) {
        if (postLogoutRedirectionURIs != null && !postLogoutRedirectionURIs.isEmpty()) {
            put(OAuth2Constants.ShortClientAttributeNames.POST_LOGOUT_REDIRECT_URIS.getType(), 
                    postLogoutRedirectionURIs);
        }
    }

    /**
     * Gets the registration access token of the OAuth2Client. If the registration access token is {@code null}, 
     * {@code null} is returned.
     *
     * @return The registration access token.
     */
    public String getAccessToken() {
        return get(OAuth2Constants.ShortClientAttributeNames.REGISTRATION_ACCESS_TOKEN.getType()).asString();
    }

    /**
     * Sets the registration access token of the OAuth2Client.
     *
     * @param accessToken The registration access token.
     */
    public void setAccessToken(String accessToken) {
        if (accessToken != null && !accessToken.isEmpty()) {
            put(OAuth2Constants.ShortClientAttributeNames.REGISTRATION_ACCESS_TOKEN.getType(), accessToken);
        }
    }

    /**
     * Indicates whether this client has a registration access token or not.
     */
    public boolean hasAccessToken() {
        final String token = getAccessToken();
        return token != null && !token.isEmpty();
    }

    /**
     * Gets the client session uri of the OAuth2Client. If the client session uri is {@code null}, {@code null} is 
     * returned.
     *
     * @return The client session uri.
     */
    public String getClientSessionURI() {
        return get(OAuth2Constants.ShortClientAttributeNames.CLIENT_SESSION_URI.getType()).asString();
    }

    /**
     * Sets the client session uri of the OAuth2Client.
     *
     * @param clientSessionURI The client session uri.
     */
    public void setClientSessionURI(String clientSessionURI) {
        if (clientSessionURI != null && !clientSessionURI.isEmpty()) {
            put(OAuth2Constants.ShortClientAttributeNames.CLIENT_SESSION_URI.getType(), clientSessionURI);
        }
    }

    /**
     * Gets the application type of the OAuth2Client. If the application type is {@code null}, {@code null} is returned.
     *
     * @return The application type.
     */
    public ApplicationType getApplicationType() {
        return ApplicationType.fromString(get(OAuth2Constants.ShortClientAttributeNames.APPLICATION_TYPE.getType())
                .asString());
    }

    /**
     * Sets the application type of the OAuth2Client.
     *
     * @param applicationType The application type.
     */
    public void setApplicationType(String applicationType) {
        if (applicationType != null && !applicationType.isEmpty()) {
            put(OAuth2Constants.ShortClientAttributeNames.APPLICATION_TYPE.getType(), applicationType);
        }
    }

    /**
     * Gets the client secret of the OAuth2Client. If the client secret is {@code null}, {@code null} is returned.
     *
     * @return The client's secret.
     */
    public String getClientSecret() {
        return get(OAuth2Constants.ShortClientAttributeNames.CLIENT_SECRET.getType()).asString();
    }

    /**
     * Sets the client secret of the OAuth2Client.
     *
     * @param clientSecret The client's secret.
     */
    public void setClientSecret(String clientSecret) {
        if (clientSecret != null && !clientSecret.isEmpty()) {
            put(OAuth2Constants.ShortClientAttributeNames.CLIENT_SECRET.getType(), clientSecret);
        }
    }

    /**
     * Gets the response types. If the response types value is {@code null}, {@code null} will be returned.
     *
     * @return A {@code Set} of response types.
     */
    public Set<String> getResponseTypes() {
        if (get(OAuth2Constants.ShortClientAttributeNames.RESPONSE_TYPES.getType()).asList(String.class) != null) {
            return new HashSet<String>(get(OAuth2Constants.ShortClientAttributeNames.RESPONSE_TYPES.getType())
                    .asList(String.class));
        } else {
            return null;
        }
    }

    /**
     * Sets the response types of the OAuth2Client.
     *
     * @param responseTypes The response types.
     */
    public void setResponseTypes(List<String> responseTypes) {
        if (responseTypes != null && !responseTypes.isEmpty()) {
            put(OAuth2Constants.ShortClientAttributeNames.RESPONSE_TYPES.getType(), responseTypes);
        }
    }

    /**
     * Sets whether the default max age value is to be utilised.
     *
     * @param defaultMaxAgeEnabled The default max age.
     */
    public void setDefaultMaxAgeEnabled(Boolean defaultMaxAgeEnabled) {
        if (defaultMaxAgeEnabled != null) {
            put(OAuth2Constants.ShortClientAttributeNames.DEFAULT_MAX_AGE_ENABLED.getType(), defaultMaxAgeEnabled);
        }
    }

    /**
     * Gets the token endpoint auth method.
     *
     * @return The token endpoint auth method.
     */
    public TokenEndpointAuthMethod getTokenEndpointAuthMethod() {
        return TokenEndpointAuthMethod.fromString(
                get(OAuth2Constants.ShortClientAttributeNames.TOKEN_ENDPOINT_AUTH_METHOD.getType()).asString());
    }

    /**
     * Sets the token endpoint auth method.
     *
     * @param tokenEndpointAuthMethod the token endpoint auth method to use.
     */
    public void setTokenEndpointAuthMethod(String tokenEndpointAuthMethod) {
        put(OAuth2Constants.ShortClientAttributeNames.TOKEN_ENDPOINT_AUTH_METHOD.getType(),
                tokenEndpointAuthMethod);
    }

    /**
     * Sets the default max age of the OAuth2Client. Minimum enforced value of 60.
     *
     * @param defaultMaxAge The default max age.
     */
    public void setDefaultMaxAge(Long defaultMaxAge) {
        if (defaultMaxAge != null) {
            if (defaultMaxAge < MIN_DEFAULT_MAX_AGE) {
                defaultMaxAge = MIN_DEFAULT_MAX_AGE;
            }
            put(OAuth2Constants.ShortClientAttributeNames.DEFAULT_MAX_AGE.getType(), defaultMaxAge);
        }
    }

    /**
     * Gets the contacts. If the contacts value is {@code null}, {@code null} will be returned.
     *
     * @return A {@code Set} of contacts
     */
    public Set<String> getContacts() {
        List<String> contacts = get(OAuth2Constants.ShortClientAttributeNames.CONTACTS.getType())
                .asList(String.class);
        if (contacts != null) {
            return new HashSet<String>(contacts);
        } else {
            return null;
        }
    }

    /**
     * Sets the contacts of the OAuth2Client.
     *
     * @param contacts The contacts.
     */
    public void setContacts(List<String> contacts) {
        if (contacts != null && !contacts.isEmpty()) {
            put(OAuth2Constants.ShortClientAttributeNames.CONTACTS.getType(), contacts);
        }
    }
}
