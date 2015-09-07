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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openam.core.rest.record;

/**
 * Exception dedicated for a recording issue
 */
public class RecordException extends Exception {

    /**
     * Record Exception default constructor
     */
    public RecordException() {
    }

    /**
     * Record Exception constructor with a message
     *
     * @param message
     */
    public RecordException(String message) {
        super(message);
    }

    /**
     * Record exception constructor with a message and an exception
     *
     * @param message
     * @param cause
     */
    public RecordException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Record exception constructor with an exception
     *
     * @param cause
     */
    public RecordException(Throwable cause) {
        super(cause);
    }
}
