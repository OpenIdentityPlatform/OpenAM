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

package org.forgerock.openam.uma;

import org.forgerock.oauth2.core.exceptions.OAuth2Exception;

/**
 * Base exception for all UMA exceptions.
 *
 * @since 13.0.0
 */
public class UmaException extends OAuth2Exception {

    /**
     * Constructs a new UmaException with specified status code, error and description.
     *
     * @param statusCode The status code of the exception. Maps to HTTP status codes.
     * @param error The error/name of the exception.
     * @param description The reason and description for the exception.
     */
    public UmaException(int statusCode, String error, String description) {
        super(statusCode, error, description);
    }
}
