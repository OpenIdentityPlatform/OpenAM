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
 * Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 */
package org.forgerock.openam.radius.server;

import java.text.MessageFormat;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.forgerock.openam.radius.server.config.RadiusServerConstants;

/**
 * Creates threads with name RADIUS-Request-Handler-# with # being the index of creation. Sets the thread group to that
 * of the calling thread and sets priority to the lesser of Thread.NORM_PRIORITY or the thread group's max priority.
 * This follows the general design of Executors.defaultThreadFactory save for the custom name.
 */
public class RadiusThreadFactory implements ThreadFactory {

    private final AtomicInteger idx = new AtomicInteger(0);

    @Override
    public Thread newThread(Runnable task) {
        final ThreadGroup grp = Thread.currentThread().getThreadGroup();
        final String name = MessageFormat.format(RadiusServerConstants.REQUEST_HANDLER_THREAD_NAME,
                idx.incrementAndGet());
        final Thread t = new Thread(grp, task, name);
        t.setPriority(Math.min(Thread.NORM_PRIORITY, grp.getMaxPriority()));
        return t;
    }
}
