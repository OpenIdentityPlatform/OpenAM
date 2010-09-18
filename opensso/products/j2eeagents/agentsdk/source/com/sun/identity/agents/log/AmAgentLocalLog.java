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
 * $Id: AmAgentLocalLog.java,v 1.2 2008/06/25 05:51:53 qcheng Exp $
 *
 */

package com.sun.identity.agents.log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.sun.identity.agents.arch.AgentBase;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.LocalizedMessage;
import com.sun.identity.agents.arch.Manager;


/** 
 * The class does agent local logging
 */
public class AmAgentLocalLog extends AgentBase 
implements ILogConfigurationConstants, IAmAgentLocalLog 
{

    public AmAgentLocalLog(Manager manager) {
        super(manager);
    }
    
    public void initialize() throws AgentException {
        try {
            setLogFileAndWriter();
            setLogFileRotationFlag();
            setLogFileRotationSize();
            setDateFormat();
            setFileSuffixDateFormat();
        } catch(Exception ex) {
            throw new AgentException("Unable to initialize Local Log Handler",
                                     ex);
        }        
    }

    public boolean log(LocalizedMessage message) {
        boolean result = false;
        try {
            if(message == null) {
                throw new IllegalArgumentException("message = " + message);
            }
            result = recordMessage(getFormattedMessage(message));
        } catch(Exception ex) {
            logError("AmAgentLocalLog: Unable to process log request: "
                     + " message = " + message, ex);

            result = false;
        }

        return result;
    }

    private String getFormattedMessage(LocalizedMessage message) {
        return getDateStamp() + message;
    }

    private String getDateStamp() {
        return "[" + getDateFormat().format(new Date()) + "] ";
    }

    private synchronized final boolean recordMessage(String message)
            throws Exception {
        getLogWriter().println(message);
        if(isLogRotationEnabled()) {
            rotateLogFile();
        }

        return true;
    }

    private synchronized final void rotateLogFile() throws Exception {
        PrintWriter writer = getLogWriter();
        File logFile = getLogFile();
        writer.flush();
        if(logFile.length() >= getLogFileRotationSize()) {
            writer.close();
            String lastFile = getLogFileCanonicalPath() + getFileSuffixString();
            logFile.renameTo(new File(lastFile));
            resetLogFileAndWriter();
        }
    }

    private void setLogFileAndWriter() throws AgentException {

        String      fileName  = null;
        PrintWriter logWriter = null;
        File        logFile   = null;

        try {
            fileName = getConfiguration(CONFIG_LOG_LOCAL_FILE).replace(
                    '/', File.separatorChar).replace(
                    '\\', File.separatorChar);
            logFile = new File(fileName);

            if(logFile.exists()) {
                if( !logFile.canWrite()) {
                    logError("AmAgentLocalLog: Log file is not writable: "
                             + fileName);

                    throw new AgentException("Log file not writable: "
                                             + fileName);
                }
            } else {
                try {
                    int    length = fileName.lastIndexOf(File.separatorChar);
                    String basePath = fileName.substring(0, length + 1);
                    File   pathDirs = new File(basePath);
                    
                    if (!pathDirs.exists()) {
                        if (!pathDirs.mkdirs()) { 
                            throw new AgentException("Failed to create file/dir:"
                                        + fileName);
                        }
                    }

                    if (!logFile.createNewFile()) {
                        throw new AgentException("Failed to create file/dir:"
                                       + fileName);
                    }

                } catch(Exception ex) {
                    logError("AmAgentLocalLog: Unable to create Log file: "
                             + fileName, ex);

                    throw new AgentException(
                        "Unable to create Local Log File: " + fileName, ex);
                }
            }

            try {
                logWriter =
                    new PrintWriter(new FileOutputStream(fileName, true),
                                    true);
            } catch(Exception ex) {
                logError("AmAgentLocalLog: Unable to set log writer", ex);

                throw new AgentException("Unable to set log writer", ex);
            }
        } catch(Exception ex) {
            logError("AmAgentLocalLog: Unable to set Local Log File", ex);

            throw new AgentException("Unable to set Local Log File", ex);
        }

        _logFileCanonicalPath = fileName;
        _logWriter            = logWriter;
        _logFile              = logFile;

        if(isLogMessageEnabled()) {
            logMessage("AmAgentLocalLog: Log file is set to: "
                       + _logFileCanonicalPath);
            logMessage("AmAgentLocalLog: Log file writer is: " + _logWriter);
        }
    }

    private boolean isLogRotationEnabled() {
        return _rotationEnable;
    }

    private void setLogFileRotationFlag() {

        _rotationEnable = getConfigurationBoolean(
            CONFIG_LOG_LOCAL_FILE_ROTATE_ENABLE,
            DEFAULT_LOG_LOCAL_FILE_ROTATE_ENABLE);

        if(isLogMessageEnabled()) {
            logMessage("AmAgentLocalLog: File rotation is: "
                       + _rotationEnable);
        }
    }

    private void setLogFileRotationSize() {

        _logFileRotationSize = getConfigurationLong(
            CONFIG_LOG_LOCAL_FILE_ROTATE_SIZE,
            DEFAULT_LOG_LOCAL_FILE_ROTATE_SIZE);

        if(isLogMessageEnabled()) {
            logMessage("AmAgentLocalLog: Log file rotation size is "
                       + _logFileRotationSize);
        }
    }

    private DateFormat getDateFormat() {
        return _dateFormat;
    }

    private void setDateFormat() {
        _dateFormat = DateFormat.getDateTimeInstance(DateFormat.FULL,
                DateFormat.FULL, getModule().getModuleLocale());
    }

    private long getLogFileRotationSize() {
        return _logFileRotationSize;
    }

    private String getLogFileCanonicalPath() {
        return _logFileCanonicalPath;
    }

    private PrintWriter getLogWriter() {
        return _logWriter;
    }

    private File getLogFile() {
        return _logFile;
    }

    private String getFileSuffixString() {
        return "_" + _fileSuffixDateFormat.format(new Date());
    }

    private void setFileSuffixDateFormat() {
        _fileSuffixDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    }

    private void resetLogFileAndWriter() throws Exception {

        _logFile = new File(getLogFileCanonicalPath());

        _logFile.createNewFile();

        _logWriter = new PrintWriter(
            new FileOutputStream(getLogFileCanonicalPath(), true), true);
    }

    private String           _logFileCanonicalPath;
    private File             _logFile;
    private boolean          _rotationEnable;
    private long             _logFileRotationSize;
    private PrintWriter      _logWriter;
    private DateFormat       _dateFormat;
    private SimpleDateFormat _fileSuffixDateFormat;
}

