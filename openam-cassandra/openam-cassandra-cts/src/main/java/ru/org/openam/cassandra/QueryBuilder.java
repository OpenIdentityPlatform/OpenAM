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
 * Copyright 2015-2016 ForgeRock AS.
 */

package ru.org.openam.cassandra;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;
import org.forgerock.openam.cts.continuous.ContinuousQuery;
import org.forgerock.openam.cts.continuous.ContinuousQueryListener;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.tokens.CoreTokenField;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Select.Where;
import com.google.common.primitives.Ints;
import com.sun.identity.shared.debug.Debug;

import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;

public class QueryBuilder extends org.forgerock.openam.sm.datalayer.api.query.QueryBuilder<Session, Filter> {

	private DataLayerConfiguration cfg;
    private ConnectionFactory<Session> connectionFactory;

    /**
     * Default constructor ensures the Object Class is defined.
     *
     * @param dataLayerConfiguration Required for data store dataLayerConfiguration.
     * @param debug To debug writer for this class.
     * @param connectionFactoryProvider A producer of factories used to communicate down to the data layer with.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	@Inject
    public QueryBuilder(DataLayerConfiguration cfg, 
                            @Named(DataLayerConstants.DATA_LAYER_DEBUG) Debug debug,
                            ConnectionFactory connectionFactory
                            ) {
        super(debug);
        this.cfg=cfg;
        this.connectionFactory = connectionFactory;
    }

    /**
     * Perform the query and return the results as Entry instances.
     *
     * @param connection The connection used to perform the request.
     * @return A non null but possibly empty collection.
     */
    @SuppressWarnings("unchecked")
	public <T> Iterator<Collection<T>> executeRawResults(Session connection, Class<T> returnType) {
    		debug.message("executeRawResults {}",this);
    		try {
    			Set<String> requestedAttributes=new HashSet<String>(Arrays.asList(this.requestedAttributes));
    			requestedAttributes.add("coreTokenId");
    			final Set<CoreTokenField> requestedCoreTokenFields=new HashSet<CoreTokenField>(requestedAttributes.size());
    			for (String requestedAttribute : requestedAttributes) 
    				requestedCoreTokenFields.add(CoreTokenField.fromLDAPAttribute(requestedAttribute));
	    		Where where=com.datastax.driver.core.querybuilder.QueryBuilder
	    			.select(requestedAttributes.toArray(new String[0]))
	    			.from(cfg.getKeySpace(), this.filter.getTable())
	    			.where();
	    		for(Clause clause : this.filter.clauses)
	    			where=where.and(clause);
	    		Select select=where.allowFiltering();
	    		if (this.sizeLimit>0)
	    			select=select.limit(this.sizeLimit);
	    		if (this.pageSize>0)
	    			select=(Select)select.setFetchSize(this.pageSize);
	    		if (this.timeLimit.getValue()>0 && this.timeLimit.to(TimeUnit.MILLISECONDS)<=Integer.MAX_VALUE)
	    			select=(Select)select.setReadTimeoutMillis(Ints.checkedCast(this.timeLimit.to(TimeUnit.MILLISECONDS)));
	    		//final ResultSet rc = new ExecuteCallback(connectionFactory.create(),select).execute();
	    		//final Iterator<Row> iterator=rc.iterator();
	    		final Iterator<Row> iterator=Collections.EMPTY_LIST.iterator(); //disable long query
	    		return new Iterator<Collection<T>>() {
	  				@Override
					public boolean hasNext() {
						return iterator.hasNext();
					}
					@Override
					public Collection<T> next() {
						final Row row=iterator.next();
						List<T> res=new ArrayList<T>(1);
						if (row!=null)
							res.add((T)TokenStorageAdapter.Row2ParitalToken(requestedCoreTokenFields, row));
						return res;
					}
					@Override
					public void remove() {}
				};
    		}catch (Exception e) {
			throw new RuntimeException(e);
		}
    }

    @SuppressWarnings("rawtypes")
	@Override
    public ContinuousQuery executeContinuousQuery(ContinuousQueryListener listener) throws DataLayerException {
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

    /**
     * Presents the QueryBuilder in a human readable format.
     *
     * Note: This function is not performant and should only be used for debugging purposes.
     *
     * @return Non null string representing this QueryBuilder.
     */
    @Override
    public String toString() {
        return MessageFormat.format(
                "Query: " +
                        "      Filter: {0} " +
                        "  Attributes: {1}",
                filter,
                StringUtils.join(requestedAttributes, ", "));
    }
}
