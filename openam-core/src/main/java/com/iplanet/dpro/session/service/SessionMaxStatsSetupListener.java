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

import static com.iplanet.dpro.session.service.SessionConstants.SESSION_DEBUG;
import static com.iplanet.dpro.session.service.SessionConstants.STATS_MASTER_TABLE;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.session.service.SessionAccessManager;

import com.sun.identity.setup.SetupListener;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.stats.Stats;

/**
 * Responsible for initialising {@link SessionMaxStats} on server start-up if {@link Stats} are enabled.
 */
public class SessionMaxStatsSetupListener implements SetupListener {

    @Override
    public void setupComplete() {
        inBackgroundThread(initializeMaxSessionStats());
    }

    private void inBackgroundThread(Runnable runnable) {
        new Thread(runnable).start();
    }

    private Runnable initializeMaxSessionStats() {
        return InjectorHolder.getInstance(InitializeMaxSessionStats.class);
    }

    private static class InitializeMaxSessionStats implements Runnable {

        private final Debug debug;
        private final Stats stats;
        private final SessionAccessManager accessManager;
        private final SessionNotificationSender notificationSender;

        @Inject
        InitializeMaxSessionStats(
                @Named(SESSION_DEBUG) final Debug debug,
                @Named(STATS_MASTER_TABLE) final Stats stats,
                final SessionAccessManager sessionAccessManager,
                final SessionNotificationSender sessionNotificationSender) {
            this.debug = debug;
            this.stats = stats;
            this.accessManager = sessionAccessManager;
            this.notificationSender = sessionNotificationSender;
        }

        @Override
        public void run() {
            try {
                if (stats.isEnabled()) {
                    stats.addStatsListener(
                            new SessionMaxStats(accessManager, notificationSender, stats)
                    );
                }
            } catch (Exception ex) {
                debug.error("Session stats initialization failed.", ex);
            }
        }
    }
}
