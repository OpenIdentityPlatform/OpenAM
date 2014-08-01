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

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.impl.query.PartialToken;
import org.forgerock.openam.cts.impl.queue.config.QueueConfiguration;

import javax.inject.Inject;
import java.util.Collection;

/**
 * Implementation provides an appropriate asynchronous ResultHandler implementation based on the
 * type requested.
 */
public class AsyncResultHandlerFactory implements ResultHandlerFactory {

    private final QueueConfiguration config;

    /**
     * @param config Required for queue configuration.
     */
    @Inject
    public AsyncResultHandlerFactory(QueueConfiguration config) {
        this.config = config;
    }

    /**
     * @return Non null result handler.
     */
    public ResultHandler<Token> getCreateHandler() {
        return new AsyncResultHandler<Token>(config);
    }

    /**
     * @return Non null result handler.
     */
    public ResultHandler<Token> getReadHandler() {
        return new AsyncResultHandler<Token>(config);
    }

    /**
     * @return Non null result handler.
     */
    public ResultHandler<Token> getUpdateHandler() {
        return new AsyncResultHandler<Token>(config);
    }

    /**
     * @return Non null result handler.
     */
    public ResultHandler<String> getDeleteHandler() {
        return new AsyncResultHandler<String>(config);
    }

    /**
     * @return Non null result handler.
     */
    public ResultHandler<Collection<Token>> getQueryHandler() {
        return new AsyncResultHandler<Collection<Token>>(config);
    }

    /**
     * @return Non null result handler.
     */
    public ResultHandler<Collection<PartialToken>> getPartialQueryHandler() {
        return new AsyncResultHandler<Collection<PartialToken>>(config);
    }

    @Override
    public ResultHandler<Collection<PartialToken>> getDeleteOnQueryHandler() {
        return InjectorHolder.getInstance(DeleteOnQueryResultHandler.class);
    }
}
