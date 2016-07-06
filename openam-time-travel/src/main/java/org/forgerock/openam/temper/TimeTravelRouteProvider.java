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
 */

package org.forgerock.openam.temper;

import static java.lang.String.valueOf;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openam.temper.TimeTravelTimeService.getOffset;
import static org.forgerock.openam.temper.TimeTravelTimeService.setOffset;

import org.forgerock.api.annotations.Action;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.RequestHandler;
import org.forgerock.api.annotations.Schema;
import org.forgerock.api.annotations.Update;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.rest.ResourceRouter;
import org.forgerock.openam.rest.RestRouteProvider;
import org.forgerock.openam.rest.ServiceRouter;
import org.forgerock.openam.rest.authz.AdminOnlyAuthzModule;
import org.forgerock.util.promise.Promise;

/**
 * CREST service provider for the Time Travel API.
 */
public class TimeTravelRouteProvider implements RestRouteProvider {

    @Override
    public void addRoutes(ResourceRouter rootResourceRouter, ResourceRouter realmResourceRouter,
            ResourceRouter internalRouter, ServiceRouter rootServiceRouter, ServiceRouter realmServiceRouter) {

        rootResourceRouter.route("timetravel")
                .authorizeWith(AdminOnlyAuthzModule.class)
                .toAnnotatedSingleton(TimeTravelResourceProvider.class);
    }

    /**
     * The actual resource provider.
     */
    @RequestHandler(@Handler(mvccSupported = false, resourceSchema = @Schema(fromType = String.class)))
    public static class TimeTravelResourceProvider {
        /**
         * The {@code fastforward} action allows the time to be moved forward by a number of milliseconds.
         *
         * @param request The request.
         * @return An empty JSON response.
         */
        @Action(operationDescription = @Operation)
        public Promise<ActionResponse, ResourceException> fastforward(ActionRequest request) {
            setOffset(getOffset() + request.getContent().get("amount").asLong());
            return newActionResponse(json(object())).asPromise();
        }

        /**
         * The update method allows the time offset from system time to set explicitly.
         *
         * @param request The request.
         * @return The offset.
         */
        @Update(operationDescription = @Operation)
        public Promise<ResourceResponse, ResourceException> update(UpdateRequest request) {
            Long offset = request.getContent().get("offset").asLong();
            setOffset(offset);
            String offsetString = valueOf(offset);
            return newResourceResponse(offsetString, offsetString, json(object(field("offset", offset)))).asPromise();
        }
    }

}
