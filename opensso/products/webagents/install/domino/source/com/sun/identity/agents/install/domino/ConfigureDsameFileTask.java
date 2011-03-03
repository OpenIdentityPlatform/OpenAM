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
 * $Id: ConfigureDsameFileTask.java,v 1.1 2009/11/04 22:09:38 leiming Exp $
 *
 */
package com.sun.identity.agents.install.domino;

import java.io.File;
import java.util.Map;
import java.util.HashMap;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.ITask;
import com.sun.identity.install.tools.configurator.InstallConstants;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.ConfigUtil;
import com.sun.identity.install.tools.util.ReplaceTokens;



/**
 * Copies dsame.conf to IBM Lotus Domino server's data directory.
 * dsame.conf's tags gets replaced with agent instance specific
 * information.
 */
public class ConfigureDsameFileTask
        implements ITask, IConstants, InstallConstants, IConfigKeys {

    private static final String LOC_TSK_MSG_CONFIGURE_DSAME_FILE_EXECUTE =
            "TSK_MSG_CONFIGURE_DSAME_FILE_EXECUTE";
    private static final String LOC_TSK_MSG_CONFIGURE_DSAME_FILE_ROLLBACK =
            "TSK_MSG_CONFIGURE_DSAME_FILE_ROLLBACK";
    private static final String LOC_TSK_MSG_CONFIGURE_DSAME_FILE_FAIL =
            "TSK_MSG_CONFIGURE_DSAME_FILE_FAIL";


    /**
     * 
     * @param name 
     * @param stateAccess 
     * @param properties 
     * @throws com.sun.identity.install.tools.configurator.InstallException 
     * @return true or false status of the task
     */
    public boolean execute(String name, IStateAccess stateAccess,
            Map properties) throws InstallException {

        return copyDsameFile(stateAccess, properties);
    }

    /**
     * 
     * @param stateAccess 
     * @param properties 
     * @return 
     */
    public LocalizedMessage getExecutionMessage(IStateAccess stateAccess,
            Map properties) {
        Object[] args = {getDsameConfFile(stateAccess)};
        return LocalizedMessage.get(
                LOC_TSK_MSG_CONFIGURE_DSAME_FILE_EXECUTE,
                STR_DOMINO_GROUP, args);
    }

    /**
     * 
     * @param stateAccess 
     * @param properties 
     * @return 
     */
    public LocalizedMessage getRollBackMessage(IStateAccess stateAccess,
            Map properties) {
        Object[] args = {getDsameConfFile(stateAccess)};
        return LocalizedMessage.get(
                LOC_TSK_MSG_CONFIGURE_DSAME_FILE_ROLLBACK,
                STR_DOMINO_GROUP, args);
    }

    /**
     * 
     * @param name 
     * @param stateAccess 
     * @param properties 
     * @throws com.sun.identity.install.tools.configurator.InstallException 
     * @return 
     */
    public boolean rollBack(String name, IStateAccess stateAccess,
            Map properties) throws InstallException {
        String confFile = getDsameConfFile(stateAccess);
        new File(confFile).delete();
        return true;
    }

    /**
     * Tag swaps etc/dsame.template and copies to IBM Lotus Domino Server's data
     * directory as dsame.conf. 
     * @param stateAccess 
     * @param properties 
     * @return 
     */
    protected boolean copyDsameFile(IStateAccess stateAccess,
            Map properties) throws InstallException {

        boolean status = false;
        String srcFile = null;
        String destFile = null;
        try {
            // gets dsame.template file from etc directory and gets
            // a file called dsame.conf
            srcFile = getDsameConfTemplateFile(stateAccess);
            destFile = getDsameConfFile(stateAccess);

            // Tags get swapped with agent instance's config
            // files' information.
            Map tokens = new HashMap();
            tokens.put("AGENTBOOTSTRAP",
                    stateAccess.get(STR_CONFIG_FILE_PATH_TAG));
            tokens.put("AGENTCONFIG",
                    stateAccess.get(STR_CONFIG_AGENT_CONFIG_FILE_PATH_TAG));

            // Tag swapping is done here into dsame.conf
            ReplaceTokens filter = new ReplaceTokens(srcFile, destFile, tokens);
            filter.tagSwapAndCopyFile();
           
            Debug.log("ConfigureDsameFileTask.copyDsameFile() - " +
                    srcFile + " : " + destFile);

            status = true;
        } catch (Exception e) {
            Debug.log("ConfigureDsameFileTask." +
                    "copyDsameFile() - Failed to copy  " + srcFile + " : " +
                    destFile, e);
            throw new InstallException(
                    LocalizedMessage.get(LOC_TSK_MSG_CONFIGURE_DSAME_FILE_FAIL,
                    STR_DOMINO_GROUP));

        }
        return status;
    }

    private String getDsameConfTemplateFile(IStateAccess stateAccess)
            throws Exception {

        String srcFile;
        srcFile = ConfigUtil.getEtcDirPath() + FILE_SEP +
                STR_DSAME_FILE_TEMPLATE;
        return srcFile;
    }

    private String getDsameConfFile(IStateAccess stateAccess) {
        // Domino config dir.
        String homeDir = (String)stateAccess.get(STR_KEY_DOMINO_INST_CONF_DIR);
        // agent instance's dsame.conf
        StringBuffer buffer = new StringBuffer();
        buffer.append(homeDir).append(FILE_SEP);
        buffer.append(STR_DSAME_CONF_FILE);

        return buffer.toString();
    }
}
