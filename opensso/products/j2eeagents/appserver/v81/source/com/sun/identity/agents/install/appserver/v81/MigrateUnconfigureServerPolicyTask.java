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
 * $Id: MigrateUnconfigureServerPolicyTask.java,v 1.2 2008/06/25 05:52:11 qcheng Exp $
 *
 */

package com.sun.identity.agents.install.appserver.v81;

import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.configurator.MigrateFromInstFinderStore;
import com.sun.identity.install.tools.configurator.InstallConstants;
import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.util.ConfigUtil;


/**
 * Un-Configure server.policy file during migration.
 */
public class MigrateUnconfigureServerPolicyTask extends
        UnconfigureServerPolicyTask implements InstallConstants {
    
    
    /*
     * get previous agent's lib path for unconfiguring.
     */
    protected String getLibPath(IStateAccess stateAccess) {
        String libPath = MigrateFromInstFinderStore.getProductHome() +
                FILE_SEP + INSTANCE_LIB_DIR_NAME;
        
        String remoteHomeDir = getRemoteHomeDir(stateAccess);
        if (remoteHomeDir != null) {
            libPath = remoteHomeDir + FILE_SEP +
                    INSTANCE_LIB_DIR_NAME;
        }
        Debug.log("MigrateUnconfigureServerPolicyTask.getLibPath() - " +
                "lib Dir:" + libPath);
        
        return libPath;
    }
    
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
