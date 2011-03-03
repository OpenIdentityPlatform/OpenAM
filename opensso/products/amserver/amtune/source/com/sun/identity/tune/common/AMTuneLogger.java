/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: AMTuneLogger.java,v 1.2 2008/08/29 10:13:08 kanduls Exp $
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.tune.common;

import com.sun.identity.tune.constants.AMTuneConstants;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * This class writes log to file, it is singletone implementation.
 *
 */
public class AMTuneLogger {

    private static AMTuneLogger pLogger;
    private static boolean loggerInit = false;
    private static String logTemplate;
    private static Logger logger;
    private static SimpleFormatter simpleF;
    private SimpleDateFormat dateFormat;
    private FileHandler fileH;
    private String debugLogsFile;
    /**
     * Constructs instance of the logger.
     * @return
     */
    public static AMTuneLogger getLoggerInst() {
        if (!loggerInit) {
            pLogger = new AMTuneLogger();
        }
        return pLogger;
    }

    /**
     * Sets the log level.
     * @param logLevel The value should be ALL|FINE|FINER|FINEST|SEVERE|WARNING|
     *      INFO.
     */
    public static void setLogLevel(String logLevel) {
        if ((logLevel != null)) {
            logger.setLevel(Level.parse(logLevel));
        }
    }

    /**
     * Constructs AMTuneLogger instance.
     */
    private AMTuneLogger() {
        try {
            logger = Logger.getLogger("com.sun.identity.tune");
            logger.setUseParentHandlers(false);
            simpleF = new SimpleFormatter();
            dateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss:SSS a zzz");
            logTemplate = this.getClass().getName() + ".{0}: {1}";
            //Create a logs directory two levels below the cur directory
            File tempF = new File("tempdbk");
            String filePath = tempF.getAbsolutePath();
            filePath = filePath.replace("tempdbk", "");
            tempF.delete();
            File logDir = new File(filePath + "../.." +
                    AMTuneConstants.FILE_SEP + AMTuneConstants.LOG_DIR);
            if (!logDir.isDirectory()) {
                logDir.mkdir();
            }
            debugLogsFile = logDir.getAbsolutePath() + 
                    AMTuneConstants.FILE_SEP + "amtune-errors";
            fileH = new FileHandler(debugLogsFile);
            fileH.setFormatter(simpleF);
            logger.addHandler(fileH);
            loggerInit = true;
        } catch (Exception ex) {
            System.err.println("Couldn't initialize logger");
            ex.printStackTrace();
        }
    }

    /**
     * Writes log message to file
     *
     * @param level Log Level.
     * @param methodName Method name.
     * @param message Message to be written to the file.
     */
    public void log(Level level, String methodName, Object message) {
        Object[] args = {methodName, message};
        String msg = MessageFormat.format(logTemplate, args);
        logger.log(level, msg);
    }

    /**
     * Writes a log entry.
     */
    public void logException(String methodName, Throwable th) {
        String prefix = this.dateFormat.format(new Date())
                        + ": " + Thread.currentThread().toString();
        StringBuilder buf = new StringBuilder(prefix);
        buf.append("\n");
        if(th != null) {
            buf.append('\n');
            StringWriter stBuf = new StringWriter(300);
            PrintWriter stackStream = new PrintWriter(stBuf);
            th.printStackTrace(stackStream);
            stackStream.flush();
            buf.append(stBuf.toString());
        }
        String message = buf.toString();
        Object[] args = {methodName, message};
        logger.log(Level.SEVERE, MessageFormat.format(logTemplate, args));
        //System.err.println(MessageFormat.format(logTemplate, args));
    }
    /**
     * Get debug log file name
     */
    public String getLogFilePath() {
        return debugLogsFile;
    }
    /**
     * Closes the logger.
     */
    public void close() {
        if(fileH != null) {
            fileH.flush();
            fileH.close();
        }
        fileH = null;
    }
}
