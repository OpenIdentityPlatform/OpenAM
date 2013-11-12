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
 * $Id: DomainXMLBase.java,v 1.14 2010/02/16 22:01:06 hari44 Exp $
 *
 */

/**
 * Portions Copyrighted 2010-2012 ForgeRock Inc
 */
package com.sun.identity.agents.install.appserver.v81;

import com.sun.identity.agents.arch.IAgentConfigurationConstants;
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
import org.forgerock.openam.agents.install.appserver.VersionChecker;

/**
 * The class used by the installer to make changes in domain.xml file
 * of the Sun App Server
 */
public class DomainXMLBase implements InstallConstants, IConfigKeys, IConstants {

    public DomainXMLBase() {
    }

    public boolean addAgentJavaConfig(XMLDocument domainXMLDoc,
            XMLElement instanceConfig, IStateAccess stateAccess) throws Exception {
        boolean status = false;
        XMLElement javaConfig = getUniqueElement(STR_JAVA_CONFIG_ELEMENT,
                instanceConfig);
        if (javaConfig != null) {
            if (!VersionChecker.isGlassFishv3(stateAccess)) {
                String classPathTag = STR_CLASSPATH_ATTR;
                String classpath = javaConfig.getAttributeValue(classPathTag);
                if (classpath == null) {
                    //classpath-suffix tag is used in some AS containers
                    classPathTag = STR_CLASSPATH_SUFFIX_ATTR;
                    classpath = javaConfig.getAttributeValue(classPathTag);
                }
                if (classpath != null) {
                    String updatedClasspath = appendAgentClassPath(classpath,
                            stateAccess);
                    javaConfig.updateAttribute(classPathTag, updatedClasspath);
                } else {
                    Debug.log("DomainXMLBase.addAgentClasspath() - Error: "
                            + "Missing '" + classPathTag + "' " + "attribute");
                }
            }

            // Add JVM Options - If an error occurs an Exception will be thrown
            addAgentJVMOptions(domainXMLDoc, javaConfig, stateAccess);
            status = true;
        } else {
            Debug.log("DomainXMLBase.addAgentClasspath() - Error: "
                    + "Missing '" + STR_JAVA_CONFIG_ELEMENT + "' element.");
        }
        return status;
    }

    private String appendAgentClassPath(String classpath,
            IStateAccess stateAccess) throws Exception {
        StringBuilder sb = new StringBuilder(classpath);
        String[] agentEntries = getAgentClasspathEntries(stateAccess);
        int count = agentEntries.length;

        for (int i = 0; i < count; i++) {
            sb.append(STR_SERVER_CLASSPATH_SEP);
            sb.append(agentEntries[i]);
        }
        String resultClasspath = sb.toString();
        Debug.log("DomainXMLBase.appendAgentClassPath() Original "
                + "classpath: " + classpath + "\nResult classpath: "
                + resultClasspath);

        return resultClasspath;
    }

    private void addAgentJVMOptions(XMLDocument domainXMLDoc,
            XMLElement javaConfig, IStateAccess stateAccess) throws Exception {
        Iterator<String> iter = getAgentJVMOptions(stateAccess).iterator();
        StringBuilder sb = new StringBuilder(256);
        while (iter.hasNext()) {
            String option = iter.next();
            sb.append("<jvm-options>").append(option).append("</jvm-options>");
            XMLElement jvmOption = domainXMLDoc.newElementFromXMLFragment(
                    sb.toString());
            javaConfig.addChildElement(jvmOption, false);
            sb.delete(0, sb.length()); // clear the buffer
        }
    }

