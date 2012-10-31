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
 * $Id: GenericAgentProfileViewBean.java,v 1.15 2009/01/07 16:03:06 veiming Exp $
 *
 */

package com.sun.identity.console.agentconfig;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.ChildDisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.sso.SSOException;
import com.sun.identity.common.configuration.AgentConfiguration;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.agentconfig.model.AgentsModel;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.property.AgentPropertyXMLBuilder;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SMSException;
import com.sun.web.ui.model.CCNavNode;
import com.sun.web.ui.view.alert.CCAlert;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/**
 * Generic Agent Profile View Bean.
 */
public class GenericAgentProfileViewBean 
    extends AgentProfileViewBean {
    static final String DEFAULT_DISPLAY_URL =
        "/console/agentconfig/GenericAgentProfile.jsp";
    static final String PS_TABNAME = "agentTabName";
    static final String TAB_PREFIX = "4600";
    static final int TAB_GENERAL_ID = 4600;
    static final int TAB_GROUP_ID = 4601;

    private Set attributeSchemas;
    
    /**
     * Creates an instance of this view bean.
     *
     * @param name Name of view bean.
     */
    public GenericAgentProfileViewBean(String name) {
        super(name);
    }

    /**
     * Creates an instance of this view bean.
     */
    public GenericAgentProfileViewBean() {
        super("GenericAgentProfile");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    protected void setSelectedTabNode(String realmName) {
        String strID = (String)getPageSessionAttribute(getTrackingTabIDName());
        int id = TAB_GENERAL_ID;

        if ((strID == null) || (strID.trim().length() == 0)) {
            HttpServletRequest req = getRequestContext().getRequest();
            strID = req.getParameter(getTrackingTabIDName());
            setPageSessionAttribute(getTrackingTabIDName(), strID);
        }

        if ((strID != null) && (strID.trim().length() > 0)) {
            id = Integer.parseInt(strID);
            tabModel.clear();
            tabModel.setSelectedNode(id);
        }
    }

    
    protected AMPropertySheetModel createPropertySheetModel(String type) {
        String agentType = getAgentType();
        AgentsModel model = (AgentsModel)getModel();
        String tabName = (String)getPageSessionAttribute(PS_TABNAME);
        String choice = (String)getPageSessionAttribute(
            AgentsViewBean.LOCAL_OR_NOT);
        AgentPropertyXMLBuilder blder = null;
        
        try {
            if (isLocalConfig(agentType)) {
                blder = new AgentPropertyXMLBuilder(
                    agentType, isGroup, true, tabName, model);
            } else {
                blder = new AgentPropertyXMLBuilder(
                    agentType, isGroup, 
                    is2dot2Agent() || isAgentAuthenticator(), tabName, model);
            }
            attributeSchemas = blder.getAttributeSchemas();
            return new AMPropertySheetModel(blder.getXML(
                inheritedPropertyNames, choice));
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
        return null;
    }
    
    protected void setDefaultValues(String type)
        throws AMConsoleException {
        if (propertySheetModel != null) {
            AgentsModel model = (AgentsModel)getModel();
            String universalId = (String)getPageSessionAttribute(UNIVERSAL_ID);
            
            try {
                if (!submitCycle) {
                    String curRealm = (String) getPageSessionAttribute(
                        AMAdminConstants.CURRENT_REALM);
                    // !isGroup should that we do not inherit value is
                    // the identity is a group.
                    Map attrValues = model.getAttributeValues(curRealm,
                        universalId, !isGroup);
                    removeAgentRootURLKey(attrValues);
                    propertySheetModel.clear();
                    AMPropertySheet prop = (AMPropertySheet)getChild(
                        PROPERTY_ATTRIBUTE);
                    prop.setAttributeValues(attrValues, model);
                } 
            } catch (AMConsoleException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
                disableButton("button1", true);
            }

            String[] uuid = {universalId};
            propertySheetModel.setValues(PROPERTY_UUID, uuid, model);
        }
    }
    
    protected Map getFormValues()
        throws AMConsoleException, ModelControlException {
        AMPropertySheet prop = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
        Map values = prop.getAttributeValues(getPropertyNames());
        addAgentRootURLKey(values);
        return values;
    }
    
    protected HashSet getPropertyNames() {
        String agentType = getAgentType();
        HashSet names = new HashSet();
        for (Iterator i = attributeSchemas.iterator(); i.hasNext(); ) {
            AttributeSchema as = (AttributeSchema)i.next();
            names.add(as.getName());
        }
        if (isLocalConfig(agentType)) {
            names.remove(AgentsViewBean.DESCRIPTION);
         }
        return names;        
    }

    protected void createTabModel() {
        String agentType = getAgentType();
        if (isLocalConfig(agentType)) {
            agentType = AgentsViewBean.AGENT_2_2;
        }
        
        if (agentType != null) {
            super.createTabModel();
            AgentsModel model = (AgentsModel)getModel();
            AgentTabManager mgr = AgentTabManager.getInstance();
            List tabs = mgr.getTabs(agentType);
            if ((tabs != null) && !tabs.isEmpty()) {
                for (int i = 0; i < tabs.size(); i++) {
                    String tabName = (String)tabs.get(i);
                    tabModel.addNode(new CCNavNode(
                        Integer.parseInt(TAB_PREFIX + i),
                    model.getLocalizedString(
                        "tab.label." + agentType + "." + tabName), "", ""));
                 }
            } else {
                 tabModel.addNode(new CCNavNode(TAB_GENERAL_ID,
                    "tab.general", "", ""));
            }

            String group = (String)getPageSessionAttribute(IS_GROUP);
            if ((group != null) && group.equals("true")) {
                tabModel.addNode(new CCNavNode(TAB_GROUP_ID, "tab.group",
                    "", ""));
            }
        }
    }
    
    public boolean beginBtnInheritDisplay(ChildDisplayEvent event) {
        String choice = (String)getPageSessionAttribute(
                AgentsViewBean.LOCAL_OR_NOT);
        return super.beginBtnInheritDisplay(event) && !is2dot2Agent() && 
            !isAgentAuthenticator() && !isLocalConfig(getAgentType());
    }

     /**
     * Returns <code>true</code> if tab set has more than one tab.
     *
     * @param event Child Display Event.
     * @return <code>true</code> if tab set has more than one tab.
     */
    public boolean beginTabCommonDisplay(ChildDisplayEvent event) {
        return (tabModel.getNodeCount() > 1);
    }

    /**
     * Handles tab selection.
     *
     * @param event Request Invocation Event.
     * @param nodeID Tab Id.
     */
    public void nodeClicked(RequestInvocationEvent event, int nodeID) {
        String agentType = getAgentType();
        AgentTabManager mgr = AgentTabManager.getInstance();
        boolean forward = false;
        
        if (nodeID == TAB_GENERAL_ID) {
            removePageSessionAttribute(PS_TABNAME);
            setPageSessionAttribute(getTrackingTabIDName(),
                Integer.toString(nodeID));
        } else if (nodeID == TAB_GROUP_ID) {
            AgentGroupMembersViewBean vb = 
                (AgentGroupMembersViewBean)getViewBean(
                    AgentGroupMembersViewBean.class);
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
            forward = true;
        } else {
            List tabs = mgr.getTabs(agentType);
            String strIdx = Integer.toString(nodeID);
            strIdx = strIdx.substring(TAB_PREFIX.length());
            int idx = Integer.parseInt(strIdx);

            String tabName = (String)tabs.get(idx);
            setPageSessionAttribute(PS_TABNAME, tabName);
            setPageSessionAttribute(getTrackingTabIDName(),
                Integer.toString(nodeID));
        }
        
        if (!forward) {
            try {
                Class clazz = AgentsViewBean.getAgentCustomizedViewBean(
                    agentType);
                AMViewBeanBase vb = (AMViewBeanBase)getViewBean(clazz);
                passPgSessionMap(vb);
                vb.forwardTo(getRequestContext());
            } catch (ClassNotFoundException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
            }
        }
    }
    
    protected boolean isFirstTab() {
        String agentType = getAgentType();
        String tabName = (String)getPageSessionAttribute(PS_TABNAME);
        return AgentTabManager.getInstance().isFirstTab(agentType, tabName);
    }
    
    protected boolean handleRealmNameInTabSwitch(RequestContext rc) {
        return false;
    }
    
    private boolean isLocalConfig(String agentType) {
        String choice = (String)getPageSessionAttribute(
                AgentsViewBean.LOCAL_OR_NOT);
        
        return (choice != null && choice.equals(AgentsViewBean.PROP_LOCAL) &&
                (agentType.equals(AgentsViewBean.AGENT_WEB) ||
                agentType.equals(AgentsViewBean.DEFAULT_ID_TYPE)));
    }
    
    protected String getAgentType() {
        return (String)getPageSessionAttribute(
            AgentsViewBean.PG_SESSION_SUPERCEDE_AGENT_TYPE);
    }

    private void removeAgentRootURLKey(Map map) {
        String type = getAgentType();
        if (!checkAgentType(type) && !isAgentAuthenticator()) {
            return;
        }
        
        Set values = (Set)map.get(AgentConfiguration.DEVICE_KEY);
        if ((values != null) && !values.isEmpty()) {
            Set newValues = new HashSet();
            for (Iterator i = values.iterator(); i.hasNext(); )  {
                String val = (String)i.next();
                if (val.startsWith(AgentConfiguration.AGENT_ROOT_URL)) {
                    val = val.substring(
                        AgentConfiguration.AGENT_ROOT_URL.length());
                }
                newValues.add(val);
            }
            map.put(AgentConfiguration.DEVICE_KEY, newValues);
        }
    }

    private void addAgentRootURLKey(Map map) {
        String type = getAgentType();
        if (!checkAgentType(type) && !isAgentAuthenticator()) {
            return;
        }

        Set values = (Set)map.get(AgentConfiguration.DEVICE_KEY);
        if ((values != null) && !values.isEmpty()) {
            Set newValues = new HashSet();
            for (Iterator i = values.iterator(); i.hasNext(); )  {
                String val = AgentConfiguration.AGENT_ROOT_URL + 
                    (String)i.next();
                newValues.add(val);
            }
            map.put(AgentConfiguration.DEVICE_KEY, newValues);
        }
    }

}
