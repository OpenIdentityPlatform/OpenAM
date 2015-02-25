/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: UnitTestBase.java,v 1.2 2008/06/25 05:53:06 qcheng Exp $
 *
 */

package com.sun.identity.shared.test;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is the base for all <code>OpenSSO</code> unit testcases.
 * It has commonly used methods; and hopefully we can grow this class
 * to support more methods in future.
 */
public abstract class UnitTestBase {
    private String logEntryTemplate;
    private String className;
    
    static private Logger logger;

    static {
        try {
            logger = Logger.getLogger("com.sun.identity.test");
            logger.addHandler(new FileHandler("logs/unittest"));
            String logLevel = System.getProperty("log.level");
            if ((logLevel != null)) {
                logger.setLevel(Level.parse(logLevel));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }    
    
    private UnitTestBase() {
    }
    
    protected UnitTestBase(String componentName) {
        logEntryTemplate = componentName + "." + 
            this.getClass().getName() + ".{0}: {1}";
        className = this.getClass().getName();
    }
    
    /**
     * Writes a log entry for entering a test method.
     *
     * @param methodName Name of method.
     * @param params Parameter to the method being entered.
     */
    protected void entering(String methodName, Object[] params) {
        if (params != null) {
            logger.entering(className, methodName, params);
        } else {
            logger.entering(className, methodName);
        }
    }

    /**
     * Writes a log entry for exiting a test method.
     *
     * @param methodName Name of method.
     */
    protected void exiting(String methodName) {
        logger.exiting(className, methodName);
    }
    
    /**
     * Writes a log entry.
     *
     * @param level Log Level.
     * @param methodName Name of method.
     * @param message Log Message.
     */
    protected void log(Level level, String methodName, String message) {
        Object[] args = {methodName, message};
        logger.log(level, MessageFormat.format(logEntryTemplate, args));
    }

    /**
     * Writes a log entry.
     *
     * @param level Log Level.
     * @param methodName Name of method.
     * @param message Log Message.
     * @param params Parameters for the log message.
     */
    protected void log(
        Level level, 
        String methodName, 
        String message, 
        Object[] params
    ) {
        Object[] args = {methodName, message};
        logger.log(level, MessageFormat.format(logEntryTemplate, args),
            params);
    }
}
