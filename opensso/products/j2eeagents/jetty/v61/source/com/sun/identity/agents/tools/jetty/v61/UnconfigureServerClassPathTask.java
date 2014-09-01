/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: UnconfigureServerClassPathTask.java,v 1.1 2009/01/21 18:43:58 kanduls Exp $
 */
package com.sun.identity.agents.tools.jetty.v61;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.ITask;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;
import java.util.Map;

public class UnconfigureServerClassPathTask extends ServerClasspathBase
    implements ITask {

    public boolean execute(String name, IStateAccess stateAccess,
        Map properties) {
        Debug.log("UnconfigureServerClassPathTask.execute(): " +
                "start updating classpath.");
        return super.removeClasspath(stateAccess);
    }

    public boolean rollBack(String name, IStateAccess state, Map properties)
        throws InstallException {
        //Nothing to be done while uninstall        
        return true;
    }

    public LocalizedMessage getExecutionMessage(IStateAccess stateAccess,
        Map properties) {
        Object[] args = {JETTY_CLASSPATH_CONF_FILE};
        LocalizedMessage message = LocalizedMessage.get(
                TSK_MSG_UNCONFIGURE_AGENT_CLASSPATH_EXECUTE, STR_JETTY_GROUP,
                args);
        return message;
    }

    public LocalizedMessage getRollBackMessage(IStateAccess stateAccess,
        Map properties) {
        //No roll back while uninstall
        return null;
    }
}

