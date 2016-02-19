/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ProcessExecutor.java,v 1.1 2008/11/22 02:19:58 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.core.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

public class ProcessExecutor {
    private static final String DEFAULT_STDOUT_FILE = "stdout";
    private static final String DEFAULT_STDERR_FILE = "stderr";
    private String stdoutFileName = DEFAULT_STDOUT_FILE;
    private String stderrFileName = DEFAULT_STDERR_FILE;
    private boolean canDeleteOutputFile = true;
    private boolean canDeleteErrorFile = true;
    private static final long DEFAULT_TIMEOUT_SEC = 600;
    private static final String NEWLINE = System.getProperty("line.separator");
    private long mTimeoutMilliseconds = 0;
    private String[] mCmdStrings = null;
    private File mOutFile = null;
    private File mErrFile = null;
    private String[] mInputLines = null;
    private int mExitValue = -1;
    public static final long kSleepTime = 500;
    
    /**
     * Creates new ProcessExecutor
     */
    public ProcessExecutor(String[] cmd) {
        this(cmd, DEFAULT_TIMEOUT_SEC, null);
    }
    
    /**
     * Creates new ProcessExecutor
     */
    public ProcessExecutor(String[] cmd, String[] inputLines) {
        this(cmd, DEFAULT_TIMEOUT_SEC, inputLines);
    }
    
    /**
     * Creates new ProcessExecutor
     */
    public ProcessExecutor(String[] cmd, long timeoutSeconds) {
        this(cmd, timeoutSeconds, null);
    }
    
    /**
     * Creates a new <code> ProcessExecutor </code> that executes the given
     * command.
     *
     * @param cmd String that has command name and its command line arguments
     * @param timeoutSeconds long integer timeout to be applied in seconds.
     *        After this time if the process to execute does not end, it will
     *        be destroyed.
     */
    public ProcessExecutor(
        String[] cmd,
        long timeoutSeconds,
        String[] inputLines
    ) {
        mCmdStrings = cmd;
        mInputLines = inputLines;
        char fwdSlashChar = '/';
        char backSlashChar = '\\';
        for(int i=0; i<mCmdStrings.length; i++) {
            if (!isWindows()) {
                mCmdStrings[i] =
                    mCmdStrings[i].replace(backSlashChar, fwdSlashChar);
            } else {
                mCmdStrings[i] =
                    mCmdStrings[i].replace(fwdSlashChar, backSlashChar);
            }
        }
        mTimeoutMilliseconds = (long)timeoutSeconds * 1000;
    }
    
    
    private boolean getDeleteOutFilePolicy(){
        return canDeleteOutputFile;
    }
    
    private boolean getDeleteErrFilePolicy(){
        return canDeleteErrorFile;
    }
    
    private File createRedirectFile(
        String file,
        String defaultFile,
        boolean deletePolicy
    ) {
        File retval = null;
        try {
            if (file.equalsIgnoreCase(defaultFile)){
                retval = File.createTempFile(file , null);
            } else{
                retval = new File(file);
                retval.createNewFile();
            }
            retval.deleteOnExit();
        } catch(IOException ioex) {
            retval = null;
            deleteTempFiles();
        }
        return retval;
    }
    
    private void init()  {
        try {
            mOutFile = createRedirectFile(
                stdoutFileName, DEFAULT_STDOUT_FILE, getDeleteOutFilePolicy());
            mErrFile = createRedirectFile(
                stderrFileName, DEFAULT_STDERR_FILE, getDeleteErrFilePolicy());
        } catch (IllegalArgumentException iae){
            deleteTempFiles();
            
        }
    }
    
    private boolean isWindows(){
        String oSName =  System.getProperty("os.name");
        String oSNameLower = oSName.toLowerCase();
        int indx = oSNameLower.indexOf("windows");
        if (indx == -1) {
            return false;
        } else {
            return true;
        }
    }
    
    private void deleteTempFiles(){
        if (mOutFile != null && canDeleteOutputFile) mOutFile.delete();
        if (mErrFile != null && canDeleteErrorFile) mErrFile.delete();
    }
    
    public void execute()  {
        execute(false);
    }
    
    public String[] executeAndGetError(
        boolean bReturnOutputLines,
        boolean exceptionOnNonZeroExitVal
    ) {
        init();
        String[] cmdOp = null;
        try {
            Process subProcess = Runtime.getRuntime().exec(mCmdStrings);
            InputStream inputStream = null;
            if(mInputLines != null)
                addInputLinesToProcessInput(subProcess);
            if (!bReturnOutputLines) {
                redirectProcessOutput(subProcess);
            } else {
                inputStream = subProcess.getErrorStream();
            } 
            redirectProcessOutput(subProcess);
            long timeBefore = System.currentTimeMillis();
            boolean timeoutReached = false;
            boolean isSubProcessFinished = false;
            boolean shouldBeDone = false;
            
            if (bReturnOutputLines) {
                cmdOp = getInputStrings(inputStream);
            }
            while (! shouldBeDone) {
                try {
                    mExitValue = subProcess.exitValue();
                    isSubProcessFinished = true;
                    break;
                } catch(IllegalThreadStateException itse) {
                    isSubProcessFinished = false;
                    sleep(kSleepTime);
                    //ignore exception
                }
                long timeAfter = System.currentTimeMillis();
                timeoutReached =
                    (timeAfter - timeBefore) >= mTimeoutMilliseconds;
                shouldBeDone = timeoutReached || isSubProcessFinished;
                
            }
            if (!isSubProcessFinished) {
                subProcess.destroy();
                mExitValue = -255;
            } else {
                mExitValue = subProcess.exitValue();
                if (mExitValue != 0) {
                    /* read the error message from error file */
                    final String errorMessage = getFileBuffer(mErrFile);
                    if (!exceptionOnNonZeroExitVal) {
                        return cmdOp;
                    }
                }
                if (bReturnOutputLines) {
                    //inputStream = getRedirectedProcessOutputHandle();
                    return cmdOp;
                } else {
                    return null;
                }
            }
        } catch(SecurityException se) {
            return null;
        } catch(IOException ioe) {
            return null;
        } finally {
            deleteTempFiles();
        }
        return cmdOp;
    }
    
