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
 * $Id: ServiceXMLBase.java,v 1.1 2008/12/11 14:36:06 naghaon Exp $
 *
 */

package com.sun.identity.agents.tools.jboss;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallConstants;
import com.sun.identity.install.tools.util.ConfigUtil;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.xml.XMLDocument;
import com.sun.identity.install.tools.util.xml.XMLElement;
import com.sun.identity.install.tools.util.OSChecker;

import java.io.File;
import java.util.ArrayList;

/**
 * @author sevani
 *
 * Configure and unconfigures agent's jar files in JBoss server instance's
 * jboss-service.xml.
 */

public class ServiceXMLBase implements InstallConstants, 
        IConfigKeys, IConstants {
    public ServiceXMLBase() {
    }
    
    public boolean addAgentClasspath(IStateAccess stateAccess) {
        boolean status = true;
        String serverXMLFile = getServiceXMLFile(stateAccess);
        try {
            File serverXML = new File(serverXMLFile);
            XMLDocument serviceXMLDoc = new XMLDocument(serverXML);
            XMLElement serviceRoot = serviceXMLDoc.getRootElement();
            if (serviceRoot != null) {
                // Create and Add the Agent jars element

                String libPath = ConfigUtil.getLibPath();
                // removes the drive: from the path
                if (OSChecker.isWindows()) {
                    libPath = libPath.substring(2, libPath.length()); 
                }
                
                StringBuffer sb = new StringBuffer();
                sb.append("<classpath codebase=\"").
                   append(libPath);
                sb.append("\" ");
                sb.append("archives=\"").append(STR_AGENT_JAR);
                sb.append("\"/> ");
            
                XMLElement agentClasspath = 
                    serviceXMLDoc.newElementFromXMLFragment(sb.toString());
                serviceRoot.addChildElementAt(agentClasspath, 1, true);
            
                status = addAMClientSDKClasspath(serviceXMLDoc, stateAccess);
                if (status) {
                    serviceXMLDoc.setIndentDepth(2);
                    serviceXMLDoc.store();
                    Debug.log("ServiceXMLBase.addAgentClasspath() - " +
                        "Added agentClasspath element in jboss-service.xml");
                }
            } else {
                Debug.log("ServiceXMLBase.addAgentClasspath() - Error: " +
                    "Unable to add agentClasspath. Missing '" +
                    STR_SERVER_ELEMENT + "' element in jboss-service.xml");
                status = false;
            }
        } catch (Exception e) {
            status = false;
            Debug.log("ServiceXMLBase.addAgentClasspath() - Error: " +
                    "Unable to add agentClasspath in jboss-service.xml", e);
        } 
        return status;
    }
    
    private boolean addAMClientSDKClasspath(XMLDocument serviceXMLDoc,
            IStateAccess stateAccess) {
        boolean status = true;
        try {
            XMLElement serviceRoot = serviceXMLDoc.getRootElement();
            if (serviceRoot != null) {
            
                // Create and Add the Agent jars element
                StringBuffer sb = new StringBuffer();
                String libPath = ConfigUtil.getLibPath();
                // removes the drive: from the path
                if (OSChecker.isWindows()) {
                    libPath = libPath.substring(2, libPath.length()); 
                }
                sb.append("<classpath codebase=\"").
                   append(libPath);
                sb.append("\" ");
                sb.append("archives=\"").append(STR_AM_CLIENT_SDK_JAR);
                sb.append("\"/> ");
            
                XMLElement amclientClasspath = 
                    serviceXMLDoc.newElementFromXMLFragment(sb.toString());
                serviceRoot.addChildElementAt(amclientClasspath, 2, true);
            
            } else {
                Debug.log("ServiceXMLBase.addAMClientSDKClasspath() - Error: " +
                    "Unable to add AMClientSDKClasspath. Missing '" +
                    STR_SERVER_ELEMENT + "' element in jboss-service.xml");
                status = false;
            }
        
        } catch (Exception e) {
            status = false;
            Debug.log("ServiceXMLBase.addAMClientSDKClasspath() - Error: " +
                    "Unable to add agentClasspath in jboss-service.xml", e);
        } 
        return status;
    }
    
    public boolean removeAgentClasspath(IStateAccess stateAccess) {
        boolean status = true;
        String serverXMLFile = getServiceXMLFile(stateAccess);
        String serverInstanceName = getServerInstanceName(stateAccess);
        try {
            File serverXML = new File(serverXMLFile);
            XMLDocument serviceXMLDoc = new XMLDocument(serverXML);
            XMLElement agentClasspath = getAgentClasspath(serviceXMLDoc);
            if (agentClasspath != null) {
                // Remove the Agent jar element
                agentClasspath.delete();
                XMLElement amclientSDKClasspath = 
                        getAMClientSDKClasspath(serviceXMLDoc);
                if (amclientSDKClasspath != null) {
                    amclientSDKClasspath.delete();
                }
                serviceXMLDoc.setIndentDepth(8);
                serviceXMLDoc.store();
            } else {
                status = false;
            }
        } catch (Exception e) {
            status = false;
            Debug.log("ServiceXMLBase.removeAgentClasspath() - Error: " +
                    "Unable to remove agentClasspath in jboss-service.xml", e);
        } 
        return status;
    }
    
    public XMLElement getInstanceConfig(XMLDocument serviceXMLDoc) {
        // Obtain the service root <server> element
        XMLElement serviceRoot = serviceXMLDoc.getRootElement();
        
        return serviceRoot;
    }
    
    private XMLElement getAgentClasspath(XMLDocument xmlDoc) {
        return getElement(xmlDoc, STR_AGENT_CLASSPATH,
                STR_ARCHIVES_ATTR, STR_AGENT_JAR);
    }
    
    private XMLElement getAMClientSDKClasspath(XMLDocument xmlDoc) {
        return getElement(xmlDoc, STR_AGENT_CLASSPATH,
                STR_ARCHIVES_ATTR, STR_AM_CLIENT_SDK_JAR);
    }
    
    public XMLElement getElement(XMLDocument xmlDoc, String elementName,
            String attrName, String attrValue) {
        XMLElement result = null;
        ArrayList list = 
                xmlDoc.getRootElement().getNamedChildElements(elementName);
        if (list != null &&  list.size() > 0) {
            int count = list.size();
            for (int i = 0; i < count; i++) {
                XMLElement element = (XMLElement) list.get(i);
                String value = element.getAttributeValue(attrName);
                if (value.equals(attrValue)) {
                    result = element;
                    break;
                }
            }
        }
        return result;
    }
    
    public XMLElement getUniqueElement(String name, XMLDocument xmlDoc) {
        XMLElement result = null;
        ArrayList list = xmlDoc.getRootElement().getNamedChildElements(name);
        if (list != null && list.size() == 1) {
            result = (XMLElement) list.get(0);
        }
        return result;
    }
    
    public String getServiceXMLFile(IStateAccess stateAccess) {
        return (String) stateAccess.get(STR_KEY_JB_SERVICE_XML_FILE);
    }
    
    public String getServerInstanceName(IStateAccess stateAccess) {
        return (String) stateAccess.get(STR_KEY_JB_INST_NAME);
    }
    
    private String getAgentInstanceName(IStateAccess stateAccess) {
        String instanceName = null;
        String agentInstanceName = 
                (String) stateAccess.get(STR_KEY_AGENT_INSTANCE_NAME);
        
        if (agentInstanceName != null && agentInstanceName.trim().length() > 0) 
        {
            instanceName = agentInstanceName;
            Debug.log("Using remote agent instance name : "+ agentInstanceName);
        } else {
            instanceName = stateAccess.getInstanceName();
        }
        
        return instanceName;
        
    }
    
}
