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
 * $Id: LogHelper.java,v 1.2 2008/06/25 05:51:29 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.util;

import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogHelper {

    public static void addLogWriterShutdownHook(PrintWriter writer) {
        Runtime.getRuntime().addShutdownHook(
                new Thread(new LogWriterShutdownHook(writer)));
    }

    public static void archiveLastLogFile(File logFile) {

        if (logFile.exists()) {
            int index = getLastLogIndex(logFile);

            File archivedFile = new File(logFile.getAbsolutePath() + "."
                    + index);
            if (!logFile.renameTo(archivedFile)) {
                throw new RuntimeException("Unable to archive file: log-file: "
                        + logFile.getAbsolutePath() + ", archive-file: "
                        + archivedFile.getAbsolutePath());
            }
        }

    }

    public static int getLastLogIndex(File logFile) {
        int index = 0;
        File logsDir = logFile.getParentFile();
        File[] logFiles = logsDir.listFiles();
        String archiveNamePrefix = logFile.getName() + ".";
        if (logFiles != null) {
            for (int i = 0; i < logFiles.length; i++) {
                String name = logFiles[i].getName();
                if (name.startsWith(archiveNamePrefix)) {
                    String suffix = name.substring(archiveNamePrefix.length());

                    try {
                        int nextIndex = Integer.parseInt(suffix);
                        if (nextIndex > index) {
                            index = nextIndex;
                        }
                    } catch (NumberFormatException nfex) {
                        // No handling required
                    }
                }
            }
        }

        return index + 1;
    }

    public static void initializeLogsDir(File logsDir) throws Exception {
        if (!logsDir.exists()) {
            if (!logsDir.mkdirs()) {
                throw new Exception("Unable to create logs dir: "
                        + logsDir.getAbsolutePath());
            }
        } else {
            if (!logsDir.canWrite()) {
                throw new Exception("Unable to write to logs dir: "
                        + logsDir.getAbsolutePath());
            }
        }
    }

    private static class LogWriterShutdownHook implements Runnable {

        public void run() {
            try {
                PrintWriter writer = getLogWriter();
                if (writer != null) {
                    writer.flush();
                    writer.close();
                }
            } catch (Exception ex) {
                // No handling required
            }
        }

        private LogWriterShutdownHook(PrintWriter writer) {
            setLogWriter(writer);
        }

        private void setLogWriter(PrintWriter writer) {
            _logWriter = writer;
        }

        private PrintWriter getLogWriter() {
            return _logWriter;
        }

        private PrintWriter _logWriter;
    }

    public static String getDateStamp() {
        return getDateFormat().format(new Date());
    }

    private static SimpleDateFormat getDateFormat() {
        return dateFormat;
    }

    public static final String STR_DATE_FORMAT = 
        "[MM/dd/yyyy HH:mm:ss:SSS z] ";

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat(
            STR_DATE_FORMAT);
}
