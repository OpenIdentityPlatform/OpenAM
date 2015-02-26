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
 * $Id: ConfigureServerPolicyTask.java,v 1.1 2008/12/11 14:36:06 naghaon Exp $
 *
 */

package com.sun.identity.agents.tools.jboss;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.ITask;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;
import java.util.Map;

public class ConfigureServerPolicyTask extends ServerPolicyBase
        implements ITask {
    
    public static final String LOC_TSK_MSG_CONFIGURE_SERVER_POLICY_EXECUTE =
            "TSK_MSG_CONFIGURE_SERVER_POLICY_EXECUTE";
    public static final String LOC_TSK_MSG_CONFIGURE_SERVER_POLICY_ROLLBACK =
            "TSK_MSG_CONFIGURE_SERVER_POLICY_ROLLBACK";
    
    public boolean execute(String name, IStateAccess stateAccess,
            Map properties) throws InstallException {
        boolean status = false;
        boolean modifyServerPolicyTask = modifyServerPolicyTask(stateAccess);

        if (modifyServerPolicyTask) {
            status = addToServerPolicy(stateAccess);
        } else {
            Debug.log("Skipping ConfigureServerPolicyTask.execute()");
            status = true;
        }

        return status;
    }
    
    public LocalizedMessage getExecutionMessage(IStateAccess stateAccess,
            Map properties) {
        String serverPolicyFile = getServerPolicyFile(stateAccess);
        Object[] args = { serverPolicyFile };
        LocalizedMessage message = LocalizedMessage.get(
                LOC_TSK_MSG_CONFIGURE_SERVER_POLICY_EXECUTE,
                STR_JB_GROUP, args);
        return message;
    }
    
    public LocalizedMessage getRollBackMessage(IStateAccess stateAccess,
            Map properties) {
        String serverPolicyFile = getServerPolicyFile(stateAccess);
        Object[] args = { serverPolicyFile };
        LocalizedMessage message = LocalizedMessage.get(
                LOC_TSK_MSG_CONFIGURE_SERVER_POLICY_ROLLBACK,
                STR_JB_GROUP, args);
        return message;
    }
    
    public boolean rollBack(String name, IStateAccess stateAccess,
            Map properties) throws InstallException {
        boolean status = false;
        boolean modifyServerPolicyTask = modifyServerPolicyTask(stateAccess);

        if (modifyServerPolicyTask) {
            status = removeFromServerPolicy(stateAccess);
        } else {
            Debug.log("Skipping ConfigureServerPolicyTask.rollback()");
            status = true;
        }
        
        return status;
    }

    private boolean modifyServerPolicyTask(IStateAccess stateAccess) {
        boolean result = false;
        String modifyServerPolicyFile = (String) 
                       stateAccess.get(STR_KEY_JB_MODIFY_SERVER_POLICY_FILE);

        if (modifyServerPolicyFile != null) {
            result = Boolean.valueOf(modifyServerPolicyFile).booleanValue();
            Debug.log("ConfigureServerPolicyTask: modifyServerPolicyTask() = " + 
                modifyServerPolicyFile);
        }

        return result;
    }

}
