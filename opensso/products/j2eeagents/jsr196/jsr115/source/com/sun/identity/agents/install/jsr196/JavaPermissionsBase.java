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
 * $Id: JavaPermissionsBase.java,v 1.1 2009/01/30 12:09:38 kalpanakm Exp $
 *
 */

package com.sun.identity.agents.install.jsr196;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallConstants;
import com.sun.identity.install.tools.util.ConfigUtil;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.FileEditor;
import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.util.DeletePattern;


/**
 * The class grants or removes Java Permissions to agent
 */
public class JavaPermissionsBase implements InstallConstants, IConfigKeys {
    
    public JavaPermissionsBase() {
    }
    
    public boolean addToServerPolicy(IStateAccess stateAccess) {
        boolean status = false;        
        String serverPolicyFile = getServerPolicyFile(stateAccess); 
        try {
            String libPath = getLibPath(stateAccess);
            FileUtils.appendDataToFile(serverPolicyFile, 
                    getPermissions(stateAccess));
            status = true;
        } catch (Exception e) {
            Debug.log("JavaPermissionsBase.addToServerPolicy() - Error " +
                "occurred while adding Agent Realm to '" + serverPolicyFile + 
                "'. ", e);
        }
    
        return status;
    }
    
    public boolean removeFromServerPolicy(IStateAccess stateAccess) {
        // Remove the lines with the match patterns from the login conf file
        String serverPolicyFileName = getServerPolicyFile(stateAccess);        
        FileEditor fileEditor = new FileEditor(serverPolicyFileName);
        
        boolean status = false;
        try {
            String libPath = getLibPath(stateAccess);
            DeletePattern pattern = new DeletePattern(libPath,
                DeletePattern.INT_MATCH_OCCURRANCE, 2);
            status = fileEditor.deleteLines(pattern);
        } catch (Exception e) {            
            Debug.log("JavaPermissionsBase.removeFromServerPolicy() - " + 
                "Exception occurred while removing the Agent Realm from " +
                "file '" + serverPolicyFileName + "'. ", e);            
        }       
        return status;
    }
    
    protected String getLibPath(IStateAccess stateAccess) {
        
        String libPath  = ConfigUtil.getLibPath();
        String remoteHomeDir = (String) stateAccess.get(
                STR_REMOTE_AGENT_INSTALL_DIR_KEY);
            // get the agent install directory on a remote instance
	    if (remoteHomeDir != null && remoteHomeDir.trim().length() > 0) {
	        libPath = remoteHomeDir + FILE_SEP + INSTANCE_LIB_DIR_NAME;
            }
        return libPath;
    }
    
    public String getServerPolicyFile(IStateAccess stateAccess) {
        return (String) stateAccess.get(STR_AS70_SERVER_POLICY_FILE_KEY);
    }
        
    private String getPermissions(IStateAccess stateAccess) {
         StringBuffer sb = new StringBuffer();
        sb.append(LINE_SEP);
        sb.append("grant codeBase \"file:").append(getLibPath(stateAccess));
        sb.append("/*\" {").append(LINE_SEP);
        sb.append("       permission java.security.AllPermission;");
        sb.append(LINE_SEP);
        sb.append("};");
        _javaPermissions = sb.toString();
        
        return _javaPermissions;
    }
    
    private void setPermissions(String javaPermissions) {
        _javaPermissions = javaPermissions;
    }
    
    public static final String STR_AS70_SERVER_POLICY_FILE_KEY = 
        "AS_SERVER_POLICY_FILE";    
    private String _javaPermissions;
}
