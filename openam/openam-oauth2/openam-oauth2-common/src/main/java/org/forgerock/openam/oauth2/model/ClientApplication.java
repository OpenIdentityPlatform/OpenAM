/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock Inc. All rights reserved.
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
 * "Portions Copyrighted [2012] [ForgeRock Inc]"
 */
package org.forgerock.openam.oauth2.model;

import java.net.URI;
import java.util.Set;


/**
 * Implements the interface that needs to be implemented to read the client settings
 */
public interface ClientApplication {

    public enum ClientType {
        CONFIDENTIAL, PUBLIC;
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

}
