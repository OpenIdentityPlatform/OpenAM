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
 * $Id: HomeDirValidator.java,v 1.2 2008/11/28 12:36:22 saueree Exp $
 */

package com.sun.identity.agents.tools.tomcat.v6;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.configurator.ValidationResult;
import com.sun.identity.install.tools.configurator.ValidationResultStatus;
import com.sun.identity.install.tools.configurator.ValidatorBase;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.OSChecker;

import java.io.File;

import java.util.HashMap;
import java.util.Map;


public class HomeDirValidator extends ValidatorBase implements IConstants,
    IConfigKeys {
    /*
     * Localized constants
     */
    private static String LOC_VA_MSG_TOMCAT_VAL_CATALINA_HOME
    = "VA_MSG_TOMCAT_VAL_CATALINA_HOME";
    private static String LOC_VA_WRN_TOMCAT_IN_VAL_CATALINA_HOME
    = "VA_WRN_TOMCAT_IN_VAL_CATALINA_HOME";

    /** Hashmap of Validator names and integers */
    Map validMap = new HashMap();

    public HomeDirValidator() throws InstallException {
        super();
    }

    /**
     * Method isHomeDirValid
     *
     *
     * @param dir
     * @param props
     * @param IStateAccess
     *
     * @return ValidationResult
     *
     */
    public ValidationResult isHomeDirValid(
        String catalinaHomeDir,
        Map props,
        IStateAccess state) {
        ValidationResultStatus validRes = ValidationResultStatus.STATUS_FAILED;
        LocalizedMessage returnMessage = null;

        if ((catalinaHomeDir != null)
                && (catalinaHomeDir.trim()
                                       .length() >= 0)) {
            // The config dir has been normalized to have "/" only
            StringBuffer sb = new StringBuffer(catalinaHomeDir);
            sb.append(STR_FORWARD_SLASH);
            sb.append(STR_TOMCAT_SERVER_LIB);
            sb.append(STR_FORWARD_SLASH);
            sb.append(STR_CATALINA_JAR);

            String catalinaJarFile = sb.toString();

            if (FileUtils.isFileValid(catalinaJarFile)
                    && canUpdateServerClassPath(catalinaHomeDir)) {
                returnMessage = LocalizedMessage.get(
                        LOC_VA_MSG_TOMCAT_VAL_CATALINA_HOME,
                        STR_TOMCAT_GROUP,
                        new Object[] { catalinaHomeDir });

                validRes = ValidationResultStatus.STATUS_SUCCESS;

                state.put(
                    STR_CATALINA_JAR_PATH,
                    catalinaJarFile);

                Debug.log(
                    "HomeDirValidator: Added catalina config to state --> "
                    + catalinaHomeDir + " " + catalinaJarFile);
            }
        }

        if (validRes.getIntValue() == ValidationResultStatus.INT_STATUS_FAILED)
        {
            returnMessage = LocalizedMessage.get(
                    LOC_VA_WRN_TOMCAT_IN_VAL_CATALINA_HOME,
                    STR_TOMCAT_GROUP,
                    new Object[] { catalinaHomeDir });
        }

        Debug.log(
            "HomeDirValidator : Is $CATALINA_HOME directory "
            + catalinaHomeDir + " valid ? " + validRes.isSuccessful());

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
                "VALID_CATALINA_HOME_DIR",
                this.getClass().getMethod(
                    "isHomeDirValid",
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

    protected boolean canUpdateServerClassPath(String catalinaHomeDir) {
        boolean result = false;
        String temp = catalinaHomeDir + STR_FORWARD_SLASH
            + STR_BIN_DIRECTORY + STR_FORWARD_SLASH;

        String setClassPathFile;

        if (OSChecker.isWindows()) {
            setClassPathFile = temp + STR_SET_CLASSPATH_FILE_WINDOWS;
        } else {
            setClassPathFile = temp + STR_SET_CLASSPATH_FILE_UNIX;
        }

        File file = new File(setClassPathFile);

        if (file.exists() && file.canWrite() && file.canRead()
                && file.getParentFile()
                           .canWrite()) {
            result = true;
        }

        return result;
    }
}
