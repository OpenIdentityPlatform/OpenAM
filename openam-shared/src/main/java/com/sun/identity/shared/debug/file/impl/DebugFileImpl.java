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
 * Portions Copyrighted 2014-2015 ForgeRock AS.
 */
package com.sun.identity.shared.debug.file.impl;

import com.sun.identity.shared.debug.DebugConstants;
import com.sun.identity.shared.debug.file.DebugConfiguration;
import com.sun.identity.shared.debug.file.DebugFile;
import com.sun.identity.shared.locale.Locale;
import org.forgerock.util.time.TimeService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manage a log file :
 * - compute its complete name
 * - create log directory
 * - manage the log rotation
 */
public class DebugFileImpl implements DebugFile {

    private final TimeService clock;

    private final String debugName;

    private final String debugDirectory;

    private PrintWriter debugWriter = null;

    private long fileCreationTime = 0;

    private long nextRotation = 0;

    private final SimpleDateFormat suffixDateFormat;

    private final ReadWriteLock fileLock = new ReentrantReadWriteLock();

    private final DebugConfiguration configuration;

    /**
     * Constructor
     *
     * @param configuration debug configuration
     * @param debugName     log file name
     * @param debugFilePath log file path
     */
    public DebugFileImpl(DebugConfiguration configuration, String debugName, String debugFilePath) {
        this(configuration, debugName, debugFilePath, TimeService.SYSTEM);
    }

    /**
     * Constructor
     *
     * @param configuration debug configuration
     * @param debugName     log file name
     * @param debugFilePath log file path
     * @param clock         Clock used to generate date
     */
    public DebugFileImpl(DebugConfiguration configuration, String debugName, String debugFilePath, TimeService clock) {
        this.debugName = debugName;
        this.debugDirectory = debugFilePath;
        this.clock = clock;
        this.configuration = configuration;

        //initialize SimpleDateFormat
        SimpleDateFormat tmpSuffixDateFormat = null;
        if (!configuration.getDebugSuffix().isEmpty()) {
            try {
                tmpSuffixDateFormat = new SimpleDateFormat(configuration.getDebugSuffix());
            } catch (IllegalArgumentException iae) {
                // cannot debug as we are debug
                String message = "An error occurred with the date format suffix : '" + configuration.getDebugSuffix() +
                        "'. Please check the configuration file '" + DebugConstants.CONFIG_DEBUG_PROPERTIES + "'.";
                StdDebugFile.printError(debugName, message, iae);
                tmpSuffixDateFormat = new SimpleDateFormat(DebugConstants.DEFAULT_DEBUG_SUFFIX_FORMAT);
            }
        }
        this.suffixDateFormat = tmpSuffixDateFormat;
    }

    @Override
    public void writeIt(StringBuilder buf, String msg, Throwable th) throws IOException {

        if (debugWriter == null) {
            initialize();
        } else if (needsRotate()) {
            rotate();
        }
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

        // printing is the printer can be consider here as a reading access to it
        fileLock.readLock().lock();
        try {
            debugWriter.println(buf.toString());
        } finally {
            fileLock.readLock().unlock();
        }

    }

    /**
     * Close the log file
     */
    private void close() {
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
        if (configuration.getDebugPrefix() != null) {
            newFileName.append(configuration.getDebugPrefix());
        }

        //Set name
        newFileName.append(fileName);

        //Set suffix
        if (suffixDateFormat != null && configuration.getRotationInterval() > 0) {
            synchronized (suffixDateFormat) {
                newFileName.append(suffixDateFormat.format(new Date(fileCreationTime)));
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
            // Is rounded to the lower minute
            fileCreationTime -= fileCreationTime % (1000 * 60);

            nextRotation = fileCreationTime + configuration.getRotationInterval() * 60 * 1000;

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
                        this, ioex);
            }
        }
    }

    /**
     * Check if a log rotation is needed
     *
     * @return true if the log file need to be rotate
     */
    private boolean needsRotate() {
        if (configuration.getRotationInterval() > 0) {

            return nextRotation <= clock.now();
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

            fileLock.writeLock().lock();
            try {
                if (this.debugWriter != null) {
                    close();

                }
                initialize();
            } finally {
                fileLock.writeLock().unlock();
            }
        }
    }

    @Override
    public String toString() {
        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss:SSS a zzz");
        return "DebugFileImpl{" +
                "debugName='" + debugName + '\'' +
                ", debugDirectory='" + debugDirectory + '\'' +
                ", fileCreationTime=" + dateFormat.format(new Date(fileCreationTime)) +
                '}';

    }
}
