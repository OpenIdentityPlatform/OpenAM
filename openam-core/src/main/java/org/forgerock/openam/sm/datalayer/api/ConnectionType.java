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
 * Copyright 2014 ForgeRock AS.
 */
package org.forgerock.openam.sm.datalayer.api;

import org.forgerock.openam.cts.impl.queue.TaskDispatcher;
import org.forgerock.openam.cts.reaper.CTSReaper;
import org.forgerock.openam.entitlement.indextree.IndexTreeService;

/**
 * Defines the types of connections factories that should be used to provide connections
 * to callers.
 */
public enum ConnectionType {
    /**
     * @see TaskDispatcher
     */
    CTS_ASYNC,
    /**
     * @see CTSReaper
     */
    CTS_REAPER,
    /**
     * @see IndexTreeService
     */
    DATA_LAYER;
}
