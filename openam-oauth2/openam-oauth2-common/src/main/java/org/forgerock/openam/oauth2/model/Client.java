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
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.oauth2.model;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Stores an OAuth2 Client.
 */
public class Client extends JsonValue {

    /**
     * Stores the possible OAuth2Client types.
     */
    public enum ClientType {
        CONFIDENTIAL("Confidential"), PUBLIC("Public");

        private String type;

        ClientType(String type) {
            this.type = type;
        }

        /**
         * Gets the client type.
         * @return The client type as a string.
         */
        public String getType() {
            return this.type;
        }

        /**
         * Gets the client type from a string.
         * @param type The string to translate into a client type.
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

        @Override
        public String toString() {
            return type;
        }
    }

    /**
     * Stores a Client SubjectType.
     */
    public enum SubjectType {
        PAIRWISE("Pairwise"), PUBLIC("Public");

        private String type;

        SubjectType(String type) {
            this.type = type;
        }

        /**
         * Gets the subject type as a string.
         * @return The client subject type as a string.
         */
        public String getType() {
            return this.type;
        }

        /**
         * Translates a string type into a subject type.
         * @param type The string to translate into a subject type.
         * @return A subject type if one is found, else null.
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

        @Override
        public String toString() {
            return type;
        }
    }

    /**
     * Stores a client application type
     */
    public enum ApplicationType {
        WEB("web"), NATIVE("native");

        private String type;

        ApplicationType(String type) {
            this.type = type;
        }

        /**
         * Gets the client application type
         * @return
         */
        public String getType() {
            return this.type;
        }

        /**
         * Translates a string into an application type.
         * @param type The string to translate into a application type.
         * @return An application type if found, else null.
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
     * Creates an OAuth2Client.
     * @param clientID The client_id of the client.
     * @param clientType The client_type of the client.
     * @param redirectionURIs The redirection_uris of the client.
     * @param allowedGrantScopes The scopes of the client.
     * @param defaultGrantScopes The default_scopes of the client.
     * @param displayName The display_name of the client
     * @param displayDescription The client_description of the client.
     * @param clientName The client_name of the client.
     * @param subjectType The subject_type of the client.
     * @param idTokenSignedResponseAlgorithm The idtoken_signed_response_algorithm of the client.
     * @param postLogoutRedirectionURI The post_logout_redirect_uri of the client.
     * @param accessToken The registration_access_token of the client.
     * @param clientSessionURI The client_session_uri of the client.
     * @param applicationType The application_type of the client.
     * @param clientSecret The client_secret of the client.
     * @param responseTypes the response_types of the client.
     */
    public Client(String clientID,
                  String clientType,
                  List<String> redirectionURIs,
                  List<String> allowedGrantScopes,
                  List<String> defaultGrantScopes,
                  List<String> displayName,
                  List<String> displayDescription,
                  String clientName,
                  String subjectType,
                  String idTokenSignedResponseAlgorithm,
                  String postLogoutRedirectionURI,
                  String accessToken,
                  String clientSessionURI,
                  String applicationType,
                  String clientSecret,
                  List<String> responseTypes) {
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
        setPostLogoutRedirectionURI(postLogoutRedirectionURI);
        setRedirectionURIs(redirectionURIs);
        setApplicationType(applicationType);
        setClientSecret(clientSecret);
        setResponseTypes(responseTypes);

    }

    /**
     * Returns the client_id value as a {@code String} object. If the client_id value is
     * {@code null}, this method returns {@code null}.
     *
     * @return the string value.
     */
    public String getClientID() {
        return get(OAuth2Constants.ShortClientAttributeNames.CLIENT_ID.getType()).asString();
    }

    /**
     * Sets the client_id of the OAuth2Client
     *
     * @param clientID
     *            the client_id to set.
     */
    public void setClientID(String clientID) {
        if (clientID != null && !clientID.isEmpty()) {
            put(OAuth2Constants.ShortClientAttributeNames.CLIENT_ID.getType(), clientID);
        }
    }

    /**
     * Returns the client_type value as a {@code ClientType} object. If the client_type value is
     * {@code null}, this method returns {@code null}.
     *
     * @return the ClientType value.
     */
    public ClientType getClientType() {
        return ClientType.fromString(get(OAuth2Constants.ShortClientAttributeNames.CLIENT_TYPE.getType()).asString());
    }

