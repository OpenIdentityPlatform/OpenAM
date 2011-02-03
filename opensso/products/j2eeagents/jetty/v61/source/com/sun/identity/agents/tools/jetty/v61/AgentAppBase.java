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
 * $Id: AgentAppBase.java,v 1.1 2009/01/21 18:43:52 kanduls Exp $
 *
 */


package com.sun.identity.agents.tools.jetty.v61;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.util.ConfigUtil;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.FileUtils;
import java.io.File;


public class AgentAppBase implements IConstants, IConfigKeys {
    
    protected boolean copyAgentAppWarFile(IStateAccess stateAccess) {
        boolean status = false;
        String srcDir = ConfigUtil.getEtcDirPath();
        String destDir = (String)stateAccess.get(STR_KEY_JETTY_INST_DEPLOY_DIR);
        
        try {
            FileUtils.copyJarFile(srcDir, destDir, STR_AGENT_APP_WAR_FILE);
            Debug.log("ConfigureAgentAppTask.copyAgentAppWarFile() - copy "
                    + STR_AGENT_APP_WAR_FILE + " from " + srcDir + " to "
                    + destDir);
            status = true;
        } catch (Exception e) {
            Debug.log("ConfigureAgentAppTask." +
                    "copyAgentAppWarFile() - Error occured while copying "
                    + STR_AGENT_APP_WAR_FILE
                    + " from "
                    + srcDir
                    + " to " + destDir, e);
        }
        return status;
    }
    
    protected boolean removeAgentAppWar(IStateAccess stateAccess) {
        boolean status = false;
        String destDir = (String)stateAccess.get(STR_KEY_JETTY_INST_DEPLOY_DIR);
        String agentAppWar = destDir + STR_FORWARD_SLASH +
                STR_AGENT_APP_WAR_FILE;
        try {
            File file = new File(agentAppWar);
            status = file.delete();
            Debug.log("ConfigureAgentAppTask.removeAgentAppWar(): "
                + " Removed file " + agentAppWar);
        } catch (Exception e) {
            Debug.log("ConfigureAgentAppTask.removeAgentAppWar(): "
                + " Failed to remove file " + agentAppWar, e);
        }
        return status;
    }
}

