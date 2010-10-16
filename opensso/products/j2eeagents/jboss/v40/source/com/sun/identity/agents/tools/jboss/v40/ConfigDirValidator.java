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
 * $Id: ConfigDirValidator.java,v 1.1 2008/12/11 15:01:54 naghaon Exp $
 *
 */

package com.sun.identity.agents.tools.jboss.v40;

import com.sun.identity.agents.tools.jboss.IConfigKeys;
import com.sun.identity.agents.tools.jboss.IConstants;
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

/**
 * Validates JBoss server instance's conf directory.
 */
public class ConfigDirValidator extends ValidatorBase
        implements IConfigKeys, IConstants {
    
    /*
     * Localized constants
     */
    public static String LOC_VA_MSG_JB_VAL_CONFIG_DIR =
            "VA_MSG_JB_VAL_CONFIG_DIR";
    public static String LOC_VA_WRN_JB_IN_VAL_CONFIG_DIR =
            "VA_WRN_JB_IN_VAL_CONFIG_DIR";
    
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
        
        ValidationResultStatus validRes =
                ValidationResultStatus.STATUS_FAILED;
        LocalizedMessage returnMessage = null;
        
        if((configDir != null) && (configDir.trim().length() >= 0)) {
            
            // jboss-service.xml
            String jbServiceXmlFile = configDir + STR_FORWARD_SLASH +
                    STR_SERVICE_XML_FILE;
            // login-config.xml
            String loginConfFile = configDir + STR_FORWARD_SLASH +
                    STR_LOGIN_CONF_XML_FILE;
            
            if ((FileUtils.isFileValid(jbServiceXmlFile)) &&
                    (FileUtils.isFileValid(loginConfFile)))  {
                
                // store instance's  file locations in install state
                state.put(STR_KEY_JB_SERVICE_XML_FILE, jbServiceXmlFile);
                state.put(STR_KEY_JB_LOGIN_CONF_XML_FILE, loginConfFile);

                // store JBoss server instance name 
                String jbInstName = (new File(jbServiceXmlFile)).
                                           getParentFile().getParentFile().
                                           getName();
                state.put(STR_KEY_JB_INST_NAME, jbInstName);

                // store JBoss home and bin in install state
                String jbHomeDir = (new File(jbServiceXmlFile)).
                                           getParentFile().getParentFile().
                                           getParentFile().getParent();
                if ((jbHomeDir != null) && (jbHomeDir.length() > 0)) {
                   StringBuffer sb = new StringBuffer(jbHomeDir);
                   sb.append(STR_FORWARD_SLASH);
                   sb.append(STR_JB_SERVER_BIN);
                   sb.append(STR_FORWARD_SLASH);
                   sb.append(STR_JB_RUN_JAR);

                   String jbJarFile = sb.toString();

                   if (FileUtils.isFileValid(jbJarFile)) {
                       state.put(
                           STR_KEY_JB_HOME_DIR,
                           jbHomeDir);

                       state.put(
                           STR_KEY_JB_RUN_JAR_FILE,
                           jbJarFile);

                    }
                }
                returnMessage =
                        LocalizedMessage.get(LOC_VA_MSG_JB_VAL_CONFIG_DIR,
                        STR_JB_GROUP,new Object[] {configDir});
                        validRes = ValidationResultStatus.STATUS_SUCCESS;
            }
        }
        
        if (validRes.getIntValue() ==
                ValidationResultStatus.INT_STATUS_FAILED) {
            returnMessage =
                    LocalizedMessage.get(LOC_VA_WRN_JB_IN_VAL_CONFIG_DIR,
                    STR_JB_GROUP,new Object[] {configDir});
        }
        
        Debug.log("ConfigDirValidator : Is AS Config dir " +
                configDir + " valid ? " + validRes.isSuccessful());
	return new ValidationResult(validRes,null,returnMessage);
    }
    
    public void initializeValidatorMap() throws InstallException {
        
        Class[] paramObjs = {String.class,Map.class,IStateAccess.class};
        
        try {
                    
            getValidatorMap().put("VALID_JB_CONFIG_DIR",
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
