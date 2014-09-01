/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock US Inc. All Rights Reserved
 *
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
 * information:
 *
 * "Portions copyright [year] [name of copyright owner]".
 *
 */
package com.iplanet.dpro.session.exceptions;

/**
 * Provides a specific Exception for Entries which already exist
 * in the Repository.
 * 
 * @author jeff.schenk@forgerock.com
 */
public class EntryAlreadyExistsException extends Exception {
    private static final long serialVersionUID = 42L;

    /**
     * Constructs a new exception with {@code null} as its detail message.
     */
    public EntryAlreadyExistsException() {
    }

    /**
     * Constructs a new exception with the specified detail message.
     */
    public EntryAlreadyExistsException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified cause.
     */
    public EntryAlreadyExistsException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     */
    public EntryAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
