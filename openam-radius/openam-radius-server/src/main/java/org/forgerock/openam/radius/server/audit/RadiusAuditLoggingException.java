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
package org.forgerock.openam.radius.server.audit;

/**
 * Exception thrown when it's not possible to make an audit entry.
 */
@SuppressWarnings("serial")
public class RadiusAuditLoggingException extends Exception {

    /**
     * Constructs a new RadiusAuditLoggingException with the specified detail message. The cause is not initialized, and
     * may subsequently be initialized by a call to initCause.
     *
     * @param message the detail message. The detail message is saved for later retrieval by the getMessage() method.
     */
    public RadiusAuditLoggingException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause. Note that the detail message associated
     * with cause is not automatically incorporated in this exception's detail message.
     *
     * @param message the detail message (which is saved for later retrieval by the getMessage() method).
     * @param cause the cause (which is saved for later retrieval by the getCause() method). (A null value is permitted,
     *            and indicates that the cause is nonexistent or unknown.)
     */
    public RadiusAuditLoggingException(String message, Throwable cause) {
        super(message, cause);
    }

}
