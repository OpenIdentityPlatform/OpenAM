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

import java.io.Closeable;

/**
 * The {@link PushNotificationService} is responsible for monitoring configuration changes and detecting
 * when it should create new instances of any given {@link PushNotificationDelegate}. If a configuration
 * change takes place, the service will ascertain whether a new instance is required by calling
 * {@link #isRequireNewDelegate(PushNotificationServiceConfig)}.
 *
 * If a new instance is required, the previous instance will be shutdown by calling the {@link #close()}
 * method before creating a new instance.
 *
 * If a new instance is not required then the existing instance will be updated by calling
 * {@link #updateDelegate(PushNotificationServiceConfig)}.
 */
public interface PushNotificationDelegate extends Closeable {

    /**
     * Starts any required services for this delegate. This may include e.g. listeners to specific ports.
     * This method is called once after a new delegate is generated, but before it is made available for
     * general use by the PushNotificationService.
     *
     * @throws PushNotificationException if there were any issues starting the service.
     */
    void startServices() throws PushNotificationException;

    /**
     * Used to send a message out to the PushNotificationDelegate to be delivered.
     * @param message The message to send.
     */
    void send(PushMessage message);

    /**
     * Returns whether or not the new config is so different from the old that the existing delegate
     * should be removed from the pool.
     *
     * @param newConfig The new configuration to check against the old.
     * @return True if the configuration change requires the PushNotificationService to instantiate a new instance of
     * this delegate.
     */
    boolean isRequireNewDelegate(PushNotificationServiceConfig newConfig);

    /**
     * Updates the existing delegate's config. Implementors must not alter the connection parameters
     * of the delegate, but may alter extraneous information such as values added to static fields in the
     * delegate's communicated messages.
     *
     * @param newConfig The new config from which to update the existing delegate.
     */
    void updateDelegate(PushNotificationServiceConfig newConfig);

}