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

import org.forgerock.oauth2.core.OAuth2Constants.UrlLocation;

/**
 * Thrown when the resource owner authentication fails.
 *
 * @since 12.0.0
 */
public class AccessDeniedException extends OAuth2Exception {

    /**
     * Constructs a new AccessDeniedException with specified message.
     * The {@link UrlLocation} for the parameters are defaulted to QUERY.
     *
     * @param message The reason for the exception.
     */
    public AccessDeniedException(final String message) {
        this(message, UrlLocation.QUERY);
    }

    /**
     * Constructs a new AccessDeniedException with message from the specified cause.
     *
     * @param cause The cause of the exception.
     */
    public AccessDeniedException(final Throwable cause) {
        this(cause.getMessage());
    }

    /**
     * Constructs a new AccessDeniedException with specified message.
     *
     * @param message The reason for the exception.
     */
    public AccessDeniedException(final String message, final UrlLocation parameterLocation) {
        super(400, "access_denied", message, parameterLocation);
    }
}
