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

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Interface for providing the OAuth2 Provider implementation a chance to extract implementation specific information
 * from the OAuth2 HttpServletRequest.
 *
 * @since 12.0.0
 */
public interface ContextHandler {

    /**
     * Creates the context {@code Map<String, Object>} containing the implementation specific information from the
     * OAuth2 HttpServetRequest.
     *
     * @param request The HttpServletRequest of the OAuth2 request.
     * @return A {@code Map<String, Object>} containing OAuth2 Provider implementation specific context information.
     */
    Map<String, Object> createContext(final HttpServletRequest request);
}
