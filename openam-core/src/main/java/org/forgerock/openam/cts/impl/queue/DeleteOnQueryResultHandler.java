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
 * Portions copyright 2025 3A Systems LLC.
 */
package org.forgerock.openam.cts.impl.queue;

import com.google.inject.name.Named;
import com.sun.identity.shared.debug.Debug;
import java.util.Collection;
import jakarta.inject.Inject;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.sm.datalayer.api.ResultHandler;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;

/**
 * This is a {@link org.forgerock.openam.sm.datalayer.api.ResultHandler} implementation that grabs the results of a Partial Query and for each token found
 * performs an asynchronous delete operation. This can come in handy if there is a need to find tokens based on
 * secondary storage keys or other arbitrary information and the matching tokens needs to be deleted right away.
 *
 * @see TaskDispatcher#read(String, org.forgerock.openam.sm.datalayer.api.ResultHandler)
 * @see TaskDispatcher#query(TokenFilter, org.forgerock.openam.sm.datalayer.api.ResultHandler)
 */
public class DeleteOnQueryResultHandler implements ResultHandler<Collection<PartialToken>, CoreTokenException> {

    private final TaskDispatcher taskDispatcher;
    private final ResultHandlerFactory resultHandlerFactory;
    private final Debug debug;

    @Inject
    public DeleteOnQueryResultHandler(TaskDispatcher taskDispatcher, ResultHandlerFactory resultHandlerFactory,
            @Named(CoreTokenConstants.CTS_DEBUG) Debug debug) {
        this.taskDispatcher = taskDispatcher;
        this.resultHandlerFactory = resultHandlerFactory;
        this.debug = debug;
    }

    @Override
    public Collection<PartialToken> getResults() {
        throw new UnsupportedOperationException("getResults() not implemented");
    }

    @Override
    public void processResults(Collection<PartialToken> results) {
        String tokenId = null;
        for (PartialToken result : results) {
            try {
                tokenId = result.getValue(CoreTokenField.TOKEN_ID);
                taskDispatcher.delete(tokenId, resultHandlerFactory.getDeleteHandler());
            } catch (CoreTokenException ex) {
                debug.error(String.format("Unable to submit delete task for token ID {0}", tokenId), ex);
            }
        }
    }

    @Override
    public void processError(Exception exception) {
        // Nothing to do, the error is already logged in TaskProcessor
    }
}
