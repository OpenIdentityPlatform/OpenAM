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
 * $Id: ServerEditViewBeanBase.java,v 1.3 2008/09/11 16:33:16 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.service;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.ChildDisplayEvent;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.common.configuration.ServerPropertyValidator;
import com.sun.identity.common.configuration.UnknownPropertyNameException;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.AMViewConfig;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.service.model.ServerSiteModel;
import com.sun.identity.console.service.model.ServerSiteModelImpl;
import com.sun.web.ui.model.CCNavNode;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCCheckBox;
import com.sun.web.ui.view.html.CCButton;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.view.tabs.CCTabs;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/**
 * Base class for server profile view bean.
 */
public abstract class ServerEditViewBeanBase
    extends AMPrimaryMastHeadViewBean
{
    static final String PG_ATTR_SERVER_NAME = "pgAttrServerName";

    public static final String TAB_TRACKER = "CCTabs.serverConfig";
    public static final String TAB_NAME = "cscGeneral";
    private static final String PROPERTY_PREFIX = "csc";
    private static final String PGTITLE_THREE_BTNS = "pgtitleThreeBtns";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";

    private static final String BTN_INHERIT = "btnInherit";
    
    private CCPageTitleModel ptModel;
    private AMPropertySheetModel propertySheetModel;
    private Set allPropertyNames = new HashSet();
    private Set activePropertyNames = new HashSet();
    protected boolean submitCycle;

    /**
     * Creates a edit server configuration view bean.
     */
    public ServerEditViewBeanBase(String name, String url) {
        super(name);
        setDefaultDisplayURL(url);
    }
    
    protected void initialize() {
        if (!initialized) {
            String serverName = (String)getPageSessionAttribute(
                PG_ATTR_SERVER_NAME);
            if (serverName != null) {
                super.initialize();
                createPageTitleModel();
                createTabModel(serverName);
                createPropertyModel(serverName);
                registerChildren();
                initialized = true;
            }
        }
    }

    protected void registerChildren() {
        super.registerChildren();
        ptModel.registerChildren(this);
        registerChild(PGTITLE_THREE_BTNS, CCPageTitle.class);
        registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
        registerChild(BTN_INHERIT, CCButton.class);
        propertySheetModel.registerChildren(this);
    }

    protected View createChild(String name) {
        View view = null;

        if (name.equals(PGTITLE_THREE_BTNS)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (name.equals(PROPERTY_ATTRIBUTE)) {
            view = new AMPropertySheet(this, propertySheetModel, name);
        } else if ((propertySheetModel != null) &&
            propertySheetModel.isChildSupported(name)
        ) {
            view = propertySheetModel.createChild(this, name, getModel());
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
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
     * Displays the profile of a site.
     */
    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event);
        String serverName = (String)getPageSessionAttribute(
            PG_ATTR_SERVER_NAME);
        ServerSiteModel model = (ServerSiteModel)getModel();
        ptModel.setPageTitleText(model.getEditServerPageTitle(serverName));
        try {
            setConfigProperties(serverName, model);
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }
    
    private void setConfigProperties(String serverName, ServerSiteModel model)
        throws AMConsoleException {
        if (!submitCycle) {
            Map attributeValues = model.getServerConfiguration(serverName);
            for (Iterator i = activePropertyNames.iterator(); i.hasNext(); ) {
                String name = (String)i.next();
                String propertyName = getActualPropertyName(name);
                String val = (String)attributeValues.get(propertyName);
                if (val == null) {
                    propertySheetModel.setValue(name, "");
                } else {
                    View view = this.getChild(name);
                    if (view instanceof CCCheckBox) {
                        String trueValue = ServerPropertyValidator.getTrueValue(
                            propertyName);
                        String v = (val.equals(trueValue)) ? "true" : "false";
                        propertySheetModel.setValue(name, v);
                    } else {
                        propertySheetModel.setValue(name, val);
                    }
                }
            }
        }
    }

    static String getActualPropertyName(String name) {
        name = name.substring(3);
        return name.replaceAll("-", ".");
    }
    
    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.save");
        ptModel.setValue("button2", "button.reset");
        ptModel.setValue("button3", getBackButtonLabel());
    }

    protected void createTabModel(String serverName) {
        AMViewConfig amconfig = AMViewConfig.getInstance();
        tabModel = amconfig.getTabsModel(TAB_NAME, "/",
            getRequestContext().getRequest());
        if (serverName.equals(ServerConfiguration.DEFAULT_SERVER_CONFIG)) {
            CCNavNode node = (CCNavNode)tabModel.getNodeById(425);
            tabModel.removeNode(node);
        }
        registerChild(TAB_COMMON, CCTabs.class);
    }
/*
    void validateTabsModel(ServerSiteModel model, CCTabsModel tabModel) {
        try {
            if (!ServerConfiguration.isLegacy(model.getUserSSOToken())) {
                tabModel.removeNode(tabModel.getNodeById(42));
            }
        } catch (SSOException e) {
            AMModelBase.debug.error(
                "ServerEditViewBeanBase.validateTabsModel", e);
        } catch (SMSException e) {
            AMModelBase.debug.error(
                "ServerEditViewBeanBase.validateTabsModel", e);
        }
    }*/

    public boolean beginBtnInheritDisplay(ChildDisplayEvent event) {
        String serverName = (String)getPageSessionAttribute(
            PG_ATTR_SERVER_NAME);
        return !serverName.equals(ServerConfiguration.DEFAULT_SERVER_CONFIG);
    }

    private void createPropertyModel(String serverName) {
        String xml = AMAdminUtils.getStringFromInputStream(
            getClass().getClassLoader().getResourceAsStream(getPropertyXML()));
        Set uiNames = getAllConfigUINames(xml);
        ServerSiteModel model = (ServerSiteModel)getModel();

        try {
            Map attrValues = model.getServerConfiguration(serverName);
            Map defaultValues = model.getServerDefaults();
            Map textValues = new HashMap();
            
            for (Iterator i = uiNames.iterator(); i.hasNext(); ) {
                String uiName = (String)i.next();
                String propertyName = getActualPropertyName(uiName);
                if (!attrValues.containsKey(propertyName)) {
                    textValues.put(uiName, defaultValues.get(propertyName));
                } else {
                    activePropertyNames.add(uiName);
                    i.remove();
                }
                allPropertyNames.add(uiName);
            }
            
            if (!textValues.isEmpty()) {
                xml = textifyXML(xml, uiNames, textValues);
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }

        // this is because default server configuration does not have
        // parent site.
        xml = removeParentSiteBlob(xml);
        
        propertySheetModel = new AMPropertySheetModel(xml);
        propertySheetModel.clear();
    }
    
    static Set getAllConfigUINames(String xml) {
        Set names = new HashSet();
        int start = xml.indexOf("<cc name=\"csc");
        
        while (start != -1) {
            int end = xml.indexOf("\"", start +14);
            names.add(xml.substring(start+10, end));
            start = xml.indexOf("<cc name=\"csc", end);
        }
        return names;
    }
    
    private String textifyXML(String xml, Set names, Map values) {
        for (Iterator i = names.iterator(); i.hasNext(); ) {
            String name = (String)i.next();
            String value = (String)values.get(name);
            if (value == null) {
                value = "";
            }
            
            Object[] params = {name, value};
            String xmlComponent = MessageFormat.format(TEXT_TEMPLATE, params);

            int idx = xml.indexOf("<cc name=\"" + name + "\"");
            if (idx != -1) {
                int endIdx = xml.indexOf("</cc>", idx);
                xml = xml.substring(0, idx) + xmlComponent + 
                    xml.substring(endIdx+5);
            }
        }
        return xml;
    }

    protected void modifyProperties() {
        String serverName = (String)getPageSessionAttribute(
            PG_ATTR_SERVER_NAME);
        ServerSiteModel model = (ServerSiteModel)getModel();

        try {
            model.modifyServer(serverName, null, getAttributeValues());
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                "serverconfig.updated");
        } catch (UnknownPropertyNameException e) {
            // ignore, this cannot happen because properties on this page
            // are pre checked.
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }

    protected Map getAttributeValues() {
        Map map = new HashMap();
        for (Iterator i = activePropertyNames.iterator(); i.hasNext(); ) {
            String uiName = (String)i.next();
            String value = (String)getDisplayFieldValue(uiName);
            String propertyName = getActualPropertyName(uiName);

            View view = getChild(uiName);
            if (view instanceof CCCheckBox) {
                value = (value.equals("true")) ?
                    ServerPropertyValidator.getTrueValue(propertyName) :
                    ServerPropertyValidator.getFalseValue(propertyName);
            }

            map.put(propertyName, value);
        }
        return map;
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
    
    
    public void handleBtnInheritRequest(RequestInvocationEvent event)
        throws ModelControlException {
        ServerConfigInheritViewBean vb =(ServerConfigInheritViewBean)
            getViewBean(ServerConfigInheritViewBean.class);
        vb.setPageSessionAttribute(
            ServerConfigInheritViewBean.PG_ATTR_CONFIG_PAGE, 
            getClass().getName());
        vb.setPageSessionAttribute(
            ServerConfigInheritViewBean.PG_ATTR_PROPERTY_NAMES, 
            (HashSet)allPropertyNames);
        unlockPageTrailForSwapping();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
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

    protected String removeParentSiteBlob(String xml) {
        //General Tab View Bean will overwrite this.
        return xml;
    }

    protected String getTrackingTabIDName() {
        return TAB_TRACKER;
    }

    protected abstract String getPropertyXML();
    private static final String TEXT_TEMPLATE =
        "<cc name=\"{0}\" tagclass=\"com.sun.web.ui.taglib.html.CCStaticTextFieldTag\"><attribute name=\"defaultValue\" value=\"{1}\" /></cc>";
}
