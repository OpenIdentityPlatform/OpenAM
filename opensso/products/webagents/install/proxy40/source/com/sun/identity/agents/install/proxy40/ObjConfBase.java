/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ObjConfBase.java,v 1.2 2009/03/05 23:28:21 subbae Exp $
 *
 */

package com.sun.identity.agents.install.proxy40;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallConstants;
import com.sun.identity.install.tools.configurator.ITask;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.ConfigUtil;
import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.util.OSChecker;

import java.util.Map;

/**
 * Configures SPS server instance's obj.conf
 */

public class ObjConfBase implements 
    InstallConstants, IConstants, IConfigKeys {
    
    protected boolean configureObjConf(IStateAccess stateAccess) {
        boolean status = false;
        String agentBin = ConfigUtil.getBinDirPath() + FILE_SEP;
        String agentLib = ConfigUtil.getLibPath() + FILE_SEP ;
        String objFile = 
            (String)stateAccess.get(STR_KEY_PROXY40_OBJ_FILE);
        String agentConfigDir = ConfigUtil.getHomePath() +
                                FILE_SEP +
                                stateAccess.getInstanceName() +
                                FILE_SEP +
                                STR_INSTANCE_CONFIG_DIR_NAME ;

        if (OSChecker.isWindows()) {
            agentLib = agentBin + "ampxy4.dll";
        } else {
            agentLib = agentLib + "libampxy4.so";
        }

        try {

            int line_num = FileUtils.getLastOccurence(
                objFile, "Init fn", true, false, false);

            StringBuffer line =
                new StringBuffer("Init fn=\"load-modules\" shlib=\"");
            line.append(agentLib.replace('\\', '/'))
                .append("\" funcs=\"web_agent_init,validate_session_policy,")
                .append("process_notification,add_agent_header\"");
            FileUtils.insertLineByNumber(objFile, line_num +1, line.toString());

            line.setLength(0);
            line.append("Init fn=\"web_agent_init\" dsameconfdir=\"")
                .append(agentConfigDir.replace('\\', '/'))
                .append("\" LateInit=\"yes\"");
            FileUtils.insertLineByNumber(objFile, line_num +2, line.toString());

            int i = FileUtils.getFirstOccurence(
                objFile, "AuthTrans fn=\"match-browser\"", true, false, false, 0);

            FileUtils.insertLineByNumber(objFile, i +1, "NameTrans fn=add_agent_header");

            int line_wo_comma = FileUtils.getLastOccurence(
                objFile, "PathCheck fn=url-check", true, false, false);

            int line_w_comma = FileUtils.getLastOccurence(
                objFile, "PathCheck fn=\"url-check\"", true, false, false);

            if ( line_wo_comma != -1 ) {
                FileUtils.insertLineByNumber(
                    objFile, line_wo_comma +1, "PathCheck fn=validate_session_policy");
            } else if ( line_w_comma != -1 ) {
                FileUtils.insertLineByNumber(
                    objFile, line_w_comma +1, "PathCheck fn=validate_session_policy");
            }

            // Add service function
            FileUtils.appendDataToFile(objFile,
                "<Object ppath=\".*/UpdateAgentCacheServlet*\">");
            FileUtils.appendDataToFile(objFile,
                "Service fn=\"process_notification\"");
            FileUtils.appendDataToFile(objFile,"</Object>");

            status = true;

        } catch (Exception exc) {
            Debug.log("ObjConfBase.configureObjConf() - " +
                    "Exception occurred while adding the Agent entry from " +
                    "file '" + objFile + "'. ", exc);

        }

        return status;
    }

    protected boolean unconfigureObjConf(IStateAccess stateAccess) {
        boolean status = false;
        String objFile = 
            (String)stateAccess.get(STR_KEY_PROXY40_OBJ_FILE);

        try {

            int i = FileUtils.getFirstOccurence(
                objFile, "funcs=\"web_agent_init,", false, false, false, 0);

            if (i != -1) {
                FileUtils.removeLinesByNum(objFile, i, 1);
            }

            i = FileUtils.getFirstOccurence(
                    objFile, "Init fn=\"web_agent_init",
                    true, false, false, 0);

            if (i != -1) {
                FileUtils.removeLinesByNum(objFile, i, 1);
            }

            int j = FileUtils.getFirstOccurence(
                objFile, "PathCheck fn=validate_session_policy",
                true, false, false);

            if (j > 0) {
                FileUtils.removeLinesByNum(objFile, j, 1);
            }

            j = FileUtils.getFirstOccurence(
                objFile, "NameTrans fn=add_agent_header",
                true, false, false);

            if (j > 0) {
                FileUtils.removeLinesByNum(objFile, j, 1);
            }

            j = FileUtils.getFirstOccurence(objFile,
                "<Object ppath=\".*/UpdateAgentCacheServlet*\">",
                false, false, false, 0);

            if (j > 0) {
                FileUtils.removeLinesByNum(objFile, j, 3);
            }

            status = true;

        } catch (Exception exc) {
            Debug.log("ObjConfBase.unconfigureObjConf() - " +
                    "Exception occurred while adding the Agent entry from " +
                    "file '" + objFile + "'. ", exc);
        }
        return status;
    }

}
