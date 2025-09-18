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
 * Portions Copyrighted 2022 Open Identity Platform Community
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package com.iplanet.dpro.session.service;

import java.net.URL;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

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
public class SessionNotificationSender implements InternalSessionListener {

    private static final String THREAD_POOL_NAME = "amSession";

    private final Debug sessionDebug;
    private final SessionServerConfig serverConfig;
    private final SessionInfoFactory sessionInfoFactory;
    private final ThreadPool threadPool;

    @Inject
    public SessionNotificationSender(
            final @Named(SessionConstants.SESSION_DEBUG) Debug sessionDebug,
            final SessionServiceConfig serviceConfig,
            final SessionServerConfig serverConfig,
            final SessionInfoFactory sessionInfoFactory,
            final ShutdownManager shutdownManager) {

        this.sessionDebug = sessionDebug;
        this.serverConfig = serverConfig;
        this.sessionInfoFactory = sessionInfoFactory;

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

    @Override
    public void onEvent(final InternalSessionEvent event) {
        switch (event.getType()) {
            case SESSION_CREATION:
            case LOGOUT:
            case DESTROY:
            case PROPERTY_CHANGED:
            case EVENT_URL_ADDED:
                sendEvent(event);
                break;
            default:
                // ignore all other types of event
        }
    }

    private void sendEvent(final InternalSessionEvent event) {
        sessionDebug.message("Running sendEvent, type = " + event.getType().getCode());

        try {
            SessionNotificationSenderTask sns = new SessionNotificationSenderTask(event);
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

    /**
     * Inner Session Notification Publisher Class Thread.
     */
    private class SessionNotificationSenderTask implements Runnable {

        private final InternalSessionEvent event;
        private Map<String, Set<SessionID>> urls;

        SessionNotificationSenderTask(final InternalSessionEvent event) {
            this.event = event;
        }

        /**
         * returns true if remote URL exists else returns false.
         */
        boolean sendToLocal() {
            boolean remoteURLExists = false;
            this.urls = event.getInternalSession().getSessionEventURLs();

            // The check individual URLs
            if (!urls.isEmpty()) {
                for (Map.Entry<String, Set<SessionID>> entry : urls.entrySet()) {
                    String url = entry.getKey();
                    try {
                        URL parsedUrl = new URL(url);
                        if (serverConfig.isLocalNotificationService(parsedUrl)) {
                            for (SessionID sid : entry.getValue()) {
                                SessionInfo info = sessionInfoFactory.makeSessionInfo(event.getInternalSession(), sid);
                                SessionNotification notification =
                                        new SessionNotification(info, event.getType().getCode(), event.getTime());
                                SessionNotificationHandler.handler.processNotification(notification, false);
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

                                SessionInfo info = sessionInfoFactory.makeSessionInfo(event.getInternalSession(), sid);
                                SessionNotification notification =
                                        new SessionNotification(info, event.getType().getCode(), event.getTime());
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
