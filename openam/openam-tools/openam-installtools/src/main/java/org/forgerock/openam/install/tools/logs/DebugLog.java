/*
 * Copyright 2014 ForgeRock, AS.
 *
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
 */

package org.forgerock.openam.install.tools.logs;

/**
 * Abstracts over the debug logging framework used by the installer, to allow easier unit testing.
 * Note: eventually this should be replaced throughout the installer by slf4j.
 *
 * @since 12.0.0
 */
public interface DebugLog {
    /**
     * Adds a message to the installation debug log.
     *
     * @param message the message to add to the log.
     */
    void log(String message);

    /**
     * Adds a message and a stack trace to the debug log.
     * @param message the message to add to the log.
     * @param ex the exception to log the stack trace for.
     */
    void log(String message, Throwable ex);
}
