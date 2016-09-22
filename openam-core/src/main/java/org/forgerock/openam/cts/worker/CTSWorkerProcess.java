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
*/
package org.forgerock.openam.cts.worker;

import org.forgerock.openam.cts.impl.query.worker.CTSWorkerQuery;

/**
 * Interface which defines the handling of results from a supplied {@link CTSWorkerQuery} using a provided
 * {@link CTSWorkerFilter}.
 */
public interface CTSWorkerProcess {

    /**
     * Executes this process against the results from the provided CTSWorkerQuery, which should be filtered
     * by the supplied CTSWorkerFilter first.
     *
     * Thread Safety: The implementation of this processor must respond to interrupts. Operations
     * should aim to complete quickly, and those that block must be interruptible. Once interrupted
     * the thread will exit.
     *
     * @param query The query which has been executed on the data layer.
     * @param filter The filter to ensure this process should apply to a given element.
     */
    void handle(CTSWorkerQuery query, CTSWorkerFilter filter);

}
