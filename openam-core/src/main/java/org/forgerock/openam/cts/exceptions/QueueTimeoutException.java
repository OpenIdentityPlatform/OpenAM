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
 * Copyright 2013-2014 ForgeRock AS.
 */
package org.forgerock.openam.cts.exceptions;

import org.forgerock.openam.cts.impl.task.Task;

import java.lang.InterruptedException;
import java.text.MessageFormat;

/**
 * Signals that the CTS Async processing took too long to place the
 * Task on the queue.
 */
public class QueueTimeoutException extends CoreTokenException {
    /**
     * Indicates that the Queue operation exceeded the timeout before it could complete.
     *
     * @param task The task that was being processed at the time.
     */
    public QueueTimeoutException(Task task) {
        super(MessageFormat.format(
                "Timed out whilst waiting on queue.\n" +
                "Task: {0}",
                task));
    }

    /**
     * Indicates that the Queue operation was interrupted before it could complete.
     *
     * @param task The task that was being processed at the time.
     * @param error The {@link InterruptedException}.
     */
    public QueueTimeoutException(Task task, InterruptedException error) {
        super(MessageFormat.format(
                "Interrupted whilst waiting on queue.\n" +
                "Task: {0}",
                task),
                error);
    }
}
