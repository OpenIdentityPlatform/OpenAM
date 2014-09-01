/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [2012] [Forgerock Inc]"
 */

package org.forgerock.restlet.ext.openam.logging;

import java.util.logging.Logger;

import org.restlet.engine.log.LoggerFacade;

import com.sun.identity.shared.debug.Debug;

/**
 * An OpenAMLoggerFacade does ...
 * 
 * @author Laszlo Hordos
 */
public class OpenAMLoggerFacade extends LoggerFacade {

    /**
     * Returns an instance of {@link OpenAMLogger}, wrapping the result of
     * {@link com.sun.identity.shared.debug.Debug#getInstance(String)} where the
     * logger name is "".
     * 
     * @return An anonymous logger.
     */
    @Override
    public Logger getAnonymousLogger() {
        return new OpenAMLogger(Debug.getInstance(""));
    }

    /**
     * Returns an instance of {@link OpenAMLogger}, wrapping the result of
     * {@link com.sun.identity.shared.debug.Debug#getInstance(String)} with the
     * logger name.
     * 
     * @param loggerName
     *            The logger name.
     * @return An anonymous logger.
     */
    @Override
    public Logger getLogger(String loggerName) {
        return new OpenAMLogger(Debug.getInstance(loggerName));
    }
}
