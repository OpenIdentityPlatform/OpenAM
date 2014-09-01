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
 * $Id: ConfigureWebAppsTask.java,v 1.2 2008/11/28 12:36:21 saueree Exp $
 */


package com.sun.identity.agents.tools.tomcat.v6;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.ITask;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.util.ConfigUtil;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.xml.XMLDocument;
import com.sun.identity.install.tools.util.xml.XMLElement;

import java.io.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class ConfigureWebAppsTask extends WebAppsTaskBase implements ITask {
    public static final String LOC_TSK_MSG_CONFIGURE_WEB_APPS_EXECUTE =
    	"TSK_MSG_CONFIGURE_WEB_APPS_EXECUTE";
    public static final String LOC_TSK_MSG_CONFIGURE_WEB_APPS_ROLLBACK =
    	"TSK_MSG_CONFIGURE_WEB_APPS_ROLLBACK";

    /**
     * Adds the &lt;login-config&gt; and &lt;form-login-config&gt; elements to
     * the <code>web.xml</code> file of the supplied applications
     *
     * @param appsDir
     *            is the server webapps directory containing the applications
     * @param apps
     *            is a String Array of the names of the Server Applications
     * @param data
     *            state info
     * @return true if update is successful
     */
    public boolean execute(
        String name,
        IStateAccess stateAccess,
        Map properties) throws InstallException {
        boolean status = true;

        try {
            ArrayList appContexts = (ArrayList) stateAccess.get(
                    STR_WEB_APP_CONTEXT_PATH_LIST);

            if ((appContexts != null) && !appContexts.isEmpty()) {
                Debug.log(
                    "ConfigureWebAppsTask.execute(): Instrumenting " +
                    "webApplication contexts: "
                    + appContexts.toString());

                status = updateApps(
                        appContexts,
                        stateAccess);
                status = updateAgentProps(stateAccess) && status;
            } else {
                Debug.log(
                    "ConfigureWebAppsTask.execute(): No web " +
                    "applications to instrument");
            }
        } catch (Exception ex) {
            Debug.log(
                "ConfigureWebAppsTask.execute(): encountered exception "
                + ex.getMessage(),
                ex);
            status = false;
        }

        return status;
    }

    public LocalizedMessage getExecutionMessage(
        IStateAccess stateAccess,
        Map properties) {
        LocalizedMessage message = LocalizedMessage.get(
                LOC_TSK_MSG_CONFIGURE_WEB_APPS_EXECUTE,
                STR_TOMCAT_GROUP);

        return message;
    }

    public LocalizedMessage getRollBackMessage(
        IStateAccess stateAccess,
        Map properties) {
        LocalizedMessage message = LocalizedMessage.get(
                LOC_TSK_MSG_CONFIGURE_WEB_APPS_ROLLBACK,
                STR_TOMCAT_GROUP);

        return message;
    }

    public boolean rollBack(
        String name,
        IStateAccess stateAccess,
        Map properties) throws InstallException {
        boolean status = false;
        status = super.unconfigureWebApps(stateAccess);

        return status;
    }

    private boolean updateApps(
        ArrayList apps,
        IStateAccess stateAccess) {
        String appXmlPath = null;
        XMLElement element = null;
        int index = 0;
        boolean result = true;
        boolean foundLoginConfig = false;

        try {
            for (int i = 0; i < apps.size(); i++) {
                appXmlPath = (String) apps.get(i);
                Debug.log(
                    "ConfigureWebAppsTask.updateApps(): Modifying : "
                    + appXmlPath);

                File webxml = new File(appXmlPath);
                FileUtils.backupFile(
                    appXmlPath,
                    PRE_AGENT_BACKUP_SUFFIX);

                XMLDocument xmldoc = new XMLDocument(webxml);
                xmldoc.setIndentDepth(2);
                xmldoc.setNoValueIndent();

                if (!super.skipFilter(stateAccess)) {
                    Debug.log(
                        "ConfigureWebAppsTask.updateApps(): Adding filter to"
                        + appXmlPath);
                    result = result && addFilterElement(xmldoc);
                } else {
                    Debug.log(
                        "ConfigureWebAppsTask.updateApps(): Skipping " +
                        "filter addition for "
                        + appXmlPath);
                }

                if (!result) {
                    break;
                }

                if (!replaceLoginConfigElement(
                            xmldoc,
                            stateAccess,
                            appXmlPath)) {
                    index = findIndexToAddElem(xmldoc);
                    addLoginConfigElem(
                        xmldoc,
                        index);
                }

                xmldoc.store();
            }
        } catch (Exception ex) {
            Debug.log(
                "ConfigureWebAppsTask.updateApps(): encountered exception "
                + ex.getMessage(),
                ex);
            result = false;
        }

        return result;
    } // end updateApps

    private boolean replaceLoginConfigElement(
        XMLDocument xmldoc,
        IStateAccess stateAccess,
        String appXmlPath) throws Exception {
        boolean result = false;
        XMLElement element;

        ArrayList elements = xmldoc.getRootElement()
                                   .getChildElements();

        for (int index = 0; index < elements.size(); index++) {
            element = (XMLElement) elements.get(index);

            Debug.log(
                "ConfigureWebAppsTask.updateApps(): Processing element ["
                + index + "] : " + element.getName());

            if (element.getName()
                           .equals(ELEMENT_LOGIN_CONFIG)) {
                Debug.log(
                    "ConfigureWebAppsTask.updateApps(): Found existing " +
                    "login-config element : "
                    + element.toString());

                // save data
                stateAccess.put(
                    appXmlPath,
                    element.toXMLString());
                element.delete();

                Debug.log(
                    "ConfigureWebAppsTask.updateApps(): Adding agent " +
                    "login-config element");

                addLoginConfigElem(
                    xmldoc,
                    index);

                result = true;

                break;
            }
        }

        return result;
    }

    private int findIndexToAddElem(XMLDocument xmldoc) {
        int index = 0;
        XMLElement element;

        HashMap map = new HashMap();

        map.put(
            ELEMENT_SEC_ROLE,
            ELEMENT_SERVLET);
        map.put(
            ELEMENT_SEC_ROLE,
            ELEMENT_SERVLET_MAPPING);
        map.put(
            ELEMENT_SEC_ROLE,
            ELEMENT_SEC_ROLE);
        map.put(
            ELEMENT_ENV_ENTRY,
            ELEMENT_ENV_ENTRY);

        ArrayList childElements = xmldoc.getRootElement()
                                        .getChildElements();

        for (; index < childElements.size(); index++) {
            element = (XMLElement) childElements.get(index);

            if (map.containsKey(element.getName())) {
                break;
            }
        }

        return index;
    }

    private XMLElement addLoginConfigElem(
        XMLDocument xmldoc,
        int index) throws Exception {
        XMLElement loginConfigElement = xmldoc.newElement(
                ELEMENT_LOGIN_CONFIG);

        xmldoc.getRootElement()
              .addChildElementAt(
            loginConfigElement,
            index,
            true);

        loginConfigElement.addChildElement(
            xmldoc.newElement(
                ELEMENT_AUTH_METHOD,
                VALUE_AUTH_METHOD_FORM),
            true);

        loginConfigElement.addChildElement(
            xmldoc.newElement(
                ELEMENT_REALM,
                VALUE_REALM_NAME),
            true);

        createFormLoginConfigElem(
            xmldoc,
            loginConfigElement);

        return loginConfigElement;
    }

    private XMLElement createFormLoginConfigElem(
        XMLDocument xmldoc,
        XMLElement parent) throws Exception {
        XMLElement formLoginCfgElement = xmldoc.newElement(
                ELEMENT_FORM_LOGIN_CONFIG);

        parent.addChildElement(
            formLoginCfgElement,
            true);

        // add child elements to <form-login-config>
        formLoginCfgElement.addChildElement(
            xmldoc.newElement(
                ELEMENT_FORM_LOGIN_PAGE,
                VALUE_FORM_LOGIN_PAGE),
            true);

        formLoginCfgElement.addChildElement(
            xmldoc.newElement(
                ELEMENT_FORM_ERROR_PAGE,
                VALUE_FORM_ERROR_PAGE),
            true);

        return formLoginCfgElement;
    }

    private boolean updateAgentProps(IStateAccess state) {
        boolean result = false;

        StringBuffer agentPropFileBuff = new StringBuffer(
                ConfigUtil.getHomePath());
        agentPropFileBuff.append(FILE_SEP);
        agentPropFileBuff.append(state.getInstanceName());
        agentPropFileBuff.append(FILE_SEP);
        agentPropFileBuff.append(AMAGENT_PROPS_FILE_PATH);

        String agentPropFile = agentPropFileBuff.toString();
        ArrayList webAppPaths = (ArrayList) state.get(STR_WEB_APPS_PATH);

        try {
            File configProp = new File(agentPropFile);

            if (configProp.exists() && configProp.canRead()
                    && configProp.canWrite()) {
                result = true;
            } else {
                Debug.log(
                    "ConfigureWebAppsTask.updateAgentProps(): Cannot " +
                    "read or write " + agentPropFile);

                return result;
            }

            Debug.log(
                "ConfigureWebAppsTask.updateAgentProps(): Modifying "
                + agentPropFile);

            String appContextPath;
            String appContext;
            String formLoginPage;
            String formErrorPage;

            for (int index = 0; index < webAppPaths.size(); index++) {
                appContextPath = (String) webAppPaths.get(index);

                appContext = appContextPath.substring(
                        appContextPath.lastIndexOf(File.separatorChar) + 1,
                        appContextPath.length());

                Debug.log(
                    "ConfigureWebAppsTask.updateAgentProps(): " +
                    "configuring context " + appContext);

                // com.sun.identity.agents.config.filter.mode[manager]
                //=J2EE_POLICY
                result = result
                    && FileUtils.addMapProperty(
                        agentPropFile,
                        KEY_FILTER_MODE_MAP_PROP,
                        appContext,
                        STRING_KEY_J2EE_MODE);

                formLoginPage = STR_FORWARD_SLASH + appContext
                    + VALUE_FORM_LOGIN_PAGE;
                formErrorPage = STR_FORWARD_SLASH + appContext
                    + VALUE_FORM_ERROR_PAGE;

                // com.sun.identity.agents.config.login.form[0] =
                // /manager/AMLogin.html
                result = result
                    && FileUtils.addListProperty(
                        agentPropFile,
                        KEY_LOGIN_FORMLIST_PROP,
                        formLoginPage);

                // com.sun.identity.agents.config.login.error.uri[0] =
                // /admin/AMError.html
                result = result
                    && FileUtils.addListProperty(
                        agentPropFile,
                        KEY_LOGIN_ERRORLIST_PROP,
                        formErrorPage);

                if (appContext.equalsIgnoreCase(STR_ADMIN_APP_CONTEXT)) {
                	//add logout uri for admin app
                	result = result
                    && FileUtils.addMapProperty(
                        agentPropFile,
                        KEY_LOGOUT_URI_MAP_PROP,
                        appContext,
                        STR_ADMIN_APP_CONTEXT_LOGOUT_URI);
                }
            }
        } catch (Exception ex) {
            Debug.log(
                "ConfigureWebAppsTask.updateSystemProps(): " +
                "encountered exception "
                + ex.getMessage() + " adding properties to "
                + agentPropFile);

            result = false;
        }

        return result;
    }
}
