package org.openidentityplatform.openam.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;

public class Cluster {

	static CqlSession session=CqlSession.builder()
			.withApplicationName("OpenAM")
			.withConfigLoader(DriverConfigLoader.fromDefaults(Repo.class.getClassLoader()))
			.build();

	public static CqlSession getSession() {
		return session;
	}

}
