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

package org.forgerock.oauth2.core;

import java.util.Set;

/**
 * Represents an OAuth2 Authorization Code.
 *
 * @since 12.0.0
 */
public class AuthorizationCode {

    private final String code;
    private final CoreToken token;

    /**
     * Constructs a new AuthorizationCode.
     *
     * @param code The authorization code.
     * @param token The backing CoreToken.
     */
    public AuthorizationCode(final String code, final CoreToken token) {
        this.code = code;
        this.token = token;
    }

    /**
     * The authorization code.
     *
     * @return The authorization code.
     */
    public String getCode() {
        return code;
    }

    /**
     * The resource owner's identifier.
     *
     * @return The resource owner's identifier.
     */
    public String getResourceOwnerId() {
        return token.getUserID();
    }

    /**
     * The client's identifier.
     *
     * @return The client's identifier.
     */
    public String getClientId() {
        return token.getClientID();
    }

    /**
     * The validated scope.
     *
     * @return The validated scope.
     */
    public Set<String> getScope() {
        return token.getScope();
    }

    /**
     * The redirect URI.
     *
     * @return The redirect URI.
     */
    public String getRedirectUri() {
        return token.getRedirectURI();
    }

    /**
     * The nonce.
     *
     * @return The nonce.
     */
    public String getNonce() {
        return token.getNonce();
    }

    /**
     * Sets the authorization code as have being issued, i.e. swapped for an AccessToken.
     */
    public void setIssued() {
        token.setIssued();
    }

    /**
     * Whether the authorization code has been issued, i.e. swapped for an AccessToken.
     *
     * @return {@code true} if the authorization code has been issued.
     */
    public boolean isIssued() {
        return token.isIssued();
    }

    /**
     * Whether the authorization code has expired.
     *
     * @return The authorization code has expired.
     */
    public boolean isExpired() {
        return token.isExpired();
    }

    /**
     * The backing CoreToken.
     *
     * @return The CoreToken.
     */
    public CoreToken getToken() {
        return token;
    }
}
