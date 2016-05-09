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
 * Thrown when client authorization fails.
 *
 * @since 12.0.0
 */
public class UnauthorizedClientException extends OAuth2Exception {

    /**
     * Constructs a new UnauthorizedClientException with the default message.
     */
    public UnauthorizedClientException() {
        this("The client is not authorized to request an authorization code using this method.");
    }

    /**
     * Constructs a new UnauthorizedClientException with the specified message.
     *
     * @param message The reason for the exception.
     */
    public UnauthorizedClientException(String message) {
        super(400, "unauthorized_client", message);
    }
}
