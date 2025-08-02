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
 * Portions copyright 2025 3A Systems LLC.
 */

package com.iplanet.dpro.session.service;

import static org.forgerock.json.JsonValue.*;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.notifications.NotificationBroker;
import org.forgerock.openam.notifications.NotificationsConfig;
import org.forgerock.openam.notifications.Topic;
import org.forgerock.openam.session.SessionEventType;

import com.iplanet.dpro.session.SessionID;
import com.sun.identity.shared.debug.Debug;

/**
 * Responsible for publishing session notifications over {@link NotificationBroker}.
 */
@Singleton
public class SessionNotificationPublisher implements InternalSessionListener {

    private final Debug sessionDebug;
    private final NotificationsConfig notificationsConfig;
    private final NotificationBroker broker;

    @Inject
    public SessionNotificationPublisher(
            final @Named(SessionConstants.SESSION_DEBUG) Debug sessionDebug,
            final NotificationsConfig notificationsConfig,
            final NotificationBroker broker) {
        this.sessionDebug = sessionDebug;
        this.notificationsConfig = notificationsConfig;
        this.broker = broker;
    }

    @Override
    public void onEvent(final InternalSessionEvent event) {
        switch (event.getType()) {
            case SESSION_CREATION:
            case IDLE_TIMEOUT:
            case MAX_TIMEOUT:
            case LOGOUT:
            case DESTROY:
            case PROPERTY_CHANGED:
                sendEvent(event);
                break;
            default:
                // ignore all other types of event
        }
    }

    private void sendEvent(final InternalSessionEvent event) {
        sessionDebug.message("Publishing session notification, type = {}", event.getType().toString());

        if (notificationsConfig.isAgentsEnabled()) {
            publishSessionNotification(event.getInternalSession().getSessionID(), event.getType());
            for (SessionID sessionId : event.getInternalSession().getRestrictedTokens()) {
                publishSessionNotification(sessionId, event.getType());
            }
        }
    }

    private void publishSessionNotification(SessionID sessionId, SessionEventType sessionEventType) {
        JsonValue notification = json(object(
                field("tokenId", sessionId.toString()),
                field("eventType", sessionEventType)));
        broker.publish(Topic.of("/agent/session"), notification);
    }

}
