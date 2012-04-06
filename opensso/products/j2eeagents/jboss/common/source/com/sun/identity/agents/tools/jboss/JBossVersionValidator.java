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
 * $Id: JBossVersionValidator.java,v 1.2 2009/01/25 05:57:27 naghaon Exp $
 *
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */
package com.sun.identity.agents.tools.jboss;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.configurator.ValidationResult;
import com.sun.identity.install.tools.configurator.ValidationResultStatus;
import com.sun.identity.install.tools.configurator.ValidatorBase;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.ExecuteCommand;
import com.sun.identity.install.tools.util.LocalizedMessage;
import java.util.HashMap;
import java.util.Map;

public class JBossVersionValidator extends ValidatorBase
    implements IConstants, IConfigKeys {
    /*
     * Localized constants
     */
    private static String LOC_VA_MSG_JB_VAL_VERSION = 
    	"VA_MSG_JB_VAL_VERSION";
    private static String LOC_VA_WRN_JB_IN_VAL_VERSION = 
    	"VA_WRN_JB_IN_VAL_VERSION";
   
    /** Hashmap of Validator names and integers */
    Map validMap = new HashMap();

    public JBossVersionValidator() throws InstallException {
        super();
    }

    /**
     * Method isJBossVersionValid
     *
     *
     * @param dir
     * @param props
     * @param IStateAccess
     *
     * @return ValidationResult
     *
     */
    public ValidationResult isJBossVersionValid(
        String jbossHomeDir,
        Map props,
        IStateAccess state) {
        ValidationResultStatus validRes = ValidationResultStatus.STATUS_FAILED;
        LocalizedMessage returnMessage = null;

        if (isJBossVersionValid(state)) {
            returnMessage = LocalizedMessage.get(
                    LOC_VA_MSG_JB_VAL_VERSION,
                    STR_JB_GROUP,
                    new Object[] { jbossHomeDir });

            validRes = ValidationResultStatus.STATUS_SUCCESS;
        }

        if (validRes.getIntValue() == ValidationResultStatus.INT_STATUS_FAILED) 
        {
            returnMessage = LocalizedMessage.get(
                    LOC_VA_WRN_JB_IN_VAL_VERSION,
                    STR_JB_GROUP,
                    new Object[] { jbossHomeDir });
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
                "VALID_JBOSS_VERSION",
                this.getClass().getMethod(
                    "isJBossVersionValid",
                    paramObjs));
        } catch (NoSuchMethodException nsme) {
            Debug.log(
                "JBossVersionValidator: NoSuchMethodException "
                + "thrown while loading method :",
                nsme);
            throw new InstallException(
                LocalizedMessage.get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND),
                nsme);
        } catch (SecurityException se) {
            Debug.log(
                "JBossVersionValidator: SecurityException thrown "
                + "while loading method :",
                se);
            throw new InstallException(
                LocalizedMessage.get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND),
                se);
        } catch (Exception ex) {
            Debug.log(
                "JBossVersionValidator: Exception thrown while "
                + "loading method :",
                ex);
            throw new InstallException(
                LocalizedMessage.get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND),
                ex);
        }
    }

    private boolean isJBossVersionValid(IStateAccess stateAccess) {
        boolean version32 = false;
        boolean version40 = false;
        boolean version42 = false;
        boolean version50 = false;
        boolean validVersion = false;
   
        try {
            String jbossJarPath = (String) stateAccess.get(
                    STR_KEY_JB_RUN_JAR_FILE);

            String temp = ExecuteCommand.executeJavaCommand(
                jbossJarPath,
                STR_JB_MAIN_CLASS,
                STR_JB_VERSION_ARGUMENT);

            if (temp != null) {
                Debug.log(
                    "JBossVersionValidator.getJBossVersion() command "
                    + "returned version name =" + temp);

                int majorVersion = 0;
                int minorVersion = 0;
                int secondMinorVersion = 0;

                int index1 = temp.indexOf(STR_JBOSS);

                if (index1 != -1) {
                    index1 += STR_JBOSS.length()+1;

                    if ((index1 <= temp.length())
                        && Character.isDigit(temp.charAt(index1))) {
                        majorVersion = Integer.parseInt(
                            temp.substring(
                                index1,
                                ++index1));

                        if ((index1 <= temp.length())
                            && (temp.charAt(index1) == '.')) {
                            index1++;

                            if ((index1 <= temp.length())
                                && Character.isDigit(temp.charAt(index1))) {
                                minorVersion = Integer.parseInt(
                                    temp.substring(
                                        index1,
                                        ++index1));

                                if ((index1 <= temp.length())
                                    && (temp.charAt(index1) == '.')) {
                                    index1++;

                                    if ((index1 <= temp.length())
                                        && Character.isDigit(
                                           temp.charAt(index1))) {
                                        secondMinorVersion = Integer.parseInt(
                                            temp.substring(
                                                index1,
                                                ++index1));
                                    }
                                }
                            }
                        }
                    } 
                }

                if ((majorVersion == JB_VER_32_MAJOR) &&
                   (minorVersion >= JB_VER_32_MINOR) &&
                   (secondMinorVersion >= JB_VER_32_SECOND_MINOR)) {
                        version32 = true;
                } else if ((majorVersion == JB_VER_40_MAJOR) && 
                           (minorVersion >= JB_VER_40_MINOR)) {
                        version40 = true;
                } else if ((majorVersion == JB_VER_42_MAJOR) && 
                        (minorVersion >= JB_VER_42_MINOR)) {
                    version42 = true;
                } else if ((majorVersion == JB_VER_50_MAJOR) && 
                        (minorVersion >= JB_VER_50_MINOR)) {
                    version50 = true;
                } 

                Debug.log(
                    "JBossVersionValidator.getJBossVersion()-Major version "
                    + Integer.toString(majorVersion)
                    + ", Minor Version = "
                    + Integer.toString(minorVersion));
            }
        } catch (Exception ex) {
            Debug.log(
                "JBossVersionValidator.getJBossVersion() threw "
                + " exception: " + ex.getMessage(),
                ex);
        }

               
        if (version32 || version40 || version42 || version50) {
            validVersion = true;
        } else {
            Debug.log(
                "JBossVersionValidator.getJBossVersion(): Unsupported " +
                "JBoss version. "
                + "Please check documentation for supported versions.");
        }

        return validVersion;
    }
}
