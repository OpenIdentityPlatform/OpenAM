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
package org.forgerock.openam.radius.server.config;

/**
 * Exception thrown when the RADIUS server client configuration is invalid.
 */
public class ClientConfigurationException extends Exception {
    /**
     * serialVersion id.
     */
    private static final long serialVersionUID = 3951314541727910820L;

    /**
     * Constructs a new exception with the specified detail message. The cause is not initialized, and may subsequently
     * be initialized by a call to Throwable.initCause(java.lang.Throwable).
     *
     * @param configName
     *            the name of the client configuration that contains the invalid configuration item.
     * @param badConfigItemName
     *            the name of the bad configuration item.
     */
    public ClientConfigurationException(String configName, String badConfigItemName) {
        super("Configuration setting " + badConfigItemName + " in RADIUS Client configuration named '" + configName
                + "' is invalid. Requests from this client will be ignored.");
    }

    /**
     * Constructs a new exception with the specified detail message and cause. Note that the detail message associated
     * with cause is not automatically incorporated in this exception's detail message.
     *
     * @param configName
     *            the name of the client configuration that contains the invalid configuration item.
     * @param badConfigItemName
     *            the name of the bad configuration item.
     * @param cause
     *            the cause (which is saved for later retrieval by the Throwable.getCause() method). (A null value is
     *            permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public ClientConfigurationException(String configName, String badConfigItemName, Throwable cause) {
        super("Configuration setting " + badConfigItemName + " in RADIUS Client configuration named '" + configName
                + "' is invalid. Requests from this client will be ignored.", cause);
    }

}
