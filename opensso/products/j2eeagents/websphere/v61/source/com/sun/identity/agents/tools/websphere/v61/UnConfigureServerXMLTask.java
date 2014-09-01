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
 * $Id: UnConfigureServerXMLTask.java,v 1.1 2008/11/21 22:21:55 leiming Exp $
 *
 */

package com.sun.identity.agents.tools.websphere.v61;

import java.io.File;
import java.util.Map;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.ITask;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.xml.XMLDocument;
import com.sun.identity.install.tools.util.xml.XMLElement;
import com.sun.identity.agents.tools.websphere.ServerXMLBase;


/**
 * This class un-configures server.xml
 */

public class UnConfigureServerXMLTask extends ServerXMLBase
        implements ITask {
    
    public boolean execute(String name, IStateAccess stateAccess,
            Map properties) throws InstallException {
        
        boolean status = true;
        try {
            String serverXmlFile = (String)stateAccess.get(
                    STR_KEY_SERVER_INSTANCE_DIR) +
                    STR_FILE_SEP + STR_SERVER_XML_FILE;
            String serverInstName = (String)stateAccess.get(
                    STR_SERVER_INSTANCE_NAME);
            if ((serverXmlFile != null) && (serverXmlFile.length() > 0)) {
                XMLDocument serverXMLDoc = new XMLDocument(
                        new File(serverXmlFile));
                serverXMLDoc.setIndentDepth(2);
                XMLElement jvmEntries = findClassPathElement(
                        serverXMLDoc,serverInstName,STR_PROC_DEF);
                if (jvmEntries != null) {
                    Debug.log(
                            "UnConfigureServerXMLTask.execute - jvmEntries = " +
                            jvmEntries.toXMLString());
                    // remove IBM encryption JVM options
                    status = unConfigureJVMOptions(serverXMLDoc, 
                            jvmEntries, stateAccess);
                    // We have two cases to handle. classpath as attr or 
                    // classpath as element
                    if (status) {
                        status = unConfigureClasspath(serverXMLDoc,jvmEntries,
                            stateAccess);
                    }
                }
                serverXMLDoc.store();
            } else {
                Debug.log(
                        "UnConfigureServerXMLTask.rollback() - " +
                        "Invalid server.xml file specified = " + 
                        serverXmlFile);
                status = false;
            }
        } catch (Exception ex) {
            Debug.log("UnConfigureServerXMLTask.rollback() - Exception thrown "
                    + "while un-sconfiguring server.xml file",ex);
            status = false;
        }
        return status;
        
    }
    
    
    
    public boolean rollBack(String name, IStateAccess stateAccess,
            Map properties) throws InstallException {
        // nothing to do
        return true;
        
    }
    
    public LocalizedMessage getExecutionMessage(IStateAccess stateAccess,
            Map properties) {
        
        String serverXmlFile = (String)stateAccess.get(
                STR_KEY_SERVER_INSTANCE_DIR) +
                STR_FILE_SEP + STR_SERVER_XML_FILE;
        Object[] args = { serverXmlFile };
        LocalizedMessage message =
                LocalizedMessage.get(
                LOC_TSK_MSG_UNCONFIGURE_SERVER_XML_FILE_EXECUTE,
                STR_WAS_GROUP, args);
        
        return message;
    }
    
    public LocalizedMessage getRollBackMessage(IStateAccess stateAccess,
            Map properties) {
        String serverXmlFile = (String)stateAccess.get(
                STR_KEY_SERVER_INSTANCE_DIR) +
                STR_FILE_SEP + 
                STR_SERVER_XML_FILE;
        Object[] args = { serverXmlFile };
        LocalizedMessage message =
                LocalizedMessage.get(
                    LOC_TSK_MSG_UNCONFIGURE_SERVER_XML_FILE_ROLLBACK,
                    STR_WAS_GROUP, args);
        
        return message;
    }
    
    public static final String LOC_TSK_MSG_UNCONFIGURE_SERVER_XML_FILE_EXECUTE =
            "TSK_MSG_UNCONFIGURE_SERVER_XML_FILE_EXECUTE";
    public static final String 
            LOC_TSK_MSG_UNCONFIGURE_SERVER_XML_FILE_ROLLBACK =
            "TSK_MSG_UNCONFIGURE_SERVER_XML_FILE_ROLLBACK";
    
    public static final String STR_PROC_DEF = "processDefinitions";
    
}