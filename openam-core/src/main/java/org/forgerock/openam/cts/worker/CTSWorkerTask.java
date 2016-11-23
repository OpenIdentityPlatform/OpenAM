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

import java.text.MessageFormat;

import javax.inject.Inject;

import org.forgerock.openam.cts.impl.query.worker.CTSWorkerQuery;

/**
 * Class defining the base elements of a CTS worker task which may be executed by the {@link CTSWorkerManager}.
 */
public class CTSWorkerTask implements Runnable {

    private final CTSWorkerQuery query;
    private final CTSWorkerProcess process;
    private final CTSWorkerFilter filter;

    /**
     * Worker task constructor, requiring a query which selects elements to be processed,
     * a process defining the strategy for those elements, and a filter to remove elements to which the strategy
     * should not be applied.
     *
     * @param query The query to be performed against the data layer.
     * @param process The process to apply to filtered results from the executed query.
     * @param filter The filter to use to reduce the set returned from the executed query.
     */
    @Inject
    public CTSWorkerTask(CTSWorkerQuery query, CTSWorkerProcess process, CTSWorkerFilter filter) {
        this.query = query;
        this.process = process;
        this.filter = filter;
    }

    @Override
    public void run() {
        process.handle(query, filter);
    }

    @Override
    public String toString() {
        return MessageFormat.format("CTSWorker : Query = [{0}], Process = [{1}], Filter = [{2}]",
                query, process, filter);
    }

}