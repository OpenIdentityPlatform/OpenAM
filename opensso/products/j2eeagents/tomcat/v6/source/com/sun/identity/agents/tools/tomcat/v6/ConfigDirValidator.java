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
 * $Id: ConfigDirValidator.java,v 1.2 2008/11/28 12:36:21 saueree Exp $
 */

package com.sun.identity.agents.tools.tomcat.v6;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.configurator.ValidationResult;
import com.sun.identity.install.tools.configurator.ValidationResultStatus;
import com.sun.identity.install.tools.configurator.ValidatorBase;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.xml.XMLDocument;
import com.sun.identity.install.tools.util.xml.XMLElement;

import java.io.File;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class ConfigDirValidator extends ValidatorBase implements IConstants,
    IConfigKeys {

    public ConfigDirValidator() throws InstallException {
        super();
    }

    /**
     * Method isConfigDirValid
     *
     *
     * @param dir
     * @param props
     * @param IStateAccess
     *
     * @return ValidationResult
     *
     */
    public ValidationResult isConfigDirValid(
        String configDir,
        Map props,
        IStateAccess state) {
        ValidationResultStatus validRes = ValidationResultStatus.STATUS_FAILED;
        LocalizedMessage returnMessage = null;

        if ((configDir != null) && (configDir.trim()
                                                 .length() >= 0)) {
            // The config dir has been normalized to have "/" only
            String serverXmlFile = configDir + STR_FORWARD_SLASH
                + STR_SERVER_XML;
            String webXmlFile = configDir + STR_FORWARD_SLASH
                + STR_WEB_XML;

            Debug.log(
                "ConfigDirValidator.isConfigDirValid(): Server XML " +
                "file path is: " + serverXmlFile);
            Debug.log(
                "ConfigDirValidator.isConfigDirValid(): Global Web XML file " +
                "path is: " + webXmlFile);

            if (isValid(configDir) && isValid(serverXmlFile)
                    && isValid(webXmlFile)) {
                if (fetchAppContexts(
                            serverXmlFile,
                            state)) {
                    // store inst config dir, domain.xml, login.conf,
                    // server.policy
                    // file locations in install state
                    state.put(
                        STR_KEY_TOMCAT_SERVER_XML_FILE,
                        serverXmlFile);
                    state.put(
                        STR_KEY_TOMCAT_GLOBAL_WEB_XML_FILE,
                        webXmlFile);
                    state.put(
                        STR_KEY_TOMCAT_SERVER_CONFIG_DIR,
                        configDir);
                    state.put(
                    	STR_KEY_CATALINA_BASE_DIR,
                        new File(configDir).getParentFile().getAbsolutePath());

                    returnMessage = LocalizedMessage.get(
                            LOC_VA_MSG_TOMCAT_VAL_CONFIG_DIR,
                            STR_TOMCAT_GROUP,
                            new Object[] { configDir });

                    validRes = ValidationResultStatus.STATUS_SUCCESS;
                }
            }
        }

        if (validRes.getIntValue() == ValidationResultStatus.INT_STATUS_FAILED)
        {
            returnMessage = LocalizedMessage.get(
                    LOC_VA_WRN_TOMCAT_IN_VAL_CONFIG_DIR,
                    STR_TOMCAT_GROUP,
                    new Object[] { configDir });
        }

        Debug.log(
            "ConfigDirValidator.isConfigDirValid(): Is Tomcat Server " +
            "Config dir " + configDir + " valid ? " + validRes.isSuccessful());

        return new ValidationResult(
            validRes,
            null,
            returnMessage);
    }

    private boolean isValid(String fileName) {
        boolean valid = false;

        File file = new File(fileName);

        if (file.exists() && file.canRead() && file.canWrite()) {
            valid = true;
        } else {
            if (!file.exists()) {
                Debug.log(
                    "ConfigDirValidator.isValid(): " + fileName
                    + " does not exist");
            } else {
                Debug.log(
                    "ConfigDirValidator.isValid(): " + fileName
                    + " does not have read and write permissions");
            }
        }

        return valid;
    }

    public void initializeValidatorMap() throws InstallException {
        Class[] paramObjs = { String.class, Map.class, IStateAccess.class };

        try {
            getValidatorMap()
                .put(
                "VALID_TOMCAT_CONFIG_DIR",
                this.getClass().getMethod(
                    "isConfigDirValid",
                    paramObjs));
        } catch (NoSuchMethodException nsme) {
            Debug.log(
                "ConfigDirValidator: NoSuchMethodException "
                + "thrown while loading method :",
                nsme);
            throw new InstallException(
                LocalizedMessage.get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND),
                nsme);
        } catch (SecurityException se) {
            Debug.log(
                "ConfigDirValidator: SecurityException thrown "
                + "while loading method :",
                se);
            throw new InstallException(
                LocalizedMessage.get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND),
                se);
        } catch (Exception ex) {
            Debug.log(
                "ConfigDirValidator: Exception thrown while "
                + "loading method :",
                ex);
            throw new InstallException(
                LocalizedMessage.get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND),
                ex);
        }
    }

    public boolean fetchAppContexts(
        String serverXMLFile,
        IStateAccess state) {
        String catalinaBase = null;
        String appBaseDir = null;
        ArrayList listEngineElements;
        ArrayList listHostElements;
        XMLElement serviceElement;
        boolean result = true;

        try {
            catalinaBase = (new File(serverXMLFile)).getParentFile()
                            .getParent();

            _webAppPaths.clear();

            XMLDocument xmlDoc = new XMLDocument(new File(serverXMLFile));
            XMLElement hostElement;

            ArrayList listServiceElements = xmlDoc.getRootElement()
                                                  .getNamedChildElements(
                    ELEMENT_SERVICE);

            for (int i = 0; i < listServiceElements.size(); i++) {
                serviceElement = (XMLElement) listServiceElements.get(i);
                listEngineElements = serviceElement.getNamedChildElements(
                        ELEMENT_ENGINE);

                if (listEngineElements.size() == 1) {
                    listHostElements = ((XMLElement) listEngineElements
                        .get(0)).getNamedChildElements(ELEMENT_HOST);

                    for (int j = 0; j < listHostElements.size(); j++) {
                        hostElement = (XMLElement) listHostElements.get(j);

                        appBaseDir = catalinaBase + STR_FORWARD_SLASH
                            + hostElement.getAttributeValue(STR_APP_BASE);

                        if ((appBaseDir != null)
                                && (appBaseDir.length() > 0)) {
                            Debug.log(
                                "ConfigDirValidator.getDefaultValue(): " +
                                "Finding contexts for appBase "
                                + appBaseDir + " for host element "
                                + hostElement.getName());
                            addSubDirs(appBaseDir);
                        } else {
                            Debug.log(
                                "ConfigDirValidator.getDefaultValue(): Did " +
                                "not find appBase for host element "
                                + hostElement.getName());
                        }
                    }

                    if (listHostElements.size() == 0) {
                        Debug.log(
                            "ConfigDirValidator.getDefaultValue(): Did not " +
                            "find any Host element for Engine element " +
                            "for Service "
                            + serviceElement.getName());
                    }
                } else {
                    Debug.log(
                        "ConfigDirValidator.getDefaultValue(): Did not find " +
                        "any engine element for Service "
                        + serviceElement.getName());
                }
            }

            if (listServiceElements.size() == 0) {
                Debug.log(
                    "ConfigDirValidator.getDefaultValue(): Did not find any " +
                    "service elements for "
                    + serverXMLFile);
            }

            // if we are in CATALINA HOME, then we need to instrument webapps
            // in the server directory as well
            String serverDir = catalinaBase + STR_FORWARD_SLASH
                + STR_TOMCAT_SERVER_DIR;

            if (FileUtils.isDirValid(serverDir)) {
                Debug.log(
                    "ConfigDirValidator.getDefaultValue(): Found tomcat " +
                    "server directory "
                    + serverDir);

                File[] subDirs = new File(serverDir).listFiles();

                if (subDirs.length > 0) {
                    Debug.log(
                        "ConfigDirValidator.getDefaultValue(): Adding " +
                        "webapps under directory "
                        + subDirs[0].getAbsolutePath());
                    addSubDirs(
                        serverDir + STR_FORWARD_SLASH + STR_WEBAPP_DIR);
                } else {
                    Debug.log(
                        "ConfigDirValidator.getDefaultValue(): no webapps " +
                        "found under directory "
                        + serverDir);
                }
            }

            state.put(
                STR_WEB_APPS_PATH,
                _webAppPaths);
        } catch (Exception ex) {
            Debug.log(
                "ConfigDirValidator.getDefaultValue(): Encountered "
                + "exception: " + ex.getMessage(),
                ex);

            result = false;
        }

        return result;
    }

    private void addSubDirs(String webAppDirPath) {
        File file = new File(webAppDirPath);

        if (file.isDirectory()) {
            File[] webApps = file.listFiles();
            String appContextPath = null;

            for (int index = 0; index < webApps.length; index++) {
                if ((webApps[index].isDirectory())
                        && _listApps.contains(webApps[index].getName())) {
                    appContextPath = webApps[index].getAbsolutePath();
                    _webAppPaths.add(appContextPath);

                    Debug.log(
                        "ConfigDirValidator.addSubDirs(): Adding " +
                        "application context "
                        + appContextPath);
                } else {
                    Debug.log(
                        "ConfigDirValidator.addSubDirs(): Ignoring "
                        + appContextPath);
                }
            }
        }
    }

    private ArrayList getWebAppPaths() {
        return _webAppPaths;
    }

    private void setWebAppPaths(ArrayList webAppPaths) {
        _webAppPaths = webAppPaths;
    }

    private ArrayList getListPaths() {
        return _listApps;
    }

    private void setListPaths(ArrayList listApps) {
    	_listApps = listApps;
    }

    private static String STR_SERVER_XML = "server.xml";
    private static String STR_WEB_XML = "web.xml";
    private static String[] _instrumentedApps = { "manager",
 				"admin", "host-manager"};

    /*
     * Localized constants
     */
    private static String LOC_VA_MSG_TOMCAT_VAL_CONFIG_DIR =
    	"VA_MSG_TOMCAT_VAL_CONFIG_DIR";
    private static String LOC_VA_WRN_TOMCAT_IN_VAL_CONFIG_DIR =
    	"VA_WRN_TOMCAT_IN_VAL_CONFIG_DIR";

    /** Hashmap of Validator names and integers */
    Map validMap = new HashMap();
    ArrayList _webAppPaths = new ArrayList();
    ArrayList _listApps = new ArrayList(Arrays.asList(_instrumentedApps));

}
