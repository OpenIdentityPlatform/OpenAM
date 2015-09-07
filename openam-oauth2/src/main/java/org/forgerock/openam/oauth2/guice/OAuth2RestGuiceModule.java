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

package org.forgerock.openam.oauth2.guice;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import org.forgerock.guice.core.GuiceModule;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.openam.oauth2.OpenAMTokenStore;
import org.forgerock.openam.oauth2.rest.OAuth2RouterProvider;
import org.forgerock.openam.utils.Config;
import org.restlet.routing.Router;

/**
 * Guice module for OAuth2 Rest endpoint bindings.
 *
 * @since 13.0.0
 */
@GuiceModule
public class OAuth2RestGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Key.get(Router.class, Names.named("OAuth2Router"))).toProvider(OAuth2RouterProvider.class)
                .in(Singleton.class);

        bind(new TypeLiteral<Config<TokenStore>>() {
        }).toInstance(new Config<TokenStore>() {
            public boolean isReady() {
                return true;
            }

            public TokenStore get() {
                return InjectorHolder.getInstance(OpenAMTokenStore.class);
            }
        });
    }
}
