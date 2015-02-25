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
 * Base exception for all OAuth2 exceptions.
 *
 * @since 12.0.0
 */
public abstract class OAuth2Exception extends Exception {

    private final int statusCode;
    private final String error;
    private final UrlLocation parameterLocation;

    /**
     * Constructs a new OAuth2Exception with specified status code, error and description.
     * The {@link UrlLocation} for the parameters are defaulted to QUERY.
     *
     * @param statusCode The status code of the exception. Maps to HTTP status codes.
     * @param error The error/name of the exception.
     * @param description The reason and description for the exception.
     */
    public OAuth2Exception(final int statusCode, final String error, final String description) {
        this(statusCode, error, description, UrlLocation.QUERY);
    }

    /**
     * Constructs a new OAuth2Exception with specified status code, error and description.
     *
     * @param statusCode The status code of the exception. Maps to HTTP status codes.
     * @param error The error/name of the exception.
     * @param description The reason and description for the exception.
     * @param parameterLocation Indicates the location of the parameters in the URL.
     */
    public OAuth2Exception(final int statusCode, final String error, final String description,
                           final UrlLocation parameterLocation) {
        super(description);
        this.statusCode = statusCode;
        this.error = error;
        this.parameterLocation = parameterLocation;
    }

    /**
     * Gets the status code of the exception.
     *
     * @return The status code.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Gets the error/name of the exception.
     *
     * @return The error.
     */
    public String getError() {
        return error;
    }

    /**
     * Gets the location of the parameters in the URL.
     *
     * @return the location of the parameters.
     */
    public UrlLocation getParameterLocation() {
        return parameterLocation;
    }
}
