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

package org.forgerock.openam.notifications;

import org.forgerock.json.JsonValue;

/**
 * Delivers notifications to subscribers of topics.
 * <p>
 * A <tt>NotificationBroker</tt> can be shut down, which will cause
 * it to reject new notifications.
 *
 * @since 14.0.0
 */
public interface NotificationBroker {

    /**
     * Publish passes the notification onto subscribers of the topic. If there
     * are no subscribers to the same topic, the notification is discarded.
     * <p>
     * Publishing notifications may fail and this is signalled by returning
     * false.
     *
     * @param topic topic
     * @param notification notification
     * @return whether the notification has successfully been published.
     */
    boolean publish(Topic topic, JsonValue notification);

    /**
     * Creates a new subscription that is initially not bound to any topics.
     * <p>
     * The given consumer will be called for each notification that is published to any of the bound topics.
     *
     * @param consumer a consumer that will be called once per published notification
     * @return a new subscriber
     */
    Subscription subscribe(Consumer consumer);

    /**
     * Initiates an orderly shutdown in which previously published
     * notifications are delivered, but no new notifications will be accepted.
     * Invocation has no additional effect if already shut down.
     */
    void shutdown();

}
