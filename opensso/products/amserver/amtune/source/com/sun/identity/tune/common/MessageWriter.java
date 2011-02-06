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
 * $Id: MessageWriter.java,v 1.3 2008/08/29 10:13:08 kanduls Exp $
 */

package com.sun.identity.tune.common;

import com.sun.identity.tune.constants.AMTuneConstants;
import com.sun.identity.tune.util.AMTuneUtil;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ResourceBundle;

/**
 * Writes the messages to the file and to terminal.
 *
 */
public class MessageWriter {

    private final String logName = "amtune-config";
    private PrintWriter logFile = null;
    private String logFilePath = null;
    private static boolean writeToFile = true;
    private static boolean writeToTerm = true;
    private static MessageWriter logObj;
    private ResourceBundle rb;

    /**
     * Initializes the MessageWriter Singletone implementation.
     */

    private MessageWriter()
    throws AMTuneException {
        try {
            File logDir = new File(AMTuneUtil.getCurDir() +
                    "../.." + AMTuneConstants.FILE_SEP + 
                    AMTuneConstants.LOG_DIR);
            if (!logDir.isDirectory()){
                logDir.mkdirs();
            }
            logFilePath = logDir.getAbsolutePath() + 
                    AMTuneConstants.FILE_SEP + generateLogFileName(logName);
            rb = ResourceBundle.getBundle(AMTuneConstants.RB_NAME);
        } catch (Exception ex) {
            throw new AMTuneException(ex.getMessage());
        }
    }

    /**
     * If set to true the messages will be written to the file.
     * @param toFile
     */
    public static void setWriteToFile(boolean toFile) {
        writeToFile = toFile;
    }

    /**
     * If set to true the messages will be written to the terminal.
     * @param toFile
     */
    public static void setWriteToTerm(boolean toTerm) {
        writeToTerm = toTerm;
    }

    /**
     * Returns the instance of MessageWriter object.
     * @return MessagerWriter object
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    public static MessageWriter getInstance()
    throws AMTuneException {
        if (logObj == null){
            logObj = new MessageWriter();
        }
	return logObj;
    }

    /**
     * Writes l10n Message and appends new line.
     *
     * @param msg key for the desired string
     */
    public void writelnLocaleMsg(String msg) {
        try {
            writeln(rb.getString(msg).replace("\n", ""));
        } catch (Exception ex) {
            writeln(msg);
        }
    }

    /**
     * Writes l10n Message.
     *
     * @param msg key for the desired string
     */
    public void writeLocaleMsg(String msg) {
        try {
            write(rb.getString(msg).replace("\n", ""));
        } catch (Exception ex) {
            write(msg);
        }
    }

    /**
     * Writes message to file or terminal.
     *
     * @param msg Message to be written.
     */
    public void write(String msg) {
        try {
            // First, see if the logFile is already open. If not, open it now.
            if (logFile == null) {
                // open file in append mode
                FileOutputStream fos = new FileOutputStream(logFilePath, true);
                logFile = new PrintWriter(
                        new BufferedWriter(
                        new OutputStreamWriter(fos, "UTF-8")),
                        true); // autoflush enabled
            }
            if (writeToFile && writeToTerm) {
                logFile.print(msg);
                System.out.print(msg);
            } else if (writeToTerm) {
                System.out.print(msg);
            } else if (writeToFile) {
                logFile.print(msg);
            }
            logFile.flush();
        } catch (IOException e) {
            System.err.println(msg);
        }
    }

    /**
     * Writes message to file or terminal and appends new line.
     *
     * @param msg Message to be written.
     */
    public void writeln(String msg) {
	try {
            // First, see if the logFile is already open. If not, open it now.
            if (logFile == null) {
                // open file in append mode
                FileOutputStream fos = new FileOutputStream(logFilePath,true);
                logFile = new PrintWriter(
                    new BufferedWriter(
                    new OutputStreamWriter(fos, "UTF-8")
                    ),
                    true); // autoflush enabled
            }
            if (writeToFile && writeToTerm) {
                logFile.println(msg);
                System.out.println(msg);
            } else if (writeToTerm) {
                System.out.print(msg);
            } else if (writeToFile) {
                logFile.println(msg);
            }
            logFile.flush();
        } catch (IOException e) {
            System.err.println(msg);
        }
    }

    /**
     * Closes the file
     */
    public void close() {
        if (logFile == null) {
            return;
        }
        logFile.flush();
        logFile.close();
        logFile = null;
    }

    /**
     * Generates the log file name.
     *
     * @param prefix Prefix for the log file.
     * @return Log file name
     */
    private String generateLogFileName(String prefix) {
        if(prefix == null || prefix.length() == 0) return "";
        String logFileName = prefix + "." + AMTuneUtil.getRandomStr() + ".log";
        return logFileName;
    }
    
    /**
     * Return log file path
     * 
     */
    public String getConfigurationFilePath() {
        return logFilePath;
    }
}
