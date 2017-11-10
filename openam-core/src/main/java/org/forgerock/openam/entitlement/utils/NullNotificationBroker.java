/*
 * The contents of this file are subject to the terms of the Common Development and
 *  Distribution License (the License). You may not use this file except in compliance with the
 *  License.
 *
 *  You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 *  specific language governing permission and limitations under the License.
 *
 *  When distributing Covered Software, include this CDDL Header Notice in each file and include
 *  the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 *  Header, with the fields enclosed by brackets [] replaced by your own identifying
 *  information: "Portions copyright [year] [name of copyright owner]".
 *
 *  Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.entitlement.utils;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.notifications.Consumer;
import org.forgerock.openam.notifications.NotificationBroker;
import org.forgerock.openam.notifications.Subscription;
import org.forgerock.openam.notifications.Topic;

/**
 * A notification broker which performs no operation. This can be used in places where notification publish/subscribe
 * feature is not needed however there is a dependency on broker instance.
 *
 * For example, EntitlementService class requires an instance of NotificationBroker to notify any policySet updates.
 * The required instance is normally provided by Guice. When EntitlementService is used inside the ClientSDK
 * or ssoadm tool, where the policySet notification feature is not supported, this instance can be used
 * to meet the contract of the class EntitlementService.
 *
 * @since 14.0.0
 */
public final class NullNotificationBroker implements NotificationBroker {

    @Override
    public boolean publish(Topic topic, JsonValue notification) {
        return true;
    }

    @Override
    public Subscription subscribe(Consumer consumer) {
        throw new UnsupportedOperationException("Cannot be subscribed to a NullNotificationBroker");
    }

    @Override
    public void shutdown() {

    }

}
