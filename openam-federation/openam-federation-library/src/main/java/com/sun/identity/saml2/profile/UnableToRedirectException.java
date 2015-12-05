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

package com.sun.identity.saml2.profile;

/**
 * Exception class to indicate when the sso request redirect failed.
 */
public class UnableToRedirectException extends Exception {

    /**
     * Creates a new UnableToRedirectException.
     * @param cause the exception that caused this exception
     */
    public UnableToRedirectException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new UnableToRedirectException.
     */
    public UnableToRedirectException() {
    }

    /**
     * Creates a new UnableToRedirectException.
     * @param message a message describing the cause of the exception
     */
    public UnableToRedirectException(String message) {
        super(message);
    }

    /**
     * Creates a new UnableToRedirectException.
     * @param message a message describing the cause of the exception
     * @param cause the exception that caused this exception
     */
    public UnableToRedirectException(String message, Throwable cause) {
        super(message, cause);
    }
}
