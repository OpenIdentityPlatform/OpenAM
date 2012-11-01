/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: DelegationConfig.java,v 1.2 2008/06/25 05:42:53 qcheng Exp $
 *
 */
/**
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.console.delegation.model;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMAuthUtils;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.shared.xml.XMLUtils;
import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import javax.servlet.http.HttpServletRequest;

/* - NEED NOT LOG - */

public class DelegationConfig {
    private static final String CONFIG_FILENAME = "amAccessControl.xml"; 
    private static DelegationConfig instance = new DelegationConfig();
    private Map mapViewBeanToActionHandler = new HashMap();
    private Set ignore = new HashSet();

    private static final String NODE_NAME_VIEWBEAN = "viewbean";
    private static final String NODE_NAME_IGNORE = "neednodealwith";
    private static final String NODE_NAME_ACTION_HANDLER = "actionhandler";
    private static final String NODE_NAME_STATIC_TEXT = "statictext";
    private static final String NODE_NAME_TABLE = "table";
    private static final String ATTR_NAME_VIEW = "view";
    private static final String ATTR_NAME_MODIFY = "modify";

    private DelegationConfig() {
        Document doc = parseDocument(CONFIG_FILENAME);
        configure(doc);
    }

    private Document parseDocument(String fileName) {
        Document document = null;
        InputStream is = getClass().getClassLoader().getResourceAsStream(
            fileName);

        try {
            DocumentBuilder documentBuilder = XMLUtils.getSafeDocumentBuilder(false);
            document = documentBuilder.parse(is);
        } catch (UnsupportedEncodingException e) {
            AMModelBase.debug.error("DelegationConfig.parseDocument", e);
        } catch (ParserConfigurationException e) {
            AMModelBase.debug.error("DelegationConfig.parseDocument", e);
        } catch (SAXException e) {
            AMModelBase.debug.error("DelegationConfig.parseDocument", e);
        } catch (IOException e) {
            AMModelBase.debug.error("DelegationConfig.parseDocument", e);
        }

        return document;
    }

    private void configure(Document doc) {
        NodeList nodes = doc.getElementsByTagName(NODE_NAME_VIEWBEAN);

        if (nodes != null) {
            int sz = nodes.getLength();
            for (int i = 0; i < sz; i++) {
                Node node = nodes.item(i);
                configureViewBean(node);
            }
        }

        nodes = doc.getElementsByTagName(NODE_NAME_IGNORE);

        if (nodes != null) {
            int sz = nodes.getLength();
            for (int i = 0; i < sz; i++) {
                Node node = nodes.item(i);
                ignore.add(getAttribute(node, "classname"));
            }
        }
    }

    private String getAttribute(Node node, String attrName) {
        String value = null;
        NamedNodeMap attrs = node.getAttributes();
        Node nodeID = attrs.getNamedItem(attrName);
        if (nodeID != null) {
            value = nodeID.getNodeValue();
            value = value.trim();
        }
        return value;
    }

    private void configureViewBean(Node parent) {
        String viewbeanClassName = getAttribute(parent, "classname");

        if ((viewbeanClassName != null) && (viewbeanClassName.length() > 0)) {
            DelegationConfigNode configNode = new DelegationConfigNode(
                viewbeanClassName);
            String permView = getAttribute(parent, ATTR_NAME_VIEW);
            configNode.setPermissions(AMAdminConstants.PERMISSION_READ,
                AMAdminUtils.getDelimitedValues(permView, ","));
            String permModify = getAttribute(parent, ATTR_NAME_MODIFY);
            configNode.setPermissions(AMAdminConstants.PERMISSION_MODIFY,
                AMAdminUtils.getDelimitedValues(permModify, ","));

            mapViewBeanToActionHandler.put(viewbeanClassName, configNode);
            NodeList children = parent.getChildNodes();

            if (children != null) {
                int sz = children.getLength();

                for (int i = 0; i < sz; i++) {
                    Node child = children.item(i);

                    if (child.getNodeName().equals(NODE_NAME_ACTION_HANDLER)) {
                        String actionhandlers = getAttribute(child, "name");
                        configNode.setActionHandlers(
                            AMAdminUtils.getDelimitedValues(
                                actionhandlers, ","));
                    } else if (child.getNodeName().equals(NODE_NAME_TABLE)) {
                        String tables = getAttribute(child, "name");
                        configNode.setTables(AMAdminUtils.getDelimitedValues(
                            tables, ","));
                    } else if (child.getNodeName().equals(
                        NODE_NAME_STATIC_TEXT)
                    ) {
                        String statictexts = getAttribute(child, "name");
                        configNode.setStaticTexts(
                            AMAdminUtils.getDelimitedValues(statictexts, ","));
                    }
                }
            }
        }
    }

    public static DelegationConfig getInstance() {
        return instance;
    }
    public boolean hasPermission(
        String realmName,
        String serviceName,
        String action,
        HttpServletRequest req,
        String viewbeanClassName
    ) {
        boolean hasPermission = false;
        try {
            SSOToken ssoToken = AMAuthUtils.getSSOToken(req);
            hasPermission = hasPermission(realmName, serviceName, action,
                ssoToken, viewbeanClassName);
        } catch (SSOException e) {
            AMModelBase.debug.warning("AccessControlModelImpl.<init>", e);
        }
        return hasPermission;
    }

    public boolean hasPermission(
        String realmName,
        String serviceName,
        String action,
        AMModel model,
        String viewbeanClassName
    ) {
        return hasPermission(realmName, serviceName, action,
            model.getUserSSOToken(), viewbeanClassName);
    }

    public boolean hasPermission(
        String realmName,
        String serviceName,
        String action,
        SSOToken ssoToken,
        String viewbeanClassName
    ) {
        boolean hasPermission = false;
        DelegationConfigNode configNode = (DelegationConfigNode)
            mapViewBeanToActionHandler.get(viewbeanClassName);

        if (configNode != null) {
            try {
                hasPermission = configNode.hasPermission(
                    realmName, serviceName, action, ssoToken);
            } catch (DelegationException e) {
                AMModelBase.debug.error("DelegationConfig.hasPermission", e);
            }
        } else if (ignore.contains(viewbeanClassName)) {
            hasPermission = true;
        } else {
            AMModelBase.debug.error("DelegationConfig.hasPermission:" +
                " cannot find access control information for " +
                viewbeanClassName);
        }

        return hasPermission;
    }

    public void configureButtonsAndTables(
        String realmName,
        String serviceName,
        AMModel model,
        AMViewBeanBase viewbean
    ) {
        String viewbeanClassName = viewbean.getClass().getName();
        DelegationConfigNode configNode = (DelegationConfigNode)
            mapViewBeanToActionHandler.get(viewbeanClassName);

        if (configNode != null) {
            try {
                configNode.configureButtonsAndTables(
                    realmName, serviceName, model, viewbean);
            } catch (DelegationException e) {
                AMModelBase.debug.error("DelegationConfig.configureButtons", e);
            }
        } else if (!ignore.contains(viewbeanClassName)) {
            AMModelBase.debug.error("DelegationConfig.configureButtons:" +
                " cannot find access control information for " +
                viewbeanClassName);
        }
    }

    public boolean isUncontrolledViewBean(String viewBeanClassName) {
        return ignore.contains(viewBeanClassName);
    }
}
