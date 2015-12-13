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
package org.forgerock.openam.cts.impl.task;

import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.LDAPAdapter;
import org.forgerock.openam.cts.impl.queue.ResultHandler;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ErrorResultException;

import java.text.MessageFormat;

/**
 * Deletes a given Token from the persistence layer.
 */
public class DeleteTask extends AbstractTask {

    private final String tokenId;

    /**
     * @param tokenID The Token ID to delete when executed.
     * @param handler Non null result handler for signalling status of operation.
     */
    public DeleteTask(String tokenID, ResultHandler<String> handler) {
        super(handler);
        this.tokenId = tokenID;
    }

    /**
     * Performs the delete operation from the persistence store using the LDAP adapter.
     *
     * @param connection Non null connection to use for the operation.
     * @param ldapAdapter Non null adapter to use for the operation.
     *
     * @throws ErrorResultException If there was a problem performing the operation.
     * @throws CoreTokenException If there was a problem performing the operation.
     */
    @Override
    public void performTask(Connection connection, LDAPAdapter ldapAdapter) throws ErrorResultException, CoreTokenException {
        ldapAdapter.delete(connection, tokenId);
        handler.processResults(tokenId);
    }

    @Override
    public String toString() {
        return MessageFormat.format("DeleteTask: {0}", tokenId);
    }
}
