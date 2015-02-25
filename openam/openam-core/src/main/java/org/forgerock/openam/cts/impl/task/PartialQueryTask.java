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
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.LDAPAdapter;
import org.forgerock.openam.cts.impl.query.FilterConversion;
import org.forgerock.openam.cts.impl.query.PartialToken;
import org.forgerock.openam.cts.impl.query.QueryFactory;
import org.forgerock.openam.cts.impl.queue.ResultHandler;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.util.Reject;

import java.text.MessageFormat;
import java.util.Collection;

/**
 * Performs a partial query against LDAP. Partial queries operate like normal queries
 * except that the results are not full Token instances. Instead they are a collection
 * of Name/Value pairs which represent a subset of a Token.
 *
 * @see QueryTask
 */
public class PartialQueryTask implements Task {
    private final ResultHandler<Collection<PartialToken>> handler;
    private final TokenFilter tokenFilter;
    private final QueryFactory factory;
    private final FilterConversion conversion;

    /**
     * @param factory Non null.
     * @param conversion Non null.
     * @param tokenFilter Non null and must define at least one Return Attribute.
     * @param handler Non null, required for asynchronous response.
     */
    public PartialQueryTask(QueryFactory factory, FilterConversion conversion,
                            TokenFilter tokenFilter, ResultHandler<Collection<PartialToken>> handler) {
        this.factory = factory;
        this.conversion = conversion;
        this.handler = handler;
        this.tokenFilter = tokenFilter;
    }

    /**
     *
     * @param connection LDAP Connection to use.
     * @param ldapAdapter LDAP utility functions to perform the task with.
     * @throws CoreTokenException
     * @throws IllegalArgumentException If the TokenFilter did not define any return fields.
     */
    @Override
    public void execute(Connection connection, LDAPAdapter ldapAdapter) throws CoreTokenException {
        Reject.ifTrue(tokenFilter.getReturnFields().isEmpty());

        Filter ldapFilter = conversion.convert(tokenFilter);
        // Perform the query.
        try {
            handler.processResults(factory.createInstance()
                    .withFilter(ldapFilter)
                    .returnTheseAttributes(tokenFilter.getReturnFields())
                    .executeAttributeQuery(connection));
        } catch (CoreTokenException e) {
            handler.processError(e);
            throw e;
        }
    }

    @Override
    public String toString() {
        return MessageFormat.format("PartialQueryTask: {0}", tokenFilter);
    }
}
