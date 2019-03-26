package ru.org.openam.cassandra;

import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Using;
import com.datastax.driver.core.querybuilder.Select.Where;
import com.google.common.primitives.Ints;

public class TokenStorageAdapter implements org.forgerock.openam.sm.datalayer.api.TokenStorageAdapter {
	final static Logger logger = LoggerFactory.getLogger(TokenStorageAdapter.class);

	private final DataLayerConfiguration cfg;
	private final ConnectionFactory<Session> connectionFactory;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Inject
	public TokenStorageAdapter(DataLayerConfiguration dataLayerConfiguration,ConnectionFactory connectionFactory) {
		this.cfg = dataLayerConfiguration;
		this.connectionFactory = connectionFactory;
	}

    public Token update(Token previous, Token updated, Options options) throws DataLayerException {
        return create(updated,options);
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
		try {
			if (token.getAttribute(CoreTokenField.ETAG)==null)
				token.setAttribute(CoreTokenField.ETAG, "");
			Insert insert = QueryBuilder.insertInto(cfg.getKeySpace(), cfg.getTableName());
			final Using TTL = QueryBuilder.ttl(new Long(Math.min(((token.getExpiryTimestamp().getTimeInMillis() - System.currentTimeMillis()) / 1000)+5*60,24*60*60)).intValue());
			for (CoreTokenField field : CoreTokenField.values()) {
				Object value = null;
				try {
					value = token.getAttribute(field);
				}catch (Throwable e) {
					logger.warn("create {} for {} {}",e.toString(),field,token);
					throw e;
				}
				if (value!=null) {
					if (value instanceof TokenType)
						value = value.toString();
					else if (CoreTokenFieldTypes.isCalendar(field))
						value = ((Calendar) value).getTime();
					else if (value instanceof byte[])
						value = ByteBuffer.wrap((byte[]) value);
				}
				insert = insert.value(field.toString(), value);
			}
			new ExecuteCallback(getSession(), insert.using(TTL)).execute();
		} catch (Throwable e) {
			throw new DataLayerException("create", e);
		}
		return token;
	}

