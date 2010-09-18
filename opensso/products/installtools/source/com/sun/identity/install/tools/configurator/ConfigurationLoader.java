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
 * $Id: ConfigurationLoader.java,v 1.5 2008/08/29 20:23:39 leiming Exp $
 *
 */
package com.sun.identity.install.tools.configurator;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.lang.Boolean;

import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.xml.XMLDocument;
import com.sun.identity.install.tools.util.xml.XMLElement;

public class ConfigurationLoader {

    public ConfigurationLoader() throws InstallException {
        File configXML = getConfigurationFile();
        if (configXML.exists()) {
            loadConfigurationFile(configXML);
        } else {
            Debug.log("ConfigurationLoader : Error - Configuration file '"
                    + getConfigurationFile() + "' not found!");
            throw new InstallException(LocalizedMessage.get(
                    ERROR_CONFIG_FILE_NOT_FOUND,
                    new Object[] { getConfigurationFile() }));
        }
    }

    public InstallRunInfo getInstallRunInfo() {
        return _installRunInfo;
    }

    public InstallRunInfo getUnInstallRunInfo() {
        return _uninstallRunInfo;
    }

    public InstallRunInfo getMigrateRunInfo() {
        return _migrateRunInfo;
    }
    
    public InstallRunInfo getCustomInstallRunInfo() {
        return _customInstallRunInfo;
    }
    
    private void loadConfigurationFile(File configFile) throws InstallException
    {       XMLDocument xmlDoc = null;
        try {
            xmlDoc = new XMLDocument(configFile);
        } catch (Exception e) {
            throw new InstallException(LocalizedMessage
                    .get(ERROR_CONFIG_FILE_PARSING), e);
        }
        XMLElement root = xmlDoc.getRootElement();
        initClassPrefix(root);
        initInstanceFinder(root);
        initWelcomeMessage(root);
        initExitMessage(root);
        initInstall(root);
        initUninstall(root);
        initMigrate(root);
        initCustomInstall(root);
    }

    private void initWelcomeMessage(XMLElement root) throws InstallException {
        XMLElement welcomeElement = getUniqueElement(STR_WELCOME_ELEMENT, 
                root);
        I18NInfo i18ninfo = getI18NInfo(welcomeElement);
        setWelcomeMessageInfo(i18ninfo);
    }

    private void initExitMessage(XMLElement root) throws InstallException {
        XMLElement exitElement = getUniqueElement(STR_EXIT_ELEMENT, root);
        I18NInfo i18ninfo = getI18NInfo(exitElement);
        setExitMessageInfo(i18ninfo);
    }

    private void initUninstall(XMLElement root) throws InstallException {
        XMLElement uninstallElement = getUniqueElement(STR_UNINSTALL, root);
        ArrayList commonInteractions = getCommonInteractions(uninstallElement);
        ArrayList instanceInteractions = getInstanceInteractions(
                uninstallElement);
        ArrayList commonTasks = getCommonTasks(uninstallElement);
        ArrayList instanceTasks = getInstanceTasks(uninstallElement);

        InstallRunInfo info = new InstallRunInfo(false,
                getHomeDirLocatorClass(), getInstanceFinderInteractions(),
                commonInteractions, instanceInteractions, commonTasks,
                instanceTasks, getWelcomeMessageInfo(), getExitMessageInfo(),
                false);
        setUninstallRunInfo(info);
    }

    private void initMigrate(XMLElement root) throws InstallException {
        XMLElement migrateElement = getUniqueElement(STR_MIGRATE, root);
        ArrayList commonInteractions = getCommonInteractions(migrateElement);
        ArrayList instanceInteractions = getInstanceInteractions(
                migrateElement);
        ArrayList commonTasks = getCommonTasks(migrateElement);
        ArrayList instanceTasks = getInstanceTasks(migrateElement);

        InstallRunInfo info = new InstallRunInfo(false,
                getHomeDirLocatorClass(), getInstanceFinderInteractions(),
                commonInteractions, instanceInteractions, commonTasks,
                instanceTasks, getWelcomeMessageInfo(), getExitMessageInfo(),
                false);
        setMigrateRunInfo(info);
    }    
    
