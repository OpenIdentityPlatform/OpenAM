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
 * $Id: Debug.java,v 1.3 2008/06/25 05:43:39 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.log.spi;

import java.util.logging.LogManager;

import com.sun.identity.log.LogConstants;
import com.sun.identity.log.LogManagerUtil;

/**
 * This class is the class which instantaites the debug implementation
 * class specified in the logging configuration and calls its methods.
 * The intent of this class is to provide a general method of debugging
 * for DSAME and CMS.
 */
public class Debug {
    private static IDebug debugInst;

    static {
        LogManager logManager = LogManagerUtil.getLogManager();
        String debugImplClassName =
            logManager.getProperty(LogConstants.DEBUG_IMPL_CLASS);
        if(debugImplClassName == null) {
            debugImplClassName = "com.sun.identity.log.spi.ISDebug";
        }
        try {
            debugInst =
                (IDebug)Class.forName(debugImplClassName).newInstance();
        } catch (Exception e) {
            // can't do anything because our debug system is not up yet
        }
    }

    /**
     * All the methods below inturn call the custom debug implementation
     * class's debug methods.
     */

    /**
     * error level debug message
     *
     * @param msg The error debug message string.
     * @param t Throwable whose stacktrace will be printed to the debug file.
     */
    public static void error(String msg, Throwable t) {
        debugInst.debug(IDebug.ERROR, msg, t);
    }

    /**
     * warning level debug message
     *
     * @param msg The warning debug message string
     * @param t Throwable whose stacktrace will be printed to the debug file.
     */
    public static void warning(String msg, Throwable t) {
        debugInst.debug(IDebug.WARNING, msg, t);
    }

    /**
     * message level debug message
     *
     * @param msg The debug message string
     * @param t Throwable whose stacktrace will be printed to the debug file.
     */
    public static void message(String msg, Throwable t) {
        debugInst.debug(IDebug.MESSAGE,msg, t);
    }

    /**
     * error level debug message
     *
     * @param msg The debug message string
     */
    public static void error(String msg) {
        debugInst.debug (IDebug.ERROR, msg);
    }

    /**
     * warning level debug message
     *
     * @param msg The debug message string
     */
    public static void warning(String msg) {
        debugInst.debug(IDebug.WARNING, msg);
    }

    /**
     * message level debug message
     *
     * @param msg The debug message string
     */
    public static void message(String msg) {
        debugInst.debug(IDebug.MESSAGE, msg);
    }

    /**
     * check whether debug level set to "message"
     *
     * @return true if debug level set to "message"
     */
    public static boolean messageEnabled() {
        return debugInst.messageEnabled();
    }

    /**
     * check whether debug level set to "warning"
     *
     * @return true if debug level set to "warning"
     */
    public static boolean warningEnabled() {
        return debugInst.warningEnabled();
    }
}
