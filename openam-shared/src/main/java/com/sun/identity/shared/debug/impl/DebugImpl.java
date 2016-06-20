/*
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
 * Portions Copyrighted 2014-2016 ForgeRock AS.
 */
package com.sun.identity.shared.debug.impl;

import static org.forgerock.openam.utils.StringUtils.isNotEmpty;
import static org.forgerock.openam.utils.Time.*;

import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.DebugConstants;
import com.sun.identity.shared.debug.DebugLevel;
import com.sun.identity.shared.debug.IDebug;
import com.sun.identity.shared.debug.file.DebugFile;
import com.sun.identity.shared.debug.file.DebugFileProvider;
import com.sun.identity.shared.debug.file.impl.StdDebugFile;
import org.forgerock.openam.audit.context.AuditRequestContext;
import org.forgerock.openam.utils.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Debug implementation class.
 */
public class DebugImpl implements IDebug {

    static final Map<String, String> INSTANCE_NAMES = new ConcurrentSkipListMap<>(new WildcardComparator());

    static {
        initProperties();
    }

    private final static int DIR_ISSUE_ERROR_INTERVAL_IN_MS = 60 * 1000;
    private static final boolean SERVER_MODE = SystemPropertiesManager.getAsBoolean(Constants.SERVER_MODE);
    private static volatile long lastDirectoryIssue = 0l;

    private final String debugName;
    private boolean mergeAllMode = false;

    private DebugLevel debugLevel = DebugLevel.ON;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss:SSS a zzz");

    private DebugFileProvider debugFileProvider;
    private DebugFile debugFile = null;
    private DebugFile stdoutDebugFile;

    /**
     * Creates an instance of <code>DebugImpl</code>.
     *
     * @param debugName         Name of the debug.
     * @param debugFileProvider A debug file provider
     */
    public DebugImpl(String debugName, DebugFileProvider debugFileProvider) {
        this.debugName = debugName;

        if (SystemPropertiesManager.get(DebugConstants.CONFIG_DEBUG_LEVEL) != null) {
            setDebug(SystemPropertiesManager.get(DebugConstants.CONFIG_DEBUG_LEVEL));
        } else {
            setDebug(DebugLevel.ON);
        }

        this.debugFileProvider = debugFileProvider;
        stdoutDebugFile = debugFileProvider.getStdOutDebugFile();

        String mf = SystemPropertiesManager.get(DebugConstants.CONFIG_DEBUG_MERGEALL);
        mergeAllMode = "on".equals(mf);

        //NB : we don't initialize debugFile now, we will do it when we will write on it
    }

    /**
     * Returns debug name.
     *
     * @return debug name.
     */
    public String getName() {
        return this.debugName;
    }

    /**
     * Returns debug level.
     *
     * @return debug level.
     */
    public int getState() {
        return this.debugLevel.getLevel();
    }

    /**
     * Sets debug level.
     *
     * @param level Debug level.
     */
    public void setDebug(int level) {

        try {
            setDebug(DebugLevel.fromLevel(level));
        } catch (IllegalArgumentException e) {
            // ignore invalid level values
            StdDebugFile.printError(DebugImpl.class.getSimpleName(), e.getMessage(), e);
        }
    }

    /**
     * Sets debug level.
     *
     * @param strDebugLevel Debug level.
     */
    public void setDebug(String strDebugLevel) {

        try {
            setDebug(DebugLevel.fromName(strDebugLevel));
        } catch (IllegalArgumentException e) {
            // ignore invalid level values
            StdDebugFile.printError(DebugImpl.class.getSimpleName(), e.getMessage(), e);
        }
    }

    /**
     * Sets debug level.
     *
     * @param debugLevel Debug level.
     */
    public void setDebug(DebugLevel debugLevel) {

        this.debugLevel = debugLevel;
    }

    /**
     * Reset this instance - ths will trigger this instance to reinitialize
     * itself.
     *
     * @param mf merge flag : true: merge debugs into a single file.`
     */
    public void resetDebug(String mf) {
        mergeAllMode = "on".equals(mf);
        setDebug(SystemPropertiesManager.get(DebugConstants.CONFIG_DEBUG_LEVEL));

        // Note : we don't need to close the writer we will keep it
        // in the Writer cache in case merge mode changes again.
        debugFile = null;
    }

    /**
     * Returns <code>true</code> if debug is enabled.
     *
     * @return <code>true</code> if debug is enabled.
     */
    public boolean messageEnabled() {
        return this.debugLevel.compareLevel(DebugLevel.MESSAGE) >= 0;
    }

    /**
     * Returns <code>true</code> if debug warning is enabled.
     *
     * @return <code>true</code> if debug warning is enabled.
     */
    public boolean warningEnabled() {
        return this.debugLevel.compareLevel(DebugLevel.WARNING) >= 0;
    }

    /**
     * Returns <code>true</code> if debug error is enabled.
     *
     * @return <code>true</code> if debug error is enabled.
     */
    public boolean errorEnabled() {
        return this.debugLevel.compareLevel(DebugLevel.ERROR) >= 0;
    }

    /**
     * Writes debug message.
     *
     * @param message Debug message.
     * @param th      Throwable object along with the message.
     */
    public void message(String message, Throwable th) {
        if (messageEnabled()) {
            record(message, th);
        }
    }

