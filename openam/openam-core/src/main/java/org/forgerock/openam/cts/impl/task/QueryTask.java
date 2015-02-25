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
 * Copyright 2014 ForgeRock AS.
 */
package org.forgerock.openam.cts.impl.task;

import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.LDAPAdapter;
import org.forgerock.openam.cts.impl.query.FilterConversion;
import org.forgerock.openam.cts.impl.query.QueryFactory;
import org.forgerock.openam.cts.impl.queue.ResultHandler;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.util.Reject;

import java.text.MessageFormat;
import java.util.Collection;

/**
 * Responsible for querying the persistence store for matching Tokens.
 *
 * @see PartialQueryTask
 */
public class QueryTask implements Task {
    private final FilterConversion conversion;
    private final TokenFilter tokenFilter;
    private final ResultHandler<Collection<Token>> handler;
    private final QueryFactory factory;

    /**
     * @param factory Non null.
     * @param conversion Non null.
     * @param tokenFilter Non null and must not define any Return Attributes.
     * @param handler Non null, required for asynchronous response.
     */
    public QueryTask(QueryFactory factory, FilterConversion conversion,
                     TokenFilter tokenFilter, ResultHandler<Collection<Token>> handler) {
        this.conversion = conversion;
        this.tokenFilter = tokenFilter;
        this.handler = handler;
        this.factory = factory;
    }

    /**
     * Perform the query using the provided LDAPAdapter.
     *
     * The ResultHandler is able to receive a return type of either Tokens
     * or PartialTokens and so the value passed to the ResultHandler will
     * depend on the kind of query requested.
     *
     * @see org.forgerock.openam.cts.api.filter.TokenFilter#getReturnFields()
     *
     * @param connection LDAP Connection to use.
     * @param ldapAdapter LDAP utility functions to perform the task with.
     * @throws CoreTokenException If there was any error during the query.
     * @throws IllegalArgumentException If the TokenFilter provided defined any return fields.
     */
    @Override
    public void execute(Connection connection, LDAPAdapter ldapAdapter) throws CoreTokenException {
        Reject.ifFalse(tokenFilter.getReturnFields().isEmpty());

        Filter ldapFilter = conversion.convert(tokenFilter);
        // Perform the query.
        try {
            // Process Query and return Collection<Tokens>
            handler.processResults(factory.createInstance()
                    .withFilter(ldapFilter)
                    .execute(connection));
        } catch (CoreTokenException e) {
            handler.processError(e);
            throw e;
        }
    }

    @Override
    public String toString() {
        return MessageFormat.format("QueryTask: {0}", tokenFilter);
    }
}
