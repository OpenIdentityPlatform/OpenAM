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
 * Copyright 2014-2016 ForgeRock AS.
 */
package org.forgerock.openam.oauth2;

import org.forgerock.oauth2.core.OAuth2Request;

/**
 * Interface for checking whether an OAuth2 token is stateless or not
 *
 * @since 13.5.0
 */
public interface StatelessCheck<T> {

    /**
     * Interrogates token to ascertain the OAuth2 token is stateless or not
     *
     * @param tokenId The token id
     * @return The result of the check
     */
    T byToken(String tokenId);

    /**
     * Checks the OAuth2 token is stateless or not based on the realm
     *
     * @param realm The realm
     * @return The result of the check
     */
    T byRealm(String realm);

    /**
     *
     * Checks the OAuth2 token is stateless or not based on the request
     *
     * @param request The OAuth2 request
     * @return The result of the check
     */
    T byRequest(OAuth2Request request);
}
