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
package org.forgerock.openam.authentication.modules.push;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.iplanet.sso.SSOException;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;
import java.security.AccessController;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Named;
import org.forgerock.guice.core.GuiceModule;
import org.forgerock.openam.services.push.PushNotificationConstants;
import org.forgerock.openam.services.push.PushNotificationDelegate;
import org.forgerock.openam.services.push.PushNotificationDelegateFactory;

/**
 * Guice bindings for the Push module.
 */
@GuiceModule
public class AuthenticatorPushGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Debug.class).annotatedWith(Names.named("frPush")).toInstance(Debug.getInstance("frPush"));
        bind(new TypeLiteral<ConcurrentMap<String, PushNotificationDelegate>>(){})
                .toInstance(new ConcurrentHashMap<String, PushNotificationDelegate>());
        bind(new TypeLiteral<ConcurrentMap<String, PushNotificationDelegateFactory>>(){})
                .toInstance(new ConcurrentHashMap<String, PushNotificationDelegateFactory>());
    }

    @Provides
    @Named("PushNotificationService")
    ServiceConfigManager getPushNotificationService() throws SMSException, SSOException {
        return new ServiceConfigManager(AccessController.doPrivileged(AdminTokenAction.getInstance()),
                PushNotificationConstants.SERVICE_NAME, PushNotificationConstants.SERVICE_VERSION);
    }


}
