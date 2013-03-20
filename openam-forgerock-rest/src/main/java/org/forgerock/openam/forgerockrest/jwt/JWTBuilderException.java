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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.openam.forgerockrest.jwt;

/**
 * This exception is to be thrown when an error occurs when creating a JWT.
 */
public class JWTBuilderException extends RuntimeException {

    /**
     * Constructs a JWTBuilderException.
     *
     * @param errorMessage The error message.
     */
    public JWTBuilderException(String errorMessage) {
        super(errorMessage);
    }

    /**
     * Constructs a JWTBuilderException.
     *
     * @param errorMessage The error message.
     * @param e The cause.
     */
    public JWTBuilderException(String errorMessage, Exception e) {
        super(errorMessage, e);
    }
}