    /**
     * Executes the command. 
     *
     * @return  An array containing the output of the command
     */
    public String[] execute(
        boolean bReturnOutputLines,
        boolean exceptionOnNonZeroExitVal
    ) {
        init();
        try {
            Process subProcess = Runtime.getRuntime().exec(mCmdStrings);
            InputStream inputStream = null;
            if (mInputLines != null)
                addInputLinesToProcessInput(subProcess);
            if (!bReturnOutputLines) {
                redirectProcessOutput(subProcess);
            } else {
                inputStream = subProcess.getInputStream();
            }
            redirectProcessError(subProcess);
            long timeBefore = System.currentTimeMillis();
            boolean timeoutReached = false;
            boolean isSubProcessFinished = false;
            boolean shouldBeDone = false;
            String[] cmdOp = null;
            if (bReturnOutputLines) {
                cmdOp = getInputStrings(inputStream);
            }
            while (! shouldBeDone) {
                try {
                    mExitValue = subProcess.exitValue();
                    isSubProcessFinished = true;
                    break;
                } catch(IllegalThreadStateException itse) {
                    isSubProcessFinished = false;
                    sleep(kSleepTime);
                    //ignore exception
                }
                long timeAfter = System.currentTimeMillis();
                timeoutReached = 
                    (timeAfter - timeBefore) >= mTimeoutMilliseconds;
                shouldBeDone = timeoutReached || isSubProcessFinished;
            }
            if (!isSubProcessFinished) {
                subProcess.destroy();
                mExitValue = -255;
                return null;
            } else {
                mExitValue = subProcess.exitValue();
                if (mExitValue != 0) {
                    /* read the error message from error file */
                    final String errorMessage = getFileBuffer(mErrFile);
                    if(!exceptionOnNonZeroExitVal){
                        return cmdOp;
                    }
                }
                if (bReturnOutputLines) {
                    return cmdOp;
                } else {
                    return null;
                }
            }
        } catch(SecurityException se) {
            return null;
        } catch(IOException ioe) {
            return null;
        } finally {
            deleteTempFiles();
        }
    }
    
    public String[] execute(boolean bReturnOutputLines) {
        return execute(bReturnOutputLines,true);
    }
    
    private void addInputLinesToProcessInput(Process subProcess)  {
        if (mInputLines == null) {
            return;
        }
        PrintWriter out = null;
        try {
            out = new PrintWriter(
                new BufferedWriter(
                new OutputStreamWriter(subProcess.getOutputStream())));
            for (int i=0; i<mInputLines.length; i++) {
                out.println(mInputLines[i]);
            }
            out.flush();
        } catch (Exception e) {
            //do nothing
        } finally {
            try {
                out.close();
            } catch (Throwable t) {
            }
        }
    }
    
    private String[] getInputStrings(InputStream inputStream) {
        if (inputStream==null) {
            return null;
        }
        BufferedReader in = null;
        ArrayList list = new ArrayList();
        String str;
        try {
            in = new BufferedReader( new InputStreamReader(inputStream));
            while ((str=in.readLine())!=null) {
                list.add(str);
            }
            if (list.size() < 1) {
                return null;
            }
            return (String[])list.toArray(new String[list.size()]);
        } catch (Exception e) {
            return null;
        } finally {
            try {
                in.close();
            } catch (Throwable t) {
            }
        }
    }
    
    private void redirectProcessOutput(Process subProcess) {
        try {
            InputStream in = subProcess.getInputStream();
            OutputStream out = new FileOutputStream(mOutFile);
            new FlusherThread(in, out).start();
        } catch (Exception e) {
            //ignore exception
        }
    }
    
    private void redirectProcessError(Process subProcess)  {
        try {
            InputStream	in = subProcess.getErrorStream();
            OutputStream out = new FileOutputStream(mErrFile);
            new FlusherThread(in, out).start();
        } catch (Exception e) {
            //ignore exception
        }
    }
    
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch(InterruptedException ie) {
            //ignore exception
        }
    }
    
    /**
     * Returns the contents of a file as a String. If the file is empty, 
     * an empty string is returned.
     *
     * @param file the file to read
     * @return String containing the file contents
     */
    private String getFileBuffer(File file) {
        final StringBuffer sb = new StringBuffer();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append(NEWLINE);
            }
        } catch(Exception e) {
            //ignore exception
        } finally {
            try {
                reader.close();
            } catch(Exception e) {}
        }
        return ( sb.toString() );
    }
}

class FlusherThread extends Thread {
    BufferedInputStream mInStream = null;
    BufferedOutputStream mOutStream = null;
    
    public static final int kSize = 1024;
    
    FlusherThread(InputStream in, OutputStream out) {
        mInStream = new BufferedInputStream(in);
        mOutStream = new BufferedOutputStream(out);
    }
    
    public void run() {
        try {
            int ch;
            while ((ch = mInStream.read()) != -1) {
                mOutStream.write(ch);
            }
        } catch(IOException ioe) {
            //ignore exception
        } finally {
            try {
                mOutStream.flush();
                mOutStream.close();
            } catch(IOException e) {
                //ignore exception
            }
        }
    }
}

