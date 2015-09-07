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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openam.authentication.service;

import com.iplanet.dpro.session.service.InternalSession;

/**
 * An <strong>internal</strong> extension point that can be used to maintain cache data corresponding to a given logged
 * in user during a session upgrade.
 */
public interface SessionUpgradeHandler {

    /**
     * Allows execution of custom logic during a session upgrade to ensure that internal data structures are properly
     * maintained during a session upgrade.
     *
     * @param oldSession The old session that is about to be upgraded.
     * @param newSession The just activated new session.
     */
    void handleSessionUpgrade(InternalSession oldSession, InternalSession newSession);
}
