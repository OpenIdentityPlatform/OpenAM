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
 * $Id: ServerXMLBase.java,v 1.3 2008/12/16 00:15:34 leiming Exp $
 *
 */

package com.sun.identity.agents.tools.websphere;

import java.util.ArrayList;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.util.ConfigUtil;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.xml.XMLDocument;
import com.sun.identity.install.tools.util.xml.XMLElement;

/**
 * Base class to configure server.xml. Both configure and unconfigure of
 * server.xml tasks use this class.
 *
 */

public abstract class ServerXMLBase implements IConfigKeys, IConstants {
    
    /**
     * Common function to find the server:Process element with server instance
     * name
     */
    public XMLElement findClassPathElement(XMLDocument serverXMLDoc,
            String serverInst, String procDefElemName) {
        
        XMLElement cpElem = null;
        try {
            XMLElement elem =
                    findServerProcElement(serverXMLDoc.getRootElement(), 
                    serverInst);
            if (elem != null) {
                ArrayList list = elem.getNamedChildElements(procDefElemName);
                if ((list != null) && (list.size() > 0)) {
                    XMLElement procDef = (XMLElement) list.get(0);
                    if (procDef != null) {
                        list = procDef.getNamedChildElements(STR_JVM_ENTRIES);
                        if ((list != null) && (list.size() > 0)) {
                            XMLElement jvmEntries = (XMLElement) list.get(0);
                            if (jvmEntries != null) {
                                cpElem = jvmEntries;
                            }
                        }
                    }
                }
            } else {
                Debug.log(
                        "ServerXMLBase.findClassPathElement() - failed to find "
                        + " jvmEntries element for "
                        + " server instance = "
                        + serverInst);
            }
            
        } catch (Exception ex) {
            Debug.log("ServerXMLBase.findClassPathElement() - failed to find "
                    + " jvmEntries element for " + " server instance = "
                    + serverInst, ex);
        }
        return cpElem;
    }
    
    /**
     * The element is in either of two locations - either its a root element or
     * its a immediate child of the root element. WAS changes the process:Server
     * element from immediate child to root element if any changes are made to
     * this XML file through console and there is only one instance of the
     * server in the file
     *
     */
    private XMLElement findServerProcElement(XMLElement parent,
            String attrValue) {
        
        XMLElement serverElem = null;
        String elementName = parent.getName();
        
        if (elementName.equals(STR_PROC_SERVER)) {
            Debug.log("ServerXMLBase.findServerProcElement() - "
                    + "Root element is the server:Process element, name = "
                    + parent.getName());
            serverElem = parent;
        } else {
            serverElem =
                getElement(parent, STR_PROC_SERVER, STR_NAME_ATTR, attrValue);
            if (serverElem != null) {
                Debug.log("ServerXMLBase.findServerProcElement() = "
                        + "Found server:Process element as an immediate child");
            }
        }
        
        return serverElem;
    }
    
    /**
     * Generic function to get an element with attribute name and value
     */
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
    
    /*
     * To simplify the logic, the function first look classpath as an attr and
     * then as an element
     */
    public boolean configureClasspath(XMLDocument doc, XMLElement jvmEntries,
            IStateAccess stateAccess) {
        
        boolean status = true;
        
        try {
            // store in a temp variable for later processing
            String preAgentClasspath =
                    jvmEntries.getAttributeValue(STR_CLASSPATH_ATTR);
            
            int count = jvmEntries.removeAttribute(STR_CLASSPATH_ATTR);
            if (count >= 1) {
                Debug.log("ServerXMLBase.configureClasspath() - " 
                        + "removed XML attr " 
                        + "classpath"
                        + " from jvmEntries element : attr count = " + count);
                if ((preAgentClasspath != null)
                && (preAgentClasspath.length() > 0)) {
                    stateAccess.put(STR_KEY_PRE_AGENT_CP, preAgentClasspath);
                    Debug.log(
                            "ServerXMLBase.configureClasspath() - storing pre "
                            + "agent classpath in install state = "
                            + preAgentClasspath);
                    String finalCp =
                        appendAgentClassPath(preAgentClasspath, stateAccess);
                    Debug.log("ServerXMLBase.configureClasspath() - "
                        + "updating classpath attr " + " with = " + finalCp);
                    jvmEntries.updateAttribute(STR_CLASSPATH_ATTR, finalCp);
                } else {
                    // empty value
                    String finalCp = createAgentClassPath(stateAccess);
                    Debug.log("ServerXMLBase.configureClasspath() - "
                        + "updating classpath attr " + " with = " + finalCp);
                    jvmEntries.updateAttribute(STR_CLASSPATH_ATTR, finalCp);
                }
            } else {
                // classpath not present as attribute but as element
                Debug.log("ServerXMLBase.configureClasspath()- "
                        + "attribute not present,"
                        + "nothing to do : attr count = " + count);
                status = updateClasspathElement(doc, jvmEntries, stateAccess);
            }
        } catch (Exception ex) {
            Debug.log("ServerXMLBase.configureClasspath() - "
                    + " failed to classpath in attr" + " with ex : ", ex);
            status = false;
        }
        
        return status;
        
    }
    
