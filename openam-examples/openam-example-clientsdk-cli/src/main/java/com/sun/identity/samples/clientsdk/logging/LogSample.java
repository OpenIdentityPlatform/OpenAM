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
 * $Id: LogSample.java,v 1.5 2008/07/17 05:05:11 bigfatrat Exp $
 *
 */

package com.sun.identity.samples.clientsdk.logging;


import java.io.*;
import java.util.*;
import java.security.Principal;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.log.AMLogException;
import com.sun.identity.log.Logger;
import com.sun.identity.log.LogRecord;

/**
 * This sample demonstrates use of the log writing portion of
 * the Logging API.  It also demonstrates the login process and
 * retrieving of the SSOTokens.  Two users are necessary for this
 * sample:
 *  1. the "subject" userid to which the LogRecord refers
 *     (shown in the "LoginID" field); and
 *  2. the "logging" userid (shown in the "LoggedBy" field).
 * The "logging" userid, if not "amAdmin", must have explicit
 * log writing privileges as provided by being a member of a group
 * or role with that privilege.
 *
 * @see com.sun.identity.log.LogRecord
 * @see com.sun.identity.log.Logger
 */
public class LogSample {
    static final String DEF_REALM = "/";
    static final String DEF_USERNAME = "user1";
    static final String DEF_USERPSWD = "user1password";
    static final String DEF_LOGNAME = "TestLog";
    static final String DEF_LOGMSG = "Test Log Record";
    static final String DEF_LOGGEDBY = "amadmin";
    static final String DEF_LOGGEDBYPSWD = "amadminpswd";
    static final String DEF_MODULENAME = "MyModule";

    LogSampleUtils sampleUtils = null;
    SSOToken userSSOToken = null;
    SSOToken loggerSSOToken = null;
    AuthContext userAC = null;
    AuthContext loggerAC = null;

    public LogSample () {
        sampleUtils = new LogSampleUtils();
    }

    public static void main(String[] args) {
        
        LogSample ls = new LogSample();
        ls.logWriteProcessing();

        System.exit(0);

    }

    private void logWriteProcessing() {
        /*
         *  get:
         *    1. subject userid (subject of the LogRecord)
	 *    2. subject userid's password
         *    3. Log filename to log to
         *    4. LogRecord's "data"
         *    5. LoggedBy userid (who's doing the logging)
         *    6. LoggedBy userid's password
         *    7. Realm (for both subject userid and LoggedBy userid
         *       in this sample)
         */

        String userSID = sampleUtils.getLine("Subject Userid", DEF_USERNAME);
        String userPWD = sampleUtils.getLine("Subject Userid " + userSID +
            "'s password", DEF_USERPSWD);
        String logName = sampleUtils.getLine("Log file", DEF_LOGNAME);
        String message = sampleUtils.getLine("Log message", DEF_LOGMSG);;
        String loggedBySID = sampleUtils.getLine("LoggedBy Userid",
            DEF_LOGGEDBY);
        String loggedByPWD = sampleUtils.getLine("LoggedBy Userid's password",
            DEF_LOGGEDBYPSWD);
        String realmName = sampleUtils.getLine("Realm", DEF_REALM);

        // get AuthContexts for subject userid and loggedby userid
        try {
            userAC = new AuthContext(realmName);
            loggerAC = new AuthContext(realmName);
        } catch (AuthLoginException le) {
            System.err.println(
                "LogSampleUtils: could not get AuthContext for realm " +
                realmName);
            System.exit(2);
        }


        // do user and loggedby login and get the SSOToken
        try {
            userSSOToken = sampleUtils.realmLogin(userSID, userPWD, userAC);
            loggerSSOToken =
                sampleUtils.realmLogin(loggedBySID, loggedByPWD, loggerAC);
        } catch (SSOException ssoe) {
            System.err.println (
                "logWriteProcessing: could not get SSOToken: " +
                ssoe.getMessage());
            System.exit(3);
        } catch (AuthLoginException ale) {
            System.err.println (
                "logWriteProcessing: could not authenticate: " +
                ale.getMessage());
            System.exit(4);
        } catch (Exception e) {
            System.err.println (
                "logWriteProcessing: exception getting SSOToken: " +
                e.getMessage());
            System.exit(5);
        }

        try {
            LogRecord logRecord = 
                new LogRecord(java.util.logging.Level.INFO, message,
                    userSSOToken);
            logRecord.addLogInfo("ModuleName", DEF_MODULENAME);
            java.net.InetAddress ipAddr = java.net.InetAddress.getLocalHost();
            logRecord.addLogInfo("IPAddr", ipAddr.getHostAddress());

            Logger logger = (Logger)Logger.getLogger(logName);
            logger.log(logRecord, loggerSSOToken);

            System.out.println("LogSample: Logging Successful !!!");

            userAC.logout();
            loggerAC.logout();
        } catch (AMLogException amex) {
            System.err.println("LogSample: AMLogException: " +
                amex.getMessage());
            System.err.println("LogSample: Logging Failed; " +
                "Is user '" + loggedBySID +
                "' a member of a Role or Group with log writing privileges?");
        } catch (Exception ssoe) {
            System.err.println("LogSample: Exception: " + ssoe.getMessage());
            System.err.println("LogSample: Logging Failed !!!");
        }
    }
}
