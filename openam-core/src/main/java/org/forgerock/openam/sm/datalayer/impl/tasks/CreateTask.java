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
 * Responsible for creating a Token in persistence layer.
 */
public class CreateTask extends AbstractTask {

    private final Token token;
    private final Options options;

    /**
     * @param token Non null Token to create.
     * @param options Non null Options for the operation.
     * @param handler Non null handler to notify.
     */
    public CreateTask(Token token, Options options, ResultHandler<Token, ?> handler) {
        super(handler);
        this.token = token;
        this.options = options;
    }

    /**
     * Performs a creation operation.
     *
     * Note: If the Token already exists this operation will fail.
     *
     * @param adapter Required for datalayer operations.
     * @throws DataLayerException If there was any problem creating the Token.
     */
    @Override
    public void performTask(TokenStorageAdapter adapter) throws DataLayerException {
        Token created = adapter.create(token, options);
        if (token.getTokenId()!=null) {
        	sid2token.put(token.getTokenId(), new Token(created==null?token:created));
        }
        handler.processResults(created);
    }

    @Override
    public String toString() {
        return MessageFormat.format("CreateTask: {0}", token.getTokenId());
    }
}
