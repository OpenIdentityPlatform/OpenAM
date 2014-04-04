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

import org.forgerock.oauth2.core.exceptions.OAuth2Exception;

/**
 * Implementations of this interface will implement the logic for a specific OAuth2 Grant Type, to gain an Access Token.
 *
 * @since 12.0.0
 */
public interface GrantTypeHandler {

    /**
     * Handles the OAuth2 request for the implemented grant type.
     *
     * @param accessTokenRequest The AccessTokenRequest instance.
     * @return An AccessToken.
     * @throws OAuth2Exception If a problem occurs when processing the OAuth2 request.
     */
    AccessToken handle(final AccessTokenRequest accessTokenRequest) throws OAuth2Exception;
}
