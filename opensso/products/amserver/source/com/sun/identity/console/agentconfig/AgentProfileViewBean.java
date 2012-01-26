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
 * $Id: AgentProfileViewBean.java,v 1.14 2009/11/10 23:20:15 asyhuang Exp $
 *
 */

package com.sun.identity.console.agentconfig;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.ChildDisplayEvent;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.HREF;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.common.configuration.AgentConfiguration;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.agentconfig.model.AgentsModel;
import com.sun.identity.console.agentconfig.model.AgentsModelImpl;
import com.sun.identity.console.base.AMPostViewBean;
import com.sun.identity.idm.AMIdentity;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.model.CCTabsModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCButton;
import com.sun.web.ui.view.html.CCDropDownMenu;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.view.tabs.CCTabs;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.servlet.http.HttpServletRequest;

/**
 * Base class for all agent profile view bean.
 */
public abstract class AgentProfileViewBean
        extends AMPrimaryMastHeadViewBean {
    
    public static final String MODIFIED_PROFILE = "modifiedProfile";
    
    protected static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    private static final String PGTITLE_TWO_BTNS = "pgtitleTwoBtns";
    private static final String PG_SESSION_AGENT_TAB = "pgAgentConfigTab";
    private static final String UPDATED_PROFILE = "uProfile";
    
    static final String UNIVERSAL_ID = "universalId";
    static final String IS_GROUP = "isGroup";
    static final String PROPERTY_UUID = "tfUUID";
    static final String CHILD_AGENT_GROUP = "agentgroup";
    static final String BTN_INHERIT = "btnInherit";
    static final String BTN_DUMP = "btnDump";
    
    protected CCPageTitleModel ptModel;
    protected AMPropertySheetModel propertySheetModel;
    protected boolean submitCycle;
    protected Set inheritedPropertyNames;
    protected boolean isGroup;
    protected boolean rcSet;
    
    AgentProfileViewBean(String pageName) {
        super(pageName);
    }
    
    protected void setRequestContentInitialize(RequestContext rc) {
        HttpServletRequest req = rc.getRequest();
        Map map = req.getParameterMap();
        boolean bTab = false;
        for (Iterator i = map.keySet().iterator(); i.hasNext() && !bTab; ) {
            String key = (String)i.next();
            bTab = key.endsWith("tabCommon.TabHref");
        }
        
        if (bTab) {
            if (!rcSet) {
                rcSet = true;
                createTabModel();
                registerChild(TAB_COMMON, CCTabs.class);
            }
        } else {
            initialize();
        }
    }
    
    protected void initialize() {
        if (!initialized) {
            String universalId = (String) getPageSessionAttribute(
                UNIVERSAL_ID);
            if ((universalId != null) && (universalId.length() > 0)) {
                isGroup = ((AgentsModel)
                    getModel()).isAgentGroup(universalId);
                if (isGroup) {
                    setPageSessionAttribute(IS_GROUP, "true");
                } else {
                    setPageSessionAttribute(IS_GROUP, "false");
                }
    
                initialized = createPropertyModel();

                if (initialized) {
                    super.initialize();
                    createPageTitleModel();
                    createTabModel();
                    registerChildren();
                }
            }
        }
    }
    
    protected boolean createPropertyModel() {
        boolean created = false;
        String type = getAgentType();
        
        if ((type != null) && (type.trim().length() > 0)) {
            String curRealm = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            AgentsModel model = (AgentsModel)getModel();
            String universalId = (String)getPageSessionAttribute(UNIVERSAL_ID);
            String choice = (String) getPageSessionAttribute(
                    AgentsViewBean.LOCAL_OR_NOT);
            if (checkAgentType(type)) {
                inheritedPropertyNames = (!isGroup && !is2dot2Agent() &&
                    !isAgentAuthenticator() &&
                    !choice.equals(AgentsViewBean.PROP_LOCAL)) ?
                        model.getInheritedPropertyNames(curRealm, universalId) :
                        Collections.EMPTY_SET;
            } else {
                inheritedPropertyNames = (!isGroup && !is2dot2Agent() &&
                    !isAgentAuthenticator()) ?
                    model.getInheritedPropertyNames(curRealm, universalId) :
                    Collections.EMPTY_SET;
            }
            AMPropertySheetModel psModel = createPropertySheetModel(type);
            if (psModel != null) {
                propertySheetModel = psModel;
                propertySheetModel.clear();
            }
            created = true;
        }
        
        return created;
    }
    
    protected AMModel getModelInternal() {
        HttpServletRequest req =
                RequestManager.getRequestContext().getRequest();
        return new AgentsModelImpl(req, getPageSessionAttributes());
    }
    
    
    protected void registerChildren() {
        super.registerChildren();
        
        if (ptModel != null) {
            ptModel.registerChildren(this);
        }
        registerChild(PGTITLE_TWO_BTNS, CCPageTitle.class);
        registerChild(BTN_INHERIT, CCButton.class);
        registerChild(BTN_DUMP, CCButton.class);
        registerChild(TAB_COMMON, CCTabs.class);
        if (propertySheetModel != null) {
            registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
            propertySheetModel.registerChildren(this);
        }
    }
    
    protected View createChild(String name) {
        View view = null;
        
        if ((ptModel != null) && ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else if (name.equals(PROPERTY_ATTRIBUTE)) {
            view = new AMPropertySheet(this, propertySheetModel, name);
        } else if ((propertySheetModel != null) &&
                propertySheetModel.isChildSupported(name)
                ) {
            view = propertySheetModel.createChild(this, name, getModel());
        } else if (name.equals(AMAdminConstants.DYN_LINK_COMPONENT_NAME)) {
            view = new HREF(this, name, "");
        } else if (name.equals(PGTITLE_TWO_BTNS)) {
            view = new CCPageTitle(this, ptModel, name);
        } else {
            view = super.createChild(name);
        }
        return view;
    }
    
    protected void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
                getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.save");
        ptModel.setValue("button2", "button.reset");
        ptModel.setValue("button3", getBackButtonLabel());
    }
    
    protected String getBackButtonLabel() {
        return getModel().getLocalizedString("agentconfig.btn.back");
    }
    
    /**
     * Sets the property sheet values and agent title.
     *
     * @param event Display Event.
     * @throws ModelControlException if cannot access to framework model.
     */
    public void beginDisplay(DisplayEvent event)
    throws ModelControlException {
        super.beginDisplay(event);
        setPropertySheetValues();
        setAgentTitle();
        
        String updated = (String)removePageSessionAttribute(UPDATED_PROFILE);
        if ((updated != null) && updated.equals("true")) {
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                "message.updated");
        }

        String modified = (String)removePageSessionAttribute(MODIFIED_PROFILE);
        if ((modified != null) && modified.equals("true")) {
            setInlineAlertMessage(CCAlert.TYPE_WARNING, "message.warning",
                "message.profile.modified");
        }
    }
    
    protected void setPropertySheetValues(){
        String universalId = (String)getPageSessionAttribute(UNIVERSAL_ID);
        String agentType = getAgentType();
        AgentsModel model = (AgentsModel)getModel();
        try {
            setDefaultValues(agentType);
            
            String choice = (String) getPageSessionAttribute(
                    AgentsViewBean.LOCAL_OR_NOT);
            if (checkAgentType(agentType)) {
                if (choice != null && !choice.equals(AgentsViewBean.PROP_LOCAL)
                    && !isGroup && !is2dot2Agent()  && !isAgentAuthenticator() 
                    &&  isFirstTab()
                ) {
                    setProperty(model, agentType, universalId);
                }
            } else {
                if (!isGroup && !is2dot2Agent() && !isAgentAuthenticator() && 
                    isFirstTab()
                ) {
                    setProperty(model, agentType, universalId);
                }
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
        }

        propertySheetModel.setValue(PROPERTY_UUID, universalId);
    }
    
    private void setProperty(AgentsModel model, String type, String universalId)
        throws AMConsoleException {
        Set groups = new HashSet();
        Set set = new HashSet(2);
        set.add(type);
        String curRealm = (String) getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        model.getAgentGroupNames(curRealm, set, "*", groups);
        CCDropDownMenu menu = (CCDropDownMenu)getChild(
                CHILD_AGENT_GROUP);
        Set groupNames = new TreeSet();
        for (Iterator i = groups.iterator(); i.hasNext(); ) {
            AMIdentity amid = (AMIdentity)i.next();
            groupNames.add(amid.getName());
        }
        OptionList optList = createOptionList(groupNames);
        optList.add(0, model.getLocalizedString("agentgroup.none"), "");
        menu.setOptions(optList);
        
        String group = model.getAgentGroup(curRealm, universalId);
        if (group != null) {
            menu.setValue(group);
        }
    }
    
    protected void setAgentTitle() {
        AgentsModel model = (AgentsModel)getModel();
        String universalId = (String)getPageSessionAttribute(UNIVERSAL_ID);
        
        try {
            String title = model.getLocalizedString("edit.agentconfig.title");
            String displayName = model.getDisplayName(universalId);
            Object[] param = { displayName };
            ptModel.setPageTitleText(MessageFormat.format(title, param));
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
        }
    }
    
    protected static String getValueFromMap(Map attrValues, String name) {
        Set set = (Set)attrValues.get(name);
        return ((set != null) && !set.isEmpty()) ?
            (String)set.iterator().next() : "";
    }
    
    protected static Set putStringInSet(String value) {
        Set set = new HashSet(2);
        set.add(value);
        return set;
    }
    
    /**
     * Handles Save button click.
     *
     * @param event Request Invocation Event.
     * @throws ModelControlException if cannot access to framework model.
     */
    public void handleButton1Request(RequestInvocationEvent event)
    throws ModelControlException {
        submitCycle = true;
        boolean bRefresh = false;
        AgentsModel model = (AgentsModel)getModel();
        String universalId = (String)getPageSessionAttribute(UNIVERSAL_ID);
        String type = getAgentType();
        String choice = (String) getPageSessionAttribute(
            AgentsViewBean.LOCAL_OR_NOT);
        try {
            Map values = getFormValues();
            if (checkAgentType(type)) {
                if (!isGroup && !is2dot2Agent() && !isAgentAuthenticator() &&
                        (choice != null) && 
                        !choice.equals(AgentsViewBean.PROP_LOCAL)) {
                    for (Iterator i = inheritedPropertyNames.iterator();
                        i.hasNext(); ) {
                        values.remove(i.next());
                    }
                     if(type.equals(AgentsViewBean.DEFAULT_ID_TYPE)){
                         Iterator itr = values.keySet().iterator();
                         while (itr.hasNext()) {
                              String name = (String) itr.next();                             
                              Set v = (Set) values.get(name);
                              if ((v != null) && !v.isEmpty() && (v.size()==1)) {
                                   Iterator itr2 = v.iterator();
                                   while (itr2.hasNext()) {
                                       String subv = (String) itr2.next();
                                       subv.trim();
                                       if(subv.length()==0){                                           
                                           values.put(name, null);
                                           break;
                                       }
                                   }
                              }                             
                         }
                     }
                }
            } else {
                if (!isGroup && !is2dot2Agent() && !isAgentAuthenticator()) {
                    for (Iterator i = inheritedPropertyNames.iterator();
                        i.hasNext(); ) {
                        values.remove(i.next());
                    }
                }
                
            }
            model.setAttributeValues(universalId, values);
            String curRealm = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            
            if (checkAgentType(type)) {
                if (!isGroup && !is2dot2Agent() && isFirstTab() &&
                    !isAgentAuthenticator() && (choice != null) && 
                    !choice.equals(AgentsViewBean.PROP_LOCAL)) {
                    String agentGroup = getDisplayFieldStringValue(
                        CHILD_AGENT_GROUP);
                    bRefresh =model.setGroup(curRealm, universalId, agentGroup);
                    
                    String status = getDisplayFieldStringValue(
                        AgentsViewBean.ATTR_CONFIG_REPO);
                    if (status.equals("local")) {
                        setPageSessionAttribute(AgentsViewBean.LOCAL_OR_NOT, 
                            AgentsViewBean.PROP_LOCAL);
                        bRefresh = true;
                    }
                }
            } else {
                if (!isGroup && !is2dot2Agent() && !isAgentAuthenticator() &&
                    isFirstTab()
                ) {
                    String agentGroup = getDisplayFieldStringValue(
                        CHILD_AGENT_GROUP);
                    bRefresh =model.setGroup(curRealm, universalId, agentGroup);
                }
            }
            
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "message.updated");
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
            bRefresh = false;
        }

        if (bRefresh) {
            setPageSessionAttribute(UPDATED_PROFILE, "true");
            AMPostViewBean vb = (AMPostViewBean) getViewBean(
                AMPostViewBean.class);
            passPgSessionMap(vb);
            String url = this.getDefaultDisplayURL();
            int idx = url.indexOf("/", 1);
            url = ".." + url.substring(idx);
            vb.setTargetViewBeanURL(url);
            vb.forwardTo(getRequestContext());
        } else {
            forwardTo();
        }
    }
    
    /**
     * Disables inheritance setting button if the agent does not belong
     * a group.
     *
     * @param event Child Display Event.
     * @return <code>true</code> if the agent belongs to a group.
     */
    public boolean beginBtnInheritDisplay(ChildDisplayEvent event) {
        AgentsModel model = (AgentsModel)getModel();
        String universalId = (String)getPageSessionAttribute(UNIVERSAL_ID);
        disableButton(BTN_INHERIT, false);
        String curRealm = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
        try {
            String groupId = model.getAgentGroup(curRealm, universalId);
            if ((groupId == null) || (groupId.trim().length() == 0)) {
                disableButton(BTN_INHERIT, true);
            }
        } catch (AMConsoleException ex) {
            disableButton(BTN_INHERIT, true);
        }
        return !isGroup;
    }

    public boolean beginBtnDumpDisplay(ChildDisplayEvent event) {
        AgentsModel model = (AgentsModel)getModel();
        String universalId = (String)getPageSessionAttribute(UNIVERSAL_ID);
        disableButton(BTN_DUMP, false);
        return true;
    }
    
    /**
     * Handles inheritance setting button click.
     *
     * @param event Request Invocation Event.
     * @throws ModelControlException if cannot access to framework model.
     */
    public void handleBtnInheritRequest(RequestInvocationEvent event)
    throws ModelControlException {
        AgentConfigInheritViewBean vb =(AgentConfigInheritViewBean)
        getViewBean(AgentConfigInheritViewBean.class);
        vb.setPageSessionAttribute(
                AgentConfigInheritViewBean.PG_ATTR_CONFIG_PAGE,
                getClass().getName());
        vb.setPageSessionAttribute(
                AgentConfigInheritViewBean.PG_ATTR_PROPERTY_NAMES,
                getPropertyNames());
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    public void handleBtnDumpRequest(RequestInvocationEvent event) throws ModelControlException {        
        AgentDumpViewBean vb =(AgentDumpViewBean)getViewBean(AgentDumpViewBean.class);
        getViewBean(AgentDumpViewBean.class);
        vb.setPageSessionAttribute(AgentDumpViewBean.PG_ATTR_CONFIG_PAGE, getClass().getName());
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Handles reset button click.
     *
     * @param event Request Invocation Event.
     * @throws ModelControlException if cannot access to framework model.
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        forwardTo();
    }
    
    /**
     * Handles back to main page button click.
     *
     * @param event Request Invocation Event.
     * @throws ModelControlException if cannot access to framework model.
     */
    public void handleButton3Request(RequestInvocationEvent event) {
        AgentsViewBean vb = (AgentsViewBean)getViewBean(AgentsViewBean.class);
        removePageSessionAttribute(getTrackingTabIDName());
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }
    
    protected void createTabModel() {
        tabModel = new CCTabsModel();
    }
    
    protected String getTrackingTabIDName() {
        return PG_SESSION_AGENT_TAB;
    }
    
    protected boolean is2dot2Agent() {
        String agentType = getAgentType();
        return agentType.equals(AgentConfiguration.AGENT_TYPE_2_DOT_2_AGENT);
    }

    protected boolean isAgentAuthenticator() {
        String agentType = getAgentType();
        return (agentType != null) &&
            agentType.equals(AgentConfiguration.AGENT_TYPE_AGENT_AUTHENTICATOR);
    }
    
    protected boolean checkAgentType(String type) {
        return (type.equals(AgentsViewBean.AGENT_WEB) ||
                type.equals(AgentsViewBean.DEFAULT_ID_TYPE));
    }
    
    protected String getAgentType() {
        return (String)getPageSessionAttribute(
            AgentsViewBean.PG_SESSION_SUPERCEDE_AGENT_TYPE);
    }
    protected abstract boolean isFirstTab();
    
    protected abstract void setDefaultValues(String type)
    throws AMConsoleException;
    
    protected abstract AMPropertySheetModel createPropertySheetModel(
            String type);
    
    protected abstract Map getFormValues()
    throws AMConsoleException, ModelControlException;
    
    protected abstract HashSet getPropertyNames();
}