    /**
     * Element gets updated or added
     */
    public boolean updateClasspathElement(XMLDocument doc,
            XMLElement jvmEntries, IStateAccess stateAccess) {
        
        boolean status = true;
        try {
            // Blindly add a new element for agent classpath even if there are
            // n already existing classpath elements (n > 0)
            if (jvmEntries != null) {
                status = addNewClassPathElement(doc, jvmEntries, stateAccess);
            }
        } catch (Exception ex) {
            Debug.log("ServerXMLBase.updateClasspathElement() - "
                    + " failed to update classpath in element" + " with ex : ", 
                    ex);
            status = false;
        }
        
        return status;
    }
    
    /*
     * Generic function to add new classpath element
     */
    public boolean addNewClassPathElement(XMLDocument doc,
            XMLElement jvmEntries, IStateAccess stateAccess) {
        
        boolean status = true;
        try {
            StringBuffer sb = new StringBuffer(256);
            sb.append("<").append(STR_CLASSPATH_ELEM).append(">").append(
                    createAgentClassPath(stateAccess)).append("</").append(
                    STR_CLASSPATH_ELEM).append(">");
            XMLElement classPathElem =
                    doc.newElementFromXMLFragment(sb.toString());
            Debug.log("ServerXMLBase.addNewClassPathElement() - "
                    + "New classpath element added " + 
                    classPathElem.toXMLString());
            jvmEntries.addChildElement(classPathElem);
        } catch (Exception ex) {
            Debug.log("ServerXMLBase.addNewClassPathElement() - "
                    + "exception caught ", ex);
            status = false;
        }
        
        return status;
    }
    
