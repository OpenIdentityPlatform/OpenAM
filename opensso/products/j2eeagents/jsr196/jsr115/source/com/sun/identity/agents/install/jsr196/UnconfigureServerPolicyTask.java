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
 * $Id: UnconfigureServerPolicyTask.java,v 1.1 2009/01/30 12:09:39 kalpanakm Exp $
 *
 */

package com.sun.identity.agents.install.jsr196;

import java.util.Map;

import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.agents.install.jsr196.RemoveJavaPermissionsTask;
import com.sun.identity.agents.install.jsr196.IConfigKeys;

/** 
 * The classed used by the uninstaller to make changes in server.policy file
 * of the Sun App Server
 */
public class UnconfigureServerPolicyTask extends RemoveJavaPermissionsTask 
		implements IConfigKeys
{
    
    public boolean execute(String name, IStateAccess stateAccess, 
        Map properties) throws InstallException {
	boolean status = false;
	boolean skipTask = skipTask(stateAccess);

	if (skipTask) {
	    Debug.log("Skipping UnconfigureServerPolicyTask.execute()");
	    status = true;
	} else {
            preUnconfigureTasks(stateAccess);
            
            status = removeFromServerPolicy(stateAccess);
            
            postUnconfigureTasks(stateAccess);
	}

        return status;
    }

     protected void preUnconfigureTasks(IStateAccess stateAccess) {
     }
     
     protected void postUnconfigureTasks(IStateAccess stateAccess) {
     }
     
    private boolean skipTask(IStateAccess stateAccess) {
        boolean result = false;
        String isRemote = (String) stateAccess.get(STR_DAS_HOST_IS_REMOTE_KEY);

        if ( isRemote != null) {
            result = Boolean.valueOf(isRemote).booleanValue();
            Debug.log("UnconfigureServerPolicyTask: skipTask = " + isRemote);
        }

        return result;
    }

}
