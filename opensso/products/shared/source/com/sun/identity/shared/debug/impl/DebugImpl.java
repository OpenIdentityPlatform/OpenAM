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

package com.sun.identity.shared.debug.impl;

import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.IDebug;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.Locale;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * Debug implementation class.
 */
public class DebugImpl implements IDebug {


    private static final String CONFIG_DEBUG_DIRECTORY =
        "com.iplanet.services.debug.directory";
    private static final String CONFIG_DEBUG_LEVEL =
        "com.iplanet.services.debug.level";
    private static final String CONFIG_DEBUG_MERGEALL =
        "com.sun.services.debug.mergeall";
    private static final String CONFIG_DEBUG_MERGEALL_FILE =
        "debug.out";
    private static final String CONFIG_DEBUG_FILEMAP =
        "/debugfiles.properties";

    private static HashMap mergedWriters  = new HashMap();

    private static Properties debugFileNames = null;

    private String debugName;

    private int debugLevel = IDebug.ON;

    private PrintWriter debugWriter = null;

    private PrintWriter stdoutWriter = new PrintWriter(System.out, true);

    private SimpleDateFormat dateFormat = new SimpleDateFormat(
        "MM/dd/yyyy hh:mm:ss:SSS a zzz");

    private String debugFilePath;

    static private boolean mergeAllMode = false;

    /**
     * Creates an instance of <code>DebugImpl</code>.
     *
     * @param debugName Name of the debug.
     */
    public DebugImpl(String debugName) {
        setName(debugName);
        setDebug(SystemPropertiesManager.get(
            CONFIG_DEBUG_LEVEL));
        String mf = SystemPropertiesManager.get(CONFIG_DEBUG_MERGEALL);

        mergeAllMode = (mf != null && mf.equals("on"));
    }

    private synchronized void initialize() {
        if(this.debugWriter == null) {
            String debugDirectory = 
                SystemPropertiesManager.get(CONFIG_DEBUG_DIRECTORY);
            boolean directoryAvailable = false;
            if (debugDirectory != null &&
                debugDirectory.trim().length() > 0) {

                File dir = new File(debugDirectory);
                if (!dir.exists()) {
                    directoryAvailable = dir.mkdirs();
                } else {
                    if (dir.isDirectory() && dir.canWrite()) {
                        directoryAvailable = true;
                    }
                }
            }

            if (!directoryAvailable) {
                ResourceBundle bundle =
                    Locale.getInstallResourceBundle("amUtilMsgs");
                System.err.println(bundle.getString(
                    "com.iplanet.services.debug.nodir"));
                return;
            }

            // Determine debug file name
            resolveDebugFile(debugDirectory);

            String prefix = debugName+":"+this.dateFormat.format(new Date())
                + ": " + Thread.currentThread().toString();

            this.debugWriter = (PrintWriter) 
                               mergedWriters.get(this.debugFilePath);
            try {
                if (this.debugWriter == null) {
                    synchronized(mergedWriters) {
                        if (this.debugWriter == null) {
                            this.debugWriter = new PrintWriter(
                                new FileWriter(this.debugFilePath, true), true);
                            mergedWriters.put(this.debugFilePath, 
                                              this.debugWriter);
                        }
                    }
                }
                writeIt(prefix,
                    "**********************************************", null);
             } catch (IOException ioex) {
                // turn debugging to STDOUT since debug file is not available
                setDebug(IDebug.ON);
                ResourceBundle bundle =
                    Locale.getInstallResourceBundle("amUtilMsgs");
                System.err.println(bundle.getString(
                    "com.iplanet.services.debug.nofile"));
                ioex.printStackTrace(System.err);
                if (this.debugWriter != null) {
                    try {
                        this.debugWriter.close();
                    } catch (Exception ex1) {
                        // No handling required
                    }
                }
            }
        }
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
        return this.debugLevel;
    }

    /**
     * Sets debug level.
     *
     * @param level Debug level.
     */
    public void setDebug(int level){
        switch(level) {
            case IDebug.OFF:
            case IDebug.ERROR:
            case IDebug.WARNING:
            case IDebug.MESSAGE:
            case IDebug.ON:
                this.debugLevel = level;
                break;
            default:
                // ignore invalid level values
                break;
         }
    }

    /**
     * Reset this instance - ths will trigger this instance to reinitialize
     * itself.
     * @param mf  merge flag : true: merge debugs into a single file.`
     */
    public void resetDebug(String mf) {
        mergeAllMode = (mf != null && mf.equals("on"));

        // Note : we dont need to close the writer we will keep it
        // in the Writer cache in case merge mode chnages again.
        debugWriter = null;
    }