    private void initCustomInstall(XMLElement root) throws InstallException {
        XMLElement customInstallElement = 
		   getUniqueElement(STR_CUSTOM_INSTALL, root);
        if (customInstallElement != null) {
            ArrayList commonInteractions = 
	       getCommonInteractions(customInstallElement);
            ArrayList instanceInteractions = getInstanceInteractions(
                customInstallElement);
            ArrayList commonTasks = getCommonTasks(customInstallElement);
            ArrayList instanceTasks = getInstanceTasks(customInstallElement);

            InstallRunInfo info = new InstallRunInfo(false,
                getHomeDirLocatorClass(), getInstanceFinderInteractions(),
                commonInteractions, instanceInteractions, commonTasks,
                instanceTasks, getWelcomeMessageInfo(), getExitMessageInfo(),
                false);
            setCustomInstallRunInfo(info);
        }
    }
    
    
    private void initInstall(XMLElement root) throws InstallException {
        XMLElement installElement = getUniqueElement(STR_INSTALL, root);
        ArrayList commonInteractions = getCommonInteractions(installElement);
        ArrayList instanceInteractions = getInstanceInteractions(
                installElement);
        ArrayList commonTasks = getCommonTasks(installElement);
        ArrayList instanceTasks = getInstanceTasks(installElement);

        InstallRunInfo info = new InstallRunInfo(true,
                getHomeDirLocatorClass(), getInstanceFinderInteractions(),
                commonInteractions, instanceInteractions, commonTasks,
                instanceTasks, getWelcomeMessageInfo(), getExitMessageInfo(),
                true);
        setIntallRunInfo(info);
    }

    private ArrayList getCommonTasks(XMLElement base) throws InstallException {
        XMLElement ctElement = getUniqueElement(STR_COMMON_TASKS, base);
        return getTasks(ctElement);
    }

    private ArrayList getInstanceTasks(XMLElement base) throws InstallException
    {
        XMLElement itElement = getUniqueElement(STR_INSTANCE_TASKS, base);
        return getTasks(itElement);
    }

    private ArrayList getTasks(XMLElement base) throws InstallException {
        ArrayList result = new ArrayList();
        ArrayList taskElements = base.getNamedChildElements(STR_TASK);
        if (taskElements != null) {
            Iterator it = taskElements.iterator();
            while (it.hasNext()) {
                XMLElement nextTask = (XMLElement) it.next();
                TaskInfo info = getTaskInfo(nextTask);
                result.add(info);
            }
        }
        return result;
    }

    private TaskInfo getTaskInfo(XMLElement taskInfoElement)
            throws InstallException {
        String name = taskInfoElement.getAttributeValue(STR_NAME);
        String className = getClassName(taskInfoElement);
        Map propertiesMap = readPropElements(taskInfoElement);
        return new TaskInfo(name, propertiesMap, className);
    }

    private ArrayList getInstanceInteractions(XMLElement base)
            throws InstallException {
        XMLElement iiElement = getUniqueElement(STR_INSTANCE_INTERACTIONS, 
                base);
        return getInteractions(iiElement);
    }

    private ArrayList getCommonInteractions(XMLElement base)
            throws InstallException {
        XMLElement ciElement = getUniqueElement(STR_COMMON_INTERACTIONS, base);
        return getInteractions(ciElement);
    }

    private ArrayList getInteractions(XMLElement base) throws InstallException 
    {
        ArrayList result = new ArrayList();
        ArrayList interactionElements = base
                .getNamedChildElements(STR_INTERACTION);
        if (interactionElements != null) {
            Iterator it = interactionElements.iterator();
            while (it.hasNext()) {
                XMLElement nextInteraction = (XMLElement) it.next();
                InteractionInfo info = getInteractionInfo(nextInteraction);
                result.add(info);
            }
        }

        return result;
    }

    private void initInstanceFinder(XMLElement root) throws InstallException {
        XMLElement instanceFinderElement = getUniqueRequiredElement(
                STR_INSTANCE_FINDER, root);
        XMLElement hdlElement = getUniqueRequiredElement(STR_HOME_DIR_LOCATOR,
                instanceFinderElement);
        setHomeDirLocatorClass(getClassName(hdlElement));

        XMLElement ifiElement = getUniqueRequiredElement(
                STR_INSTANCE_FINDER_INTERACTIONS, instanceFinderElement);

        setInstanceFinderInteractions(getInteractions(ifiElement));
    }

    private I18NInfo getI18NInfo(XMLElement element) throws InstallException {
        XMLElement i18nElement = getUniqueRequiredElement(STR_I18N_ELEMENT,
                element);
        String key = i18nElement.getAttributeValue(STR_KEY);
        String group = i18nElement.getAttributeValue(STR_GROUP);

        return new I18NInfo(key, group);
    }

