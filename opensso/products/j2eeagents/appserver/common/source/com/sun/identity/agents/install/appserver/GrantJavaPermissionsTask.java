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
 * $Id: GrantJavaPermissionsTask.java,v 1.2 2008/06/25 05:52:02 qcheng Exp $
 *
 */

package com.sun.identity.agents.install.appserver;

import java.util.Map;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.ITask;
import com.sun.identity.install.tools.configurator.InstallConstants;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.util.LocalizedMessage;


/**
 * The class grants Java Permissions for Agent
 */
public class GrantJavaPermissionsTask extends JavaPermissionsBase 
    implements ITask, InstallConstants 
{   
    public GrantJavaPermissionsTask() {
        super();
    }

    public boolean execute(String name, IStateAccess stateAccess, 
        Map properties) throws InstallException 
    {    
        return addToServerPolicy(stateAccess);
    }
 
    public LocalizedMessage getExecutionMessage(IStateAccess stateAccess,
        Map properties) 
    {
        String serverPolicyFile = getServerPolicyFile(stateAccess); 
        Object[] args = { serverPolicyFile };
        LocalizedMessage message = LocalizedMessage.get(
            LOC_TSK_MSG_GRANT_JAVA_PERMS_EXECUTE, STR_AS_GROUP, args);
        return message;
    }

    public LocalizedMessage getRollBackMessage(IStateAccess stateAccess,
        Map properties) 
    {
        String serverPolicyFile = getServerPolicyFile(stateAccess); 
        Object[] args = { serverPolicyFile };
        LocalizedMessage message = LocalizedMessage.get(
            LOC_TSK_MSG_GRANT_JAVA_PERMS_ROLLBACK, STR_AS_GROUP, args);
        return message;
    }

    public boolean rollBack(String name, IStateAccess stateAccess, 
        Map properties) throws InstallException 
    {           
        return removeFromServerPolicy(stateAccess);
    }
    
    public static final String LOC_TSK_MSG_GRANT_JAVA_PERMS_EXECUTE = 
        "TSK_MSG_GRANT_JAVA_PERMS_EXECUTE";
    public static final String LOC_TSK_MSG_GRANT_JAVA_PERMS_ROLLBACK = 
        "TSK_MSG_GRANT_JAVA_PERMS_ROLLBACK";    
    
    private static final String STR_AS_GROUP = "asTools";
}
