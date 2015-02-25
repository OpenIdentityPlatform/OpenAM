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
package org.forgerock.openam.cts.impl.queue;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.LDAPAdapter;
import org.forgerock.openam.cts.impl.task.Task;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.openam.utils.IOUtils;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.ErrorResultException;

import javax.inject.Inject;
import javax.inject.Named;
import java.text.MessageFormat;
import java.util.concurrent.BlockingQueue;

/**
 * Responsible for processing CTS Tasks asynchronously.
 *
 * This implementation will draw a task from an allocated BlockingQueue and
 * makes no assumptions about the nature of the task, or the queue from which
 * it is drawing tasks.
 *
 * This decoupled design is intended to ensure that each TaskProcessor can be
 * run as part of a thread pool, and process tasks in a continuous fashion.
 *
 * Thread Policy: This runnable will respond to Thread interrupts and will
 * exit cleanly in the event of an interrupt.
 *
 * @see org.forgerock.openam.cts.impl.task.Task
 * @see org.forgerock.openam.cts.impl.task.TaskFactory
 */
public class TaskProcessor implements Runnable {
    private BlockingQueue<Task> queue;
    private final ConnectionFactory connectionFactory;
    private final Debug debug;
    private final LDAPAdapter adapter;

    /**
     * Generate a default instance of the Task Processor.
     *
     * @param connectionFactory Required for initialising connections.
     * @param adapter Required for LDAP operations.
     * @param debug Required for debugging.
     */
    @Inject
    public TaskProcessor(@Named(DataLayerConstants.DATA_LAYER_CTS_ASYNC_BINDING) ConnectionFactory connectionFactory,
                         LDAPAdapter adapter,
                         @Named(CoreTokenConstants.CTS_DEBUG) Debug debug) {
        this.connectionFactory = connectionFactory;
        this.debug = debug;
        this.adapter = adapter;
    }

    /**
     * Assign a BlockingQueue to this processor.
     *
     * Note: This must be set before execution is started.
     *
     * @param queue Non null BlockingQueue implementation to use for asynchronous processing.
     */
    public void setQueue(BlockingQueue<Task> queue) {
        this.queue = queue;
    }

    /**
     * Starts processing of the Queue Tasks.
     *
     * @throws java.lang.IllegalStateException If the queue has not been assigned.
     */
    @Override
    public void run() {
        if (queue == null) throw new IllegalStateException("Must assign a queue before starting.");

        Connection connection = null;

        // Iterate until shutdown
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Task task = queue.take();
                debug("process Task {0}", task);

                if (connection == null || !connection.isValid() || connection.isClosed()) {
                    close(connection);
                    connection = connectionFactory.getConnection();
                    debug("acquired connection");
                }

                task.execute(connection, adapter);
            } catch (CoreTokenException e) {
                error("processing task", e);
            } catch (ErrorResultException e) {
                error("acquiring connection", e);
            } catch (InterruptedException e) {
                error("interrupt detected", e);
                Thread.currentThread().interrupt();
            }
        }

        close(connection);

        debug("Processor thread shutdown.");
    }

    /**
     * Close the connection if it was not null.
     *
     * @param connection The possibly null connection to close.
     */
    private void close(Connection connection) {
        if (connection != null) {
            debug("Closing connection");
            IOUtils.closeIfNotNull(connection);
        }
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
