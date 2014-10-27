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
package com.sun.identity.authentication.spi;

/**
 * This exception associates a <code>LoginException</code> with an error code from <code>AMAuthErrorCode</code>,
 * which can later be used by <code>AMLoginContext</code> to handle the exception.
 */
public class AuthErrorCodeException extends MessageLoginException {

    private final String authErrorCode;

    /**
     * Constructs a new <code>AuthErrorCodeException</code>.
     *
     * @param authErrorCode The error code which should be one of the codes in <code>AMAuthErrorCode</code>.
     * @param rbName Resource Bundle Name where the localized error message is located.
     * @param messageCode Key in resource bundle for the message.
     */
    public AuthErrorCodeException(String authErrorCode, String rbName, String messageCode) {
        this(authErrorCode, rbName, messageCode, null);
    }

    /**
     * Constructs a new <code>AuthErrorCodeException</code>.
     *
     * @param authErrorCode The error code which should be one of the codes in <code>AMAuthErrorCode</code>.
     * @param rbName Resource Bundle Name where the localized error message is located.
     * @param messageCode Key in resource bundle for the message.
     * @param args Arguments to the error message.
     */
    public AuthErrorCodeException(String authErrorCode, String rbName, String messageCode, Object[] args) {
        super(rbName, messageCode, args);
        this.authErrorCode = authErrorCode;
    }

    /**
     * Get the error code which should be one of the codes in <code>AMAuthErrorCode</code>.
     *
     * @return The error code.
     */
    public String getAuthErrorCode() {
        return authErrorCode;
    }
}
