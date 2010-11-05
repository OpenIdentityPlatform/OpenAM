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
 * $Id: WebAppsTaskBase.java,v 1.2 2008/11/28 12:36:23 saueree Exp $
 */


package com.sun.identity.agents.tools.tomcat.v6;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.xml.XMLDocument;
import com.sun.identity.install.tools.util.xml.XMLElement;

import java.io.File;

import java.util.ArrayList;


public class WebAppsTaskBase extends FilterBase {
    protected WebAppsTaskBase() {
    }

    protected boolean unconfigureWebApps(IStateAccess stateAccess) {
        String appXmlPath = null;
        XMLElement element = null;
        int index = 0;
        boolean result = true;

        ArrayList appContexts = (ArrayList) stateAccess.get(
                STR_WEB_APP_CONTEXT_PATH_LIST);

        if ((appContexts == null) || appContexts.isEmpty()) {
            return result;
        }

        try {
            String[] apps = (String[]) appContexts.toArray(
                    new String[appContexts.size()]);

            for (int i = 0; i < apps.length; i++) {
                boolean match = false;
                appXmlPath = apps[i];

                Debug.log(
                    "WebAppsTaskBase.unconfigureWebApps(): " +
                    "UnInstrumenting application : "
                    + appXmlPath);

                File webxml = new File(appXmlPath);

                if (webxml.exists() && webxml.canRead()
                        && webxml.canWrite()) {
                    XMLDocument xmldoc = new XMLDocument(webxml);
                    xmldoc.setIndentDepth(2);
                    xmldoc.setNoValueIndent();

                    if (!skipFilter(stateAccess)) {
                        Debug.log(
                            "WebAppsTaskBase.unconfigureWebApps(): " +
                            "removing filter addition for webApp "
                            + appXmlPath);
                        result = result
                            && super.removeFilterElement(xmldoc);
                        result = result
                        	&& super.removeFilterMappingElement(xmldoc);

                    } else {
                        Debug.log(
                            "WebAppsTaskBase.unconfigureWebApps(): " +
                            "Skipping filter removal for webApp "
                            + appXmlPath);
                    }

                    XMLElement loginConfigElement = xmldoc.newElement(
                            ELEMENT_LOGIN_CONFIG);
                    ArrayList elements = xmldoc.getRootElement()
                                               .getChildElements();

                    for (index = 0; index < elements.size(); index++) {
                        element = (XMLElement) elements.get(index);

                        Debug.log(
                            "WebAppsTaskBase.unconfigureWebApps(): " +
                            "Processing element ["
                            + index + "] : " + element.getName());

                        if (element.getName()
                                       .equals(ELEMENT_LOGIN_CONFIG)) {
                            Debug.log(
                                "WebAppsTaskBase.unconfigureWebApps(): Found " +
                                "agent login-config element : "
                                + element.toString());
                            element.delete();
                            Debug.log(
                                "WebAppsTaskBase.unconfigureWebApps(): " +
                                "Removing agent login-config element : "
                                + loginConfigElement.toString());

                            match = true;

                            break;
                        }
                    } // end for

                    if (match) {
                        String xmlString = (String) stateAccess.get(
                                appXmlPath);

                        if ((xmlString != null)
                                && (xmlString.length() > 0)) {
                            XMLElement previousRealmElem = xmldoc
                                .newElementFromXMLFragment(xmlString);
                            xmldoc.getRootElement()
                                  .addChildElementAt(
                                previousRealmElem,
                                index);
                        }
                    }

                    xmldoc.store();
                } else {
                    Debug.log(
                        "WebAppsTaskBase.unconfigureWebApps(): "
                        + "Cannot find or write to application web.xml at: "
                        + appXmlPath);
                }
            } // end apps for
        } catch (Exception ex) {
            Debug.log(
                "WebAppsTaskBase.unconfigureWebApps(): encountered exception "
                + ex.getMessage(),
                ex);
            result = false;
        }

        return result;
    }

    protected boolean skipFilter(IStateAccess stateAccess) {
        boolean result = false;

        String installFilterInGlobalWebXML = (String) stateAccess.get(
                STR_KEY_INSTALL_GLOBAL_WEB_XML);

        if (Boolean.valueOf(installFilterInGlobalWebXML)
                       .booleanValue()) {
            result = true;
        }

        return result;
    }
}
