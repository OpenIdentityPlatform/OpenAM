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

package org.forgerock.openam.http;

import javax.inject.Singleton;
import java.util.ServiceLoader;

import com.google.inject.Key;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.name.Names;
import org.forgerock.guice.core.GuiceModule;
import org.forgerock.http.Handler;
import org.forgerock.http.HttpApplication;

/**
 * A Guice {@link PrivateModule} containing bindings for OpenAM HTTP
 * integration.
 *
 * <p>Exposes the {@link HttpApplication}.</p>
 *
 * @since 13.0.0
 */
@GuiceModule
public final class HttpGuiceModule extends PrivateModule {

    @Override
    protected void configure() {
        bind(HttpApplication.class).to(OpenAMHttpApplication.class);
        bind(Key.get(Handler.class, Names.named("HttpHandler"))).toProvider(HttpRouterProvider.class)
                .in(Singleton.class);

        expose(HttpApplication.class);
    }

    @Provides
    @Singleton
    Iterable<HttpRouteProvider> getHttpRouteProviders() {
        return ServiceLoader.load(HttpRouteProvider.class);
    }
}
