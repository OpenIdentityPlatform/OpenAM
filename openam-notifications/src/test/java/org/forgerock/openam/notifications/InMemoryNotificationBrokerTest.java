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

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.concurrent.ExecutorService;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.audit.context.AMExecutorServiceFactory;
import org.forgerock.openam.notifications.brokers.InMemoryNotificationBroker;
import org.forgerock.util.time.TimeService;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit test for {@link InMemoryNotificationBroker}.
 *
 * @since 14.0.0
 */
public final class InMemoryNotificationBrokerTest {

    private static final int CONSUMERS = 3;

    @Mock
    private AMExecutorServiceFactory executorServiceFactory;
    @Mock
    private ExecutorService executorService;
    @Mock
    private Consumer consumer;
    @Mock
    private TimeService timeService;

    @Captor
    private ArgumentCaptor<Runnable> readerCapture;
    @Captor
    private ArgumentCaptor<JsonValue> notificationCapture;

    private NotificationBroker broker;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(executorServiceFactory.createFixedThreadPool(anyInt(), anyString())).thenReturn(executorService);
        broker = new InMemoryNotificationBroker(executorServiceFactory, timeService, 2, CONSUMERS);
    }

    @Test
    public void whenNotificationsArePublishedTheyAreRoutedThroughToSubscribers() {
        // Given
        broker.subscribe(consumer).bindTo(Topic.of("test_topic"));

        // When
        JsonValue notification = json(object(field("tokenId", "123-456")));
        broker.publish(Topic.of("test_topic"), notification);

        // Then
        verify(executorService, times(CONSUMERS)).submit(readerCapture.capture());
        Runnable reader = readerCapture.getValue();

        broker.shutdown();
        reader.run();

        verify(consumer).accept(notificationCapture.capture());

        assertThat(notificationCapture.getValue().get(new JsonPointer("body/tokenId")).asString()).isEqualTo("123-456");
    }

    @Test
    public void whenSubscriberThrowsExceptionBrokerCatchesAndContinues() {
        // Given
        Consumer badConsumer = mock(Consumer.class);
        doThrow(RuntimeException.class).when(badConsumer).accept(any(JsonValue.class));

        broker.subscribe(badConsumer).bindTo(Topic.of("test_topic"));
        broker.subscribe(consumer).bindTo(Topic.of("test_topic"));

        // When
        JsonValue notification = json(object(field("tokenId", "123-456")));
        broker.publish(Topic.of("test_topic"), notification);

        // Then
        verify(executorService, times(CONSUMERS)).submit(readerCapture.capture());
        Runnable reader = readerCapture.getValue();

        broker.shutdown();
        reader.run();

        verify(consumer).accept(any(JsonValue.class));
    }

    @Test
    public void whenNotificationIsPublishedTimestampIsAdded() {
        // Given
        broker.subscribe(consumer).bindTo(Topic.of("test_topic"));
        given(timeService.now()).willReturn(1451649600000L);

        // When
        JsonValue notification = json(object(field("tokenId", "123-456")));
        broker.publish(Topic.of("test_topic"), notification);

        // Then
        verify(executorService, times(CONSUMERS)).submit(readerCapture.capture());
        Runnable reader = readerCapture.getValue();

        broker.shutdown();
        reader.run();

        verify(consumer).accept(notificationCapture.capture());

        assertThat(notificationCapture.getValue().isDefined("timestamp")).isTrue();
        assertThat(notificationCapture.getValue().get("timestamp").asString()).isEqualTo("2016-01-01T12:00:00.000Z");
    }

    @Test
    public void whenNotificationIsPublishedTopicIsAdded() {
        // Given
        broker.subscribe(consumer).bindTo(Topic.of("test_topic"));

        // When
        JsonValue notification = json(object(field("tokenId", "123-456")));
        broker.publish(Topic.of("test_topic"), notification);

        // Then
        verify(executorService, times(CONSUMERS)).submit(readerCapture.capture());
        Runnable reader = readerCapture.getValue();

        broker.shutdown();
        reader.run();

        verify(consumer).accept(notificationCapture.capture());

        assertThat(notificationCapture.getValue().isDefined("topic")).isTrue();
        assertThat(notificationCapture.getValue().get("topic").asString()).isEqualTo("test_topic");
    }

    @Test
    public void whenQueueIsNotFullPublishReturnsTrue() {
        // When
        JsonValue notification = json(object(field("tokenId", "123-456")));
        boolean result = broker.publish(Topic.of("test_topic"), notification);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    public void whenQueueIsFullPublishReturnsFalse() {
        // When
        JsonValue notification = json(object(field("tokenId", "123-456")));
        broker.publish(Topic.of("test_topic"), notification);
        broker.publish(Topic.of("test_topic"), notification);
        boolean result = broker.publish(Topic.of("test_topic"), notification);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    public void whenBrokerIsShuttingDownPublishReturnsFalseAndDoesntPublishAMessage() {
        // Given
        broker.subscribe(consumer).bindTo(Topic.of("test_topic"));
        broker.shutdown();

        // When
        JsonValue notification = json(object(field("tokenId", "123-456")));
        boolean result = broker.publish(Topic.of("test_topic"), notification);

        // Then
        assertThat(result).isFalse();
        verify(consumer, never()).accept(any(JsonValue.class));
    }

    @Test
    public void whenUnbindingFromTopicNoLongerDeliversNotificationsOnThatTopic() {
        // Given
        broker.subscribe(consumer)
                .bindTo(Topic.of("test_topic"))
                .unbindFrom(Topic.of("test_topic"));

        // When
        JsonValue notification = json(object(field("tokenId", "123-456")));
        broker.publish(Topic.of("test_topic"), notification);

        // Then
        verify(executorService, times(CONSUMERS)).submit(readerCapture.capture());
        Runnable reader = readerCapture.getValue();

        broker.shutdown();
        reader.run();

        verify(consumer, never()).accept(any(JsonValue.class));

    }

    @Test
    public void whenClosingSubscriptionBrokerNoLongerDeliversNotifications() throws Exception {
        // Given
        broker.subscribe(consumer)
                .bindTo(Topic.of("test_topic"))
                .close();

        // When
        JsonValue notification = json(object(field("tokenId", "123-456")));
        broker.publish(Topic.of("test_topic"), notification);

        // Then
        verify(executorService, times(CONSUMERS)).submit(readerCapture.capture());
        Runnable reader = readerCapture.getValue();

        broker.shutdown();
        reader.run();

        verify(consumer, never()).accept(any(JsonValue.class));
    }

    @Test
    public void whenUnbindingFromOneTopicBrokerStillDeliversToAnother() {
        // Given
        broker.subscribe(consumer)
                .bindTo(Topic.of("test_topic"))
                .bindTo(Topic.of("another_test_topic"))
                .unbindFrom(Topic.of("test_topic"));

        // When
        JsonValue notification = json(object(field("tokenId", "123-456")));
        broker.publish(Topic.of("test_topic"), notification);
        JsonValue anotherNotification = json(object(field("tokenId", "123-456")));
        broker.publish(Topic.of("another_test_topic"), anotherNotification);

        // Then
        verify(executorService, times(CONSUMERS)).submit(readerCapture.capture());
        Runnable reader = readerCapture.getValue();

        broker.shutdown();
        reader.run();

        verify(consumer).accept(notificationCapture.capture());

        assertThat(notificationCapture.getValue().isDefined("topic")).isTrue();
        assertThat(notificationCapture.getValue().get("topic").asString()).isEqualTo("another_test_topic");
    }

    @Test
    public void whenBindingToOneTopicOnlyReceiveNotificationsForThatTopic() {
        // Given
        broker.subscribe(consumer)
                .bindTo(Topic.of("test_topic1"));

        // When
        JsonValue notification = json(object(field("tokenId", "123-456")));
        broker.publish(Topic.of("test_topic2"), notification);

        // Then
        verify(executorService, times(CONSUMERS)).submit(readerCapture.capture());
        Runnable reader = readerCapture.getValue();

        broker.shutdown();
        reader.run();

        verify(consumer, never()).accept(any(JsonValue.class));
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void whenSubscriptionIsClosedFurtherBindingThrowAnException() throws Exception {
        // Given
        Subscription subscription = broker.subscribe(consumer)
                .bindTo(Topic.of("test_topic"));

        // When
        subscription.close();
        subscription.bindTo(Topic.of("test_topic"));
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void whenSubscriptionIsClosedFurtherUnbindingThrowAnException() throws Exception {
        // Given
        Subscription subscription = broker.subscribe(consumer)
                .bindTo(Topic.of("test_topic"));

        // When
        subscription.close();
        subscription.unbindFrom(Topic.of("test_topic"));
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void whenSubscriptionIsClosedBindQueriesThrowAnException() throws Exception {
        // Given
        Subscription subscription = broker.subscribe(consumer)
                .bindTo(Topic.of("test_topic"));

        // When
        subscription.close();
        subscription.isBoundTo(Topic.of("test_topic"));
    }

    @Test
    public void whenShuttingDownShutsDownExecutorService() throws Exception {
        broker.shutdown();

        verify(executorService).shutdownNow();
    }
}