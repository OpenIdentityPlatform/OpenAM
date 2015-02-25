/* The contents of this file are subject to the terms
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
 * $Id: CLICommand.java,v 1.3 2008/06/04 21:09:28 cmwesley Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.common.cli;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * <code>CLICommand</code> executes a command specified by the 
 * <code>ArrayList</code> variable <code>argList</code>.  The run method uses a 
 * <code>ProcessBuilder</code> to execute a command.  The resulting output and 
 * error from the command are captured in <code>outputBuffer</code> and 
 * <code>errorBuffer</code> respectively.  The <code>exitStatus</code> is set to
 * the exit status returned by the process or the value of
 * <code>UNFINISHED_PROCESS_STATUS</code> if the process fails to exit.
 */
public class CLICommand extends Thread {
    private ArrayList argList;
    private File workingDir;
    private long timeout;
    private StringBuffer outputBuffer;
    private StringBuffer errorBuffer;
    private ProcessBuilder cliProcessBuilder;
    public static final int UNFINISHED_PROCESS_STATUS = -1;
    private int exitStatus = UNFINISHED_PROCESS_STATUS;
    private float duration = 0;
    
    
    /**
     * Creates a new instance of CLICommand
     */
    public CLICommand(File dir, ArrayList args, long time) {
        workingDir = dir;
        argList = args;
        cliProcessBuilder = new ProcessBuilder(argList);
        cliProcessBuilder.directory(workingDir);
        timeout = time;
    }
    
    /**
     * Execute the command specified by the list of arguments argList
     */
    public void run() {
        Process cliProcess;
        StreamRedirector outputRedirector = new StreamRedirector(); 
        StreamRedirector errorRedirector = new StreamRedirector();
        Date startTime = new Date();
               
        synchronized(cliProcessBuilder) {
            try {
                Map<String,String> env = cliProcessBuilder.environment(); 
                env.put("PATH", workingDir.getPath() + 
                        System.getProperty("path.separator") + env.get("PATH"));
                env.put("JAVA_HOME", System.getProperty("java.home"));
                cliProcess = cliProcessBuilder.start();
                startTime = new Date();
                outputRedirector.setInputStream(cliProcess.getInputStream());
                errorRedirector.setInputStream(cliProcess.getErrorStream());
                outputRedirector.start();
                errorRedirector.start();
                setExitStatus(cliProcess.waitFor());
            } catch (InterruptedException ie) {
                setExitStatus(UNFINISHED_PROCESS_STATUS);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Date endTime = new Date();
                setDuration(endTime.getTime() - startTime.getTime());
                
                outputBuffer = outputRedirector.getBuffer();
                errorBuffer = errorRedirector.getBuffer();
            }
        }
    }
    
    /** 
     * Set the exit status for the executed command
     * @param status - the value used to exitStatus 
     */
    private void setExitStatus(int status) { exitStatus = status; }
    
    /** 
     * Get the exit status of the executed command
     * @return the current exit status of the command or the value 
     * <code>UNFINISHED_PROCESS_STATUS</code> if the process has not 
     * completed its execution or is deemed to have timed out by the caller
     */
    public int getExitStatus() { return exitStatus; }
   
    /** 
     * Get the output buffer of this command
     * @return the command's output in a StringBuffer
     */
    public StringBuffer getOutput() { return outputBuffer; }
    
    /** 
     * Get the error buffer of this command
     * @return the command's error in a StringBuffer
     */
    public StringBuffer getError() { return errorBuffer; }
    
    /**
     * Set the execution duration of this command in seconds as a float
     * @param time - number of milliseconds that this command was executed
     */
    private void setDuration(long t) { duration = t / 1000; }
    
    /**
     * Get the execution duration of this command in seconds as a float
     * @return the number of seconds that the comand was being executed
     */
    public float getDuration() { return duration; }
    
    /**
     * Get the command arguments
     */
    public ArrayList getArgList() { return argList; }
}
