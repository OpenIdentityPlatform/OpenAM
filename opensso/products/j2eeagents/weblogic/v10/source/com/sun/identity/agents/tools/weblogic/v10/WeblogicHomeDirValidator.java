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
 * $Id: WeblogicHomeDirValidator.java,v 1.4 2008/06/25 05:52:21 qcheng Exp $
 *
 */

package com.sun.identity.agents.tools.weblogic.v10;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.configurator.ValidationResult;
import com.sun.identity.install.tools.configurator.ValidationResultStatus;
import com.sun.identity.install.tools.configurator.ValidatorBase;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.configurator.InstallConstants;
import com.sun.identity.install.tools.configurator.InstallException;

/**
 * This task validates Weblogic's home directory.
 */
public class WeblogicHomeDirValidator extends ValidatorBase
        implements InstallConstants,IConfigKeys {
    
    public WeblogicHomeDirValidator() throws InstallException {
        super();
    }
    
    
    public ValidationResult isHomeDirValid(String homeDir, Map props,
            IStateAccess state) {
        
        ValidationResultStatus validRes = ValidationResultStatus.STATUS_FAILED;
        LocalizedMessage returnMessage = null;
        
        Debug.log("WeblogicHomeDirValidator:isHomeDirValid()");
        if ((homeDir != null) && (homeDir.trim().length() >= 0)) {
            File homeDirFile = new File(homeDir);
            if (homeDirFile.exists() && homeDirFile.isDirectory() &&
                    homeDirFile.canRead()) {
                Debug.log("WeblogicHomeDirValidator:isHomeDirValid() - "
                        + homeDir + " is valid");
                returnMessage = LocalizedMessage.get(
                        LOC_VA_MSG_WL_VAL_WEBLOGIC_HOME_DIR,
                        STR_WL_GROUP,new Object[] {homeDir});
                validRes = ValidationResultStatus.STATUS_SUCCESS;
            }
        }
        
        if (validRes.getIntValue() ==
                ValidationResultStatus.INT_STATUS_FAILED) {
            returnMessage =
                    LocalizedMessage.get(LOC_VA_WRN_WL_INVAL_WEBLOGIC_HOME_DIR,
                    STR_WL_GROUP, new Object[] {homeDir});
        }
        
        Debug.log("WeblogicHomeDirValidator: Is Home Directory " +
                homeDir + " valid ? " + validRes.isSuccessful());
        
        return new ValidationResult(validRes,null,returnMessage);
    }
    
    
    public ValidationResult isWeblogicServerPortal(String isPortal, Map props,
            IStateAccess state) {
        
        ValidationResultStatus validRes = ValidationResultStatus.STATUS_SUCCESS;
        LocalizedMessage returnMessage = null;
        String userEnterDomain = null;
        
        if (isPortal.equalsIgnoreCase(STR_TRUE_VALUE)) {
            userEnterDomain = STR_PORTAL_DOMAIN_TYPE;
        } else {
            userEnterDomain = STR_SERVER_DOMAIN_TYPE;
        }

        state.put(STR_KEY_WL_DOMAIN, userEnterDomain);
        
        Debug.log("WeblogicHomeDirValidator.isWeblogicServerPortal() - " +
                    "User Entered domain type is:" + userEnterDomain);
    
        return new ValidationResult(validRes, null, returnMessage);
    }
    
    
    public void initializeValidatorMap() throws InstallException {
        
        Class[] paramObjs = {String.class,Map.class,IStateAccess.class};
        
        try {
            getValidatorMap().put("VALID_WL_HOME_DIR",
                    this.getClass().getMethod("isHomeDirValid",paramObjs));
            getValidatorMap().put("VALID_WL_DOMAIN",
                    this.getClass().getMethod("isWeblogicServerPortal",
                    paramObjs));
            
        } catch (NoSuchMethodException nsme) {
            Debug.log("WeblogicHomeDirValidator: NoSuchMethodException " +
                    "thrown while loading method :",nsme);
            throw new InstallException(LocalizedMessage.get(
                    LOC_VA_ERR_VAL_METHOD_NOT_FOUND),nsme);
        } catch (SecurityException se){
            Debug.log("WeblogicHomeDirValidator: SecurityException thrown "
                    + "while loading method :",se);
            throw new InstallException(LocalizedMessage.get(
                    LOC_VA_ERR_VAL_METHOD_NOT_FOUND),se);
        } catch (Exception ex){
            Debug.log("WeblogicHomeDirValidator: Exception thrown while " +
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
    public static String LOC_VA_MSG_WL_VAL_WEBLOGIC_HOME_DIR =
            "VA_MSG_WL_VAL_WEBLOGIC_HOME_DIR";
    public static String LOC_VA_WRN_WL_INVAL_WEBLOGIC_HOME_DIR =
            "VA_WRN_WL_INVAL_WEBLOGIC_HOME_DIR";
    
}
