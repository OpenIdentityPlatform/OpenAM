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
package org.forgerock.openam.services.push;

import org.forgerock.openam.rest.AbstractRestRouteProvider;
import org.forgerock.openam.rest.ResourceRouter;
import org.forgerock.openam.rest.RestRouteProvider;
import org.forgerock.openam.services.push.sns.SnsMessageResource;

import javax.inject.Inject;

import static org.forgerock.http.routing.RoutingMode.EQUALS;
import static org.forgerock.json.resource.Resources.newAnnotatedRequestHandler;
import static org.forgerock.openam.rest.Routers.none;

/**
 * A {@link RestRouteProvider} that add routes for the audit endpoint.
 *
 * @since 13.5.0
 */
public class PushNotificationRestRouteProvider extends AbstractRestRouteProvider {

    private final static String ROUTE = "push/sns/message";

    private SnsMessageResource snsMessageResource;

    /**
     * Inject the SNS resource.
     * @param snsMessageResource The SNS resource.
     */
    @Inject
    public void setMessageResource(SnsMessageResource snsMessageResource) {
        this.snsMessageResource = snsMessageResource;
    }

    @Override
    public void addResourceRoutes(ResourceRouter rootRouter, ResourceRouter realmRouter) {
        realmRouter
                .route(ROUTE)
                .authenticateWith(none())
                .forVersion(1)
                .toRequestHandler(EQUALS,
                        newAnnotatedRequestHandler(snsMessageResource));
    }
}
