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
 * Copyright 2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package com.iplanet.sso;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Extension interface for dynamically loaded SSOProvider plugins.
 *
 * @since 14.0.0
 */
public interface SSOProviderPlugin extends SSOProvider {

    /**
     * Determines whether this SSOProvider is applicable to the given servlet request.
     *
     * @param request the request to check.
     * @return {@code true} if the request contains an SSOToken that can be handled by this provider.
     */
    boolean isApplicable(HttpServletRequest request);

    /**
     * Determines whether this SSOProvider is applicable to the given token id.
     *
     * @param tokenId the token id.
     * @return {@code true} if the given token id can be parsed by this provider.
     */
    boolean isApplicable(String tokenId);
}
