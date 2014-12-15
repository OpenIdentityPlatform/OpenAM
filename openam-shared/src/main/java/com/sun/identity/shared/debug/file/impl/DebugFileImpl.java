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
 * $Id: DebugImpl.java,v 1.4 2009/03/07 08:01:53 veiming Exp $
 *
 */

/**
 * Portions Copyrighted 2014 ForgeRock AS
 */
package com.sun.identity.shared.debug.file.impl;

import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.DebugConstants;
import com.sun.identity.shared.debug.file.DebugFile;
import com.sun.identity.shared.locale.Locale;
import org.forgerock.openam.utils.IOUtils;
import org.forgerock.util.time.TimeService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * Manage a log file :
 * - compute its complete name
 * - create log directory
 * - manage the log rotation
 */
public class DebugFileImpl implements DebugFile {

    //static
    private static String debugPrefix;

    private static String debugSuffix;

    private static int rotationInterval = -1;

    private final TimeService clock;

    static {
        initProperties();
    }

    //local
    private final String debugName;

    private final String debugDirectory;

    private PrintWriter debugWriter = null;

    private long fileCreationTime = 0;

    private final SimpleDateFormat suffixDateFormat;

    /**
     * Constructor
     *
     * @param debugName     log file name
     * @param debugFilePath log file path
     */
    public DebugFileImpl(String debugName, String debugFilePath) {
        this(debugName, debugFilePath, TimeService.SYSTEM);
    }

    /**
     * Constructor
     *
     * @param debugName     log file name
     * @param debugFilePath log file path
     */
    public DebugFileImpl(String debugName, String debugFilePath, TimeService clock) {
        this.debugName = debugName;
        this.debugDirectory = debugFilePath;
        this.clock = clock;

        //initialize SimpleDateFormat
        SimpleDateFormat tmpSuffixDateFormat = null;
        if (!debugSuffix.isEmpty()) {
            try {
                tmpSuffixDateFormat = new SimpleDateFormat(debugSuffix);
            } catch (IllegalArgumentException iae) {
                // cannot debug as we are debug
                String message = "An error occurred with the date format suffix : '" + debugSuffix +
                        "'. Please check the configuration file '" + DebugConstants.CONFIG_DEBUG_PROPERTIES
                        + "'.";
                StdDebugFile.printError(debugName, message, iae);
                tmpSuffixDateFormat = new SimpleDateFormat(DebugConstants.DEFAULT_DEBUG_SUFFIX_FORMAT);
            }
        }
        this.suffixDateFormat = tmpSuffixDateFormat;
    }

    /**
     * Write log in the log file
     *
     * @param prefix Message prefix
     * @param msg    Message to be recorded.
     * @param th     the optional <code>java.lang.Throwable</code> which if
     *               present will be used to record the stack trace.
     * @throws IOException
     */
    public void writeIt(String prefix, String msg, Throwable th) throws IOException {

        if (debugWriter == null) {
            initialize();
        } else if (needsRotate()) {
            rotate();
        }
        StringBuilder buf = new StringBuilder(prefix);
        buf.append('\n');
        buf.append(msg);
        if (th != null) {
            buf.append('\n');
            StringWriter stBuf = new StringWriter(DebugConstants.MAX_BUFFER_SIZE_EXCEPTION);
            PrintWriter stackStream = new PrintWriter(stBuf);
            th.printStackTrace(stackStream);
            stackStream.flush();
            buf.append(stBuf.toString());
        }
        debugWriter.println(buf.toString());
    }

    /**
     * Close the log file
     */
    public void close() {
        this.debugWriter.flush();
        this.debugWriter.close();
        this.debugWriter = null;
    }

    /**
     * Compute the final log file name (prefix and suffix)
     *
     * @param fileName the log file name base
     * @return the complete log file name
     */
    private String wrapFilename(String fileName) {
        StringBuilder newFileName = new StringBuilder();

        //Set prefix
        if (debugPrefix != null) {
            newFileName.append(debugPrefix);
        }

        //Set name
        newFileName.append(fileName);

        //Set suffix
        if (suffixDateFormat != null && rotationInterval > 0) {
            synchronized (suffixDateFormat) {
                newFileName.append(suffixDateFormat.format(new Date(clock.now())));
            }
        }

        return newFileName.toString();
    }

