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
     * KEYS.
     */

    /** Name of the key where the communicationType is stored. */
    public static final String COMMUNICATION_TYPE = "communicationType";
    /** Name of the key where the deviceType is stored. */
    public static final String DEVICE_TYPE = "deviceType";
    /** Name of the key where the communicationId is stored. */
    public static final String COMMUNICATION_ID = "communicationId";
    /** Name of the key where the deviceId is stored. */
    public static final String DEVICE_ID = "deviceId";
    /** Name of the key where the mechanismUid is stored. */
    public static final String MECHANISM_UID = "mechanismUid";
    /** Name of the key where the JWT is stored. */
    public static final String JWT = "jwt";

    /**
     * MESSAGE.
     */

    /** Pointer to the location of the messageId in the mobile message. */
    public static final JsonPointer MESSAGE_ID_JSON_POINTER = new JsonPointer(MESSAGE_ID);
    /** Pointer to the location of the JWT data returned by the device. */
    public static final JsonPointer DATA_JSON_POINTER = new JsonPointer(JWT);
    /** Claim set location of response. */
    public static final String RESPONSE_LOCATION = "response";
    /** Claim set location of deny. */
    public static final String DENY_LOCATION = "deny";
    /** General alg. */
    public static final String HMACSHA256 = "HmacSHA256";
    /** Deny value. */
    public static final int DENY_VALUE = 0;
    /** Accept value. */
    public static final int ACCEPT_VALUE = 1;


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
    /** Key to the service configuration region field. */
    static final String DELEGATE_REGION = "region";
    /** Key to the service configuration region field. */
    static final String MESSAGE_DISPATCHER_CACHE_SIZE = "mdCacheSize";
    /** Key to the service configuration region field. */
    static final String MESSAGE_DISPATCHER_DURATION = "mdDuration";
    /** Key to the service configuration region field. */
    static final String MESSAGE_DISPATCHER_CONCURRENCY = "mdConcurrency";


    /**
     * DEFAULTS.
     */

    /** Default delegate factory class used if the factory entry is missing. */
    static final String DEFAULT_DELEGATE_FACTORY_CLASS = "org.forgerock.openam.push.sns.SnsHttpDelegateFactory";

    /**
     * REMOTE SERVICE NAMES.
     */

    /** Apple Push Notification Service. */
    public static final String APNS = "apns";
    /** Google Cloud Messenger. */
    public static final String GCM = "gcm";

    /**
     * SERVICE.
     */

    /** Name of the PushNotificationService. */
    public static final String SERVICE_NAME = "PushNotificationService";
    /** Version of the PushNotificationService. */
    public static final String SERVICE_VERSION = "1.0";

}
