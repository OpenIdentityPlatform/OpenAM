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
 * $Id: ConfigureGlobalWebXMLTask.java,v 1.2 2008/11/28 12:36:21 saueree Exp $
 */

package com.sun.identity.agents.tools.tomcat.v6;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.ITask;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.xml.XMLDocument;

import java.io.File;

import java.util.Map;


public class ConfigureGlobalWebXMLTask extends FilterBase implements ITask {
    public static final String LOC_TSK_MSG_CONFIGURE_GLOBAL_WEB_XML_EXECUTE =
        "TSK_MSG_CONFIGURE_GLOBAL_WEB_XML_EXECUTE";
    public static final String LOC_TSK_MSG_CONFIGURE_GLOBAL_WEB_XML_ROLLBACK =
        "TSK_MSG_CONFIGURE_GLOBAL_WEB_XML_ROLLBACK";

    public boolean execute(
        String name,
        IStateAccess stateAccess,
        Map properties) throws InstallException {
        boolean status = true;
        boolean skipTask = skipTask(stateAccess);

        if (skipTask) {
            Debug.log("Skipping ConfigureGlobalWebXMLTask.execute()");
        } else {
            try {
                XMLDocument xmldoc = new XMLDocument(new File(
                            getGlobalWebXMLFile(stateAccess)));
                xmldoc.setNoValueIndent();

                // add the filter and filter mapping elements
                status = super.addFilterElement(xmldoc);
            } catch (Exception ex) {
                Debug.log(
                    "ConfigureGlobalWebXMLTask.execute(): encountered " +
                    "exception " + ex.getMessage(), ex);
                status = false;
            }
        }

        return status;
    }

    public LocalizedMessage getExecutionMessage(
        IStateAccess stateAccess,
        Map properties) {
        String globalWebXMLFile = getGlobalWebXMLFile(stateAccess);
        Object[] args = { globalWebXMLFile };
        LocalizedMessage message = LocalizedMessage.get(
                LOC_TSK_MSG_CONFIGURE_GLOBAL_WEB_XML_EXECUTE,
                STR_TOMCAT_GROUP,
                args);

        return message;
    }

    public LocalizedMessage getRollBackMessage(
        IStateAccess stateAccess,
        Map properties) {
        String globalWebXMLFile = getGlobalWebXMLFile(stateAccess);
        Object[] args = { globalWebXMLFile };
        LocalizedMessage message = LocalizedMessage.get(
                LOC_TSK_MSG_CONFIGURE_GLOBAL_WEB_XML_ROLLBACK,
                STR_TOMCAT_GROUP,
                args);

        return message;
    }

    public boolean rollBack(
        String name,
        IStateAccess stateAccess,
        Map properties) throws InstallException {
        boolean status = false;
        boolean skipTask = skipTask(stateAccess);

        if (skipTask) {
            Debug.log("Skipping ConfigureGlobalWebXMLTask.rollback()");
            status = true;
        } else {
            try {
                XMLDocument xmldoc = new XMLDocument(new File(
                            getGlobalWebXMLFile(stateAccess)));
                xmldoc.setNoValueIndent();

                // remove the filter and filter mapping elements
                status = super.removeFilterElement(xmldoc);
                status = super.removeFilterMappingElement(xmldoc)
                    && status;

                xmldoc.store();
            } catch (Exception ex) {
                Debug.log(
                    "ConfigureGlobalWebXMLTask.rollBack(): "
                    + ex.getMessage(),
                    ex);
                status = false;
            }
        }

        return status;
    }

    private String getGlobalWebXMLFile(IStateAccess stateAccess) {
        return (String) stateAccess.get(
            STR_KEY_TOMCAT_GLOBAL_WEB_XML_FILE);
    }

    private boolean skipTask(IStateAccess stateAccess) {
        boolean result = false;

        String installFilterInGlobalWebXML = (String) stateAccess.get(
                STR_KEY_INSTALL_GLOBAL_WEB_XML);
        result = Boolean.valueOf(installFilterInGlobalWebXML)
                        .booleanValue();

        return (!result);
    }
}
