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
package org.forgerock.openam.cts.impl.queue;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.sm.datalayer.api.QueueConfiguration;
import org.forgerock.openam.sm.datalayer.api.ResultHandler;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;

import com.sun.identity.shared.debug.Debug;

/**
 * Implementation provides an appropriate asynchronous ResultHandler implementation based on the
 * type requested.
 */
public class AsyncResultHandlerFactory implements ResultHandlerFactory {

    private final QueueConfiguration config;
    private final Debug debug;

    /**
     * @param config Required for queue configuration.
     * @param debug Non null debug instance for debugging.
     */
    @Inject
    public AsyncResultHandlerFactory(@DataLayer(ConnectionType.CTS_ASYNC) QueueConfiguration config,
                                     @Named(CoreTokenConstants.CTS_ASYNC_DEBUG)Debug debug) {
        this.config = config;
        this.debug = debug;
    }

    /**
     * @return Non null result handler.
     */
    public ResultHandler<Token, CoreTokenException> getCreateHandler() {
        return new AsyncResultHandler<Token>(config, debug);
    }

    /**
     * @return Non null result handler.
     */
    public ResultHandler<Token, CoreTokenException> getReadHandler() {
        return new AsyncResultHandler<Token>(config, debug);
    }

    /**
     * @return Non null result handler.
     */
    public ResultHandler<Token, CoreTokenException> getUpdateHandler() {
        return new AsyncResultHandler<Token>(config, debug);
    }

    /**
     * @return Non null result handler.
     */
    public ResultHandler<String, CoreTokenException> getDeleteHandler() {
        return new AsyncResultHandler<String>(config, debug);
    }

    /**
     * @return Non null result handler.
     */
    public ResultHandler<Collection<Token>, CoreTokenException> getQueryHandler() {
        return new AsyncResultHandler<Collection<Token>>(config, debug);
    }

    /**
     * @return Non null result handler.
     */
    public ResultHandler<Collection<PartialToken>, CoreTokenException> getPartialQueryHandler() {
        return new AsyncResultHandler<Collection<PartialToken>>(config, debug);
    }

    @Override
    public ResultHandler<Collection<PartialToken>, CoreTokenException> getDeleteOnQueryHandler() {
        return InjectorHolder.getInstance(DeleteOnQueryResultHandler.class);
    }
}