    public boolean addAgentRealm(XMLDocument domainXMLDoc,
            XMLElement instanceConfig, IStateAccess stateAccess) throws Exception {
        boolean status = true;
        XMLElement securityService = getUniqueElement(
                STR_SECURITY_SERVICE_ELEMENT, instanceConfig);
        if (securityService != null) {
            String preAgentDefaultRealm = securityService.getAttributeValue(
                    STR_DEFAULT_REALM_ATTR);
            if (preAgentDefaultRealm != null) { // Save the value, so that it
                // can be used during un-install
                stateAccess.put(STR_PRE_AGENT_DEFAULT_REALM,
                        preAgentDefaultRealm);
            }

            // Update the Default Realm to be the agentRealm
            securityService.updateAttribute(STR_DEFAULT_REALM_ATTR,
                    STR_AGENT_REALM);

            // Create and Add the Agent Realm element
            StringBuilder sb = new StringBuilder();
            sb.append("<auth-realm name=\"agentRealm\" ");
            sb.append("classname=\"").append(STR_AGENT_REALM_CLASS_NAME);
            sb.append("\">");
            sb.append("<property name=\"jaas-context\" value=\"agentRealm\"/>");
            sb.append("</auth-realm>");
            XMLElement agentRealm = domainXMLDoc.newElementFromXMLFragment(
                    sb.toString());
            securityService.addChildElementAt(agentRealm, 0, true);
        } else {
            Debug.log("DomainXMLBase.addAgentRealm() - Error: "
                    + "Unable to add agentRealm. Missing '"
                    + STR_SECURITY_SERVICE_ELEMENT + "' element in server.xml");
            status = false;
        }

        return status;
    }

    public boolean addLifecycleModule(XMLDocument domainXMLDoc, String serverInstanceName, IStateAccess stateAccess) throws Exception {
        boolean status = true;

    	// Obtain the domain root <domain> element
        XMLElement domainRoot = domainXMLDoc.getRootElement();
        // Get the <applications> element
        XMLElement applicationsElement = getUniqueElement(
                STR_APPLICATIONS_ELEMENT, domainRoot);
        
        XMLElement serverElement = getServer(domainRoot, serverInstanceName);

        if (applicationsElement != null && serverElement != null) {
            // Create lifecycleModule element
            XMLElement lifecycleModule = domainXMLDoc.newElementFromXMLFragment(
            		getLifecycleModuleStanza(stateAccess));
            applicationsElement.addChildElementAt(lifecycleModule, 0, true);

            // Create application reference
            XMLElement applicationRef = domainXMLDoc.newElementFromXMLFragment(getLifecycleModuleApplicationRef(stateAccess));
            serverElement.addChildElementAt(applicationRef,  0, true);
        } else {
            Debug.log("DomainXMLBase.addLifecycleModule() - Error: "
                    + "Unable to add lifecycleModule. Missing '"
                    + STR_APPLICATIONS_ELEMENT + "' or 'server' element in server.xml");
            status = false;
        }

        return status;
    }
    
    
    private String getLifecycleModuleStanza(IStateAccess stateAccess) {
    	
        StringBuilder sb = new StringBuilder();

        if (VersionChecker.isGlassFishv3(stateAccess)) {
	        sb.append("<application name=\"" + STR_AGENT_LIFECYCLE_MODULE + "\" object-type=\"user\">");
	        sb.append("<property name=\"is-failure-fatal\" value=\"false\"></property>");
	        sb.append("<property name=\"class-name\" value=\"" + STR_AGENT_LIFECYCLE_MODULE_CLASS + "\"></property>");
	        sb.append("<property name=\"isLifecycle\" value=\"true\"></property>");
	        sb.append("</application>");
        } else {
	        sb.append("<lifecycle-module name=\"" + STR_AGENT_LIFECYCLE_MODULE + "\" ");
	        sb.append("class-name=\"" + STR_AGENT_LIFECYCLE_MODULE_CLASS + "\" ");
	        sb.append("is-failure-fatal=\"false\" ");
	        sb.append("object-type=\"user\"/>");
        	
        }
        return sb.toString();
    }
    
