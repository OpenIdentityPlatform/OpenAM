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
 * $Id: MigrateUnconfigureDomainXMLTask.java,v 1.2 2008/06/25 05:52:11 qcheng Exp $
 *
 */

package com.sun.identity.agents.install.appserver.v81;

import java.util.ArrayList;

import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.configurator.MigrateFromInstFinderStore;
import com.sun.identity.install.tools.configurator.IStateAccess;

/**
 * Class to remove previous agent's config in domain.xml.
 */
public class MigrateUnconfigureDomainXMLTask extends UnconfigureDomainXMLTask {
    
    private static final String STR_AM_CLIENT_SDK_JAR = "amclientsdk.jar";
    private static final String STR_AM_CLIENT_SDK_JAR2 = "famclientsdk.jar";
    
    /*
     * For remote application server instance, set 
     * STR_SAVE_REMOTE_AGENT_INSTALL_DIR_KEY
     * as previous agent's install directory before unconfigure.
     */
    protected void preUnconfigureTasks(IStateAccess stateAccess) {
        
        String remoteHomeDir = getRemoteHomeDir(stateAccess);
        if (remoteHomeDir == null) {
            return;
        }
        
        String saveRemoteHomeDir = (String)stateAccess.get(
                STR_SAVE_REMOTE_AGENT_INSTALL_DIR_KEY);
        if (saveRemoteHomeDir != null && saveRemoteHomeDir.length() > 0) {
            stateAccess.put(STR_REMOTE_AGENT_INSTALL_DIR_KEY,
                    saveRemoteHomeDir);
        } else {
            stateAccess.put(STR_SAVE_REMOTE_AGENT_INSTALL_DIR_KEY,
                    remoteHomeDir);
        }
        
        String remoteAgentName = (String) stateAccess.get(
                STR_AGENT_INSTANCE_NAME_KEY);
        String saveAgentName = (String)stateAccess.get(
                STR_SAVE_AGENT_INSTANCE_NAME_KEY);
        if (saveAgentName != null && saveAgentName.length() > 0) {
            stateAccess.put(STR_AGENT_INSTANCE_NAME_KEY, saveAgentName);
        } else {
            stateAccess.put(STR_SAVE_AGENT_INSTANCE_NAME_KEY, remoteAgentName);
        }
        
    }
    
    /*
     * For remote application server instance, set
     * STR_SAVE_REMOTE_AGENT_INSTALL_DIR_KEY as new agent's install directory
     * after unconfigure, so other install tasks will use the new one.
     */
    protected void postUnconfigureTasks(IStateAccess stateAccess) {
        
        String remoteHomeDir = getRemoteHomeDir(stateAccess);
        if (remoteHomeDir == null) {
            return;
        }
        
        String migrateRemoteHomeDir = (String)stateAccess.get(
                STR_MIGRATE_REMOTE_AGENT_INSTALL_DIR_KEY);
        if (migrateRemoteHomeDir != null && migrateRemoteHomeDir.length() > 0) {
            stateAccess.put(STR_REMOTE_AGENT_INSTALL_DIR_KEY,
                    migrateRemoteHomeDir);
        }
        
        String migrateRemoteAgentName = (String)stateAccess.get(
                STR_MIGRATE_AGENT_INSTANCE_NAME_KEY);
        if (migrateRemoteAgentName != null && 
                migrateRemoteAgentName.length() > 0) {
            stateAccess.put(STR_AGENT_INSTANCE_NAME_KEY,
                    migrateRemoteAgentName);
        }
    }
    
    /*
     * get previous agent's classpath for unconfiguring.
     */
    protected String[] getAgentClasspathEntries(IStateAccess stateAccess) {
        
        String homeDir = getHomePath();
        String libPath = getLibPath();
        String localeDir = getLocaleDirPath();
        
        String remoteHomeDir = getRemoteHomeDir(stateAccess);
        // get the agent install directory on a remote instance
        if (remoteHomeDir != null) {
            homeDir = remoteHomeDir;
            libPath = remoteHomeDir + FILE_SEP + INSTANCE_LIB_DIR_NAME;
            localeDir = remoteHomeDir + FILE_SEP +
                    INSTANCE_LOCALE_DIR_NAME;
            
            Debug.log(
                    "MigrateUnconfigureDomainXMLTask." +
                    "getAgentClassPathEntries: Modified libPath = "
                    + libPath);
        }
        
        ArrayList entryList = new ArrayList();
        String instanceName = getAgentInstanceName(stateAccess);
        
        StringBuffer sb = new StringBuffer(256);
        sb.append(homeDir).append(FILE_SEP);
        sb.append(instanceName).append(FILE_SEP);
        sb.append(INSTANCE_CONFIG_DIR_NAME);
        String instanceConfigDirPath = sb.toString();
        
        entryList.add(libPath + FILE_SEP + STR_AGENT_JAR);
        entryList.add(libPath + FILE_SEP + STR_FM_CLIENT_SDK_JAR);
        entryList.add(localeDir);
        entryList.add(instanceConfigDirPath);
        entryList.add(libPath + FILE_SEP + STR_AM_CLIENT_SDK_JAR);
        entryList.add(libPath + FILE_SEP + STR_AM_CLIENT_SDK_JAR2);
        
        Debug.log("MigrateUnconfigureDomainXMLTask." +
                "getAgentClasspathEntries(): " +
                entryList);
        
        return (String[]) entryList.toArray(new String[0]);
    }
    
    /*
     * get previous agent's home directory.
     */
    protected String getHomePath() {
        return MigrateFromInstFinderStore.getProductHome();
    }
    
    /*
     * get previous agent's lib directory.
     */
    protected String getLibPath() {
        return MigrateFromInstFinderStore.getProductHome() + FILE_SEP +
                INSTANCE_LIB_DIR_NAME;
    }
    
    /*
     * get previous agent's locale directory.
     */
    protected String getLocaleDirPath() {
        return MigrateFromInstFinderStore.getProductHome() + FILE_SEP +
                INSTANCE_LOCALE_DIR_NAME;
    }
    
    /*
     * get previous agent's config directory.
     */
    protected String getConfigDirPath() {
        return MigrateFromInstFinderStore.getProductHome() + FILE_SEP +
                INSTANCE_CONFIG_DIR_NAME;
    }
    
    /*
     * get previous agent instance's name.
     */
    private String getAgentInstanceName(IStateAccess stateAccess) {
        String instanceName = null;
        String agentInstanceName = (String) stateAccess.get(
                STR_AGENT_INSTANCE_NAME_KEY);
        
        // Get the user input for agent instance name only when instance is
        // remote.
        if (agentInstanceName != null &&
                agentInstanceName.trim().length() > 0) {
            instanceName = agentInstanceName;
            Debug.log(
                    "MigrateUnconfigureDomainXMLTask.getAgentInstanceName():" +
                    "Using remote agent instance name : "+ agentInstanceName);
        } else {
            instanceName = (String)stateAccess.get(
                    STR_INSTANCE_NAME_MIGRATE_TAG);
        }
        
        return instanceName;
    }
    
    /*
     * If any, get previous agent's install directory for remote appserver 
     * instance.
     */
    private String getRemoteHomeDir(IStateAccess stateAccess) {
        
        String remoteHomeDir = (String)stateAccess.get(
                STR_REMOTE_AGENT_INSTALL_DIR_KEY);
        if (remoteHomeDir == null || remoteHomeDir.length() == 0) {
            return null;
        } else {
            return remoteHomeDir;
        }
    }

}
