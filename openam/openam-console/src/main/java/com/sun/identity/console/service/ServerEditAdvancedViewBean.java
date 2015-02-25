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
 * $Id: ServerEditAdvancedViewBean.java,v 1.5 2009/07/06 18:20:04 veiming Exp $
 *
 */

package com.sun.identity.console.service;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.common.configuration.UnknownPropertyNameException;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.AMViewConfig;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.service.model.ServerSiteModel;
import com.sun.identity.console.service.model.ServerSiteModelImpl;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.model.CCNavNode;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCTextField;
import com.sun.web.ui.view.table.CCActionTable;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.view.tabs.CCTabs;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import javax.servlet.http.HttpServletRequest;

/**
 * Server Configuration, Advanced Tab.
 */
public class ServerEditAdvancedViewBean
    extends AMPrimaryMastHeadViewBean
{
    private static final String DEFAULT_DISPLAY_URL =
        "/console/service/ServerEditAdvanced.jsp";

    private static final String PROPERTIES = "properties";
    private static final String TBL_PROPERTIES = "tblAdvancedProperties";
    private static final String TBL_BUTTON_ADD = "tblButtonAdd";
    private static final String TBL_BUTTON_DELETE = "tblButtonDelete";

    private static final String TBL_COL_NAME = "tblColName";
    private static final String TBL_DATA_NAME = "tblDataName";
    private static final String TBL_COL_VALUE = "tblColValue";
    private static final String TBL_DATA_VALUE = "tblDataValue";

    private static final String PGTITLE_THREE_BTNS = "pgtitleThreeBtns";
    
    private static Set dealtWithProperties;
    private static Map hiddenProperties;
    
    private CCPageTitleModel ptModel;
    private CCActionTableModel tblModel;
    private boolean submitCycle;
    private Set properties;
    
    static {
        Set set = new HashSet(12);
        set.add("propertyServerEditSecurity.xml");
        set.add("propertyServerEditSession.xml");
        set.add("propertyServerEditAdvanced.xml");
        set.add("propertyServerEditSDK.xml");
        set.add("propertyServerEditCTS.xml");
        set.add("propertyServerEditGeneral.xml");
        getDealtWithProperties(set);
        getHiddenProperties();
    }

    /**
     * Creates a server advanced profile view bean.
     */
    public ServerEditAdvancedViewBean() {
        super("ServerEditAdvanced");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    protected void initialize() {
        if (!initialized) {
            String serverName = (String)getPageSessionAttribute(
                ServerEditViewBeanBase.PG_ATTR_SERVER_NAME);
            if (serverName != null) {
                super.initialize();
                createPageTitleModel();
                createTabModel(serverName);
                createTableModel();
                registerChildren();
                initialized = true;
            }
        }
    }

    protected void registerChildren() {
        super.registerChildren();
        ptModel.registerChildren(this);
        registerChild(PROPERTIES, CCTextField.class);
        registerChild(PGTITLE_THREE_BTNS, CCPageTitle.class);
        registerChild(TBL_PROPERTIES, CCActionTable.class);
        tblModel.registerChildren(this);
    }

    protected View createChild(String name) {
        View view = null;

        if (name.equals(PGTITLE_THREE_BTNS)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (name.equals(PROPERTIES)) {
            view = new CCTextField(this, name, "");
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else if (tblModel.isChildSupported(name)) {
            view = tblModel.createChild(this, name);
        } else if (name.equals(TBL_PROPERTIES)) {
            view = new CCActionTable(this, tblModel, name);
        } else {
            view = super.createChild(name);
        }
        return view;
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        return new ServerSiteModelImpl(req, getPageSessionAttributes());
    }

    /**
     * Displays the advanced profile of a serer.
     *
     * @param event Display Event.
     */
    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event);
        String serverName = (String)getPageSessionAttribute(
            ServerEditViewBeanBase.PG_ATTR_SERVER_NAME);
        ServerSiteModel model = (ServerSiteModel)getModel();
        ptModel.setPageTitleText(model.getEditServerPageTitle(serverName));
        getProperties();
    }

    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.save");
        ptModel.setValue("button2", "button.reset");
        ptModel.setValue("button3", getBackButtonLabel());
    }

    protected void createTabModel() {
        if (tabModel == null) {
            AMViewConfig amconfig = AMViewConfig.getInstance();
            tabModel = amconfig.getTabsModel(
                ServerEditViewBeanBase.TAB_NAME, "/",
            getRequestContext().getRequest());
            registerChild(TAB_COMMON, CCTabs.class);
        }
    }

    private void createTableModel() {
        tblModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/tblServerConfigAdvanced.xml"));
        tblModel.setTitleLabel("label.items");
        tblModel.setActionValue(TBL_BUTTON_ADD,
            "table.serverconfig.advanced.properties.button.new");
        tblModel.setActionValue(TBL_BUTTON_DELETE,
            "table.serverconfig.advanced.properties.button.delete");
        tblModel.setActionValue(TBL_COL_NAME,
            "table.serverconfig.advanced.properties.name.column.name");
        tblModel.setActionValue(TBL_COL_VALUE,
            "table.serverconfig.advanced.properties.value.column.name");
    }
    
    protected void createTabModel(String serverName) {
        AMViewConfig amconfig = AMViewConfig.getInstance();
        tabModel = amconfig.getTabsModel(ServerEditViewBeanBase.TAB_NAME, "/",
            getRequestContext().getRequest());
        if (serverName.equals(ServerConfiguration.DEFAULT_SERVER_CONFIG)) {
            CCNavNode node = (CCNavNode)tabModel.getNodeById(425);
            tabModel.removeNode(node);
        }
        registerChild(TAB_COMMON, CCTabs.class);
    }
    
    private void getProperties() {
        if (!submitCycle) {
            try {
                ServerSiteModel model = (ServerSiteModel)getModel();
                String serverName = (String)getPageSessionAttribute(
                    ServerEditViewBeanBase.PG_ATTR_SERVER_NAME);
                properties = new TreeSet();
                Map map = model.getServerConfiguration(serverName);
                discardDealtWithProperties(map);
                discardHiddenProperties(map);
                for (Iterator i = map.entrySet().iterator(); i.hasNext(); ){
                    Map.Entry entry = (Map.Entry)i.next();
                    properties.add((String)entry.getKey() + "=" + 
                        (String)entry.getValue());
                }
            } catch (AMConsoleException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
            }
        }
        populateTableModel(properties);
    }

    private void discardDealtWithProperties(Map map) {
        for (Iterator i = map.entrySet().iterator(); i.hasNext(); ){
            Map.Entry entry = (Map.Entry)i.next();
            if (dealtWithProperties.contains(entry.getKey())) {
                i.remove();
            }
        }
    }

    private void discardHiddenProperties(Map map) {
        for (Iterator i = map.entrySet().iterator(); i.hasNext(); ){
            Map.Entry entry = (Map.Entry)i.next();
            String key = (String)entry.getKey();
            if (hiddenProperties.keySet().contains(key)) {
                hiddenProperties.put(key, entry.getValue());
                i.remove();
            }
        }
    }

    private void populateTableModel(Set properties) {
        tblModel.clearAll();
        ServerSiteModel model = (ServerSiteModel)getModel();

        if ((properties == null) || properties.isEmpty()) {
            properties = new HashSet(2);
            properties.add("=");
        }
        
        int counter = 0;
        for (Iterator iter = properties.iterator(); 
            iter.hasNext(); counter++
        ) {
            if (counter > 0) {
                tblModel.appendRow();
            }
            String t = (String)iter.next();
            int idx = t.indexOf('=');
            String name = t.substring(0, idx).trim();
            String value = t.substring(idx+1).trim();

            tblModel.setValue(TBL_DATA_NAME, name);
            tblModel.setValue(TBL_DATA_VALUE, value);
            tblModel.setSelectionVisible(counter, true);
        }
    }

    /**
     * Handles reset request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        forwardTo();
    }

    /**
     * Handles return to home page request.
     *
     * @param event Request invocation event
     */
    public void handleButton3Request(RequestInvocationEvent event)
        throws ModelControlException {
        returnToHomePage();
    }

    /**
     * Handles save properties  request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException
    {
        submitCycle = true;
        String valProperties = (String)getDisplayFieldValue(PROPERTIES);
        properties = new HashSet();
        Map map = new HashMap();
        
        StringTokenizer st = new StringTokenizer(valProperties, "\n");
        while (st.hasMoreElements()) {
            String t = st.nextToken();
            t = t.replaceAll("\r", "");
            int idx = t.indexOf('=');
            String name = t.substring(0, idx).trim();
            String value = t.substring(idx+1).trim();
            properties.add(t);
            map.put(name, value);
        }

        ServerSiteModel model = (ServerSiteModel)getModel();
        String serverName = (String)getPageSessionAttribute(
            ServerEditViewBeanBase.PG_ATTR_SERVER_NAME);

        /*
         * only global default has values for hidden properties. should not
         * try to set hidden property values for server instance
         */
        if (serverName.equals(ServerConfiguration.DEFAULT_SERVER_CONFIG)) {
            for (Iterator i = hiddenProperties.entrySet().iterator();
                i.hasNext(); ) {
                Map.Entry entry = (Map.Entry)i.next();
                map.put(entry.getKey(), entry.getValue());
            }
        }

        try {
            Map origValues = model.getServerConfiguration(serverName);
            discardDealtWithProperties(origValues);
            String unkownPropertyMessage = null; 
            try {
                model.modifyServer(serverName, null, map);
            } catch (UnknownPropertyNameException ex) {
                unkownPropertyMessage = ex.getL10NMessage(
                    model.getUserLocale());
            }
            
            for (Iterator i = origValues.keySet().iterator(); i.hasNext(); ) {
                String key = (String)i.next();
                if (map.containsKey(key)) {
                    i.remove();
                }
            }
            
            if (!origValues.isEmpty()) {
                model.updateServerConfigInheritance(serverName, 
                    origValues.keySet(), null);
            }

            if (unkownPropertyMessage != null) {
                Object[] args = {unkownPropertyMessage};
                setInlineAlertMessage(CCAlert.TYPE_WARNING, "message.warning",
                    MessageFormat.format(model.getLocalizedString(
                        "serverconfig.updated.with.invalid.properties"), args));
            } else {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "serverconfig.updated");
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
        
        forwardTo();
    }

    private void returnToHomePage() {
        backTrail();
        ServerSiteViewBean vb = (ServerSiteViewBean)getViewBean(
            ServerSiteViewBean.class);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.editserver";
    }

    protected boolean startPageTrail() {
        return false;
    }

    protected String getBackButtonLabel() {
        return getBackButtonLabel("page.title.serversite.config");
    }

    /**
     * Handles tab selected event.
     *
     * @param event Request Invocation Event.
     * @param nodeID Selected Node ID.
     */
    public void nodeClicked(RequestInvocationEvent event, int nodeID) {
        AMViewConfig amconfig = AMViewConfig.getInstance();

        try {
            AMViewBeanBase vb = getTabNodeAssociatedViewBean(
                "cscGeneral", nodeID);
            unlockPageTrailForSwapping();
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } catch (AMConsoleException e) {
            debug.error("ServerEditGeneralViewBean.nodeClicked", e);
            forwardTo();
        }
    }

    protected String getTrackingTabIDName() {
        return ServerEditViewBeanBase.TAB_TRACKER;
    }

    private static void getHiddenProperties() {
        hiddenProperties = new HashMap();
        ResourceBundle rb = ResourceBundle.getBundle("hiddenserverconfig");
        String hidden = rb.getString("hidden");
        StringTokenizer st = new StringTokenizer(hidden, " ");
        while (st.hasMoreTokens()) {
            hiddenProperties.put(st.nextToken().trim(), "");
        }
    }

    private static void getDealtWithProperties(Set uiXML) {
        dealtWithProperties = new HashSet();
        for (Iterator i = uiXML.iterator(); i.hasNext(); ) {
            String xml = (String)i.next();
            String is = AMAdminUtils.getStringFromInputStream(
                ServerEditAdvancedViewBean.class.getClassLoader()
                .getResourceAsStream("com/sun/identity/console/" + xml));
            Set set = ServerEditViewBeanBase.getAllConfigUINames(is);
            
            for (Iterator j = set.iterator(); j.hasNext(); ) {
                dealtWithProperties.add(
                    ServerEditViewBeanBase.getActualPropertyName(
                    (String)j.next()));
            }
        }
    }
}
