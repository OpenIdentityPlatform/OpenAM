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

package org.forgerock.openam.sts.publish.service;

import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;
import static org.forgerock.json.resource.http.CrestHttp.newHttpHandler;
import static org.forgerock.openam.audit.AuditConstants.Component.STS;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.util.Collections;
import java.util.Set;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.forgerock.http.Handler;
import org.forgerock.http.handler.Handlers;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.openam.http.HttpRoute;
import org.forgerock.openam.http.HttpRouteProvider;
import org.forgerock.openam.rest.ResourceRouter;
import org.forgerock.openam.rest.authz.STSPublishServiceAuthzModule;
import org.forgerock.openam.rest.router.RestRealmValidator;
import org.forgerock.openam.sts.InstanceConfigMarshaller;
import org.forgerock.openam.sts.publish.config.STSPublishInjectorHolder;
import org.forgerock.openam.sts.publish.rest.RestSTSInstancePublisher;
import org.forgerock.openam.sts.publish.soap.SoapSTSInstancePublisher;
import org.forgerock.openam.sts.rest.config.user.RestSTSInstanceConfig;
import org.forgerock.openam.sts.soap.config.user.SoapSTSInstanceConfig;
import org.slf4j.Logger;

/**
 * {@link HttpRouteProvider} for STS publish REST routes.
 *
 * @since 13.0.0
 */
public class STSPublishServiceHttpRouteProvider implements HttpRouteProvider {

    private ResourceRouter rootRouter;
    private org.forgerock.http.Filter authenticationFilter;

    @Inject
    public void setRouters(ResourceRouter router) {
        this.rootRouter = router;
    }

    @Inject
    public void setAuthenticationFilter(@Named("AuthenticationFilter") org.forgerock.http.Filter authenticationFilter) {
        this.authenticationFilter = authenticationFilter;
    }

    @Override
    public Set<HttpRoute> get() {
        return Collections.singleton(HttpRoute.newHttpRoute(STARTS_WITH, "sts-publish", new Provider<Handler>() {
            @Override
            public Handler get() {
                return getHandler();
            }
        }));
    }

    private Handler getHandler() {
        final RequestHandler restPublishRequestHandler =
                new RestSTSPublishServiceRequestHandler(
                        STSPublishInjectorHolder.getInstance(Key.get(RestSTSInstancePublisher.class)),
                        STSPublishInjectorHolder.getInstance(Key.get(RestRealmValidator.class)),
                        STSPublishInjectorHolder.getInstance(Key.get(new TypeLiteral<InstanceConfigMarshaller<RestSTSInstanceConfig>>() {})),
                        STSPublishInjectorHolder.getInstance(Key.get(Logger.class)));
        rootRouter.route("rest")
                .auditAs(STS)
                .authorizeWith(STSPublishServiceAuthzModule.class)
                .toRequestHandler(STARTS_WITH, restPublishRequestHandler);

        final RequestHandler soapPublishRequestHandler =
                new SoapSTSPublishServiceRequestHandler(
                        STSPublishInjectorHolder.getInstance(Key.get(SoapSTSInstancePublisher.class)),
                        STSPublishInjectorHolder.getInstance(Key.get(RestRealmValidator.class)),
                        STSPublishInjectorHolder.getInstance(Key.get(new TypeLiteral<InstanceConfigMarshaller<SoapSTSInstanceConfig>>() {})),
                        STSPublishInjectorHolder.getInstance(Key.get(Logger.class)));
        rootRouter.route("soap")
                .auditAs(STS)
                .authorizeWith(STSPublishServiceAuthzModule.class)
                .toRequestHandler(STARTS_WITH, soapPublishRequestHandler);

        return Handlers.chainOf(newHttpHandler(rootRouter.getRouter()), authenticationFilter);
    }
}
