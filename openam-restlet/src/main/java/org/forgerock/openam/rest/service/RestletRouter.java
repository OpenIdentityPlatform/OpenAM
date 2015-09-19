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

package org.forgerock.openam.rest.service;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.services.context.Context;
import org.forgerock.services.routing.AbstractRouter;
import org.forgerock.services.routing.IncomparableRouteMatchException;
import org.forgerock.util.Pair;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;

final class RestletRouter extends AbstractRouter<RestletRouter, Request, Restlet> {

    private final static Debug DEBUG = Debug.getInstance("frRest");

    @Override
    protected RestletRouter getThis() {
        return this;
    }

    void handle(Context context, Request request, Response response) {
        try {
            Pair<Context, Restlet> bestMatch = getBestRoute(context, request);
            if (bestMatch != null) {
                bestMatch.getSecond().handle(request, response);
            } else {
                response.setStatus(org.restlet.data.Status.CLIENT_ERROR_NOT_FOUND);
            }
        } catch (IncomparableRouteMatchException e) {
            DEBUG.message(String.format("Route for '%s' not found", request.getResourceRef().getPath()));
            response.setStatus(org.restlet.data.Status.CLIENT_ERROR_NOT_FOUND);
        }
    }
}
