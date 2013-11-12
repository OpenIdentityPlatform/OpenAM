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
 * Copyright 2013 ForgeRock AS.
 */

package org.forgerock.openam.authz.filter.configuration;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.auth.common.DebugLogger;

/**
 * Implementation of the DebugLogger for deployment of the commons AuthZFilter in OpenAM.
 *
 * @author Phill Cunnington
 * @since 10.2.0
 */
public class AuthDebugLogger implements DebugLogger {

    private final Debug debug;

    /**
     *{@inheritDoc}
     */
    public AuthDebugLogger(String logName) {
        debug = Debug.getInstance(logName);
    }

    /**
     *{@inheritDoc}
     */
    @Override
    public void debug(String message) {
        debug.message(message);
    }

    /**
     *{@inheritDoc}
     */
    @Override
    public void debug(String message, Throwable t) {
        debug.message(message, t);
    }

    /**
     *{@inheritDoc}
     */
    @Override
    public void error(String message) {
        debug.error(message);
    }

    /**
     *{@inheritDoc}
     */
    @Override
    public void error(String message, Throwable t) {
        debug.error(message, t);
    }

    /**
     *{@inheritDoc}
     */
    @Override
    public void warn(String message) {
        debug.warning(message);
    }

    /**
     *{@inheritDoc}
     */
    @Override
    public void warn(String message, Throwable t) {
        debug.warning(message, t);
    }
}
