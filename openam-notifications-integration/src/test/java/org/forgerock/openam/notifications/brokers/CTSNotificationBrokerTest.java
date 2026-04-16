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

package org.forgerock.openam.notifications.brokers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.utils.JsonValueBuilder.toJsonArray;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.InflaterInputStream;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.audit.context.AMExecutorServiceFactory;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.continuous.ChangeType;
import org.forgerock.openam.cts.continuous.ContinuousQueryListener;
import org.forgerock.openam.notifications.Consumer;
import org.forgerock.openam.notifications.NotificationBroker;
import org.forgerock.openam.notifications.Subscription;
import org.forgerock.openam.notifications.Topic;
import org.forgerock.openam.notifications.integration.brokers.CTSNotificationBroker;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.util.thread.ExecutorServiceFactory;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit test for {@link CTSNotificationBroker}.
 *
 * @since 14.0.0
 */
public final class CTSNotificationBrokerTest {

    private CTSNotificationBroker broker;

    @Mock
    private CTSPersistentStore store;
    @Mock
    private NotificationBroker localBroker;
    @Mock
    private AMExecutorServiceFactory executorServiceFactory;
    @Mock
    private ScheduledExecutorService executorService;

    @Captor
    private ArgumentCaptor<Runnable> publisherTaskCaptor;
    @Captor
    private ArgumentCaptor<Token> tokenCaptor;
    @Captor
    private ArgumentCaptor<ContinuousQueryListener<Attribute>> listenerCaptor;
    @Captor
    private ArgumentCaptor<JsonValue> jsonValueCaptor;
    @Captor
    private ArgumentCaptor<TokenFilter> filterCaptor;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(executorServiceFactory.createScheduledService(anyInt(), anyString())).thenReturn(executorService);
        broker = new CTSNotificationBroker(store, localBroker, 2, 600L, 100L, executorServiceFactory);
    }

    @Test
    public void whenConstructedBrokerSetsUpContinuousQuery() throws Exception {
        verify(store).addContinuousQueryListener(any(ContinuousQueryListener.class), any(TokenFilter.class));
    }

    @Test
    public void whenConstructedBrokerSetsUpPublishTask() throws Exception {
        verify(executorService).scheduleAtFixedRate(any(Runnable.class), eq(100L), eq(100L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    public void whenPublishingNotificationIsAddedToQueue() throws Exception {
        // When
        JsonValue notification = json(object(field("some-field", "some-value")));
        boolean result = broker.publish(Topic.of("test-topic"), notification);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    public void whenQueueIsFullThenPublishReturnsFalse() throws Exception {
        // When
        JsonValue notification = json(object(field("some-field", "some-value")));
        broker.publish(Topic.of("test-topic"), notification);
        broker.publish(Topic.of("test-topic"), notification);
        boolean result = broker.publish(Topic.of("test-topic"), notification);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    public void whenQueueIsEmptyPublisherDoesNothing() throws Exception {
        Runnable publisher = getPublisherTask();
        publisher.run();
        verify(store, never()).createAsync(any(Token.class));
    }

    @Test
    public void whenPublisherRunsNotificationTokenGetsPersistedToCTS() throws Exception {
        Runnable publisher = getPublisherTask();

        // When
        JsonValue notification = json(object(field("some-field", "some-value")));
        broker.publish(Topic.of("test-topic"), notification);
        publisher.run();

        // Then
        verify(store).createAsync(tokenCaptor.capture());

        Token token = tokenCaptor.getValue();
        InputStream stream = new InflaterInputStream(new ByteArrayInputStream(token.getBlob()));
        JsonValue entries = toJsonArray(stream);

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).isEqualTo(json(object(field("topic", "test-topic"),
                field("content", notification.getObject())))));
    }

    @Test
    public void whenPublisherRunsItPublishesAllNotificationsToCTS() throws Exception {
        Runnable publisher = getPublisherTask();

        // When
        JsonValue notification = json(object(field("some-field", "some-value")));
        broker.publish(Topic.of("test-topic"), notification);
        broker.publish(Topic.of("test-topic"), notification);
        publisher.run();

        // Then
        verify(store).createAsync(tokenCaptor.capture());

        Token token = tokenCaptor.getValue();
        InputStream stream = new InflaterInputStream(new ByteArrayInputStream(token.getBlob()));
        JsonValue entries = toJsonArray(stream);

        assertThat(entries).hasSize(2);
    }

    @Test
    public void whenPublishingNotificationTokenGetsCorrectExpiryTime() throws Exception {
        Runnable publisher = getPublisherTask();

        // When
        JsonValue notification = json(object(field("some-field", "some-value")));
        broker.publish(Topic.of("test-topic"), notification);
        publisher.run();

        // Then
        verify(store).createAsync(tokenCaptor.capture());

        Token token = tokenCaptor.getValue();
        assertThat(token.getExpiryTimestamp().getTimeInMillis())
                .isEqualTo(MockTimeService.NOW + TimeUnit.SECONDS.toMillis(600));
    }

    @Test
    public void whenObjectAddedToCTSBrokerDispatchesNotification() throws Exception {
        // Given
        Runnable publisher = getPublisherTask();
        JsonValue notification = json(object(field("some-field", "some-value")));
        broker.publish(Topic.of("test-topic"), notification);
        publisher.run();
        verify(store).createAsync(tokenCaptor.capture());
        Token token = tokenCaptor.getValue();

        ContinuousQueryListener<Attribute> listener = getContinuousQueryListener();
        Attribute attribute = mock(Attribute.class);
        given(attribute.firstValue())
                .willReturn(ByteString.valueOfBytes(token.getBlob()));

        // When
        listener.objectChanged("1234", Collections.singletonMap(CoreTokenField.BLOB.toString(), attribute),
                ChangeType.ADD);

        // Then
        verify(localBroker).publish(eq(Topic.of("test-topic")), jsonValueCaptor.capture());
        assertThat(jsonValueCaptor.getValue().isEqualTo(notification));
    }

    @Test
    public void whenObjectChangedInCTSBrokerNoActionOccurs() throws Exception {
        // Given
        ContinuousQueryListener<Attribute> listener = getContinuousQueryListener();
        Attribute attribute = mock(Attribute.class);

        // When
        listener.objectChanged("1234", Collections.singletonMap(CoreTokenField.BLOB.toString(), attribute),
                ChangeType.MODIFY);

        // Then
        verify(localBroker, never()).publish(any(Topic.class), any(JsonValue.class));
    }

    @Test
    public void whenObjectDeletedFromCTSBrokerNoActionOccurs() throws Exception {
        // Given
        ContinuousQueryListener<Attribute> listener = getContinuousQueryListener();
        Attribute attribute = mock(Attribute.class);

        // When
        listener.objectChanged("1234", Collections.singletonMap(CoreTokenField.BLOB.toString(), attribute),
                ChangeType.DELETE);

        // Then
        verify(localBroker, never()).publish(any(Topic.class), any(JsonValue.class));
    }

    @Test
    public void whenExceptionIsThrownInCTSListenerTheExceptionIsHandled() throws Exception {
        // Given
        ContinuousQueryListener<Attribute> listener = getContinuousQueryListener();
        Attribute attribute = mock(Attribute.class);
        given(attribute.firstValue()).willReturn(ByteString.valueOfUtf8("invalid-data"));

        // When
        listener.objectChanged("1234", Collections.singletonMap(CoreTokenField.BLOB.toString(), attribute),
                ChangeType.ADD);

        // Then
        verify(localBroker, never()).publish(any(Topic.class), any(JsonValue.class));
    }

    @Test
    public void whenSubscribingBrokerCallsThroughToLocalBroker() throws Exception {
        // Given
        Consumer consumer = mock(Consumer.class);
        Subscription subscription = mock(Subscription.class);
        given(localBroker.subscribe(any(Consumer.class))).willReturn(subscription);

        // When
        Subscription result = broker.subscribe(consumer);

        // Then
        verify(localBroker).subscribe(consumer);
        assertThat(result).isEqualTo(subscription);
    }

    @Test
    public void whenShutdownBrokerShutsDownLocalBroker() throws Exception {
        // When
        broker.shutdown();

        // Then
        verify(localBroker).shutdown();
    }

    @Test
    public void whenShutdownBrokerRemovesContinuousQueryListener() throws Exception {
        // Given
        verify(store).addContinuousQueryListener(listenerCaptor.capture(), filterCaptor.capture());

        // When
        broker.shutdown();

        // Then
        verify(store).removeContinuousQueryListener(listenerCaptor.getValue(), filterCaptor.getValue());
    }

    private Runnable getPublisherTask() {
        verify(executorService).scheduleAtFixedRate(publisherTaskCaptor.capture(), anyLong(), anyLong(),
                any(TimeUnit.class));
        return publisherTaskCaptor.getValue();
    }

    private ContinuousQueryListener<Attribute> getContinuousQueryListener() throws Exception {
        verify(store).addContinuousQueryListener(listenerCaptor.capture(), any(TokenFilter.class));
        return listenerCaptor.getValue();
    }
}