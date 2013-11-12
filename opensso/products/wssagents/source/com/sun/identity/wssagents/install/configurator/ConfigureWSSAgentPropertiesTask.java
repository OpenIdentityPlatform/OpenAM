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
 * $Id: ConfigureWSSAgentPropertiesTask.java,v 1.1 2009/06/12 22:03:58 huacui Exp $
 *
 */

package com.sun.identity.agents.install.configurator;

import java.io.File;
import java.util.Map;

import com.sun.identity.install.tools.configurator.ConfigurePropertiesTask;
import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.ConfigUtil;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.ReplaceTokens;

public class ConfigureWSSAgentPropertiesTask 
    extends ConfigurePropertiesTask {

    public boolean execute(String name, IStateAccess stateAccess, 
            Map properties) throws InstallException {
        boolean status = false;
        String productHome = ConfigUtil.getHomePath();
        stateAccess.put(STR_PRODUCT_HOME_DIR_TAG, productHome);

        try {
            Map tokens = stateAccess.getData();
            // Generate the AMConfig.properties file 
            String agentConfigTemplate = (String) properties.get(
                        STR_PRODUCT_CONFIG_WSS_AGENT_CONFIG_TEMPLATE_KEY);
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
                .get(STR_PRODUCT_CONFIG_WSS_AGENT_CONFIG_KEY);
        Object[] args = { productConfigFileName, stateAccess.getInstanceName()
                };
        LocalizedMessage message = LocalizedMessage.get(
                LOC_TSK_MSG_CONFIGURE_PRODUCT_PROPS_EXECUTE, args);
        return message;
    }

    public LocalizedMessage getRollBackMessage(IStateAccess stateAccess,
            Map properties) {
        String productConfigFileName = (String) properties
                .get(STR_PRODUCT_CONFIG_WSS_AGENT_CONFIG_KEY);
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

        String agentConfigFile = getAgentConfigFile(stateAccess,
                                                    properties);
        file = new File(agentConfigFile);
        status = file.delete();

        printStatus = (status) ? "Successful." : "FAILED.";
        Debug.log("ConfigurePropertiesTask.rollBack() - Deleting file '"
                + agentConfigFile + "'. " + printStatus);

        return status;
    }

    private String getAgentConfigFile(IStateAccess stateAccess,
            Map properties) {
        return (String) stateAccess.get(STR_CONFIG_DIR_PREFIX_TAG) + FILE_SEP
                + (String) properties.get(
                 STR_PRODUCT_CONFIG_WSS_AGENT_CONFIG_KEY);
    }

    public static final String STR_PRODUCT_CONFIG_WSS_AGENT_CONFIG_KEY = 
        "CONFIG_WSS_AGENT_CONFIG";

    public static final String 
        STR_PRODUCT_CONFIG_WSS_AGENT_CONFIG_TEMPLATE_KEY =
        "CONFIG_WSS_AGENT_CONFIG_TEMPLATE";

    public static final String STR_PRODUCT_HOME_DIR_TAG = "BASE_DIR";
}
