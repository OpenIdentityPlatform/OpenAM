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
     * DELEGATE.
     */

    /** Key to the service configuration senderId field. */
    static final String DELEGATE_SENDER_ID = "senderId";
    /** Key to the service configuration endpoint field. */
    static final String DELEGATE_ENDPOINT = "endpoint";
    /** Key to the service configuration apiKey field. */
    static final String DELEGATE_API_KEY = "apiKey";
    /** Key to the service configuration factory field. */
    static final String DELEGATE_FACTORY_CLASS = "delegateFactory";

    /**
     * DEFAULTS.
     */

    /** Default delegate factory class used if the factory entry is missing. */
    static final String DEFAULT_DELEGATE_FACTORY_CLASS = "org.forgerock.openam.push.gcm.GcmHttpDelegateFactory";

    /**
     * SERVICE.
     */

    /** Name of the PushNotificationService. */
    public static final String SERVICE_NAME = "PushNotificationService";
    /** Version of the PushNotificationService. */
    public static final String SERVICE_VERSION = "1.0";
}
