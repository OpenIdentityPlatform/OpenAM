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
 * $Id: ConfigureDomainXMLTask.java,v 1.3 2008/06/25 05:52:11 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */
package com.sun.identity.agents.install.appserver.v81;

import java.io.File;
import java.util.Map;
import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.ITask;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.xml.XMLDocument;
import com.sun.identity.install.tools.util.xml.XMLElement;
import org.forgerock.openam.agents.install.appserver.VersionChecker;

/**
 * The class used by the installer to make changes in domain.xml file of
 * the Sun App Server
 */
public class ConfigureDomainXMLTask extends DomainXMLBase implements ITask {

    public boolean execute(String name, IStateAccess stateAccess,
            Map properties) throws InstallException {
        boolean status = false;
        boolean skipTask = skipTask(stateAccess);

        if (skipTask) {
            Debug.log("Skipping ConfigureDomainXMLTask.execute()");
            status = true;
        } else {
            String serverXMLFile = getDomainXMLFile(stateAccess);
            String serverInstanceName = getServerInstanceName(stateAccess);
            if (serverInstanceName == null) {
                // use the default one
                serverInstanceName = DEFAULT_INSTANCE_NAME;
            }
            if (serverXMLFile != null) {
                try {
                    File serverXML = new File(serverXMLFile);
                    XMLDocument domainXMLDoc = new XMLDocument(serverXML);
                    XMLElement instanceConfig = getInstanceConfig(
                            domainXMLDoc, serverInstanceName);
                    
                    if (instanceConfig != null) {
                        status &= addAgentRealm(domainXMLDoc, instanceConfig, stateAccess);
                        status &= addLifecycleModule(domainXMLDoc, serverInstanceName, stateAccess);
                        status = addAgentJavaConfig(domainXMLDoc, instanceConfig,
                                stateAccess);
                        domainXMLDoc.setIndentDepth(2);
                        domainXMLDoc.store();
                    }
                } catch (Exception e) {
                    status = false;
                    Debug.log("ConfigureDomainXMLTask.execute() Error occurred "
                            + "while updating serverXML file '" + serverXMLFile + "'.", e);
                }
            } else {
                Debug.log("ConfigureDomainXMLTask.execute() Error could get "
                        + "server.xml file: " + serverXMLFile);
            }
        }

        return status;
    }

    public LocalizedMessage getExecutionMessage(IStateAccess stateAccess,
            Map properties) {
        String serverXMLFile = getDomainXMLFile(stateAccess);
        Object[] args = {serverXMLFile};
        LocalizedMessage message = LocalizedMessage.get(
                LOC_TSK_MSG_CONFIGURE_DOMAIN_XML_EXECUTE, STR_AS_GROUP, args);
        return message;
    }

    public LocalizedMessage getRollBackMessage(IStateAccess stateAccess,
            Map properties) {
        String serverXMLFile = getDomainXMLFile(stateAccess);
        Object[] args = {serverXMLFile};
        LocalizedMessage message = LocalizedMessage.get(
                LOC_TSK_MSG_CONFIGURE_DOMAIN_XML_ROLLBACK, STR_AS_GROUP, args);
        return message;
    }

    public boolean rollBack(String name, IStateAccess stateAccess,
            Map properties) throws InstallException {
        boolean status = false;
        boolean skipTask = skipTask(stateAccess);

        if (skipTask) {
            Debug.log("Skipping ConfigureDomainXMLTask.rollback()");
            status = true;
        } else {
            String serverXMLFile = getDomainXMLFile(stateAccess);
            String serverInstanceName = getServerInstanceName(stateAccess);
            if (serverInstanceName == null) {
                // use the default one
                serverInstanceName = DEFAULT_INSTANCE_NAME;
            }
            if (serverXMLFile != null) {
                try {
                    File serverXML = new File(serverXMLFile);
                    XMLDocument domainXMLDoc = new XMLDocument(serverXML);
                    XMLElement instanceConfig = getInstanceConfig(domainXMLDoc,
                            serverInstanceName);
                    if (instanceConfig != null) {
                        status &= removeAgentRealm(domainXMLDoc, instanceConfig, stateAccess);
                        status &= removeLifecycleModule(domainXMLDoc, serverInstanceName, stateAccess);
                        if (!VersionChecker.isGlassFishv3(stateAccess)) {
                            status = removeAgentClasspath(instanceConfig, stateAccess);
                        }
                        domainXMLDoc.setIndentDepth(8);
                        domainXMLDoc.store();
                    }
                } catch (Exception e) {
                    status = false;
                    Debug.log("ConfigureDomainXMLTask.execute() Error occurred "
                            + "while updating serverXML file '" + serverXMLFile + "'.", e);
                }
            } else {
                Debug.log("ConfigureDomainXMLTask.rollBack() Error could get "
                        + "server.xml file: " + serverXMLFile);
            }
        }

        return status;
    }

    private boolean skipTask(IStateAccess stateAccess) {
        boolean result = false;
        String isRemote = (String) stateAccess.get(STR_DAS_HOST_IS_REMOTE_KEY);

        if (isRemote != null) {
            result = Boolean.valueOf(isRemote).booleanValue();
            Debug.log("ConfigureDomainXMLTask: skipTask = " + isRemote);
        }

        return result;
    }
    public static final String DEFAULT_INSTANCE_NAME = "server";
    public static final String LOC_TSK_MSG_CONFIGURE_DOMAIN_XML_EXECUTE =
            "TSK_MSG_CONFIGURE_DOMAIN_XML_EXECUTE";
    public static final String LOC_TSK_MSG_CONFIGURE_DOMAIN_XML_ROLLBACK =
            "TSK_MSG_CONFIGURE_DOMAIN_XML_ROLLBACK";
}
