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
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Host;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;


public class ExecuteCallback implements FutureCallback<ResultSet> {
	final static Logger logger=LoggerFactory.getLogger(ExecuteCallback.class.getName());
	
	final Session session;
	final Statement statement;
	Long start;
	
	public ExecuteCallback(Session session,Statement statement){
		this.session=session;
		this.statement=statement;
		if (statement.getConsistencyLevel()==null)
			statement.setConsistencyLevel(session.getCluster().getConfiguration().getQueryOptions().getConsistencyLevel());
	}
	
	public ResultSet execute(){
		start=System.currentTimeMillis();
		try{
			final ResultSet result=session.execute(statement);
			onSuccess(result);
			return result;
		}catch(Throwable e){
			onFailure(e);
			throw e;
		}

	}
	
	public ResultSetFuture executeAsync(){
		start=System.currentTimeMillis();
		final ResultSetFuture future = session.executeAsync(statement);
		Futures.addCallback(future, this,MoreExecutors.directExecutor());
		//Futures.addCallback(future,this, ForkJoinPool.commonPool());
		return future;
	}

	@Override 
	public void onSuccess(ResultSet result) {
		if (logger.isTraceEnabled())
			logger.trace("{} ms {} {} ({}) {}->{} {}"
					,System.currentTimeMillis()-start
					,statement
					,statement.getConsistencyLevel()==null?session.getCluster().getConfiguration().getQueryOptions().getConsistencyLevel():statement.getConsistencyLevel()
					,result.getAvailableWithoutFetching()
					,result.getExecutionInfo().getQueriedHost()
					,result.getExecutionInfo().getTriedHosts(),getStat(session)
			);
	}
	
	@Override 
	public void onFailure(Throwable t) {
		logger.warn("{} ms {} {}: {} {}"
				,System.currentTimeMillis()-start
				,statement
				,statement.getConsistencyLevel()==null?session.getCluster().getConfiguration().getQueryOptions().getConsistencyLevel():statement.getConsistencyLevel()
				,t.getMessage(),getStat(session));
	}
	
	public static Map<String,String> getStat(Session session){ 
		final Map<String,String> res=new TreeMap<String,String>(String.CASE_INSENSITIVE_ORDER);
		Session.State state = session.getState();
        for (Host host : state.getConnectedHosts())
//        	final HostDistance distance = session.getCluster().getConfiguration().getPolicies().getLoadBalancingPolicy().distance(host);
//        	final int connections = state.getOpenConnections(host);
//        	final int maxConnections =session.getCluster().getConfiguration().getPoolingOptions().getMaxConnectionsPerHost(distance);
//        	final int maxQueries=session.getCluster().getConfiguration().getPoolingOptions().getMaxRequestsPerConnection(distance);
        	res.put(host.toString(), MessageFormat.format("{0}/{1}", state.getOpenConnections(host),state.getInFlightQueries(host)));
        return res;
	}
}
