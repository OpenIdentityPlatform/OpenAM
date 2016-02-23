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
package org.forgerock.openam.services.push.gcm;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.services.push.PushNotificationDelegateFactory;
import org.forgerock.openam.services.push.PushNotificationServiceConfig;

/**
 * Produces GcmXMPPDelegates matching the PushNotificationServiceFactory interface.
 */
public class GcmXmppDelegateFactory implements PushNotificationDelegateFactory {

    private final Debug debug;

    /**
     * Default constructor sets the debug so passing into produced delegates.
     */
    public GcmXmppDelegateFactory() {
        debug = Debug.getInstance("frPush");
    }

    @Override
    public GcmXmppDelegate produceDelegateFor(PushNotificationServiceConfig config) {
        return new GcmXmppDelegate(config, debug);
    }

}
