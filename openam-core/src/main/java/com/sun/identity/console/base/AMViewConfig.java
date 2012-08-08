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
 * $Id: AMViewConfig.java,v 1.9 2008/06/25 05:42:48 qcheng Exp $
 *
 */

package com.sun.identity.console.base;

import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.common.DisplayUtils;
import com.sun.identity.console.agentconfig.AgentsViewBean;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMFormatUtils;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.base.model.AccessControlModel;
import com.sun.identity.console.base.model.AccessControlModelImpl;
import com.sun.identity.console.idm.EntitiesViewBean;
import com.sun.web.ui.model.CCTabsModel;
import com.sun.web.ui.model.CCNavNode;
import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class AMViewConfig {
    private static final AMViewConfig instance = new AMViewConfig();
    private static final String CONFIG_FILENAME = "amConsoleConfig.xml";

    private List tabs = new ArrayList();
    private Map profileTabs = new HashMap();
    private Map showMenus = new HashMap();
    private Map services = new HashMap();

    private static final String TABS = "tabs";
    private static final String TAB_ENTRY = "tab";
    private static final String PROFILE_TABS = "profiletabs";
    private static final String SERVICES_CONFIG = "servicesconfig";
    private static final String HIDDEN_SERVICES = "hidden";
    private static final String CONSOLE_SERVICE = "consoleservice";
    private static final String REALM_ENABLE_HIDE_ATTRS =
        "realmEnableHideAttrName";
    private static final String IDENTITY_SERVICE = "identityservice";
    private static final String AGENT_SERVICE = "agentservice";
    private static final String COMBINE_AGENT = "combineagent";
    
    private Map combineAgent = new HashMap();

    private AMViewConfig() {
        Document doc = parseDocument(CONFIG_FILENAME);
        configTabs(doc);
        configMenus(doc);
        configProfileTabs(doc);
        configServices(doc);
    }

    public static AMViewConfig getInstance() {
        return instance;
    }

    public CCTabsModel getTabsModel(String realmName, HttpServletRequest req) {
        return getTabsModel(TABS, realmName, req);
    }

    public CCTabsModel getTabsModel(
        String type,   
        String realmName,
        HttpServletRequest req
    ) {
        CCTabsModel tabModel = new CCTabsModel();
        AccessControlModel model = new AccessControlModelImpl(req);
        List tabs = getTabList(type);
        if ((tabs == null) || tabs.isEmpty()) {
            return null;
        }
        
        for (Iterator iter = tabs.iterator(); iter.hasNext(); ) {
            AMTabEntry entry = (AMTabEntry)iter.next();
            CCNavNode navNode = entry.getNavNode(model, realmName);
            if (navNode != null) {
                tabModel.addNode(navNode);
            }
        }
        return tabModel;
    }
    
    // all tabs for SAMLv2 property page are predefined in fmConsoleConfiguration.xml.
    //here unwanted tabs are hidden based on the role of the respective entity.
    
    public CCTabsModel getSAMLv2TabsModel(
            String type,
            String realmName,
            HttpServletRequest req,
            List tabsToDisplay
    ) {
        CCTabsModel tabModel = new CCTabsModel();
        AccessControlModel model = new AccessControlModelImpl(req);
        List tabs = getTabList(type);
        if ((tabs == null) || tabs.isEmpty()) {
            return null;
        }
        for (Iterator iter = tabs.iterator(); iter.hasNext(); ) {
            AMTabEntry entry = (AMTabEntry)iter.next();
            for (Iterator tab_iter = tabsToDisplay.iterator(); tab_iter.hasNext(); ) {
                String roletoDisplay = (String)tab_iter.next();
                if (entry.getViewBean().contains(roletoDisplay)) {
                    CCNavNode navNode = entry.getNavNode(model, realmName);
                    if (navNode != null) {
                        tabModel.addNode(navNode);
                    }
                }
            }
        }
        return tabModel;
    }


    public CCTabsModel addSessionTabs(CCTabsModel tabModel, AMModel model) {
        if (tabModel == null) {
            tabModel = new CCTabsModel();
        }
        CCNavNode sessionNode = (CCNavNode) tabModel.getNodeById(
                AMAdminConstants.SESSIONS_NODE_ID);

        if (sessionNode != null) {
            if ( (tabs == null) || (tabs.isEmpty()) ||
                 (sessionNode.getChildren() == null) ||
                 (sessionNode.getChildren().size() >= 3)) {
                return tabModel;
            }
            AMTabEntry entries[] = new AMTabEntry[3];
            entries[0] = (AMTabEntry) sessionNode.getChildren().get(0);
            entries[1] = (AMTabEntry) sessionNode.getChildren().get(1);
            entries[2] = (AMTabEntry) sessionNode.getChildren().get(2);
            for (AMTabEntry entry : entries) {
                sessionNode.addChild(new CCNavNode(entry.getID(), entry.getLabel(),
                        entry.getTooltip(), entry.getStatus()));
            }
        }
        return tabModel;
    }

    public boolean addEntityTabs(
        CCTabsModel tabModel,
        String realmName,
        AMModel model
    ) {
        boolean added = false;
        List supported = getSupportedEntityTypes(realmName, model);

        if (!supported.isEmpty()) {
            CCNavNode subjectNode = (CCNavNode)tabModel.getNodeById(
                AMAdminConstants.SUBJECTS_NODE_ID);

            if (subjectNode != null) {
                for (Iterator i = supported.iterator(); i.hasNext(); ) {
                    String t = (String)i.next();
                    subjectNode.addChild(new CCNavNode(t.hashCode(), t, t, t));
                }
                added = true;
            }
        }

        return added;
    }
    
    public CCNavNode addAgentTabs(CCTabsModel tabModel, AMModel model, int idx) {
        CCNavNode selected = null;
        Map mapSupported = getSupportedAgentTypesMap(model);
        List supported = getSupportedAgentTypes(model);

        if (!supported.isEmpty()) {
            CCNavNode agentNode = (CCNavNode)tabModel.getNodeById(
                AMAdminConstants.TAB_AGENT_PREFIX_INT);
            if (agentNode != null) {
                for (int i = 0; i < supported.size(); i++) {
                    String t = (String)supported.get(i);
                    int nodeId = Integer.parseInt(
                        AMAdminConstants.TAB_AGENT_PREFIX + i);

                    String i18nKey = (String)mapSupported.get(t);
                    if (i18nKey == null) {
                        i18nKey = "agenttype." + t; 
                    }
                    CCNavNode n = n = new CCNavNode(nodeId, i18nKey, t, t);
                    if (nodeId == idx) {
                        selected = n;
                    }
                    agentNode.addChild(n);                        
                }
            }
        }

        return selected;
    }
    
    /**
     * Returns the combined agent type.
     * 
     * @param type Combined Agent type referred name.
     * @return the combined agent type.
     */
    public String getCombineAgentType(String type) {
        String cName = null;
        for (Iterator i = combineAgent.keySet().iterator(); 
            (i.hasNext() && (cName == null));
        ) {
            String name = (String)i.next();
            Set set = (Set) combineAgent.get(name);
            if (set.contains(type)) {
                cName = name;
            }
        }
        return cName;
    }

    /**
     * Returns <code>ture</code> if combined agent type.
     * 
     * @param type Combined Agent type referred name.
     * @return <code>ture</code> if combined agent type.
     */
    public boolean isCombineAgentType(String type) {
        return combineAgent.keySet().contains(type);
    }
    
    /**
     * Returns the combined agent types.
     * 
     * @param type Combined Agent type referred name.
     * @return the combined agent types.
     */
    public Set getCombineAgentTypes(String type) {
        return (Set)combineAgent.get(type);
    }

    public List getSupportedAgentTypes(AMModel model) {
        Map supported = getSupportedAgentTypesMap(model);
        List ordered = null;

        if ((supported != null) && !supported.isEmpty()) {
            ordered = new ArrayList(supported.size());
            Set basket = new HashSet();
            basket.addAll(supported.keySet());
            List predefinedOrder = (List)services.get(AGENT_SERVICE);

            for (Iterator i = predefinedOrder.iterator(); i.hasNext(); ) {
                String wildcard = (String)i.next();
                List matched = matchIdentityType(basket, wildcard, model);
                if (!matched.isEmpty()) {
                    ordered.addAll(matched);
                }
            }

            /*
             * This handles identity types that are not pre-ordered.
             */
            if (!basket.isEmpty()) {
                ordered.addAll(
                    AMFormatUtils.sortItems(basket, model.getUserLocale()));
            }
        }

        if ((ordered != null) && !ordered.isEmpty()) {
            Set alreadyAddedCName = new HashSet();
            List newList = new ArrayList(ordered.size());

            for (Iterator i = ordered.iterator(); i.hasNext();) {
                String name = (String) i.next();
                String cName = getCombineAgentType(name);
                if (cName != null) {
                    if (!alreadyAddedCName.contains(cName)) {
                        newList.add(cName);
                        alreadyAddedCName.add(cName);
                    }
                } else {
                    newList.add(name);
                }
            }
            return newList;
        }
        return Collections.EMPTY_LIST;
    }


    public List getSupportedEntityTypes(String realmName, AMModel model) {
        Map supported = getSupportedEntityTypesMap(realmName, model);
        List ordered = null;

        if ((supported != null) && !supported.isEmpty()) {
            ordered = new ArrayList(supported.size());
            Set basket = new HashSet();
            basket.addAll(supported.keySet());
            List predefinedOrder = getIdentityDisplayOrder();

            for (Iterator i = predefinedOrder.iterator(); i.hasNext(); ) {
                String wildcard = (String)i.next();
                List matched = matchIdentityType(basket, wildcard, model);
                if (!matched.isEmpty()) {
                    ordered.addAll(matched);
                }
            }

            /*
             * This handles identity types that are not pre-ordered.
             */
            if (!basket.isEmpty()) {
                ordered.addAll(
                    AMFormatUtils.sortItems(basket, model.getUserLocale()));
            }
        }

        return (ordered == null) ? Collections.EMPTY_LIST : ordered;
    }

    private List matchIdentityType(Set basket, String wildcard, AMModel model) {
        Set matched = new HashSet();
        for (Iterator i = basket.iterator(); i.hasNext(); ) {
            String type = (String)i.next();
            if (DisplayUtils.wildcardMatch(type, wildcard)) {
                matched.add(type);
                i.remove();
            }
        }
        return AMFormatUtils.sortItems(matched, model.getUserLocale());
    }


    public Map getSupportedAgentTypesMap(AMModel model) {
        //supported agent type should be accessible by all users.
        Map supported = model.getSupportedAgentTypes();
        return (supported == null) ? Collections.EMPTY_MAP : supported;
    }    

    public Map getSupportedEntityTypesMap(String realmName, AMModel model) {
        Map supported = null;
        AccessControlModel accessModel = new AccessControlModelImpl(
            model.getUserSSOToken());
        Set permission = new HashSet(2);
        permission.add(AMAdminConstants.IDREPO_SERVICE_NAME);

        if (accessModel.canView(permission, null, realmName, false)) {
            supported = model.getSupportedEntityTypes(realmName);
        }

        return (supported == null) ? Collections.EMPTY_MAP : supported;
    }

    /**
     * Adds a set of subtabs to the specified parent tab. The data for 
     * creating a subtab is a list of Map entries, each with the following
     * data.
     * <ul>
     * <li>label - name displayed for the tab</li>
     * <li>tooltip - tip information displayed when mouse over tab</li>
     * <li>status - text displayed in the status line of browser</li>
     * <li>url - url to invoke when tab is selected</li>
     * <li>viewbean - viewbean that is displayed</li>
     * </ul>
     *
     * @param parentID for the parent tab
     * @param items list of subtabs to add to the parent
     */
public void setTabViews(int parentID, List items) {
        AMTabEntry parent = null;
        try {
            parent = getTabEntry(parentID);
            parent.removeChildren();
        } catch (AMConsoleException a) {
            AMModelBase.debug.error("couldn't get the parent tab ");
            return;
        }    

        int id = 1;
        for (Iterator i = items.iterator(); i.hasNext(); ) {
            Map tab = (Map)i.next();
            parent.addChild(createTabEntry(id, tab));
            id++;
        }
    }
    
    private AMTabEntry createTabEntry(int id, Map data) {
        String label = (String)data.get("label");
        String tooltip = (String)data.get("tooltip");
        String status = (String)data.get("status");
        String url = (String)data.get("url");
        String permissions = (String)data.get("permissions");
        String viewbean = (String)data.get("viewbean");

        AMTabEntry child = new AMTabEntry(
            id, label, tooltip, status, url,
            AMAdminUtils.getDelimitedValues(permissions, ","), viewbean);
        
        return child;
    }

    public String getDefaultViewBeanURL(
        String realmName,
        HttpServletRequest req
    ) {
        return getDefaultViewBeanURL(TABS, realmName, req);
    }

    public String getDefaultViewBeanURL(
        String type,
        String realmName,
        HttpServletRequest req
    ) {
        AccessControlModel model = new AccessControlModelImpl(req);
        List list = getTabList(type);
        String url = null;
        for (Iterator i = list.iterator(); i.hasNext() && (url == null); ) {
            AMTabEntry entry = (AMTabEntry)i.next();
            url = entry.getURL(model, realmName);
        }
        return url;
    }

    public int getDefaultTabId(String realmName, HttpServletRequest req) {
        return getDefaultTabId(TABS, realmName, req);
    }
                                                                                
    public int getDefaultTabId(
        String type,
        String realmName,
        HttpServletRequest req
    ) {
        AccessControlModel model = new AccessControlModelImpl(req);
        List list = getTabList(type);
        int id = -1;
        for (Iterator i = tabs.iterator(); i.hasNext() && (id == -1); ) {
            AMTabEntry entry = (AMTabEntry)i.next();
            id = entry.getID(model, realmName);
        }
        return id;
    }

    public AMViewBeanBase getTabViewBean(
        AMViewBeanBase vb,
        String realmName,
        AMModel model,
        int idx,
        int childIdx
    ) throws AMConsoleException {
        return getTabViewBean(vb, realmName, model, TABS, idx, childIdx);
    }

    public AMViewBeanBase getTabViewBean(
        AMViewBeanBase vb,
        String realmName,
        AMModel model,
        String type,
        int idx,
        int childIdx
    ) throws AMConsoleException {
        Class clazz = getTabViewBeanClass(
            vb, realmName, model, type, idx, childIdx);
        return (AMViewBeanBase)vb.getViewBean(clazz);
    }

    private Class getTabViewBeanClass(
        AMViewBeanBase vb,
        String realmName,
        AMModel model,
        String type,
        int idx,
        int childIdx
    ) throws AMConsoleException {
        List list = getTabList(type);
        Class clazz = null;
        AccessControlModel accessModel = new AccessControlModelImpl(
            model.getUserSSOToken());

        for (Iterator i = list.iterator(); i.hasNext() && (clazz == null); ) {
            AMTabEntry entry = (AMTabEntry)i.next();

            if (entry.canView(accessModel, realmName)) {
            if (idx == -1) {
                clazz = entry.getTabClass();
            } else {
                clazz = entry.getTabClass(idx);
            }

            if (clazz == null) {
                clazz = getEntityTypeClass(vb, idx, realmName, model);
                if (clazz == null) {
                    clazz = getAgentTypeClass(vb, idx, model);
                }
            } else if (BlankTabViewBean.class.equals(clazz)) {
                switch (childIdx) {
                case -1:
                    clazz = null;
                    break;
                default:
                    clazz = getTabViewBeanClass(
                        vb, realmName, model, type, childIdx, -1);
                    break;
                }
            }
            }
        }

        if (clazz == null) {
            throw new AMConsoleException(
                "AMViewConfig.getTabClass: no action class for node ID, " +idx);
        }

        return clazz;
    }

    
    private Class getEntityTypeClass(
        AMViewBeanBase vb,
        int idx, 
        String realmName, 
        AMModel model
    ) {
        Class clazz = null;
        Map supported = model.getSupportedEntityTypes(realmName);

        for (Iterator i = supported.keySet().iterator(); 
            i.hasNext() && (clazz == null);
        ) {
            String t = (String)i.next();
            if (idx == t.hashCode()) {
                clazz = EntitiesViewBean.class;
                vb.setPageSessionAttribute(
                    EntitiesViewBean.PG_SESSION_ENTITY_TYPE, t);
            }
        }
        return clazz;
    }
    
    private Class getAgentTypeClass(AMViewBeanBase vb, int idx, AMModel model) {
        Class clazz = null;
        Map supported = model.getSupportedAgentTypes();

        for (Iterator i = supported.keySet().iterator(); 
            i.hasNext() && (clazz == null);
        ) {
            String t = (String)i.next();
            if (idx == t.hashCode()) {
                clazz = AgentsViewBean.class;
                vb.setPageSessionAttribute(
                    AgentsViewBean.PG_SESSION_AGENT_TYPE, t);
            }
        }
        return clazz;
    }

    public AMTabEntry getTabEntry(int idx)
        throws AMConsoleException {
        return getTabEntry(TABS, idx);
    }

    private AMTabEntry getTabEntry(String type, int idx)
        throws AMConsoleException {
        List list = getTabList(type);
        AMTabEntry entry = null;

        for (Iterator iter = list.iterator(); 
             iter.hasNext() && (entry == null);
        ) {
            AMTabEntry e = (AMTabEntry)iter.next();
            entry = e.matchedID(idx);
        }

        if (entry == null) {
            throw new AMConsoleException(
                "AMViewConfig.getTabEntry: not found, id = " + idx);
        }

        return entry;
    }

    public OptionList getShowMenus(String name) {
        OptionList optList = new OptionList();
        List list = (List)showMenus.get(name);

        if ((list != null) && !list.isEmpty()) {
            for (Iterator iter = list.iterator(); iter.hasNext(); ) {
                AMShowMenuEntry menu = (AMShowMenuEntry)iter.next();
                optList.add(menu.getLabel(), menu.getID());
            }
        }

        return optList;
    }

    public Class getShowMenuClass(String name, String idx)
        throws AMConsoleException {
        Class clazz = null;
        List list = (List)showMenus.get(name);

        if ((list != null) && !list.isEmpty()) {
            for (Iterator iter = list.iterator();
                iter.hasNext() && (clazz == null);
            ) {
                AMShowMenuEntry menu = (AMShowMenuEntry)iter.next();
                if (menu.getID().equals(idx)) {
                    try {
                        clazz = Class.forName(menu.getViewBean());
                    } catch (ClassNotFoundException e) {
                        throw new AMConsoleException(e.getMessage());
                    }
                }
            }
        }

        if (clazz == null) {
            throw new AMConsoleException(
            "AMViewConfig.getShowMenuClass: not action class for ID, " +idx);
        }

        return clazz;
    }

    public boolean isServiceVisible(String serviceName) {
        Set hidden = (Set)services.get(HIDDEN_SERVICES);
        return !hidden.contains(serviceName);
    }

    public Set getRealmEnableHiddenConsoleAttrNames() {
        return (Set)services.get(REALM_ENABLE_HIDE_ATTRS);
    }

    public List getIdentityDisplayOrder() {
        return (List)services.get(IDENTITY_SERVICE);
    }

    private Document parseDocument(String fileName) {
        Document document = null;
        InputStream is = getClass().getClassLoader().getResourceAsStream(
            fileName);

        try {
            DocumentBuilderFactory dbFactory =
                DocumentBuilderFactory.newInstance();
            dbFactory.setValidating(false);
            dbFactory.setNamespaceAware(false);

            DocumentBuilder documentBuilder = dbFactory.newDocumentBuilder();
            documentBuilder.setEntityResolver(new DefaultHandler());
            document = documentBuilder.parse(is);
        } catch (UnsupportedEncodingException e) {
            AMModelBase.debug.error("AMViewConfig.parseDocument", e);
        } catch (ParserConfigurationException e) {
            AMModelBase.debug.error("AMViewConfig.parseDocument", e);
        } catch (SAXException e) {
            AMModelBase.debug.error("AMViewConfig.parseDocument", e);
        } catch (IOException e) {
            AMModelBase.debug.error("AMViewConfig.parseDocument", e);
        }

        return document;
    }

    private void configTabs(Document doc) {
        NodeList nodes = doc.getElementsByTagName(TABS);

        if ((nodes != null) && (nodes.getLength() == 1)) {
            Node root = nodes.item(0);
            NodeList children = root.getChildNodes();

            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);

                if (child.getNodeName().equalsIgnoreCase(TAB_ENTRY)) {
                    try {
                        AMTabEntry entry = new AMTabEntry(child);
                        tabs.add(entry);
                    } catch (AMConsoleException e) {
                        AMModelBase.debug.error("AMViewConfig.configTabs", e);
                    }
                }
            }
        } else {
            AMModelBase.debug.error(
                "AMViewConfig.configTabs TabConfig.xml is incorrect.");
        }
    }

    private List getProfileTabs(Node parent) {
        List entries = new ArrayList();
        NodeList children = parent.getChildNodes();

        int length = children.getLength();
        for (int i = 0; i < length; i++) {
            Node child = children.item(i);

            if (child.getNodeName().equalsIgnoreCase(TAB_ENTRY)) {
                try {
                    AMTabEntry entry = new AMTabEntry(child);
                    entries.add(entry);
                } catch (AMConsoleException e) {
                    AMModelBase.debug.error("AMViewConfig.getProfileTabs", e);
                }
            }
        }
        return entries;
    }

    private void configServices(Document doc) {
        NodeList nodes = doc.getElementsByTagName(SERVICES_CONFIG);

        if ((nodes != null) && (nodes.getLength() == 1)) {
            Node root = nodes.item(0);
            NodeList children = root.getChildNodes();

            if (children != null) {
                for (int i = 0; i < children.getLength(); i++) {
                    Node child = children.item(i);

                    if (child.getNodeName().equals(HIDDEN_SERVICES)) {
                        try {
                            Set set = new HashSet();
                            String names = getAttribute(child, "names");
                            StringTokenizer st = new StringTokenizer(
                                names, ",");
                            while (st.hasMoreTokens()) {
                                set.add(st.nextToken().trim());
                            }
                            services.put(HIDDEN_SERVICES, set);
                        } catch (AMConsoleException e) {
                            AMModelBase.debug.error(
                                "AMViewConfig.configServices", e);
                        }
                    } else if (child.getNodeName().equals(CONSOLE_SERVICE)) {
                        try {
                            Set set = new HashSet();
                            String names = getAttribute(
                                child, REALM_ENABLE_HIDE_ATTRS);
                            StringTokenizer st = new StringTokenizer(
                                names, ",");
                            while (st.hasMoreTokens()) {
                                set.add(st.nextToken().trim());
                            }
                            services.put(REALM_ENABLE_HIDE_ATTRS, set);
                        } catch (AMConsoleException e) {
                            AMModelBase.debug.error(
                                "AMViewConfig.configServices", e);
                        }
                    } else if (child.getNodeName().equals(IDENTITY_SERVICE)) {
                        List list = new ArrayList();
                        services.put(IDENTITY_SERVICE, list);
                        try {
                            String order = getAttribute(child, "order");
                            StringTokenizer st = new StringTokenizer(
                                order, ",");
                            while (st.hasMoreTokens()) {
                                list.add(st.nextToken().trim());
                            }
                        } catch (AMConsoleException e) {
                            AMModelBase.debug.error(
                                "AMViewConfig.configServices", e);
                        }
                    } else if (child.getNodeName().equals(AGENT_SERVICE)) {
                        List list = new ArrayList();
                        services.put(AGENT_SERVICE, list);
                        try {
                            String order = getAttribute(child, "order");
                            StringTokenizer st = new StringTokenizer(
                                order, ",");
                            while (st.hasMoreTokens()) {
                                list.add(st.nextToken().trim());
                            }
                        } catch (AMConsoleException e) {
                            AMModelBase.debug.error(
                                "AMViewConfig.configServices", e);
                        }
                    } else if (child.getNodeName().equals(COMBINE_AGENT)) {
                        try {
                            String order = getAttribute(child, "pairs");
                            StringTokenizer st =new StringTokenizer(order, "|");
                            while (st.hasMoreElements()) {
                                String token = st.nextToken();
                                int idx = token.indexOf("=");
                                if (idx != -1) {
                                    String vType = token.substring(0, idx);
                                    String suffix = token.substring(idx+1);

                                    StringTokenizer st1 = new StringTokenizer(
                                        suffix, ",");
                                    Set set = new HashSet();
                                    combineAgent.put(vType, set);
                                    while (st1.hasMoreElements()) {
                                        set.add(st1.nextToken());
                                    }
                                }
                            }
                        } catch (AMConsoleException e) {
                            AMModelBase.debug.error(
                                "AMViewConfig.configServices", e);
                        }
                        
                    }
                }
            }
        }
    }
    
    private void configProfileTabs(Document doc) {
        NodeList nodes = doc.getElementsByTagName(PROFILE_TABS);

        // there will be only 1 entry for the profiletab definitions
        if ((nodes != null) && (nodes.getLength() == 1)) {
            Node root = nodes.item(0);
            NodeList children = root.getChildNodes();

            // there can be 0 or more profile tabs defined
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);

                // get the tabs defined for the profile object
                if (child.getNodeName().equalsIgnoreCase("profile")) {
                    try {
                        // id is the name of the profile object, ie realms, 
                        // users, groups, ...
                        String id = getAttribute(child, "id");
                        if ((id != null) && (id.length() > 0)) {
                            profileTabs.put(id, getProfileTabs(child));
                        }
                    } catch (AMConsoleException e) {
                        AMModelBase.debug.error("AMViewConfig.configMenus", e);
                    }
                }
            }
        } else {
            AMModelBase.debug.warning(
                "AMViewConfig.configProfileTabs, config xml is incorrect.");
        }
    }

    private void configMenus(Document doc) {
        NodeList nodes = doc.getElementsByTagName("showmenus");

        if ((nodes != null) && (nodes.getLength() == 1)) {
            Node root = nodes.item(0);
            NodeList children = root.getChildNodes();

            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);

                if (child.getNodeName().equalsIgnoreCase("showmenu")) {
                    try {
                        String id = getAttribute(child, "id");
                        if ((id != null) && (id.length() > 0)) {
                            showMenus.put(id, getShowMenus(child));
                        }
                    } catch (AMConsoleException e) {
                        AMModelBase.debug.error("AMViewConfig.configMenus", e);
                    }
                }
            }
        }
    }

    private List getShowMenus(Node node)
        throws AMConsoleException {
        List list = new ArrayList();
        NodeList children = node.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeName().equalsIgnoreCase("menu")) {
                try {
                    list.add(new AMShowMenuEntry(child));
                } catch (AMConsoleException e) {
                    AMModelBase.debug.error("AMViewConfig.getShowMenus", e);
                }
            }
        }

        return list;
    }

    private String getAttribute(Node node, String attrName)
        throws AMConsoleException {
        String value = null;
        NamedNodeMap attrs = node.getAttributes();
        Node nodeID = attrs.getNamedItem(attrName);
        if (nodeID != null) {
            value = nodeID.getNodeValue();
            value = value.trim();
        }
        return value;
    }

    private List getTabList(String type) {
        List tmp = null;
        if (type.equals(TABS)) {
            tmp = tabs;
        } else {
            tmp = (List)profileTabs.get(type);
        }
        return tmp;
    }
    
    public void addTabEntries(String type, List entries, boolean newSet) {
        List tabList = null;

        // pull the existing tabs if we are just updating the current list.
        if (!newSet) {
            tabList = getTabList(type);
        } else {                    
            tabList = new ArrayList(entries.size()*2);
        }
        
        int id = tabList.size() + 1;
        for (Iterator i = entries.iterator(); i.hasNext(); ) { 
            Map entry = (Map)i.next();
            tabList.add(createTabEntry(id++ ,entry));
        }
        
        // store the new tabs for this type
        profileTabs.put(type, tabList);
    }
}