    /**
     * Writes debug warning message.
     *
     * @param message Debug message.
     * @param th      Throwable object along with the warning message.
     */
    public void warning(String message, Throwable th) {
        if (warningEnabled()) {
            record("WARNING: " + message, th);
        }
    }

    /**
     * Writes debug error message.
     *
     * @param message Debug message.
     * @param th      Throwable object along with the error message.
     */
    public void error(String message, Throwable th) {
        if (errorEnabled()) {
            record("ERROR: " + message, th);
        }
    }

    private void record(String msg, Throwable th) {

        StringBuilder prefix = new StringBuilder();
        String dateFormatted;
        synchronized (dateFormat) {
            dateFormatted = this.dateFormat.format(newDate());
        }
        prefix.append(debugName)
                .append(":").append(dateFormatted)
                .append(": ").append(Thread.currentThread().toString())
                .append(": TransactionId[").append(getAuditTransactionId()).append("]");

        writeIt(prefix.toString(), msg, th);
    }

    /**
     * Determine the current audit transaction id. If we are not running in server mode then return nothing to avoid
     * breaking Fedlet and other client applications that do not include the audit classes.
     *
     * @return the current audit transaction id, or the string "unknown" if not present.
     */
    private String getAuditTransactionId() {
        if (SERVER_MODE) {
            return AuditRequestContext.getTransactionIdValue();
        }
        return "unknown";
    }

    /**
     * Write message on Debug file. If it failed, it try to print it on the Sdtout Debug file.
     * If both failed, it prints in System.out
     *
     * @param prefix Message prefix
     * @param msg    Message to be recorded.
     * @param th     the optional <code>java.lang.Throwable</code> which if
     *               present will be used to record the stack trace.
     */
    private void writeIt(String prefix, String msg, Throwable th) {

        //we create the debug file only if we need to write on it
        if (debugFile == null) {
            String debugFileName = resolveDebugFileName();
            debugFile = debugFileProvider.getInstance(debugFileName);
        }

        try {
            if (this.debugLevel == DebugLevel.ON) {
                stdoutDebugFile.writeIt(prefix, msg, th);
            } else {

                try {
                    this.debugFile.writeIt(prefix, msg, th);
                } catch (IOException e) {
                    /*
                     * In order to have less logs for this kind of issue. It's waiting an interval of time before
                     * printing this error again.
                     */
                    if (lastDirectoryIssue + DIR_ISSUE_ERROR_INTERVAL_IN_MS < currentTimeMillis()) {
                        lastDirectoryIssue = currentTimeMillis();
                        stdoutDebugFile.writeIt(prefix, "Debug file can't be written : " + e.getMessage(), null);
                    }
                    stdoutDebugFile.writeIt(prefix, msg, th);

                }
            }
        } catch (IOException ioex) {
            StdDebugFile.printError(DebugImpl.class.getSimpleName(), ioex.getMessage(), ioex);
        }
    }

    /**
     * Return the Debug file name that should be used for this debug name
     *
     * @return the debug file name to use
     */
    private String resolveDebugFileName() {

        if (mergeAllMode) {
            return DebugConstants.CONFIG_DEBUG_MERGEALL_FILE;
        } else {
            // Find the bucket this debug belongs to
            String nm = INSTANCE_NAMES.get(debugName);
            if (nm != null) {
                return nm;
            } else {
                // Default to debugName if no mapping is found
                return debugName;
            }
        }
    }

    /**
     * initialize the properties
     * It will reset the current properties for every Debug instance
     */
    public static synchronized void initProperties() {

        // Load properties : debugmap.properties
        InputStream is = null;
        try {
            is = DebugImpl.class.getResourceAsStream(DebugConstants.CONFIG_DEBUG_FILEMAP);
            if (is == null && SystemPropertiesManager.get(DebugConstants.CONFIG_DEBUG_FILEMAP_VARIABLE) != null) {
                is = DebugImpl.class.getResourceAsStream(SystemPropertiesManager.get(DebugConstants
                        .CONFIG_DEBUG_FILEMAP_VARIABLE));
            }
            Properties fileNames = new Properties();
            fileNames.load(is);
            INSTANCE_NAMES.clear();
            for (Map.Entry<Object, Object> entry : fileNames.entrySet()) {
                INSTANCE_NAMES.put((String) entry.getKey(), (String) entry.getValue());
            }
        } catch (Exception ex) {
            StdDebugFile.printError(DebugImpl.class.getSimpleName(), "Can't read debug files map. '. Please check the" +
                    " configuration file '" + DebugConstants.CONFIG_DEBUG_FILEMAP + "'.", ex);
        } finally {
            IOUtils.closeIfNotNull(is);
        }
    }

    private static final class WildcardComparator implements Comparator<String> {
        @Override
        public int compare(String s1, String s2) {
            return wildCardMatch(s1, s2) ? 0 : stringCompare(s1, s2);
        }

        private int stringCompare(String s1, String s2) {
            if (s1 == null || s2 == null) {
                return s1 == null ? (s2 == null ? 0 : -1) : 1;
            }
            return s1.compareTo(s2);
        }

        private boolean wildCardMatch(String value, String pattern) {
            return isNotEmpty(value) && isNotEmpty(pattern)
                    && pattern.endsWith("*")
                    && value.startsWith(pattern.substring(0, pattern.length() - 1));
        }
    }
}