    /**
     * Sets the client_type of the OAuth2Client
     *
     * @param clientType
     *            the client_id to set.
     */
    public void setClientType(String clientType) {
        if (clientType != null && !clientType.isEmpty()) {
            put(OAuth2Constants.ShortClientAttributeNames.CLIENT_TYPE.getType(), clientType);
        }
    }

    /**
     * Returns the redirection_uris value as a {@code Set} object. If the redirection_uris value is
     * {@code null}, this method returns {@code null}.
     *
     * @return the Set value.
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
            OAuth2Utils.DEBUG.error("Client.getRedirectionURIs(): Unable to parse uri", e);
        }
        return setOfURIs;
    }

    /**
     * Returns the redirection_uris value as a {@code Set} object. If the redirection_uris value is
     * {@code null}, this method returns {@code null}.
     *
     * @return the Set value.
     */
    public Set<String> getRedirectionURIsAsString() {
        if (get(OAuth2Constants.ShortClientAttributeNames.REDIRECT_URIS.getType()).asList(String.class) != null) {
            return new HashSet<String>(get(OAuth2Constants.ShortClientAttributeNames.REDIRECT_URIS.getType()).asList(String.class));
        } else {
            return null;
        }
    }

    /**
     * Sets the redirection_uris of the OAuth2Client
     *
     * @param redirectionURIs
     *            the redirection_uris to set.
     */
    public void setRedirectionURIs(List<String> redirectionURIs) {
        if (redirectionURIs != null && !redirectionURIs.isEmpty()) {
            put(OAuth2Constants.ShortClientAttributeNames.REDIRECT_URIS.getType(), redirectionURIs);
        }
    }

    /**
     * Returns the scopes value as a {@code Set} object. If the scopes value is
     * {@code null}, this method returns {@code null}.
     *
     * @return the Set value.
     */
    public Set<String> getAllowedGrantScopes() {
        if (get(OAuth2Constants.ShortClientAttributeNames.SCOPES.getType()).asList(String.class) != null) {
            return new HashSet<String>(get(OAuth2Constants.ShortClientAttributeNames.SCOPES.getType()).asList(String.class));
        } else {
            return null;
        }
    }

    /**
     * Sets the scopes of the OAuth2Client
     *
     * @param allowedGrantScopes
     *            the scopes to set.
     */
    public void setAllowedGrantScopes(List<String> allowedGrantScopes) {
        if (allowedGrantScopes != null && !allowedGrantScopes.isEmpty()) {
            put(OAuth2Constants.ShortClientAttributeNames.SCOPES.getType(), allowedGrantScopes);
        }
    }

    /**
     * Returns the default_scopes value as a {@code Set} object. If the default_scopes value is
     * {@code null}, this method returns {@code null}.
     *
     * @return the Set value.
     */
    public Set<String> getDefaultGrantScopes() {
        if (get(OAuth2Constants.ShortClientAttributeNames.DEFAULT_SCOPES.getType()).asList(String.class) != null) {
            return new HashSet<String>(get(OAuth2Constants.ShortClientAttributeNames.DEFAULT_SCOPES.getType()).asList(String.class));
        } else {
            return null;
        }
    }

    /**
     * Sets the default_scopes of the OAuth2Client
     *
     * @param defaultGrantScopes
     *            the default_scopes to set.
     */
    public void setDefaultGrantScopes(List<String> defaultGrantScopes) {
        if (defaultGrantScopes != null && !defaultGrantScopes.isEmpty()) {
            put(OAuth2Constants.ShortClientAttributeNames.DEFAULT_SCOPES.getType(), defaultGrantScopes);
        }
    }

    /**
     * Returns the display_name value as a {@code Set} object. If the display_name value is
     * {@code null}, this method returns {@code null}.
     *
     * @return the Set value.
     */
    public Set<String> getDisplayName() {
        if (get(OAuth2Constants.ShortClientAttributeNames.DISPLAY_NAME.getType()).asList(String.class) != null) {
            return new HashSet<String>(get(OAuth2Constants.ShortClientAttributeNames.DISPLAY_NAME.getType()).asList(String.class));
        } else {
            return null;
        }
    }

    /**
     * Sets the display_name of the OAuth2Client
     *
     * @param displayName
     *            the display_names to set.
     */
    public void setDisplayName(List<String> displayName) {
        if (displayName != null && !displayName.isEmpty()) {
            put(OAuth2Constants.ShortClientAttributeNames.DISPLAY_NAME.getType(), displayName);
        }
    }

