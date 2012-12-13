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
 * $Id: ExecuteCommand.java,v 1.2 2008/06/25 05:51:29 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.util;
import com.sun.identity.install.tools.util.Debug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *  This class executes a given script/commnand using Runtime.exec().
 */
public class ExecuteCommand {

   
    public static String JAVA_HOME = "java.home";
    public static String CLASSPATH = "-classpath";
    private static final String FILE_SEP = 
        System.getProperty("file.separator");

    /**
     * Execute java command 
     */
    public static String executeJavaCommand(String jarName,
        String className, String arguments) throws Exception {

        StringBuffer output = new StringBuffer();
        String javaHome = System.getProperty(JAVA_HOME); 
        if (javaHome == null)
            throw new Exception("JAVA_HOME is not set." +
            " Make sure Java is available on the m/c to " + 
            "proceed further.");
        String javaExe = System.getProperty(JAVA_HOME) 
                + FILE_SEP + "bin" + FILE_SEP + "java";
        Debug.log(
                "ExecuteCommand.executeJavaCommand(): JAVA_HOME = "
                + javaExe + " jarFile = " + jarName);

        String[] commandArray = {
                    javaExe, CLASSPATH, jarName,
                    className, arguments };
        executeCommand(
                commandArray,
                null,
                output);

        return (output != null) ? output.toString() : null;
    }

    /**
     * Method executeCommand
     *
     *
     * @param commandArray
     * @param environment
     * @param resultBuffer
     *
     * @return
     *
     */
    public static int executeCommand(
        String[] commandArray,
        String[] environment,
        StringBuffer resultBuffer) {
        int status;
        BufferedReader reader = null;

        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(
                    commandArray,
                    environment);
            String line;

            reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            if (resultBuffer != null) {
                resultBuffer.setLength(0);

                for (line = reader.readLine(); line != null;
                        line = reader.readLine()) {
                    resultBuffer.append(line)
                                .append('\n');
                }
            } else {
                line = reader.readLine();

                while (line != null) {
                    line = reader.readLine();
                }
            }

            status = process.waitFor();
        } catch (InterruptedException exc) {
            throw new RuntimeException(
                "ExecuteCommand.executeCommand(...) error waiting for "
                + commandArray[0]);
        } catch (IOException exc) {
            throw new RuntimeException(
                "ExecuteCommand.executeCommand(...) : "
                + "error executing " + commandArray[0]);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException exc) {
                    Debug.log(
                        "ExecuteCommand.executeCommand(...) : "
                        + "Error executing java runtime command",
                        exc);
                }
            }
        }

        return status;
    }

}
