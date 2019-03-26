package ru.org.openam.cassandra;

import java.net.InetAddress;
import java.text.MessageFormat;
import java.util.ArrayList;
//import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.ldap.LDAPURL;
import org.forgerock.openam.sm.ConnectionConfig;
import org.forgerock.openam.sm.ConnectionConfigFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.api.StoreMode;
//import org.forgerock.openam.sm.datalayer.utils.ConnectionCount;
import org.forgerock.openam.sm.datalayer.utils.TimeoutConfig;
import org.forgerock.openam.sm.exceptions.InvalidConfigurationException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.PromiseImpl;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.sun.identity.shared.debug.Debug;

/**
 * Responsible for generating ConnectionFactory instances. The instances
 * generated are tailored to the {@link ConnectionType} required by the caller.
 * <p/>
 * This factory provider is aware of two main use cases for the service
 * management layer (also known as Data Layer).
 * <p/>
 * Default - Uses the service management configuration for connections. This
 * will connect to the defined LDAP server, whether that is embedded or
 * external.
 * <p/>
 * External - Uses CTS Configuration for CTS connections which are pointed
 * towards an external LDAP server. Uses service management configuration for
 * {@link StoreMode#DEFAULT} connections.
 */
@Singleton
public class ConnectionFactoryProvider implements org.forgerock.openam.sm.datalayer.providers.ConnectionFactoryProvider<Session> {

	// Injected
	private final TimeoutConfig timeoutConfig;
	private final ConnectionConfigFactory configFactory;

	private final Debug debug;
	private final ConnectionType connectionType;

	/**
	 * Generates an instance and registers the shutdown listener.
	 *
	 * @param connectionConfigFactory
	 *            Required to resolve configuration parameters, non null.
	 * @param timeoutConfig
	 *            Timeout Configuration, Non null.
	 * @param count
	 *            Connection Count logic, Non null.
	 * @param debug
	 *            Required for debugging.
	 */
	@Inject
	public ConnectionFactoryProvider(ConnectionType connectionType, ConnectionConfigFactory connectionConfigFactory, TimeoutConfig timeoutConfig, @Named(DataLayerConstants.DATA_LAYER_DEBUG) Debug debug) {
		this.configFactory = connectionConfigFactory;
		this.timeoutConfig = timeoutConfig;
		this.debug = debug;
		this.connectionType = connectionType;
	}

	/**
	 * Creates instances of ConnectionFactory which are aware of the need to
	 * share the DataLayer and CTS connections in the same connection pool.
	 *
	 * @return {@inheritDoc}
	 */
	Cluster cluster=null;
	public org.forgerock.openam.sm.datalayer.api.ConnectionFactory<Session> createFactory() throws InvalidConfigurationException {
		if (cluster==null) 
			synchronized (this) {
				if (cluster==null) {
					ConnectionConfig config = configFactory.getConfig(connectionType);
					int timeout = timeoutConfig.getTimeout(connectionType);
			
					debug("Creating Embedded Factory:\nURL: {0}\nMax Connections: {1}\nHeartbeat: {2}\nOperation Timeout: {3}", config.getLDAPURLs(), config.getMaxConnections(), config.getLdapHeartbeat(), timeout);
			
					List<String> seed = new ArrayList<String>(config.getLDAPURLs().size());
					for (LDAPURL ldap : config.getLDAPURLs())
						try {
							seed.add(InetAddress.getByName(ldap.getHost()).getHostAddress());
						} catch (Exception e) {
							debug("{0} {1}", ldap.getHost().toString(), e.getMessage());
						}
					if (seed.size() < 1)
						throw new InvalidConfigurationException("server list is empty");
					cluster = ClusterCache.getCluster(seed.toArray(new String[0]), config.getBindDN(), new String(config.getBindPassword()));
				}
			}
		return new ConnectionFactory(cluster);
	}

	private void debug(String format, Object... args) {
		if (debug.messageEnabled())
			debug.message(MessageFormat.format(CoreTokenConstants.DEBUG_ASYNC_HEADER + format, args));
	}

	public static class ConnectionFactory implements org.forgerock.openam.sm.datalayer.api.ConnectionFactory<Session> {
		final Cluster cluster;

		public ConnectionFactory(Cluster cluster) {
			this.cluster = cluster;
		}

		@Override
		public Promise<Session, DataLayerException> createAsync() {
			final Promise<Session, DataLayerException> promise = PromiseImpl.create();
			try {
				((PromiseImpl<Session, DataLayerException>) promise).tryHandleResult(create());
			} catch (Throwable e) {
				((PromiseImpl<Session, DataLayerException>) promise).tryHandleException(new DataLayerException("session is null"));
			}
			return promise;
		}

		Session session=null;
		@Override
		public Session create() throws DataLayerException {
			if (!isValid(session))
				synchronized (this) {
					if (!isValid(session))
						session =  cluster.connect();
				}
			return session;
		}

		@Override
		public void close() {
			if (isValid(session))
				synchronized (this) {
					if (isValid(session))
						session.close();
				}
		}

		@Override
		public boolean isValid(Session session) {
			return session != null && !session.isClosed();
		}

	}
}
