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

import static org.forgerock.openam.services.push.PushNotificationConstants.*;

import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceConfig;

/**
 * Helper for reading a PushNotificationService config, to decouple and aid testing.
 */
public class PushNotificationServiceConfigHelper {

    private final static long DEFAULT_SIZE = 10000L;
    private final static long DEFAULT_DURATION = 120L;
    private final static int DEFAULT_CONCURRENCY = 16;

    private ServiceConfig serviceConfig;

    private final Debug debug;

    /**
     * Produce a new PushNotificationServiceConfigHelper for the provided ServiceConfig.
     *
     * @param serviceConfig The realm-specific service config to read.
     * @param debug The debug writer, in case of errors reading the config.
     */
    public PushNotificationServiceConfigHelper(ServiceConfig serviceConfig, Debug debug) {
        this.serviceConfig = serviceConfig;
        this.debug = debug;
    }

    /**
     * Retrieve the factory class used to generate PushNotificationDelegates described by this config.
     * @return A String containing the classname of the PushNotificationDelegateFactory class to use.
     */
    public String getFactoryClass() {
        return CollectionHelper.getMapAttr(serviceConfig.getAttributes(), DELEGATE_FACTORY_CLASS,
                DEFAULT_DELEGATE_FACTORY_CLASS);
    }

    /**
     * Retrieve a new PushNotificationServiceConfig from this Helper.
     * @return A valid PushNotificationServiceConfig for the delegate described by this service config.
     * @throws PushNotificationException if there was an issue building a config object from the service data.
     */
    public PushNotificationServiceConfig getConfig() throws PushNotificationException {

        String accessKey = CollectionHelper.getMapAttr(serviceConfig.getAttributes(), DELEGATE_ACCESS_KEY);
        String secret = CollectionHelper.getMapAttr(serviceConfig.getAttributes(), DELEGATE_SECRET);
        String appleEndpoint = CollectionHelper.getMapAttr(serviceConfig.getAttributes(), DELEGATE_APPLE_ENDPOINT);
        String googleEndpoint = CollectionHelper.getMapAttr(serviceConfig.getAttributes(), DELEGATE_GOOGLE_ENDPOINT);
        String delegateFactory = CollectionHelper.getMapAttr(serviceConfig.getAttributes(), DELEGATE_FACTORY_CLASS);
        String region = CollectionHelper.getMapAttr(serviceConfig.getAttributes(), DELEGATE_REGION);

        long maxSize = CollectionHelper.getLongMapAttr(serviceConfig.getAttributes(), MESSAGE_DISPATCHER_CACHE_SIZE,
                DEFAULT_SIZE, debug);
        long duration = CollectionHelper.getLongMapAttr(serviceConfig.getAttributes(), MESSAGE_DISPATCHER_DURATION,
                DEFAULT_DURATION, debug);
        int concurrency = CollectionHelper.getIntMapAttr(serviceConfig.getAttributes(), MESSAGE_DISPATCHER_CONCURRENCY,
                DEFAULT_CONCURRENCY, debug);

        return new PushNotificationServiceConfig.Builder()
                .withAccessKey(accessKey)
                .withSecret(secret)
                .withAppleEndpoint(appleEndpoint)
                .withGoogleEndpoint(googleEndpoint)
                .withDelegateFactory(delegateFactory)
                .withRegion(region)
                .withMessageDispatcherSize(maxSize)
                .withMessageDispatcherDuration(duration)
                .withMessageDispatcherConcurrency(concurrency)
                .build();
    }

}
