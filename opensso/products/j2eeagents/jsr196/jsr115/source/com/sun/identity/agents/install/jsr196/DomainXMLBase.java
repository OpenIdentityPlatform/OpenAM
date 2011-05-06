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
 * $Id: DomainXMLBase.java,v 1.1 2009/01/30 12:09:38 kalpanakm Exp $
 *
 */

package com.sun.identity.agents.install.jsr196;

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
import com.sun.identity.agents.install.jsr196.IConfigKeys;

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
            
            // Add JVM Options - If an error occurs an Exception will be thrown
            addAgentJVMOptions(domainXMLDoc, javaConfig, stateAccess);
        } else {
            Debug.log("DomainXMLBase.addAgentClasspath() - Error: " + 
                "Missing '" + STR_JAVA_CONFIG_ELEMENT + "' element.");
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
    
    private void addAgentJVMOptions(XMLDocument domainXMLDoc, 
        XMLElement javaConfig, IStateAccess stateAccess) throws Exception {           
        Iterator iter = getAgentJVMOptions(stateAccess).iterator();
        StringBuffer sb = new StringBuffer(256);
        while (iter.hasNext()) {
            String option = (String) iter.next();
            sb.append("<jvm-options>").append(option).append("</jvm-options>");
            XMLElement jvmOption = domainXMLDoc.newElementFromXMLFragment(
                sb.toString());
            javaConfig.addChildElement(jvmOption, true);
            sb.delete(0, sb.length()); // clear the buffer
        }
    }
        
    public boolean addJSR196Provider(XMLDocument domainXMLDoc, 
        XMLElement instanceConfig, IStateAccess stateAccess) throws Exception {
        boolean status = true;
        int index = 0;
        XMLElement securityService = getUniqueElement(
            STR_SECURITY_SERVICE_ELEMENT, instanceConfig);
        if (securityService != null) {
            
            ArrayList childElements = securityService.getChildElements();          
            
            for(int i=0; i < childElements.size(); i++) {
               XMLElement aa  = (XMLElement) childElements.get(i);
               String nn = aa.getName();
               if (!nn.equalsIgnoreCase("property"))
                   index = i;
            }
            
            // Create and Add the JSR196 provider
            StringBuffer sb = new StringBuffer();
            sb.append("<message-security-config auth-layer=\"HttpServlet\" default-client-provider=\"JSR196Provider\" default-provider=\"JSR196Provider\">");
            sb.append("<provider-config class-name=\"com.sun.opensso.agents.jsr196.OpenSSOServerAuthModule\" provider-id=\"JSR196Provider\" provider-type=\"server\">");
            sb.append("<request-policy auth-source=\"content\"/>");
            sb.append("<response-policy auth-source=\"content\"/>");
            sb.append("</provider-config>");
            sb.append("</message-security-config>");
            XMLElement jsr196Provider = domainXMLDoc.newElementFromXMLFragment(
                sb.toString());
            securityService.addChildElementAt(jsr196Provider, index, true);
        } else {
            Debug.log("DomainXMLBase.addJSR196Provider() - Error: " +
                "Unable to add JSR196 Provider. Missing '" + 
                STR_SECURITY_SERVICE_ELEMENT + "' element in server.xml");
            status = false;
        }
        
        return status;
    }
    
    public boolean addJSR115Provider(XMLDocument domainXMLDoc, 
        XMLElement instanceConfig, IStateAccess stateAccess) throws Exception {
        boolean status = true;
        int index=0;
        XMLElement securityService = getUniqueElement(
            STR_SECURITY_SERVICE_ELEMENT, instanceConfig);
        if (securityService != null) {                        
            
            String preAgentJACCProvider = securityService.getAttributeValue(
               STR_JACC );
            if (preAgentJACCProvider != null) { // Save the value, so that it
                // can be used during un-install
                stateAccess.put(STR_PRE_AGENT_JACC_PROVIDER, 
                    preAgentJACCProvider);
            }            
            // Update the Default Realm to be the agentRealm
            securityService.updateAttribute(STR_JACC, 
                "openssoJACCProvider");                    
            
            String preAgentP2RMapping = securityService.getAttributeValue(STR_P2RMAPPING);
            
            if (preAgentP2RMapping != null) {
                stateAccess.put(STR_PRE_AGENT_P2RMAPPING, preAgentP2RMapping);
            }
            
            securityService.updateAttribute(STR_P2RMAPPING, "true");
            
            ArrayList childElements = securityService.getChildElements();          
            
            for(int i=0; i < childElements.size(); i++) {
               XMLElement aa  = (XMLElement) childElements.get(i);
               String nn = aa.getName();
               if (nn.equalsIgnoreCase("jacc-provider"))
                   index = i;
            }
                        
            // Create and Add the Agent Realm element
            StringBuffer sb = new StringBuffer();
            sb.append("<jacc-provider name=\"openssoJACCProvider\" policy-configuration-factory-provider=\"com.sun.opensso.agents.jsr115.OpenSSOJACCPolicyConfigurationFactory\" ");
            sb.append("policy-provider=\"com.sun.opensso.agents.jsr115.OpenSSOJACCPolicy\"/>");            
            XMLElement jsr115Provider = domainXMLDoc.newElementFromXMLFragment(
                sb.toString());
            securityService.addChildElementAt(jsr115Provider, index, true);
        } else {
            Debug.log("DomainXMLBase.addJSR115Provider() - Error: " +
                "Unable to add jsr115provider. Missing '" + 
                STR_SECURITY_SERVICE_ELEMENT + "' element in server.xml");
            status = false;
        }
        
        return status;
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
            
            // Remove the Agent JVMOptions. If error occurs Exception is thrown
            removeAgentJVMOptions(javaConfig, stateAccess);
            
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
    
    private void removeAgentJVMOptions(XMLElement javaConfig, 
            IStateAccess stateAccess) throws Exception {
        ArrayList jvmOptions = javaConfig.getNamedChildElements(
            STR_JVM_OPTIONS_ELEMENT);
        if (jvmOptions != null && !jvmOptions.isEmpty()) {
            int count = jvmOptions.size();
            for (int i = 0; i < count; i++) {
                XMLElement element = (XMLElement) jvmOptions.get(i);
                String value = element.getValue();
                if (value != null && 
                    getAgentJVMOptions(stateAccess).contains(value.trim())) 
                { // Agent JVM Option - Delete it.
                    element.delete();
                }
            }
        }        
    }
       
    public boolean removeJSR196Provider(XMLDocument domainXMLDoc, 
        XMLElement domainRoot, IStateAccess stateAccess) throws Exception {     
        boolean status = true;        
        XMLElement securityService = getUniqueElement(
            STR_SECURITY_SERVICE_ELEMENT, domainRoot);
        if (securityService != null) {            
            XMLElement provider = getElement(securityService, 
               "message-security-config", "default-provider", "JSR196Provider");
	    if (provider != null) {
                provider.delete();
            }
        } else {
            Debug.log("DomainXMLBase.removeJSR196Provider() - Error: Unable to" +
                    " remove jsr196provider . Missing '" + 
                    STR_SECURITY_SERVICE_ELEMENT + "' element in server.xml");
            status = false;
        }        
        return status;
    }
    
    public boolean removeJSR115Provider(XMLDocument domainXMLDoc, 
        XMLElement domainRoot, IStateAccess stateAccess) throws Exception {     
        boolean status = true;        
        XMLElement securityService = getUniqueElement(
            STR_SECURITY_SERVICE_ELEMENT, domainRoot);
        if (securityService != null) {
            String preAgentJACCProvider = (String) stateAccess.get(
                STR_PRE_AGENT_JACC_PROVIDER);
            
            // Store "" string if previous value not found or empty
            preAgentJACCProvider = (preAgentJACCProvider != null && 
                preAgentJACCProvider.length() > 0) ? preAgentJACCProvider : ""; 

            securityService.updateAttribute(STR_JACC, 
                preAgentJACCProvider);
            
            String preAgentP2RMapping = (String) stateAccess.get(
                    STR_PRE_AGENT_P2RMAPPING);
            
            preAgentP2RMapping = (preAgentP2RMapping != null && 
                    preAgentP2RMapping.length() > 0) ? preAgentP2RMapping: "false";
            
            securityService.updateAttribute(STR_P2RMAPPING, preAgentP2RMapping);
            
            // Remove the Agent Realm 
           XMLElement provider = getElement(securityService, 
               "jacc-provider", "name", "openssoJACCProvider");
	    if (provider != null) {
                provider.delete();
            }
        } else {
            Debug.log("DomainXMLBase.removeJSR115Provider - Error: Unable to del " +
                "jaccProvider. Missing '" + STR_SECURITY_SERVICE_ELEMENT + 
                "' element in server.xml");
            status = false;
        }        
        return status;
    }
    
    public String getInstanceConfigName(XMLElement domainRoot, String serverName) {        
        XMLElement serversElement = getUniqueElement(STR_SERVERS_ELEMENT, 
            domainRoot);
        XMLElement serverElement = getElement(serversElement, 
            STR_SERVER_ELEMENT, STR_NAME_ATTR, serverName);
        String instanceConfigName = serverElement.getAttributeValue(
            STR_CONFIG_REL_ATTR);        
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
    
    private boolean getCoexistFlag(IStateAccess stateAccess) {
        if (_coexistFlag == null) {
	    String strCoexistFlag = (String) stateAccess.get(STR_AM_COEXIST_KEY);
	    Debug.log("DomainXMLBase.getCoexistFlag(): "
                    + "Co-exist flag in Install State: "
	            + strCoexistFlag);
	        
	    if (strCoexistFlag == null || strCoexistFlag.trim().length() == 0) {
	        strCoexistFlag = "false";
	    }
	      
	    Boolean coexistFlag = Boolean.valueOf(strCoexistFlag);
	    Debug.log("DomainXMLBase.getCoexistFlag(): Co-exist flag: "
	            + coexistFlag);	 
	    _coexistFlag = coexistFlag;
        }
        
        return _coexistFlag.booleanValue();
    }
    
    protected String[] getAgentClasspathEntries(IStateAccess stateAccess) {

        if (_agentClasspathEntries == null) {                                          
            String homeDir = ConfigUtil.getHomePath();
            String libPath = ConfigUtil.getLibPath();
            String localeDir = ConfigUtil.getLocaleDirPath(); 

            String remoteHomeDir = (String) stateAccess.get(
                STR_REMOTE_AGENT_INSTALL_DIR_KEY);
            // get the agent install directory on a remote instance
	    if (remoteHomeDir != null && remoteHomeDir.trim().length() > 0) {
		homeDir = remoteHomeDir;
	        libPath = remoteHomeDir + FILE_SEP + INSTANCE_LIB_DIR_NAME;
	        localeDir = remoteHomeDir + FILE_SEP + INSTANCE_LOCALE_DIR_NAME;
		Debug.log(
                    "DomainXMLBase.getAgentClassPathEntries: Modified libPath = "
                     + libPath);
            }

            String[] entries = null;
            if (getCoexistFlag(stateAccess)) {
                entries = new String[] {
                        libPath + FILE_SEP + STR_AGENT_JAR,
                        localeDir,
                };                                
            } else {
	        String instanceName = getAgentInstanceName(stateAccess);

                StringBuffer sb = new StringBuffer(256);
                sb.append(homeDir).append(FILE_SEP);
                sb.append(instanceName).append(FILE_SEP);
                sb.append(INSTANCE_CONFIG_DIR_NAME);            
                String instanceConfigDirPath = sb.toString(); 

                entries = new String[] {
                        libPath + FILE_SEP + STR_AGENT_JAR,
                        libPath + FILE_SEP + STR_FM_CLIENT_SDK_JAR, 
			localeDir,
                        instanceConfigDirPath
                };                
            }
            
            for (int i=0; i<entries.length; i++) {
                Debug.log("DomainXMLBase.getAgentClasspathEntries(): "
                         + "next entry: " + entries[i]);
            }            
            _agentClasspathEntries = entries;
        }
        
        return _agentClasspathEntries;
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
    
    private Set getAgentJVMOptions(IStateAccess stateAccess) {
        if (_agentJVMOptions == null) {
            _agentJVMOptions = new TreeSet(); // An Ordered set
            if (!getCoexistFlag(stateAccess)) {
                String logConfigFileOption = null;
                // check if remote instance.
                String remoteHomeDir = (String) stateAccess.get(
                        STR_REMOTE_AGENT_INSTALL_DIR_KEY);
                // get the agent install directory on a remote instance
                if (remoteHomeDir != null && 
                        remoteHomeDir.trim().length() > 0) {
                    logConfigFileOption = STR_LOG_CONFIG_FILE_OPTION_PREFIX +
                            remoteHomeDir + FILE_SEP + 
                            INSTANCE_CONFIG_DIR_NAME  + FILE_SEP + 
                            STR_LOG_CONFIG_FILENAME;
                } else {
                    logConfigFileOption = STR_LOG_CONFIG_FILE_OPTION_PREFIX
                            + getConfigDirPath() + FILE_SEP
                            + STR_LOG_CONFIG_FILENAME;
                }
                
                _agentJVMOptions.add(logConfigFileOption);
                _agentJVMOptions.add(STR_LOG_COMPATMODE_OPTION);
                _agentJVMOptions.add(STR_JACC_REPOSITORY_LOCATION + STR_JACC_REPOSITORY_FILE);
            }
        }        
        Debug.log("DomainXMLBase.getAgentJVMOptions(): options: "
                + _agentJVMOptions);
        return _agentJVMOptions;
    }
    
    private Boolean _coexistFlag;
    private String[] _agentClasspathEntries;
    private Set _agentJVMOptions;

    public static final String STR_LOG_CONFIG_FILE_OPTION_PREFIX =
        "-Djava.util.logging.config.file=";
    public static final String STR_LOG_CONFIG_FILENAME = 
        "OpenSSOAgentLogConfig.properties";
    public static final String STR_LOG_COMPATMODE_OPTION = 
        "-DLOG_COMPATMODE=Off";    
    public static final String STR_JACC_REPOSITORY_LOCATION =
            "-Dcom.sun.enterprise.jaccprovider.property.repository=";
    public static final String STR_JACC_REPOSITORY_FILE = 
            "${com.sun.aas.instanceRoot}/generated/policy";
    public static final String STR_SERVERS_ELEMENT = "servers";
    public static final String STR_CONFIGS_ELEMENT = "configs";
    public static final String STR_CONFIG_ELEMENT = "config";
    public static final String STR_SERVER_ELEMENT = "server";
    public static final String STR_JVM_OPTIONS_ELEMENT = "jvm-options";
    public static final String STR_JAVA_CONFIG_ELEMENT = "java-config";
    public static final String STR_SECURITY_SERVICE_ELEMENT = 
        "security-service";
    public static final String STR_AUTH_REALM_ELEMENT = "auth-realm";
    public static final String STR_PROPERTY_ELEMENT = "property";
    
    public static final String STR_CLASSPATH_ATTR = "server-classpath";
    public static final String STR_CLASSPATH_SUFFIX_ATTR = "classpath-suffix";
    public static final String STR_DEFAULT_REALM_ATTR = "default-realm";
    public static final String STR_JACC = "jacc";
    public static final String STR_P2RMAPPING = "activate-default-principal-to-role-mapping";
    public static final String STR_CLASS_NAME_ATTR = "classname";
    public static final String STR_CONFIG_REL_ATTR = "config-ref";
    public static final String STR_NAME_ATTR = "name";
    public static final String STR_VALUE_ATTR = "value";
        
    public static final String STR_SERVER_CLASSPATH_SEP = 
        "${path.separator}";
    
    public static final String STR_AS_GROUP = "as81Tools";
    
    public static final String STR_AGENT_JAR = "agent.jar";
    public static final String STR_FM_CLIENT_SDK_JAR = "openssoclientsdk.jar";
    
    public static final String STR_AGENT_REALM = "agentRealm";
  
    public static final String STR_JAAS_CONTEXT = "jaas-context";        
    
    public static final String STR_SERVER_INSTANCE_NAME_KEY = "INSTANCE_NAME";

    public static final String STR_PRE_AGENT_DEFAULT_REALM = 
        "PRE_AGENT_DEFAULT_REALM"; 
    
    public static final String STR_PRE_AGENT_JACC_PROVIDER = "PRE_AGENT_JACC_PROVIDER";
    
    public static final String STR_PRE_AGENT_P2RMAPPING = "PRE_AGENT_P2RMAPPING";
}
