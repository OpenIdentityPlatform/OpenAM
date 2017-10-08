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

package org.forgerock.oauth2.core.exceptions;

/**
 * Thrown when any internal server error occurs.
 *
 * @since 12.0.0
 */
public class ServerException extends OAuth2Exception {

    /**
     * Constructs a new ServerException with the default message.
     */
    public ServerException() {
        this("The client identifier provided is invalid, the client failed to authenticate, the client did not include its credentials, provided multiple client credentials, or used unsupported credentials type.");
    }

    /**
     * Constructs a new ServerException with the specified message.
     *
     * @param message The reason for the exception.
     */
    public ServerException(final String message) {
        super(400, "server_error", message);
    }

    /**
     * Constructs a new ServerException with the message from the specified cause.
     *
     * @param cause The cause of the exception.
     */
    public ServerException(final Throwable cause) {
        this(cause.getMessage());
    }
}
