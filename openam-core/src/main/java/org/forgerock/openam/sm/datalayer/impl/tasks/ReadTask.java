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
 * Performs a Read against the LDAP persistence layer.
 */
public class ReadTask implements Task {
    private final String tokenId;
    private final ResultHandler<Token, ?> handler;

    /**
     * @param tokenId The Token ID to read.
     * @param handler The ResultHandler to update with the result.
     */
    public ReadTask(String tokenId, ResultHandler<Token, ?> handler) {
        this.tokenId = tokenId;
        this.handler = handler;
    }

    /**
     * Uses the LDAP Adapter to perform the read and updates the result handler with
     * the success or failure result.
     *
     * In the event of a failure, this function will still throw the expected
     * exception, even though the result handler will be notified.
     *
     * @param connection Non null connection.
     * @param adapter Non null for connection-coupled operations.
     * @throws CoreTokenException If there was an error whilst performing the read.
     */
    @Override
    public <T> void execute(T connection, TokenStorageAdapter<T> adapter) throws DataLayerException {
        try {
            Token token = adapter.read(connection, tokenId);
            handler.processResults(token);
        } catch (DataLayerException e) {
            handler.processError(e);
            throw e;
        }
    }

    @Override
    public String toString() {
        return MessageFormat.format("ReadTask: {0}", tokenId);
    }
}
