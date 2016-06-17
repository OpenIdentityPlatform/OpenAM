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
package org.forgerock.openam.services.push.sns.utils;

import com.sun.identity.shared.debug.Debug;
import javax.inject.Inject;
import javax.inject.Named;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.utils.JSONSerialisation;
import org.forgerock.openam.services.push.PushNotificationService;
import org.forgerock.openam.services.push.sns.SnsMessageResource;

/**
 * Helper factory to produce SnsMessageResources.
 */
public class SnsMessageResourceFactory {

    private final CTSPersistentStore coreTokenService;
    private final JSONSerialisation jsonSerialisation;
    private final PushNotificationService pushNotificationService;
    private final Debug debug;

    /**
     * SnsMessageResourceFactory to provude SnsMessageResources with the appropriate resources.
     *
     * @param coreTokenService The CTS instance to utilise.
     * @param jsonSerialisation For serializing down to the CTS.
     * @param debug For logging purposes.
     * @param pushNotificationService for retrieving the appropriate message dispatcher for this realm.
     */
    @Inject
    public SnsMessageResourceFactory(CTSPersistentStore coreTokenService, JSONSerialisation jsonSerialisation,
                                     @Named("frRest") Debug debug, PushNotificationService pushNotificationService) {
        this.coreTokenService = coreTokenService;
        this.jsonSerialisation = jsonSerialisation;
        this.pushNotificationService = pushNotificationService;
        this.debug = debug;
    }

    /**
     * Generates a new SnsMessageResource.
     *
     * @return a new SnsMessageResource.
     */
    public SnsMessageResource produce() {
        return new SnsMessageResource(coreTokenService, pushNotificationService, jsonSerialisation, debug);
    }
}