    private SkipIfInfo getSkipIfInfo(XMLElement element)
            throws InstallException {
        SkipIfInfo skipIfInfo = null;

        XMLElement skipIfElement = getUniqueElement(STR_SKIP_IF_ELEMENT,
                element);
        if (skipIfElement != null) {
            String key = skipIfElement.getAttributeValue(STR_KEY);
            ArrayList values = readValueElements(skipIfElement);

            String ignorecase = skipIfElement.getAttributeValue(
                    STR_IGNORECASE);
            if (ignorecase != null) {
                boolean ignoreCase = Boolean.valueOf(ignorecase)
                    .booleanValue();
                skipIfInfo = new SkipIfInfo(key, values, ignoreCase);
            } else {
                skipIfInfo = new SkipIfInfo(key, values, false);
            }
        }

        return skipIfInfo;
    }

    private InteractionInfo getInteractionInfo(XMLElement element)
            throws InstallException {
        String lookupKey = element.getAttributeValue(STR_LOOKUP_KEY);
        if (lookupKey == null || lookupKey.trim().length() == 0) {
            Debug.log("ConfigurationLoader: Error null lookup key for "
                    + "element " + element.getName());
            throw new InstallException(LocalizedMessage
                    .get(ERROR_CONFIG_FILE_PARSING));
        }
        I18NInfo i18nInfo = getI18NInfo(element);

        XMLElement dvfElement = getUniqueElement(STR_DEFAULT_VALUE_FINDER,
                element);
        DefaultValueFinderInfo defaultValueFinderInfo = 
            getDefaultValueFinderInfo(dvfElement);

        boolean required = true;
        String requiredString = element.getAttributeValue(STR_REQUIRED);
        if (requiredString != null
                && requiredString.trim().equalsIgnoreCase(STR_FALSE)) {
            required = false;
        }

        boolean persistent = true;
        String persistentString = element.getAttributeValue(STR_PERSISTENT);
        if (persistentString != null
                && persistentString.trim().equalsIgnoreCase(STR_FALSE)) {
            persistent = false;
        }

        boolean display = true;
        String displayString = element.getAttributeValue(STR_DISPLAY);
        if (displayString != null
                && displayString.trim().equalsIgnoreCase(STR_FALSE)) {
            display = false;
        }
        
        String interType = STR_INSTALL_INTER;
        String type = element.getAttributeValue(STR_INTER_TYPE);

        SkipIfInfo skipIfInfo = getSkipIfInfo(element);

        String valueNormalizerClass = getValueNormalizerClass(element);

        InteractionInfo result = new InteractionInfo(lookupKey, skipIfInfo,
                i18nInfo, defaultValueFinderInfo, required, persistent,
                interType, display, valueNormalizerClass);

        XMLElement validations = getUniqueElement(STR_VALIDATIONS, element);
        ArrayList list = validations.getNamedChildElements(STR_VALIDATION);
        if (list != null && list.size() > 0) {
            Iterator it = list.iterator();
            while (it.hasNext()) {
                XMLElement validation = (XMLElement) it.next();
                ValidationInfo validationInfo = getValidationInfo(validation);
                result.addValidationInfo(validationInfo);
            }
        }

        return result;
    }

    private String getValueNormalizerClass(XMLElement element)
            throws InstallException {
        String valueNormalizerClass = null;
        XMLElement valueNormalizer = getUniqueElement(STR_VALUE_NORMALIZER,
                element);
        if (valueNormalizer != null) {
            valueNormalizerClass = getClassName(valueNormalizer);
        }

        return valueNormalizerClass;
    }

    private ValidationInfo getValidationInfo(XMLElement element)
            throws InstallException {
        String className = getClassName(element);
        String name = element.getAttributeValue(STR_NAME);
        Map properties = readPropElements(element);
        return new ValidationInfo(name, properties, className);
    }

    private Map readPropElements(XMLElement element) throws InstallException {

        ArrayList list = element.getNamedChildElements(STR_PROPERTY);
        Map propsMap = new HashMap();
        if (list != null && (list.size() > 0)) {
            for (int i = 0; i < list.size(); i++) {
                XMLElement propElem = (XMLElement) list.get(i);
                if (propElem != null) {
                    String propName = propElem.getAttributeValue(STR_NAME);
                    String propValue = propElem.getAttributeValue(STR_VALUE);
                    if ((propName != null) && (propName.length() > 0)) {
                        propsMap.put(propName, propValue);
                    }
                }
            }
        }
        return propsMap;
    }

    private ArrayList readValueElements(XMLElement element)
            throws InstallException {
        ArrayList values = new ArrayList();

        ArrayList list = element.getNamedChildElements(STR_VALUE);
        if (list != null && (list.size() > 0)) {
            for (int i = 0; i < list.size(); i++) {
                XMLElement valueElem = (XMLElement) list.get(i);
                if (valueElem != null) {
                    String value = valueElem.getValue();
                    values.add(value);
                }
            }
        }
        return values;
    }

