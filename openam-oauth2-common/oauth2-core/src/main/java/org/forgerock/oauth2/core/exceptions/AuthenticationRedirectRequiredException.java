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

package org.forgerock.oauth2.core.exceptions;

import java.net.URI;

/**
 * An exception that is thrown during the processing of a OAuth2 request when the request requires the resource owner
 * to be redirected to be authenticated before processing can be allowed to proceed.
 *
 * @since 12.0.0
 */
public class AuthenticationRedirectRequiredException extends OAuth2Exception {

    private final URI redirectUri;

    /**
     * Constructs a new exception with the specified redirect URI.
     *
     * @param redirectUri The redirect URI.
     */
    public AuthenticationRedirectRequiredException(final URI redirectUri) {
        this.redirectUri = redirectUri;
    }

    /**
     * Returns the redirect URI.
     *
     * @return The redirect URI.
     */
    public URI getRedirectUri() {
        return redirectUri;
    }
}
