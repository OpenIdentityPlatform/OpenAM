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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2014-2015 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.publish.service;

import static org.forgerock.authz.filter.crest.AuthorizationFilters.createAuthorizationFilter;
import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;
import static org.forgerock.http.routing.Version.version;
import static org.forgerock.json.resource.RouteMatchers.requestResourceApiVersionMatcher;
import static org.forgerock.json.resource.RouteMatchers.requestUriMatcher;

import javax.inject.Provider;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.http.routing.Version;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.FilterChain;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.Router;
import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.openam.audit.AuditConstants.Component;
import org.forgerock.openam.rest.AuthenticationFilter;
import org.forgerock.openam.rest.authz.LoggingAuthzModule;
import org.forgerock.openam.rest.authz.STSPublishServiceAuthzModule;
import org.forgerock.openam.rest.fluent.AuditFilter;
import org.forgerock.openam.rest.fluent.AuditFilterWrapper;
import org.forgerock.openam.rest.router.RestRealmValidator;
import org.forgerock.openam.sts.InstanceConfigMarshaller;
import org.forgerock.openam.sts.publish.config.STSPublishInjectorHolder;
import org.forgerock.openam.sts.publish.rest.RestSTSInstancePublisher;
import org.forgerock.openam.sts.publish.soap.SoapSTSInstancePublisher;
import org.forgerock.openam.sts.rest.config.user.RestSTSInstanceConfig;
import org.forgerock.openam.sts.soap.config.user.SoapSTSInstanceConfig;
import org.slf4j.Logger;

/**
 * Referenced in the web.xml. Returns the ConnectionFactory required by the Crest Rest STS instance publish
 * SingletonResourceProvider.
 */
public class STSPublishServiceConnectionFactoryProvider {
    private static final Version VERSION = version(1);
    public static ConnectionFactory getConnectionFactory(final AuthenticationFilter defaultAuthenticationFilter) {
        Router router = new Router();
        final RequestHandler restPublishRequestHandler =
                new RestSTSPublishServiceRequestHandler(
                    STSPublishInjectorHolder.getInstance(Key.get(RestSTSInstancePublisher.class)),
                    STSPublishInjectorHolder.getInstance(Key.get(RestRealmValidator.class)),
                    STSPublishInjectorHolder.getInstance(Key.get(new TypeLiteral<InstanceConfigMarshaller<RestSTSInstanceConfig>>() {})),
                    STSPublishInjectorHolder.getInstance(Key.get(Logger.class)));
        Router restVersionRouter = new Router();
        restVersionRouter.addRoute(requestResourceApiVersionMatcher(VERSION), restPublishRequestHandler);
        FilterChain restAuthzFilterChain = createAuthorizationFilter(restVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(STSPublishServiceAuthzModule.class), STSPublishServiceAuthzModule.NAME));
        AuditFilterWrapper restAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.STS);
        FilterChain restFilterChain = new FilterChain(restAuthzFilterChain, restAuditFilter);
        router.addRoute(requestUriMatcher(STARTS_WITH, "rest"), restFilterChain);

        final RequestHandler soapPublishRequestHandler =
                new SoapSTSPublishServiceRequestHandler(
                        STSPublishInjectorHolder.getInstance(Key.get(SoapSTSInstancePublisher.class)),
                        STSPublishInjectorHolder.getInstance(Key.get(RestRealmValidator.class)),
                        STSPublishInjectorHolder.getInstance(Key.get(new TypeLiteral<InstanceConfigMarshaller<SoapSTSInstanceConfig>>() {})),
                        STSPublishInjectorHolder.getInstance(Key.get(Logger.class)));
        Router soapVersionRouter = new Router();
        soapVersionRouter.addRoute(requestResourceApiVersionMatcher(VERSION), soapPublishRequestHandler);
        FilterChain soapAuthzFilterChain = createFilter(soapVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(STSPublishServiceAuthzModule.class), STSPublishServiceAuthzModule.NAME));
        AuditFilterWrapper soapAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.STS);
        FilterChain soapFilterChain = new FilterChain(soapAuthzFilterChain, defaultAuthenticationFilter, soapAuditFilter);
        router.addRoute(requestUriMatcher(STARTS_WITH, "soap"), soapFilterChain);

        return Resources.newInternalConnectionFactory(router);
    }
}
