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
 * Portions Copyrighted 2014-2016 ForgeRock AS.
 */
package com.sun.identity.shared.debug.file.impl;

import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.DebugConstants;
import com.sun.identity.shared.debug.file.DebugConfiguration;
import com.sun.identity.shared.debug.file.DebugFile;
import com.sun.identity.shared.locale.Locale;
import org.forgerock.openam.utils.IOUtils;
import org.forgerock.openam.utils.StringUtils;
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
import java.util.concurrent.atomic.AtomicReference;
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

    private volatile PrintWriter debugWriter = null;

    private long fileCreationTime = 0;

    private long nextRotation = 0;

    private final SimpleDateFormat suffixDateFormat;

    private final DebugConfiguration configuration;

    private final AtomicReference<String> debugDirectory = new AtomicReference<>();

    private final ReadWriteLock fileLock = new ReentrantReadWriteLock();

    private File currentFile;

    /**
     * Constructor
     *
     * @param configuration debug configuration
     * @param debugName     log file name
     */
    public DebugFileImpl(DebugConfiguration configuration, String debugName) {
        this(configuration, debugName, TimeService.SYSTEM);
    }

    /**
     * Constructor
     *
     * @param configuration debug configuration
     * @param debugName     log file name
     * @param clock         Clock used to generate date
     */
    public DebugFileImpl(DebugConfiguration configuration, String debugName, TimeService clock) {
        this.debugName = debugName;
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

    private boolean isConfigChanged() throws IOException {
        String newDebugDir = SystemPropertiesManager.get(DebugConstants.CONFIG_DEBUG_DIRECTORY);
        if (StringUtils.isEmpty(newDebugDir)) {
            ResourceBundle bundle = Locale.getInstallResourceBundle("amUtilMsgs");
            throw new IOException(bundle.getString("com.iplanet.services.debug.nodir") + " Current Debug File : " +
                    this);
        }
        return !newDebugDir.equals(debugDirectory.getAndSet(newDebugDir));
    }

    @Override
    public void writeIt(String prefix, String msg, Throwable th) throws IOException {

        if (isConfigChanged()) {
            initialize();
        }

        if (needsTimeRotation() || needsSizeRotation()) {
            rotate();
        }

        StringBuilder buf = new StringBuilder();
        buf.append(prefix);
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
        IOUtils.closeIfNotNull(debugWriter);
        this.debugWriter = null;
        this.currentFile = null;
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
        if (suffixDateFormat != null &&
                (configuration.getRotationInterval() > 0 || configuration.getRotationFileSizeInByte() > 0)) {
            synchronized (suffixDateFormat) {
                newFileName.append(suffixDateFormat.format(new Date(fileCreationTime)));
            }
        }

        return newFileName.toString();
    }

    /**
     * Initialize a new log file - setting up the directory if necessary.
     *
     * @throws IOException
     */
    private void initialize() throws IOException {

        fileLock.writeLock().lock();

        try {
            //if we're switching to a new directory & writer or rotating, flush
            close();

            // remember when we rotated last
            fileCreationTime = clock.now();

            //only if the size rotation is off
            if (configuration.getRotationFileSizeInByte() == -1) {
                // Is rounded to the lower minute
                fileCreationTime -= fileCreationTime % (1000 * 60);
            }

            nextRotation = fileCreationTime + configuration.getRotationInterval() * 60 * 1000;
            boolean directoryAvailable = false;
            String debugDir = debugDirectory.get();

            //Create the log directory
            if (debugDir != null && debugDir.trim().length() > 0) {
                File dir = new File(debugDir);
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
            String debugFilePath = debugDirectory.get() + File.separator + wrapFilename(debugName);

            try {
                this.currentFile = new File(debugFilePath);
                this.debugWriter = new PrintWriter(new FileWriter(currentFile, true), true);
                debugWriter = new PrintWriter(new FileWriter(debugFilePath, true), true);
            } catch (IOException ioex) {
                close();
                ResourceBundle bundle = Locale.getInstallResourceBundle("amUtilMsgs");
                throw new IOException(bundle.getString("com.iplanet.services.debug.nofile") + " Current Debug File : " +
                        this, ioex);
            }
        } finally {
            fileLock.writeLock().unlock();
        }
    }

    /**
     * Check if a log rotation is needed depending on the file size
     *
     * @return true if the log file need to be rotated
     */
    private boolean needsSizeRotation() {
        if (currentFile == null) {
            return false;
        }

        return configuration.getRotationFileSizeInByte() != -1
                && currentFile.length() >= configuration.getRotationFileSizeInByte();
    }

    /*
     * Check if a log rotation is needed depending on the time interval
     *
     * @return true if the log file need to be rotated
     */
    private boolean needsTimeRotation() {
        return configuration.getRotationInterval() > 0 && nextRotation <= clock.now();
    }

    private synchronized void rotate() throws IOException {
        if (needsTimeRotation() || needsSizeRotation()) {
            initialize();
        }
    }

    @Override
    public String toString() {
        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss:SSS a zzz");
        return "DebugFileImpl{" +
                "debugDirectory" + debugDirectory.get() +
                "debugName='" + debugName + '\'' +
                ", fileCreationTime=" + dateFormat.format(new Date(fileCreationTime)) +
                '}';

    }
}
