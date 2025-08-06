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
 * Copyright 2019 Open Identity Platform Community.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.openidentityplatform.openam.cassandra;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.utils.LdapTokenAttributeConversion;
import org.forgerock.openam.sm.ConnectionConfig;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.openam.sm.datalayer.api.TaskExecutor;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.forgerock.openam.sm.datalayer.impl.PooledTaskExecutor;
import org.forgerock.openam.sm.datalayer.impl.ldap.EntryConverter;
import org.forgerock.openam.sm.datalayer.impl.ldap.EntryPartialTokenConverter;
import org.forgerock.openam.sm.datalayer.impl.ldap.EntryStringConverter;
import org.forgerock.openam.sm.datalayer.impl.ldap.EntryTokenConverter;
import org.forgerock.openam.sm.datalayer.impl.ldap.ExternalLdapConfig;
import org.forgerock.openam.sm.datalayer.impl.ldap.LdapDataLayerConfiguration;
import org.forgerock.openam.sm.datalayer.impl.ldap.LdapSearchHandler;
import org.forgerock.openam.sm.datalayer.providers.ConnectionFactoryProvider;
import org.forgerock.openam.sm.datalayer.providers.DataLayerConnectionFactoryCache;
import com.google.inject.Key;
import com.google.inject.PrivateBinder;
import com.google.inject.Provider;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;

public abstract class DataLayerConnectionModule extends org.forgerock.openam.sm.datalayer.api.DataLayerConnectionModule {

	    protected DataLayerConnectionModule(Class<? extends TaskExecutor> executorType, boolean exposesQueueConfiguration) {
	        super(executorType, TokenStorageAdapter.class, exposesQueueConfiguration);
	    }

	    public DataLayerConnectionModule() {
	        this(PooledTaskExecutor.class, false);
	    }

	    @Override
	    protected void configureConnections(PrivateBinder binder) {
	        binder.bind(org.forgerock.openam.sm.datalayer.api.query.QueryFactory.class).to(QueryFactory.class);
	        binder.bind(LdapTokenAttributeConversion.class);
	        binder.bind(LdapSearchHandler.class);
	        binder.bind(EntryPartialTokenConverter.class);
	        binder.bind(EntryTokenConverter.class);
	        binder.bind(QueryBuilder.class);
	        binder.bind(QueryFactory.class);
	        binder.bind(ConnectionConfig.class).annotatedWith(Names.named(DataLayerConstants.EXTERNAL_CONFIG)).toProvider(ExternalConnectionConfigProvider.class);
	        binder.bind(ConnectionFactoryProvider.class).to(org.openidentityplatform.openam.cassandra.ConnectionFactoryProvider.class);
	        binder.bind(ConnectionFactory.class).toProvider(getConnectionFactoryProviderType());

	        @SuppressWarnings("rawtypes")
			final MapBinder<Class, EntryConverter> entryConverterBinder = MapBinder.newMapBinder(binder, Class.class,EntryConverter.class);
	        entryConverterBinder.addBinding(String.class).to(EntryStringConverter.class);
	        entryConverterBinder.addBinding(PartialToken.class).to(EntryPartialTokenConverter.class);
	        entryConverterBinder.addBinding(Token.class).to(EntryTokenConverter.class);

	        binder.bind(LdapDataLayerConfiguration.class).to(getLdapConfigurationType()).in(Singleton.class);
	        binder.bind(Key.get(LdapDataLayerConfiguration.class, DataLayer.Types.typed(connectionType))).toProvider(binder.getProvider(LdapDataLayerConfiguration.class));
	        binder.expose(Key.get(LdapDataLayerConfiguration.class, DataLayer.Types.typed(connectionType)));
	        
	    }
	    static boolean CTSReaperInitConfigured=false;
	    
	    protected Class<? extends LdapDataLayerConfiguration> getLdapConfigurationType() {
	        return DataLayerConfiguration.class;
	    }
	    
	    /**
	     * Returns the provider of {@link org.forgerock.openam.sm.datalayer.api.ConnectionFactory}. The default is to use
	     * {@link org.forgerock.openam.sm.datalayer.providers.DataLayerConnectionFactoryCache}.
	     * @return
	     *  return
	     */
	    @SuppressWarnings("rawtypes")
		protected Class<? extends jakarta.inject.Provider<ConnectionFactory>> getConnectionFactoryProviderType() {
	        return DataLayerConnectionFactoryCache.class;
	    }

	    private static final class ExternalConnectionConfigProvider implements Provider<ConnectionConfig> {
	        private final LdapDataLayerConfiguration configuration;
	        private final ExternalLdapConfig externalConfig;

	        @Inject
	        public ExternalConnectionConfigProvider (ExternalLdapConfig externalConfig, LdapDataLayerConfiguration configuration) {
	            this.externalConfig = externalConfig;
	            this.configuration = configuration;
	        }

	        public ConnectionConfig get() {
	            externalConfig.update(configuration);
	            return externalConfig;
	        }
	    }

	}

