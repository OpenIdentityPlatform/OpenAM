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

package org.forgerock.openam.sts.rest.service;

import java.util.Collections;
import java.util.Set;

import static org.forgerock.http.routing.RoutingMode.*;

import javax.inject.Provider;

import org.forgerock.http.Handler;
import org.forgerock.json.resource.http.CrestHttp;
import org.forgerock.openam.http.HttpRoute;
import org.forgerock.openam.http.HttpRouteProvider;
import org.forgerock.util.Function;
import org.forgerock.util.promise.NeverThrowsException;

/**
 * {@link HttpRouteProvider} for STS service REST routes.
 *
 * @since 13.0.0
 */
public class RestSTSServiceHttpRouteProvider implements HttpRouteProvider {

    @Override
    public Set<HttpRoute> get() {
        return Collections.singleton(HttpRoute.newHttpRoute(STARTS_WITH, "rest-sts", new Provider<Handler>() {
            @Override
            public Handler get() {
                return CrestHttp.newHttpHandler(
                        RestSTSServiceConnectionFactoryProvider.getConnectionFactory(),
                        RestSTSServiceHttpServletContextFactoryProvider.getHttpServletContextFactory());
            }
        }));
    }
}