    /**
     *
     * To simplify the logic, will first look for classpath as attr and then as
     * element
     *
     */
    public boolean unConfigureClasspath(XMLDocument doc, XMLElement jvmEntries,
            IStateAccess stateAccess) {
        
        boolean status = true;
        
        try {
            // Store in a temp variable for later processing
            String classpath = jvmEntries.getAttributeValue(STR_CLASSPATH_ATTR);
            // We have to update as an attribute if this check is true
            if ((classpath != null) && (classpath.length() > 0)) {
                Debug.log("ServerXMLBase.unConfigureClasspath() - " + "trying "
                        + "to update classpath as an attribute");
                String preAgentCp =
                        (String) stateAccess.get(STR_KEY_PRE_AGENT_CP);
                if ((preAgentCp != null) && (preAgentCp.length() > 0)) {
                    Debug.log("ServerXMLBase.unConfigureClasspath() - "
                            + "setting pre agent classpath =" + preAgentCp);
                    jvmEntries.updateAttribute(STR_CLASSPATH_ATTR, preAgentCp);
                } else {
                    Debug.log("ServerXMLBase.unConfigureClasspath() - "
                            + "setting agent classpath  to empty value");
                    jvmEntries.updateAttribute(STR_CLASSPATH_ATTR, "");
                }
            } else {
                // unconfigure classpath as an element
                ArrayList list =
                        jvmEntries.getNamedChildElements(STR_CLASSPATH_ELEM);
                if ((list != null) && (list.size() > 0)) {
                    // WAS can create more than one classpath element
                    for (int i = 0; i < list.size(); i++) {
                        XMLElement classpathElem = (XMLElement) list.get(i);
                        // Be sure this is the one to update
                        if ((classpathElem != null)
                        && (classpathElem.getValue() != null)
                        && (classpathElem.getValue().indexOf(
                                ConfigUtil.getLocaleDirPath()) >= 0)) {
                            
                            Debug.log("ServerXMLBase.unConfigureClasspath() - "
                                    + "found "
                                    + "classpath element to unconfigure ="
                                    + classpathElem.toXMLString());
                            // Never know if there was pre agent classpath as
                            // attr before agent install
                            String preAgentClasspath =
                                (String) stateAccess.get(STR_KEY_PRE_AGENT_CP);
                            if ((preAgentClasspath != null)
                            && (preAgentClasspath.length() > 0)) {
                                Debug.log(
                                    "ServerXMLBase.unConfigureClasspath() - "
                                    + "classpath "
                                    + " element updated with = "
                                    + preAgentClasspath);
                                classpathElem.updateValue(preAgentClasspath);
                            } else {
                                // remove the element since no pre agent
                                // classpath found
                                Debug.log(
                                    "ServerXMLBase.unConfigureClasspath() - "
                                    + "classpath element deleted, no previous "
                                    + "entry found ");
                                classpathElem.delete();
                            }
                            break;
                        }
                    }
                }
            }
            
        } catch (Exception ex) {
            Debug.log("ServerXMLBase.unConfigureClasspath() - "
                    + " failed to classpath in attr" + " with ex : ", ex);
            status = false;
        }
        
        return status;
        
    }
    
    /*
     * Function to append agent classpath to pre existing classpath
     */
    
    public String appendAgentClassPath(String classpath,
            IStateAccess stateAccess) {
        StringBuffer sb = new StringBuffer(classpath);
        String[] agentEntries = getAgentClasspathEntries(stateAccess);
        int count = agentEntries.length;
        for (int i = 0; i < count; i++) {
            sb.append(STR_CLASSPATH_SEP);
            sb.append(agentEntries[i]);
        }
        
        String resultClasspath = sb.toString();
        Debug.log("ServerXMLBase.appendAgentClassPath() Original "
                + "classpath: " + classpath + "\nResult classpath: "
                + resultClasspath);
        
        return resultClasspath;
    }
    
    /*
     * Function to get agent classpath
     */
    public String createAgentClassPath(IStateAccess stateAccess) {
        StringBuffer sb = new StringBuffer();
        String[] agentEntries = getAgentClasspathEntries(stateAccess);
        int count = agentEntries.length;
        for (int i = 0; i < count; i++) {
            sb.append(agentEntries[i]);
            if (i < (count - 1)) {
                sb.append(STR_CLASSPATH_SEP);
            }
        }
        String resultClasspath = sb.toString();
        Debug.log("ServerXMLBase.createAgentClassPath() " + "classpath: "
                + resultClasspath);
        
        return resultClasspath;
    }
    
    /*
     * Helper function to get agent classpath entries as an array of strings
     */
    private String[] getAgentClasspathEntries(IStateAccess stateAccess) {
        StringBuffer sb = new StringBuffer(256);
        
        sb.append(ConfigUtil.getHomePath()).append(STR_FILE_SEP);
        sb.append(stateAccess.getInstanceName()).append(STR_FILE_SEP);
        sb.append(STR_CONFIG_DIR_LEAF);
        String instanceConfigDirPath = sb.toString();
        String localeDir = ConfigUtil.getLocaleDirPath();
        
        String[] entries = new String[] { instanceConfigDirPath, localeDir };
        
        return entries;
    }

    /*
     * get new JVM options
     */
    private String getJVMOptions() {
        return  STR_IBM_ENC_JVM_OPTIONS_VALUE +
                STR_LOG_COMPATMODE_OPTION +
                STR_LOG_CONFIG_FILE_OPTION_PREFIX +
                ConfigUtil.getConfigDirPath() + STR_FILE_SEP +
                STR_LOG_CONFIG_FILENAME;
    }

