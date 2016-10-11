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

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.notifications.Consumer;
import org.forgerock.openam.notifications.NotificationBroker;
import org.forgerock.openam.notifications.Subscription;
import org.forgerock.openam.notifications.Topic;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit test for {@link NotificationsWebSocket}.
 *
 * @since 14.0.0
 */
public final class NotificationsWebSocketTest {

    private NotificationsWebSocket notificationsWebSocket;

    @Mock
    private NotificationBroker broker;
    @Mock
    private Subscription subscription;
    @Mock
    private Session session;
    @Mock
    private RemoteEndpoint.Basic basic;

    @Captor
    private ArgumentCaptor<Consumer> consumerCaptor;
    @Captor
    private ArgumentCaptor<JsonValue> jsonCaptor;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        notificationsWebSocket = new NotificationsWebSocket(broker);
        when(broker.subscribe(any(Consumer.class))).thenReturn(subscription);
        when(session.getBasicRemote()).thenReturn(basic);
    }

    @Test
    public void whenConnectionOpenedANewSubscriptionIsCreated() {
        // When
        notificationsWebSocket.open(session);

        // Then
        verify(broker).subscribe(any(Consumer.class));
    }

    @Test
    public void whenConnectionOpenedNewSubscriptionHasNoBindings() {
        // When
        notificationsWebSocket.open(session);

        verify(subscription, never()).bindTo(any(Topic.class));
    }

    @Test
    public void whenSubscriptionRequestSentThenSubscriptionIsBoundToTopic() {
        // When
        notificationsWebSocket.open(session);
        notificationsWebSocket.message(session, json(object(
                field("action", "subscribe"),
                field("topic", "test_topic")
        )));

        // Then
        verify(subscription).bindTo(Topic.of("test_topic"));
    }

    @Test
    public void whenSubscriptionRequestSentThenReplyIsSent() throws Exception {
        // When
        notificationsWebSocket.open(session);
        notificationsWebSocket.message(session, json(object(
                field("action", "subscribe"),
                field("topic", "test_topic")
        )));

        // Then
        verify(basic).sendObject(jsonCaptor.capture());
        assertThat(jsonCaptor.getValue()).stringAt("message").isEqualTo("subscription registered");
    }

    @Test
    public void whenIdIsPassedInRequestThenIdIsPresentInResponse() throws Exception {
        // When
        notificationsWebSocket.open(session);
        notificationsWebSocket.message(session, json(object(
                field("action", "subscribe"),
                field("topic", "test_topic"),
                field("id", "this_is_an_id")
        )));

        // Then
        verify(basic).sendObject(jsonCaptor.capture());
        assertThat(jsonCaptor.getValue()).stringAt("id").isEqualTo("this_is_an_id");
    }

    @Test
    public void whenNoIdIsPassedInRequestThenNoIdIsPresentInResponse() throws Exception {
        // When
        notificationsWebSocket.open(session);
        notificationsWebSocket.message(session, json(object(
                field("action", "subscribe"),
                field("topic", "test_topic")
        )));

        // Then
        verify(basic).sendObject(jsonCaptor.capture());
        assertThat(jsonCaptor.getValue().isDefined("id")).isFalse();
    }

    @Test
    public void whenActionIsMissingFromRequestSendsErrorMessage() throws Exception {
        // When
        notificationsWebSocket.open(session);
        notificationsWebSocket.message(session, json(object(
                field("topic", "test_topic")
        )));

        // Then
        verify(basic).sendObject(jsonCaptor.capture());
        assertThat(jsonCaptor.getValue()).stringAt("error").isEqualTo("missing required field \"action\"");
    }

    @Test
    public void whenActionIsNotSubscribeTheRequestSendsErrorMessage() throws Exception {
        // When
        notificationsWebSocket.open(session);
        notificationsWebSocket.message(session, json(object(
                field("action", "some_other_action"),
                field("topic", "test_topic")
        )));

        // Then
        verify(basic).sendObject(jsonCaptor.capture());
        assertThat(jsonCaptor.getValue()).stringAt("error").isEqualTo("unknown action \"some_other_action\"");
    }

    @Test
    public void whenActionIsNotAStringInTheRequestSendsErrorMessage() throws Exception {
        // When
        notificationsWebSocket.open(session);
        notificationsWebSocket.message(session, json(object(
                field("action", 123),
                field("topic", "test_topic")
        )));

        // Then
        verify(basic).sendObject(jsonCaptor.capture());
        assertThat(jsonCaptor.getValue()).stringAt("error").isEqualTo("\"action\" must be a string");
    }

    @Test
    public void whenTopicIsMissingFromRequestSendsErrorMessage() throws Exception {
        // When
        notificationsWebSocket.open(session);
        notificationsWebSocket.message(session, json(object(
                field("action", "subscribe")
        )));

        // Then
        verify(basic).sendObject(jsonCaptor.capture());
        assertThat(jsonCaptor.getValue()).stringAt("error").isEqualTo("missing required field \"topic\"");
    }

    @Test
    public void whenTopicIsNotAStringInTheRequestSendsErrorMessage() throws Exception {
        // When
        notificationsWebSocket.open(session);
        notificationsWebSocket.message(session, json(object(
                field("action", "subscribe"),
                field("topic", 123)
        )));

        // Then
        verify(basic).sendObject(jsonCaptor.capture());
        assertThat(jsonCaptor.getValue()).stringAt("error").isEqualTo("\"topic\" must be a string");
    }

    @Test
    public void whenThereIsAnErrorAndIdInRequestItIsIncludedInErrorMessage() throws Exception {
        // When
        notificationsWebSocket.open(session);
        notificationsWebSocket.message(session, json(object(
                field("action", "some_other_action"),
                field("topic", "test_topic"),
                field("id", "this_is_an_id")
        )));

        // Then
        verify(basic).sendObject(jsonCaptor.capture());
        assertThat(jsonCaptor.getValue()).stringAt("id").isEqualTo("this_is_an_id");
        assertThat(jsonCaptor.getValue()).stringAt("error").isEqualTo("unknown action \"some_other_action\"");
    }

    @Test
    public void whenThereIsAnIdThatIsNotAStringRequestSendsErrorMessage() throws Exception {
        // When
        notificationsWebSocket.open(session);
        notificationsWebSocket.message(session, json(object(
                field("action", "some_other_action"),
                field("topic", "test_topic"),
                field("id", 123)
        )));

        // Then
        verify(basic).sendObject(jsonCaptor.capture());
        assertThat(jsonCaptor.getValue()).stringAt("error").isEqualTo("\"id\" must be a string");
    }


    @Test
    public void whenConsumerReceivesNotificationIsPushedToTheWebSocket() throws Exception {
        // Given
        notificationsWebSocket.open(session);
        verify(broker).subscribe(consumerCaptor.capture());

        Consumer consumer = consumerCaptor.getValue();

        // When
        consumer.accept(json(object(field("some_key", "some_value"))));

        // Then
        verify(basic).sendObject(jsonCaptor.capture());
        assertThat(jsonCaptor.getValue()).stringAt("some_key").isEqualTo("some_value");
    }

    @Test
    public void whenTheWebSocketIsClosedTheSubscriptionIsClosed() {
        // Given
        notificationsWebSocket.open(session);

        // When
        notificationsWebSocket.close();

        // Then
        verify(subscription).close();
    }

}