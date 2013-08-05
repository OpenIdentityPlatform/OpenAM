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

package org.forgerock.openam.auth.shared;

import com.sun.identity.log.Logger;

import javax.inject.Singleton;

/**
 * Simple factory for getting an instance of a Logger.
 *
 * @author Phill Cunnington
 * @since 10.2.0
 */
@Singleton
public class LoggerFactory {

    /**
     * Gets an instance of a Logger with the specified name.
     *
     * @param name The name of the Logger.
     * @return The Logger instance.
     */
    public Logger getLogger(String name) {
        return (Logger) Logger.getLogger(name);
    }
}
