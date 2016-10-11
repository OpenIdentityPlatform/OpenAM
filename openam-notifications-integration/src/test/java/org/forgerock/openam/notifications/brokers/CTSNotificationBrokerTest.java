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
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.utils.JsonValueBuilder.toJsonValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.continuous.ChangeType;
import org.forgerock.openam.cts.continuous.ContinuousQueryListener;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.notifications.Consumer;
import org.forgerock.openam.notifications.NotificationBroker;
import org.forgerock.openam.notifications.Subscription;
import org.forgerock.openam.notifications.Topic;
import org.forgerock.openam.notifications.integration.brokers.CTSNotificationBroker;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.ByteString;
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
        broker = new CTSNotificationBroker(store, localBroker, 600L);
    }

    @Test
    public void whenConstructedBrokerSetsUpContinuousQuery() throws Exception {
        verify(store).addContinuousQueryListener(any(ContinuousQueryListener.class), any(TokenFilter.class));
    }

    @Test
    public void whenPublishingNotificationTokenGetsPersistedToCTS() throws Exception {
        // When
        JsonValue notification = json(object(field("some-field", "some-value")));
        boolean result = broker.publish(Topic.of("test-topic"), notification);

        // Then
        assertThat(result).isTrue();
        verify(store).createAsync(tokenCaptor.capture());

        Token token = tokenCaptor.getValue();
        JsonValue entry = toJsonValue(token.getBlob());

        assertThat(entry.isEqualTo(json(object(field("topic", "test-topic"),
                field("content", notification.getObject())))));
    }

    @Test
    public void whenPublishingNotificationTokenGetsCorrectExpiryTime() throws Exception {
        // When
        JsonValue notification = json(object(field("some-field", "some-value")));
        boolean result = broker.publish(Topic.of("test-topic"), notification);

        // Then
        assertThat(result).isTrue();
        verify(store).createAsync(tokenCaptor.capture());

        Token token = tokenCaptor.getValue();
        assertThat(token.getExpiryTimestamp().getTimeInMillis()).isEqualTo(MockTimeService.NOW + TimeUnit.SECONDS.toMillis(600));
    }

    @Test
    public void whenObjectAddedToCTSBrokerDispatchesNotification() throws Exception {
        // Given
        String notificationData = "{\"topic\":\"test-topic\",\"content\":{\"some-field\":\"some-value\"}}";
        JsonValue notification = json(object(field("some-field", "some-value")));
        verify(store).addContinuousQueryListener(listenerCaptor.capture(), any(TokenFilter.class));
        ContinuousQueryListener<Attribute> listener = listenerCaptor.getValue();
        Attribute attribute = mock(Attribute.class);
        given(attribute.firstValue()).willReturn(ByteString.valueOfUtf8(notificationData));

        // When
        listener.objectChanged("1234", Collections.singletonMap(CoreTokenField.BLOB.toString(), attribute), ChangeType.ADD);

        // Then
        verify(localBroker).publish(eq(Topic.of("test-topic")), jsonValueCaptor.capture());
        assertThat(jsonValueCaptor.getValue().isEqualTo(notification));
    }

    @Test
    public void whenObjectChangedInCTSBrokerNoActionOccurs() throws CoreTokenException {
        // Given
        String notificationData = "{\"topic\":\"test-topic\",\"content\":{\"some-field\":\"some-value\"}}";
        verify(store).addContinuousQueryListener(listenerCaptor.capture(), any(TokenFilter.class));
        ContinuousQueryListener<Attribute> listener = listenerCaptor.getValue();
        Attribute attribute = mock(Attribute.class);
        given(attribute.firstValue()).willReturn(ByteString.valueOfUtf8(notificationData));

        // When
        listener.objectChanged("1234", Collections.singletonMap(CoreTokenField.BLOB.toString(), attribute), ChangeType.MODIFY);

        // Then
        verify(localBroker, never()).publish(any(Topic.class), any(JsonValue.class));
    }

    @Test
    public void whenObjectDeletedFromCTSBrokerNoActionOccurs() throws CoreTokenException {
        // Given
        String notificationData = "{\"topic\":\"test-topic\",\"content\":{\"some-field\":\"some-value\"}}";
        verify(store).addContinuousQueryListener(listenerCaptor.capture(), any(TokenFilter.class));
        ContinuousQueryListener<Attribute> listener = listenerCaptor.getValue();
        Attribute attribute = mock(Attribute.class);
        given(attribute.firstValue()).willReturn(ByteString.valueOfUtf8(notificationData));

        // When
        listener.objectChanged("1234", Collections.singletonMap(CoreTokenField.BLOB.toString(), attribute), ChangeType.DELETE);

        // Then
        verify(localBroker, never()).publish(any(Topic.class), any(JsonValue.class));
    }

    @Test
    public void whenExceptionIsThrownInCTSListenerTheExceptionIsHandled() throws CoreTokenException {
        // Given
        String notificationData = "{\"topic\":123,\"content\":{\"some-field\":\"some-value\"}}";
        verify(store).addContinuousQueryListener(listenerCaptor.capture(), any(TokenFilter.class));
        ContinuousQueryListener<Attribute> listener = listenerCaptor.getValue();
        Attribute attribute = mock(Attribute.class);
        given(attribute.firstValue()).willReturn(ByteString.valueOfUtf8(notificationData));

        // When
        listener.objectChanged("1234", Collections.singletonMap(CoreTokenField.BLOB.toString(), attribute), ChangeType.ADD);

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

}