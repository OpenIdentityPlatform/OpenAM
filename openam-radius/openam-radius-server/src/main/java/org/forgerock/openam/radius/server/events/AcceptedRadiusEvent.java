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
package org.forgerock.openam.radius.server.events;

import org.forgerock.openam.radius.server.RadiusRequest;
import org.forgerock.openam.radius.server.RadiusRequestContext;
import org.forgerock.openam.radius.server.RadiusResponse;

/**
 * Event submitted to the event bus when a radius request is accepted and the request packet has been interpreted and
 * context, user etc are available.
 */
public class AcceptedRadiusEvent extends RadiusEvent {

    /**
     * the request associated with the event.
     */
    private final RadiusRequest request;

    /**
     * the response to the request (if available).
     */
    private final RadiusResponse response;

    /**
     * the context in which the request was received.
     */
    private final RadiusRequestContext requestContext;

    /**
     * Constructor.
     *
     * @param request the request associated with the event
     * @param response the response to the request (if available), null if not.
     * @param context the context in which the request was received.
     */
    public AcceptedRadiusEvent(RadiusRequest request, RadiusResponse response, RadiusRequestContext context) {
        this.request = request;
        this.response = response;
        this.requestContext = context;
    }

    /**
     * Get the request id of the access request.
     *
     * @return the request id of the access request.
     */
    public String getRequestId() {
        return this.request.getRequestId();
    }

    /**
     * Get the name of the authenticating entity.
     *
     * @return the name of the authenticating entity.
     */
    public String getUsername() {
        return this.request.getUsername();
    }

    /**
     * Get the universalId of the authenticating entity.
     *
     * @return the universalId of the authenticating entity.
     */
    public String getUniversalId() {
        return this.response.getUniversalId();
    }

    /**
     * Get the realm that the access request will be authenticated against.
     *
     * @return the realm that the access request will be authenticated against.
     */
    public String getRealm() {
        return response.getRealm();
    }

    /**
     * Get the request context in which the access request was made.
     *
     * @return the request context in which the access request was made.
     */
    public RadiusRequestContext getRequestContext() {
        return requestContext;
    }

    /**
     * Return the response to the request, or null if none has been sent.
     *
     * @return the response to the request, or null if none has been sent.
     */
    public RadiusResponse getResponse() {
        return this.response;
    }

    /**
     * Return the request.
     *
     * @return the request.
     */
    public RadiusRequest getRequest() {
        return this.request;
    }
}
