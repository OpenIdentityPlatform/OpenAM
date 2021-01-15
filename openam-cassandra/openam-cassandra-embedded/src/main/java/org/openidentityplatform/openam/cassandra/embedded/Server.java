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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.service.CassandraDaemon;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.datastax.oss.driver.api.core.CqlSession;
import com.iplanet.am.util.SystemProperties;

public class Server implements Runnable, Closeable {
	final private ExecutorService executor = Executors.newSingleThreadExecutor();
	private CassandraDaemon cassandraDaemon;
	    
	public void run() {
		try {
			//check for external cassandra settings
			if (SystemProperties.get("datastax-java-driver.basic.load-balancing-policy.local-datacenter")!=null || SystemProperties.get("datastax-java-driver.basic.contact-points.0")!=null) {
				return;
			}
			
			//config
			final File path=new File(System.getProperty("cassandra.storagedir",System.getProperty("java.io.tmpdir")+File.separator+"embeddedCassandra"));
			path.mkdirs();
	        System.setProperty("cassandra-foreground", "true");
	        System.setProperty("cassandra.storagedir", path.getPath());
	        //prepare default keystore
	        if (!Files.exists(Paths.get("target"))) {
	        	Files.createDirectory(Paths.get("target"));
	        }
	        if (!Files.exists(Paths.get("target"+File.separator+"embedded_keystore"))) {
	        	Files.copy(this.getClass().getResourceAsStream("/embedded_keystore"),Paths.get("target"+File.separator+"embedded_keystore"),StandardCopyOption.REPLACE_EXISTING);
	        }
	        
	        //start
	        final CountDownLatch startupLatch = new CountDownLatch(1) ;
	        executor.execute( new Runnable(){
	            @Override
	            public void run() {
	                cassandraDaemon = new CassandraDaemon();
	                cassandraDaemon.activate();
	                startupLatch.countDown();
	            }});
	        if (!startupLatch.await(5, TimeUnit.MINUTES)) {
                throw new AssertionError("Cassandra daemon did not start within timeout");
            }
	        System.setProperty("datastax-java-driver.basic.contact-points.0",DatabaseDescriptor.getRpcAddress().getHostAddress()+":"+DatabaseDescriptor.getNativeTransportPort());
	        System.setProperty("datastax-java-driver.basic.load-balancing-policy.local-datacenter", DatabaseDescriptor.getLocalDataCenter());
	        
	        //load
	        final String dataSetLocation=System.getProperty(Server.class.getPackage().getName()+".import","cassandra/import.cql");
	        final InputStream inputStream=this.getClass().getResourceAsStream("/" + dataSetLocation);
	        if (inputStream==null) {
	        	throw new AssertionError("cannot get resource"+dataSetLocation);
	        }
	        final CqlSession session = CqlSession.builder().withApplicationName("OpenAM Embedded").build();
	        Arrays.stream(StringUtils.normalizeSpace(IOUtils.toString(inputStream,"UTF-8")).split(";")).map(statement -> StringUtils.normalizeSpace(statement) + ";").forEach(session::execute);
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
		if (cassandraDaemon!=null) {
			cassandraDaemon.stop();
			cassandraDaemon=null;
		}
	}
	
	public static void main(String[] args) {
		Server s=new Server();
		s.run();
		s.close();
		System.exit(0);
	}

}
