/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: SessionService.java,v 1.37 2010/02/03 03:52:54 bina Exp $
 *
 * Portions Copyrighted 2010-2016 ForgeRock AS.
 */

package com.iplanet.dpro.session.service;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.utils.Time.currentTimeMillis;

import java.net.URL;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.notifications.NotificationBroker;
import org.forgerock.openam.notifications.Topic;
import org.forgerock.openam.notifications.NotificationsConfig;
import org.forgerock.openam.session.SessionEventType;
import org.forgerock.util.thread.listener.ShutdownListener;
import org.forgerock.util.thread.listener.ShutdownManager;

import com.iplanet.am.util.ThreadPool;
import com.iplanet.am.util.ThreadPoolException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.SessionNotificationHandler;
import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.dpro.session.share.SessionNotification;
import com.iplanet.dpro.session.utils.SessionInfoFactory;
import com.iplanet.services.comm.server.PLLServer;
import com.iplanet.services.comm.share.Notification;
import com.iplanet.services.comm.share.NotificationSet;
import com.sun.identity.shared.debug.Debug;

/**
 * Responsible for sending PLL session notification events to registered listeners.
 *
 * Remote listeners (e.g. other instances of AM, Agents, and rich clients) will be notified using PLL/HTTP.
 *
 * Local listeners (i.e. this instance of AM) will be notified by calling SessionNotificationHandler directly.
 */
@Singleton
public class SessionNotificationSender {

    private static final String THREAD_POOL_NAME = "amSession";

    private final Debug sessionDebug;
    private final SessionServiceConfig serviceConfig;
    private final SessionServerConfig serverConfig;
    private final SessionInfoFactory sessionInfoFactory;
    private final ThreadPool threadPool;
    private final NotificationBroker broker;

    @Inject
    public SessionNotificationSender(
            final @Named(SessionConstants.SESSION_DEBUG) Debug sessionDebug,
            final SessionServiceConfig serviceConfig,
            final SessionServerConfig serverConfig,
            final SessionInfoFactory sessionInfoFactory,
            final ShutdownManager shutdownManager,
            final NotificationBroker broker) {

        this.sessionDebug = sessionDebug;
        this.serviceConfig = serviceConfig;
        this.serverConfig = serverConfig;
        this.sessionInfoFactory = sessionInfoFactory;
        this.broker = broker;

        threadPool = new ThreadPool(THREAD_POOL_NAME, serviceConfig.getNotificationThreadPoolSize(),
                serviceConfig.getNotificationThreadPoolThreshold(), true, sessionDebug);
        shutdownManager.addShutdownListener(
                new ShutdownListener() {
                    public void shutdown() {
                        threadPool.shutdown();
                    }
                }
        );

    }

    /**
     * Returns current Notification queue size.
     */
    public int getNotificationQueueSize() {
        return threadPool.getCurrentSize();
    }

    /**
     * Sends the Internal Session event to the SessionNotificationSender.
     *
     * @param session    Internal Session.
     * @param eventType Event Type.
     */
    public void sendEvent(InternalSession session, SessionEventType eventType) {
        sessionDebug.message("Running sendEvent, type = " + eventType.getCode());

        if (NotificationsConfig.INSTANCE.isAgentsEnabled()) {
            // TODO: Pull websocket notification logic into its own class (as SessionEventListener)
            publishSessionNotification(session.getSessionID(), eventType);

            for (SessionID sessionId : session.getRestrictedTokens()) {
                publishSessionNotification(sessionId, eventType);
            }
        }

        try {
            SessionNotificationSenderTask sns = new SessionNotificationSenderTask(session, eventType);
            // First send local notification. sendToLocal will return
            // true if remote URL's exists than add the notification
            // to the thread pool to process remote notifications.
            if (sns.sendToLocal()) {
                threadPool.run(sns);
            }

        } catch (ThreadPoolException e) {
            sessionDebug.error("Sending Notification Error: ", e);
        }
    }

    private void publishSessionNotification(SessionID sessionId, SessionEventType sessionEventType) {
        JsonValue notification = json(object(
                field("tokenId", sessionId.toString()),
                field("eventType", sessionEventType)));
        broker.publish(Topic.of("/agent/session"), notification);
    }

    /**
     * Inner Session Notification Publisher Class Thread.
     */
    private class SessionNotificationSenderTask implements Runnable {

        private final InternalSession session;
        private final SessionEventType eventType;
        private Map<String, Set<SessionID>> urls;

        SessionNotificationSenderTask(InternalSession session, SessionEventType eventType) {
            this.session = session;
            this.eventType = eventType;
        }

        /**
         * returns true if remote URL exists else returns false.
         */
        boolean sendToLocal() {
            boolean remoteURLExists = false;
            this.urls = session.getSessionEventURLs(eventType, serviceConfig.getLogoutDestroyBroadcast());

            // The check individual URLs
            if (!urls.isEmpty()) {
                for (Map.Entry<String, Set<SessionID>> entry : urls.entrySet()) {
                    String url = entry.getKey();
                    try {
                        URL parsedUrl = new URL(url);
                        if (serverConfig.isLocalNotificationService(parsedUrl)) {
                            for (SessionID sid : entry.getValue()) {
                                SessionInfo info = sessionInfoFactory.makeSessionInfo(session, sid);
                                SessionNotification notification =
                                        new SessionNotification(info, eventType.getCode(), currentTimeMillis());
                                SessionNotificationHandler.handler.processLocalNotification(notification);
                            }
                        } else {
                            // If the Global notification is for a remote URL, it should be handled from run()
                            // - This allows remote notification to be handled asynchronously from another thread
                            remoteURLExists = true;
                        }
                    } catch (Exception e) {
                        sessionDebug.error("Local Individual notification to " + url, e);
                    }
                }
            }
            return remoteURLExists;
        }


        /**
         * Thread which sends the Session Notification.
         */
        public void run() {
            if (urls == null) {
                throw new IllegalStateException("Must call sendToLocal before starting thread");
            }

            // The check individual URLs
            if (!urls.isEmpty()) {
                for (Map.Entry<String, Set<SessionID>> entry: urls.entrySet()) {
                    String url = entry.getKey();
                    try {
                        URL parsedUrl = new URL(url);
                        // Only send to remote URLs, local URLs should be handled by sendToLocal
                        if (!serverConfig.isLocalNotificationService(parsedUrl)) {
                            for (SessionID sid : entry.getValue()) {

                                SessionInfo info = sessionInfoFactory.makeSessionInfo(session, sid);
                                SessionNotification notification =
                                        new SessionNotification(info, eventType.getCode(), currentTimeMillis());
                                Notification notificationXml = new Notification(notification.toXMLString());
                                NotificationSet notificationSet = new NotificationSet(SessionService.SESSION_SERVICE);
                                notificationSet.addNotification(notificationXml);

                                PLLServer.send(parsedUrl, notificationSet);
                            }
                        }
                    } catch (Exception e) {
                        sessionDebug.error("Remote Individual notification to " + url, e);
                    }
                }
            }
        }
    }

}
