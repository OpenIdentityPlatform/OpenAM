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
 * $Id: AgentConfigInheritViewBean.java,v 1.8 2008/08/08 17:34:45 veiming Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.console.agentconfig;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.sso.SSOException;
import com.sun.identity.common.configuration.AgentConfiguration;
import com.sun.identity.console.agentconfig.model.AgentsModel;
import com.sun.identity.console.agentconfig.model.AgentsModelImpl;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.components.view.html.SerializedField;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SMSException;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.html.CCStaticTextField;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.view.table.CCActionTable;
import com.sun.web.ui.view.alert.CCAlert;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/**
 * Servers and Sites Management main page.
 */
public class AgentConfigInheritViewBean
    extends AMPrimaryMastHeadViewBean
{
    private static final String DEFAULT_DISPLAY_URL =
        "/console/agentconfig/AgentConfigInherit.jsp";
    private static final String TBL_PROPERTY_NAMES = "tblPropertyNames";
    private static final String PGTITLE_THREE_BTNS = "pgtitleThreeBtns";
    private static final String CHILD_STATICTEXT = "StaticText";
        
    static final String PG_ATTR_PROPERTY_NAMES = "pgAttrPropertyNames";
    static final String PG_ATTR_CONFIG_PAGE = "pgAttrConfigPage";
    
    private static final String TBL_COL_PROPERTY_NAME = "tblColPropertyName";
    private static final String TBL_COL_VALUE = "tblColValue";
    private static final String TBL_DATA_PROPERTY_NAME = "tblDataPropertyName";
    private static final String TBL_DATA_PROPERTY_HELP = "tblDataPropertyHelp";
    private static final String TBL_DATA_VALUE = "tblDataValue";
    
    private CCActionTableModel tblPropertyNamesModel = null;
    private CCPageTitleModel ptModel;
    private boolean submitCycle;
    
    /**
     * Creates a agent configurtion inheritance setting view bean.
     */
    public AgentConfigInheritViewBean() {
        super("AgentConfigInherit");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }
    
    protected void initialize() {
        if (!initialized) {
            String universalId = (String)getPageSessionAttribute(
                AgentProfileViewBean.UNIVERSAL_ID);
            if (universalId != null) {
                super.initialize();
                createPageTitleModel();
                createTableModel();
                registerChildren();
                initialized = true;
            }
        }
    }
    
    protected void registerChildren() {
        super.registerChildren();
        ptModel.registerChildren(this);
        registerChild(TBL_PROPERTY_NAMES, CCActionTable.class);
        tblPropertyNamesModel.registerChildren(this);
        registerChild(CHILD_STATICTEXT, CCStaticTextField.class);
    }

    protected View createChild(String name) {
        View view = null;
        if (name.equals(TBL_PROPERTY_NAMES)) {
            SerializedField szCache = (SerializedField)getChild(
                SZ_CACHE);
            populatePropertyNameTableModel((List)szCache.getSerializedObj());
            view = new CCActionTable(this, tblPropertyNamesModel, name);
        } else if (name.equals(PGTITLE_THREE_BTNS)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (tblPropertyNamesModel.isChildSupported(name)) {
            view = tblPropertyNamesModel.createChild(this, name);
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else if (name.equals(CHILD_STATICTEXT)) {
            view = new CCStaticTextField(this, name, null);       
        } else {
            view = super.createChild(name);
        }
        return view;
    }
    
    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.save");
        ptModel.setValue("button2", "button.reset");
        ptModel.setValue("button3", getBackButtonLabel());
    }

    /**
     * Displays servers and sites information.
     *
     * @param event Display Event.
     * @throws ModelControlException if unable to initialize model.
     */
    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event);
        Set propertyNames = (Set)getPageSessionAttribute(
            PG_ATTR_PROPERTY_NAMES);
        populatePropertyNameTableModel(propertyNames);
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req = getRequestContext().getRequest();
        return new AgentsModelImpl(req, getPageSessionAttributes());
    }

    private void createTableModel() {
        tblPropertyNamesModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/tblAgentInheritProperties.xml"));
        tblPropertyNamesModel.setTitleLabel("label.items");
        tblPropertyNamesModel.setActionValue(TBL_COL_PROPERTY_NAME,
            "table.inherit.property.name.column.name");
        tblPropertyNamesModel.setActionValue(TBL_COL_VALUE,
            "table.inherit.property.name.column.value");
    }

    private void populatePropertyNameTableModel(Collection propertyNames) {
        if (!submitCycle && (propertyNames != null)) {
            tblPropertyNamesModel.clearAll();
            SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
            String universalId = (String)getPageSessionAttribute(
                AgentProfileViewBean.UNIVERSAL_ID);
            String agentType = getAgentType();
            String curRealm = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            AgentsModel model = (AgentsModel)getModel();
            Set inheritedPropertyNames = model.getInheritedPropertyNames(
                curRealm, universalId);
            Map nameToSchemas = model.getAttributeSchemas(agentType,
                propertyNames);
            removeNonInheritable(nameToSchemas, propertyNames);
            try {
                ResourceBundle rb = AgentConfiguration.getServiceResourceBundle(
                    model.getUserLocale());

                String groupName = model.getAgentGroup(curRealm, universalId);
                Map groupValues = model.getGroupAttributeValues(
                    curRealm, groupName);
                ArrayList cache = new ArrayList();

                int counter = 0;
                for (Iterator i = propertyNames.iterator(); i.hasNext();
                    counter++) {
                    if (counter > 0) {
                        tblPropertyNamesModel.appendRow();
                    }
                    
                    String name = (String)i.next();
                    AttributeSchema as = (AttributeSchema)nameToSchemas.get(
                        name);
                    if (as != null) {
                        String displayName = rb.getString(as.getI18NKey());
                        tblPropertyNamesModel.setValue(TBL_DATA_PROPERTY_NAME,
                            displayName);

                        try {
                            String help = rb.getString(as.getI18NKey() +
                                ".help");
                            tblPropertyNamesModel.setValue(
                                TBL_DATA_PROPERTY_HELP, help);
                        } catch (MissingResourceException e) {
                            // need to clear the help value
                            tblPropertyNamesModel.setValue(
                                TBL_DATA_PROPERTY_HELP, "");
                        }

                        Object oValue = groupValues.get(name);
                        String value = "";
                        if (oValue != null) {
                            value = oValue.toString();
                            if (value.length() >= 2) {
                                value = value.substring(1, value.length()-1);
                            }
                        }

                        tblPropertyNamesModel.setValue(TBL_DATA_VALUE, value);
                        tblPropertyNamesModel.setSelectionVisible(counter, true);
                        tblPropertyNamesModel.setRowSelected(
                            inheritedPropertyNames.contains(name));
                        cache.add(name);
                    }
                }
                szCache.setValue(cache);
            } catch (AMConsoleException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
            } catch (SMSException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
            } catch (SSOException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
            }
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
     * Handles save profile request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException {
        submitCycle = true;
        CCActionTable table = (CCActionTable)getChild(TBL_PROPERTY_NAMES);
        table.restoreStateData();
        Integer[] selected = tblPropertyNamesModel.getSelectedRows();
        SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
        List list = (List)szCache.getSerializedObj();
        Map inherit = new HashMap();

        for (Iterator i = list.iterator(); i.hasNext(); ) {
            String name = (String)i.next();
            inherit.put(name, "0");
        }
        
        for (int i = 0; i < selected.length; i++) {
            String name = (String)list.get(selected[i].intValue());
            inherit.put(name, "1");
        }

        try {
            AgentsModel model = (AgentsModel)getModel();
            String universalId = (String)getPageSessionAttribute(
                AgentProfileViewBean.UNIVERSAL_ID);
            model.updateAgentConfigInheritance(universalId, inherit);
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                "agentcfg.inheritance.updated");
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }

        forwardTo();        
    }

    /**
     * Handles return to server configuration page request.
     *
     * @param event Request invocation event
     */
    public void handleButton3Request(RequestInvocationEvent event)
        throws ModelControlException
    {
        try {
            AMViewBeanBase vb = (AMViewBeanBase)getViewBean(
                Class.forName((String)
                    getPageSessionAttribute(PG_ATTR_CONFIG_PAGE)));
            removePageSessionAttribute(PG_ATTR_CONFIG_PAGE);
            removePageSessionAttribute(PG_ATTR_PROPERTY_NAMES);
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } catch (ClassNotFoundException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }
    
    protected String getBackButtonLabel() {
        return getBackButtonLabel("page.title.agent.config");
    }

    private void removeNonInheritable(
        Map nameToSchema,
        Collection propertyNames
    ) {
        propertyNames.remove(AMAdminConstants.ATTR_USER_PASSWORD);
        // Fix for OPENAM-440
        propertyNames.remove(AMAdminConstants.AGENT_REPOSITORY_LOCATION_ATTR);
        for (Iterator i = nameToSchema.keySet().iterator(); i.hasNext(); ) {
            String name = (String)i.next();
            if (name.equalsIgnoreCase(AMAdminConstants.ATTR_USER_PASSWORD)
                    || name.equalsIgnoreCase(AMAdminConstants.AGENT_REPOSITORY_LOCATION_ATTR)) {
                i.remove();
                break;
            }
        }
    }
    
    protected String getAgentType() {
        return (String)getPageSessionAttribute(
            AgentsViewBean.PG_SESSION_SUPERCEDE_AGENT_TYPE);
    }
}
