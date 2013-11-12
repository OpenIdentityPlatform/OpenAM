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
 * $Id: StartupScriptValidator.java,v 1.2 2008/06/25 05:52:21 qcheng Exp $
 *
 */

package com.sun.identity.agents.tools.weblogic.v10;

import java.io.File;
import java.util.Map;
import java.util.HashMap;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.configurator.ValidationResult;
import com.sun.identity.install.tools.configurator.ValidationResultStatus;
import com.sun.identity.install.tools.configurator.ValidatorBase;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.util.LocalizedMessage;

/**
 * This task validates Weblogic's startup script.
 */
public class StartupScriptValidator extends ValidatorBase
        implements IConfigKeys {
    
    public StartupScriptValidator() throws InstallException {
        super();
    }
    
    /**
     * Method isStartupScriptValid
     *
     * @param dir
     * @param props
     * @param IStateAccess
     *
     * @return ValidationResult
     *
     */
    public ValidationResult isStartupScriptValid(String startupScr, Map props,
            IStateAccess state) {
        
        ValidationResultStatus validRes = ValidationResultStatus.STATUS_FAILED;
        LocalizedMessage returnMessage = null;
        
        if ((startupScr != null) && (startupScr.trim().length() >= 0)) {
            
            if (FileUtils.isFileValid(startupScr)) {
                // Store the script location in install state
                state.put(STR_KEY_WL_STARTUP_SCRIPT, startupScr);
                // Update state information
                setStartupScriptDir(state, startupScr);
                setConfigXML(state);
            }
            returnMessage = LocalizedMessage.get(
                    LOC_VA_MSG_WL_VAL_STARTUP_SCRIPT,
                    STR_WL_GROUP,new Object[] {startupScr});
            validRes = ValidationResultStatus.STATUS_SUCCESS;
            
        }
        
        if (validRes.getIntValue() ==
                ValidationResultStatus.INT_STATUS_FAILED) {
            returnMessage =
                    LocalizedMessage.get(LOC_VA_WRN_WL_IN_VAL_STARTUP_SCRIPT,
                    STR_WL_GROUP, new Object[] {startupScr});
        }
        
        Debug.log("startupScriptValidator : Is Startup script " +
                startupScr + " valid ? " + validRes.isSuccessful());
        
        return new ValidationResult(validRes,null,returnMessage);
    }
    
    
    public void initializeValidatorMap() throws InstallException {
        
        Class[] paramObjs = {String.class,Map.class,IStateAccess.class};
        
        try {
            getValidatorMap().put("VALID_WL_STARTUP_SCRIPT",
                this.getClass().getMethod("isStartupScriptValid",paramObjs));
            
        } catch (NoSuchMethodException nsme) {
            Debug.log("StartupScriptValidator: NoSuchMethodException " +
                    "thrown while loading method :",nsme);
            throw new InstallException(LocalizedMessage.get(
                    LOC_VA_ERR_VAL_METHOD_NOT_FOUND),nsme);
        } catch (SecurityException se){
            Debug.log("StartupScriptValidator: SecurityException thrown "
                    + "while loading method :",se);
            throw new InstallException(LocalizedMessage.get(
                    LOC_VA_ERR_VAL_METHOD_NOT_FOUND),se);
        } catch (Exception ex){
            Debug.log("StartupScriptValidator: Exception thrown while " +
                    "loading method :",ex);
            throw new InstallException(LocalizedMessage.get(
                    LOC_VA_ERR_VAL_METHOD_NOT_FOUND),ex);
        }
    }
    
    private void setStartupScriptDir(IStateAccess state, String startupScr) {
        String wlHomeDir = (new File(startupScr)).getParent();
        state.put(STR_KEY_STARTUP_SCRIPT_DIR, wlHomeDir);
    }
    
    private void setConfigXML(IStateAccess state) {
        String configXML = ((String) state.get(STR_KEY_STARTUP_SCRIPT_DIR)) +
                STR_FORWARD_SLASH + STR_CONFIG_XML;
        state.put(STR_KEY_CONFIG_XML, configXML);
    }
    
    /** Hashmap of Validator names and integers */
    Map validMap = new HashMap();
    
    /*
     * Localized constants
     */
    public static String LOC_VA_MSG_WL_VAL_STARTUP_SCRIPT =
            "VA_MSG_WL_VAL_STARTUP_SCRIPT";
    public static String LOC_VA_WRN_WL_IN_VAL_STARTUP_SCRIPT =
            "VA_WRN_WL_IN_VAL_STARTUP_SCRIPT";
    
}