    /**
     * Sets debug level.
     *
     * @param strDebugLevel Debug level.
     */
    public void setDebug(String strDebugLevel){
        int debugLevel = IDebug.ON;
        if (strDebugLevel != null && strDebugLevel.trim().length() > 0) {
            if (strDebugLevel.equals(IDebug.STR_OFF)) {
                debugLevel = IDebug.OFF;
            } else if (strDebugLevel.equals(IDebug.STR_ERROR)) {
                debugLevel = IDebug.ERROR;
            } else if (strDebugLevel.equals(IDebug.STR_WARNING)) {
                debugLevel = IDebug.WARNING;
            } else if (strDebugLevel.equals(IDebug.STR_MESSAGE)) {
                debugLevel = IDebug.MESSAGE;
            }
        }
        setDebug(debugLevel);
    }

    /**
     * Returns <code>true</code> if debug is enabled.
     *
     * @return <code>true</code> if debug is enabled.
     */
    public boolean messageEnabled(){
        return (this.debugLevel > IDebug.WARNING);
    }

    /**
     * Returns <code>true</code> if debug warning is enabled.
     *
     * @return <code>true</code> if debug warning is enabled.
     */
    public boolean warningEnabled(){
        return (this.debugLevel > IDebug.ERROR);
    }

    /**
     * Returns <code>true</code> if debug error is enabled.
     *
     * @return <code>true</code> if debug error is enabled.
     */
    public boolean errorEnabled(){
        return (this.debugLevel > IDebug.OFF);
    }

    /**
     * Writes debug message.
     *
     * @param message Debug message.
     * @param th Throwable object along with the message.
     */
    public void message(String message, Throwable th){
        if (messageEnabled()) {
            record(message, th);
        }
    }

    /**
     * Writes debug warning message.
     *
     * @param message Debug message.
     * @param th Throwable object along with the warning message.
     */
    public void warning(String message, Throwable th){
        if (warningEnabled()) {
            record("WARNING: " + message, th);
        }
    }

    /**
     * Writes debug error message.
     *
     * @param message Debug message.
     * @param th Throwable object along with the error message.
     */
    public void error(String message, Throwable th){
        if (errorEnabled()) {
            record("ERROR: " + message, th);
        }
    }

    private void record(String msg, Throwable th) {
        String prefix = debugName+":"+this.dateFormat.format(new Date())
                        + ": " + Thread.currentThread().toString();
        writeIt(prefix, msg, th);
    }

    private void writeIt(String prefix, String msg, Throwable th) {
        if (this.debugLevel == IDebug.ON) {
            writeIt(this.stdoutWriter, prefix, msg, th);
        } else {
            if(this.debugWriter == null) {
                initialize();
            }

            if(this.debugWriter != null) {
                writeIt(this.debugWriter, prefix, msg, th);
            } else {
                writeIt(this.stdoutWriter, prefix, "DebugWriter is null.", th);
                writeIt(this.stdoutWriter, prefix, msg, th);
            }
        }
    }

    private void writeIt(
        PrintWriter writer,
        String prefix,
        String msg,
        Throwable th
    ) {
        StringBuffer buf = new StringBuffer(prefix);
        buf.append('\n');
        buf.append(msg);
        if(th != null) {
            buf.append('\n');
            StringWriter stBuf = new StringWriter(300);
            PrintWriter stackStream = new PrintWriter(stBuf);
            th.printStackTrace(stackStream);
            stackStream.flush();
            buf.append(stBuf.toString());
        }
        writer.println(buf.toString());
    }

    private void setName(String debugName) {
        this.debugName = debugName;
    }

    private PrintWriter getStdoutWriter() {
        return this.stdoutWriter;
    }

    protected void finalize() throws Throwable {
        if (this.debugWriter != null) {
            try {
                this.debugWriter.flush();
                this.debugWriter.close();
            } catch (Exception ex) {
                // No handling required
            }
        }
    }
    private void resolveDebugFile(String debugDirectory) {
        if (mergeAllMode) {
            this.debugFilePath = debugDirectory + 
                                 File.separator + CONFIG_DEBUG_MERGEALL_FILE;
        } else {
            // Find the bucket this debug belongs to
            if (debugFileNames == null) {
                synchronized(mergedWriters) {
                    if (debugFileNames == null) {
                        debugFileNames = new Properties();
                        // Load properties : debugmap.properties
                        try {
                            InputStream is = 
                                this.getClass().getResourceAsStream(
                                                    CONFIG_DEBUG_FILEMAP);
                            debugFileNames.load(is);
                        } catch(Exception ex) {
                        }
                    }
                }
            }
            String nm = (String) debugFileNames.getProperty(debugName);
            if (nm != null ) {
                this.debugFilePath = debugDirectory + 
                                 File.separator + nm;
            } else {
                // Default to debugName if no mapping is found
                this.debugFilePath = debugDirectory + 
                                 File.separator + debugName;
            }
        }
    }

}
