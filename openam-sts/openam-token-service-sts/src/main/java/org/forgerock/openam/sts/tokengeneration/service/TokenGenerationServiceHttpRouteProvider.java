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

package org.forgerock.openam.sts.tokengeneration.service;

import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.util.Collections;
import java.util.Set;

import org.forgerock.http.Handler;
import org.forgerock.json.resource.http.CrestHttp;
import org.forgerock.openam.http.HttpRoute;
import org.forgerock.openam.http.HttpRouteProvider;
import org.forgerock.openam.rest.AuthenticationFilter;
import org.forgerock.util.Function;
import org.forgerock.util.promise.NeverThrowsException;

public class TokenGenerationServiceHttpRouteProvider implements HttpRouteProvider {

    private Provider<AuthenticationFilter> authenticationFilterProvider;

    @Inject
    public void setAuthenticationFilterProvider(
            @Named("RestAuthenticationFilter") Provider<AuthenticationFilter> authenticationFilterProvider) {
        this.authenticationFilterProvider = authenticationFilterProvider;
    }

    @Override
    public Set<HttpRoute> get() {
        return Collections.singleton(HttpRoute.newHttpRoute(STARTS_WITH, "sts-tokengen", new Function<Void, Handler, NeverThrowsException>() {
                    @Override
                    public Handler apply(Void value) throws NeverThrowsException {
                        return CrestHttp.newHttpHandler(TokenGenerationServiceConnectionFactoryProvider.getConnectionFactory(authenticationFilterProvider.get()));
                    }
                }));
    }
}
