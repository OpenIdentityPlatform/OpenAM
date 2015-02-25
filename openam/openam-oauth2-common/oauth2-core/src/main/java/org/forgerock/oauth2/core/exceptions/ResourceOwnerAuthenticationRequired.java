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
 * Thrown when the resource owner needs to be authenticated before the authorization can be granted to a OAuth2 client.
 *
 * @since 12.0.0
 */
public class ResourceOwnerAuthenticationRequired extends OAuth2Exception {

    private final URI redirectUri;

    /**
     * Constructs a new ResourceOwnerAuthenticationRequired instance with the specified redirect uri.
     *
     * @param redirectUri The redirect uri of the login page for the user agent to be redirected to.
     */
    public ResourceOwnerAuthenticationRequired(final URI redirectUri) {
        super(307, "redirection_temporary", "The request requires a redirect.");
        this.redirectUri = redirectUri;
    }

    /**
     * Gets the redirect uri of the login page.
     *
     * @return The redirct uri.
     */
    public URI getRedirectUri() {
        return redirectUri;
    }
}
