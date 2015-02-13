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

import javax.inject.Singleton;

import org.forgerock.openam.sm.datalayer.api.query.QueryFactory;
import org.forgerock.openam.sm.datalayer.impl.tasks.TaskFactory;

import com.google.inject.Key;
import com.google.inject.PrivateBinder;
import com.google.inject.PrivateModule;

public abstract class DataLayerConnectionModule extends PrivateModule {

    private final boolean exposesExecutor;
    private final boolean exposesQueueConfiguration;
    protected ConnectionType connectionType;

    protected DataLayerConnectionModule(boolean exposesExecutor, boolean exposesQueueConfiguration) {
        this.exposesExecutor = exposesExecutor;
        this.exposesQueueConfiguration = exposesQueueConfiguration;
    }

    public void setConnectionType(ConnectionType connectionType) {
        this.connectionType = connectionType;
    }

    @Override
    protected void configure() {
        PrivateBinder binder = binder().withSource(connectionType);

        binder.bind(ConnectionType.class).toInstance(connectionType);

        configureConnections(binder);
        configureTaskExecutor(binder);
        configureDataStore(binder);

        expose(binder, ConnectionFactory.class);
        expose(binder, QueryFactory.class);

        if (exposesQueueConfiguration) {
            expose(binder, QueueConfiguration.class);
        }

        if (exposesExecutor) {
            binder.bind(TaskFactory.class).in(Singleton.class);
            expose(binder, TaskFactory.class);
            expose(binder, TaskExecutor.class);
        }
    }

    private <T> void expose(PrivateBinder binder, Class<T> type) {
        Key<T> key = Key.get(type, DataLayer.Types.typed(connectionType));
        binder.bind(key).toProvider(binder.getProvider(type));
        binder.expose(key);
    }

    /**
     * Configures the connection classes.
     * <p>
     * At the minimum, this must bind instances of:
     * <ul>
     *     <li>{@link org.forgerock.openam.sm.datalayer.api.ConnectionFactory}</li>
     *     <li>{@link org.forgerock.openam.sm.datalayer.api.query.QueryFactory}</li>
     * </ul>
     * @param binder The module's binder.
     */
    abstract protected void configureConnections(PrivateBinder binder);

    /**
     * Configures the task executor and associated classes. If the data layer is only being used for its
     * connection factory, this may not need to do anything.
     * <p>
     * At the minimum, this must bind instances of:
     * <ul>
     *     <li>{@link org.forgerock.openam.sm.datalayer.api.TaskExecutor}</li>
     *     <li>{@link org.forgerock.openam.sm.datalayer.api.TokenStorageAdapter}</li>
     * </ul>
     * <p>
     * If the task executor is a {@link org.forgerock.openam.sm.datalayer.impl.SeriesTaskExecutor}, a
     * {@link org.forgerock.openam.sm.datalayer.api.QueueConfiguration} will also need to be bound.
     * @return Whether a task executor was configured. If true, an instance of {@link TaskFactory} will be bound.
     * @param binder The module's binder.
     */
    abstract protected void configureTaskExecutor(PrivateBinder binder);

    /**
     * If the connection type requires a data store, it can be bound here.
     */
    protected void configureDataStore(PrivateBinder binder) {

    }

}
