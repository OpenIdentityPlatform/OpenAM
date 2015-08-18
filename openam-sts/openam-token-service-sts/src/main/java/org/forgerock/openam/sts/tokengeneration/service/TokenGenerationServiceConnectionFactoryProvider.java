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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.tokengeneration.service;

import static org.forgerock.authz.filter.crest.AuthorizationFilters.createAuthorizationFilter;
import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;
import static org.forgerock.http.routing.Version.version;
import static org.forgerock.json.resource.RouteMatchers.requestUriMatcher;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.http.routing.Version;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.FilterChain;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.Router;
import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.openam.rest.AuthenticationFilter;
import org.forgerock.openam.rest.authz.LoggingAuthzModule;
import org.forgerock.openam.rest.authz.STSTokenGenerationServiceAuthzModule;
import org.forgerock.openam.rest.fluent.AuditFilter;
import org.forgerock.openam.rest.fluent.AuditFilterWrapper;
import org.forgerock.openam.sts.tokengeneration.CTSTokenPersistence;
import org.forgerock.openam.sts.tokengeneration.config.TokenGenerationServiceInjectorHolder;
import org.forgerock.openam.sts.tokengeneration.oidc.OpenIdConnectTokenGeneration;
import org.forgerock.openam.sts.tokengeneration.saml2.SAML2TokenGeneration;
import org.forgerock.openam.sts.tokengeneration.state.RestSTSInstanceState;
import org.forgerock.openam.sts.tokengeneration.state.STSInstanceStateProvider;
import org.forgerock.openam.sts.tokengeneration.state.SoapSTSInstanceState;
import org.slf4j.Logger;

/**
 * CREST servlet connection factory provider for the token-generation-service. References the TokenGenerationServiceInjectorHolder
 * which initializes the guice injector to create the bindings defined in the TokenGenerationModule.
 */
public class TokenGenerationServiceConnectionFactoryProvider {

    private static final Version VERSION = version(1);

    public static ConnectionFactory getConnectionFactory(AuthenticationFilter defaultAuthenticationFilter) {
        Router router = new Router();
        final CollectionResourceProvider tokenGenerationService =
                new TokenGenerationService(
                        TokenGenerationServiceInjectorHolder.getInstance(Key.get(SAML2TokenGeneration.class)),
                        TokenGenerationServiceInjectorHolder.getInstance(Key.get(OpenIdConnectTokenGeneration.class)),
                        TokenGenerationServiceInjectorHolder.getInstance(Key.get(new TypeLiteral<STSInstanceStateProvider<RestSTSInstanceState>>(){})),
                        TokenGenerationServiceInjectorHolder.getInstance(Key.get(new TypeLiteral<STSInstanceStateProvider<SoapSTSInstanceState>>(){})),
                        TokenGenerationServiceInjectorHolder.getInstance(Key.get(CTSTokenPersistence.class)),
                        TokenGenerationServiceInjectorHolder.getInstance(Key.get(Logger.class)));
        Router issueVersionRouter = new Router();
        issueVersionRouter.addRoute(VERSION, tokenGenerationService);
        FilterChain issueAuthzFilterChain = createAuthorizationFilter(issueVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(STSTokenGenerationServiceAuthzModule.class), STSTokenGenerationServiceAuthzModule.NAME));
        AuditFilterWrapper issueAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.STS);
        FilterChain issueFilterChain = new FilterChain(issueAuthzFilterChain, defaultAuthenticationFilter, issueAuditFilter);
        router.addRoute(requestUriMatcher(STARTS_WITH, ""), issueFilterChain);
        return Resources.newInternalConnectionFactory(router);
    }
}