    /**
     * Initialize a new log file
     *
     * @throws IOException
     */
    private synchronized void initialize() throws IOException {
        if (this.debugWriter == null) {
            // remember when we rotated last
            fileCreationTime = clock.now();

            //Create the log directory
            boolean directoryAvailable = false;
            if (debugDirectory != null && debugDirectory.trim().length() > 0) {
                File dir = new File(debugDirectory);
                if (!dir.exists()) {
                    directoryAvailable = dir.mkdirs();
                } else if (dir.isDirectory() && dir.canWrite()) {
                    directoryAvailable = true;
                }
            }

            if (!directoryAvailable) {
                ResourceBundle bundle = Locale.getInstallResourceBundle("amUtilMsgs");
                throw new IOException(bundle.getString("com.iplanet.services.debug.nodir") + " Current Debug File : "
                        + this);
            }

            //Create the log file
            String debugFilePath = debugDirectory + File.separator + wrapFilename(debugName);

            try {
                this.debugWriter = new PrintWriter(new FileWriter(debugFilePath, true), true);
            } catch (IOException ioex) {
                if (this.debugWriter != null) {
                    close();
                }
                ResourceBundle bundle = Locale.getInstallResourceBundle("amUtilMsgs");
                throw new IOException(bundle.getString("com.iplanet.services.debug.nofile") + " Current Debug File : " +
                        "" + this, ioex);
            }
        }
    }

    /**
     * Check if a log rotation is needed
     *
     * @return true if the log file need to be rotate
     */
    private boolean needsRotate() {
        if (rotationInterval > 0) {
            //init calendar
            Calendar now = Calendar.getInstance();
            now.setTimeInMillis(clock.now());
            Calendar then = Calendar.getInstance();
            then.setTimeInMillis(fileCreationTime);

            //compare dates
            then.add(Calendar.MINUTE, rotationInterval);
            return now.after(then);
        }
        return false;
    }

    /**
     * Rotate log file
     *
     * @throws IOException
     */
    private synchronized void rotate() throws IOException {
        if (needsRotate()) {
            if (this.debugWriter != null) {
                close();
            }
            initialize();
        }
    }

    /**
     * initialize the properties
     * It will reset the current properties for every Debug instance
     */
    public static synchronized void initProperties() {
        InputStream is = null;
        try {
            is = DebugFileImpl.class.getResourceAsStream(DebugConstants.CONFIG_DEBUG_PROPERTIES);
            if (is == null && SystemPropertiesManager.get(DebugConstants.CONFIG_DEBUG_PROPERTIES_VARIABLE) != null) {
                is = DebugFileImpl.class.getResourceAsStream(SystemPropertiesManager.get(DebugConstants
                        .CONFIG_DEBUG_PROPERTIES_VARIABLE));
            }
            Properties rotationConfig = new Properties();
            rotationConfig.load(is);

            debugPrefix = rotationConfig.getProperty(DebugConstants.CONFIG_DEBUG_LOGFILE_PREFIX);
            debugSuffix = rotationConfig.getProperty(DebugConstants.CONFIG_DEBUG_LOGFILE_SUFFIX);
            String rotation = rotationConfig.getProperty(DebugConstants.CONFIG_DEBUG_LOGFILE_ROTATION);
            if (!rotation.isEmpty()) {
                try {
                    rotationInterval = Integer.parseInt(rotation);
                } catch (NumberFormatException e) {
                    //Can't parse the number
                    String message = "'" + DebugConstants.CONFIG_DEBUG_LOGFILE_ROTATION + "' value can't be parse :" +
                            " '" + rotation + "'. Please " + "check the configuration file '" + DebugConstants
                            .CONFIG_DEBUG_PROPERTIES + "'.";
                    StdDebugFile.printError(DebugFile.class.getSimpleName(), message, e);
                    rotationInterval = -1;
                }
            }
        } catch (Exception ex) {
            //it's possible, that we don't have the config file
            String message = "Can't load debug file properties. Please check the configuration file '" + DebugConstants.CONFIG_DEBUG_PROPERTIES + "'.";
            StdDebugFile.printError(DebugFile.class.getSimpleName(), message, ex);
        } finally {
            IOUtils.closeIfNotNull(is);
        }

    }


    @Override
    public String toString() {
        synchronized (suffixDateFormat) {
            return "DebugFileImpl{" +
                    "debugName='" + debugName + '\'' +
                    ", debugDirectory='" + debugDirectory + '\'' +
                    ", fileCreationTime=" + suffixDateFormat.format(new Date(fileCreationTime)) +
                    '}';
        }
    }
}
