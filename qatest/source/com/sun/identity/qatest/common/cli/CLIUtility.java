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
 * $Id: CLIUtility.java,v 1.10 2009/02/13 15:36:56 cmwesley Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.common.cli;

import com.sun.identity.qatest.common.TestCommon;
import java.io.File;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;

/**
 * <code>CLIUtility</code> is a base class for classes that contain methods
 * to execute CLI commands.
 */
public class CLIUtility extends TestCommon {
    
    private static ResourceBundle rbCLI;
    private File workingDir;
    private CLICommand command;
    private Vector<String> errorVector;
    private Vector<String> outputVector;
    private int INITIAL_ARG_LIST_SIZE = 6;
    protected static String timeout;
    protected static String localeValue;
    protected static String cliPath; 
    protected static String passwdFile;
    protected ArrayList<String> argList;

    static {
        try {
            rbCLI = ResourceBundle.getBundle("cli" + fileseparator + "cliTest");
            timeout = rbCLI.getString("command-timeout"); 
            localeValue = rbCLI.getString("locale");
            cliPath = rbCLI.getString("cli-path");
            passwdFile = System.getProperty("user.dir") + 
                    System.getProperty("file.separator") + serverName +
                    System.getProperty("file.separator") + "built" + 
                    System.getProperty("file.separator") + "classes" + 
                    System.getProperty("file.separator") + "cli.pass";
        } catch (Exception e) {
            e.printStackTrace();
        }
    } 
       
    /** 
     * Creates a new instance of CLIUtility 
     * @param path - the absolute path of the command that will be executed
     * @throws SecurityException if the SecurityManager denies read access to
     * the File object created from the path input parameter
     */
    public CLIUtility(String path) throws SecurityException {
        super("CLIUtility");
        try {       
            if (path != null) {
                File utilityFile = new File(path);
                if (utilityFile.exists() && utilityFile.isFile()) {
                    if (utilityFile.isAbsolute()) {
                        workingDir = utilityFile.getParentFile();
                    } else {
                        workingDir = 
                                utilityFile.getAbsoluteFile().getParentFile();
                    }
                }
            } 
            argList = new ArrayList(INITIAL_ARG_LIST_SIZE);
            addArgument(path);
            for (int i=1; i < INITIAL_ARG_LIST_SIZE; i++) {
                argList.add("");
            }            
        } catch (SecurityException se) {
            se.printStackTrace();
            throw se;
        }  
    }
    
    
    /**
     * Tokenize a StringBuffer into token strings using a delimiter string
     * @param buffer - a StringBuffer that will be tokenized
     * @param delimiter - a String containing the characters to separate tokens
     * @returns a Vector containing the tokens found in buffer
     */
    private Vector tokenizeBuffer(StringBuffer buffer, String delimiter) {
        StringTokenizer tokenizer = new StringTokenizer(buffer.toString(),
                delimiter);
        Vector tokenVector = new Vector(tokenizer.countTokens());
        
        while (tokenizer.hasMoreTokens()) {
            tokenVector.add(tokenizer.nextToken());
        }
        
        return (tokenVector);
    } 
    
    /**
     * Tokenize the <code>CLICommand</code> output buffer
     * @returns a Vector where each element corresponds to a line in the output
     */
    protected Vector tokenizeOutputBuffer() {
        if (outputVector == null) {
            outputVector = tokenizeBuffer(command.getOutput(), newline);
        }
        return (outputVector);
    }
    
    /**
     * Tokenize the <code>CLICommand</code> error buffer
     * @return a Vector where each element corresponds to a line in the error
     */
    protected Vector tokenizeErrorBuffer() {
        if (errorVector == null) {
            errorVector = tokenizeBuffer(command.getError(), newline);
        }
        return (errorVector);
    }
    
    /**
     * Search for a string in the output of the executed <code>CLICommand</code>
     * @param searchString - the string to search for in the output
     * @return true if the string is found in the output and false if the string
     * is not found in the output
     */
    public boolean findStringInOutput(String searchString) {
        return (tokenizeOutputBuffer().contains(searchString));
    }
    
    /**
     * Search for a string in the error of the executed <code>CLICommand</code>
     * @param searchString - the string to search for in the error
     * @return true if the string is found in the error and false if the string
     * is not found in the error
     */
    public boolean findStringInError (String searchString) {
        return (tokenizeErrorBuffer().contains(searchString));
    }
    
    /**
     * Add an argument to the end of the argument list
     * @param arg - a String containing the argument to be stored
     */
    protected void addArgument(String arg) {
        argList.add(arg);
    }
    
    /**
     * Set an argument in a particular position in the list
     * @param index - the position in the list which should be set
     * @param arg - a String containing the argument to be stored
     */
    protected void setArgument(int index, String arg) {
        argList.set(index, arg);
    }
    
    /**
     * Clear the arguments in the argument list from the end to the element
     * before the input index
     * @param indexToPreserve - the index of the first argument that should 
     * not be deleted
     */
    public void clearArguments(int indexToPreserve) {
        int originalSize = argList.size();
        for (int i = originalSize - 1; i > indexToPreserve; i--) {
            argList.remove(i);
        }
    }
    
    /**
     * Execute the command specified by the list of arguments
     * @param timeout - the number of milliseconds to wait for the command
     * to complete its execution
     * @return the exit status of the executed command or the value of 
     * <code>CLICommand.UNFINISHED_PROCESS_STATUS</code> if the process does
     * not complete before the timeout elapses
     */
    protected int executeCommand(long timeout) 
    throws Exception {
        command = new CLICommand(workingDir, argList, timeout);
        try {
            synchronized(command) {
                command.start();

                while (command.isAlive()) {
                    command.wait(timeout);
                    command.interrupt();
                }
            }
        } catch (InterruptedException ie) { 
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            return(getExitStatus());
        }   
    }
    
