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
package org.forgerock.openam.oauth2.model;

import java.net.URI;
import java.util.Set;


/**
 * Implements the interface that needs to be implemented to read the client settings
 *
 * @supported.all.api
 */
public interface ClientApplication {

    public enum ClientType {
        CONFIDENTIAL, PUBLIC;
    }

    public enum SubjectType {
        PAIRWISE, PUBLIC;
    }

    /**
     * Gets the client id
     * @return a string representing the client id
     * @see <a href="http://tools.ietf.org/html/rfc6749#section-2.2">2.2.  Client Identifier</a>
     */
    public String getClientId();

    /**
     * Gets the client type
     * @return a {@link ClientType} either confidential or public
     * @see <a href="http://tools.ietf.org/html/rfc6749#section-2.1">2.1.  Client Types</a>
     */
    public ClientType getClientType();

    /**
     * The authorization server SHOULD require all clients to register their
     * redirection endpoint prior to utilizing the authorization endpoint
     * <p/>
     * The authorization server SHOULD require the client to provide the
     * complete redirection URI (the client MAY use the "state" request
     * parameter to achieve per-request customization). If requiring the
     * registration of the complete redirection URI is not possible, the
     * authorization server SHOULD require the registration of the URI scheme,
     * authority, and path (allowing the client to dynamically vary only the
     * query component of the redirection URI when requesting authorization).
     * <p/>
     * The authorization server MAY allow the client to register multiple
     * redirection endpoints.
     * 
     * @return
     * @see <a
     *      href="http://tools.ietf.org/html/draft-ietf-oauth-v2-24#section-3.1.2.3">3.1.2.3.
     *      Dynamic Configuration</a>
     */
    public Set<URI> getRedirectionURIs();

    /**
     * Gets the access token type the client expects to recieve
     * @return a string representing the access token type
     * @see <a href="http://tools.ietf.org/html/rfc6749#section-7.1">7.1.  Access Token Types</a>
     */
    public String getAccessTokenType();

    /**
     * Gets the authenticaiton schemes the
     * @return a {@link ClientType} either confidential or public
     * @see <a href="http://tools.ietf.org/html/rfc6749#section-7.1">7.1.  Access Token Types</a>
     */
    public String getClientAuthenticationSchema();

    /**
     * Gets the Scopes registered for a client
     * @return a set of strings representing the Scopes of the client
     * @see <a href="http://tools.ietf.org/html/rfc6749#section-3.3">3.3.  Access Token Scope</a>
     */
    public Set<String> getAllowedGrantScopes();

    /**
     * Gets the default Scopes assigned to tokens for this client
     * 
     * @return a set of strings representing the default scopes for this client
     * @see <a href="http://tools.ietf.org/html/rfc6749#section-3.3">3.3.  Access Token Scope</a>
     */
    public Set<String> getDefaultGrantScopes();

    /**
     * Get the auto_grant property of the client
     * <p/>
     * If "auto_grant" is true then the server does not require the Resource
     * Owner's approval unless the request has the
     * {@link com.sun.identity.shared.OAuth2Constants.Custom#APPROVAL_PROMPT}
     * property and the value is null.
     * <p/>
     * This function is not part of the OAuth2 specification
     * 
     * @return
     */
    public boolean isAutoGrant();

    /**
     * Contains a set of strings that are in the format of "locale"|"displayName"
     * @return
     *          a set of display names for various locales
     */
    public Set<String> getDisplayName();

    /**
     * Contains a set of strings that are in the format of "locale"|"displayDescription"
     * @return
     *          a set of display descriptions for various locales
     */
    public Set<String> getDisplayDescription();

    /**
     * Gets the Response Types this client will use
     * @return
     */
    public Set<String> getResponseTypes();

    /**
     * Gets the Grant Types this client will use
     * @return
     */
    public Set<String> getGrantTypes();

    /**
     * Gets the list of people allowed to edit this client
     * @return
     */
    public String getContacts();

    /**
     * Gets the readable name of the client
     * @return
     */
    public String getClientName();

    /**
     * Gets the URI that has the clients logo
     * @return
     */
    public String getLogoURI();

    /**
     * Gets the method of authentication for the token endpoint
     * @return
     */
    public String getTokenEndpointAuthMethod();

    /**
     * Gets the URI of the clients policy
     * @return
     */
    public String getPolicyURI();

    /**
     * Gets the URI of the Clients Terms of Service
     * @return
     */
    public String getTosURI();

    /**
     * Gets the URI that contains the clients Json Web Keys
     * @return
     */
    public String getJwksURI();

    /**
     * Gets the clients Sector Identifier URI
     * @return
     */
    public String getSectorIdentifierURI();

    /**
     * Gets the clients subject type
     * @return
     */
    public SubjectType getSubjectType();

    /**
     * Gets the signing algorithm used to sign the request object
     * @return
     */
    public String getRequestObjectSigningAlgorithm();

    /**
     * Gets the algorithm to use to sign the userinfo endpoint response
     * @return
     */
    public String getUserInfoSignedResponseAlgorithm();

    /**
     * Gets the algorithm to use to encrypt the userinfo endpoint response
     * @return
     */
    public String getUserInfoEncryptedResposneAlgorithm();

    /**
     * Gets the algorithm to use to ecrypt and sign the response from the userinfo endpoint
     * @return
     */
    public String getUserInfoEncryptedResponseEncoding();

    /**
     * Gets the algorithm to use to sign the IDToken response
     * @return
     */
    public String getIDTokenSignedResponseAlgorithm();

    /**
     * Gets the algorithm to use to encrypt the IDToken response
     * @return
     */
    public String getIDTokenEncryptedResposneAlgorithm();

    /**
     * Gets the algorithm to use to ecrypt and sign the response for the IDToken
     * @return
     */
    public String getIDTokenEncryptedResponseEncoding();

    /**
     * Gets the default time a client can be authenticated for before they must re-authenticate
     * @return
     */
    public String getDefaultMaxAge();

    /**
     * Gets whether or not the Auth Time attribute is required in the IDToken
     * @return
     */
    public String getRequireAuthTime();

    /**
     * gets the default ACR Values for the client
     * @return
     */
    public String getDefaultACRValues();

    /**
     * The URI for the client to initiate login
     * @return
     */
    public String getinitiateLoginURI();

    /**
     * Gets the URI to redirect to when the client logs out
     * @return
     */
    public String getPostLogoutRedirectionURI();

    /**
     * Gets the client URI that has a JSON array of valid Request URIS.
     * @return
     */
    public String getRequestURIS();

    /**
     * Gets the client URI that has a JSON array of valid Request URIS.
     * @return
     */
    public String getAccessToken();

    /**
     * Gets the uri for the rp used in session management.
     * @return
     */
    public String getClientSessionURI();

}
