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
 * $Id: Debug.java,v 1.2 2008/06/25 05:51:28 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.util;

import java.io.File;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import com.sun.identity.install.tools.admin.ToolsConfiguration;

public class Debug {

    public static void log(String message) {
        log(message, null);
    }

    public synchronized static void log(String message, Throwable th) {
        String prefix = LogHelper.getDateStamp();
        if (isSTDOUTEnabled() || getLogWriter() == null) {
            writeIt(prefix + message, th, getSTDOUTWriter());
        }
        writeIt(prefix + message, th, getLogWriter());
    }

    private static synchronized void writeIt(String message, Throwable th,
            PrintWriter writer) {
        if (writer != null) {
            writer.println(message);
            writer.flush();
            if (th != null) {
                th.printStackTrace(writer);
                writer.flush();
            }
        }
    }

    private static void setSTDOUTEnabled(boolean enabled) {
        stdoutEnabled = enabled;
    }

    private static boolean isSTDOUTEnabled() {
        return stdoutEnabled;
    }

    private static File getDebugLogDir() {
        return new File(getDebugLogDirPath());
    }

    private static void initializeDebug() {
        try {
            LogHelper.initializeLogsDir(getDebugLogDir());
            setSTDOUTEnabled(ConfigUtil.isDebugEnabled());
            LogHelper.archiveLastLogFile(getDebugLogFile());
            setSTDOUTWriter(new PrintWriter(new OutputStreamWriter(
                    System.out)));
            setLogWriter(new PrintWriter(new FileWriter(getDebugLogFile())));
            LogHelper.addLogWriterShutdownHook(getLogWriter());
        } catch (Exception ex) {
            System.err.println("Unable to initialize Debug");
            ex.printStackTrace(System.err);
        }

    }

    private static File getDebugLogFile() {
        return new File(getDebugLogDirPath() + "/" + STR_DEBUG_FILENAME);
    }

    private static String getDebugLogDirPath() {
        return ConfigUtil.getLogsDirPath() + "/" + STR_DEBUG_LOGS_DIR_NAME;
    }

    private static void setLogWriter(PrintWriter writer) {
        logWriter = writer;
    }

    private static PrintWriter getLogWriter() {
        return logWriter;
    }

    private static void setSTDOUTWriter(PrintWriter writer) {
        stdoutWriter = writer;
    }

    private static PrintWriter getSTDOUTWriter() {
        return stdoutWriter;
    }

    private static PrintWriter logWriter;

    private static PrintWriter stdoutWriter;

    private static boolean stdoutEnabled;

    public static final String STR_DEBUG_LOGS_DIR_NAME = "debug";

    public static final String STR_DEBUG_FILENAME = ToolsConfiguration
            .getProductShortName()
            + ".log";

    static {
        initializeDebug();
    }
}
