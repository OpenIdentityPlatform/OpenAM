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
 * $Id: ConfigDirValidator.java,v 1.1 2009/01/30 12:09:38 kalpanakm Exp $
 *
 */

package com.sun.identity.agents.install.jsr196;

import java.util.HashMap;
import java.util.Map;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.configurator.ValidationResult;
import com.sun.identity.install.tools.configurator.ValidationResultStatus;
import com.sun.identity.install.tools.configurator.ValidatorBase;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.util.LocalizedMessage;

/**
 * The configuration directory validation class for installation
 */
public class ConfigDirValidator extends ValidatorBase 
            implements IConstants, IConfigKeys {

    /**
     * Default constrcutor
     */    
    public ConfigDirValidator() throws InstallException {
        super();
    }
   
    
    public ValidationResult isConfigDirValid(String configDir, Map props,
        IStateAccess state) {

        ValidationResultStatus validRes =
            ValidationResultStatus.STATUS_FAILED;
        LocalizedMessage returnMessage = null; 
        
        if((configDir != null) && (configDir.trim().length() >= 0)) {
            
            // The config dir has been normalized to have "/" only
            String domainXmlFile = configDir + STR_FORWARD_SLASH + 
                STR_DOMAIN_XML;
            String loginConfFile = configDir + STR_FORWARD_SLASH + 
                STR_LOGIN_CONF;
            String serverPolicyFile = configDir + STR_FORWARD_SLASH + 
                STR_SERVER_POLICY;
            
            if ((FileUtils.isFileValid(domainXmlFile)) && 
                    (FileUtils.isFileValid(loginConfFile)) && 
                        (FileUtils.isFileValid(serverPolicyFile)))  {
                
                // store inst config dir, domain.xml, login.conf, server.policy 
                // file locations in install state
                state.put(STR_KEY_AS_DOMAIN_XML_FILE,domainXmlFile);
                state.put(STR_KEY_AS_LOGIN_CONF_FILE,loginConfFile);
                state.put(STR_KEY_AS_SERVER_POLICY_FILE,serverPolicyFile);
                
                returnMessage = 
                    LocalizedMessage.get(LOC_VA_MSG_AS_VAL_CONFIG_DIR,
                        STR_AS_GROUP,new Object[] {configDir});
                validRes = ValidationResultStatus.STATUS_SUCCESS;
            }
        } 
        
        if (validRes.getIntValue() == 
                ValidationResultStatus.INT_STATUS_FAILED) {
            returnMessage = 
                LocalizedMessage.get(LOC_VA_WRN_AS_IN_VAL_CONFIG_DIR,
                    STR_AS_GROUP,new Object[] {configDir});
        } 
        
        Debug.log("ConfigDirValidator : Is AS Config dir " + 
            configDir + " valid ? " + validRes.isSuccessful());
        return new ValidationResult(validRes,null,returnMessage);
    }
    
    public void initializeValidatorMap() throws InstallException {
        
        Class[] paramObjs = {String.class,Map.class,IStateAccess.class};
        
        try {
            getValidatorMap().put("VALID_AS_CONFIG_DIR",
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
    
    /** Hashmap of Validator names and integers */
    Map validMap = new HashMap();
    
    public static String STR_DOMAIN_XML = "domain.xml";
    public static String STR_LOGIN_CONF = "login.conf";
    public static String STR_SERVER_POLICY = "server.policy";
    
    /*
     * Localized constants
     */
    public static String LOC_VA_MSG_AS_VAL_CONFIG_DIR = 
        "VA_MSG_AS_VAL_CONFIG_DIR";
    public static String LOC_VA_WRN_AS_IN_VAL_CONFIG_DIR = 
        "VA_WRN_AS_IN_VAL_CONFIG_DIR";

}
