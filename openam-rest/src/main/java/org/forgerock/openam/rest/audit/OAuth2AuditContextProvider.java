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
* Copyright 2015 ForgeRock AS.
*/
package org.forgerock.openam.rest.audit;

import org.forgerock.openam.audit.AuditConstants.TrackingIdKey;
import org.restlet.Request;

/**
 * A provider which provides user id and tracking details for auditing purposes. Providers will draw these details
 * from different sources, such as OAuth2 access tokens and SSO session tokens.
 *
 * @since 13.0.0
 */
public interface OAuth2AuditContextProvider {

    /**
     * Get the user id for auditing purposes. The user id will come from a form of identification which may or may not
     * be included in the request.
     *
     * @param request The request.
     * @return The user id, if it is available.
     */
    String getUserId(Request request);

    /**
     * Get the context for auditing purposes. The context will come from a form of identification which may or may not
     * be included in the request.
     *
     * @param request The request.
     * @return The user id, if it is available.
     */
    String getTrackingId(Request request);

    /**
     * Get the {@link TrackingIdKey} key identifying the specific type of context being looked for by the provider.
     *
     * @return The key.
     */
    TrackingIdKey getTrackingIdKey();
}
