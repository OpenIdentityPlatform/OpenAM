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

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.notifications.Consumer;
import org.forgerock.openam.notifications.NotificationBroker;
import org.forgerock.openam.notifications.Subscription;
import org.forgerock.openam.notifications.Topic;
import org.forgerock.util.Reject;
import org.forgerock.util.time.TimeService;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A notification broker that uses an in-memory shared queue for incoming notifications and
 * a single thread for reading from it. Routing of notifications to subscriptions is done
 * on the reading thread.
 * <p>
 * The queue is a fixed size and therefore publish may block until the queue has space
 * and may eventually fail if there is not space within a given time.
 *
 * @since 14.0.0
 */
public final class InMemoryNotificationBroker implements NotificationBroker {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryNotificationBroker.class);
    private static final DateTimeFormatter TS_FORMATTER = ISODateTimeFormat.dateTime().withZoneUTC();

    private final BlockingQueue<NotificationEntry> queue;
    private final List<InternalSubscription> subscriptions;
    private final TimeService timeService;
    private final Future<?> readerFuture;

    private final long queueTimeoutMilliseconds;
    private volatile boolean shutdown;

    /**
     * Constructs a new InMemoryNotificationBroker.
     *
     * @param executorService an executor service for creating the reading thread
     * @param timeService a time service for adding timestamps to messages
     * @param queueTimeoutMilliseconds the maximum number of milliseconds to wait when attempting to publish a message
     * @param queueSize the number of notifications to buffer in memory
     */
    @Inject
    public InMemoryNotificationBroker(ExecutorService executorService, TimeService timeService,
            @Named("queueTimeoutMilliseconds") long queueTimeoutMilliseconds, @Named("queueSize") int queueSize) {
        Reject.ifNull(executorService, "Executor service must not be null");
        Reject.ifNull(timeService, "Time service must not be null");
        Reject.ifTrue(queueTimeoutMilliseconds < 0, "Queue timeout must be a positive integer");
        Reject.ifTrue(queueSize < 0, "Queue size must be a positive integer");

        this.queueTimeoutMilliseconds = queueTimeoutMilliseconds;
        this.timeService = timeService;

        queue = new ArrayBlockingQueue<>(queueSize);
        subscriptions = new CopyOnWriteArrayList<>();
        readerFuture = executorService.submit(new NotificationReader());
    }

    @Override
    public boolean publish(Topic topic, JsonValue notification) {
        Reject.ifNull(topic, "Topic must not be null");
        Reject.ifNull(notification, "Notification must not be null");

        if (shutdown) {
            logger.info("Not publishing notification as broker shutting down");
            return false;
        }

        try {
            NotificationEntry entry = NotificationEntry.of(topic, packageNotification(topic, notification));

            if (!queue.offer(entry, queueTimeoutMilliseconds, TimeUnit.MILLISECONDS)) {
                logger.warn("Failed to publish notification to queue, notification has been discarded");
                return false;
            }

            return true;
        } catch (InterruptedException iE) {
            logger.warn("Failed to publish notification to queue as thread was interrupted", iE);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private JsonValue packageNotification(Topic topic, JsonValue notification) {
        String timeStamp = TS_FORMATTER.print(new DateTime(timeService.now()));

        return json(object(
                field("topic", topic.getIdentifier()),
                field("timestamp", timeStamp),
                field("body", notification.getObject())));
    }

    @Override
    public Subscription subscribe(Consumer consumer) {
        Reject.ifNull(consumer, "Consumer must not be null");
        InternalSubscription subscription = new InternalSubscription(consumer);
        subscriptions.add(subscription);
        return subscription;
    }

    @Override
    public void shutdown() {
        shutdown = true;
        readerFuture.cancel(true);
    }

    private final class NotificationReader implements Runnable {

        @Override
        public void run() {
            while (!shutdown) {
                try {
                    NotificationEntry entry = queue.poll(100L, TimeUnit.MILLISECONDS);

                    if (entry == null) {
                        continue;
                    }

                    deliver(entry);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    // Informs the broker that the reader is shutting down as
                    // it will no longer be able to serve notifications.
                    shutdown = true;
                    break;
                }
            }

            List<NotificationEntry> remainingEntries = new ArrayList<>();
            queue.drainTo(remainingEntries);

            for (NotificationEntry entry : remainingEntries) {
                deliver(entry);
            }
        }

        private void deliver(NotificationEntry entry) {
            for (InternalSubscription subscription : subscriptions) {
                try {
                    subscription.consumeIfApplicable(entry.topic, entry.notification);
                } catch (RuntimeException ex) {
                    logger.warn("Exception thrown whilst delivering notifications", ex);
                }
            }
        }

    }

    private final class InternalSubscription implements Subscription {

        private final Set<Topic> topics;
        private final Consumer consumer;
        private volatile boolean closed;

        private InternalSubscription(Consumer consumer) {
            this.consumer = consumer;
            topics = new CopyOnWriteArraySet<>();
        }

        @Override
        public Subscription bindTo(Topic topic) {
            Reject.rejectStateIfTrue(closed, "Subscription is closed");
            Reject.ifNull(topic, "Topic must not be null");
            topics.add(topic);
            return this;
        }

        @Override
        public boolean isBoundTo(Topic topic) {
            Reject.rejectStateIfTrue(closed, "Subscription is closed");
            Reject.ifNull(topic, "Topic must not be null");
            return topics.contains(topic);
        }

        @Override
        public Subscription unbindFrom(Topic topic) {
            Reject.rejectStateIfTrue(closed, "Subscription is closed");
            Reject.ifNull(topic, "Topic must not be null");
            topics.remove(topic);
            return this;
        }

        @Override
        public void close() {
            closed = true;
            subscriptions.remove(this);
        }

        // Called from reader thread.
        void consumeIfApplicable(Topic topic, JsonValue notification) {
            if (consumer == null) {
                return;
            }

            if (isBoundTo(topic)) {
                consumer.accept(notification);
            }
        }
    }

    private static final class NotificationEntry {

        private final Topic topic;
        private final JsonValue notification;

        private NotificationEntry(Topic topic, JsonValue notification) {
            this.topic = topic;
            this.notification = notification;
        }

        static NotificationEntry of(Topic topic, JsonValue notification) {
            return new NotificationEntry(topic, notification);
        }

    }

}
