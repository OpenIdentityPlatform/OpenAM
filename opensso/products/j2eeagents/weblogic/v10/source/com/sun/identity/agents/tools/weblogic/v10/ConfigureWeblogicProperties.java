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
 * $Id: ConfigureWeblogicProperties.java,v 1.3 2008/06/25 05:52:21 qcheng Exp $
 *
 */

package com.sun.identity.agents.tools.weblogic.v10;

import java.util.Map;

import com.sun.identity.install.tools.configurator.ITask;
import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallConstants;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.ConfigUtil;
import com.sun.identity.install.tools.util.FileUtils;

/**
 * This task will configure Agents configuration properties file for 
 * Weblogic Portal.
 */
public class ConfigureWeblogicProperties implements ITask, IConfigKeys,
        InstallConstants {
    
    public boolean execute(String name, IStateAccess stateAccess,
            Map properties) throws InstallException {
        boolean status = false;
        String domainType = (String)stateAccess.get(STR_KEY_WL_DOMAIN);
        
        Debug.log("ConfigureWeblogicProperties.execute() : domain type = "
                + domainType);
        
        if ((domainType != null) &&
                (domainType.equals(STR_PORTAL_DOMAIN_TYPE))) {
            
            String portalApp = (String)stateAccess.get(STR_PORTAL_CONTEXT_URI);
            String confFilePath = getInstanceConfigPath(stateAccess) +
                    STR_FORWARD_SLASH + STR_AMAGENT_FILE;
            
            Debug.log("ConfigureWeblogicProperties.execute() - Configuring " +
                    confFilePath + " with WebLogic Portal related properties");
            
            if ((portalApp != null) && (portalApp.length() > 0)) {
                String portalURI = portalApp.substring(1);
                
                // Update the j2ee auth handler
                status = FileUtils.addMapProperty(confFilePath,STR_AUTH_HANDLER,
                        portalURI, STR_J2EE_WL_PORTAL_HANDLER);
                // Update the j2ee logout handler
                status = status &&
                        FileUtils.addMapProperty(confFilePath,
                        STR_LOGOUT_HANDLER,
                        portalURI, STR_J2EE_WL_PORTAL_LOGOUT_HANDLER);
            }
        } else {
            // Nothing to do
            Debug.log("ConfigureWeblogicProperties.execute() : " +
                    "nothing to execute");
            status = true;
        }
        
        return status;
    }
    
    private String getInstanceConfigPath(IStateAccess stateAccess) {
        
        String instanceName = stateAccess.getInstanceName();
        String homeDir = ConfigUtil.getHomePath();
        
        StringBuffer sb = new StringBuffer(256);
        sb.append(homeDir).append(STR_FORWARD_SLASH);
        sb.append(instanceName).append(STR_FORWARD_SLASH);
        sb.append(INSTANCE_CONFIG_DIR_NAME);
        
        return sb.toString();
    }
    
    
    public boolean rollBack(String name, IStateAccess stateAccess,
            Map properties) throws InstallException {
        boolean status = true;
        Debug.log("ConfigureWeblogicProperties.rollBack() - ");
        return status;
    }
    
    public LocalizedMessage getExecutionMessage(IStateAccess stateAccess,
            Map properties) {
        String confFilePath = ConfigUtil.getConfigDirPath() +
                STR_FORWARD_SLASH + STR_AMAGENT_FILE;
        
        Object[] args = { confFilePath };
        LocalizedMessage message =
                LocalizedMessage.get(LOC_TSK_MSG_CONFIGURE_AMAGENT_EXECUTE,
                STR_WL_GROUP, args);
        
        return message;
    }
    
    public LocalizedMessage getRollBackMessage(IStateAccess stateAccess,
            Map properties) {
        String confFilePath = ConfigUtil.getConfigDirPath() +
                STR_FORWARD_SLASH + STR_AMAGENT_FILE;
        Object[] args = { confFilePath };
        LocalizedMessage message =
                LocalizedMessage.get(LOC_TSK_MSG_CONFIGURE_AMAGENT_ROLLBACK,
                STR_WL_GROUP, args);
        
        return message;
    }
    
    public static final String LOC_TSK_MSG_CONFIGURE_AMAGENT_EXECUTE =
            "TSK_MSG_CONFIGURE_AMAGENT_EXECUTE";
    public static final String LOC_TSK_MSG_CONFIGURE_AMAGENT_ROLLBACK =
            "TSK_MSG_CONFIGURE_AMAGENT_ROLLBACK";
}
