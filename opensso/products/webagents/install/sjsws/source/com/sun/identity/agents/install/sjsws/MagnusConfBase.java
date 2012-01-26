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
 * $Id: MagnusConfBase.java,v 1.2 2008/06/25 05:54:40 qcheng Exp $
 *
 */

package com.sun.identity.agents.install.sjsws;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallConstants;
import com.sun.identity.install.tools.configurator.ITask;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.util.ConfigUtil;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.ConfigUtil;
import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.util.OSChecker;

import java.util.Map;

/**
 * Configures SWS server instance's magnus.conf
 */

public class MagnusConfBase implements 
    InstallConstants, IConstants, IConfigKeys {
    
    protected boolean configureMagnusConf(IStateAccess stateAccess) {
        boolean status = false;
        String magnusFile = 
            (String)stateAccess.get(STR_KEY_SWS_MAGNUS_FILE);
        String agentBin = ConfigUtil.getBinDirPath() + FILE_SEP;
        String agentLib = ConfigUtil.getLibPath() + FILE_SEP;
        String agentConfigDir = ConfigUtil.getHomePath() + 
                                FILE_SEP +
                                stateAccess.getInstanceName() +
                                FILE_SEP +
                                STR_INSTANCE_CONFIG_DIR_NAME ;

        if (OSChecker.isWindows()) {
            agentLib = agentBin + "ames6.dll";
        } else {
            agentLib = agentLib + "libames6.so";
        }

        try {
            int i = FileUtils.getFirstOccurence(
                magnusFile, "Init fn=\"load-modules", true, false, false, 0);

            StringBuffer line =
                new StringBuffer("Init fn=\"load-modules\" shlib=\"");
            line.append(agentLib.replace('\\', '/'))
                .append("\" funcs=\"web_agent_init,validate_session_policy,")
                .append("append_post_data,process_notification\"");
            FileUtils.insertLineByNumber(magnusFile, i +1, line.toString());

            line.setLength(0);
            line.append("Init fn=\"web_agent_init\" dsameconfdir=\"")
                .append(agentConfigDir.replace('\\', '/'))
                .append("\" LateInit=\"yes\"");
            FileUtils.insertLineByNumber(magnusFile, i +2, line.toString());

            FileUtils.insertLineByNumber(magnusFile,
                i +3, "Init fn=\"pool-init\" disable=\"false\"");

            status = true;
        } catch (Exception exc) {
            Debug.log("MagnusConfBase.configureMagnusConf() - " +
                    "Exception occurred while adding the Agent entry from " +
                    "file '" + magnusFile + "'. ", exc);
        }

        return status;
    }

    protected boolean unconfigureMagnusConf(IStateAccess stateAccess) {
        boolean status = false;
        String magnusFile = 
            (String)stateAccess.get(STR_KEY_SWS_MAGNUS_FILE);

        try {
            int i = FileUtils.getFirstOccurence(
                magnusFile, "funcs=\"web_agent_init,", false, false, false, 0);


            if (i != -1) {
                FileUtils.removeLinesByNum(magnusFile, i, 1);
            }

            i = FileUtils.getFirstOccurence(
                    magnusFile, "Init fn=\"web_agent_init", 
                    true, false, false, 0);
            if (i != -1) {
                FileUtils.removeLinesByNum(magnusFile, i, 1);
            }

            i = FileUtils.getFirstOccurence(
                    magnusFile, "Init fn=\"pool-init\" disable=\"false\"", 
                    true, false, false, 0);
            if (i != -1) {
                FileUtils.removeLinesByNum(magnusFile, i, 1);
            }

            status = true;
        } catch (Exception exc) {
            Debug.log("MagnusConfBase.unconfigureMagnusConf() - " +
                    "Exception occurred while removing the Agent entry from " +
                    "file '" + magnusFile + "'. ", exc);
        }
        return status;
    }

}
