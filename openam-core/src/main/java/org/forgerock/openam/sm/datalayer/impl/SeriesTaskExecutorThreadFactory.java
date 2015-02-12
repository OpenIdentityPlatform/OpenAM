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
 * Copyright 2014-2015 ForgeRock AS.
 */
package org.forgerock.openam.sm.datalayer.impl;

import java.util.concurrent.BlockingQueue;

import javax.inject.Inject;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.sm.datalayer.api.Task;
import org.forgerock.util.Reject;

import com.google.inject.Injector;

/**
 * Responsible for generating instances of SeriesTaskExecutorThreads.
 */
public class SeriesTaskExecutorThreadFactory {

    private final Injector injector;

    @Inject
    public SeriesTaskExecutorThreadFactory(Injector injector) {
        this.injector = injector;
    }

    /**
     * Creates an instance of a SeriesTaskExecutorThread and assigns the BlockingQueue instance
     * to it.
     *
     * @param queue Non null BlockingQueue to assign.
     *
     * @return Non null SeriesTaskExecutorThread, ready to be started.
     */
    public SeriesTaskExecutorThread create(BlockingQueue<Task> queue) {
        Reject.ifNull(queue);
        SeriesTaskExecutorThread processor = injector.getInstance(SeriesTaskExecutorThread.class);
        processor.setQueue(queue);
        return processor;
    }
}
