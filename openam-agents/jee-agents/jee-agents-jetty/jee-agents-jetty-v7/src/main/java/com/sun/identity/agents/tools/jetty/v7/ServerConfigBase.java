/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: ServerConfigBase.java,v 1.1 2009/01/21 18:43:56 kanduls Exp $
 */

/**
 * Portions Copyrighted 2013 ForgeRock AS.
 */

package com.sun.identity.agents.tools.jetty.v7;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.util.ConfigUtil;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.FileUtils;
import java.io.File;
import java.util.Arrays;

public class ServerConfigBase implements IConfigKeys, IConstants {
    
    public ServerConfigBase() {
        
    }
    
    public boolean copyAgentLoginConf(IStateAccess stateAccess) {

        boolean status = false;
        String jettyConfigDir = (String) stateAccess.get(STR_KEY_JETTY_SERVER_CONFIG_DIR);
        String jettyLoginConfFile = jettyConfigDir + STR_FORWARD_SLASH + LOGIN_CONF_FILE;
        String loginConfFile = ConfigUtil.getConfigDirPath() + STR_FORWARD_SLASH + LOGIN_CONF_FILE;
        try {
            status = FileUtils.copyFile(loginConfFile, jettyLoginConfFile);
        } catch (Exception ex) {
            Debug.log("ServerConfigBase.copyAgentLoginConf(): Error copying " +
                    loginConfFile + " to " + jettyLoginConfFile + " " + ex.getMessage());
        }
        return status;
    }
    
    public boolean copyAgentLoginXml(IStateAccess stateAccess) {

        boolean status = false;
        String jettyConfigDir = (String) stateAccess.get(STR_KEY_JETTY_SERVER_CONFIG_DIR);
        String jettyLoginXmlFile = jettyConfigDir + STR_FORWARD_SLASH + LOGIN_XML_FILE;
        String loginXmlFile = ConfigUtil.getConfigDirPath() + STR_FORWARD_SLASH + LOGIN_XML_FILE;
        try {
            status = FileUtils.copyFile(loginXmlFile, jettyLoginXmlFile);
        } catch (Exception ex) {
            Debug.log("ServerConfigBase.copyAgentLoginXml(): Error copying " +
                    loginXmlFile + " to " + jettyLoginXmlFile + " " +
                    ex.getMessage());
        }

        return status;
    }

    public boolean appendAgentRealmConfig(IStateAccess stateAccess) {

        String jettyHomeDir = (String) stateAccess.get(STR_KEY_JETTY_HOME_DIR);
        String startIniFile = jettyHomeDir + STR_FORWARD_SLASH + JETTY_START_INI;

        boolean status = FileUtils.appendLinesToFile(startIniFile, LOGIN_REALM_CONF);
        if (!status) {
            Debug.log("ServerConfigBase.appendAgentRealmConfig(): Error appending " +
                Arrays.asList(LOGIN_REALM_CONF) + " to " + startIniFile);
        }

        return status;
    }

    public boolean deleteAgentLoginConf(IStateAccess stateAccess) {

        String jettyConfigDir = (String) stateAccess.get(STR_KEY_JETTY_SERVER_CONFIG_DIR);
        String jettyLoginConfFile = jettyConfigDir + STR_FORWARD_SLASH + LOGIN_CONF_FILE;
        File delFile = new File(jettyLoginConfFile);
        boolean status = delFile.delete();

        return status;
    }

    public boolean deleteAgentLoginXml(IStateAccess stateAccess) {

        String jettyConfigDir = (String) stateAccess.get(STR_KEY_JETTY_SERVER_CONFIG_DIR);
        String jettyLoginConfFile = jettyConfigDir + STR_FORWARD_SLASH + LOGIN_XML_FILE;
        File delFile = new File(jettyLoginConfFile);
        boolean status = delFile.delete();

        return status;
    }

    public boolean deleteAgentRealmConfig(IStateAccess stateAccess) {

        boolean status = false;

        String jettyHomeDir = (String) stateAccess.get(STR_KEY_JETTY_HOME_DIR);
        String startIniFile = jettyHomeDir + STR_FORWARD_SLASH + JETTY_START_INI;
        String[] appendedValues = LOGIN_REALM_CONF;

        // Use the first unique line
        int firstOccurence =
                FileUtils.getFirstOccurence(startIniFile, appendedValues[LOGIN_REALM_CONF_UNIQUE], true, false, false);
        if (firstOccurence > 0) {
           status = FileUtils.removeLinesByNum(startIniFile,
                   firstOccurence - LOGIN_REALM_CONF_UNIQUE, appendedValues.length);
        }

        if (!status) {
            Debug.log("ServerConfigBase.deleteAgentRealmConfig(): Error removing appended lines starting at "
                    + firstOccurence + " from " + startIniFile);
        }

        return status;
    }
}
