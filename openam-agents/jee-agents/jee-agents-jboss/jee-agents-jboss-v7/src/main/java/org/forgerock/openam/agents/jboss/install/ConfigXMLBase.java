/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock, Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
package org.forgerock.openam.agents.jboss.install;

import static com.sun.identity.agents.arch.IAgentConfigurationConstants.*;
import static com.sun.identity.install.tools.admin.ICommonToolsConstants.*;
import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.util.ConfigUtil;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.xml.XMLDocument;
import com.sun.identity.install.tools.util.xml.XMLElement;
import java.io.File;
import java.text.MessageFormat;
import java.util.List;
import static org.forgerock.openam.agents.jboss.install.InstallerConstants.*;

/**
 * Provides functionality for manipulating JBoss configuration XML file (both for adding and removing agent related
 * settings).
 *
 * @author Peter Major
 */
public class ConfigXMLBase {

    private static final String EXTENSIONS = "extensions";
    private static final String GLOBAL_MODULES = "global-modules";
    private static final String PROFILE = "profile";
    private static final String SUBSYSTEM = "subsystem";
    private static final String SECURITY_DOMAINS = "security-domains";
    private static final String SYSTEM_PROPERTIES = "system-properties";
    private static final String XMLNS = "xmlns";
    private static final String EE_NAMESPACE_PREFIX = "urn:jboss:domain:ee:";
    private static final String SECURITY_NAMESPACE_PREFIX = "urn:jboss:domain:security:";
    private static final String AGENT_MODULE_NAME = "org.forgerock.openam.agent";
    private static final String AM_REALM = "AMRealm";
    private static final String JVM_PROPERTY_TAG = "<property name=\"" + CONFIG_JVM_OPTION_NAME + "\" value=\"{0}\"/>";
    private static final String SYSTEM_PROPERTIES_TAG = "<system-properties>{0}</system-properties>";
    private static final String MODULE_TAG = "<module name=\"" + AGENT_MODULE_NAME + "\" />";
    private static final String GLOBAL_MODULES_TAG = "<global-modules>" + MODULE_TAG + "</global-modules>";
    private static final String SECURITY_DOMAIN_TAG = "<security-domain name=\"" + AM_REALM + "\">"
            + " <authentication>"
            + "  <login-module code=\"com.sun.identity.agents.jboss.v40.AmJBossLoginModule\" flag=\"required\"/>"
            + " </authentication>"
            + "</security-domain>";

    /**
     * Performs the following steps:
     * <ul>
     *  <li>Sets the JVM property for the agent, so the configuration can be found by the agent.</li>
     *  <li>Adds the agent as a global module if it needs to (depends on user input).</li>
     *  <li>Adds a JBoss security domain, so the JAAS integration can work.</li>
     * </ul>
     *
     * @param state Agent installer state.
     * @return <code>true</code> if performing the changes was successful, otherwise <code>false</code>.
     */
    public boolean performChanges(IStateAccess state) {
        StringBuilder path = new StringBuilder(100);
        path.append(ConfigUtil.getHomePath()).append(File.separator);
        path.append(state.getInstanceName()).append(File.separator);
        path.append(INSTANCE_CONFIG_DIR_NAME);
        String property = MessageFormat.format(JVM_PROPERTY_TAG, path.toString());
        try {
            XMLDocument xml = getXMLDocument(state);
            XMLElement xmlRoot = xml.getRootElement();
            addSystemProperty(xml, xmlRoot, property);

            if (Boolean.valueOf((String) state.get(GLOBAL_MODULE))) {
                addGlobalModule(xml, xmlRoot);
            }
            addSecurityDomain(xml, xmlRoot);
            xml.store();
        } catch (Exception ex) {
            Debug.log("An error occurred while updating the config XML", ex);
            return false;
        }
        return true;
    }

    private void addSystemProperty(XMLDocument xml, XMLElement xmlRoot, String property) throws Exception {
        List<XMLElement> existingProperties = xmlRoot.getNamedChildElements(SYSTEM_PROPERTIES);
        if (existingProperties.isEmpty()) {
            //there is no element for existingProperties just yet.
            List<XMLElement> elements = xmlRoot.getChildElements();
            int idx;
            for (idx = 0; idx < elements.size(); idx++) {
                if (elements.get(idx).getName().equals(EXTENSIONS)) {
                    XMLElement props = xml.newElementFromXMLFragment(
                            MessageFormat.format(SYSTEM_PROPERTIES_TAG, property));
                    xmlRoot.addChildElementAt(props, idx + 1, true);
                    break;
                }
            }
        } else {
            existingProperties.get(0).addChildElementAt(xml.newElementFromXMLFragment(property), 0);
        }
    }

