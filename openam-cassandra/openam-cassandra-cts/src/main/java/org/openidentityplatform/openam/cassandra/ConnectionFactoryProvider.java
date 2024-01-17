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
 */

package org.openidentityplatform.openam.cassandra;

import java.text.MessageFormat;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.sm.ConnectionConfig;
import org.forgerock.openam.sm.ConnectionConfigFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.api.StoreMode;
import org.forgerock.openam.sm.datalayer.utils.TimeoutConfig;
import org.forgerock.openam.sm.exceptions.InvalidConfigurationException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.PromiseImpl;

import com.datastax.oss.driver.api.core.CqlSession;
import com.sun.identity.shared.debug.Debug;

/**
 * Responsible for generating ConnectionFactory instances. The instances
 * generated are tailored to the {@link ConnectionType} required by the caller.
 * <p>
 * This factory provider is aware of two main use cases for the service
 * management layer (also known as Data Layer).
 * <p>
 * Default - Uses the service management configuration for connections. This
 * will connect to the defined LDAP server, whether that is embedded or
 * external.
 * <p>
 * External - Uses CTS Configuration for CTS connections which are pointed
 * towards an external LDAP server. Uses service management configuration for
 * {@link StoreMode#DEFAULT} connections.
 */
@Singleton
public class ConnectionFactoryProvider implements org.forgerock.openam.sm.datalayer.providers.ConnectionFactoryProvider<CqlSession> {
	public static String profile="cts";
	// Injected
	private final TimeoutConfig timeoutConfig;
	private final ConnectionConfigFactory configFactory;
//	private final DataLayerConfiguration dataLayerConfiguration;

	private final Debug debug;
	private final ConnectionType connectionType;

	/**
	 * Generates an instance and registers the shutdown listener.
	 *
	 * @param connectionType
	 *            Required, non null.
	 * @param connectionConfigFactory
	 *            Required to resolve configuration parameters, non null.
	 * @param dataLayerConfiguration
	 *            Required, non null.            
	 * @param timeoutConfig
	 *            Timeout Configuration, Non null.
	 * @param debug
	 *            Required for debugging.
	 */
	@Inject
	public ConnectionFactoryProvider(ConnectionType connectionType, ConnectionConfigFactory connectionConfigFactory, DataLayerConfiguration dataLayerConfiguration,TimeoutConfig timeoutConfig, @Named(DataLayerConstants.DATA_LAYER_DEBUG) Debug debug) {
		this.configFactory = connectionConfigFactory;
		this.timeoutConfig = timeoutConfig;
		this.debug = debug;
		this.connectionType = connectionType;
//		this.dataLayerConfiguration=dataLayerConfiguration;
	}

	static ConnectionFactory connectionFactory=null;
	/**
	 * Creates instances of ConnectionFactory which are aware of the need to
	 * share the DataLayer and CTS connections in the same connection pool.
	 *
	 * @return {@inheritDoc}
	 */
	public org.forgerock.openam.sm.datalayer.api.ConnectionFactory<CqlSession> createFactory() throws InvalidConfigurationException {
		if (connectionFactory==null) {
			synchronized (this.getClass()) {
				if (connectionFactory==null) {
					final ConnectionConfig config = configFactory.getConfig(connectionType);
					final int timeout = timeoutConfig.getTimeout(connectionType);
			
					debug("Creating Embedded Factory:\nURL: {0}\nMax Connections: {1}\nHeartbeat: {2}\nOperation Timeout: {3}", config.getLDAPURLs(), config.getMaxConnections(), config.getLdapHeartbeat(), timeout);
			
//					final String keyspace=dataLayerConfiguration.getKeySpace();
					
			//		final String username=config.getBindDN();
			//		final String password=new String(config.getBindPassword());
					
//					CqlSessionBuilder builder=CqlSession.builder()
//							.withApplicationName("OpenAM CTS: "+keyspace)
//							.withConfigLoader(DriverConfigLoader.fromDefaults(Repo.class.getClassLoader()));
			//		if (StringUtils.isNotBlank(username)&&StringUtils.isNotBlank(password)) {
			//			builder=builder.withAuthCredentials(username, password);
			//		}
			//		if (config.getLDAPURLs()!=null) {
			//			for (LDAPURL ldap : config.getLDAPURLs()) {
			//				try {
			//					builder=builder.addContactPoint(new InetSocketAddress(ldap.getHost(),SystemProperties.getAsInt("cassandra.native_transport_port", ldap.getPort())));
			//				}catch (Throwable e) {
			//					debug.error("bad address {}: {}",ldap,e.getMessage());
			//				}
			//			}
			//		}
					connectionFactory=new ConnectionFactory();
				}
			}
		}
		return connectionFactory;
	}

	private void debug(String format, Object... args) {
		if (debug.messageEnabled())
			debug.message(MessageFormat.format(CoreTokenConstants.DEBUG_ASYNC_HEADER + format, args));
	}

	public static class ConnectionFactory implements org.forgerock.openam.sm.datalayer.api.ConnectionFactory<CqlSession> {
		
		public ConnectionFactory() {
		}

		@Override
		public Promise<CqlSession, DataLayerException> createAsync() {
			final Promise<CqlSession, DataLayerException> promise = PromiseImpl.create();
			try {
				((PromiseImpl<CqlSession, DataLayerException>) promise).tryHandleResult(create());
			} catch (Throwable e) {
				((PromiseImpl<CqlSession, DataLayerException>) promise).tryHandleException(new DataLayerException("session is null"));
			}
			return promise;
		}

		static CqlSession session=null;
		@Override
		public CqlSession create() throws DataLayerException {
			if (session==null) {
				synchronized (this.getClass()) {
					if (session==null) {
						session=Cluster.getSession(); 
					}
				}
			}
			return session;
		}

		@Override
		public void close() {
			if (session!=null && !session.isClosed()) {
				session=null;
			}
		}

		@Override
		public boolean isValid(CqlSession session) {
			return session != null && !session.isClosed();
		}
	}
}
