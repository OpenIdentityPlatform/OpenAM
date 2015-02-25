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

import org.forgerock.oauth2.core.OAuth2Constants;

/**
 * Thrown when the requested response type is not supported by either the client or the OAuth2 provider.
 *
 * @since 12.0.0
 */
public class UnsupportedResponseTypeException extends OAuth2Exception {

    /**
     * Constructs a new UnsupportedResponseTypeException instance with the specified message.
     *
     * @param message The reason for the exception.
     */
    public UnsupportedResponseTypeException(final String message) {
        this(message, OAuth2Constants.UrlLocation.QUERY);
    }

    /**
     * Constructs a new UnsupportedResponseTypeException instance with the specified message.
     *
     * @param message The reason for the exception.
     * @param parameterLocation Indicates the location of the parameters in the URL.
     */
    public UnsupportedResponseTypeException(final String message, final OAuth2Constants.UrlLocation parameterLocation) {
        super(400, "unsupported_response_type", message, parameterLocation);
    }
}
