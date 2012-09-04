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
 * $Id: RemoveDsameFileTask.java,v 1.1 2009/11/04 22:09:38 leiming Exp $
 *
 */
package com.sun.identity.agents.install.domino;

import java.io.File;
import java.util.Map;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.ITask;
import com.sun.identity.install.tools.configurator.InstallConstants;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;



/**
 * Remove dsame.conf under IBM Lotus Domino server's data directory.
 */
public class RemoveDsameFileTask
        implements ITask, IConstants, InstallConstants, IConfigKeys {

    private static final String LOC_TSK_MSG_CONFIGURE_DSAME_FILE_ROLLBACK =
            "TSK_MSG_CONFIGURE_DSAME_FILE_ROLLBACK";


    /**
     * Remove dsame.conf file in IBM Lotus Domino's config directory.
     * @param name 
     * @param stateAccess 
     * @param properties 
     * @throws com.sun.identity.install.tools.configurator.InstallException 
     * @return true or false status of the task
     */
    public boolean execute(String name, IStateAccess stateAccess,
            Map properties) throws InstallException {

        boolean status = true;
        String confFile = getDsameConfFile(stateAccess);
        Debug.log("RemoveDsameFileTask.execute()() - " + confFile);
        
        if (! new File(confFile).delete()) {
            Debug.log("RemoveDsameFileTask.execute() - " +
                    "Unable to delete file: " +
                    confFile);
            status = false;
        }

        return status;
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
                LOC_TSK_MSG_CONFIGURE_DSAME_FILE_ROLLBACK,
                STR_DOMINO_GROUP, args);
    }

    /**
     * There no rollback for removing dsame.conf.
     * @param stateAccess 
     * @param properties 
     * @return 
     */
    public LocalizedMessage getRollBackMessage(IStateAccess stateAccess,
            Map properties) {
        return null;
    }

    /**
     * There no rollback for removing dsame.conf.
     * @param name 
     * @param stateAccess 
     * @param properties 
     * @throws com.sun.identity.install.tools.configurator.InstallException 
     * @return 
     */
    public boolean rollBack(String name, IStateAccess stateAccess,
            Map properties) throws InstallException {
        return true;
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
