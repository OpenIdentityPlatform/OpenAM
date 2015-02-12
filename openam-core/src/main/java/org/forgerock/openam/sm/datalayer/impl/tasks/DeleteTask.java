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

import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.api.ResultHandler;
import org.forgerock.openam.sm.datalayer.api.Task;
import org.forgerock.openam.sm.datalayer.api.TokenStorageAdapter;

import java.text.MessageFormat;

/**
 * Deletes a given Token from the persistence layer.
 */
public class DeleteTask implements Task {

    private final String tokenId;
    private final ResultHandler<String, ?> handler;

    /**
     * @param tokenID The Token ID to delete when executed.
     * @param handler Non null result handler for signalling status of operation.
     */
    public DeleteTask(String tokenID, ResultHandler<String, ?> handler) {
        this.tokenId = tokenID;
        this.handler = handler;
    }

    /**
     * Performs the delete operation from the persistence store using the LDAP adapter.
     *
     * @param connection Non null connection to use for the operation.
     * @param adapter Non null adapter to use for the operation.
     *
     * @throws CoreTokenException If there was a problem performing the operation.
     */
    @Override
    public <T> void execute(T connection, TokenStorageAdapter<T> adapter) throws DataLayerException {
        try {
            adapter.delete(connection, tokenId);
            handler.processResults(tokenId);
        } catch (DataLayerException e) {
            handler.processError(e);
            throw e;
        }
    }

    @Override
    public String toString() {
        return MessageFormat.format("DeleteTask: {0}", tokenId);
    }
}
