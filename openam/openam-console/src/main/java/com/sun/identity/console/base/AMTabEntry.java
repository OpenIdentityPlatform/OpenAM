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
 * $Id: AMTabEntry.java,v 1.3 2008/06/25 05:42:47 qcheng Exp $
 *
 */

package com.sun.identity.console.base;

import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.console.base.model.AccessControlModel;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.controller.TabController;
import com.sun.web.ui.model.CCNavNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class AMTabEntry {    
    public static final String NONE_VIEWBEAN = "none";
    public static final String NONE_URL = "none";

    private int id;
    private String label;
    private String tooltip;
    private String status;
    private String url;
    private String viewbean;
    private boolean delegateUI;
    private Set permissions;
    private String accessLevel;
    private Class controller;
    private AMTabEntry parentNode;
    private List children = new ArrayList();
    private Set serviceNamePrefixes = new HashSet();
    private Set serviceNames = new HashSet();

    public AMTabEntry(Node root)
        throws AMConsoleException
    {
        NamedNodeMap attrs = root.getAttributes();
        if (attrs == null) {
            throw new AMConsoleException(
                "AMTabEntry.<init> incorrect XML format");
        }

        setID(attrs);
        label = setAttribute(attrs, "label",
            "AMTabEntry.<init> missing label attribute");
        tooltip = setAttribute(attrs, "tooltip",
            "AMTabEntry.<init> missing tooltip attribute");
        status = setAttribute(attrs, "status",
            "AMTabEntry.<init> missing status attribute");
        viewbean = setAttribute(attrs, "viewbean",
            "AMTabEntry.<init> missing viewbean attribute");
        url = setAttribute(attrs, "url",
            "AMTabEntry.<init> missing url attribute");
        accessLevel = setAttribute(attrs, "accesslevel", null);
        permissions = setAttributes(attrs, "permissions",
            "AMTabEntry.<init> missing permissions attribute" + label);
        delegateUI = getBoolAttribute(attrs, "delegateUI");
        controller = getController(attrs);
        setChildren(root);
        setServiceNameInfo(root);
    }
            
    public AMTabEntry(
        int _id,
        String _label,
        String _tooltip,
        String _status,
        String _url,
        Set _permissions,
        String _viewbean
    ) {
        id = _id;
        label = _label;
        tooltip = _tooltip;
        status = _status;
        url = _url;
        permissions = _permissions;
        viewbean = _viewbean;
    }

    public AMTabEntry matchedID(int idx) {
        AMTabEntry entry = null;

        if (getID() == idx) {
            entry = this;
        } else {
            int sz = children.size();
            for (int i = 0; (i < sz) && (entry == null); i++) {
                AMTabEntry child = (AMTabEntry)children.get(i);
                entry = child.matchedID(idx);
            }
        }

        return entry;
    }

    public int getID() {
        String strID = Integer.toString(id);
        AMTabEntry parent = parentNode;

        if (parent != null) {
            strID = parent.getID() + strID;
        }

        return Integer.parseInt(strID);
    }

    public void removeChildren() {
        children.clear();
    }

    public void addChild(AMTabEntry tab) {
        boolean found = false;
        for (Iterator i = children.iterator(); i.hasNext() && !found;) {
            AMTabEntry a = (AMTabEntry)i.next();

            if (a.url.equals(tab.url)) {
                found = true;
            }
        }
        if (!found) {
            children.add(tab);
            tab.parentNode = this;
        }
    }

    public String getURL(AccessControlModel model, String realmName) {
        String target = null;

        if (canView() && canView(model, realmName)) {
            if (url.equals(NONE_URL)) {
                for (Iterator i = children.iterator();
                    i.hasNext() && (target == null);
                ) {
                    AMTabEntry child = (AMTabEntry)i.next();
                    target = child.getURL(model, realmName);
                }
            } else {
                target = url;
            }
        }

        return target;
    }

    public boolean canView(AccessControlModel model, String realmName) {
        return canView() &&
            model.canView(permissions, accessLevel, realmName, delegateUI);
    }

    private boolean canView() {
        boolean can = true;
        if (controller != null) {
            try {
                TabController c = (TabController)controller.newInstance();
                can = c.isVisible();
            } catch (InstantiationException e) {
                AMModelBase.debug.error("AMTabEntry.canView", e);
            } catch (IllegalAccessException e) {
                AMModelBase.debug.error("AMTabEntry.canView", e);
            }
        }
        return can;
    }

    public List getChildren() {
        return children;
    }

    public int getID(AccessControlModel model, String realmName) {
        return (canView(model, realmName)) ? id : -1;
    }

    public String getViewBean() {
        return viewbean;
    }

    public Set getPermissions() {
        return permissions;
    }

    public CCNavNode getNavNode(AccessControlModel model, String realmName) {
        boolean hasChild = true;
        CCNavNode navNode = null;

        if (canView() && canView(model, realmName)) {
            navNode = new CCNavNode(getID(), label, tooltip, status);

            if (!children.isEmpty()) {
                hasChild = false;

                for (Iterator iter = children.iterator(); iter.hasNext(); ) {
                    AMTabEntry child = (AMTabEntry)iter.next();
                    CCNavNode childNavNode = child.getNavNode(model, realmName);

                    if (childNavNode != null) {
                        navNode.addChild(childNavNode);
                        hasChild = true;
                    }
                }
            }
        }

        return (hasChild) ? navNode : null;
    }

    public Class getTabClass() 
        throws AMConsoleException {
        Class clazz = null;
        if (viewbean.equals(NONE_VIEWBEAN)) {
            clazz = com.sun.identity.console.base.BlankTabViewBean.class;
        } else {
            try {
                clazz = Class.forName(viewbean);
            } catch (ClassNotFoundException e) {
                throw new AMConsoleException(e);
            }
        }
        return clazz;
    }

    public Class getTabClass(int id) 
        throws AMConsoleException {
        Class clazz = null;
        if (getID() == id) {
            if (viewbean.equals(NONE_VIEWBEAN)) {
                clazz = com.sun.identity.console.base.BlankTabViewBean.class;
            } else {
                try {
                    clazz = Class.forName(viewbean);
                } catch (ClassNotFoundException e) {
                    throw new AMConsoleException(e);
                }
            }
        } else {
            for (Iterator iter = children.iterator();
                iter.hasNext() && (clazz == null);
            ) {
                AMTabEntry child = (AMTabEntry)iter.next();
                clazz = child.getTabClass(id);
            }
        }

        return clazz;
    }

    public Set getServiceNames() {
        return serviceNames;
    }

    public Set getServiceNamePrefixes() {
        return serviceNamePrefixes;
    }

    private void setID(NamedNodeMap attrs)
        throws AMConsoleException {
        Node nodeID = attrs.getNamedItem("id");
        if (nodeID == null) {
            throw new AMConsoleException(
                "AMTabEntry.<init> missing id attribute");
        }
        try {
            id = Integer.parseInt(nodeID.getNodeValue());
        } catch (NumberFormatException e) {
            throw new AMConsoleException(
                "AMTabEntry.<init> incorrect id attribute");
        }
    }

    private boolean getBoolAttribute(NamedNodeMap attrs, String attrName) {
        boolean boolVal = false;
        Node nodeID = attrs.getNamedItem(attrName);
        if (nodeID != null) {
            String value = nodeID.getNodeValue().trim();
            boolVal = value.equals("true");
        }
        return boolVal;
    }

    private Class getController(NamedNodeMap attrs) {
        Class clazz = null;
        Node nodeID = attrs.getNamedItem("controller");
        if (nodeID != null) {
            String value = nodeID.getNodeValue().trim();
            if (value.length() > 0) {
                try {
                    clazz = Class.forName(value);
                } catch (ClassNotFoundException e) {
                    AMModelBase.debug.error("AMTabEntry.getController", e);
                }
            }
        }
        return clazz;
    }

    private String setAttribute(
        NamedNodeMap attrs,
        String attrName,
        String exceptionMsg
    ) throws AMConsoleException {
        String value = null;
        Node nodeID = attrs.getNamedItem(attrName);
        if (nodeID == null) {
            if (exceptionMsg != null) {
                throw new AMConsoleException(exceptionMsg);
            }
        } else {
            value = nodeID.getNodeValue().trim();
            if (value.length() == 0) {
                value = null;
                if (exceptionMsg != null) {
                    throw new AMConsoleException(exceptionMsg);
                }
            }
        }

        return value;
    }

    private Set setAttributes(
        NamedNodeMap attrs,
        String attrName,
        String exceptionMsg
    ) throws AMConsoleException {
        Node nodeID = attrs.getNamedItem(attrName);
        if (nodeID == null) {
            throw new AMConsoleException(exceptionMsg);
        }
        String strValue = nodeID.getNodeValue().trim();

        if (strValue.length() == 0) {
            throw new AMConsoleException(exceptionMsg);
        } else {
            Set values = new HashSet();
            StringTokenizer st = new StringTokenizer(strValue, ",");
            while (st.hasMoreTokens()) {
                values.add(st.nextToken().trim());
            }
            return values;
        }
    }


    private void setChildren(Node root)
        throws AMConsoleException {
        NodeList childrenNodes = root.getChildNodes();
        for (int i = 0; i < childrenNodes.getLength(); i++) {
            Node child = childrenNodes.item(i);
            if (child.getNodeName().equalsIgnoreCase("tab")) {
                AMTabEntry childTab = new AMTabEntry(child);
                /// 42 is Site and Server Sub tab
                if (!ServerConfiguration.isLegacy() ||
                    (childTab.getID() != 42)
                ){
                    childTab.parentNode = this;
                    children.add(childTab);
                }
            }
        }
    }

    private void setServiceNameInfo(Node root) {
        NodeList childrenNodes = root.getChildNodes();
        for (int i = 0; i < childrenNodes.getLength(); i++) {
            Node child = childrenNodes.item(i);
            if (child.getNodeName().equalsIgnoreCase("services")) {
                serviceNamePrefixes = getChildNodeValues(
                    child, "prefixes", "prefix");
                serviceNames = getChildNodeValues(
                    child, "serviceNames", "serviceName");
            }
        }
    }

    private Set getChildNodeValues(
        Node startNode,
        String parentTagName,
        String childTagName
    ) {
        Set values = new HashSet();
        NodeList childrenNodes = startNode.getChildNodes();
        for (int i = 0; i < childrenNodes.getLength(); i++) {
            Node child = childrenNodes.item(i);
            if (child.getNodeName().equalsIgnoreCase(parentTagName)) {
                values.addAll(getChildNodeValues(child, childTagName));
            }
        }
        return values;
    }

    private Set getChildNodeValues(Node startNode, String childTagName) {
        Set values = new HashSet();
        NodeList childrenNodes = startNode.getChildNodes();
        for (int i = 0; i < childrenNodes.getLength(); i++) {
            Node child = childrenNodes.item(i);
            if (child.getNodeName().equalsIgnoreCase(childTagName)) {
                values.add(child.getFirstChild().getNodeValue());
            }
        }
        return values;
    }

    public String getLabel() {
        return label;
    }

    public String getTooltip() {
        return tooltip;
    }

    public String getStatus() {
        return status;
    }

}
