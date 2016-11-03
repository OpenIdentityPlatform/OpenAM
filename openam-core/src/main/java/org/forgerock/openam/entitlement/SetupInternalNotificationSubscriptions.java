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

package org.forgerock.openam.entitlement;

import static org.forgerock.openam.entitlement.PolicyConstants.SUPER_ADMIN_SUBJECT;
import static org.forgerock.openam.entitlement.utils.EntitlementUtils.getApplicationService;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.notifications.Consumer;
import org.forgerock.openam.notifications.NotificationBroker;
import org.forgerock.openam.notifications.Topic;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.PolicyEventType;
import com.sun.identity.entitlement.PrivilegeIndexStore;
import com.sun.identity.entitlement.opensso.DataStore;
import com.sun.identity.setup.SetupListener;

/**
 * Responsible for subscribing to internal policy notifications.
 *
 * @since 14.0.0
 */
public final class SetupInternalNotificationSubscriptions implements SetupListener {

    public static final Topic TOPIC_INTERNAL_POLICY = Topic.of("/internal/policy");
    public static final Topic TOPIC_INTERNAL_POLICYSET = Topic.of("/internal/policySet");

    public static final String MESSAGE_ATTR_EVENT_TYPE = "eventType";
    public static final String MESSAGE_ATTR_REALM = "realm";
    public static final String MESSAGE_ATTR_NAME = "name";

    @Override
    public void setupComplete() {
        NotificationBroker broker = InjectorHolder.getInstance(NotificationBroker.class);

        setUpPolicySubscriptions(broker);
        setUpPolicySetSubscriptions(broker);
    }

    private void setUpPolicySubscriptions(NotificationBroker broker) {
        broker.subscribe(new PolicyNotificationConsumer()).bindTo(TOPIC_INTERNAL_POLICY);
    }

    private void setUpPolicySetSubscriptions(NotificationBroker broker) {
        broker.subscribe(new PolicySetNotificationConsumer()).bindTo(TOPIC_INTERNAL_POLICYSET);
    }

    private static class PolicyNotificationConsumer implements Consumer {
        @Override
        public void accept(JsonValue notification) {
            JsonValue eventType = notification.get(MESSAGE_ATTR_EVENT_TYPE);
            JsonValue realm = notification.get(MESSAGE_ATTR_REALM);

            if (eventType.isNull() || realm.isNull()) {
                PolicyConstants.DEBUG.warning("One or more required fields {}, {} are missing. " +
                        "Discarding the Policy notification {}",
                        MESSAGE_ATTR_EVENT_TYPE, MESSAGE_ATTR_REALM, notification);
                return;
            }

            PolicyEventType type = PolicyEventType.valueOf(eventType.asString());
            switch (type) {
            case CREATE:
                handlePolicyCreation(realm);
                break;
            case DELETE:
                handlePolicyDeletion(notification, realm);
                break;
            }
        }

        private void handlePolicyCreation(JsonValue realm) {
            DataStore.getInstance().clearIndexCount(realm.asString(), false);
        }

        private void handlePolicyDeletion(JsonValue notification, JsonValue realm) {
            JsonValue policyName = notification.get(MESSAGE_ATTR_NAME);

            if (policyName.isNull()) {
                PolicyConstants.DEBUG.warning("Required field {} is missing. " +
                        "Discarding the Policy notification {}", MESSAGE_ATTR_NAME, notification);
                return;
            }

            PrivilegeIndexStore pis = PrivilegeIndexStore.getInstance(
                    PolicyConstants.SUPER_ADMIN_SUBJECT, realm.asString());
            try {
                pis.delete(policyName.asString(), false);
            } catch (EntitlementException e) {
                //ignore
            }
            // Get an instance as required otherwise it can cause issues on container restart.
            DataStore.getInstance().clearIndexCount(realm.asString(), false);
        }
    }

    private class PolicySetNotificationConsumer implements Consumer {
        @Override
        public void accept(JsonValue notification) {
            JsonValue eventType = notification.get(MESSAGE_ATTR_EVENT_TYPE);
            JsonValue realm = notification.get(MESSAGE_ATTR_REALM);

            if (eventType.isNull() || realm.isNull()) {
                PolicyConstants.DEBUG.warning("One or more required fields {}, {} are missing. " +
                        "Discarding the PolicySet notification {}",
                        MESSAGE_ATTR_EVENT_TYPE, MESSAGE_ATTR_REALM, notification);
                return;
            }

            PolicyEventType type = PolicyEventType.valueOf(eventType.asString());
            switch (type) {
            case UPDATE:
                handlePolicySetUpdate(realm);
                break;
            }
        }

        private void handlePolicySetUpdate(JsonValue realm) {
            getApplicationService(SUPER_ADMIN_SUBJECT, realm.asString()).clearCache();
        }
    }
}