    private String getLifecycleModuleApplicationRef(IStateAccess stateAccess) {
    	
        if (VersionChecker.isGlassFishv3(stateAccess)) {
        	return "<application-ref ref=\"" + STR_AGENT_LIFECYCLE_MODULE + "\" virtual-servers=\"server\"></application-ref>";
        } else {
        	return "<application-ref disable-timeout-in-minutes=\"30\" enabled=\"true\" lb-enabled=\"false\" ref=\"" + STR_AGENT_LIFECYCLE_MODULE + "\"/>";
        }
    }
    
    
    public boolean removeAgentClasspath(XMLElement instanceConfig,
            IStateAccess stateAccess) throws Exception {
        boolean status = false;

        XMLElement javaConfig = getUniqueElement(STR_JAVA_CONFIG_ELEMENT,
                instanceConfig);
        if (javaConfig != null) {
            if (!VersionChecker.isGlassFishv3(stateAccess)) {
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
                } else {
                    Debug.log("DomainXMLBase.removeAgentClasspath() - "
                            + "Error: Missing '" + classPathTag + "' "
                            + "attribute");
                }
            }
            // Remove the Agent JVMOptions. If error occurs Exception is thrown
            removeAgentJVMOptions(javaConfig, stateAccess);
            status = true;
        } else {
            Debug.log("DomainXMLBase.removeAgentClasspath() - Error:"
                    + " Missing '" + STR_JAVA_CONFIG_ELEMENT + "' element.");
        }

