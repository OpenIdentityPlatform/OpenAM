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
 * Performs a Read against the persistence layer.
 */
public class ReadTask extends AbstractTask {

    private final String tokenId;
    private final Options options;

    /**
     * @param tokenId The Token ID to read.
     * @param options The Options for the operation.
     * @param handler The ResultHandler to update with the result.
     */
    public ReadTask(String tokenId, Options options, ResultHandler<Token, ?> handler) {
        super(handler);
        this.tokenId = tokenId;
        this.options = options;
    }

    /**
     * Uses the LDAP Adapter to perform the read and updates the result handler with
     * the success or failure result.
     *
     * In the event of a failure, this function will still throw the expected
     * exception, even though the result handler will be notified.
     *
     * @param adapter Non null for connection-coupled operations.
     * @throws DataLayerException If there was an error whilst performing the read.
     */
    @Override
    public void performTask(TokenStorageAdapter adapter) throws DataLayerException {
    	Token token = sid2token.getIfPresent(tokenId);
    	if (token==null) {
    		token=adapter.read(tokenId, options);
    		if (token!=null) {
        		sid2token.put(tokenId, new Token(token));
        	}
    	}else {
    		token=new Token(token); 
    	}
		handler.processResults(token);
    }

    @Override
    public String toString() {
        return MessageFormat.format("ReadTask: {0}", tokenId);
    }
}
