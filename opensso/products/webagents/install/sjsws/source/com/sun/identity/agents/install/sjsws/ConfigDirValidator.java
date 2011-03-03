/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ConfigDirValidator.java,v 1.3 2008/06/25 05:54:40 qcheng Exp $
 *
 */

package com.sun.identity.agents.install.sjsws;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallConstants;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.configurator.ValidationResult;
import com.sun.identity.install.tools.configurator.ValidationResultStatus;
import com.sun.identity.install.tools.configurator.ValidatorBase;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.util.LocalizedMessage;


import java.util.Map;
import java.io.File;

/**
 * Validates SWS server instance's conf directory.
 * This checks for certain SWS configuration files in the user
 * specified directory and validates the directory.
 */
public class ConfigDirValidator extends ValidatorBase
        implements InstallConstants, IConfigKeys, IConstants {

    private static String LOC_VA_MSG_SWS_VAL_CONFIG_DIR =
            "VA_MSG_SWS_VAL_CONFIG_DIR";
    private static String LOC_VA_WRN_SWS_IN_VAL_CONFIG_DIR =
            "VA_WRN_SWS_IN_VAL_CONFIG_DIR";
    
    /**
     * Constructs an instance of <code>ConfigDirValidator</code>
     */
    public ConfigDirValidator() throws InstallException {
        super();
    }
    
    /**
     * Validates the user specified config directory by verifying 
     * httpd, httpd.conf etc. files 
     *
     * @param configDir user specified config directory.
     * @param props properties
     * @param IStateAccess installer state access information
     *
     * @return ValidationResult
     */
    public ValidationResult isConfigDirValid(String configDir, Map props,
            IStateAccess state) {
        
        ValidationResultStatus validRes =
                ValidationResultStatus.STATUS_FAILED;
        LocalizedMessage returnMessage = null;
        
        if((configDir != null) && (configDir.trim().length() >= 0)) {
            
            // SWS obj.conf file
            String sjswsObjFile = configDir + FILE_SEP +
                    STR_SWS_OBJ_FILE;
            String sjswsMagnusFile = configDir + FILE_SEP +
                    STR_SWS_MAGNUS_FILE;
            
            if (FileUtils.isFileValid(sjswsObjFile) &&
                FileUtils.isFileValid(sjswsMagnusFile)) {
                
                // store instance's  file locations in install state
                state.put(STR_KEY_SWS_OBJ_FILE, sjswsObjFile);

                // store SWS home and bin in install state
                String sjswsHomeDir = (new File(sjswsObjFile)).
                                           getParentFile().
                                           getParentFile().
                                           getParent();
                state.put(
                           STR_KEY_SWS_HOME_DIR,
                           sjswsHomeDir);
                state.put(
                           STR_KEY_SWS_MAGNUS_FILE, sjswsMagnusFile);

                // store SWS agent specific value for notification
                // enable property in agent's configuration properties file
                state.put(STR_KEY_NOTIFICATION_ENABLE, STR_TRUE);

                // store SWS agent specific value for log
                // rotation property in agent's configuration properties file 
                state.put(STR_KEY_LOG_ROTATION, STR_TRUE);
   
                returnMessage =
                        LocalizedMessage.get(LOC_VA_MSG_SWS_VAL_CONFIG_DIR,
                        STR_SWS_GROUP,new Object[] {configDir});
                        validRes = ValidationResultStatus.STATUS_SUCCESS;
            }
        }
        
        if (validRes.getIntValue() ==
                ValidationResultStatus.INT_STATUS_FAILED) {
            returnMessage =
                    LocalizedMessage.get(LOC_VA_WRN_SWS_IN_VAL_CONFIG_DIR,
                    STR_SWS_GROUP,new Object[] {configDir});
        }
        
        Debug.log("ConfigDirValidator : Is SWS Config dir " +
                configDir + " valid ? " + validRes.isSuccessful());
        return new ValidationResult(validRes,null,returnMessage);
    }

    /**
     * Initializes validator data
     *
     * @exception InstallException if an error occurs during installation
     */
    public void initializeValidatorMap() throws InstallException {
        
        Class[] paramObjs = {String.class,Map.class,IStateAccess.class};
        
        try {
            getValidatorMap().put("VALID_SWS_CONFIG_DIR",
                    this.getClass().getMethod("isConfigDirValid",paramObjs));
            
        } catch (NoSuchMethodException nsme) {
            Debug.log("ConfigDirValidator: NoSuchMethodException " +
                    "thrown while loading method :",nsme);
            throw new InstallException(LocalizedMessage.get(
                    LOC_VA_ERR_VAL_METHOD_NOT_FOUND),nsme);
        } catch (SecurityException se){
            Debug.log("ConfigDirValidator: SecurityException thrown "
                    + "while loading method :",se);
            throw new InstallException(LocalizedMessage.get(
                    LOC_VA_ERR_VAL_METHOD_NOT_FOUND),se);
        } catch (Exception ex){
            Debug.log("ConfigDirValidator: Exception thrown while " +
                    "loading method :",ex);
            throw new InstallException(LocalizedMessage.get(
                    LOC_VA_ERR_VAL_METHOD_NOT_FOUND),ex);
        }
        
    }
}
