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

package org.forgerock.openam.shared.audit.context;

import org.forgerock.util.Reject;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Exposes aspects of a wrapped ThreadPoolExecutor's configuration.
 *
 * {Alink ExecutorService} decorators can choose to expose instances of this class rather than breaking encapsulation
 * of the ExecutorService that they wrap.
 *
 * @since 13.0.0
 */
public class ExecutorServiceConfigurator {

    private final ThreadPoolExecutor delegate;

    public ExecutorServiceConfigurator(ThreadPoolExecutor delegate) {
        Reject.ifNull(delegate);
        this.delegate = delegate;
    }

    /**
     * @see ThreadPoolExecutor#setCorePoolSize(int)
     */
    public void setCorePoolSize(int corePoolSize) {
        delegate.setCorePoolSize(corePoolSize);
    }

    /**
     * @see ThreadPoolExecutor#getCorePoolSize()
     */
    public int getCorePoolSize() {
        return delegate.getCorePoolSize();
    }

    /**
     * @see ThreadPoolExecutor#setMaximumPoolSize(int)
     */
    public void setMaximumPoolSize(int maximumPoolSize) {
        delegate.setMaximumPoolSize(maximumPoolSize);
    }

    /**
     * @see ThreadPoolExecutor#getMaximumPoolSize()
     */
    public int getMaximumPoolSize() {
        return delegate.getMaximumPoolSize();
    }

    /**
     * @see ThreadPoolExecutor#setKeepAliveTime(long, TimeUnit)
     */
    public void setKeepAliveTime(long time, TimeUnit unit) {
        delegate.setKeepAliveTime(time, unit);
    }

    /**
     * @see ThreadPoolExecutor#getKeepAliveTime(TimeUnit)
     */
    public long getKeepAliveTime(TimeUnit unit) {
        return delegate.getKeepAliveTime(unit);
    }

}
