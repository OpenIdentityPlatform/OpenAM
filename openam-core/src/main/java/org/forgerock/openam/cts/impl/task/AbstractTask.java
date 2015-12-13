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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openam.cts.impl.task;

/**
 * Abstract task processed by the Task Processor.
 * @param <T> Connection to use.
 */
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.exceptions.LDAPOperationFailedException;
import org.forgerock.openam.cts.impl.LDAPAdapter;
import org.forgerock.openam.cts.impl.queue.ResultHandler;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ErrorResultException;

public abstract class AbstractTask<T> implements Task {

    protected final ResultHandler<T> handler;
    private boolean isError = false;

    /**
     * @param handler Non null handler to notify.
     */
    public AbstractTask(ResultHandler<T> handler) {
        this.handler = handler;
    }

    @Override
    public void execute(Connection connection, LDAPAdapter ldapAdapter) throws CoreTokenException {
        if (isError) {
            return;
        }

        try {
            performTask(connection, ldapAdapter);
        } catch (ErrorResultException e) {
            LDAPOperationFailedException error = new LDAPOperationFailedException(e.getResult());
            processError(error);
            throw error;
        }
    }

    @Override
    public void processError(CoreTokenException error) {
        isError = true;
        handler.processError(error);
    }

    /**
     * Perform the task.
     *
     * @param connection LDAP Connection to use.
     * @param ldapAdapter LDAP utility functions to perform the task with.
     * @throws CoreTokenException If there was a problem processing the task.
     */
    public abstract void performTask(Connection connection, LDAPAdapter ldapAdapter) throws CoreTokenException,
            ErrorResultException;

}
