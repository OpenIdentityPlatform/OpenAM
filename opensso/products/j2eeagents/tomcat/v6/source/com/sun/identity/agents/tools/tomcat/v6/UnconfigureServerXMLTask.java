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
 * $Id: UnconfigureServerXMLTask.java,v 1.2 2008/11/28 12:36:23 saueree Exp $
 */

package com.sun.identity.agents.tools.tomcat.v6;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.ITask;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.util.LocalizedMessage;

import java.util.Map;


public class UnconfigureServerXMLTask extends ServerXMLBase
    implements ITask {
    public static final String LOC_TSK_MSG_UNCONFIGURE_SERVER_XML_EXECUTE =
    	"TSK_MSG_UNCONFIGURE_SERVER_XML_EXECUTE";

    public boolean execute(
        String name,
        IStateAccess stateAccess,
        Map properties) throws InstallException {
        boolean status = false;
        status = super.unconfigureServerXML(stateAccess);

        return status;
    }

    public LocalizedMessage getExecutionMessage(
        IStateAccess stateAccess,
        Map properties) {
        String serverXMLFile = getServerXMLFile(stateAccess);
        Object[] args = { serverXMLFile };
        LocalizedMessage message = LocalizedMessage.get(
                LOC_TSK_MSG_UNCONFIGURE_SERVER_XML_EXECUTE,
                STR_TOMCAT_GROUP,
                args);

        return message;
    }

    public LocalizedMessage getRollBackMessage(
        IStateAccess stateAccess,
        Map properties) {
        // No roll back during un-install
        return null;
    }

    public boolean rollBack(
        String name,
        IStateAccess state,
        Map properties) throws InstallException {
        // Nothing to roll back during un-install
        return true;
    }
}
