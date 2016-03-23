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

/**
 * Constants common to GCM implementations of a Push Notification Delegate.
 */
final class GcmConstants {

    /**
     * Uninstantiable.
     */
    private GcmConstants() {
        //This section intentionally left blank
    }

    /**
     * DATA FORMAT.
     */

    /** Key to the recipient of the message. */
    public static final String TO = "to";
    /** Key to the content of the message. */
    public static final String DATA = "data";
    /** Key to the priority of the message. */
    public static final String PRIORITY = "priority";

    /**
     * HTTP.
     */

    /** Message method used. */
    public static final String HTTP_METHOD = "POST";
    /** Key to the auth header. */
    public static final String HTTP_AUTH_HEADER = "Authorization";
    /** Key to the content-type header. */
    public static final String HTTP_CONTENT_TYPE_HEADER = "Content-Type";
    /** Content of the messages. */
    public static final String HTTP_CONTENT_TYPE = "application/json";
    /** The priority to use for the message. */
    public static final String HIGH_PRIORITY = "high";
    /** Prefix to auth header value. */
    public static final String HTTP_AUTH_KEY = "key=";
}
