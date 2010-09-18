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
 * $Id: StartupScriptBase.java,v 1.5 2008/06/25 05:52:21 qcheng Exp $
 *
 */

package com.sun.identity.agents.tools.weblogic.v10;

import java.io.File;

import com.sun.identity.install.tools.util.ConfigUtil;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.FileEditor;
import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.util.DeletePattern;
import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallConstants;


/**
 * This task configure setAgentEvn script.
 */
public class StartupScriptBase implements InstallConstants, IConfigKeys {
    
    public StartupScriptBase() {
    }
    
    /**
     * Set classpath to j2ee_agents/am_wl10_agent/lib/agent.jar:
     * j2ee_agents/weblogic_v10_agent/lib/openssoclientsdk.jar:
     * j2ee_agents/weblogic_v10_agent/agent_xxx/config:
     * j2ee_agents/weblogic_v10_agent/locale
     */
    protected String getAgentClasspathEntries(IStateAccess stateAccess) {
        StringBuffer sb = new StringBuffer(256);
        
        String[] entries = getAgentClasspathArrayEntries(stateAccess);
        int count = entries.length;
        
        for(int i = 0; i < count; i++) {
            sb.append(entries[i]);
            if (i < count - 1) {
                sb.append(File.pathSeparator);
            }
        }
        
        return sb.toString();
    }
    
    
    private String[] getAgentClasspathArrayEntries(IStateAccess stateAccess) {
        
        if (agentClasspathEntries == null) {
            
            String homeDir = ConfigUtil.getHomePath();
            String libPath = ConfigUtil.getLibPath();
            String localeDir = ConfigUtil.getLocaleDirPath();
            
            
            String[] entries = null;
            if (getCoexistFlag(stateAccess)) {
                entries = new String[] {
                    libPath + FILE_SEP + STR_AGENT_JAR,
                    localeDir,
                };
            } else {
                String instanceName = stateAccess.getInstanceName();
	        if (instanceName == null) {
	            instanceName = DEFAULT_INSTANCE_NAME;
                }
                
                StringBuffer sb = new StringBuffer(256);
                sb.append(homeDir).append(FILE_SEP);
                sb.append(instanceName).append(FILE_SEP);
                sb.append(INSTANCE_CONFIG_DIR_NAME);
                String instanceConfigDirPath = sb.toString();
                
                entries = new String[] {
                    libPath + FILE_SEP + STR_AGENT_JAR,
                    libPath + FILE_SEP
                            + STR_FAM_CLIENT_SDK_JAR,
                    localeDir,
                    instanceConfigDirPath
                };
            }
            
            for (int i=0; i<entries.length; i++) {
                Debug.log("StartupScriptBase.getAgentClasspathEntries(): " +
                        "next entry: " + entries[i]);
            }
            agentClasspathEntries = entries;
        }
        
        return agentClasspathEntries;
    }
    
    protected String getAgentJavaOptions(IStateAccess stateAccess) {
        StringBuffer sb = new StringBuffer(256);
        
        if (agentJavaOptions == null) {
            if (!getCoexistFlag(stateAccess)) {
                String logConfigFileOption =
                        STR_LOG_CONFIG_FILE_OPTION_PREFIX
                        + ConfigUtil.getConfigDirPath() + FILE_SEP
                        + STR_LOG_CONFIG_FILENAME;
                sb.append(logConfigFileOption);
                sb.append(" ");
                sb.append(STR_LOG_COMPATMODE_OPTION);
            }
        }
        
        agentJavaOptions = sb.toString();
        Debug.log("StartupScriptBase.getAgentJavaOptions(): options: "
                + agentJavaOptions);
        return agentJavaOptions;
        
    }
    
    protected String getAgentEnvScriptPath(IStateAccess stateAccess) {
        
        String destFile = null;
        String osName = System.getProperty(STR_OS_NAME_PROPERTY);
        String startupscriptDir =
                (String) stateAccess.get(STR_KEY_STARTUP_SCRIPT_DIR);
        String instanceName = (String) stateAccess.get(STR_KEY_SERVER_NAME);
	if (instanceName == null) {
	    instanceName = DEFAULT_INSTANCE_NAME;
        }
        
        if (osName.toLowerCase().startsWith(STR_WINDOWS)) {
            destFile =
                    startupscriptDir + FILE_SEP + AGENT_ENV + "_" +
                    instanceName + ".cmd";
        } else {
            destFile = startupscriptDir + FILE_SEP +
                    AGENT_ENV + "_" + instanceName +".sh";
        }
        
        agentEnvScriptFile = FileUtils.replaceBackWithForward(destFile);
        return agentEnvScriptFile;
        
    }
    
    private boolean getCoexistFlag(IStateAccess stateAccess) {
        if (this.coexistFlag == null) {
            String strCoexistFlag = 
                    (String) stateAccess.get(STR_AM_COEXIST_KEY);
            Debug.log("StartupScriptBase.getCoexistFlag(): " +
                    "Co-exist flag in Install State: " +
                    strCoexistFlag);
            boolean coexistBoolean = 
                (strCoexistFlag != null) &&
                (strCoexistFlag.equalsIgnoreCase("true") ? true : false);
            this.coexistFlag = new Boolean(coexistBoolean);
            
            Debug.log("StartupScriptBase.getCoexistFlag(): Co-exist flag: " +
                    this.coexistFlag);
        }

        return coexistFlag.booleanValue(); 
    } 
    
    private Boolean coexistFlag;
    private String[] agentClasspathEntries;
    private String agentJavaOptions;
    private String agentEnvScriptFile;
    public static final String DEFAULT_INSTANCE_NAME = "AdminServer";
    
}
