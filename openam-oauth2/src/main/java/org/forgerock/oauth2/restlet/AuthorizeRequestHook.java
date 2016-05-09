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

package org.forgerock.oauth2.restlet;

import org.forgerock.oauth2.core.OAuth2Request;
import org.restlet.Request;
import org.restlet.Response;

/**
 * By implementing this interface the implementation can make changes to the
 * restlet response.
 */
public interface AuthorizeRequestHook {

    /**
     * Called before the authorize request is actually handled
     * @param o2request The current OAuth2 request.
     * @param request The restlet request.
     * @param response The restlet response.
     */
    void beforeAuthorizeHandling(OAuth2Request o2request, Request request, Response response);

    /**
     * Called after the authorize request has been completed and an authorization code is being returned.
     * @param o2request The current OAuth2 request.
     * @param request The restlet request.
     * @param response The restlet response.
     */
    void afterAuthorizeSuccess(OAuth2Request o2request, Request request, Response response);

}
