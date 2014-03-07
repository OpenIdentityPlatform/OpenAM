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

import java.util.List;

/**
 * Builds a Client
 */
public class ClientBuilder {
    private String clientID;
    private String clientType;
    private List<String> redirectionURIs;
    private List<String> allowedGrantScopes;
    private List<String> defaultGrantScopes;
    private List<String> displayName;
    private List<String> displayDescription;
    private String clientName;
    private String subjectType;
    private String idTokenSignedResponseAlgorithm;
    private String postLogoutRedirectionURI;
    private String accessToken;
    private String clientSessionURI;
    private String applicationType;
    private String clientSecret;
    private List<String> responseTypes;

    /**
     * Sets the client_id of the OAuth2Client
     *
     * @param clientID
     *            the client_id to set.
     */
    public ClientBuilder setClientID(String clientID) {
        this.clientID = clientID;
        return this;
    }

    /**
     * Sets the client_type of the OAuth2Client
     *
     * @param clientType
     *            the client_type to set.
     */
    public ClientBuilder setClientType(String clientType) {
        this.clientType = clientType;
        return this;
    }

    /**
     * Sets the redirection_uris of the OAuth2Client
     *
     * @param redirectionURIs
     *            the redirection_uris to set.
     */
    public ClientBuilder setRedirectionURIs(List<String> redirectionURIs) {
        this.redirectionURIs = redirectionURIs;
        return this;
    }

    /**
     * Sets the scopes of the OAuth2Client
     *
     * @param allowedGrantScopes
     *            the scopes to set.
     */
    public ClientBuilder setAllowedGrantScopes(List<String> allowedGrantScopes) {
        this.allowedGrantScopes = allowedGrantScopes;
        return this;
    }

    /**
     * Sets the default_scopes of the OAuth2Client
     *
     * @param defaultGrantScopes
     *            the default_scopes to set.
     */
    public ClientBuilder setDefaultGrantScopes(List<String> defaultGrantScopes) {
        this.defaultGrantScopes = defaultGrantScopes;
        return this;
    }

    /**
     * Sets the display_name of the OAuth2Client
     *
     * @param displayName
     *            the display_name to set.
     */
    public ClientBuilder setDisplayName(List<String> displayName) {
        this.displayName = displayName;
        return this;
    }

    /**
     * Sets the client_description of the OAuth2Client
     *
     * @param displayDescription
     *            the client_description to set.
     */
    public ClientBuilder setDisplayDescription(List<String> displayDescription) {
        this.displayDescription = displayDescription;
        return this;
    }

    /**
     * Sets the client_name of the OAuth2Client
     *
     * @param clientName
     *            the client_name to set.
     */
    public ClientBuilder setClientName(String clientName) {
        this.clientName = clientName;
        return this;
    }

    /**
     * Sets the subject_type of the OAuth2Client
     *
     * @param subjectType
     *            the subject_type to set.
     */
    public ClientBuilder setSubjectType(String subjectType) {
        this.subjectType = subjectType;
        return this;
    }

    /**
     * Sets the id_token_signed_response_alg of the OAuth2Client
     *
     * @param idTokenSignedResponseAlgorithm
     *            the id_token_signed_response_alg to set.
     */
    public ClientBuilder setIdTokenSignedResponseAlgorithm(String idTokenSignedResponseAlgorithm) {
        this.idTokenSignedResponseAlgorithm = idTokenSignedResponseAlgorithm;
        return this;
    }

    /**
     * Sets the post_logout_redirection_uri of the OAuth2Client
     *
     * @param postLogoutRedirectionURI
     *            the post_logout_redirection_uri to set.
     */
    public ClientBuilder setPostLogoutRedirectionURI(String postLogoutRedirectionURI) {
        this.postLogoutRedirectionURI = postLogoutRedirectionURI;
        return this;
    }

    /**
     * Sets the registration_access_token of the OAuth2Client
     *
     * @param accessToken
     *            the registration_access_token to set.
     */
    public ClientBuilder setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        return this;
    }

    /**
     * Sets the client_session_uri of the OAuth2Client
     *
     * @param clientSessionURI
     *            the client_session_uri to set.
     */
    public ClientBuilder setClientSessionURI(String clientSessionURI) {
        this.clientSessionURI = clientSessionURI;
        return this;
    }

    /**
     * Sets the application_type of the OAuth2Client
     *
     * @param applicationType
     *            the application_type to set.
     */
    public ClientBuilder setApplicationType(String applicationType) {
        this.applicationType = applicationType;
        return this;
    }

    /**
     * Sets the client_secret of the OAuth2Client
     *
     * @param clientSecret
     *            the client_secret to set.
     */
    public ClientBuilder setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
        return this;
    }

    /**
     * Sets the response_types of the OAuth2Client
     *
     * @param responseTypes
     *            the response_types to set.
     */
    public ClientBuilder setResponseTypes(List<String> responseTypes) {
        this.responseTypes = responseTypes;
        return this;
    }

    /**
     * Creates the OAuth2 Client
     */
    public Client createClient() {
        return new Client(clientID,
                clientType,
                redirectionURIs,
                allowedGrantScopes,
                defaultGrantScopes,
                displayName,
                displayDescription,
                clientName,
                subjectType,
                idTokenSignedResponseAlgorithm,
                postLogoutRedirectionURI,
                accessToken,
                clientSessionURI,
                applicationType,
                clientSecret,
                responseTypes);
    }
}