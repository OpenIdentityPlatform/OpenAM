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
 * $Id: WebsphereDetailsValidator.java,v 1.2 2008/11/21 22:21:44 leiming Exp $
 *
 */

package com.sun.identity.agents.tools.websphere;

import java.io.File;
import java.util.Map;
import java.util.HashMap;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.ValidationResult;
import com.sun.identity.install.tools.configurator.ValidationResultStatus;
import com.sun.identity.install.tools.configurator.ValidatorBase;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.configurator.InstallConstants;
import com.sun.identity.install.tools.configurator.InstallException;

/**
 * Validator class for WAS.
 *
 */
public class WebsphereDetailsValidator extends ValidatorBase
        implements InstallConstants,IConfigKeys,IConstants {
    
    public WebsphereDetailsValidator() throws InstallException {
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
    public ValidationResult isHomeDirValid(String homeDir, Map props,
            IStateAccess state) {
        
        ValidationResultStatus validRes = ValidationResultStatus.STATUS_FAILED;
        LocalizedMessage returnMessage = null;
        
        Debug.log("WebsphereDetailsValidator:isHomeDirValid()");
        if ((homeDir != null) && (homeDir.trim().length() >= 0)) {
            File homeDirFile = new File(homeDir);
            if (homeDirFile.exists() && homeDirFile.isDirectory() &&
                    homeDirFile.canRead()) {
                Debug.log("WebsphereDetailsValidator:isHomeDirValid() - "
                        + homeDir + " is valid");
                File binDir = new File(homeDir + STR_FILE_SEP + STR_BIN_LEAF);
                File libDir = new File(homeDir + STR_FILE_SEP + STR_LIB_LEAF);
                
                if (binDir.exists() && binDir.isDirectory() &&
                        libDir.exists() && libDir.isDirectory()) {
                    returnMessage = LocalizedMessage.get(
                            LOC_VA_MSG_VAL_WEBSPHERE_HOME_DIR,
                            STR_WAS_GROUP,new Object[] {homeDir});
                    validRes = ValidationResultStatus.STATUS_SUCCESS;
                }
            }
        }
        
        if (validRes.getIntValue() ==
                ValidationResultStatus.INT_STATUS_FAILED) {
            returnMessage =
                    LocalizedMessage.get(LOC_VA_WRN_INVAL_WEBSPHERE_HOME_DIR,
                    STR_WAS_GROUP, new Object[] {homeDir});
        }
        
        Debug.log("WebsphereDetailsValidator: Is Home Directory " +
                homeDir + " valid ? " + validRes.isSuccessful());
        
        return new ValidationResult(validRes,null,returnMessage);
    }
    
    /**
     * Method isServerInstanceDirectoryValid
     *
     *
     * @param dir
     * @param props
     * @param IStateAccess
     *
     * @return ValidationResult
     *
     */
    public ValidationResult isServerInstanceDirectoryValid(String serverInstDir,
            Map props, IStateAccess state) {
        
        ValidationResultStatus validRes = ValidationResultStatus.STATUS_FAILED;
        LocalizedMessage returnMessage = null;
        
        if ((serverInstDir != null) && (serverInstDir.trim().length() >= 0)) {
            String serverXmlFile = serverInstDir + STR_FILE_SEP +
                    STR_SERVER_XML_FILE;
            
            if (FileUtils.isFileValid(serverXmlFile)) {
                // Store the server instance file location in state
                state.put(STR_KEY_WAS_SERVER_XML_FILE, serverXmlFile);
                returnMessage = LocalizedMessage.get(
                        LOC_VA_MSG_WEBSPHERE_VAL_SERVER_XML,
                        STR_WAS_GROUP,new Object[] {serverInstDir});
                validRes = ValidationResultStatus.STATUS_SUCCESS;
            }
        }
        
        if (validRes.getIntValue() ==
                ValidationResultStatus.INT_STATUS_FAILED) {
            returnMessage =
                    LocalizedMessage.get(LOC_VA_WRN_WEBSPHERE_IN_VAL_SERVER_XML,
                    STR_WAS_GROUP, new Object[] {serverInstDir});
        }
        
        Debug.log("WebsphereDetailsValidator : Is Server Instance Directory " +
                serverInstDir + " valid ? " + validRes.isSuccessful());
        
        return new ValidationResult(validRes,null,returnMessage);
    }
    
    /**
     * Method isServerInstanceNameValid
     *
     *
     * @param name
     * @param props
     * @param IStateAccess
     *
     * @return ValidationResult
     *
     */
    public ValidationResult isServerInstanceNameValid(String name,
            Map props, IStateAccess state) {
        
        ValidationResultStatus validRes = ValidationResultStatus.STATUS_FAILED;
        LocalizedMessage returnMessage = null;
        
        if ((name != null) && (name.trim().length() >= 0)) {
            String serverXmlFile = (String)state.get(
                    STR_KEY_WAS_SERVER_XML_FILE);
            
            if (FileUtils.isFileValid(serverXmlFile)) {
                if (FileUtils.getFirstOccurence(serverXmlFile,"\"" + name
                        + "\"",false, false, true) > 0) {
                    returnMessage = LocalizedMessage.get(
                            LOC_VA_MSG_WEBSPHERE_VAL_SERVER_INST_NAME,
                            STR_WAS_GROUP,new Object[] {name});
                    validRes = ValidationResultStatus.STATUS_SUCCESS;
                }
            }
        }
        
        if (validRes.getIntValue() ==
                ValidationResultStatus.INT_STATUS_FAILED) {
            returnMessage =
                    LocalizedMessage.get(
                    LOC_VA_WRN_WEBSPHERE_IN_VAL_SERVER_INST_NAME,
                    STR_WAS_GROUP, new Object[] {name});
        }
        
        Debug.log("WebsphereDetailsValidator : Is Server Instance Name " +
                name + " valid ? " + validRes.isSuccessful());
        
        return new ValidationResult(validRes,null,returnMessage);
    }
    
    
    
    
    public void initializeValidatorMap() throws InstallException {
        
        Class[] paramObjs = {String.class,Map.class,IStateAccess.class};
        
        try {
            getValidatorMap().put("VALID_WEBSPHERE_HOME_DIR",
                    this.getClass().getMethod("isHomeDirValid",paramObjs));
            getValidatorMap().put("VALID_SERVER_INSTANCE_DIR",
                    this.getClass().getMethod("isServerInstanceDirectoryValid",
                    paramObjs));
            getValidatorMap().put("VALID_SERVER_INSTANCE_NAME",
                    this.getClass().getMethod("isServerInstanceNameValid",
                    paramObjs));
            
        } catch (NoSuchMethodException nsme) {
            Debug.log("WebsphereDetailsValidator: NoSuchMethodException " +
                    "thrown while loading method :",nsme);
            throw new InstallException(LocalizedMessage.get(
                    LOC_VA_ERR_VAL_METHOD_NOT_FOUND),nsme);
        } catch (SecurityException se){
            Debug.log("WebsphereDetailsValidator: SecurityException thrown "
                    + "while loading method :",se);
            throw new InstallException(LocalizedMessage.get(
                    LOC_VA_ERR_VAL_METHOD_NOT_FOUND),se);
        } catch (Exception ex){
            Debug.log("WebsphereDetailsValidator: Exception thrown while " +
                    "loading method :",ex);
            throw new InstallException(LocalizedMessage.get(
                    LOC_VA_ERR_VAL_METHOD_NOT_FOUND),ex);
        }
    }
    
    /** Hashmap of Validator names and integers */
    Map validMap = new HashMap();
    
   /*
    * Localized constants
    */
    public static String LOC_VA_MSG_VAL_WEBSPHERE_HOME_DIR =
            "VA_MSG_VAL_WEBSPHERE_HOME_DIR";
    public static String LOC_VA_WRN_INVAL_WEBSPHERE_HOME_DIR =
            "VA_WRN_INVAL_WEBSPHERE_HOME_DIR";
    public static String LOC_VA_MSG_WEBSPHERE_VAL_SERVER_XML =
            "VA_MSG_WEBSPHERE_VAL_SERVER_XML";
    public static String LOC_VA_WRN_WEBSPHERE_IN_VAL_SERVER_XML =
            "VA_WRN_WEBSPHERE_IN_VAL_SERVER_XML";
    public static String LOC_VA_MSG_WEBSPHERE_VAL_SERVER_INST_NAME =
            "VA_MSG_WEBSPHERE_VAL_SERVER_INST_NAME";
    public static String LOC_VA_WRN_WEBSPHERE_IN_VAL_SERVER_INST_NAME =
            "VA_WRN_WEBSPHERE_IN_VAL_SERVER_INST_NAME";
    
    public static String STR_BIN_LEAF = "bin";
    public static String STR_LIB_LEAF = "lib";
    
}

