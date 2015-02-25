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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.oidc;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.sun.identity.common.HttpURLConnectionManager;
import org.forgerock.guice.core.GuiceModule;
import org.forgerock.jaspi.modules.openid.resolvers.OpenIdResolverFactory;

import javax.inject.Singleton;

@GuiceModule
public class OpenIdConnectGuiceModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(OpenIdResolverCache.class).to(OpenIdResolverCacheImpl.class).in(Scopes.SINGLETON);
    }

    /*
    The OpenIdResolverCacheImpl needs an instance of the OpenIdResolverFactory to do its work. Can't bind the
    OpenIdResovlerFactory directly, as it has neither a no-arg ctor or a ctor with @Inject, so get around this with
    a provider.
     */
    @Provides
    @Singleton
    OpenIdResolverFactory getResolverFactory() {
        return new OpenIdResolverFactory(HttpURLConnectionManager.getReadTimeout(), HttpURLConnectionManager.getConnectTimeout());
    }
}
