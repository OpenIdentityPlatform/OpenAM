/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: DomainXMLBase.java,v 1.1 2009/06/12 22:03:03 huacui Exp $
 *
 */

package com.sun.identity.agents.install.appserver.v91;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallConstants;
import com.sun.identity.install.tools.util.ConfigUtil;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.xml.XMLDocument;
import com.sun.identity.install.tools.util.xml.XMLElement;
import com.sun.identity.agents.install.appserver.IConfigKeys;

/**
 * The class used by the installer to make changes in domain.xml file
 * of the Sun App Server
 */
public class DomainXMLBase implements InstallConstants, IConfigKeys, IConstants 
{    
    public DomainXMLBase() {        
    }
 
    public boolean addAgentJavaConfig(XMLDocument domainXMLDoc, 
        XMLElement instanceConfig, IStateAccess stateAccess) throws Exception {
        boolean status = false;
        XMLElement javaConfig = getUniqueElement(STR_JAVA_CONFIG_ELEMENT, 
            instanceConfig);
        if (javaConfig != null) {
            String classPathTag = STR_CLASSPATH_ATTR;
            String classpath = javaConfig.getAttributeValue(classPathTag);
            if (classpath == null) {
                // classpath-suffix tag is used in some AS containers
                classPathTag = STR_CLASSPATH_SUFFIX_ATTR;
                classpath = javaConfig.getAttributeValue(classPathTag);
            }
            if (classpath != null) {
                String updatedClasspath = appendAgentClassPath(classpath, 
                    stateAccess);
                javaConfig.updateAttribute(classPathTag, updatedClasspath);
                status = true;
            } else {
                Debug.log("DomainXMLBase.addAgentClasspath() - Error: " +
                    "Missing '" + classPathTag + "' " + "attribute");
            }
        } else {
            Debug.log("DomainXMLBase.addAgentClasspath() - Error: " + 
                "Missing '" + STR_JAVA_CONFIG_ELEMENT + "' element.");
        }
        return status;
    }
    
    public boolean addHttpProvider(XMLDocument domainXMLDoc, 
        XMLElement instanceConfig, IStateAccess stateAccess) throws Exception {
        boolean status = false;
        XMLElement securityService = getUniqueElement(STR_SECURITY_SERVICE_ELEMENT, 
            instanceConfig);
        if (securityService != null) {
            // Add HttpProvider for wsc agent
            StringBuffer sb = new StringBuffer(256);
            sb.append("<message-security-config auth-layer=\"HttpServlet\">");
            sb.append("<provider-config class-name=");
            sb.append("\"com.sun.identity.wssagents.common.provider.FAMHttpAuthModule\" ");
            sb.append("provider-id=\"FAMHttpProvider\" provider-type=\"server\">");
            sb.append("<request-policy/>");
            sb.append("<response-policy/>");
            sb.append("<property name=\"providername\" value=\"wsc\"/>");
            sb.append("</provider-config>");
            sb.append("</message-security-config>");
            XMLElement provider = domainXMLDoc.newElementFromXMLFragment(
                sb.toString());
            securityService.addChildElement(provider, true);
            status = true;
        } else {
            Debug.log("DomainXMLBase.addHttpProvider() - Error: " + 
                "Missing '" + STR_SECURITY_SERVICE_ELEMENT + "' element.");
        }
        return status;
    }

    private String appendAgentClassPath(String classpath, 
        IStateAccess stateAccess) {               
        StringBuffer sb = new StringBuffer(classpath);        
        String[] agentEntries =  getAgentClasspathEntries(stateAccess);
        int count = agentEntries.length;        
        for (int i = 0; i < count; i++) {
            sb.append(STR_SERVER_CLASSPATH_SEP);
            sb.append(agentEntries[i]);
        }
        
        String resultClasspath = sb.toString();
        Debug.log("DomainXMLBase.appendAgentClassPath() Original " +
                "classpath: " + classpath + "\nResult classpath: " + 
                resultClasspath);
        
        return resultClasspath;
    }
    
