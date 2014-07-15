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
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.queue.QueueSelector;
import org.forgerock.openam.cts.impl.queue.TaskDispatcher;
import org.forgerock.openam.cts.impl.queue.TaskProcessor;
import org.forgerock.openam.cts.reaper.CTSReaper;

import javax.inject.Inject;
import javax.inject.Named;
import java.text.MessageFormat;

/**
 * Answers the question of how many connections are available to the CTS Async {@link TaskProcessor}
 * by also taking into account the CTS Reaper which will need one connection for its longer duration
 * query.
 *
 * This implementation will select an appropriate power of two within the given connection count
 * which allows for the CTS Reaper.
 *
 * @see CTSReaper
 * @see TaskDispatcher
 * @see QueueSelector
 */
public class ReaperConnectionCount implements AsyncProcessorCount {

    private final CTSConnectionCount ctsCount;
    private final Debug debug;

    /**
     * @param ctsCount Non null, required to resolve the number of connections available to the CTS.
     */
    @Inject
    public ReaperConnectionCount(CTSConnectionCount ctsCount,
                                 @Named(CoreTokenConstants.CTS_ASYNC_DEBUG) Debug debug) {
        this.ctsCount = ctsCount;
        this.debug = debug;
    }

    /**
     * Uses the CTSConnectionCount to acquire the number of available connections. If this value allows
     * for the power of two requirement and the CTS Reaper requirement then the power of two value is returned.
     *
     * @return {@inheritDoc}
     */
    @Override
    public int getProcessorCount() throws CoreTokenException {
        int cts = ctsCount.getProcessorCount();
        debug("CTS Connection Count: {0}", cts);

        // Allow for the CTS Reaper in the available connections.
        int pot = QueueSelector.findPowerOfTwo(cts);
        if (pot == cts) {
            pot = QueueSelector.findPowerOfTwo(cts - 1);
        }

        debug("Connections for async processing: {0}", pot);
        return pot;
    }

    private void debug(String format, Object... args) {
        if (debug.messageEnabled()) {
            debug.message(MessageFormat.format(
                    CoreTokenConstants.DEBUG_ASYNC_HEADER + format, args));
        }
    }
}