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

/**
 * Represents a single consumer of events.
 * <p>
 * A subscription may be over multiple topics.
 * <p>
 * A subscription is closable. Once a subscription is closed,
 * it will not receive any more notifications.
 *
 * @since 14.0.0
 */
public interface Subscription extends AutoCloseable {

    /**
     * Updates the subscription to receive notifications for the given topic.
     *
     * @param topic the new topic to bind to
     * @return this subscription
     */
    Subscription bindTo(Topic topic);

    /**
     * Updates the subscription to stop receiving notifications for the given topic.
     *
     * @param topic the topic to be unbound from
     * @return this subscription
     */
    Subscription unbindFrom(Topic topic);

    /**
     * Determines whether the subscription is bound to the given topic.
     *
     * @param topic the topic of interest
     * @return whether this subscription is bound to the given topic
     */
    boolean isBoundTo(Topic topic);

    /**
     * Closes the subscription. After a subscription is closed no more notifications will be delivered to the consumer.
     */
    @Override
    void close();

}
