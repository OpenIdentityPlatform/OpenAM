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

import java.util.ArrayList;
import java.util.concurrent.CompletionStage;

import javax.xml.rpc.holders.StringHolder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.ColumnDefinitions;
import com.datastax.oss.driver.api.core.cql.ExecutionInfo;
import com.datastax.oss.driver.api.core.cql.QueryTrace;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.datastax.oss.driver.internal.core.cql.DefaultBatchStatement;
import com.datastax.oss.driver.internal.core.cql.DefaultBoundStatement;
import com.iplanet.am.util.SystemProperties;


public class ExecuteCallback {
	final static Logger logger=LoggerFactory.getLogger(ExecuteCallback.class.getName());
	
	final CqlSession session;
	final Statement<?> statement;
	Long start;
	
	public ExecuteCallback(String profile,CqlSession session,Statement<?>  statement){
		this.session=session;
		this.statement=statement.setExecutionProfileName(profile).setTracing(logger.isTraceEnabled() && SystemProperties.getAsBoolean(ExecuteCallback.class.getPackage().getName().concat(".trace.server"), false));
	}
	
	public ResultSet execute(){
		start=System.currentTimeMillis();
		try{
			final ResultSet result=session.execute(statement);
			onSuccess(result.getExecutionInfo());
			return result;
		}catch(Throwable e){
			onFailure(e);
			throw e;
		}

	}
	
	public CompletionStage<AsyncResultSet> executeAsync(){
		start=System.currentTimeMillis();
		final CompletionStage<AsyncResultSet> sessionStage = session.executeAsync(statement);
		sessionStage.whenComplete(
		    (version, error) -> {
		        if (error != null) {
		          onFailure(error);
		        } else {
		          onSuccess(version.getExecutionInfo());
		        }
		      });
		return sessionStage;
	}


	public void onSuccess(ExecutionInfo result) {
		if (logger.isTraceEnabled() && result.getTracingId()!=null) { 
			final QueryTrace trace=result.getQueryTrace();
			logger.trace("{}μs {} {}",trace.getDurationMicros(),trace.getParameters(),trace.getCoordinatorAddress());
		}else if (logger.isTraceEnabled()){
			logger.trace("{} {} ms {}: {} {}: {}"
					,statement.getExecutionProfileName() 
					,System.currentTimeMillis()-start
					,getKeyspace()
					,debugQuery(statement)
					,statement.getConsistencyLevel()
			);
		}
	}
	
	public void onFailure(Throwable t) {
		logger.warn("{} {} ms {}: {} {}: {}"
				,statement.getExecutionProfileName()
				,System.currentTimeMillis()-start
				,getKeyspace()
				,debugQuery(statement)
				,statement.getConsistencyLevel()
				,t.getMessage()
		);
	}
	
	String getKeyspace() {
		return statement.getKeyspace()==null?session.getKeyspace().get().toString():statement.getKeyspace().toString();
	}
	
	static ArrayList<String> debugQuery(Statement<?>  statement) {
		final ArrayList<String> debugs=new ArrayList<String>();
		if (statement instanceof DefaultBoundStatement) {
			final StringHolder query=new StringHolder(((DefaultBoundStatement)statement).getPreparedStatement().getQuery());
			final ColumnDefinitions cd=((DefaultBoundStatement)statement).getPreparedStatement().getVariableDefinitions();
			cd.forEach(c -> {
				try {
					if (c.getType().asCql(false, false).equals("text") ) {
						query.value=query.value.replace(":"+c.getName().asInternal(),"'"+((DefaultBoundStatement)statement).getString(c.getName())+"'");
					}else if (c.getType().asCql(false, false).equals("int") ) {
						query.value=query.value.replace(":"+c.getName().asInternal(),""+((DefaultBoundStatement)statement).getInt(c.getName()));
					}else if (c.getType().asCql(false, false).equals("timestamp") ) {
						query.value=query.value.replace(":"+c.getName().asInternal(),""+((DefaultBoundStatement)statement).getInstant(c.getName()).toString());
					}else if (c.getType().asCql(false, false).equals("list<text>") ) {
						query.value=query.value.replace(":"+c.getName().asInternal(),""+((DefaultBoundStatement)statement).getList(c.getName(),String.class));
					}else {
						query.value=query.value+" unknown type "+c.getType().asCql(false, false);
					}
				}catch (Throwable e) {
					query.value=query.value+" error "+e.toString();
				}
			});
			debugs.add(query.value);
		}else if (statement instanceof SimpleStatement) {
			debugs.add(((SimpleStatement)statement).getQuery()+" "+((SimpleStatement)statement).getNamedValues());
		}else if (statement instanceof DefaultBatchStatement) {
			((DefaultBatchStatement)statement).forEach(t -> {
				debugs.addAll(debugQuery(t));
				}
			);
			
		}else {
			debugs.add("unknown type: "+statement); 
		}
		return debugs;
	}
}
