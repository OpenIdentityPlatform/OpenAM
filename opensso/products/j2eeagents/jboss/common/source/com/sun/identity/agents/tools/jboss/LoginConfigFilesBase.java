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
 * $Id: LoginConfigFilesBase.java,v 1.1 2008/12/11 14:36:06 naghaon Exp $
 *
 */

package com.sun.identity.agents.tools.jboss;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallConstants;
import com.sun.identity.install.tools.util.ConfigUtil;
import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.util.Debug;
import java.io.File;

public class LoginConfigFilesBase implements InstallConstants, IConfigKeys,
        IConstants {
    
    public LoginConfigFilesBase() {
    }
    
    public boolean copyLoginConfigXML(IStateAccess stateAccess) {
        boolean status = false;
        String srcDir = ConfigUtil.getConfigDirPath();
        String confDir = getConfigDir(stateAccess);
        String srcFile = srcDir + FILE_SEP +
                STR_AM_LOGIN_CONF_XML_FILE;
        String destFile = confDir + FILE_SEP +
                STR_AM_LOGIN_CONF_XML_FILE;
        try {
            FileUtils.copyFile(srcFile, destFile);
            Debug.log("ConfigureLoginConfigFileTask.execute() - copy " +
                    STR_AM_LOGIN_CONF_XML_FILE +
                    " from " + srcDir + " to " + confDir);
            status = true;
        } catch (Exception e) {
            Debug.log("ConfigureLoginConfigFileTask.execute() " +
                    " - Error occured while copying " + 
                    STR_AM_LOGIN_CONF_XML_FILE + " from " +
                    srcDir + " to " + confDir, e);
        }
        
        return status;
    }
    
    public boolean copyLoginConfigServiceXML(IStateAccess stateAccess) {
        boolean status = false;
        String srcDir = ConfigUtil.getConfigDirPath();
        String deployDir = getDeployDir(stateAccess);
        String srcFile = srcDir + FILE_SEP +
                STR_AM_LOGIN_CONF_SERVICE_XML_FILE;
        String destFile = deployDir + FILE_SEP +
                STR_AM_LOGIN_CONF_SERVICE_XML_FILE;
        try {
            FileUtils.copyFile(srcFile, destFile);
            Debug.log("ConfigureLoginConfigFileTask.execute() - copy " +
                    STR_AM_LOGIN_CONF_SERVICE_XML_FILE +
                    " from " + srcDir + " to " + deployDir);
            status = true;
        } catch (Exception e) {
             Debug.log("ConfigureLoginConfigFileTask.execute()" +
                    " - Error occured" +
                    " while copying " + STR_AM_LOGIN_CONF_SERVICE_XML_FILE +
                    " from " + srcDir + " to " + deployDir, e);
        }
        
        return status;
    }
    
    public boolean removeLoginConfigXML(IStateAccess stateAccess) {
        boolean status = false;
        String confDir = getConfigDir(stateAccess);
        try {
            File file = new File(confDir, STR_AM_LOGIN_CONF_XML_FILE);
            status = file.delete();
        } catch (Exception e) {
            Debug.log(
                    "ConfigureLoginConfigFileTask.rollBack() Unable to delete"
                    + " am-login-config.xml file", e);
        }
        
        return status;
    }
    
    public boolean removeLoginConfigServiceXML(IStateAccess stateAccess) {
        boolean status = false;
        String deployDir = getDeployDir(stateAccess);
        try {
            File file = new File(deployDir, STR_AM_LOGIN_CONF_SERVICE_XML_FILE);
            status = file.delete();
        } catch (Exception e) {
            Debug.log(
                    "ConfigureLoginConfigFileTask.rollBack() Unable to delete"
                    + " am-login-config-service.xml file", e);
        }
        
        return status;
    }
    
    private String getConfigDir(IStateAccess stateAccess) {
        String result = "";
        
        String confDir = (String)stateAccess.get(STR_KEY_JB_INST_CONF_DIR);
        String instance = (String)stateAccess.get(STR_KEY_JB_INST_NAME);
        if (confDir != null && confDir.length() > 0) {
            result = confDir;
        } else {
            String jbossDir = (String) stateAccess.get(STR_KEY_JB_HOME_DIR);
            result = jbossDir + FILE_SEP + STR_JB_SERVER +
                    FILE_SEP + instance +
                    FILE_SEP + STR_JB_INST_CONF;
            // Update state information
            stateAccess.put(STR_KEY_JB_INST_CONF_DIR, result); 
        }
        return result;
    }
    
    private String getDeployDir(IStateAccess stateAccess) {
        String result = "";
        
        String deployDir = (String)stateAccess.get(STR_KEY_JB_INST_DEPLOY_DIR);
        String instance = (String)stateAccess.get(STR_KEY_JB_INST_NAME);
        if (deployDir != null && deployDir.length() > 0) {
            result = deployDir;
        } else {
            String jbossDir = (String) stateAccess.get(STR_KEY_JB_HOME_DIR);
            result = jbossDir + FILE_SEP + STR_JB_SERVER +
                    FILE_SEP + instance +
                    FILE_SEP + STR_JB_INST_DEPLOY;
            // Update state information
            stateAccess.put(STR_KEY_JB_INST_DEPLOY_DIR, result); }
        return result;
    }
}