    /**
     * Returns the client_description value as a {@code Set} object. If the client_description value is
     * {@code null}, this method returns {@code null}.
     *
     * @return the Set value.
     */
    public Set<String> getDisplayDescription() {
        if (get(OAuth2Constants.ShortClientAttributeNames.CLIENT_DESCRIPTION.getType()).asList(String.class) != null) {
            return new HashSet<String>(get(OAuth2Constants.ShortClientAttributeNames.CLIENT_DESCRIPTION.getType()).asList(String.class));
        } else {
            return null;
        }
    }

    /**
     * Sets the client_description of the OAuth2Client
     *
     * @param displayDescription
     *            the client_description to set.
     */
    public void setDisplayDescription(List<String> displayDescription) {
        if (displayDescription != null && !displayDescription.isEmpty()) {
            put(OAuth2Constants.ShortClientAttributeNames.CLIENT_DESCRIPTION.getType(), displayDescription);
        }
    }

    /**
     * Returns the client_name value as a {@code String} object. If the client_name value is
     * {@code null}, this method returns {@code null}.
     *
     * @return the string value.
     */
    public String getClientName() {
        if (get(OAuth2Constants.ShortClientAttributeNames.CLIENT_NAME.getType()).asString() != null) {
            return get(OAuth2Constants.ShortClientAttributeNames.CLIENT_NAME.getType()).asString();
        } else {
            return null;
        }
    }

    /**
     * Sets the client_name of the OAuth2Client
     *
     * @param clientName
     *            the client_name to set.
     */
    public void setClientName(String clientName) {
        if (clientName != null && !clientName.isEmpty()) {
            put(OAuth2Constants.ShortClientAttributeNames.CLIENT_NAME.getType(), clientName);
        }
    }

    /**
     * Returns the subject_type value as a {@code SubjectType} object. If the subject_type value is
     * {@code null}, this method returns {@code null}.
     *
     * @return the SubjectType value.
     */
    public SubjectType getSubjectType() {
        return SubjectType.fromString(get(OAuth2Constants.ShortClientAttributeNames.SUBJECT_TYPE.getType()).asString());
    }

    /**
     * Sets the subject_type of the OAuth2Client
     *
     * @param subjectType
     *            the subject_type to set.
     */
    public void setSubjectType(String subjectType) {
        if (subjectType != null && !subjectType.isEmpty()) {
            put(OAuth2Constants.ShortClientAttributeNames.SUBJECT_TYPE.getType(), subjectType);
        }
    }

    /**
     * Returns the id_token_signed_response_alg value as a {@code String} object. If the id_token_signed_response_alg value is
     * {@code null}, this method returns {@code null}.
     *
     * @return the string value.
     */
    public String getIdTokenSignedResponseAlgorithm() {
        return get(OAuth2Constants.ShortClientAttributeNames.ID_TOKEN_SIGNED_RESPONSE_ALG.getType()).asString();
    }

    /**
     * Sets the id_token_signed_response_alg of the OAuth2Client
     *
     * @param idTokenSignedResponseAlgorithm
     *            the id_token_signed_response_alg to set.
     */
    public void setIdTokenSignedResponseAlgorithm(String idTokenSignedResponseAlgorithm) {
        if (idTokenSignedResponseAlgorithm != null && !idTokenSignedResponseAlgorithm.isEmpty()) {
            put(OAuth2Constants.ShortClientAttributeNames.ID_TOKEN_SIGNED_RESPONSE_ALG.getType(), idTokenSignedResponseAlgorithm);
        } else {
            put(OAuth2Constants.ShortClientAttributeNames.ID_TOKEN_SIGNED_RESPONSE_ALG.getType(), "HS256");
        }
    }

    /**
     * Returns the post_logout_redirection_uri value as a {@code String} object. If the post_logout_redirection_uri value is
     * {@code null}, this method returns {@code null}.
     *
     * @return the string value.
     */
    public String getPostLogoutRedirectionURI() {
        return get(OAuth2Constants.ShortClientAttributeNames.POST_LOGOUT_REDIRECT_URIS.getType()).asString();
    }

