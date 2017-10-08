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

import org.forgerock.openam.services.push.dispatch.MessageDispatcher;

/**
 * Defines how PushNotificationDelegates should be created. This acts as a plugin point for
 * customers to ensure they can configure their own PushNotificationDelegates if desired.
 *
 * @see PushNotificationService#createFactory(String)
 */
public interface PushNotificationDelegateFactory {

    /**
     * Produce a delegate for a given configuration. This delegate should be ready to use
     * as soon as the delegate has been produced.
     * @param config The config that will be used to configure the PushNotificationService.
     * @param realm The realm in which this delegate will exist.
     * @param messageDispatcher The message dispatcher for this delegate.
     * @return A valid PushNotificationService, ready to send (and receive if appropriate) messages.
     * @throws PushNotificationException in the case where we cannot generate an appropriate delegate.
     */
    PushNotificationDelegate produceDelegateFor(PushNotificationServiceConfig config, String realm,
                                                MessageDispatcher messageDispatcher)
            throws PushNotificationException;

}
