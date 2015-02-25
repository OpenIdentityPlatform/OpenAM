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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.openidconnect;

import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;

import javax.servlet.http.HttpServletRequest;

/**
 * Interface is to define what needs to be implemented to do the OpenID Connect check session endpoint.
 *
 * @since 12.0.0
 * @supported.all.api
 */
public interface CheckSession {

    /**
     * Get the cookie name containing the session information.
     *
     * @return The cookie name.
     */
    public String getCookieName();

    /**
     * Get the URL the postMessage must be coming from (registered in client) to process the message.
     *
     * @param request The HttpServletRequest.
     * @return The url as a string or empty String.
     */
    public String getClientSessionURI(HttpServletRequest request) throws UnauthorizedClientException,
            InvalidClientException;

    /**
     * Check if the JWT contains a valid session id.
     *
     * @param request The HttpServletRequset.
     * @return {@code true} if valid.
     */
    public boolean getValidSession(HttpServletRequest request);
}