    private DefaultValueFinderInfo getDefaultValueFinderInfo(
            XMLElement element) throws InstallException {
        DefaultValueFinderInfo defInfo = null;
        if (element != null) {
            String className = getClassName(element);
            String staticValue = null;
            XMLElement staticValueElement = getUniqueElement(STR_STATIC,
                    element);
            if (staticValueElement != null) {
                staticValue = staticValueElement.getAttributeValue(STR_VALUE);
            }
            defInfo = new DefaultValueFinderInfo(className, staticValue);
        }
        return defInfo;
    }

    private void initClassPrefix(XMLElement root) throws InstallException {
        XMLElement classPrefix = getUniqueRequiredElement(STR_CLASS_PREFIX,
                root);
        HashMap classPrefixMap = new HashMap();
        ArrayList types = classPrefix.getChildElements();
        if (types != null) {
            Iterator it = types.iterator();
            while (it.hasNext()) {
                XMLElement nextType = (XMLElement) it.next();
                if (nextType.getName().equals(STR_PACAKGE)) {
                    String typeValue = nextType.getAttributeValue(STR_TYPE);
                    String name = nextType.getAttributeValue(STR_NAME);
                    if (typeValue == null || typeValue.trim().length() == 0
                            || name == null || name.trim().length() == 0) {
                        Debug.log("ConfigurationLoader: Invalid class "
                                + "prefix : " + typeValue + ": " + name);
                        throw new InstallException(LocalizedMessage
                                .get(ERROR_CONFIG_FILE_PARSING));
                    }
                    classPrefixMap.put(typeValue, name);
                } else {
                    Debug.log("ConfigurationLoader: Invalid class prefix "
                            + "type: " + nextType.getName());
                    throw new InstallException(LocalizedMessage
                            .get(ERROR_CONFIG_FILE_PARSING));
                }
            }
        }
        setClassPrefixMap(classPrefixMap);
    }

    private String getClassName(XMLElement element) throws InstallException {
        XMLElement classElement = getUniqueRequiredElement(STR_CLASS, element);
        String name = classElement.getAttributeValue(STR_NAME);
        String type = classElement.getAttributeValue(STR_TYPE);
        return getClassName(name, type);
    }

    private String getClassName(String classNameShort, String pkgType)
            throws InstallException {
        String result = classNameShort;
        if (pkgType != null && pkgType.trim().length() > 0) {
            String pkgPrefix = (String) getClassPrefixMap().get(pkgType);
            if (pkgPrefix != null && pkgPrefix.trim().length() > 0) {
                result = pkgPrefix + "." + classNameShort;
            } else {
                Debug
                        .log("ConfigurationLoader: Invalid class type: "
                                + pkgType);
                throw new InstallException(LocalizedMessage
                        .get(ERROR_CONFIG_FILE_PARSING));
            }
        }
        return result;
    }

    private XMLElement getUniqueElement(String name, XMLElement parent) {
        XMLElement result = null;
        ArrayList list = parent.getNamedChildElements(name);
        if (list != null && list.size() == 1) {
            result = (XMLElement) list.get(0);
        }

        return result;
    }

    private XMLElement getUniqueRequiredElement(String name, XMLElement parent)
            throws InstallException {
        XMLElement result = null;
        if (parent != null) {
            result = getUniqueElement(name, parent);
            if (result == null) {
                Debug.log("ConfigurationLoader: Unique element: " + name
                            + " not found in element: " + parent.getPath());
                throw new InstallException(LocalizedMessage
                        .get(ERROR_CONFIG_FILE_PARSING));
            }
        } else {
            Debug.log("ConfigurationLoader: parent element: " + name
                            + " not found : ");
	}
        return result;
    }

    private HashMap getClassPrefixMap() {
        return _classPrefixMap;
    }

    private void setClassPrefixMap(HashMap map) {
        _classPrefixMap = map;
    }

    private String getHomeDirLocatorClass() {
        return _homeDirLocatorClass;
    }

    private void setHomeDirLocatorClass(String className) {
        _homeDirLocatorClass = className;
    }

    private ArrayList getInstanceFinderInteractions() {
        return _instanceFinderInteractions;
    }

    private void setInstanceFinderInteractions(ArrayList interactionList) {
        _instanceFinderInteractions = interactionList;
    }

