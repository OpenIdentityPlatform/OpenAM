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
 * $Id: ConfigDirValidator.java,v 1.1 2009/01/21 18:43:53 kanduls Exp $
 */
package com.sun.identity.agents.tools.jetty.v61;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.configurator.ValidationResult;
import com.sun.identity.install.tools.configurator.ValidationResultStatus;
import com.sun.identity.install.tools.configurator.ValidatorBase;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;
import java.io.File;
import java.util.Map;

public class ConfigDirValidator extends ValidatorBase implements IConstants,
        IConfigKeys {

    public ConfigDirValidator() throws InstallException {
        super();
    }

    /**
     * Method isConfigDirValid
     *
     *
     * @param dir
     * @param props
     * @param IStateAccess
     *
     * @return ValidationResult
     *
     */
    public ValidationResult isConfigDirValid(String configDir, Map props,
            IStateAccess state) {
        ValidationResultStatus validRes = ValidationResultStatus.STATUS_FAILED;
        LocalizedMessage returnMessage = null;

        if ((configDir != null) && (configDir.trim().length() >= 0)) {
            // The config dir has been normalized to have "/" only
            String serverXmlFile = configDir + STR_FORWARD_SLASH + 
                    STR_SERVER_XML;
            Debug.log(
                    "ConfigDirValidator.isConfigDirValid(): Server XML " +
                    "file path is: " + serverXmlFile);

            if (isValid(configDir) && isValid(serverXmlFile)) {
                state.put(STR_KEY_JETTY_SERVER_XML_FILE, serverXmlFile);
                returnMessage = LocalizedMessage.get(
                        VA_MSG_JETTY_VAL_CONFIG_DIR,
                        STR_JETTY_GROUP,
                        new Object[]{configDir});
                validRes = ValidationResultStatus.STATUS_SUCCESS;
            }
        }
        if (validRes.getIntValue() == ValidationResultStatus.INT_STATUS_FAILED) 
        {
            returnMessage = LocalizedMessage.get(
                    VA_WRN_JETTY_IN_VAL_CONFIG_DIR,
                    STR_JETTY_GROUP,
                    new Object[]{configDir});
        }
        Debug.log(
                "ConfigDirValidator.isConfigDirValid(): Is Jetty Server " +
                "Config dir " + configDir + " valid ? " + 
                validRes.isSuccessful());

        return new ValidationResult(validRes, null, returnMessage);
    }

    private boolean isValid(String fileName) {
        boolean valid = false;

        File file = new File(fileName);

        if (file.exists() && file.canRead() && file.canWrite()) {
            valid = true;
        } else {
            if (!file.exists()) {
                Debug.log(
                        "ConfigDirValidator.isValid(): " + fileName + 
                        " does not exist");
            } else {
                Debug.log(
                        "ConfigDirValidator.isValid(): " + fileName + 
                        " does not have read and write permissions");
            }
        }
        return valid;
    }

    public void initializeValidatorMap() throws InstallException {
        Class[] paramObjs = {String.class, Map.class, IStateAccess.class};

        try {
            getValidatorMap().put(
                    "VALID_JETTY_CONFIG_DIR",
                    this.getClass().getMethod(
                    "isConfigDirValid",
                    paramObjs));
        } catch (NoSuchMethodException nsme) {
            Debug.log(
                    "ConfigDirValidator: NoSuchMethodException " + 
                    "thrown while loading method :",
                    nsme);
            throw new InstallException(
                    LocalizedMessage.get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND),
                    nsme);
        } catch (SecurityException se) {
            Debug.log(
                    "ConfigDirValidator: SecurityException thrown " + 
                    "while loading method :",
                    se);
            throw new InstallException(
                    LocalizedMessage.get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND),
                    se);
        } catch (Exception ex) {
            Debug.log(
                    "ConfigDirValidator: Exception thrown while " + 
                    "loading method :",
                    ex);
            throw new InstallException(
                    LocalizedMessage.get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND),
                    ex);
        }
    }
}

