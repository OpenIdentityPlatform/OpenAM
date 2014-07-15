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
package org.forgerock.openam.cts.impl.queue.config;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.ExternalTokenConfig;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.sm.ServerGroupConfiguration;

import javax.inject.Inject;
import javax.inject.Named;
import java.lang.IllegalArgumentException;
import java.text.MessageFormat;

/**
 * Responsible for resolving the number of available connections that can be utilised by the
 * CTS for its asynchronous processing operations.
 *
 * This class will resolve the complexity around the various sources which the CTS connections
 * can be provided from.
 */
public class CTSConnectionCount implements AsyncProcessorCount {
    private final ExternalTokenConfig ctsConfig;
    private final ServerGroupConfiguration serverGroupConfig;
    private final Debug debug;

    @Inject
    public CTSConnectionCount(@Named(CoreTokenConstants.CTS_SMS_CONFIGURATION) ServerGroupConfiguration serverGroupConfig,
                              ExternalTokenConfig ctsConfig,
                              @Named(CoreTokenConstants.CTS_ASYNC_DEBUG) Debug debug) {

        this.ctsConfig = ctsConfig;
        this.serverGroupConfig = serverGroupConfig;
        this.debug = debug;
    }

    /**
     * Determines the number of available connections based on the maximum value allowed
     * for either the default configuration, or the external configuration of the CTS.
     *
     * @return A positive integer.
     * @throws IllegalArgumentException If the External CTS configuration did not
     * have a numeric value assigned for its maximum connections.
     */
    public int getProcessorCount() throws CoreTokenException {
        int result;
        switch (ctsConfig.getStoreMode()) {
            case DEFAULT: {
                //Share the connections between SMS and CTS
                result = serverGroupConfig.getMaxConnections() / 2;
            }
            break;
            case EXTERNAL: {
                String maxConnections = ctsConfig.getMaxConnections();
                try {
                    result = Integer.valueOf(maxConnections);
                } catch (NumberFormatException nfe) {
                    String error = MessageFormat.format(
                            "Invalid value assigned for {0}: {1}",
                            CoreTokenConstants.CTS_MAX_CONNECTIONS,
                            maxConnections);
                    debug.error(error);
                    throw new CoreTokenException(error, nfe);
                }
            }
            break;
            default:
                throw new IllegalStateException("CTS Store mode is invalid");
        }

        return result;
    }
}
