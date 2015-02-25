/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: TomcatVersionValidator.java,v 1.2 2008/11/28 12:36:22 saueree Exp $
 *
 * Portions Copyrighted 2014-2015 ForgeRock AS.
 */

package com.sun.identity.agents.tools.tomcat.v6;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.configurator.ValidationResult;
import com.sun.identity.install.tools.configurator.ValidationResultStatus;
import com.sun.identity.install.tools.configurator.ValidatorBase;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.HashMap;
import java.util.Map;


public class TomcatVersionValidator extends ValidatorBase
    implements IConstants, IConfigKeys {
    /*
     * Localized constants
     */
    private static String LOC_VA_MSG_TOMCAT_VAL_TOMCAT_VERSION =
    	"VA_MSG_TOMCAT_VAL_TOMCAT_VERSION";
    private static String LOC_VA_WRN_TOMCAT_IN_VAL_TOMCAT_VERSION =
    	"VA_WRN_TOMCAT_IN_VAL_TOMCAT_VERSION";

    /** Hashmap of Validator names and integers */
    Map validMap = new HashMap();

    public TomcatVersionValidator() throws InstallException {
        super();
    }

    /**
     * Method isTomcatVersionValid
     *
     *
     * @param catalinaHomeDir
     * @param props
     * @param state
     *
     * @return ValidationResult
     *
     */
    public ValidationResult isTomcatVersionValid(
        String catalinaHomeDir,
        Map props,
        IStateAccess state) {
        ValidationResultStatus validRes = ValidationResultStatus.STATUS_FAILED;
        LocalizedMessage returnMessage = null;

        if (isTomcatVersionValid(state)) {
            returnMessage = LocalizedMessage.get(
                    LOC_VA_MSG_TOMCAT_VAL_TOMCAT_VERSION,
                    STR_TOMCAT_GROUP,
                    new Object[] { catalinaHomeDir });

            validRes = ValidationResultStatus.STATUS_SUCCESS;
        }

        if (validRes.getIntValue() == ValidationResultStatus.INT_STATUS_FAILED)
        {
            returnMessage = LocalizedMessage.get(
                    LOC_VA_WRN_TOMCAT_IN_VAL_TOMCAT_VERSION,
                    STR_TOMCAT_GROUP,
                    new Object[] { catalinaHomeDir });
        }

        return new ValidationResult(
            validRes,
            null,
            returnMessage);
    }

    public void initializeValidatorMap() throws InstallException {
        Class[] paramObjs = { String.class, Map.class, IStateAccess.class };

        try {
            getValidatorMap()
                .put(
                "VALID_TOMCAT_VERSION",
                this.getClass().getMethod(
                    "isTomcatVersionValid",
                    paramObjs));
        } catch (NoSuchMethodException nsme) {
            Debug.log(
                "HomeDirValidator: NoSuchMethodException "
                + "thrown while loading method :",
                nsme);
            throw new InstallException(
                LocalizedMessage.get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND),
                nsme);
        } catch (SecurityException se) {
            Debug.log(
                "HomeDirValidator: SecurityException thrown "
                + "while loading method :",
                se);
            throw new InstallException(
                LocalizedMessage.get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND),
                se);
        } catch (Exception ex) {
            Debug.log(
                "HomeDirValidator: Exception thrown while "
                + "loading method :",
                ex);
            throw new InstallException(
                LocalizedMessage.get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND),
                ex);
        }
    }

    private boolean isTomcatVersionValid(IStateAccess stateAccess) {

        StringBuffer output = new StringBuffer();

        try {
            String catalinaJarPath = (String) stateAccess.get(STR_CATALINA_JAR_PATH);

            String JAVA_EXE = System.getProperty("java.home") + "/bin/java";

            Debug.log("TomcatVersionValidator.getTomcatVersion(): JAVA_HOME = " + JAVA_EXE +
                    " jarFile = " + catalinaJarPath);

            String[] commandArray = {JAVA_EXE, STR_CLASSPATH, catalinaJarPath, STR_TOMCAT_VERSION_CLASS};

            executeCommand(commandArray, null, output);
        } catch (Exception ex) {
            Debug.log("TomcatVersionValidator.getTomcatVersion() threw exception", ex);
        }

        String versionString = null;
        if (output.length() != 0) {
            versionString = getTomcatVersion(output.toString());
        }

        // A non-null value means a supported version was found
        if (versionString != null) {
            stateAccess.put(STR_TOMCAT_VERSION, versionString);
            return true;
        } else {
            return false;
        }
    }

    /**
     * For the given ServerInfo output from org.apache.catalina.util.ServerInfo, return true or false depending on if
     * this version of Tomcat is supported.
     * @param serverInfo The out from the ServerInfo command.
     * @return the version as a String from constants TOMCAT_VER_60, TOMCAT_VER_70, TOMCAT_VER_80
     * or null if not a valid version
     */
    public static String getTomcatVersion(String serverInfo) {

        String result = null;

        if (serverInfo != null && !serverInfo.isEmpty()) {
            Debug.log("TomcatVersionValidator.getTomcatVersion() - validating ServerInfo:" + serverInfo);

            // Parse the output looking for the Server number string, for example:
            // ﻿Server number:  6.0.24.0
            // Server number:  7.0.39.0
            int currentIndex = serverInfo.indexOf(STR_APACHE_TOMCAT_SERVER_NUMBER);
            if (currentIndex == -1) {
                return null;
            }
            currentIndex += STR_APACHE_TOMCAT_SERVER_NUMBER.length();
            int endIndex = serverInfo.indexOf('\n', currentIndex);
            // If we don't find this character, just take the remainder of the string, we only care about the start
            if (endIndex == -1) {
                endIndex = serverInfo.length() - 1;
            }
            // Grab the version number, trimming any spaces in front/end to simplify the matching.
            String versionLine = serverInfo.substring(currentIndex, endIndex).trim();

            // Only versions 6.0.x, 7.0.x and 8.0.x are currently valid.
            if (versionLine.startsWith("6.0")) {
                result = TOMCAT_VER_60;
            } else if (versionLine.startsWith("7.0")) {
                result = TOMCAT_VER_70;
            } else if (versionLine.startsWith("8.0")) {
                result = TOMCAT_VER_80;
            }

            Debug.log("TomcatVersionValidator.getTomcatVersion() - version:" + versionLine +
                    " found version: " + result);
        }

        return result;
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
    private int executeCommand(
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
                "TomcatVersionValidator.executeCommand(...) error waiting for "
                + commandArray[0]);
        } catch (IOException exc) {
            throw new RuntimeException(
                "TomcatVersionValidator.executeCommand(...) : "
                + "error executing " + commandArray[0]);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException exc) {
                    Debug.log(
                        "TomcatVersionValidator.executeCommand(...) : "
                        + "Error executing java runtime command",
                        exc);
                }
            }
        }

        return status;
    }
}
