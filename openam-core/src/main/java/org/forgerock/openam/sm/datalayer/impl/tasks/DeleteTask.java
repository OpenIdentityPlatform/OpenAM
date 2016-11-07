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
import java.util.Collections;

import org.forgerock.openam.cts.api.CTSOptions;
import org.forgerock.openam.sm.datalayer.api.AbstractTask;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.api.ResultHandler;
import org.forgerock.openam.sm.datalayer.api.TokenStorageAdapter;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.forgerock.util.Options;

/**
 * Deletes a given Token from the persistence layer.
 */
public class DeleteTask extends AbstractTask<PartialToken> {

    private final String tokenId;
    private final Options options;

    /**
     * @param tokenID The Token ID to delete when executed.
     * @param options The Options for the operation.
     * @param handler Non null result handler for signalling status of operation.
     */
    public DeleteTask(String tokenID, Options options, ResultHandler<PartialToken, ?> handler) {
        super(handler);
        this.tokenId = tokenID;
        this.options = options;
    }

    /**
     * Performs the delete operation from the persistence store using the TokenStorageAdapter.
     *
     * @param adapter Non null adapter to use for the operation.
     *
     * @throws DataLayerException If there was a problem performing the operation.
     */
    @Override
    public void performTask(TokenStorageAdapter adapter) throws DataLayerException {
        PartialToken token = adapter.delete(tokenId, options);
        handler.processResults(token);
    }

    @Override
    public String toString() {
        return MessageFormat.format("DeleteTask: {0}", tokenId);
    }
}
