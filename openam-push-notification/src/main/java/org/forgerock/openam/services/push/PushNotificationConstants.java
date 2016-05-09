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

import static org.forgerock.openam.services.push.PushMessage.MESSAGE_ID;

import org.forgerock.json.JsonPointer;

/**
 * Constants for the PushNotification Services and Delegates.
 */
public final class PushNotificationConstants {

    /**
     * Uninstantiable.
     */
    private PushNotificationConstants() {
        //This section intentionally left blank.
    }

    /**
     * MESSAGE.
     */

    /** Pointer to the location of the Device name in the mobile message. */
    public static final JsonPointer DEVICE_NAME_JSON_POINTER = new JsonPointer("data/deviceName");
    /** Pointer to the location of the Mobile platform Communication ID in the mobile message. */
    public static final JsonPointer DEVICE_COMMUNICATION_ID_JSON_POINTER = new JsonPointer("data/communicationId");
    /** Pointer to the location of the login mechanism id in the mobile message. */
    public static final JsonPointer DEVICE_MECHANISM_UID_JSON_POINTER = new JsonPointer("data/mechanismUid");
    /** Pointer to the location of the communication type id in the mobile message. */
    public static final JsonPointer DEVICE_COMMUNICATION_TYPE_JSON_POINTER = new JsonPointer("data/communicationType");
    /** Pointer to the location of the device type id in the mobile message. */
    public static final JsonPointer DEVICE_TYPE_JSON_POINTER = new JsonPointer("data/deviceType");
    /** Pointer to the location of the device id in the mobile message. */
    public static final JsonPointer DEVICE_ID_JSON_POINTER = new JsonPointer("data/deviceId");
    /** Pointer to the location of the messageId in the mobile message. */
    public static final JsonPointer MESSAGE_ID_JSON_POINTER = new JsonPointer("data/" + MESSAGE_ID);

    /**
     * DELEGATE.
     */

    /** Key to the service configuration accessKey field. */
    static final String DELEGATE_ACCESS_KEY = "accessKey";
    /** Key to the service configuration endpoint field. */
    static final String DELEGATE_APPLE_ENDPOINT = "appleEndpoint";
    /** Key to the service configuration apiKey field. */
    static final String DELEGATE_GOOGLE_ENDPOINT = "googleEndpoint";
    /** Key to the service configuration apiKey field. */
    static final String DELEGATE_SECRET = "secret";
    /** Key to the service configuration factory field. */
    static final String DELEGATE_FACTORY_CLASS = "delegateFactory";

    /**
     * DEFAULTS.
     */

    /** Default delegate factory class used if the factory entry is missing. */
    static final String DEFAULT_DELEGATE_FACTORY_CLASS = "org.forgerock.openam.push.sns.SnsHttpDelegateFactory";

    /**
     * SERVICE.
     */

    /** Name of the PushNotificationService. */
    public static final String SERVICE_NAME = "PushNotificationService";
    /** Version of the PushNotificationService. */
    public static final String SERVICE_VERSION = "1.0";
}
