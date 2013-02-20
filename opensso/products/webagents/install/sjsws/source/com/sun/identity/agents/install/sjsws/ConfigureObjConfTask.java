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
 * objs://opensso.dev.java.net/public/CDDLv1.0.html or
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
 * $Id: ConfigureObjConfTask.java,v 1.2 2008/06/25 05:54:40 qcheng Exp $
 *
 */

package com.sun.identity.agents.install.sjsws;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallConstants;
import com.sun.identity.install.tools.configurator.ITask;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.ConfigUtil;
import com.sun.identity.install.tools.util.FileEditor;
import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.util.DeletePattern;

import java.util.Map;

/**
 * Configures SWS server instance's obj.conf with Agent 
 * entry.
 */

public class ConfigureObjConfTask extends ObjConfBase implements 
    InstallConstants, ITask, IConstants, IConfigKeys {
    
    private static final String LOC_TSK_MSG_CONFIGURE_OBJ_CONF_EXECUTE =
            "TSK_MSG_CONFIGURE_OBJ_CONF_EXECUTE";
    private static final String LOC_TSK_MSG_CONFIGURE_OBJ_CONF_ROLLBACK =
            "TSK_MSG_CONFIGURE_OBJ_CONF_ROLLBACK";
    
    public boolean execute(String name, IStateAccess stateAccess,
            Map properties) throws InstallException {
        return configureObjConf(stateAccess);
    }
    
    public LocalizedMessage getExecutionMessage(IStateAccess stateAccess,
            Map properties) {
        String objConfFile = 
            (String)stateAccess.get(STR_KEY_SWS_OBJ_FILE);
        Object[] args = { objConfFile };
        LocalizedMessage message = LocalizedMessage.get(
                LOC_TSK_MSG_CONFIGURE_OBJ_CONF_EXECUTE,
                STR_SWS_GROUP, args);
        return message;
    }
    
    public LocalizedMessage getRollBackMessage(IStateAccess stateAccess,
            Map properties) {
        String objConfFile = 
            (String)stateAccess.get(STR_KEY_SWS_OBJ_FILE);
        Object[] args = { objConfFile };
        LocalizedMessage message = LocalizedMessage.get(
                LOC_TSK_MSG_CONFIGURE_OBJ_CONF_ROLLBACK,
                STR_SWS_GROUP, args);
        return message;
    }
    
    public boolean rollBack(String name, IStateAccess stateAccess,
            Map properties) throws InstallException {
        return unconfigureObjConf(stateAccess);
    }

}
