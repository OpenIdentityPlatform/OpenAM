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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.openam.forgerockrest.authn.exceptions;

/**
 * This exception is designed to be thrown from RESTful authentication calls when an error occurs.
 */
public class RestAuthException extends Exception {

    /**
     * Unauthorized HTTP Status Code.
     */
    public static final int UNAUTHORIZED = 401;

    private int statusCode;
    private String failureUrl;

    /**
     * Constructs a RestAuthException.
     *
     * @param responseStatus The HTTP response to code to send back to the client.
     * @param errorMessage The error message relating to the exception.
     */
    public RestAuthException(int responseStatus, String errorMessage) {
        super(errorMessage);
        statusCode = responseStatus;
    }

    /**
     * Constructs a RestAuthException.
     *
     * @param responseStatus The HTTP response to code to send back to the client.
     * @param throwable The cause of the exception.
     */
    public RestAuthException(int responseStatus, Throwable throwable) {
        super(throwable);
        statusCode = responseStatus;
    }

    /**
     * Constructs a RestAuthException.
     *
     * @param responseStatus The HTTP response to code to send back to the client.
     * @param errorMessage The error message relating to the exception.
     * @param throwable The cause of the exception.
     */
    public RestAuthException(int responseStatus, String errorMessage, Throwable throwable) {
        super(errorMessage, throwable);
        statusCode = responseStatus;
    }

    /**
     * Sets the go to on failure url.
     *
     * @param failureUrl The failure url.
     */
    public void setFailureUrl(final String failureUrl) {
        this.failureUrl = failureUrl;
    }

    /**
     * Gets the status code.
     *
     * @return The status code.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Gets the failure URL.
     *
     * @return The failure URL.
     */
    public String getFailureUrl() {
        return failureUrl;
    }
}
