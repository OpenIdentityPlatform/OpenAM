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

package org.forgerock.openidconnect;

import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.forgerock.json.fluent.JsonValue;

/**
 * Service for retrieving user's information from the access token the user granted the authorization.
 *
 * @since 12.0.0
 */
public interface UserInfoService {

    /**
     * Gets the user's information for the specified access token.
     *
     * @param request The OAuth2 request.
     * @return A JsonValue of the user's information.
     * @throws OAuth2Exception If there is any issue in getting the user information.
     */
    JsonValue getUserInfo(OAuth2Request request) throws OAuth2Exception;
}
