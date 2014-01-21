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
 * Copyright 2013-2014 ForgeRock Inc.
 */

package org.forgerock.openam.forgerockrest.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.name.Names;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.openam.guice.AMGuiceModule;
import org.forgerock.openam.rest.resource.RealmRouterConnectionFactory;
import org.forgerock.openam.rest.router.RestEndpointManager;
import org.forgerock.openam.rest.router.RestEndpointManagerProxy;
import org.forgerock.openam.utils.AMKeyProvider;
import org.forgerock.util.SignatureUtil;

import javax.inject.Singleton;

import static org.forgerock.openam.forgerockrest.guice.RestEndpointGuiceProvider.CrestRealmConnectionFactoryProvider;
import static org.forgerock.openam.forgerockrest.guice.RestEndpointGuiceProvider.RestCollectionResourceEndpointsBinder;
import static org.forgerock.openam.forgerockrest.guice.RestEndpointGuiceProvider.RestServiceEndpointsBinder;
import static org.forgerock.openam.forgerockrest.guice.RestEndpointGuiceProvider.RestSingletonResourceEndpointsBinder;

/**
 * Guice Module for configuring bindings for the AuthenticationRestService classes.
 */
@AMGuiceModule
public class ForgerockRestGuiceModule extends AbstractModule {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void configure() {
        bind(AMKeyProvider.class).in(Singleton.class);
        bind(SignatureUtil.class).toProvider(new Provider<SignatureUtil>() {
            public SignatureUtil get() {
                return SignatureUtil.getInstance();
            }
        });

        bind(Debug.class).annotatedWith(Names.named("frRest")).toInstance(Debug.getInstance("frRest"));


        // vvvv Rest Endpoint Bindings vvvv
        bind(RestEndpointManager.class).to(RestEndpointManagerProxy.class);

        // CREST Connection Factory
        bind(ConnectionFactory.class)
                .annotatedWith(Names.named(RealmRouterConnectionFactory.CONNECTION_FACTORY_NAME))
                .toProvider(CrestRealmConnectionFactoryProvider.class)
                .in(Singleton.class);

        // Actual endpoint bindings
        RestCollectionResourceEndpointsBinder.newRestCollectionResourceEndpointBinder(binder());
        RestSingletonResourceEndpointsBinder.newRestSingletonResourceEndpointBinder(binder());
        RestServiceEndpointsBinder.newRestServiceEndpointBinder(binder());
        // ^^^^ Rest Endpoint Bindings ^^^^
    }
}
