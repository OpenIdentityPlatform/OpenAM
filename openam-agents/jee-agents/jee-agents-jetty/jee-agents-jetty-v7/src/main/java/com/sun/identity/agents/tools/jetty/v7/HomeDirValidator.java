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
 * $Id: HomeDirValidator.java,v 1.1 2009/01/21 18:43:55 kanduls Exp $
 */

/**
 * Portions Copyrighted 2013 ForgeRock AS.
 */

package com.sun.identity.agents.tools.jetty.v7;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.configurator.ValidationResult;
import com.sun.identity.install.tools.configurator.ValidationResultStatus;
import com.sun.identity.install.tools.configurator.ValidatorBase;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.util.LocalizedMessage;
import java.io.File;
import java.util.Map;

public class HomeDirValidator extends ValidatorBase implements IConstants, IConfigKeys {

    public HomeDirValidator() throws InstallException {
        super();
    }

    /**
     * Method isHomeDirValid
     *
     *
     * @param jettyHomeDir
     * @param props
     * @param state
     *
     * @return ValidationResult
     *
     */
    public ValidationResult isHomeDirValid(String jettyHomeDir, Map props, IStateAccess state) {

        ValidationResultStatus validRes = ValidationResultStatus.STATUS_FAILED;
        LocalizedMessage returnMessage = null;

        if ((jettyHomeDir != null) && (jettyHomeDir.trim().length() >= 0)) {

            // The config dir has been normalized to have "/" only
            String jettyJarFile = jettyHomeDir + STR_FORWARD_SLASH + JETTY_START_JAR;
            String jettyIniFile = jettyHomeDir + STR_FORWARD_SLASH + JETTY_START_INI;
            String webAppDir =jettyHomeDir + STR_FORWARD_SLASH + JETTY_WEB_APP_DIR;
            if (FileUtils.isFileValid(jettyJarFile) && FileUtils.isFileValid(jettyIniFile) &&
                    canUpdateJettyConfigFile(jettyHomeDir)) {
                returnMessage = LocalizedMessage.get(VA_MSG_JETTY_VAL_HOME, STR_JETTY_GROUP,
                        new Object[] { jettyHomeDir });
                validRes = ValidationResultStatus.STATUS_SUCCESS;
                state.put(STR_KEY_JETTY_START_JAR_PATH, jettyJarFile);
                Debug.log("HomeDirValidator: Added Jetty start.jar to state --> " + jettyHomeDir);
                state.put(STR_KEY_JETTY_START_INI_PATH, jettyIniFile);
                Debug.log("HomeDirValidator: Added Jetty start.ini to state --> " + jettyIniFile);
                state.put(STR_KEY_JETTY_INST_DEPLOY_DIR, webAppDir);
                Debug.log("HomeDirValidator: Added Jetty webapp dir to state --> " + webAppDir);
            }
        }
        if (validRes.getIntValue() == ValidationResultStatus.INT_STATUS_FAILED) {
            returnMessage = LocalizedMessage.get(VA_WRN_JETTY_IN_VAL_HOME, STR_JETTY_GROUP,
                    new Object[] { jettyHomeDir });
        }
        Debug.log("HomeDirValidator : Is Jetty Home directory " + jettyHomeDir + " valid ? " + validRes.isSuccessful());

        return new ValidationResult(validRes, null, returnMessage);
    }

    public void initializeValidatorMap() throws InstallException {

        Class[] paramObjs = { String.class, Map.class, IStateAccess.class };

        try {
            getValidatorMap().put("VALID_JETTY_HOME_DIR", this.getClass().getMethod("isHomeDirValid", paramObjs));
        } catch (NoSuchMethodException nsme) {
            Debug.log("HomeDirValidator: NoSuchMethodException thrown while loading method :", nsme);
            throw new InstallException(LocalizedMessage.get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND), nsme);
        } catch (SecurityException se) {
            Debug.log("HomeDirValidator: SecurityException thrown while loading method :", se);
            throw new InstallException(LocalizedMessage.get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND), se);
        } catch (Exception ex) {
            Debug.log("HomeDirValidator: Exception thrown while loading method :", ex);
            throw new InstallException(LocalizedMessage.get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND), ex);
        }
    }

    protected boolean canUpdateJettyConfigFile(String jettyHomeDir) {

        String jettyConfFile =
                jettyHomeDir + STR_FORWARD_SLASH + STR_ETC_DIRECTORY + STR_FORWARD_SLASH + STR_SERVER_XML;
        File file = new File(jettyConfFile);

        return (file.exists() && file.canWrite() && file.canRead() && file.getParentFile().canWrite());
    }
}

