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

package org.forgerock.openam.notifications.websocket;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.io.IOException;

import javax.inject.Inject;
import javax.websocket.EncodeException;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.notifications.Consumer;
import org.forgerock.openam.notifications.NotificationBroker;
import org.forgerock.openam.notifications.Subscription;
import org.forgerock.openam.notifications.Topic;
import org.forgerock.util.Reject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WebSocket notification endpoint. Provides the glue between the WebSocket and the {@link NotificationBroker}.
 * <p>
 * Currently only supports the parsing of subscription messages. The expected format is:
 * <pre>
 *     {
 *         "id": "some-id-here", # this is optional
 *         "action": "subscribe",
 *         "topic": "/some/topic/string"
 *     }
 * </pre>
 * <p>
 * When the <tt>id</tt> is present in the request, it shall be echoed back with any response messages.
 *
 * @since 14.0.0
 */
@ServerEndpoint(value = "/notifications",
        encoders = JsonValueEncoder.class, decoders = JsonValueDecoder.class,
        configurator = NotificationsWebSocketConfigurator.class,
        subprotocols = "v1.notifications.forgerock.org")
public final class NotificationsWebSocket {

    private static final Logger logger = LoggerFactory.getLogger(NotificationsWebSocket.class);

    private final NotificationBroker broker;
    private Subscription subscription;

    /**
     * No args constructor as required by JSR-356. Instances created
     * with this constructor are not expected to be used.
     */
    public NotificationsWebSocket() {
        broker = null;
    }

    /**
     * Constructs a new notification websocket endpoint.
     *
     * @param broker the notification broker
     */
    @Inject
    public NotificationsWebSocket(NotificationBroker broker) {
        Reject.ifNull(broker, "Broker must not be null");
        this.broker = broker;
    }

    /**
     * When a new notification websocket is opened, creates a new subscription.
     *
     * @param session the websocket session
     */
    @OnOpen
    public void open(Session session) {
        Reject.ifNull(session, "Session must not be null");
        subscription = broker.subscribe(new WebSocketConsumer(session));
    }

    /**
     * When the notification websocket is closed, the subscription is closed.
     */
    @OnClose
    public void close() {
        subscription.close();
    }

    /**
     * Handles the processing of messages sent down the notification websocket.
     *
     * @param session the websocket session
     * @param json the json message
     */
    @OnMessage
    public void message(Session session, JsonValue json) {
        Reject.ifNull(session, "Session must not be null");
        Reject.ifNull(json, "Json must not be null");

        if (json.isDefined("id") && !json.get("id").isString()) {
            sendError(session, null, "\"id\" must be a string");
            return;
        }

        String id = json.get("id").asString();

        if (!json.isDefined("action")) {
            sendError(session, id, "missing required field \"action\"");
            return;
        }
        if (!json.get("action").isString()) {
            sendError(session, id, "\"action\" must be a string");
            return;
        }

        String action = json.get("action").asString();

        if (!action.equals("subscribe")) {
            sendError(session, id, "unknown action \"" + action + "\"");
            return;
        }
        if (!json.isDefined("topic")) {
            sendError(session, id, "missing required field \"topic\"");
            return;
        }
        if (!json.get("topic").isString()) {
            sendError(session, id, "\"topic\" must be a string");
            return;
        }

        String topic = json.get("topic").asString();

        subscription.bindTo(Topic.of(topic));
        sendMessage(session, id, "subscription registered");
    }

    private void sendMessage(Session session, String id, String message) {
        try {
            JsonValue json = json(object(field("message", message)));

            if (id != null) {
                json.put("id", id);
            }

            session.getBasicRemote().sendObject(json);
        } catch (IOException | EncodeException e) {
            logger.error("Unable to send message to client Message was \"" + message + "\"", e);
        }
    }

    private void sendError(Session session, String id, String message) {
        try {
            JsonValue json = json(object(field("error", message)));

            if (id != null) {
                json.put("id", id);
            }

            session.getBasicRemote().sendObject(json);
        } catch (IOException | EncodeException e) {
            logger.error("Unable to send error to client. Error was \"" + message + "\"", e);
        }
    }

    private static final class WebSocketConsumer implements Consumer {

        private final Session session;

        private WebSocketConsumer(Session session) {
            this.session = session;
        }

        @Override
        public void accept(JsonValue notification) {
            Reject.ifNull(notification);

            try {
                session.getBasicRemote().sendObject(notification);
            } catch (IOException | EncodeException e) {
                logger.error("Unable to send notification", e);
            }
        }

    }

}