    /**
     * Return the exit status of the <code>CLICommand</code> member variable
     * @return the exit status of the executed command
     */
    protected int getExitStatus() { return command.getExitStatus(); }
    
    /**
     * Get the <code>CLICommand</code> object
     */
    public CLICommand getCommand() { return command; }
    
    /**
     * Get the command being executed
     */
    public ArrayList getArgList() { 
        if (command != null) {
            return command.getArgList(); 
        } else {
            return null;
        }
    }
    
    /**
     * Display log messages for a <code>CLICommand</code>.
     * Log the command arguemnts, output, error, and exit status.
     * @param methodName - the name of the method from which the command
     * was executed.
     */
    public void logCommand(String methodName) { 
        if (argList != null) {
            log(Level.FINEST, "logCommand", methodName + " Command executed: " + 
                    getAllArgs());
        } else {
            log(Level.SEVERE, "logCommand", methodName + 
                    " Argument list is null");
        }
        if (command != null) {
            StringBuffer outputBuffer = command.getOutput();
            StringBuffer errorBuffer = command.getError();
            log(Level.FINEST, methodName, "Command execution time: " + 
                    command.getDuration() + " seconds");
            if (outputBuffer != null) {
                log(Level.FINEST, "logCommand", methodName + " Command output: "
                        + outputBuffer);
            }
            if (errorBuffer != null) {
                log(Level.FINEST, "logCommand", methodName + " Command error: " 
                        + errorBuffer);
            }
            log(Level.FINEST, "logCommand", methodName + 
                    " Command exit status: " + getExitStatus());
        } else {
            log(Level.SEVERE, "logCommand", methodName + " Command is null");
        }
    }
    
    /**
     * Find string(s) separated by a delimiter in the output of a
     * <code>FederationManagerCLI</code> command
     * @param searchStrings - a string containing one or more strings separated
     * by the characters in delimiter
     * @param delimiter - a string containing a delimiter between search strings
     * @return true if all search strings are found and false if one or more
     * search strings are not found
     */
    public boolean findStringsInOutput(String searchStrings, String delimiter) {
        boolean stringsFound = true;
        StringTokenizer tokenizer = new StringTokenizer(searchStrings, 
                delimiter);
        
        while (tokenizer.hasMoreTokens()) {
            String searchToken = tokenizer.nextToken();
            if (searchToken.indexOf('!') != 0) {
                log(Level.FINE, "findStringsInOutput", "Searching for string \'" + 
                        searchToken + "\' in command output.");
                if (!findStringInOutput(searchToken)) {
                    stringsFound = false;
                    log(Level.FINE, "findStringsInOutput", "Could not find string '"
                            + searchToken + "'" + " in the command output.");
                } else {
                    log(Level.FINEST, "findStringsInOutput", "Found the string '" + 
                            searchToken + "'" + " in the command output.");
                }
            } else {
                String token = searchToken.substring(1);
                log(Level.FINE, "findStringsInOutput", "Searching not to " +
                        "find \'" + token + "\' in the command output.");
                if (findStringInOutput(token)) {
                    stringsFound = false;
                    log(Level.FINE, "findStringsInOutput", "Found unexpected " +
                            "string \'" + token + "\' in the command output.");
                } else {
                    log(Level.FINEST, "findStringsInOutput", "Did not find " + 
                            "unexpected string \'" + token + "\' in the " + 
                            "command output.");
                }
            }
        }
        return stringsFound;
    }
    
    /**
     * Find string(s) separated by a delimiter in the error of a
     * <code>FederationManagerCLI</code> command
     * @param searchStrings - a string containing one or more strings separated
     * by the characters in delimiter
     * @param delimiter - a string containing a delimiter between search strings
     * @return true if all search strings are found and false if one or more
     * search strings are not found
     */
    public boolean findStringsInError(String searchStrings, String delimiter) {
        boolean stringsFound = true;
        StringTokenizer tokenizer = new StringTokenizer(searchStrings, 
                delimiter);
        
        while (tokenizer.hasMoreTokens()) {
            String searchToken = tokenizer.nextToken();
            log(Level.FINE, "findStringsInError", "Searching for string \'" + 
                    searchToken + "\' in command error.");      
            if (!findStringInError(searchToken)) {
                stringsFound = false;
                log(Level.FINE, "findStringsInError", "Could not find string '" 
                        + searchToken + "'" + " in the command error.");
            } else {
                log(Level.FINEST, "findStringsInError", "Found the string '" + 
                            searchToken + "'" + " in the command error.");
            } 
        }
        return stringsFound;
    }
    
    /**
     * Get a string containing all the command arguments
     * @return a String containing all the argument values
     */
    public String getAllArgs() {
        StringBuffer buffer = new StringBuffer(argList.get(0));
        for (int i=1; i < argList.size(); i++) {
            buffer.append(" ").append(argList.get(i));
        }
        return buffer.toString();
    }
    
    /**
     * Get the path of the CLI
     * @return a String with the CLI path
     */
    public String getCliPath() { return cliPath; }

        /**
     * Set the current working directory in which the CLI will be executed
     * dir - the File object containing the directory in which the CLI should
     *       be executed
     */
    protected void setWorkingDir(File dir) { workingDir = dir; }
}
