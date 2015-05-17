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

import java.util.concurrent.ExecutorService;

/**
 * An ExecutorService that <ul>may</ul> be re-configured after construction.
 *
 * This interface is a work-around for the fact that ThreadPoolExecutor does not implement an interface for
 * its configuration methods. As such, in order to re-configure a ThreadPoolExecutor after its construction
 * you must cast it to its concrete type. This aspect of the ThreadPoolExecutor design makes it hard to write
 * decorators that support re-configuration, (such as {@link AuditRequestContextPropagatingExecutorService}).
 *
 * @since 13.0.0
 */
public interface ConfigurableExecutorService extends ExecutorService {

    /**
     * @return True if this objects can actually be re-configured.
     * @see #getConfigurator()
     */
    boolean isConfigurable();

    /**
     * @return An ExecutorServiceConfigurator for this object if it supports re-configuration.
     * @throws IllegalStateException if this object does not support re-configuration.
     */
    ExecutorServiceConfigurator getConfigurator();
}
