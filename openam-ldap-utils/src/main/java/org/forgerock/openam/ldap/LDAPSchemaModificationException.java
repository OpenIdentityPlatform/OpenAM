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
 * Copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.ldap;

import java.io.IOException;

/**
 * Exception thrown when an LDAP schema modification fails.
 */
public class LDAPSchemaModificationException extends IOException {

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message The detail message explaining the error.
     */
    public LDAPSchemaModificationException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message
     * and cause of the error.
     *
     * @param message The detail message explaining the error.
     * @param cause   The cause of the error (can be null).
     */
    public LDAPSchemaModificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
