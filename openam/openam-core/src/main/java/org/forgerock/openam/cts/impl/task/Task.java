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
 * Copyright 2014 ForgeRock AS.
 */
package org.forgerock.openam.cts.impl.task;

import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.LDAPAdapter;
import org.forgerock.opendj.ldap.Connection;

/**
 * Represents a Task which can be performed by a the implementation.
 * Each task is intended to be self contained and should be intialised with
 * enough state to be processed by the Task Processor.
 *
 * @see org.forgerock.openam.cts.impl.queue.TaskProcessor
 */
public interface Task {
    /**
     * Perform the task.
     *
     * @param connection LDAP Connection to use.
     * @param ldapAdapter LDAP utility functions to perform the task with.
     * @throws CoreTokenException If there was a problem processing the task.
     */
   void execute(Connection connection, LDAPAdapter ldapAdapter) throws CoreTokenException;
}