    public boolean removeAgentClasspath(XMLElement instanceConfig, 
        IStateAccess stateAccess) throws Exception {
        boolean status = false;
        
        XMLElement javaConfig = getUniqueElement(STR_JAVA_CONFIG_ELEMENT, 
            instanceConfig);
        if (javaConfig != null) {
            String classPathTag = STR_CLASSPATH_ATTR;
            String classpath = javaConfig.getAttributeValue(classPathTag);
            if (classpath == null) {
                // classpath-suffix tag is used in some AS containers
                classPathTag = STR_CLASSPATH_SUFFIX_ATTR;
                classpath = javaConfig.getAttributeValue(classPathTag);
            }
            if (classpath != null) {
                String updatedClasspath = deleteAgentClasspath(classpath, 
                    stateAccess);
                javaConfig.updateAttribute(classPathTag, updatedClasspath);
                status = true;
            } else {
                Debug.log("DomainXMLBase.removeAgentClasspath() - " +
                    "Error: Missing '" + classPathTag + "' " +
                    "attribute");                
            }            
            
        } else {
            Debug.log("DomainXMLBase.removeAgentClasspath() - Error:" + 
                " Missing '" + STR_JAVA_CONFIG_ELEMENT + "' element.");
        }
                
        return status;
    }

    private String deleteAgentClasspath(String classpath, 
        IStateAccess stateAccess) {       
        StringBuffer sb = new StringBuffer(classpath);        
        String[] agentEntries =  getAgentClasspathEntries(stateAccess);
        int count = agentEntries.length;
        
        for (int i = 0; i < count; i++) {
            Debug.log("ConfigureDomainXMLBase.deleteAgentClasspath() deleting" +
                " token: " + agentEntries[i]);
            String tempClasspath = sb.toString(); 
            int startTokenIndex = tempClasspath.indexOf(
                agentEntries[i]);
            if (startTokenIndex != -1) { 
                // Start index is the point where ':' for this token starts OR 0
                int beginIndex = tempClasspath.lastIndexOf(
                    STR_SERVER_CLASSPATH_SEP, startTokenIndex);
                beginIndex = (beginIndex != -1) ? beginIndex : 0;
                
                // End index is the starting of next ':' OR end of the String
                int endIndex = tempClasspath.indexOf(STR_SERVER_CLASSPATH_SEP, 
                    startTokenIndex);
                endIndex = (endIndex != -1) ? endIndex : tempClasspath.length();
                
                sb.delete(beginIndex, endIndex);
            }            
        }         
                        
        String resultClasspath = sb.toString();
        Debug.log("DomainXMLBase.deleteAgentClasspath() Original " +
                "classpath: " + classpath + "\nResult classpath: " + 
                resultClasspath);
                
        return resultClasspath;
    }
   
    public boolean removeHttpProvider(XMLElement instanceConfig, 
        IStateAccess stateAccess) throws Exception {
        boolean status = true;
        XMLElement securityService = getUniqueElement(
            STR_SECURITY_SERVICE_ELEMENT, instanceConfig);
        if (securityService != null) {
            // Remove the http provider
            XMLElement provider = getElement(securityService, 
                   STR_MESSAGE_SECURITY_CONFIG_ELEMENT, 
                   STR_AUTH_LAYER_ATTR, STR_AUTH_LAYER_VALUE);
            if (provider != null) {
                provider.delete();
            }
        } else {
            Debug.log("DomainXMLBase.removedHttpProvider() - Error: "
                + "Unable to remove the Http Provider. Missing '" 
                + STR_SECURITY_SERVICE_ELEMENT +
                "' element in domain.xml");
            status = false;
        }
        return status;
    }

    public String getInstanceConfigName(XMLElement domainRoot, String serverName) {        
        String instanceConfigName = null;
        XMLElement serversElement = getUniqueElement(STR_SERVERS_ELEMENT, 
            domainRoot);
        XMLElement serverElement = getElement(serversElement, 
            STR_SERVER_ELEMENT, STR_NAME_ATTR, serverName);
        if (serverElement != null) {
            instanceConfigName = serverElement.getAttributeValue(
                                               STR_CONFIG_REL_ATTR);
        }
        return instanceConfigName;
    }
    
