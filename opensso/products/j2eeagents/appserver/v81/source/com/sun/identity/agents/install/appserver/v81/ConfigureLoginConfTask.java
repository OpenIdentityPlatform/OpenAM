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
 * $Id: ConfigureLoginConfTask.java,v 1.3 2008/06/25 05:52:11 qcheng Exp $
 *
 */

package com.sun.identity.agents.install.appserver.v81;

import java.util.Map;

import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.agents.install.appserver.ConfigureLoginConfTaskBase;
import com.sun.identity.agents.install.appserver.IConfigKeys;

/**
 * The class used by installer to make changes in login.conf file of 
 * the Sun App Server
 */
public class ConfigureLoginConfTask extends ConfigureLoginConfTaskBase 
    implements IConfigKeys, IConstants {

    public ConfigureLoginConfTask() {
        super();
    }

    public boolean execute(String name, IStateAccess stateAccess,
        Map properties) throws InstallException {
        boolean status = false;
        boolean skipTask = skipTask(stateAccess);

        if (skipTask) {
           Debug.log("Skipping ConfigureLoginConfTask.execute()");
           status = true;
        } else {
            status = appendToFile(stateAccess);
        }
        return status;
    }

    public boolean rollBack(String name, IStateAccess stateAccess,
        Map properties) throws InstallException {
        boolean status = false;
        boolean skipTask = skipTask(stateAccess);

        if (skipTask) {
            Debug.log("Skipping ConfigureLoginConfTask.rollBack()");
            status = true;
        } else {
            status = removeFromFile(stateAccess);
        }

        return status;
    }
            
    public String getLoginModuleClassName() {
        return STR_LOGIN_MODULE_CLASS_NAME;
    }         

    private boolean skipTask(IStateAccess stateAccess) {
        boolean result = false;
        String isRemote = (String) stateAccess.get(STR_DAS_HOST_IS_REMOTE_KEY);

        if ( isRemote != null) {
            result = Boolean.valueOf(isRemote).booleanValue();
            Debug.log("ConfigureLoginConfTask: skipTask() = " + isRemote);
        }

        return result;
    }

}
