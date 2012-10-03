/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: PWResetAdminLog.java,v 1.2 2008/06/25 05:43:42 qcheng Exp $
 *
 */

package com.sun.identity.password.ui.model;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOToken;
import com.sun.identity.log.LogRecord;
import com.sun.identity.log.Logger;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.locale.Locale;
import java.security.AccessController;
import java.util.ResourceBundle;
import java.util.logging.Level;

/**
 * <code>PWResetAdminLog</code> defines the methods to log messages
 * to password reset log file.
 */
public class PWResetAdminLog
{
    private Logger logger = null;
    private static final String logFile = "amPasswordReset.access";
    private static final String ACTIVE = "active";
    private java.util.Locale locale = null;
    private static boolean logStatus = false;
    private SSOToken token = null;


    /** 
     * Resource bundle object 
     */
    protected static ResourceBundle rb = null;

    static {
	String status = SystemProperties.get(Constants.AM_LOGSTATUS);
        logStatus = status.equalsIgnoreCase(ACTIVE);
    }

    /** 
     * Constructs a logger object
     *
     * @param token a valid SSO Token object
     */
    public PWResetAdminLog(SSOToken token) {
        this.token = token;
        if (logStatus) {
            logger = (com.sun.identity.log.Logger)Logger.getLogger(logFile);
        }
        String lstr = SystemProperties.get(Constants.AM_LOCALE);

        locale = com.sun.identity.shared.locale.Locale.getLocale(lstr);
        rb = PWResetResBundleCacher.getBundle(PWResetModel.DEFAULT_RB, locale);

        if (rb == null) {
            PWResetModelImpl.debug.error(
               "could not get ResourceBundle for " + PWResetModel.DEFAULT_RB);
        }
    }
    
    /**
     * Writes a log record to the password reset log file.
     * The key to the message to be written is passed in and will be
     * read from the password reset properties file.
     *
     * @param key to look up in the properties file
     */
    public void doLogKey(String key) {
        if (logStatus) {
	    doLog(Locale.getString(rb, key, PWResetModelImpl.debug));
        }
    }

    /**
     * Writes a log record to the password reset log file.
     * The message to be written is built from a key and a message 
     * The key is used to access the properties file.
     *
     * @param key to look up in the properties file
     * @param message to write with the key
     */
    public void doLog(String key, String message) {
        if (logStatus) {
	    doLog(Locale.getString(rb, key, PWResetModelImpl.debug) 
	        + " " + message);
        }
    }

    /** 
     * Writes a log record to the password reset log file.
     * The message text which will be written to the log file is
     * passed in to this method and must be localized already.
     *
     * @param msgString string which is to be written to the password reset
     *        log file
     */
    public synchronized void doLog(String msgString) {
        if (logger == null) {
            if (logStatus) {
	        PWResetModelImpl.debug.error(
		    "PWResetAdminLog.doLog - no logger. Would have logged: "
		    + msgString);
            }
        } else {
            if (logStatus) {
                SSOToken adminToken = (SSOToken)AccessController.doPrivileged(
                    AdminTokenAction.getInstance());
                LogRecord lr = new LogRecord(Level.INFO, msgString, token);
                logger.log(lr, adminToken);
            }
        }
    }

    /**
     * Returns true if logging is enabled.
     *
     * @return true if logging is enabled
     */
    public boolean isEnabled() {
        return logStatus;
    }
}
