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
package org.forgerock.openam.cts.impl.queue;

import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.impl.CoreTokenAdapter;
import org.forgerock.openam.cts.impl.query.PartialToken;

import java.util.Collection;

/**
 * Defines the types of ResultHandler that can be generated for processing the result
 * of asynchronous operations.
 */
public interface ResultHandlerFactory {
    /**
     * @return Handler suitable for a create operation.
     */
    ResultHandler<Token> getCreateHandler();

    /**
     * @return Handler suitable for a read operation.
     */
    ResultHandler<Token> getReadHandler();

    /**
     * @return Handler suitable for an update operation.
     */
    ResultHandler<Token> getUpdateHandler();

    /**
     * @return Handler suitable for a delete operation.
     */
    ResultHandler<String> getDeleteHandler();

    /**
     * @return Handler suitable for a query operation.
     */
    ResultHandler<Collection<Token>> getQueryHandler();

    /**
     * @return Handler suitable for a partial query operation.
     */
    ResultHandler<Collection<PartialToken>> getPartialQueryHandler();

    /**
     * Returns a {@link ResultHandler} that performs a delete operation on the query's results.
     *
     * @return Handler suitable for delete on query operation.
     */
    ResultHandler<Collection<PartialToken>> getDeleteOnQueryHandler();
}
