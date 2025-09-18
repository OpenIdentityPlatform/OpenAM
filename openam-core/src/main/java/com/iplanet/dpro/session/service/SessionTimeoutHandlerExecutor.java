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
 * Portions Copyrighted 2025 3A Systems LLC.
 */
package com.iplanet.dpro.session.service;

import static org.forgerock.openam.session.SessionConstants.SESSION_DEBUG;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.forgerock.openam.session.*;
import org.forgerock.openam.session.service.SessionTimeoutHandler;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.shared.debug.Debug;

/**
 * Executes all the globally set session timeout handlers in response to session timeout events.
 */
public class SessionTimeoutHandlerExecutor implements InternalSessionListener {

    public static final String EXECUTOR_BINDING_NAME = "SESSION_TIMEOUT_HANDLER_EXECUTOR";
    private final Debug sessionDebug;
    private final SSOTokenManager ssoTokenManager;
    private final SessionServiceConfig serviceConfig;
    private final ExecutorService executorService;

    @Inject
    SessionTimeoutHandlerExecutor(
            final @Named(SESSION_DEBUG) Debug sessionDebug,
            final @Named(EXECUTOR_BINDING_NAME) ExecutorService executorService,
            final SSOTokenManager ssoTokenManager,
            final SessionServiceConfig serviceConfig) {
        this.sessionDebug = sessionDebug;
        this.executorService = executorService;
        this.ssoTokenManager = ssoTokenManager;
        this.serviceConfig = serviceConfig;
    }

    @Override
    public void onEvent(final InternalSessionEvent event) {
        switch (event.getType()) {
            case IDLE_TIMEOUT:
            case MAX_TIMEOUT:
                execSessionTimeoutHandlers(event);
                break;
            default:
                // ignore all other types of event
        }
    }

    private void execSessionTimeoutHandlers(final InternalSessionEvent event) {

        final SessionID sessionId = event.getInternalSession().getSessionID();
        final SessionEventType eventType = event.getType();

        // Take snapshot of reference to ensure atomicity.
        final Set<String> handlers = serviceConfig.getTimeoutHandlers();

        if (!handlers.isEmpty()) {
            try {
                final SSOToken token = ssoTokenManager.createSSOToken(sessionId.toString());
                final List<Future<?>> futures = new ArrayList<>();
                final CountDownLatch latch = new CountDownLatch(handlers.size());

                for (final String clazz : handlers) {
                    Runnable timeoutTask = new Runnable() {

                        public void run() {
                            try {
                                SessionTimeoutHandler handler =
                                        Class.forName(clazz).asSubclass(SessionTimeoutHandler.class).newInstance();
                                switch (eventType) {
                                    case IDLE_TIMEOUT:
                                        handler.onIdleTimeout(token);
                                        break;
                                    case MAX_TIMEOUT:
                                        handler.onMaxTimeout(token);
                                        break;
                                }
                            } catch (Exception ex) {
                                if (Thread.interrupted()
                                        || ex instanceof InterruptedException
                                        || ex instanceof InterruptedIOException) {
                                    sessionDebug.warning("Timeout Handler was interrupted");
                                } else {
                                    sessionDebug.error("Error while executing the following session timeout handler: " + clazz, ex);
                                }
                            } finally {
                                latch.countDown();
                            }
                        }
                    };
                    futures.add(executorService.submit(timeoutTask)); // This should not throw any exceptions.
                }

                // Wait 1000ms for all handlers to complete.
                try {
                    latch.await(1000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ignored) {
                    //we can't handle it here, so propagate it.
                    Thread.currentThread().interrupt();
                }

                for (Future<?> future : futures) {
                    if (!future.isDone()) {
                        // It doesn't matter really if the future completes between isDone and cancel.
                        future.cancel(true); // Interrupt.
                    }
                }
            } catch (SSOException ssoe) {
                sessionDebug.warning("Unable to construct SSOToken for executing timeout handlers", ssoe);
            }
        }
    }
}
