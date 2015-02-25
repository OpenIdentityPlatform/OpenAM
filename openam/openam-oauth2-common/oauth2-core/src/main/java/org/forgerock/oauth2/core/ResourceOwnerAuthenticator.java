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

/**
 * Authenticates a resource owner from the credentials provided on the request.
 *
 * @since 12.0.0
 */
public interface ResourceOwnerAuthenticator {

    /**
     * Authenticates a resource owner by extracting the resource owner's credentials from the request and authenticating
     * against the OAuth2 provider's internal user store.
     *
     * @param request The OAuth2 request.
     * @return The authenticated ResourceOwner, or {@code null} if authentication failed.
     */
    ResourceOwner authenticate(OAuth2Request request);
}