    private void setIntallRunInfo(InstallRunInfo info) {
        _installRunInfo = info;
    }

    private void setUninstallRunInfo(InstallRunInfo info) {
        _uninstallRunInfo = info;
    }
    
    private void setMigrateRunInfo(InstallRunInfo info) {
        _migrateRunInfo = info;
    }
    
    private void setCustomInstallRunInfo(InstallRunInfo info) {
        _customInstallRunInfo = info;
    }

    private I18NInfo getWelcomeMessageInfo() {
        return _welcomeMessageInfo;
    }

    private I18NInfo getExitMessageInfo() {
        return _exitMessageInfo;
    }

    private void setWelcomeMessageInfo(I18NInfo welcomeMess) {
        _welcomeMessageInfo = welcomeMess;
    }

    private void setExitMessageInfo(I18NInfo exitMess) {
        _exitMessageInfo = exitMess;
    }

    private HashMap _classPrefixMap;

    private String _homeDirLocatorClass;

    private ArrayList _instanceFinderInteractions;

    private InstallRunInfo _installRunInfo;

    private InstallRunInfo _uninstallRunInfo;
    
    private InstallRunInfo _migrateRunInfo;
    
    private InstallRunInfo _customInstallRunInfo;

    private I18NInfo _welcomeMessageInfo;

    private I18NInfo _exitMessageInfo;

    ////////////////////////////////////////////////////////////////////////

    /**
     * This method will be extrnalized when integrating with productadmin
     * @return
     */
    private File getConfigurationFile() {
        URL resUrl = ClassLoader.getSystemResource(CONFIG_FILE_NAME);
        if (resUrl == null) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl != null) {
                resUrl = cl.getResource(CONFIG_FILE_NAME);
            }
        }

        if (resUrl == null) {
            throw new RuntimeException("Failed to get configuration file:"
                    + CONFIG_FILE_NAME);
        }

        return new File(resUrl.getPath());
    }

    public static final String CONFIG_FILE_NAME = "configure.xml";

    public static final String STR_CLASS_PREFIX = "class-prefix";

    public static final String STR_PACAKGE = "package";

    public static final String STR_TYPE = "type";

    public static final String STR_NAME = "name";

    public static final String STR_INSTANCE_FINDER = "instance-finder";

    public static final String STR_HOME_DIR_LOCATOR = "home-dir-locator";

    public static final String STR_CLASS = "class";

    public static final String STR_INSTANCE_FINDER_INTERACTIONS = 
        "instance-finder-interactions";

    public static final String STR_INTERACTION = "interaction";

    public static final String STR_LOOKUP_KEY = "lookupkey";

    public static final String STR_I18N_ELEMENT = "i18n";

    public static final String STR_SKIP_IF_ELEMENT = "skipIf";

    public static final String STR_VALUE_NORMALIZER = "value-normalizer";

    public static final String STR_DEFAULT_VALUE_FINDER = 
        "default-value-finder";

    public static final String STR_STATIC = "static";

    public static final String STR_VALUE = "value";

    public static final String STR_IGNORECASE = "ignorecase";

    public static final String STR_VALIDATIONS = "validations";

    public static final String STR_VALIDATION = "validation";

    public static final String STR_PROPERTY = "property";

    public static final String STR_REQUIRED = "required";
    
    public static final String STR_DISPLAY = "optional-display";

    public static final String STR_PERSISTENT = "persistent";

    public static final String STR_INTER_TYPE = "type";

    public static final String STR_INSTALL_INTER = "install";

    public static final String STR_FALSE = "false";

    public static final String STR_TRUE = "true";

    public static final String STR_INSTALL = "install";

    public static final String STR_COMMON_INTERACTIONS = 
        "common-interactions";

    public static final String STR_WELCOME_ELEMENT = "welcome-message";

    public static final String STR_EXIT_ELEMENT = "exit-message";

    public static final String STR_INSTANCE_INTERACTIONS = 
        "instance-interactions";

    public static final String STR_KEY = "key";

    public static final String STR_GROUP = "group";

    public static final String STR_TASK = "task";

    public static final String STR_COMMON_TASKS = "common-tasks";

    public static final String STR_INSTANCE_TASKS = "instance-tasks";

    public static final String STR_UNINSTALL = "uninstall";

    public static final String STR_MIGRATE = "migrate";
    
    public static final String STR_CUSTOM_INSTALL = "custom-install";
    
    public static final String ERROR_CONFIG_FILE_NOT_FOUND = 
        "error_config_file_not_found";

    public static final String ERROR_CONFIG_FILE_PARSING = 
        "error_config_file_parsing";

}
