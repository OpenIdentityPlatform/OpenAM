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
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.AtomicMonotonicTimestampGenerator;
import com.datastax.driver.core.Cluster;
//import com.datastax.driver.core.ClusterWidePercentileTracker;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.HostDistance;
//import com.datastax.driver.core.PerHostPercentileTracker;
//import com.datastax.driver.core.PercentileTracker;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.ProtocolOptions.Compression;
//import com.datastax.driver.core.ProtocolVersion;
//import com.datastax.driver.core.QueryLogger;
import com.datastax.driver.core.QueryOptions;
//import com.datastax.driver.core.policies.ConstantSpeculativeExecutionPolicy;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.datastax.driver.core.policies.DowngradingConsistencyRetryPolicy;
import com.datastax.driver.core.policies.LatencyAwarePolicy;
import com.datastax.driver.core.policies.LoggingRetryPolicy;
//import com.datastax.driver.core.policies.PercentileSpeculativeExecutionPolicy;
//import com.datastax.driver.core.policies.Policies;
import com.datastax.driver.core.policies.RoundRobinPolicy;
import com.datastax.driver.core.policies.TokenAwarePolicy;
import com.iplanet.am.util.SystemProperties;

public class ClusterCache {
	final static Logger logger=LoggerFactory.getLogger(ClusterCache.class.getName());
	static ConcurrentHashMap<String, Cluster> clusters=new ConcurrentHashMap<String, Cluster>();
	
	public static synchronized Cluster getCluster(String[] servers,String user,String password){
		if (StringUtils.isNotBlank(SystemProperties.get("cassandra.hosts")))
			servers=SystemProperties.get("cassandra.hosts").split(",|;");
		Cluster cluster=null;
		for (String server : servers) {
			String key=MessageFormat.format("{0}/{1}/{2}", server,user,password).toLowerCase();
			cluster=clusters.get(key);
			if (cluster!=null)
				return cluster;
		}
		cluster=newCluster(servers,user, password);
		for (String server : servers) 
			clusters.put(MessageFormat.format("{0}/{1}/{2}", server,user,password).toLowerCase(), cluster);
		return cluster;
	}
	
	public static Cluster newCluster(String[] servers,String user,String password){
		logger.info("create cluster {}/{}",user,servers);
		
		final PoolingOptions poolingOptions = new PoolingOptions();
		poolingOptions.setConnectionsPerHost(HostDistance.LOCAL,  1, 1);
		poolingOptions.setConnectionsPerHost(HostDistance.REMOTE, 1, 1);
		poolingOptions.setMaxRequestsPerConnection(HostDistance.LOCAL, 2048);
		poolingOptions.setMaxRequestsPerConnection(HostDistance.REMOTE, 512);
		poolingOptions.setHeartbeatIntervalSeconds(30);
		
		QueryOptions qo=new QueryOptions();
		qo.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
		qo.setMetadataEnabled(true);
		qo.setDefaultIdempotence(true);
		//qo.setFetchSize(128); //default 5000
		// There are more options than shown here, please refer to the API docs
		// for more information
//		PercentileTracker tracker = ClusterWidePercentileTracker
//		    .builder(10*1000)
//		    .build();

		Cluster.Builder builder = Cluster.builder()
				    .withCredentials(user,password)
				    .withPoolingOptions(poolingOptions)
				    .withLoadBalancingPolicy(
	    				LatencyAwarePolicy.builder(
	    						new TokenAwarePolicy(
	    								SystemProperties.get("DC")!=null ? DCAwareRoundRobinPolicy.builder().withLocalDc(SystemProperties.get("DC")).build() : new RoundRobinPolicy()
	    						)
	    				).build()
				     )
				    .withRetryPolicy(new LoggingRetryPolicy(DowngradingConsistencyRetryPolicy.INSTANCE))
				    .withQueryOptions(qo)
				    .withTimestampGenerator(new AtomicMonotonicTimestampGenerator())
//				    .withSpeculativeExecutionPolicy(
////				    		new ConstantSpeculativeExecutionPolicy(
////				            700, // delay before a new execution is launched
////				            3    // maximum number of executions
////				        )
//				    		new PercentileSpeculativeExecutionPolicy(
//							        tracker,
//							        99.0,     // percentile
//							        3)
//				    		)
				    .withCompression(Compression.LZ4);
//					.withCompression(Compression.NONE);
					//.withProtocolVersion(ProtocolVersion.V5);
		//if (System.getProperty("os.arch").contains("sparc")){
//			logger.info("downgrade version V3");
//			builder=builder.withProtocolVersion(ProtocolVersion.V3);
		//}
		for (String address : servers) 
			builder=builder.addContactPoint(address);
		final Cluster cluster=builder.build();
		//cluster.register(QueryLogger.builder().build());
		//cluster.register(tracker);
		return cluster;
	}

}
