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

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.cts.impl.task.Task;
import org.forgerock.util.Reject;

import java.util.concurrent.BlockingQueue;

/**
 * Responsible for generating instances of TaskProcessors.
 */
public class TaskProcessorFactory {
    /**
     * Creates an instance of a TaskProcessor and assigns the BlockingQueue instance
     * to the TaskProcessor.
     *
     * @param queue Non null BlockingQueue to assign.
     *
     * @return Non null TaskProcessor, ready to be started.
     */
    public TaskProcessor create(BlockingQueue<Task> queue) {
        Reject.ifNull(queue);
        TaskProcessor processor = InjectorHolder.getInstance(TaskProcessor.class);
        processor.setQueue(queue);
        return processor;
    }
}
