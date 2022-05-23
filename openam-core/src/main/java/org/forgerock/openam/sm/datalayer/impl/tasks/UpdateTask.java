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
 * Copyright 2014-2016 ForgeRock AS.
 */
package org.forgerock.openam.sm.datalayer.impl.tasks;

import java.text.MessageFormat;

import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.sm.datalayer.api.AbstractTask;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.api.ResultHandler;
import org.forgerock.openam.sm.datalayer.api.TokenStorageAdapter;
import org.forgerock.util.Options;

/**
 * Responsible for updating the persistence layer with the provided Token.
 */
public class UpdateTask extends AbstractTask {

    private final Token token;
    private final Options options;

    /**
     * @param token Non null Token to update.
     * @param options Non null Options for the operation.
     * @param handler Non null handler to notify.
     */
    public UpdateTask(Token token, Options options, ResultHandler<Token, ?> handler) {
        super(handler);
        this.token = token;
        this.options = options;
    }

    /**
     * Performs a read of the store to determine the state of the Token.
     *
     * If the Token exists, then an update is performed, otherwise a create is
     * performed.
     *
     * @param adapter Non null for connection-coupled operations.
     * @throws DataLayerException If there was an error of any kind.
     */
    @Override
    public void performTask(TokenStorageAdapter adapter) throws DataLayerException {
    	Token previous =null; 
    	if (token.getTokenId()!=null) {
    		previous=sid2token.getIfPresent(token.getTokenId());
    	}
    	if (previous==null) {
    		previous=adapter.read(token.getTokenId(), options);
    	}
    	final Token updated;
        if (previous == null) {
            updated = adapter.create(token, options);
        } else {
            updated = adapter.update(previous, token, options);
        }
        if (token.getTokenId()!=null) {
        	sid2token.put(token.getTokenId(), updated==null?token:updated);
        }
        handler.processResults(updated);
    }

    @Override
    public String toString() {
        return MessageFormat.format("UpdateTask: {0}", token.getTokenId());
    }
}
