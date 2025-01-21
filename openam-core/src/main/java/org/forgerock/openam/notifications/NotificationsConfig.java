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

package org.forgerock.openam.notifications;

import static com.sun.identity.shared.Constants.NOTIFICATIONS_AGENTS_ENABLED;

import jakarta.inject.Singleton;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.common.configuration.ConfigurationListener;

/**
 * Provides notifications configuration.
 *
 * @since 14.0.0
 */
@Singleton
public class NotificationsConfig {

    private volatile boolean agentsEnabled;

    NotificationsConfig() {
        SystemProperties.observe(new ConfigurationListener() {

            @Override
            public void notifyChanges() {
                agentsEnabled = SystemProperties.getAsBoolean(NOTIFICATIONS_AGENTS_ENABLED, true);
            }

        }, NOTIFICATIONS_AGENTS_ENABLED);

        agentsEnabled = SystemProperties.getAsBoolean(NOTIFICATIONS_AGENTS_ENABLED, true);
    }

    /**
     * Whether notifications for agents is enabled.
     *
     * @return whether notifications for agents is enabled
     */
    public boolean isAgentsEnabled() {
        return agentsEnabled;
    }

}
