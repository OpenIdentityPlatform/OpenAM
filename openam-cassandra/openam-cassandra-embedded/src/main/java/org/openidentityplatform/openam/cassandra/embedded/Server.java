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

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

public class Server implements Runnable, Closeable {
	Session session=null;
	public void run() {
		try {
			if (System.getProperty("cassandra.native_transport_port")==null)
				System.setProperty("cassandra.native_transport_port","9042");
			EmbeddedCassandraServerHelper.startEmbeddedCassandra();//EmbeddedCassandraServerHelper.DEFAULT_CASSANDRA_YML_FILE,System.getProperty("java.io.tmpdir")+"/embeddedCassandra");
			session = new Cluster.Builder().withPort(EmbeddedCassandraServerHelper.getNativeTransportPort()).addContactPoints("127.0.0.1").build().connect();
			CQLDataLoader dataLoader = new CQLDataLoader(session);
			String dataSetLocation=System.getProperty(Server.class.getPackage().getName()+".import","test.cql");
			dataLoader.load(new File(dataSetLocation).exists()?new FileCQLDataSet(dataSetLocation): new ClassPathCQLDataSet(dataSetLocation));
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
		if (session!=null)
			session.close();
	}
	
	public static void main(String[] args) {
		Server s=new Server();
		s.run();
		s.close();
		System.exit(0);
		Thread.currentThread().interrupt();
	}

}
