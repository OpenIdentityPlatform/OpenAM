/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: LoginConfBase.java,v 1.2 2008/06/25 05:52:02 qcheng Exp $
 *
 */

package com.sun.identity.agents.install.appserver;

import java.util.HashSet;
import java.util.Set;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallConstants;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.FileEditor;
import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.util.DeletePattern;


/** 
 * The abstract class configures the agent in login.conf file
 * of the Sun App Server
 */
public abstract class LoginConfBase implements InstallConstants {
    
    public LoginConfBase() {
        
    }
    
    /**
     * Method to be implemented by the sub classes which return the fully 
     * qualified Agent Realm class name.
     * 
     * @return the agent realm class name 
     */
    public abstract String getLoginModuleClassName();
        
    public boolean appendToFile(IStateAccess stateAccess) 
        throws InstallException 
    {       
        boolean status = false;      
        String loginConfFile = getLoginConfFile(stateAccess);
        try {
            FileUtils.appendDataToFile(loginConfFile, getAgentRealmConfig());
            status = true;
        } catch (Exception e) {
            Debug.log("LoginConfBase.appendToFile() - Error occurred while " +
                "adding Agent Realm to '" + loginConfFile + "'. ", e);
        }
    
        return status;
    }
    
    public boolean removeFromFile(IStateAccess stateAccess) 
        throws InstallException 
    {
        DeletePattern pattern1 = new DeletePattern("agentRealm", 
            DeletePattern.INT_MATCH_OCCURRANCE, 0);        
        DeletePattern pattern2 = new DeletePattern(getLoginModuleClassName(), 
            DeletePattern.INT_MATCH_OCCURRANCE, 0);
        DeletePattern pattern3 = new DeletePattern("};", 
            DeletePattern.INT_MATCH_FROM_START, 0);
        pattern3.setLastOccurranceInFile(true);

        Set matchPatterns = new HashSet();       
        matchPatterns.add(pattern1);
        matchPatterns.add(pattern2);
        matchPatterns.add(pattern3);     
        
        // Remove the lines with the match patterns from the login conf file
        String loginConfFile = getLoginConfFile(stateAccess);        
        FileEditor fileEditor = new FileEditor(loginConfFile);
        
        boolean status = false;
        try {
            status = fileEditor.deleteLines(matchPatterns);
        } catch (Exception e) {                                   
            Debug.log("ConfigureLoginConfTask.removeFromFile() - Exception" +
                " occurred while removing the Agent Realm from file '" + 
                loginConfFile + "'. ", e);            
        }
        
        return status;
    }
    
    public String getLoginConfFile(IStateAccess stateAccess) {
        return (String) stateAccess.get(STR_KEY_AS_LOGIN_CONF_FILE);
    }
    
    public String getAgentRealmConfig() {
        if (_agentRealmConfig == null) {
            StringBuffer sb = new StringBuffer(256);
            sb.append("agentRealm {").append(LINE_SEP);
            sb.append("    ").append(getLoginModuleClassName());
            sb.append("  required;").append(LINE_SEP);
            sb.append("};");        
            _agentRealmConfig = sb.toString();
        }
        return _agentRealmConfig;
    }
            
    private String _agentRealmConfig;
    
    public static final String STR_AS_GROUP = "asTools";
    public static String STR_KEY_AS_LOGIN_CONF_FILE = "AS_LOGIN_CONF_FILE";
}
