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
 * $Id: AMAdminLog.java,v 1.2 2008/06/25 05:42:49 qcheng Exp $
 *
 */

package com.sun.identity.console.base.model;

import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOToken;
import com.sun.identity.log.Logger;
import com.sun.identity.log.LogRecord;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ResourceBundle;
import java.util.logging.Level;

/* - NEED NOT LOG - */

/**
 * This class allows you to perform log.  It is strongly suggested that you
 * should log all the critical and data updating events such updating a LDAP
 * attributes and removing an LDAP entry.
 */
public class AMAdminLog {
    private static Debug debug = Debug.getInstance(
        AMAdminConstants.CONSOLE_DEBUG_FILENAME);
    private static final String accessLogFile = "amConsole.access";
    private static final String errorLogFile = "amConsole.error";
    private static AMAdminLog instance = new AMAdminLog();

    private Logger accessLogger = null;
    private Logger errorLogger = null;
    private java.util.Locale locale = null;
    private SSOToken ssoToken = null;
    private ResourceBundle resBundle = null;
    private boolean logStatus = false;

    private AMAdminLog() {
        getSuperAdminSSOToken();

        String status = SystemProperties.get(Constants.AM_LOGSTATUS);
        logStatus = status.equalsIgnoreCase(AMAdminConstants.STRING_ACTIVE);

        if (logStatus) {
            accessLogger = (com.sun.identity.log.Logger)Logger.getLogger(
                accessLogFile);
            errorLogger = (com.sun.identity.log.Logger)Logger.getLogger(
                errorLogFile);

            String lstr = SystemProperties.get(Constants.AM_LOCALE);
            locale = com.sun.identity.shared.locale.Locale.getLocale(lstr);
            resBundle = AMResBundleCacher.getBundle(
                AMAdminConstants.DEFAULT_RB, locale);
        }
    }

    private void getSuperAdminSSOToken() {
        ssoToken = (SSOToken)AccessController.doPrivileged(
            new PrivilegedAction() {
                public Object run() {
                    try {
                        return AMAdminUtils.getSuperAdminSSOToken();
                    } catch (SecurityException e) {
                        debug.error("AMAdminLog.getSuperAdminSSOToken", e);
                        return null;
                    }
                }
            });
    }

    /**
     * Returns an instance of the logger. 
     *
     * @return an instance of the logger. 
     */
    public static AMAdminLog getInstance() {
        return instance;
    }

    public void doLog(LogRecord rec) {
        if (rec != null) {
            if (rec.getLevel().equals(Level.INFO)) {
                doLog(accessLogger, rec);
            } else {
                doLog(errorLogger, rec);
            }
        }
    }

    private void doLog(Logger logger, LogRecord rec) {
        if (logger == null) {
            if (logStatus) {
                debug.error(
                    "AMAdminLog.doLog - no logger. Would have logged: " +
                    rec.getMessage());
            }
        } else {
            try {
                logger.log(rec, ssoToken);
            } catch (Exception log) {
                debug.error(
                    "AMAdminLog.doLog - problem writing to log " + 
                    log.getMessage());
            }

        }
    }
}