    public XMLElement getInstanceConfig(XMLDocument domainXMLDoc, String serverName) {
        // Obtain the domain root <domain> element
        XMLElement domainRoot = domainXMLDoc.getRootElement();
        // Get the <configs> element
        XMLElement configsElement = getUniqueElement(
            STR_CONFIGS_ELEMENT, domainRoot);
        // Get the <config> element for the instance
        String serverConfigName = getInstanceConfigName(domainRoot, serverName);
        XMLElement instanceConfigElement = getElement(configsElement, 
            STR_CONFIG_ELEMENT, STR_NAME_ATTR, serverConfigName);
        
        return instanceConfigElement;
    }
    
    public XMLElement getElement(XMLElement parent, String elementName, 
        String attrName, String attrValue) {
        XMLElement result = null;
        ArrayList list = parent.getNamedChildElements(elementName);                    
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
       
    public XMLElement getUniqueElement(String name, XMLElement parent) {
        XMLElement result = null;
        ArrayList list = parent.getNamedChildElements(name);
        if (list != null && list.size() == 1) {
            result = (XMLElement) list.get(0);
        }
        return result;
    }
    
    public String getDomainXMLFile(IStateAccess stateAccess) {
        return (String) stateAccess.get(STR_KEY_AS_DOMAIN_XML_FILE);
    }
    
    public String getServerInstanceName(IStateAccess stateAccess) {
        return (String) stateAccess.get(STR_SERVER_INSTANCE_NAME_KEY);
    }
    
    protected String[] getAgentClasspathEntries(IStateAccess stateAccess) {
        if (agentClasspathEntries == null) {
            String baseDir = (String)stateAccess.get(
                                STR_KEY_AS_INSTALL_DIR);
            StringBuffer sb = new StringBuffer();
            sb.append(baseDir).append(FILE_SEP).append(STR_ADDONS_DIR);
            sb.append(FILE_SEP).append(STR_OPENSSO).append(FILE_SEP);
            sb.append(STR_AGENT_JAR);
 
            String[] entries = new String[] { sb.toString() };                
            for (int i=0; i<entries.length; i++) {
                Debug.log("DomainXMLBase.getAgentClasspathEntries(): "
                         + "next entry: " + entries[i]);
            }            
            agentClasspathEntries = entries;
        }
        return agentClasspathEntries;
    }
   
    private String getAgentInstanceName(IStateAccess stateAccess) {
        String instanceName = null;
	String agentInstanceName = (String) stateAccess.get(
            STR_AGENT_INSTANCE_NAME_KEY);

	// Get the user input for agent instance name only when instance is remote
	if (agentInstanceName != null && agentInstanceName.trim().length() > 0) {
	    instanceName = agentInstanceName;
	    Debug.log("Using remote agent instance name : "+ agentInstanceName);
	} else {
	    instanceName = stateAccess.getInstanceName();
        }

	return instanceName;
    }
 
    protected String getConfigDirPath() {
        return ConfigUtil.getConfigDirPath();
    }
    
    private String[] agentClasspathEntries;

    public static final String STR_SERVERS_ELEMENT = "servers";
    public static final String STR_CONFIGS_ELEMENT = "configs";
    public static final String STR_CONFIG_ELEMENT = "config";
    public static final String STR_SERVER_ELEMENT = "server";
    public static final String STR_JAVA_CONFIG_ELEMENT = "java-config";
    public static final String STR_SECURITY_SERVICE_ELEMENT = 
        "security-service";
    public static final String STR_CLASSPATH_ATTR = "server-classpath";
    public static final String STR_CLASSPATH_SUFFIX_ATTR = "classpath-suffix";
    public static final String STR_CLASS_NAME_ATTR = "classname";
    public static final String STR_CONFIG_REL_ATTR = "config-ref";
    public static final String STR_NAME_ATTR = "name";
    public static final String STR_VALUE_ATTR = "value";
    public static final String STR_MESSAGE_SECURITY_CONFIG_ELEMENT =  
        "message-security-config";
    public static final String STR_AUTH_LAYER_ATTR = "auth-layer";
    public static final String STR_AUTH_LAYER_VALUE = "HttpServlet";
    public static final String STR_SERVER_CLASSPATH_SEP = 
        "${path.separator}";
    public static final String STR_FM_CLIENT_SDK_JAR = "openssoclientsdk.jar";
    public static final String STR_AGENT_JAR = "openssowssproviders.jar";
    public static final String STR_SERVER_INSTANCE_NAME_KEY = "INSTANCE_NAME";
}
