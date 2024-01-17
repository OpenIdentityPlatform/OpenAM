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

import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.openam.cts.api.fields.CoreTokenFieldTypes;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.continuous.ContinuousQuery;
import org.forgerock.openam.cts.continuous.ContinuousQueryListener;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.api.LdapOperationFailedException;
import org.forgerock.openam.sm.datalayer.api.OptimisticConcurrencyCheckFailedException;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.tokens.TokenType;
import org.forgerock.util.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.*;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.querybuilder.relation.Relation;
import com.datastax.oss.driver.api.querybuilder.select.Select;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class TokenStorageAdapter implements org.forgerock.openam.sm.datalayer.api.TokenStorageAdapter {
	final static Logger logger = LoggerFactory.getLogger(TokenStorageAdapter.class);

	private final DataLayerConfiguration cfg;
	static ConnectionFactory<CqlSession> connectionFactory;

	static PreparedStatement static_statement_read;
	static PreparedStatement static_statement_delete;
	static PreparedStatement static_statement_update;
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Inject
	public TokenStorageAdapter(DataLayerConfiguration dataLayerConfiguration,ConnectionFactory connectionFactoryIn) throws DataLayerException {
		this.cfg = dataLayerConfiguration;
		connectionFactory = connectionFactoryIn;
	}

	PreparedStatement get_statement_read() throws DataLayerException {
		if (static_statement_read==null) {
			static_statement_read=getSession().prepare("select * from \""+cfg.getKeySpace()+"\".\""+cfg.getTableName()+"\" where "+CoreTokenField.TOKEN_ID.toString()+"=:coreTokenId limit 1");
		}
		return static_statement_read;
	}
	
	PreparedStatement get_statement_delete() throws DataLayerException {
		if (static_statement_delete==null) {
			static_statement_delete=getSession().prepare("delete from \""+cfg.getKeySpace()+"\".\""+cfg.getTableName()+"\" where "+CoreTokenField.TOKEN_ID.toString()+"=:coreTokenId");
		}
		return static_statement_delete;
	}
	
	PreparedStatement get_statement_update() throws DataLayerException {
		if (static_statement_update==null) {
			static_statement_update=getSession().prepare("update \""+cfg.getKeySpace()+"\".\""+cfg.getTableName()+"\" using ttl :ttl set coreTokenDate01=:coreTokenDate01,coreTokenDate02=:coreTokenDate02,coreTokenDate03=:coreTokenDate03,coreTokenDate04=:coreTokenDate04,coreTokenDate05=:coreTokenDate05,coreTokenExpirationDate=:coreTokenExpirationDate,coreTokenInteger01=:coreTokenInteger01,coreTokenInteger02=:coreTokenInteger02,coreTokenInteger03=:coreTokenInteger03,coreTokenInteger04=:coreTokenInteger04,coreTokenInteger05=:coreTokenInteger05,coreTokenInteger06=:coreTokenInteger06,coreTokenInteger07=:coreTokenInteger07,coreTokenInteger08=:coreTokenInteger08,coreTokenInteger09=:coreTokenInteger09,coreTokenInteger10=:coreTokenInteger10,coreTokenObject=:coreTokenObject,coreTokenString01=:coreTokenString01,coreTokenString02=:coreTokenString02,coreTokenString03=:coreTokenString03,coreTokenString04=:coreTokenString04,coreTokenString05=:coreTokenString05,coreTokenString06=:coreTokenString06,coreTokenString07=:coreTokenString07,coreTokenString08=:coreTokenString08,coreTokenString09=:coreTokenString09,coreTokenString10=:coreTokenString10,coreTokenString11=:coreTokenString11,coreTokenString12=:coreTokenString12,coreTokenString13=:coreTokenString13,coreTokenString14=:coreTokenString14,coreTokenString15=:coreTokenString15,coreTokenMultiString01=:coreTokenMultiString01,coreTokenMultiString02=:coreTokenMultiString02,coreTokenMultiString03=:coreTokenMultiString03,coreTokenType=:coreTokenType,coreTokenUserId=:coreTokenUserId,etag=:etag,createTimestamp=:createTimestamp where coreTokenId=:coreTokenId");
		}
		return static_statement_update;
	}
	
	public Token update(Token token, boolean ifExists) throws DataLayerException {
		try {
			BoundStatement statement=get_statement_update().bind().setInt("ttl", new Long(Math.min(((token.getExpiryTimestamp().getTimeInMillis() - System.currentTimeMillis()) / 1000)+5*60,24*60*60)).intValue());
			for (CoreTokenField field : CoreTokenField.values()) {
				Object value = null;
				try {
					value = token.getAttribute(field);
				}catch (Throwable e) {
					logger.warn("create {} for {} {}",e.toString(),field,token);
					throw e;
				}
				if (value!=null) {
					if (value instanceof TokenType) {
						statement=statement.setString(field.toString(), value.toString());
					}else if (CoreTokenFieldTypes.isCalendar(field)) {
						statement=statement.setInstant(field.toString(), ((Calendar) value).toInstant());
					}else if (value instanceof byte[]) {
						statement=statement.setByteBuffer(field.toString(), ByteBuffer.wrap((byte[]) value));
					}else if (value instanceof String) {
						statement=statement.setString(field.toString(), (String)value);
					}else if (value instanceof Integer) {
						statement=statement.setInt(field.toString(), (Integer)value);
					}
				}
			}
			if (!ifExists) {
				new ExecuteCallback(ConnectionFactoryProvider.profile,getSession(), statement).execute();
			}else {
				new ExecuteCallback(ConnectionFactoryProvider.profile,getSession(), statement).executeAsync();
			}
		} catch (Throwable e) {
			throw new DataLayerException("update", e);
		}
		return token;
	}
	
    public Token update(Token previous, Token token, Options options) throws DataLayerException {
    	return update(token, true);
    }

    /**
     * Create the Token in the database.
     *
     * @param token Non null Token to create.
     * @param options Non null Options for the operations.
     * @return token The instance of the newly created token.
     *               The newly created token would contain the additional etag information.
     * @throws org.forgerock.openam.sm.datalayer.api.DataLayerException If the operation failed for a known reason.
     */
	public Token create(Token token, Options options) throws DataLayerException {
		return update(token, false);
	}

	/**
     * Performs a read against the LDAP connection and converts the result into a Token.
     * 
     * @param tokenId The non null Token ID to read.
     * @param options The non null Options for the operation.
     * @return Token if found, otherwise null.
     */
    public Token read(String tokenId, Options options) throws DataLayerException {
		try{
			final Row row=new ExecuteCallback(ConnectionFactoryProvider.profile,getSession(),get_statement_read().bind().setString(CoreTokenField.TOKEN_ID.toString(), tokenId)).execute().one();
			return row==null?null:Row2Token(row);
	    }catch(Throwable e){
			throw new DataLayerException("read", e);
		}
    }
    
    /**
     * Performs a delete against the Token ID provided.
     *
     * @param tokenId The non null Token ID to delete.
     * @param options The non null Options for the operation.
     * @return A {@link PartialToken} containing at least the {@link CoreTokenField#TOKEN_ID}.
     * @throws LdapOperationFailedException If the operation failed, this exception will capture the reason.
     * @throws OptimisticConcurrencyCheckFailedException If the operation failed due to an assertion on the tokens ETag.
     */
	public PartialToken delete(String tokenId, Options options) throws DataLayerException {
		PartialToken ptoken=null;
		try {
			Token token = read(tokenId,options);
			if (token != null) {
				new ExecuteCallback(ConnectionFactoryProvider.profile,getSession(),get_statement_delete().bind().setString(CoreTokenField.TOKEN_ID.toString(), tokenId)).execute();
				
				final Map<CoreTokenField, Object> entry=new HashMap<CoreTokenField, Object>();
				entry.put(CoreTokenField.TOKEN_ID, token.getAttribute(CoreTokenField.TOKEN_ID));
				ptoken=new PartialToken(entry);
			}
		} catch (Throwable e) {
			throw new DataLayerException("delete", e);
		}
		return ptoken;
	}

	static Cache<String, PreparedStatement> preparedCache=CacheBuilder.newBuilder()
			.expireAfterAccess(15, TimeUnit.MINUTES)
			.maximumSize(1024*10)
			.build();
	
	PreparedStatement getPreparedStatement(SimpleStatement statement) throws ExecutionException {
		return preparedCache.get(statement.getQuery(), 
    			new Callable<PreparedStatement>() {
					@Override
					public PreparedStatement call() throws Exception {
						logger.debug("add prepared: {}",statement.getQuery());
						return getSession().prepare(statement);
					}
				}
    		);
	}
	
    /**
     * Performs a full-token query using the provided filter.
     *
     * @param query The non null filter specification.
     * @throws DataLayerException If the operation failed, this exception will capture the reason.
     */
	@Override
	public Collection<Token> query(TokenFilter query) throws DataLayerException {
		final Collection<Token> res = new ArrayList<Token>();
		try {
			final Filter filter=query.getQuery().accept(new org.openidentityplatform.openam.cassandra.QueryFilterVisitor(),null);
			Select select=selectFrom(cfg.getKeySpace(),filter.getTable()).all();
    		for(Relation clause : filter.clauses) { 
    			select=select.where(clause);
    		}
    		if (filter.allowFilter()) {
    			select=select.allowFiltering();
    		}
    		if (query.getSizeLimit()>0) {
    			select=select.limit(query.getSizeLimit());
    		}
    		BoundStatement statement=getPreparedStatement(select.build()).bind();
    		for (Entry<String, Object> field2value : filter.field2value.entrySet()) {
    			final Object value=field2value.getValue();
    			if (value!=null) {
					if (value instanceof TokenType) {
						statement=statement.setString(field2value.getKey(), value.toString());
					}else if (value instanceof Calendar) {
						statement=statement.setInstant(field2value.getKey(), ((Calendar) value).toInstant());
					}else if (value instanceof byte[]) {
						statement=statement.setByteBuffer(field2value.getKey(), ByteBuffer.wrap((byte[]) value));
					}else if (value instanceof String) {
						statement=statement.setString(field2value.getKey(), (String)value);
					}else if (value instanceof Integer) {
						statement=statement.setInt(field2value.getKey(), (Integer)value);
					}
				}
			}
    		if (query.getTimeLimit().getValue()>0 && query.getTimeLimit().to(TimeUnit.MILLISECONDS)<=Integer.MAX_VALUE) {
    			statement=statement.setTimeout(Duration.ofMillis(query.getTimeLimit().to(TimeUnit.MILLISECONDS)));
    		}
    		for(final Row row: new ExecuteCallback(ConnectionFactoryProvider.profile,getSession(),statement).execute()) {
    			res.add(Row2Token(row));
    		}
		} catch (Throwable e) {
			throw new DataLayerException(MessageFormat.format("query {0}",query), e);
		}
		return res;
	}

    /**
     * Performs a partial query using the provided filter.
     *
     * @param query The non null filter specification.
     * @throws DataLayerException If the operation failed, this exception will capture the reason.
     */
	@SuppressWarnings("unchecked")
	@Override
	public Collection<PartialToken> partialQuery(TokenFilter query) throws DataLayerException {
		//TokenFilter: Filter: [(coreTokenType eq "OAUTH_BLACKLIST" and coreTokenDate01 ge "java.util.GregorianCalendar[time=1511886009211,areFieldsSet=true,areAllFieldsSet=true,lenient=true,zone=sun.util.calendar.ZoneInfo[id="UTC",offset=0,dstSavings=0,useDaylight=false,transitions=0,lastRule=null],firstDayOfWeek=1,minimalDaysInFirstWeek=1,ERA=1,YEAR=2017,MONTH=10,WEEK_OF_YEAR=48,WEEK_OF_MONTH=5,DAY_OF_MONTH=28,DAY_OF_YEAR=332,DAY_OF_WEEK=3,DAY_OF_WEEK_IN_MONTH=4,AM_PM=1,HOUR=4,HOUR_OF_DAY=16,MINUTE=20,SECOND=9,MILLISECOND=211,ZONE_OFFSET=0,DST_OFFSET=0]" and ! (coreTokenString01 eq "01"))] Attributes: coreTokenExpirationDate,coreTokenId,
		if (StringUtils.contains(query.toString(), "OAUTH_BLACKLIST"))
			return Collections.EMPTY_LIST; 
		final Collection<PartialToken> res = new ArrayList<PartialToken>();
		try {
			final Set<CoreTokenField> requestedCoreTokenFields=query.getReturnFields();
			final Set<String> requestedAttributes=new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
			for (CoreTokenField tokenField : requestedCoreTokenFields) 
				requestedAttributes.add(tokenField.toString());
			requestedAttributes.add("coreTokenId");
			final Filter filter=query.getQuery().accept(new org.openidentityplatform.openam.cassandra.QueryFilterVisitor(),null);
    		Select select=selectFrom(cfg.getKeySpace(),filter.getTable()).columns(requestedAttributes.toArray(new String[0]));
    		for(Relation clause : filter.clauses) { 
    			select=select.where(clause);
    		}
    		if (filter.allowFilter()) {
    			select=select.allowFiltering();
    		}
    		BoundStatement statement=getPreparedStatement(select.build()).bind();
    		for (Entry<String, Object> field2value : filter.field2value.entrySet()) {
    			final Object value=field2value.getValue();
    			if (value!=null) {
					if (value instanceof TokenType) {
						statement=statement.setString(field2value.getKey(), value.toString());
					}else if (value instanceof Calendar) {
						statement=statement.setInstant(field2value.getKey(), ((Calendar) value).toInstant());
					}else if (value instanceof byte[]) {
						statement=statement.setByteBuffer(field2value.getKey(), ByteBuffer.wrap((byte[]) value));
					}else if (value instanceof String) {
						statement=statement.setString(field2value.getKey(), (String)value);
					}else if (value instanceof Integer) {
						statement=statement.setInt(field2value.getKey(), (Integer)value);
					}
				}
			}
    		if (query.getTimeLimit().getValue()>0 && query.getTimeLimit().to(TimeUnit.MILLISECONDS)<=Integer.MAX_VALUE) {
    			statement=statement.setTimeout(Duration.ofMillis(query.getTimeLimit().to(TimeUnit.MILLISECONDS)));
    		}
    		for(final Row row: new ExecuteCallback(ConnectionFactoryProvider.profile,getSession(),statement).execute()) {
    			res.add(Row2ParitalToken(requestedCoreTokenFields,row));
    		}
		} catch (Throwable e) {
			throw new DataLayerException(MessageFormat.format("partialQuery {0}", query), e);
		}
		return res;
	}

	Token Row2Token(Row row) {
		final Token res = new Token(row.getString(CoreTokenField.TOKEN_ID.toString()), TokenType.valueOf(row.getString(CoreTokenField.TOKEN_TYPE.toString())));
		for (CoreTokenField field : CoreTokenField.values()) {
			Object value = null;
			if (CoreTokenField.TOKEN_TYPE.equals(field)) {
				continue;
			}else if (CoreTokenFieldTypes.isCalendar(field)) {
				final Instant d = row.getInstant(field.toString());
				if (d != null) {
					value = Calendar.getInstance();
					((Calendar) value).setTimeInMillis(Date.from(d).getTime());
				}
			} else if (CoreTokenFieldTypes.isByteArray(field)) {
				final ByteBuffer bytes = row.getByteBuffer(field.toString());
				if(bytes != null) {
					value = bytes.array();
				}
			}
			else if (CoreTokenFieldTypes.isInteger(field)) {
				value = row.getInt(field.toString());
			}else if (CoreTokenFieldTypes.isString(field)) {
				value = row.getString(field.toString());
			}else {
				throw new IllegalStateException();
			}
			if (value != null && !Token.isFieldReadOnly(field)) {
				res.setAttribute(field, value);
			}
		}
		return res;
	}

	public static PartialToken Row2ParitalToken(Set<CoreTokenField> fields, Row row) {
		final Map<CoreTokenField, Object> res = new HashMap<CoreTokenField, Object>();
		for (CoreTokenField field : fields) {
			Object value = null;
			if (CoreTokenFieldTypes.isCalendar(field)) {
				final Instant d = row.getInstant(field.toString());
				if (d != null) {
					value = Calendar.getInstance();
					((Calendar) value).setTimeInMillis(Date.from(d).getTime());
				}
			} else if (CoreTokenFieldTypes.isByteArray(field)) {
				value = row.getByteBuffer(field.toString()).array();
			}else if (CoreTokenFieldTypes.isInteger(field)) {
				value = row.getInt(field.toString());
			}else if (CoreTokenFieldTypes.isString(field)) {
				value = row.getString(field.toString());
			}else {
				throw new IllegalStateException();
			}
			if (value != null ) {
				res.put(field, value);
			}
		}
		return new PartialToken(res);
	}

    /**
     * Performs a continuous query using the provided filter.
     *
     * @param filter The non null filter specification.
     * @throws DataLayerException If the operation failed, this exception will capture the reason.
     */
	@SuppressWarnings("rawtypes")
	@Override
	public ContinuousQuery startContinuousQuery(TokenFilter filter, ContinuousQueryListener listener) throws DataLayerException {
		logger.debug("startContinuousQuery {} {} not implemented",filter,listener);
		return new ContinuousQuery() {
			@Override
			public void stopQuery() {
			}
			@Override
			public void startQuery() throws DataLayerException {
			}
			@Override
			public ContinuousQuery removeContinuousQueryListener(ContinuousQueryListener listener) {
				return this;
			}
			@Override
			public ContinuousQuery addContinuousQueryListener(ContinuousQueryListener listener) {
				return this;
			}
		};
	}
	
	CqlSession getSession() throws DataLayerException {
		return connectionFactory.create();
	}
}

