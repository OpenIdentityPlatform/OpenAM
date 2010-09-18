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

package com.sun.identity.agents.tools.jetty.v61;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.util.ConfigUtil;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.FileUtils;
import java.io.File;

public class ServerConfigBase implements IConfigKeys, IConstants {
    
    public ServerConfigBase() {
        
    }
    
    public boolean copyAgentLoginConf(IStateAccess stateAccess) {
        boolean status = false;
        String jettyConfigDir = 
                (String) stateAccess.get(STR_KEY_JETTY_SERVER_CONFIG_DIR);
        String jettyLoginConfFile = jettyConfigDir + STR_FORWARD_SLASH + 
                LOGIN_CONF_FILE;
        String loginConfFile = ConfigUtil.getConfigDirPath() + 
                STR_FORWARD_SLASH + LOGIN_CONF_FILE;
        try {
            status = FileUtils.copyFile(loginConfFile, jettyLoginConfFile);
        } catch (Exception ex) {
            Debug.log("ServerConfigBase.copyAgentLoginConf(): Error copying " +
                    loginConfFile + " to " + jettyLoginConfFile + " " + 
                    ex.getMessage());
        }
        return status;
    }
    
    public boolean deleteAgentLoginConf(IStateAccess stateAccess) {
        boolean status = false;
        String jettyConfigDir = 
                (String) stateAccess.get(STR_KEY_JETTY_SERVER_CONFIG_DIR);
        String jettyLoginConfFile = jettyConfigDir + STR_FORWARD_SLASH + 
                LOGIN_CONF_FILE;
        File delFile = new File(jettyLoginConfFile);
        status = delFile.delete();
        return status;
    }
}
