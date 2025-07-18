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

package org.forgerock.openam.notifications.brokers;

import static org.forgerock.json.JsonValue.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.audit.context.AMExecutorServiceFactory;
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
 * The queue is a fixed size and therefore notifications may be lost if the queue becomes
 * full.
 *
 * @since 14.0.0
 */
public final class InMemoryNotificationBroker implements NotificationBroker {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryNotificationBroker.class);
    private static final DateTimeFormatter TS_FORMATTER = ISODateTimeFormat.dateTime().withZoneUTC();

    private final BlockingQueue<NotificationEntry> queue;
    private final List<InternalSubscription> subscriptions;
    private final TimeService timeService;

    private final ExecutorService executorService;
    private volatile boolean shutdown;

    /**
     * Constructs a new InMemoryNotificationBroker.
     *
     * @param executorServiceFactory an executor service factory for scheduling reader threads
     * @param timeService a time service for adding timestamps to messages
     * @param queueSize the number of notifications to buffer in memory
     */
    @Inject
    public InMemoryNotificationBroker(AMExecutorServiceFactory executorServiceFactory, TimeService timeService,
            @Named("queueSize") int queueSize, @Named("consumers") int consumers) {
        Reject.ifNull(executorServiceFactory, "Executor service factory must not be null");
        Reject.ifNull(timeService, "Time service must not be null");
        Reject.ifTrue(queueSize <= 0, "Queue size must be a positive integer");
        Reject.ifTrue(consumers <= 0, "Number of consumer threads must be a positive integer");

        this.timeService = timeService;

        queue = new ArrayBlockingQueue<>(queueSize);
        subscriptions = new CopyOnWriteArrayList<>();
        executorService = executorServiceFactory.createFixedThreadPool(consumers, "InMemoryNotificationsBroker");
        for (int i = 0; i < consumers; i++) {
            executorService.submit(new NotificationReader());
        }
    }

    @Override
    public boolean publish(Topic topic, JsonValue notification) {
        Reject.ifNull(topic, "Topic must not be null");
        Reject.ifNull(notification, "Notification must not be null");

        if (shutdown) {
            logger.info("Not publishing notification as broker shutting down");
            return false;
        }

        NotificationEntry entry = NotificationEntry.of(topic, packageNotification(topic, notification));

        if (!queue.offer(entry)) {
            logger.info("Failed to publish notification because queue is full. Notification discarded");
            return false;
        }

        return true;
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
        executorService.shutdownNow();
    }

    private final class NotificationReader implements Runnable {

        @Override
        public void run() {
            while (!shutdown) {
                try {
                    NotificationEntry entry = queue.poll(10L, TimeUnit.SECONDS);

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
