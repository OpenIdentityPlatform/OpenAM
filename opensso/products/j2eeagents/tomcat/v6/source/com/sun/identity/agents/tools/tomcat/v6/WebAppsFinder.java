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
 * $Id: WebAppsFinder.java,v 1.2 2008/11/28 12:36:23 saueree Exp $
 */

package com.sun.identity.agents.tools.tomcat.v6;

import com.sun.identity.install.tools.configurator.IDefaultValueFinder;
import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.util.xml.XMLDocument;
import com.sun.identity.install.tools.util.xml.XMLElement;

import java.io.File;

import java.util.ArrayList;


public class WebAppsFinder implements IDefaultValueFinder, IConstants,
    IConfigKeys {
    private ArrayList _webAppPaths = new ArrayList();

    public String getDefaultValue(
        String key,
        IStateAccess state,
        String value) {
        String defaultVal = null;
        String catalinaBase = null;
        String appBaseDir = null;
        ArrayList listEngineElements;
        ArrayList listHostElements;
        XMLElement serviceElement;

        try {
            String serverXMLFile = (String) state.get(
                    STR_KEY_TOMCAT_SERVER_XML_FILE);
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
                                "WebAppsFinder.getDefaultValue: " +
                                "Finding contexts for appBase "
                                + appBaseDir + " for host element "
                                + hostElement.getName());
                            addSubDirs(appBaseDir);
                        } else {
                            Debug.log(
                                "WebAppsFinder.getDefaultValue: Did not " +
                                "find appBase for host element "
                                + hostElement.getName());
                        }
                    }

                    if (listHostElements.size() == 0) {
                        Debug.log(
                            "WebAppsFinder.getDefaultValue: Did not find " +
                            "any Host element for Engine element for Service "
                            + serviceElement.getName());
                    }
                } else {
                    Debug.log(
                        "WebAppsFinder.getDefaultValue: Did not find any " +
                        "engine element for Service"
                        + serviceElement.getName());
                }
            }

            if (listServiceElements.size() == 0) {
                Debug.log(
                    "WebAppsFinder.getDefaultValue: Did not find any " +
                    "service elements for "
                    + serverXMLFile);
            }

            // if we are in CATALINA HOME, then we need to instrument webapps
            // in the server directory as well
            String serverDir = catalinaBase + STR_FORWARD_SLASH
                + STR_TOMCAT_SERVER_DIR;

            if (FileUtils.isDirValid(serverDir)) {
                Debug.log(
                    "WebAppsFinder.getDefaultValue: Found tomcat " +
                    "server directory "
                    + serverDir);

                File[] subDirs = new File(serverDir).listFiles();

                if (subDirs.length > 0) {
                    Debug.log(
                        "WebAppsFinder.getDefaultValue: Adding webapps " +
                        "under directory "
                        + subDirs[0].getAbsolutePath());
                    addSubDirs(
                        serverDir + STR_FORWARD_SLASH + STR_WEBAPP_DIR);
                } else {
                    Debug.log(
                        "WebAppsFinder.getDefaultValue: no webapps " +
                        "found under directory "
                        + serverDir);
                }
            }
        } catch (Exception ex) {
            Debug.log(
                "WebAppsFinder.getDefaultValue: Encountered "
                + "exception " + ex.getMessage(),
                ex);
        }

        state.put(
            STR_WEB_APPS_PATH,
            _webAppPaths);

        if (_webAppPaths.size() > 0) {
            Object[] values = _webAppPaths.toArray();
            defaultVal = (String) values[0];
        } else {
            defaultVal = catalinaBase + STR_FORWARD_SLASH + STR_WEBAPP_DIR;
        }

        return defaultVal;
    }

    private void addSubDirs(String webAppDirPath) {
        File file = new File(webAppDirPath);

        if (file.isDirectory()) {
            File[] webApps = file.listFiles();
            String appContextPath = null;

            for (int index = 0; index < webApps.length; index++) {
                if (webApps[index].isDirectory()) {
                    appContextPath = webApps[index].getAbsolutePath();
                    _webAppPaths.add(appContextPath);

                    Debug.log(
                        "WebAppsFinder.addSubDirs: Adding application context "
                        + appContextPath);
                } else {
                    Debug.log(
                        "WebAppsFinder.addSubDirs: Ignoring "
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
}
