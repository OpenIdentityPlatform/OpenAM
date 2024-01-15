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
 * Copyright 2016 ForgeRock AS.
 * Portions Copyrighted 2023 3A Systems LLC.
 */
package org.forgerock.openam.cts.impl.query.worker.queries;

import static org.forgerock.openam.sm.datalayer.api.ConnectionType.CTS_SESSION_IDLE_TIMEOUT_WORKER;
import static org.forgerock.openam.utils.Time.getCalendarInstance;
import static org.forgerock.util.query.QueryFilter.equalTo;
import static org.forgerock.util.query.QueryFilter.lessThanOrEqualTo;

import java.util.Calendar;

import javax.inject.Inject;

import org.forgerock.openam.cts.CoreTokenConfig;
import org.forgerock.openam.cts.api.fields.SessionTokenField;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.sm.datalayer.api.query.QueryBuilder;
import org.forgerock.openam.sm.datalayer.api.query.QueryFactory;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.tokens.TokenType;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.util.Reject;
import org.forgerock.util.query.QueryFilter;

import com.iplanet.dpro.session.service.SessionState;

/**
 * A query that selects all CTS session tokens whose idle time has expired.
 *
 * @param <C> The type of connection queries are made for.
 */
public class SessionIdleTimeExpiredQuery<C> extends CTSWorkerBaseQuery {

    private final QueryFactory<C, Filter> queryFactory;
    private final int pageSize;

    @Inject
    public SessionIdleTimeExpiredQuery(@DataLayer(CTS_SESSION_IDLE_TIMEOUT_WORKER) ConnectionFactory factory,
            @DataLayer(CTS_SESSION_IDLE_TIMEOUT_WORKER) QueryFactory queryFactory, CoreTokenConfig config) {
        super(factory);
        Reject.ifTrue(config.getCleanupPageSize() <= 0);
        this.queryFactory = queryFactory;
        this.pageSize = config.getCleanupPageSize();
    }

    @Override
    public QueryBuilder<C, Filter> getQuery() {
    	final Calendar now = getCalendarInstance();

        QueryFilter<CoreTokenField> filter =
                QueryFilter.and(
                        lessThanOrEqualTo(SessionTokenField.MAX_IDLE_EXPIRATION_TIME.getField(), now),
                        equalTo(SessionTokenField.SESSION_STATE.getField(), SessionState.VALID.toString()),
                        equalTo(CoreTokenField.TOKEN_TYPE, TokenType.SESSION));

        return queryFactory.createInstance()
                .withFilter(filter.accept(queryFactory.createFilterConverter(), null))
                .pageResultsBy(pageSize)
                .returnTheseAttributes(
                        CoreTokenField.TOKEN_ID,
                        CoreTokenField.ETAG,
                        SessionTokenField.SESSION_ID.getField());
    }

}
