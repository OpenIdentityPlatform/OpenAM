/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: LoggerFactory.java,v 1.2 2009/10/22 21:04:36 veiming Exp $
 */

package com.sun.identity.entitlement.log;


import com.sun.identity.entitlement.PrivilegeManager;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * This factory returns the logger object (java.util.logging.Logger or its
 * super class). This factory instantiates a class that is set in
 * "com.sun.identity.log.loggerprovider" system property. And this class
 * is responsible for returning the Logger object. com.sun.identity.log.Logger
 * will be returned if this system property is not set. Otherwise,
 * java.util.logging.LogManager.getLogManager.getLogger will be returned if
 * com.sun.identity.log.Logger is absent.
 */
public class LoggerFactory {
    private static LoggerFactory instance = new LoggerFactory();
    private ILoggerProvider provider;

    private LoggerFactory() {
        String prop = System.getProperty(
            ILoggerProvider.SYSTEM_PROPERTY_LOG_PROVIDER);
        if ((prop != null) && (prop.trim().length() > 0)) {
            provider = getLoggerProvider(prop);
        }

        if (provider == null) {
            provider = getLoggerProvider(
                "com.sun.identity.log.LoggerProvider");
        }
    }

    private ILoggerProvider getLoggerProvider(String className) {
        try {
            className = className.trim();
            Class clazz = Class.forName(className);
            return (ILoggerProvider) clazz.newInstance();
        } catch (InstantiationException ex) {
            PrivilegeManager.debug.error("LoggerFactory.<init>", ex);
        } catch (IllegalAccessException ex) {
            PrivilegeManager.debug.error("LoggerFactory.<init>", ex);
        } catch (ClassNotFoundException ex) {
            PrivilegeManager.debug.error("LoggerFactory.<init>", ex);
        }
        return null;
    }

    /**
     * Returns logger object.
     *
     * @param name Name of logger.
     * @return logger object.
     */
    private Logger getLoggEr(String name) {
        return (provider != null) ? provider.getLogger(name)
            : LogManager.getLogManager().getLogger(name);
    }

    /**
     * Returns logger object.
     *
     * @param name Name of logger.
     * @return logger object.
     */
    public static Logger getLogger(String name) {
        return instance.getLoggEr(name);
    }
}
