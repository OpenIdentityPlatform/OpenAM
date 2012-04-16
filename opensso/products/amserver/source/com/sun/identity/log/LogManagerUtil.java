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
 * $Id: LogManagerUtil.java,v 1.9 2009/07/27 19:46:59 bigfatrat Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.log;

import java.io.IOException;
import java.security.AccessController;
import java.util.Enumeration;

import com.iplanet.sso.SSOToken;
import com.iplanet.am.util.SystemProperties;
import com.sun.identity.common.ShutdownListener;
import com.sun.identity.common.ShutdownManager;
import com.sun.identity.common.ShutdownPriority;
import com.sun.identity.log.messageid.LogMessageProviderBase;
import com.sun.identity.log.messageid.MessageProviderFactory;
import com.sun.identity.security.AdminTokenAction;

/**
 * This class is a work around for situations where our
 * log manager conflicts with an existing log manager in
 * the container.
 */
public class LogManagerUtil {
    private static com.sun.identity.log.LogManager lmgr = null;
    private static boolean isAMLog;
    static {
       /*
        * Uses AM's log manager if in Server Mode
        * or COMPATMODE is OFF (in case of client side)
        */
       String compatMode = SystemProperties.get("LOG_COMPATMODE", "Off");
       if ((compatMode.trim().equalsIgnoreCase("Off")) ||
           SystemProperties.isServerMode()) {
           isAMLog = true;
       } else {
           isAMLog = false;
       }
       lmgr = new com.sun.identity.log.LogManager();
       
       /*
        * get admin token for log service's use to write
        * the start and stop records
        */
       if (SystemProperties.isServerMode()) {
           ShutdownManager shutdownMan = ShutdownManager.getInstance();
           if (shutdownMan.acquireValidLock()) {
               try {
                   shutdownMan.addShutdownListener(new
                       ShutdownListener() {

                           public void shutdown() {
                               logEndRecords();
                           }
                       }, ShutdownPriority.LOWEST);
               } finally {
                   shutdownMan.releaseLockAndNotify();
               }
           }
        }
    }

    /**
     * Returns a local LogManager object if LOG_COMPATMODE
     * environment variable is set to "Off". Otherwise returns
     * the global LogManager in the JVM.
     *
     * @return LogManager object.
     */
    public static com.sun.identity.log.LogManager getLogManager() {
        return lmgr;
    }

    /**
      * Returns whether should use AMLog mode
      *
      */
    public static boolean isAMLoggingMode() {
        return isAMLog;
    }

    static String oldcclass = null;
    static String newcclass = null;
    static String oldcfile = null;
    static String newcfile = null;

    /**
     * Sets up the log configuration reader class or file in the
     * environment, so that our LogManager's custom configuration
     * will be read.
     */
    public static void setupEnv() {
        if (lmgr != null) {
            oldcclass = SystemProperties.get("java.util.logging.config.class");
            newcclass = SystemProperties.get(
                "s1is.java.util.logging.config.class");
            oldcfile = SystemProperties.get("java.util.logging.config.file");
            newcfile = SystemProperties.get(
                "s1is.java.util.logging.config.file");
            try {
                if (newcclass != null) {
                    System.setProperty("java.util.logging.config.class", 
                        newcclass);
                }
                if (newcfile != null) {
                    System.setProperty("java.util.logging.config.file", 
                        newcfile);
                }
            } catch (Throwable err) {
            }
        }
    }

    /**
     * Resets the environment to the default one.
     */
    public static void resetEnv() {
        if (lmgr != null) {
            if (oldcclass != null) {
                System.setProperty("java.util.logging.config.class",
                    oldcclass);
            }
            if (oldcfile != null) {
                System.setProperty("java.util.logging.config.file",oldcfile);
            }
        }
    }

    /**
     *  get a privileged SSOToken from the TokenManager
     */
    protected static SSOToken getLoggingSSOToken() {
        SSOToken st = null;
        try {
            st = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
        } catch (Exception ex) {
            // can't do much about this
        }
        return st;
    }

    /**
     *  Log a LogRecord indicating the end of logging to all opened files
     */
    public static void logEndRecords() {
        if (lmgr != null) {
            try {
                SSOToken ssot = getLoggingSSOToken();
                LogMessageProviderBase provider =
                    (LogMessageProviderBase)MessageProviderFactory.getProvider(
                        "Logging");
                String[] s = new String[1];
                Enumeration e = lmgr.getLoggerNames();
                com.sun.identity.log.LogRecord lr = null;
                while (e.hasMoreElements()) {
                    String logger = (String)e.nextElement();
                    if (logger.length() != 0 && !logger.equals("global")) {
                        Logger result = (Logger)Logger.getLogger(logger);
                        s[0] = logger;
                        lr = provider.createLogRecord(
                            LogConstants.END_LOG_NAME, s, ssot);
                        result.log(lr, ssot);
                        result.flush();
                    }
                }
            } catch (IOException ioex) {
                // can't do much here
            } 
        }
    }
}
