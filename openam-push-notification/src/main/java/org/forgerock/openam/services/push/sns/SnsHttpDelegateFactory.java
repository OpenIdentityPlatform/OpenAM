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
package org.forgerock.openam.services.push.sns;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.resource.Router;
import org.forgerock.openam.services.push.PushNotificationDelegateFactory;
import org.forgerock.openam.services.push.PushNotificationException;
import org.forgerock.openam.services.push.PushNotificationServiceConfig;

/**
 * Produces SnsHttpDelegates matching the PushNotificationServiceFactory interface.
 */
public class SnsHttpDelegateFactory implements PushNotificationDelegateFactory {

    private final static Key<Router> KEY = Key.get(Router.class, Names.named("CrestRealmRouter"));

    private final Debug debug;
    private final SnsMessageResource messageResource;
    private final SnsPushMessageConverter pushMessageConverter;
    private final Router router;

    /**
     * Default constructor sets the debug for passing into produced delegates.
     */
    public SnsHttpDelegateFactory() {
        debug = Debug.getInstance("frPush");
        messageResource = InjectorHolder.getInstance(SnsMessageResource.class);
        pushMessageConverter  = InjectorHolder.getInstance(SnsPushMessageConverter.class);
        router = InjectorHolder.getInstance(KEY);
    }

    @Override
    public SnsHttpDelegate produceDelegateFor(PushNotificationServiceConfig config) throws PushNotificationException {
        AmazonSNSClient service = new AmazonSNSClient(
                new BasicAWSCredentials(config.getAccessKey(), config.getSecret()));
        service.setRegion(Region.getRegion(Regions.US_WEST_2));
        return new SnsHttpDelegate(service, config, router, messageResource, pushMessageConverter, debug);
    }

}
