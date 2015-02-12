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

package org.forgerock.openam.sm.datalayer.impl;

import java.io.Closeable;
import java.io.IOException;
import java.text.MessageFormat;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.api.Task;
import org.forgerock.openam.sm.datalayer.api.TaskExecutor;
import org.forgerock.openam.sm.datalayer.api.TokenStorageAdapter;

import com.sun.identity.shared.debug.Debug;

/**
 * A simple TaskExecutor that simply obtains a connection and executes the task on it.
 * @param <T> The type of connection being used.
 */
public class SimpleTaskExecutor<T> implements TaskExecutor {

    private final ConnectionFactory<T> connectionFactory;
    private final TokenStorageAdapter<T> adapter;
    private final Debug debug;
    private T connection;

    @Inject
    public SimpleTaskExecutor(ConnectionFactory connectionFactory,
            @Named(DataLayerConstants.DATA_LAYER_DEBUG) Debug debug,
            TokenStorageAdapter adapter) {
        this.connectionFactory = connectionFactory;
        this.adapter = adapter;
        this.debug = debug;
    }

    @Override
    public synchronized void start() throws DataLayerException {
        if (connection == null) {
            this.connection = connectionFactory.create();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    /**
     * The simple executor obtains a connection and executes the task with it.
     * @param tokenId The token the task is in reference to. May be null in the case of query.
     * @param task The task to be executed.
     */
    @Override
    public void execute(String tokenId, Task task) {
        try {
            if (!connectionFactory.isValid(connection)) {
                close();
                start();
            }
            task.execute(connection, adapter);
        } catch (DataLayerException e) {
            error("processing task", e);
        }
    }

    /**
     * Close the connection if it was not null.
     */
    private synchronized void close() {
        if (connection instanceof Closeable) {
            debug("Closing connection");
            try {
                ((Closeable)connection).close();
            } catch (IOException e) {
                debug.message("IOException when closing connection", e);
            }
        }
        connection = null;
    }

    private void debug(String format, Object... args) {
        if (debug.messageEnabled()) {
            debug.message(MessageFormat.format(
                    CoreTokenConstants.DEBUG_ASYNC_HEADER + "Task Processor: " + format, args));
        }
    }

    private void error(String message, Throwable t) {
        debug.error(CoreTokenConstants.DEBUG_ASYNC_HEADER + "Task Processor Error: " + message, t);
    }
}
