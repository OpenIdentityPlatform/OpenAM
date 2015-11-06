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
/**
 *
 */
package org.forgerock.openam.radius.server;

/**
 * Exception to be thrown for Radius startup, shutdown & re-configuration errors.
 */
public class RadiusLifecycleException extends Exception {
    /**
     * Constructor.
     *
     * @param message - the detail message. The detail message is saved for later retrieval by the
     *            Throwable.getMessage() method.
     */
    public RadiusLifecycleException(String message) {
        super(message);
    }

    /**
     * Constructor.
     *
     * @param message - the detail message. The detail message is saved for later retrieval by the
     *            Throwable.getMessage() method.
     * @param cause - the cause (which is saved for later retrieval by the Throwable.getCause() method). (A null value
     *            is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public RadiusLifecycleException(String message, Throwable cause) {
        super(message, cause);
    }
}
