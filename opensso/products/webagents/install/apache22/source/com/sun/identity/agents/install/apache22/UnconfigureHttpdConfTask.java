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
 * $Id: UnconfigureHttpdConfTask.java,v 1.2 2008/06/25 05:54:35 qcheng Exp $
 *
 */

package com.sun.identity.agents.install.apache22;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.ITask;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;

import java.util.Map;

/**
 * Unconfigures Apache instance's httpd.conf. dsame.conf entry gets
 * removed from httpd.conf file.
 */
public class UnconfigureHttpdConfTask extends HttpdConfBase
        implements ITask {
    
    private static final String LOC_TSK_MSG_UNCONFIGURE_HTTPD_CONF_EXECUTE =
            "TSK_MSG_UNCONFIGURE_HTTPD_CONF_EXECUTE";
    
    /**
     * Unconfigures httpd.conf file.
     * @param name 
     * @param stateAccess 
     * @param properties 
     * @throws com.sun.identity.install.tools.configurator.InstallException 
     * @return true or false status of the task
     */
    public boolean execute(String name, IStateAccess stateAccess,
            Map properties) throws InstallException {
        return removeFromHttpdConf(stateAccess);
    }
    
    public LocalizedMessage getExecutionMessage(IStateAccess stateAccess,
            Map properties) {
        String httpdConfFile = 
            (String)stateAccess.get(STR_KEY_APC22_HTTPD_FILE);
        Object[] args = { httpdConfFile };
        return LocalizedMessage.get(
                LOC_TSK_MSG_UNCONFIGURE_HTTPD_CONF_EXECUTE,
                STR_APC22_GROUP, args);
    }
    
    public LocalizedMessage getRollBackMessage(IStateAccess stateAccess,
            Map properties) {
        // No roll back during un-install
        return null;
    }
    
    public boolean rollBack(String name, IStateAccess state, Map properties)
        throws InstallException {
        // Nothing to roll back during un-install
        return true;
    }
}
