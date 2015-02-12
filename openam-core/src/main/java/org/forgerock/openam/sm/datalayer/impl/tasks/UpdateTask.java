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

import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.api.ResultHandler;
import org.forgerock.openam.sm.datalayer.api.Task;
import org.forgerock.openam.sm.datalayer.api.TokenStorageAdapter;

import java.text.MessageFormat;

/**
 * Responsible for updating the LDAP persistence with the provided Token.
 */
public class UpdateTask implements Task {
    private final Token token;
    private final ResultHandler<Token, ?> handler;

    /**
     * @param token Non null Token to update.
     * @param handler Non null handler to notify.
     */
    public UpdateTask(Token token, ResultHandler<Token, ?> handler) {
        this.token = token;
        this.handler = handler;
    }

    /**
     * Performs a read of the store to determine the state of the Token.
     *
     * If the Token exists, then an update is performed, otherwise a create is
     * performed.
     *
     * @param connection Non null Connection.
     * @param adapter Non null for connection-coupled operations.
     * @throws CoreTokenException If there was an error of any kind.
     */
    @Override
    public <T> void execute(T connection, TokenStorageAdapter<T> adapter) throws DataLayerException {
        try {
            Token previous = adapter.read(connection, token.getTokenId());
            if (previous == null) {
                adapter.create(connection, token);
            } else {
                adapter.update(connection, previous, token);
            }
            handler.processResults(token);
        } catch (DataLayerException e) {
            handler.processError(e);
            throw e;
        }
    }

    @Override
    public String toString() {
        return MessageFormat.format("UpdateTask: {0}", token.getTokenId());
    }
}
