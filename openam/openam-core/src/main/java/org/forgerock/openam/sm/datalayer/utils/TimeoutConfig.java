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
package org.forgerock.openam.sm.datalayer.utils;

import com.iplanet.am.util.SystemProperties;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;

/**
 * Responsible for resolving the configuration around the timeouts that will be applied
 * to the connections.
 */
public class TimeoutConfig {

    public static final int NO_TIMEOUT = 0;

    /**
     * The timeout for the connection factory type in seconds.
     *
     * @param type The {@link ConnectionType} to acquire timeout for.
     * @return Zero indicates no timeout, positive is timeout in seconds.
     *
     * @throws IllegalStateException If the type was unknown.
     */
    public int getTimeout(ConnectionType type) {
        switch (type) {
            case CTS_ASYNC:
                return SystemProperties.getAsInt(DataLayerConstants.CORE_TOKEN_ASYNC_TIMEOUT, 10);
            case CTS_REAPER:
                return SystemProperties.getAsInt(DataLayerConstants.CORE_TOKEN_REAPER_TIMEOUT, NO_TIMEOUT);
            case DATA_LAYER:
                return SystemProperties.getAsInt(DataLayerConstants.DATA_LAYER_TIMEOUT, 10);
            default:
                throw new IllegalStateException();
        }
    }
}