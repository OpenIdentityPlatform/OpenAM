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

package org.forgerock.openam.sm.datalayer.impl.ldap;

import java.util.concurrent.ExecutorService;

import org.forgerock.openam.cts.impl.LDAPConfig;
import org.forgerock.openam.cts.impl.LdapAdapter;
import org.forgerock.openam.sm.datalayer.api.DataLayerConfiguration;
import org.forgerock.openam.sm.datalayer.api.QueueConfiguration;
import org.forgerock.openam.sm.datalayer.api.TaskExecutor;
import org.forgerock.openam.sm.datalayer.api.TokenStorageAdapter;
import org.forgerock.openam.sm.datalayer.api.query.FilterConversion;
import org.forgerock.openam.sm.datalayer.api.query.QueryBuilder;
import org.forgerock.openam.sm.datalayer.api.query.QueryFactory;
import org.forgerock.openam.sm.datalayer.api.query.QueryFilter;
import org.forgerock.openam.sm.datalayer.impl.PooledTaskExecutor;
import org.forgerock.openam.sm.datalayer.impl.SimpleTaskExecutor;
import org.forgerock.openam.sm.datalayer.providers.ConnectionFactoryProvider;
import org.forgerock.openam.sm.datalayer.providers.LdapConnectionFactoryProvider;
import org.forgerock.openam.utils.ModifiedProperty;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.Filter;

import com.google.inject.TypeLiteral;

/**
 * An abstract LDAP set of configuration.
 */
public abstract class LdapDataLayerConfiguration extends LDAPConfig implements DataLayerConfiguration<Connection, Filter> {

    private static final TypeLiteral<QueryFilter<Filter>> QUERY_FILTER_TYPE = new TypeLiteral<QueryFilter<Filter>>() {};
    private static final TypeLiteral<QueryBuilder<Connection, Filter>> QUERY_BUILDER_TYPE =
            new TypeLiteral<QueryBuilder<Connection, Filter>>() {};

    protected LdapDataLayerConfiguration(String rootSuffix) {
        super(rootSuffix);
    }

    /**
     * Defaults to the LdapQueryFactory.
     */
    @Override
    public Class<? extends QueryFactory<Connection, Filter>> getQueryFactoryType() {
        return LdapQueryFactory.class;
    }

    /**
     * Returns the {@link SimpleTaskExecutor}.
     */
    @Override
    public Class<? extends TaskExecutor> getTaskExecutorType() {
        return PooledTaskExecutor.class;
    }

    @Override
    public Class<? extends ConnectionFactoryProvider<Connection>> getConnectionFactoryProviderType() {
        return LdapConnectionFactoryProvider.class;
    }

    /**
     * Returns null for the {@link SimpleTaskExecutor}.
     */
    @Override
    public ExecutorService createExecutorService() {
        return null;
    }

    /**
     * Returns null as the SimpleTaskExecutor does not require one.
     */
    @Override
    public Class<? extends QueueConfiguration> getQueueConfigurationType() {
        return null;
    }

    /**
     * Returns default LdapFilterConversion.
     */
    @Override
    public Class<? extends FilterConversion<Filter>> getFilterConversionType() {
        return LdapFilterConversion.class;
    }

    /**
     * Returns LdapAdapter.
     */
    @Override
    public Class<? extends TokenStorageAdapter<Connection>> getTokenStorageAdapterType() {
        return LdapAdapter.class;
    }

    /**
     * Update the configuration of the LDAP connection details.
     * @param hosts The LDAP hosts.
     * @param username The LDAP username.
     * @param password The LDAP password.
     * @param maxConnections The maximum number of connections.
     * @param sslMode The SSL mode.
     * @param heartbeat The heartbeat interval.
     */
    public abstract void updateExternalLdapConfiguration(ModifiedProperty<String> hosts,
            ModifiedProperty<String> username, ModifiedProperty<String> password,
            ModifiedProperty<String> maxConnections, ModifiedProperty<Boolean> sslMode,
            ModifiedProperty<Integer> heartbeat);

}