	/**
     * Performs a read against the LDAP connection and converts the result into a Token.
     * 
     * @param connection The non null connection to perform this call against.
     * @param tokenId The id of the Token to read.
     * @return Token if found, otherwise null.
     */
    public Token read(String tokenId, Options options) throws DataLayerException {
    		try{
    			for (Row row : new ExecuteCallback(getSession(),QueryBuilder.select().all().from(cfg.getKeySpace(),cfg.getTableName()).where(QueryBuilder.eq(CoreTokenField.TOKEN_ID.toString(), tokenId)).limit(1)).execute()) 
    				return Row2Token(row);
	    }catch(Throwable e){
			throw new DataLayerException("read", e);
		}
    		return null;
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
				new ExecuteCallback(getSession(), QueryBuilder.delete().all().from(cfg.getKeySpace(), cfg.getTableName()).where(QueryBuilder.eq(CoreTokenField.TOKEN_ID.toString(), tokenId))).executeAsync();
				
				final Map<CoreTokenField, Object> entry=new HashMap<CoreTokenField, Object>();
				entry.put(CoreTokenField.TOKEN_ID, token.getAttribute(CoreTokenField.TOKEN_ID));
				ptoken=new PartialToken(entry);
			}
		} catch (Throwable e) {
			throw new DataLayerException("delete", e);
		}
		return ptoken;
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
			final Filter filter=query.getQuery().accept(new ru.org.openam.cassandra.QueryFilterVisitor(),null);
			Where where=com.datastax.driver.core.querybuilder.QueryBuilder
	    			.select()
	    			.from(cfg.getKeySpace(), filter.getTable())
	    			.where();
	    		for(Clause clause : filter.clauses)
	    			where=where.and(clause);
	    		Select select=where.allowFiltering();
	    		if (query.getSizeLimit()>0)
	    			select=select.limit(query.getSizeLimit());
	    		if (query.getTimeLimit().getValue()>0 && query.getTimeLimit().to(TimeUnit.MILLISECONDS)<=Integer.MAX_VALUE)
	    			select=(Select)select.setReadTimeoutMillis(Ints.checkedCast(query.getTimeLimit().to(TimeUnit.MILLISECONDS)));
	    		for(final Row row: new ExecuteCallback(connectionFactory.create(),select).execute())
	    			res.add(Row2Token(row));
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
			final Filter filter=query.getQuery().accept(new ru.org.openam.cassandra.QueryFilterVisitor(),null);
	    		Where where=com.datastax.driver.core.querybuilder.QueryBuilder
	    			.select(requestedAttributes.toArray(new String[0]))
	    			.from(cfg.getKeySpace(), filter.getTable())
	    			.where();
	    		for(Clause clause : filter.clauses)
	    			where=where.and(clause);
	    		Select select=where.allowFiltering();
	    		if (query.getSizeLimit()>0)
	    			select=select.limit(query.getSizeLimit());
	    		if (query.getTimeLimit().getValue()>0 && query.getTimeLimit().to(TimeUnit.MILLISECONDS)<=Integer.MAX_VALUE)
	    			select=(Select)select.setReadTimeoutMillis(Ints.checkedCast(query.getTimeLimit().to(TimeUnit.MILLISECONDS)));
	    		for(final Row row: new ExecuteCallback(connectionFactory.create(),select).execute())
	    			res.add(Row2ParitalToken(requestedCoreTokenFields,row));
		} catch (Throwable e) {
			throw new DataLayerException(MessageFormat.format("partialQuery {0}", query), e);
		}
		return res;
	}

	Token Row2Token(Row row) {
		final Token res = new Token(row.getString(CoreTokenField.TOKEN_ID.toString()), TokenType.valueOf(row.getString(CoreTokenField.TOKEN_TYPE.toString())));
		for (CoreTokenField field : CoreTokenField.values()) {
			Object value = null;
			if (CoreTokenField.TOKEN_TYPE.equals(field))
				continue;
			else if (CoreTokenFieldTypes.isCalendar(field)) {
				Date d = row.getTimestamp(field.toString());
				if (d != null) {
					value = Calendar.getInstance();
					((Calendar) value).setTimeInMillis(d.getTime());
				}
			} else if (CoreTokenFieldTypes.isByteArray(field)) {
				ByteBuffer bytes = row.getBytes(field.toString());
				if(bytes != null)
					value = bytes.array();
			}
			else if (CoreTokenFieldTypes.isInteger(field))
				value = row.getInt(field.toString());
			else if (CoreTokenFieldTypes.isString(field))
				value = row.getString(field.toString());
			else
				throw new IllegalStateException();
			if (value != null && !Token.isFieldReadOnly(field))
				res.setAttribute(field, value);
		}
		return res;
	}

	public static PartialToken Row2ParitalToken(Set<CoreTokenField> fields, Row row) {
		final Map<CoreTokenField, Object> res = new HashMap<CoreTokenField, Object>();
		for (CoreTokenField field : fields) {
			Object value = null;
			if (CoreTokenField.TOKEN_TYPE.equals(field))
				continue;
			else if (CoreTokenFieldTypes.isCalendar(field)) {
				Date d = row.getTimestamp(field.toString());
				if (d != null) {
					value = Calendar.getInstance();
					((Calendar) value).setTimeInMillis(d.getTime());
				}
			} else if (CoreTokenFieldTypes.isByteArray(field))
				value = row.getBytes(field.toString()).array();
			else if (CoreTokenFieldTypes.isInteger(field))
				value = row.getInt(field.toString());
			else if (CoreTokenFieldTypes.isString(field))
				value = row.getString(field.toString());
			else
				throw new IllegalStateException();
			if (value != null )
				res.put(field, value);
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
	
	Session getSession() throws DataLayerException {
		return connectionFactory.create();
	}
}