    private void addGlobalModule(XMLDocument xml, XMLElement xmlRoot) throws Exception {
        XMLElement subsystem = getSubsystemForNamespace(xmlRoot, EE_NAMESPACE_PREFIX);
        List<XMLElement> existingGlobalModules = subsystem.getNamedChildElements(GLOBAL_MODULES);
        if (existingGlobalModules.isEmpty()) {
            XMLElement globalModules = xml.newElementFromXMLFragment(GLOBAL_MODULES_TAG);
            subsystem.addChildElementAt(globalModules, 0, true);
        } else {
            existingGlobalModules.get(0).addChildElementAt(xml.newElementFromXMLFragment(MODULE_TAG), 0, true);
        }
    }

    private void addSecurityDomain(XMLDocument xml, XMLElement xmlRoot) throws Exception {
        XMLElement subsystem = getSubsystemForNamespace(xmlRoot, SECURITY_NAMESPACE_PREFIX);
        List<XMLElement> existingSecurityDomains = subsystem.getNamedChildElements(SECURITY_DOMAINS);
        existingSecurityDomains.get(0).addChildElementAt(xml.newElementFromXMLFragment(SECURITY_DOMAIN_TAG), 0, true);
    }

    /**
     * Rolls back the installation steps:
     * <ul>
     *  <li>Removes the agent's JVM property.</li>
     *  <li>Removes the agent's global module if present.</li>
     *  <li>Removes the agent's security domain.</li>
     * </ul>
     *
     * @param state Agent installer state.
     * @return <code>true</code> if rollbacking all the changes was successful, <code>false</code> otherwise.
     */
    public boolean rollbackChanges(IStateAccess state) {
        try {
            XMLDocument xml = getXMLDocument(state);
            XMLElement xmlRoot = xml.getRootElement();
            removeSystemProperty(xmlRoot);
            removeGlobalModule(xmlRoot);
            removeSecurityDomain(xmlRoot);
            xml.store();
        } catch (Exception ex) {
            Debug.log("An error occured while trying to roll back changes", ex);
            return false;
        }
        return true;
    }

    private void removeSystemProperty(XMLElement xmlRoot) throws Exception {
        List<XMLElement> propertiesRoot = xmlRoot.getNamedChildElements(SYSTEM_PROPERTIES);
        removeSettingOrParentTagWithName(propertiesRoot, CONFIG_JVM_OPTION_NAME);
    }

    private void removeGlobalModule(XMLElement xmlRoot) throws Exception {
        XMLElement subsystem = getSubsystemForNamespace(xmlRoot, EE_NAMESPACE_PREFIX);
        List<XMLElement> globalModulesRoot = subsystem.getNamedChildElements(GLOBAL_MODULES);
        removeSettingOrParentTagWithName(globalModulesRoot, AGENT_MODULE_NAME);
    }

    private void removeSecurityDomain(XMLElement xmlRoot) throws Exception {
        XMLElement subsystem = getSubsystemForNamespace(xmlRoot, SECURITY_NAMESPACE_PREFIX);
        List<XMLElement> securityDomainsRoot = subsystem.getNamedChildElements(SECURITY_DOMAINS);
        removeSettingOrParentTagWithName(securityDomainsRoot, AM_REALM);
    }

    private void removeSettingOrParentTagWithName(List<XMLElement> elements, String name) throws Exception {
        if (elements.isEmpty()) {
            Debug.log("Unable to find element declaration, cannot remove " + name);
        } else {
            List<XMLElement> childElements = elements.get(0).getChildElements();
            if (childElements != null) {
                for (XMLElement childElement : childElements) {
                    if (name.equals(childElement.getAttributeValue("name"))) {
                        if (childElements.size() == 1) {
                            //this is the only setting, let's remove the whole global-modules block
                            elements.get(0).delete();
                        } else {
                            childElement.delete();
                        }
                        break;
                    }
                }
            }
        }
    }

    /**
     * Returns the path to the JBoss configuration file.
     *
     * @param state Agent installer state which holds the agent install parameters.
     * @return The path to the JBoss configuration file.
     */
    protected String getConfigFilePath(IStateAccess state) {
        return (String) state.get(CONFIG_FILE);
    }

    private XMLDocument getXMLDocument(IStateAccess state) throws Exception {
        String configFile = getConfigFilePath(state);
        Debug.log("Trying to read XML configuration from file: " + configFile);
        return new XMLDocument(new File(configFile));
    }

    private XMLElement getSubsystemForNamespace(XMLElement xmlRoot, String namespace) {
        List<XMLElement> profile = xmlRoot.getNamedChildElements(PROFILE);
        if (!profile.isEmpty()) {
            List<XMLElement> subsystems = profile.get(0).getNamedChildElements(SUBSYSTEM);
            for (XMLElement subsystem : subsystems) {
                String xmlns = subsystem.getAttributeValue(XMLNS);
                if (xmlns != null && xmlns.startsWith(namespace)) {
                    return subsystem;
                }
            }
        }
        throw new IllegalStateException("Unable to find EE subsystem in configuration XML");
    }
}