    /*
     * add IBM JVM options
     */
    public boolean configureJVMOptions(XMLDocument doc, XMLElement jvmOptions,
            IStateAccess stateAccess) {
        
        boolean status = true;
        
        try {
            String preJVMOptionsValue =
                    jvmOptions.getAttributeValue(STR_JVM_OPTIONS_NAME);
            String newJVMOptionsValue = getJVMOptions();

            if (preJVMOptionsValue != null && preJVMOptionsValue.length() > 0) {
                if (preJVMOptionsValue.indexOf(newJVMOptionsValue) >= 0) {
                    Debug.log("ServerXMLBase.configureJVMOptions() - "
                            + " IBM JVM options already exist, "
                            + " skip adding them.");
                    return true;
                }
                newJVMOptionsValue =
                        preJVMOptionsValue + " " + newJVMOptionsValue;
            }
            
            jvmOptions.removeAttribute(STR_JVM_OPTIONS_NAME);
            Debug.log("ServerXMLBase.configureJVMOptions() - "
                    + "removed XML attribute JVM Options"
                    + " from jvmEntries element.");
            
            jvmOptions
                    .updateAttribute(STR_JVM_OPTIONS_NAME, newJVMOptionsValue);
            Debug.log("ServerXMLBase.configureJVMOptions() - "
                    + "added IBM JVM options.");
            
        } catch (Exception ex) {
            Debug.log("ServerXMLBase.configureJVMOptions() - "
                    + " failed to add JVM options with exception : ", ex);
            status = false;
        }
        
        return status;
    }
    
    /*
     * remove IBM JVM options
     */
    public boolean unConfigureJVMOptions(XMLDocument doc,
            XMLElement jvmOptions, IStateAccess stateAccess) {
        
        boolean status = true;
        
        try {
            String preJVMOptionsValue =
                    jvmOptions.getAttributeValue(STR_JVM_OPTIONS_NAME);
            String newJVMOptionsValue = getJVMOptions();
            
            if (preJVMOptionsValue == null || preJVMOptionsValue.length() == 0
                    || preJVMOptionsValue.indexOf(newJVMOptionsValue) < 0) {
                Debug.log("ServerXMLBase.unConfigureJVMOptions() - "
                        + " IBM JVM options do not exist, "
                        + " skip removing them.");
                return true;
            }
            newJVMOptionsValue =
                preJVMOptionsValue.replaceAll(newJVMOptionsValue, "").trim();
            
            jvmOptions.removeAttribute(STR_JVM_OPTIONS_NAME);
            Debug.log("ServerXMLBase.unConfigureJVMOptions() - "
                    + "removed XML attribute JVM Options"
                    + " from jvmEntries element.");
            
            jvmOptions
                    .updateAttribute(STR_JVM_OPTIONS_NAME, newJVMOptionsValue);
            Debug.log("ServerXMLBase.unConfigureJVMOptions() - "
                    + "removed IBM JVM options.");
        } catch (Exception ex) {
            Debug.log("ServerXMLBase.unConfigureJVMOptions() - "
                    + " failed to remove JVM options with exception : ", ex);
            status = false;
        }
        
        return status;
    }
    
    public static final String STR_PROC_SERVER = "process:Server";
    public static final String STR_NAME_ATTR = "name";
    public static final String STR_JVM_ENTRIES = "jvmEntries";
    public static final String STR_JVM_OPTIONS_NAME = "genericJvmArguments";
    public static final String STR_IBM_ENC_JVM_OPTIONS_VALUE =
            "-DamKeyGenDescriptor.provider=IBMJCE "
            + "-DamCryptoDescriptor.provider=IBMJCE "
            + "-DamRandomGenProvider=IBMJCE ";
    public static final String STR_LOG_COMPATMODE_OPTION =
            "-DLOG_COMPATMODE=Off ";
    public static final String STR_LOG_CONFIG_FILE_OPTION_PREFIX =
            "-Djava.util.logging.config.file=";
    public static final String STR_LOG_CONFIG_FILENAME =
            "OpenSSOAgentLogConfig.properties";
}
