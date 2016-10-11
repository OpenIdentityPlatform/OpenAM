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

package org.forgerock.openam.notifications.integration.brokers;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.utils.JsonValueBuilder.toJsonValue;
import static org.forgerock.openam.utils.Time.currentTimeMillis;
import static org.forgerock.openam.utils.TimeUtils.fromUnixTime;
import static org.forgerock.util.query.QueryFilter.equalTo;

import java.util.Calendar;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.json.JsonException;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.filter.TokenFilterBuilder;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.continuous.ChangeType;
import org.forgerock.openam.cts.continuous.ContinuousQueryListener;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.notifications.Consumer;
import org.forgerock.openam.notifications.NotificationBroker;
import org.forgerock.openam.notifications.Subscription;
import org.forgerock.openam.notifications.Topic;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.tokens.TokenType;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.util.generator.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Uses the CTS to propagate notifications across an OpenAM cluster.
 * <p>
 * When publishing, notifications are written to the CTS. A continuous
 * query listener {@link ContinuousQueryListener} responds to new notifications
 * in the CTS and passes them on to interested subscribers.
 * <p>
 * This implementation uses a local-server broker to handle the brokerage
 * of messages that come in from the CTS.
 *
 * @since 14.0.0
 */
public final class CTSNotificationBroker implements NotificationBroker {

    private static final Logger logger = LoggerFactory.getLogger(CTSNotificationBroker.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final NotificationBroker localBroker;
    private final CTSPersistentStore store;
    private final SessionNotificationListener listener;
    private final long tokenExpirySeconds;
    private final IdGenerator idGenerator;

    /**
     * Constructs a new broker.
     *
     * @param store a CTS persistent store that notifications will be written to and read from
     * @param localBroker a local-server broker used to propagate messages to local subscribers
     * @param tokenExpirySeconds the number of seconds that a notification will live in the CTS before it is deleted
     */
    @Inject
    public CTSNotificationBroker(CTSPersistentStore store,
            @Named("localBroker") NotificationBroker localBroker,
            @Named("tokenExpirySeconds") long tokenExpirySeconds) {

        this.localBroker = localBroker;
        this.store = store;
        this.tokenExpirySeconds = tokenExpirySeconds;
        idGenerator = IdGenerator.DEFAULT;
        listener = new SessionNotificationListener();

        try {
            store.addContinuousQueryListener(listener, getTokenFilter());
        } catch (CoreTokenException ctE) {
            throw new RuntimeException("Unable to register session notifications", ctE);
        }
    }

    @Override
    public boolean publish(Topic topic, JsonValue notification) {
        JsonValue entry = json(object(
                field("topic", topic.getIdentifier()),
                field("content", notification.getObject())));

        try {
            Token token = new Token(idGenerator.generate(), TokenType.NOTIFICATION);
            token.setBlob(mapper.writeValueAsBytes(entry.getObject()));

            long expiryTime = currentTimeMillis() + TimeUnit.SECONDS.toMillis(tokenExpirySeconds);
            Calendar expiryTimeStamp = fromUnixTime(expiryTime, TimeUnit.MILLISECONDS);
            token.setExpiryTimestamp(expiryTimeStamp);

            store.createAsync(token);
        } catch (CoreTokenException ctE) {
            logger.error("Failed to write notification to CTS", ctE);
            return false;
        } catch (JsonProcessingException jpE) {
            throw new JsonException(jpE);
        }

        return true;
    }

    @Override
    public Subscription subscribe(Consumer consumer) {
        return localBroker.subscribe(consumer);
    }

    @Override
    public void shutdown() {
        localBroker.shutdown();

        try {
            store.removeContinuousQueryListener(listener, getTokenFilter());
        } catch (CoreTokenException ctE) {
            logger.warn("Failed to remove continuous query listener", ctE);
        }
    }

    private static TokenFilter getTokenFilter() {
        return new TokenFilterBuilder()
                .returnAttribute(CoreTokenField.BLOB)
                .withQuery(equalTo(CoreTokenField.TOKEN_TYPE, TokenType.NOTIFICATION))
                .build();
    }

    private final class SessionNotificationListener implements ContinuousQueryListener<Attribute> {

        @Override
        public void objectChanged(String tokenId, Map<String, Attribute> changeSet, ChangeType changeType) {
            if (changeType == ChangeType.ADD) {
                try {
                    ByteString entryBlob = changeSet.get(CoreTokenField.BLOB.toString()).firstValue();
                    JsonValue entry = toJsonValue(entryBlob.toByteArray());

                    String topic = entry.get("topic").asString();
                    JsonValue content = entry.get("content");

                    localBroker.publish(Topic.of(topic), content);
                } catch (Exception e) {
                    logger.error("Failed to publish notification to the local broker", e);
                }
            }
        }

        @Override
        public void objectsChanged(Set<String> tokenIds) {
        }

        @Override
        public void connectionLost() {
            logger.error("Continuous query listener has lost its connection");
        }

        @Override
        public void processError(DataLayerException dlE) {
            logger.error("Notification token listener error", dlE);
        }
    }

}