    /**
     * Sets the post_logout_redirection_uri of the OAuth2Client
     *
     * @param postLogoutRedirectionURI
     *            the post_logout_redirection_uri to set.
     */
    public void setPostLogoutRedirectionURI(String postLogoutRedirectionURI) {
        if (postLogoutRedirectionURI != null && !postLogoutRedirectionURI.isEmpty()) {
            put(OAuth2Constants.ShortClientAttributeNames.POST_LOGOUT_REDIRECT_URIS.getType(), postLogoutRedirectionURI);
        }
    }

    /**
     * Returns the registration_access_token value as a {@code String} object. If the registration_access_token value is
     * {@code null}, this method returns {@code null}.
     *
     * @return the string value.
     */
    public String getAccessToken() {
        return get(OAuth2Constants.ShortClientAttributeNames.REGISTRATION_ACCESS_TOKEN.getType()).asString();
    }

    /**
     * Sets the registration_access_token of the OAuth2Client
     *
     * @param accessToken
     *            the registration_access_token to set.
     */
    public void setAccessToken(String accessToken) {
        if (accessToken != null && !accessToken.isEmpty()) {
            put(OAuth2Constants.ShortClientAttributeNames.REGISTRATION_ACCESS_TOKEN.getType(), accessToken);
        }
    }

    /**
     * Returns the client_session_uri value as a {@code String} object. If the client_session_uri value is
     * {@code null}, this method returns {@code null}.
     *
     * @return the string value.
     */
    public String getClientSessionURI() {
        return get(OAuth2Constants.ShortClientAttributeNames.CLIENT_SESSION_URI.getType()).asString();
    }

    /**
     * Sets the client_session_uri of the OAuth2Client
     *
     * @param clientSessionURI
     *            the client_session_uri to set.
     */
    public void setClientSessionURI(String clientSessionURI) {
        if (clientSessionURI != null && !clientSessionURI.isEmpty()) {
            put(OAuth2Constants.ShortClientAttributeNames.CLIENT_SESSION_URI.getType(), clientSessionURI);
        }
    }

    /**
     * Returns the application_type value as a {@code ApplicationType} object. If the application_type value is
     * {@code null}, this method returns {@code null}.
     *
     * @return the ApplicationType value.
     */
    public ApplicationType getApplicationType() {
        return ApplicationType.fromString(get(OAuth2Constants.ShortClientAttributeNames.APPLICATION_TYPE.getType()).asString());
    }

    /**
     * Sets the application_type of the OAuth2Client
     *
     * @param applicationType
     *            the application_type to set.
     */
    public void setApplicationType(String applicationType) {
        if (applicationType != null && !applicationType.isEmpty()) {
            put(OAuth2Constants.ShortClientAttributeNames.APPLICATION_TYPE.getType(), applicationType);
        }
    }

    /**
     * Returns the client_secret value as a {@code String} object. If the client_secret value is
     * {@code null}, this method returns {@code null}.
     *
     * @return the string value.
     */
    public String getClientSecret() {
        return get(OAuth2Constants.ShortClientAttributeNames.CLIENT_SECRET.getType()).asString();
    }

    /**
     * Sets the client_secret of the OAuth2Client
     *
     * @param clientSecret
     *            the client_secret to set.
     */
    public void setClientSecret(String clientSecret) {
        if (clientSecret != null && !clientSecret.isEmpty()) {
            put(OAuth2Constants.ShortClientAttributeNames.CLIENT_SECRET.getType(), clientSecret);
        }
    }

    /**
     * Returns the response_types value as a {@code Set} object. If the response_types value is
     * {@code null}, this method returns {@code null}.
     *
     * @return the Set value.
     */
    public Set<String> getResponseTypes() {
        if (get(OAuth2Constants.ShortClientAttributeNames.RESPONSE_TYPES.getType()).asList(String.class) != null) {
            return new HashSet<String>(get(OAuth2Constants.ShortClientAttributeNames.RESPONSE_TYPES.getType()).asList(String.class));
        } else {
            return null;
        }
    }

    /**
     * Sets the response_types of the OAuth2Client
     *
     * @param responseTypes
     *            the response_types to set.
     */
    public void setResponseTypes(List<String> responseTypes) {
        if (responseTypes != null && !responseTypes.isEmpty()) {
            put(OAuth2Constants.ShortClientAttributeNames.RESPONSE_TYPES.getType(), responseTypes);
        }
    }
}
