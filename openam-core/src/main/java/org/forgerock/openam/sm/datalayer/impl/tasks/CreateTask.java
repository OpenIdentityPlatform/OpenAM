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

import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.sm.datalayer.api.AbstractTask;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.api.ResultHandler;
import org.forgerock.openam.sm.datalayer.api.TokenStorageAdapter;

import java.text.MessageFormat;

/**
 * Responsible for creating a Token in LDAP Store.
 */
public class CreateTask extends AbstractTask {
    private final Token token;

    /**
     * @param token Non null Token to create.
     * @param handler Non null handler to notify.
     */
    public CreateTask(Token token, ResultHandler<Token, ?> handler) {
        super(handler);
        this.token = token;
    }

    /**
     * Performs a creation operation.
     *
     * Note: If the Token already exists this operation will fail.
     *
     * @param connection Non null connection to use.
     * @param adapter Required for LDAP operations.
     * @throws DataLayerException If there was any problem creating the Token.
     */
    @Override
    public void performTask(Object connection, TokenStorageAdapter adapter) throws DataLayerException {
        adapter.create(connection, token);
        handler.processResults(token);
    }

    @Override
    public String toString() {
        return MessageFormat.format("CreateTask: {0}", token.getTokenId());
    }
}
