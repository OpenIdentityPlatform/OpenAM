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
 * $Id: ConfigurePropertiesTask.java,v 1.8 2008/08/04 19:29:27 huacui Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.io.File;
import java.util.Map;

import com.sun.identity.install.tools.util.ConfigUtil;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.ReplaceTokens;

public class ConfigurePropertiesTask implements ITask, InstallConstants {

    public boolean execute(String name, IStateAccess stateAccess, 
            Map properties) throws InstallException {
        boolean status = false;
 
        String agentTemplate = (String) properties.get(
                   STR_PRODUCT_CONFIG_FILENAME_AGENT_BOOTSTRAP_TEMPLATE_KEY);
        
        String configFile = ConfigUtil.getConfigDirPath()
                           + FILE_SEP
                           + agentTemplate;
        String instanceConfigFile = getInstanceConfigFile(stateAccess,
                                                          properties);

        Debug.log("ConfigurePropertiesTask.execute() - Generating a tag "
                + "swapped '" + instanceConfigFile + "' using file '"
                + configFile);

        try {
            Map tokens = stateAccess.getData();
            ReplaceTokens filter = new ReplaceTokens(configFile,
                                               instanceConfigFile, tokens);
            filter.tagSwapAndCopyFile();
            // Copy the Config File location to the state
            stateAccess.put(STR_CONFIG_FILE_PATH_TAG, instanceConfigFile);

            // Generate the OpenSSOAgentConfiguration.properties file 
            String agentConfigTemplate = (String) properties.get(
                        STR_PRODUCT_CONFIG_FILENAME_AGENT_CONFIG_TEMPLATE_KEY);
            String agentConfigTemplateFile = ConfigUtil.getConfigDirPath()
                                             + FILE_SEP
                                             + agentConfigTemplate;
            String agentConfigFile = getAgentConfigFile(stateAccess,
                                                        properties);
            Debug.log("ConfigurePropertiesTask.execute()- Generating a tag "
                      + "swapped '" + agentConfigFile + "' using file '"
                      + agentConfigTemplateFile);
            ReplaceTokens tagFilter = new ReplaceTokens(
                                              agentConfigTemplateFile,
                                              agentConfigFile, tokens);
            tagFilter.tagSwapAndCopyFile();
            stateAccess.put(STR_CONFIG_AGENT_CONFIG_FILE_PATH_TAG, 
                                agentConfigFile);
            status = true;
        } catch (Exception e) {
            Debug.log("ConfigurePropertiesTask.execute() - Exception "
                    + "occurred while tag swapping properties. ", e);
        }
        return status;
    }

    public LocalizedMessage getExecutionMessage(IStateAccess stateAccess,
            Map properties) {
        String productConfigFileName = (String) properties
                .get(STR_PRODUCT_CONFIG_FILENAME_AGENT_BOOTSTRAP_KEY);
        Object[] args = { productConfigFileName, stateAccess.getInstanceName() 
                };
        LocalizedMessage message = LocalizedMessage.get(
                LOC_TSK_MSG_CONFIGURE_PRODUCT_PROPS_EXECUTE, args);
        return message;
    }

    public LocalizedMessage getRollBackMessage(IStateAccess stateAccess,
            Map properties) {
        String productConfigFileName = (String) properties
                .get(STR_PRODUCT_CONFIG_FILENAME_AGENT_BOOTSTRAP_KEY);
        Object[] args = { productConfigFileName, stateAccess.getInstanceName() 
                };
        LocalizedMessage message = LocalizedMessage.get(
                LOC_TSK_MSG_CONFIGURE_PRODUCT_PROPS_ROLLBACK, args);
        return message;
    }

    public boolean rollBack(String name, IStateAccess stateAccess,
            Map properties) throws InstallException {
        File file = null;
        boolean status = false;
	String printStatus = null;

        String instanceConfigFile = getInstanceConfigFile(stateAccess,
                properties);
        file = new File(instanceConfigFile);
        status = file.delete();

        printStatus = (status) ? "Successful." : "FAILED.";
        Debug.log("ConfigurePropertiesTask.rollBack() - Deleting file '"
                + instanceConfigFile + "'. " + printStatus);

	String agentConfigFile = getAgentConfigFile(stateAccess,
                                                           properties);
        file = new File(agentConfigFile);
        status = file.delete();

        printStatus = (status) ? "Successful." : "FAILED.";
        Debug.log("ConfigurePropertiesTask.rollBack() - Deleting file '"
                + agentConfigFile + "'. " + printStatus);
	
        return status;
    }

    private String getInstanceConfigFile(IStateAccess stateAccess,
            Map properties) {
        return (String) stateAccess.get(STR_CONFIG_DIR_PREFIX_TAG) + FILE_SEP
                + (String) properties.get(
                           STR_PRODUCT_CONFIG_FILENAME_AGENT_BOOTSTRAP_KEY);
    }

    private String getAgentConfigFile(IStateAccess stateAccess,
            Map properties) {
        return (String) stateAccess.get(STR_CONFIG_DIR_PREFIX_TAG) + FILE_SEP
                + (String) properties.get(
                                STR_PRODUCT_CONFIG_FILENAME_AGENT_CONFIG_KEY);
                                 
    }

    public String getAgentMigratePropertiesFile(IStateAccess stateAccess,
            Map properties) {
        return (String) ConfigUtil.getConfigDirPath() + FILE_SEP
                + (String) properties.get(
                  STR_PRODUCT_CONFIG_FILENAME_AGENT_MIGRATE_PROPERTIES_KEY);
                                 
    }

    public static final String LOC_DR_ERR_TAG_SWAP_CONFIG = 
        "DR_ERR_TAG_SWAP_CONFIG";

    public static final String 
        STR_PRODUCT_CONFIG_FILENAME_AGENT_BOOTSTRAP_KEY = 
        "CONFIG_FILENAME_AGENT_BOOTSTRAP";
    
    public static final String 
         STR_PRODUCT_CONFIG_FILENAME_AGENT_BOOTSTRAP_TEMPLATE_KEY = 
        "CONFIG_FILENAME_AGENT_BOOTSTRAP_TEMPLATE";    

    public static final String STR_PRODUCT_CONFIG_FILENAME_AGENT_CONFIG_KEY = 
        "CONFIG_FILENAME_AGENT_CONFIG";

    public static final String 
	STR_PRODUCT_CONFIG_FILENAME_AGENT_MIGRATE_PROPERTIES_KEY = 
        "CONFIG_FILENAME_AGENT_MIGRATE_PROPERTIES";

    public static final String 
        STR_PRODUCT_CONFIG_FILENAME_AGENT_CONFIG_TEMPLATE_KEY =
        "CONFIG_FILENAME_AGENT_CONFIG_TEMPLATE";

    public static final String LOC_TSK_MSG_CONFIGURE_PRODUCT_PROPS_EXECUTE = 
        "TSK_MSG_CONFIGURE_PRODUCT_PROPS_EXECUTE";

    public static final String LOC_TSK_MSG_CONFIGURE_PRODUCT_PROPS_ROLLBACK = 
        "TSK_MSG_CONFIGURE_PRODUCT_PROPS_ROLLBACK";
}
