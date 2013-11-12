/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions copyright [year] [name of copyright owner]"
 */
package org.forgerock.openam.oauth2.openid;

import javax.servlet.http.HttpServletRequest;

/**
 * Interface is to define what needs to be implemented to do the OpenID Connect CheckSession endpoint
 * @supported.all.api
 */
public interface CheckSession {
    /**
     * Get the cookie name containing the session information
     * @return the cookie name
     */
    public String getCookieName();

    /**
     * Get the URL the postMessage must be coming from (registered in client) to process the message.
     * @param request
     * @return blank string if error, else the url as a string
     */
    public String getClientSessionURI(HttpServletRequest request);

    /**
     * Check if the JWT contains a valid session id.
     * @param request
     * @return true if valid; false otherwise
     * @
     */
    public boolean getValidSession(HttpServletRequest request);
}
