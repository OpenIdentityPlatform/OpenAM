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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openidconnect;

import java.util.List;

/**
 * Builds a OAuth2 Client.
 *
 * @since 11.0.0
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
    private List<String> contacts;

    /**
     * Sets the client id of the OAuth2Client.
     *
     * @param clientID The client id.
     */
    public ClientBuilder setClientID(String clientID) {
        this.clientID = clientID;
        return this;
    }

    /**
     * Sets the client type of the OAuth2Client.
     *
     * @param clientType The client type.
     */
    public ClientBuilder setClientType(String clientType) {
        this.clientType = clientType;
        return this;
    }

    /**
     * Sets the redirection uris of the OAuth2Client.
     *
     * @param redirectionURIs The redirection uris.
     */
    public ClientBuilder setRedirectionURIs(List<String> redirectionURIs) {
        this.redirectionURIs = redirectionURIs;
        return this;
    }

    /**
     * Sets the allowed scopes of the OAuth2Client.
     *
     * @param allowedGrantScopes The allowed scopes.
     */
    public ClientBuilder setAllowedGrantScopes(List<String> allowedGrantScopes) {
        this.allowedGrantScopes = allowedGrantScopes;
        return this;
    }

    /**
     * Sets the default scopes of the OAuth2Client.
     *
     * @param defaultGrantScopes The default scopes.
     */
    public ClientBuilder setDefaultGrantScopes(List<String> defaultGrantScopes) {
        this.defaultGrantScopes = defaultGrantScopes;
        return this;
    }

    /**
     * Sets the display name of the OAuth2Client.
     *
     * @param displayName The display name.
     */
    public ClientBuilder setDisplayName(List<String> displayName) {
        this.displayName = displayName;
        return this;
    }

    /**
     * Sets the client description of the OAuth2Client.
     *
     * @param displayDescription The client description.
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
     * Sets the subject type of the OAuth2Client.
     *
     * @param subjectType The subject type.
     */
    public ClientBuilder setSubjectType(String subjectType) {
        this.subjectType = subjectType;
        return this;
    }

    /**
     * Sets the id token signed response alg of the OAuth2Client.
     *
     * @param idTokenSignedResponseAlgorithm The id token signed response alg.
     */
    public ClientBuilder setIdTokenSignedResponseAlgorithm(String idTokenSignedResponseAlgorithm) {
        this.idTokenSignedResponseAlgorithm = idTokenSignedResponseAlgorithm;
        return this;
    }

    /**
     * Sets the post logout redirection uri of the OAuth2Client.
     *
     * @param postLogoutRedirectionURI The post logout redirection uri.
     */
    public ClientBuilder setPostLogoutRedirectionURI(String postLogoutRedirectionURI) {
        this.postLogoutRedirectionURI = postLogoutRedirectionURI;
        return this;
    }

    /**
     * Sets the registration access token of the OAuth2Client.
     *
     * @param accessToken The registration access token.
     */
    public ClientBuilder setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        return this;
    }

    /**
     * Sets the client session uri of the OAuth2Client.
     *
     * @param clientSessionURI The client session uri.
     */
    public ClientBuilder setClientSessionURI(String clientSessionURI) {
        this.clientSessionURI = clientSessionURI;
        return this;
    }

    /**
     *
     * @param contacts The contact information for the clients
     * @return
     */
    public ClientBuilder setContacts(List<String> contacts) {
        this.contacts = contacts;
        return this;
    }

    /**
     * Sets the application type of the OAuth2Client.
     *
     * @param applicationType The application type.
     */
    public ClientBuilder setApplicationType(String applicationType) {
        this.applicationType = applicationType;
        return this;
    }

    /**
     * Sets the client secret of the OAuth2Client.
     *
     * @param clientSecret The client's secret.
     */
    public ClientBuilder setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
        return this;
    }

    /**
     * Sets the response types of the OAuth2Client.
     *
     * @param responseTypes The response types.
     */
    public ClientBuilder setResponseTypes(List<String> responseTypes) {
        this.responseTypes = responseTypes;
        return this;
    }

    /**
     * Creates the OAuth2 Client.
     *
     * @return The OAuth2 Client.
     */
    public Client createClient() {
        return new Client(clientID, clientType, redirectionURIs, allowedGrantScopes, defaultGrantScopes, displayName,
                displayDescription, clientName, subjectType, idTokenSignedResponseAlgorithm, postLogoutRedirectionURI,
                accessToken, clientSessionURI,applicationType, clientSecret, responseTypes, contacts);
    }
}