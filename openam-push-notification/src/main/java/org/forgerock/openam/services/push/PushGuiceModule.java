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

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Names;
import com.iplanet.sso.SSOException;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;
import java.security.AccessController;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import javax.inject.Named;
import org.forgerock.guava.common.cache.Cache;
import org.forgerock.guava.common.cache.CacheBuilder;
import org.forgerock.guice.core.GuiceModule;
import org.forgerock.openam.services.push.dispatch.MessagePromise;

/**
 * Guice module for OpenAM Push related classes.
 */
@GuiceModule
public class PushGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Debug.class).annotatedWith(Names.named("frPush")).toInstance(Debug.getInstance("frPush"));
    }

    /**
     * Generates a new Cache for the MessageDispatcher.
     * @return a newly constructed Cache.
     */
    @Provides
    public Cache<String, MessagePromise> getMessageDispatchCache() {
        return CacheBuilder.newBuilder()
                .concurrencyLevel(16)
                .maximumSize(10000)
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .build();
    }

    @Provides
    ConcurrentMap<String, PushNotificationDelegate> getPushNotificationDelegateMap() {
        return new ConcurrentHashMap<>();
    }

    @Provides
    ConcurrentMap<String, PushNotificationDelegateFactory> getPushNotificationDelegateFactoryMap() {
        return new ConcurrentHashMap<>();
    }

    @Provides
    @Named("PushNotificationService")
    ServiceConfigManager getPushNotificationService() throws SMSException, SSOException {
        return new ServiceConfigManager(AccessController.doPrivileged(AdminTokenAction.getInstance()),
                PushNotificationConstants.SERVICE_NAME, PushNotificationConstants.SERVICE_VERSION);
    }

}
