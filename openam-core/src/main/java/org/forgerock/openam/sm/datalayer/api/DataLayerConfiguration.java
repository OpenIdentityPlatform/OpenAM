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

package org.forgerock.openam.sm.datalayer.api;

import java.util.concurrent.ExecutorService;

import org.forgerock.openam.sm.datalayer.api.query.FilterConversion;
import org.forgerock.openam.sm.datalayer.api.query.QueryFactory;
import org.forgerock.openam.sm.datalayer.providers.ConnectionFactoryProvider;
import org.forgerock.openam.utils.ModifiedProperty;

import com.google.inject.Binder;

/**
 * Configuration of a data layer connection type.
 * @see org.forgerock.openam.sm.datalayer.api.ConnectionType
 */
public interface DataLayerConfiguration<C, F> {
    /**
     * The type of TaskExecutor to use with this connection type.
     */
    Class<? extends TaskExecutor> getTaskExecutorType();

    /**
     * Gets the mode of the data layer.
     */
    StoreMode getStoreMode();

    /**
     * The type of connection factory provider to create.
     */
    Class<? extends ConnectionFactoryProvider<C>> getConnectionFactoryProviderType();

    /**
     * Create an ExecutorService if the TaskExecutor needs it.
     */
    ExecutorService createExecutorService();

    /**
     * Get the query factory type.
     */
    Class<? extends QueryFactory<C, F>> getQueryFactoryType();

    /**
     * The type of the QueueConfiguration implementation (required by SeriesTaskExecutor).
     */
    Class<? extends QueueConfiguration> getQueueConfigurationType();

    /**
     * The type of the FilterConversion implementation.
     */
    Class<? extends FilterConversion<F>> getFilterConversionType();

    /**
     * The type of the TokenStorageAdapter implementation.
     */
    Class<? extends TokenStorageAdapter<C>> getTokenStorageAdapterType();

}
