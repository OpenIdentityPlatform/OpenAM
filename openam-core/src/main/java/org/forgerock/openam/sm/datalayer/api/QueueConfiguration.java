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

package org.forgerock.openam.sm.datalayer.api;

public interface QueueConfiguration {
    /**
     * The maximum duration the caller should wait to place their asynchronous
     * task on a work queue, and the maximum duration the caller should wait to
     * retrieve the result of processing from the queue.
     *
     * @return A positive integer in seconds to wait.
     */
    int getQueueTimeout();

    /**
     * The size of each work queue that is used by the asynchronous work queue mechanism.
     * This will control how many items can be queued up in the CTS before this causes
     * the caller to block.
     *
     * @return A positive integer for the size of the queue,
     */
    int getQueueSize();

    /**
     * The number of asynchronous Task Processors that should be initialised.
     * This value is based on the number of connections available.
     *
     * @see org.forgerock.openam.sm.ConnectionConfigFactory
     * @see org.forgerock.openam.sm.datalayer.utils.ConnectionCount
     * @see org.forgerock.openam.cts.impl.queue.QueueSelector
     *
     * @throws DataLayerException If there was any issue resolving the configuration of the processors.
     * @return A positive number of processors to initialise.
     */
    int getProcessors() throws DataLayerException;
}
