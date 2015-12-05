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
import org.forgerock.openam.radius.server.config.RadiusServerConstants;

import com.sun.identity.shared.debug.Debug;

/**
 * Event submitted to the event bus when a radius request is accepted and an Access-Challenge message has been returned
 * to the client.
 */
public class AuthRequestChallengedEvent extends AcceptedRadiusEvent {

    private static final Debug LOG = Debug.getInstance(RadiusServerConstants.RADIUS_SERVER_LOGGER);

    /**
     * Constructor.
     *
     * @param request the request associated with the event
     * @param response the response to the request (if available), null if not.
     * @param context the context in which the request was received.
     */
    public AuthRequestChallengedEvent(RadiusRequest request, RadiusResponse response, RadiusRequestContext context) {
        super(request, response, context);
        LOG.message("Constructed AuthRequestChallengedEvent.AuthRequestChallengedEvent()");

    }
}
