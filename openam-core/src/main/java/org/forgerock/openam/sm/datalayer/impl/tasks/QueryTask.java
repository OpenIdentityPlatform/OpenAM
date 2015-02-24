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
 * Copyright 2014-2015 ForgeRock AS.
 */
package org.forgerock.openam.sm.datalayer.impl.tasks;

import java.text.MessageFormat;
import java.util.Collection;

import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.api.ResultHandler;
import org.forgerock.openam.sm.datalayer.api.Task;
import org.forgerock.openam.sm.datalayer.api.TokenStorageAdapter;
import org.forgerock.util.Reject;

/**
 * Responsible for querying the persistence store for matching Tokens.
 *
 * @see PartialQueryTask
 */
public class QueryTask implements Task {
    private final TokenFilter tokenFilter;
    private final ResultHandler<Collection<Token>, ?> handler;

    /**
     * @param tokenFilter Non null and must not define any Return Attributes.
     * @param handler Non null, required for asynchronous response.
     */
    public QueryTask(TokenFilter tokenFilter, ResultHandler<Collection<Token>, ?> handler) {
        this.tokenFilter = tokenFilter;
        this.handler = handler;
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
     * @param connection Connection to use.
     * @param adapter Utility functions to perform the task with.
     * @throws DataLayerException If there was any error during the query.
     * @throws IllegalArgumentException If the TokenFilter provided defined any return fields.
     */
    @Override
    public <C> void execute(C connection, TokenStorageAdapter<C> adapter) throws DataLayerException {
        Reject.ifFalse(tokenFilter.getReturnFields().isEmpty());
        try {
            handler.processResults(adapter.query(connection, tokenFilter));
        } catch (DataLayerException e) {
            handler.processError(e);
        }
    }

    @Override
    public String toString() {
        return MessageFormat.format("QueryTask: {0}", tokenFilter);
    }
}
