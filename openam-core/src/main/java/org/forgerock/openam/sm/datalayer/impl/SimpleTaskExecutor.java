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
 * Copyright 2015-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.sm.datalayer.impl;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.api.Task;
import org.forgerock.openam.sm.datalayer.api.TaskExecutor;
import org.forgerock.openam.sm.datalayer.api.TokenStorageAdapter;

import com.sun.identity.shared.debug.Debug;

/**
 * A simple TaskExecutor that simply executes a task.
 */
public class SimpleTaskExecutor implements TaskExecutor {

    private final TokenStorageAdapter adapter;
    private final Debug debug;

    @Inject
    public SimpleTaskExecutor(@Named(DataLayerConstants.DATA_LAYER_DEBUG) Debug debug,
            TokenStorageAdapter adapter) {
        this.adapter = adapter;
        this.debug = debug;
    }

    @Override
    public synchronized void start() throws DataLayerException {
       //this section intentionally left blank
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    /**
     * The simple executor that executes a task.
     *
     * TokenId is unused in this implementation.
     *
     * @param tokenId The token the task is in reference to. May be null in the case of query.
     * @param task The task to be executed.
     */
    @Override
    public void execute(String tokenId, Task task) {
        try {
            task.execute(adapter);
        } catch (DataLayerException e) {
            error("processing task", e);
        }
    }

    private void error(String message, Throwable t) {
        debug.error(CoreTokenConstants.DEBUG_ASYNC_HEADER + "Task Processor Error: " + message, t);
    }
}
