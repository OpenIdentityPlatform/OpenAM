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
 * Exception for issues encountered during the operation of the PushNotificationService or
 * PushNotificationDelegates.
 */
public class PushNotificationException extends Exception {

    /**
     * Construct a new PushNotificationException with a message.
     * @param message description of the exceptional circumstances.
     */
    public PushNotificationException(String message) {
        super(message);
    }

    /**
     * Construct a new PushNotificationException with a message and a Throwable indicating the cause
     * of the exception.
     * @param message description of the exceptional circumstances.
     * @param throwable cause of the throwable circumstances.
     */
    public PushNotificationException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