        return status;
    }

    private String deleteAgentClasspath(String classpath,
            IStateAccess stateAccess) {
        StringBuilder sb = new StringBuilder(classpath);
        String[] agentEntries = getAgentClasspathEntries(stateAccess);
        int count = agentEntries.length;

        for (int i = 0; i < count; i++) {
            Debug.log("ConfigureDomainXMLBase.deleteAgentClasspath() deleting"
                    + " token: " + agentEntries[i]);
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
        Debug.log("DomainXMLBase.deleteAgentClasspath() Original "
                + "classpath: " + classpath + "\nResult classpath: "
                + resultClasspath);

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
                if (value != null
                        && getAgentJVMOptions(stateAccess).contains(value.trim())) { // Agent JVM Option - Delete it.
                    element.delete();
                }
            }
        }
    }

    public boolean removeAgentRealm(XMLDocument domainXMLDoc,
            XMLElement domainRoot, IStateAccess stateAccess) throws Exception {
        boolean status = true;
        XMLElement securityService = getUniqueElement(
                STR_SECURITY_SERVICE_ELEMENT, domainRoot);
        if (securityService != null) {
            String preAgentDefaultRealm = (String) stateAccess.get(
                    STR_PRE_AGENT_DEFAULT_REALM);

            if (preAgentDefaultRealm != null && preAgentDefaultRealm.length() > 0) {
                securityService.updateAttribute(STR_DEFAULT_REALM_ATTR, preAgentDefaultRealm);
            } else {
                securityService.removeAttribute(STR_DEFAULT_REALM_ATTR);
            }


            // Remove the Agent Realm 
            XMLElement agentRealm = getAgentRealm(securityService);
            if (agentRealm != null) {
                agentRealm.delete();
            }
        } else {
            Debug.log("DomainXMLBase.removeAgentRealm() - Error: Unable to remove "
                    + "agentRealm. Missing '" + STR_SECURITY_SERVICE_ELEMENT
                    + "' element in server.xml");
            status = false;
        }
        return status;
    }
    
    public boolean removeLifecycleModule(XMLDocument domainXMLDoc, String serverInstanceName, IStateAccess stateAccess) throws Exception {
        boolean status = true;

    	// Obtain the domain root <domain> element
        XMLElement domainRoot = domainXMLDoc.getRootElement();
        // Get the <applications> element
        XMLElement applicationsElement = getUniqueElement(
                STR_APPLICATIONS_ELEMENT, domainRoot);
        
        XMLElement serverElement = getServer(domainRoot, serverInstanceName);
        
        if (applicationsElement != null && serverElement != null) {
	        XMLElement lifecycleModuleElement = getLifecycleModule(applicationsElement, stateAccess);
	        if (lifecycleModuleElement != null) {
	        	lifecycleModuleElement.delete();
	        }
	       	
	        XMLElement applicationRef = getLifecycleModuleReference(serverElement);
	        if (applicationRef != null) {
	        	applicationRef.delete();
	        }
        } else {
            Debug.log("DomainXMLBase.removeLifecycleModule() - Error: "
                    + "Unable to remove lifecycleModule. Missing '"
                    + STR_APPLICATIONS_ELEMENT + "' or 'server' element in server.xml");
            status = false;
        }
        return status;
    }

    public String getInstanceConfigName(XMLElement domainRoot, String serverName) {
        
    	XMLElement serverElement = getServer(domainRoot, serverName);
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
    
    
    private XMLElement getServer(XMLElement domainRoot, String serverName) {
        XMLElement serversElement = getUniqueElement(STR_SERVERS_ELEMENT,
                domainRoot);
        XMLElement serverElement = getElement(serversElement,
                STR_SERVER_ELEMENT, STR_NAME_ATTR, serverName);

        return serverElement;
    }
    
    private XMLElement getAgentRealm(XMLElement securityService) {
        return getElement(securityService, STR_AUTH_REALM_ELEMENT,
                STR_NAME_ATTR, STR_AGENT_REALM);
    }
    
    private XMLElement getLifecycleModule(XMLElement applications, IStateAccess stateAccess) {
    	
    	String moduleElement = null;
    	
    	if (VersionChecker.isGlassFishv3(stateAccess)) {
    		moduleElement = STR_APPLICATION_ELEMENT_V3;
    	} else {
    		moduleElement = STR_APPLICATION_ELEMENT_V2;
    	}

    	return getElement(applications, moduleElement,
                STR_NAME_ATTR, STR_AGENT_LIFECYCLE_MODULE);
    }
    
    private XMLElement getLifecycleModuleReference(XMLElement server) {
    	return getElement(server, STR_APPLICATION_REF_ELEMENT, STR_REF_ATTR, STR_AGENT_LIFECYCLE_MODULE);
    }
    

    public XMLElement getElement(XMLElement parent, String elementName,
            String attrName, String attrValue) {
        XMLElement result = null;
        ArrayList list = parent.getNamedChildElements(elementName);
        if (list != null && list.size() > 0) {
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
            String libPath = ConfigUtil.getLibPath();
            String localeDir = ConfigUtil.getLocaleDirPath();

            String remoteHomeDir = (String) stateAccess.get(
                    STR_REMOTE_AGENT_INSTALL_DIR_KEY);
            // get the agent install directory on a remote instance
            if (remoteHomeDir != null && remoteHomeDir.trim().length() > 0) {
                libPath = remoteHomeDir + FILE_SEP + INSTANCE_LIB_DIR_NAME;
                localeDir = remoteHomeDir + FILE_SEP + INSTANCE_LOCALE_DIR_NAME;
                Debug.log("DomainXMLBase.getAgentClassPathEntries: Modified libPath = "
                        + libPath);
            }

            String[] entries = null;
            if (getCoexistFlag(stateAccess)) {
                entries = new String[]{
                            libPath + FILE_SEP + STR_AGENT_JAR,
                            localeDir};
            } else {
                entries = new String[]{
                            libPath + FILE_SEP + STR_AGENT_JAR,
                            libPath + FILE_SEP + STR_FM_CLIENT_SDK_JAR,
                            localeDir};
            }

            for (int i = 0; i < entries.length; i++) {
                Debug.log("DomainXMLBase.getAgentClasspathEntries(): "
                        + "next entry: " + entries[i]);
            }
            _agentClasspathEntries = entries;
        }

        return _agentClasspathEntries;
    }

    protected String getConfigDirPath() {
        return ConfigUtil.getConfigDirPath();
    }

    private Set<String> getAgentJVMOptions(IStateAccess stateAccess) {
        if (_agentJVMOptions == null) {
            _agentJVMOptions = new TreeSet(); // An Ordered set
            if (!getCoexistFlag(stateAccess)) {
                // get the agent install directory on a remote instance
                StringBuilder sb = new StringBuilder();
                sb.append("-D");
                sb.append(IAgentConfigurationConstants.CONFIG_JVM_OPTION_NAME);
                sb.append('=');
                sb.append(ConfigUtil.getHomePath()).append('/');
                sb.append(stateAccess.getInstanceName()).append('/');
                sb.append(INSTANCE_CONFIG_DIR_NAME);

                _agentJVMOptions.add(sb.toString());
                _agentJVMOptions.add(STR_LOG_COMPATMODE_OPTION);
            }
        }
        Debug.log("DomainXMLBase.getAgentJVMOptions(): options: "
                + _agentJVMOptions);
        return _agentJVMOptions;
    }
    private Boolean _coexistFlag;
    private String[] _agentClasspathEntries;
    private Set<String> _agentJVMOptions;
    public static final String STR_LOG_CONFIG_FILE_OPTION_PREFIX =
            "-Djava.util.logging.config.file=";
    public static final String STR_LOG_COMPATMODE_OPTION =
            "-DLOG_COMPATMODE=Off";
    public static final String STR_SERVERS_ELEMENT = "servers";
    public static final String STR_CONFIGS_ELEMENT = "configs";
    public static final String STR_CONFIG_ELEMENT = "config";
    public static final String STR_SERVER_ELEMENT = "server";
    public static final String STR_JVM_OPTIONS_ELEMENT = "jvm-options";
    public static final String STR_JAVA_CONFIG_ELEMENT = "java-config";
    public static final String STR_SECURITY_SERVICE_ELEMENT =
            "security-service";
    public static final String STR_APPLICATION_ELEMENT_V3 = "application";
    public static final String STR_APPLICATION_ELEMENT_V2 = "lifecycle-module";
    public static final String STR_APPLICATIONS_ELEMENT = "applications";
    public static final String STR_APPLICATION_REF_ELEMENT = "application-ref";
    
    public static final String STR_AUTH_REALM_ELEMENT = "auth-realm";
    public static final String STR_PROPERTY_ELEMENT = "property";
    public static final String STR_CLASSPATH_ATTR = "server-classpath";
    public static final String STR_CLASSPATH_SUFFIX_ATTR = "classpath-suffix";
    public static final String STR_DEFAULT_REALM_ATTR = "default-realm";
    public static final String STR_CLASS_NAME_ATTR = "classname";
    public static final String STR_CONFIG_REL_ATTR = "config-ref";
    public static final String STR_NAME_ATTR = "name";
    public static final String STR_REF_ATTR = "ref";
    
    public static final String STR_VALUE_ATTR = "value";
    public static final String STR_SERVER_CLASSPATH_SEP =
            "${path.separator}";
    public static final String STR_AGENT_JAR = "agent.jar";
    public static final String STR_FM_CLIENT_SDK_JAR = "openssoclientsdk.jar";
    public static final String STR_AGENT_REALM = "agentRealm";
    public static final String STR_AGENT_LIFECYCLE_MODULE = "ASLifeCycleListener";
    public static final String STR_AGENT_LIFECYCLE_MODULE_CLASS="org.forgerock.agents.appserver.v81.ASLifeCycleListener";
    
    public static final String STR_JAAS_CONTEXT = "jaas-context";
    public static final String STR_SERVER_INSTANCE_NAME_KEY = "INSTANCE_NAME";
    public static final String STR_PRE_AGENT_DEFAULT_REALM =
            "PRE_AGENT_DEFAULT_REALM";
}
