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

package org.openidentityplatform.openam.cassandra.embedded;

import java.io.Closeable;
import java.io.File;

import org.cassandraunit.CQLDataLoader;
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.cassandraunit.dataset.cql.FileCQLDataSet;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;

public class Server implements Runnable, Closeable {
	public void run() {
		try {
			if (System.getProperty("cassandra.native_transport_port")==null)
				System.setProperty("cassandra.native_transport_port","9042");
			EmbeddedCassandraServerHelper.startEmbeddedCassandra();//EmbeddedCassandraServerHelper.DEFAULT_CASSANDRA_YML_FILE,System.getProperty("java.io.tmpdir")+"/embeddedCassandra");
			final com.datastax.oss.driver.api.core.CqlSession session = EmbeddedCassandraServerHelper.getSession();
			final CQLDataLoader dataLoader = new CQLDataLoader(session);
			String dataSetLocation=System.getProperty(Server.class.getPackage().getName()+".import","cassandra/import.cql");
			dataLoader.load(new File(dataSetLocation).exists()?new FileCQLDataSet(dataSetLocation): new ClassPathCQLDataSet(dataSetLocation));
			session.close();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
		Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
            	close();
            }
        });
	}

	public void close() {
		EmbeddedCassandraServerHelper.stopEmbeddedCassandra();
	}
	
	public static void main(String[] args) {
		Server s=new Server();
		s.run();
		s.close();
		System.exit(0);
		Thread.currentThread().interrupt();
	}

}
