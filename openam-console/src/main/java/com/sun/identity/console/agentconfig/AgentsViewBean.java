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
 * $Id: AgentsViewBean.java,v 1.18 2009/12/22 17:49:04 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.agentconfig;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.ChildDisplayEvent;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.HREF;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.configuration.AgentConfiguration;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.AMViewConfig;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMFormatUtils;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.components.view.html.SerializedField;
import com.sun.identity.console.realm.HasEntitiesTabs;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.sm.SMSSchema;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCButton;
import com.sun.web.ui.view.html.CCTextField;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.view.table.CCActionTable;
import com.sun.identity.idm.IdRepoException;
import com.iplanet.sso.SSOException;
import com.sun.identity.idm.IdType;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import com.sun.identity.console.agentconfig.model.AgentsModel;
import com.sun.identity.console.agentconfig.model.AgentsModelImpl;
import com.sun.identity.console.base.AMPostViewBean;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.realm.RMRealmViewBean;
import com.sun.web.ui.model.CCBreadCrumbsModel;
import com.sun.web.ui.model.CCNavNode;
import com.sun.web.ui.view.breadcrumb.CCBreadCrumbs;
import com.sun.web.ui.view.tabs.CCTabs;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.TreeSet;

/**
 * Main page of agent configuration.
 */
public class AgentsViewBean
    extends AMPrimaryMastHeadViewBean
    implements HasEntitiesTabs
{
    private static final String PARENTAGE_PATH = "parentagepath";
    private static final String PARENTAGE_PATH_HREF = "parentagepathHref";
    private static final String TXT_ROOT = "txtRoot";

    static final String GENERIC_VIEW_BEAN =
        "com.sun.identity.console.agentconfig.GenericAgentProfileViewBean";
    static final String DEFAULT_DISPLAY_URL =
        "/console/agentconfig/Agents.jsp";
    public static final String PG_SESSION_AGENT_TYPE = "agenttype";
    public static final String PG_SESSION_SUPERCEDE_AGENT_TYPE = 
        "superagenttype";
    static final String PG_SESSION_AGENT_MAIN_TAB = "pgAgentMainTab";
    private static final String CACHE_AGENTS = "agents";
    private static final String CACHE_AGENT_GROUPS = "agentgroups";
    
    private static final String TF_FILTER = "tfFilter";
    private static final String TF_GROUP_FILTER = "tfGroupFilter";

    private static final String BTN_SEARCH = "btnSearch";

    private static final String TBL_SEARCH = "tblSearch";
    private static final String TBL_BUTTON_ADD = "tblButtonAdd";
    private static final String TBL_BUTTON_DELETE = "tblButtonDelete";
    private static final String TBL_SEARCH_GROUP = "tblSearchGroup";
    private static final String TBL_BUTTON_ADD_GROUP = "tblButtonGroupAdd";
    private static final String TBL_BUTTON_DELETE_GROUP ="tblButtonGroupDelete";
    
    private static final String TBL_COL_NAME = "tblColName";
    private static final String TBL_DATA_NAME = "tblDataName";
    private static final String TBL_DATA_UNIVERSALNAME = "tblDataUniversalName";
    private static final String TBL_DATA_ACTION_HREF = "tblDataActionHref";
    private static final String TBL_COL_TYPE = "tblColType";
    private static final String TBL_DATA_TYPE = "tblDataType";
    private static final String TBL_COL_LOCAL = "tblColLocal";
    private static final String TBL_DATA_LOCAL = "tblDataLocal";

    private static final String TBL_COL_NAME_GROUP = "tblColGroupName";
    private static final String TBL_DATA_NAME_GROUP = "tblDataGroupName";
    private static final String TBL_COL_TYPE_GROUP = "tblColGroupType";
    private static final String TBL_DATA_TYPE_GROUP = "tblDataGroupType";

    private static final String TBL_DATA_UNIVERSALNAME_GROUP = 
        "tblDataUniversalGroupName";
    private static final String TBL_DATA_ACTION_HREF_GROUP = 
        "tblDataActionGroupHref";

    private static final String PAGETITLE = "pgtitle";
    public static final String DEFAULT_ID_TYPE = "J2EEAgent";
    static final String ATTR_NAME_AGENT_TYPE = "Type=";
    
    public static String DESCRIPTION = "description";
    public static final String LOCAL_OR_NOT = "localornot";
    final static String ATTR_CONFIG_REPO =
        "com.sun.identity.agents.config.repository.location";
    public static final String PROP_LOCAL="local";
    public static final String PROP_CENTRAL="centralized";
    public static final String AGENT_2_2 = "2.2_Agent";
    public static final String AGENT_WEB = "WebAgent";

    private CCActionTableModel tblModel;
    private CCActionTableModel tblGroupModel;
    private CCPageTitleModel ptModel;
    private boolean tblModelPopulated = false;
    private CCNavNode selectedNode;
    private static Map agentViewBeans = new HashMap();
    public boolean combinedType;
    private boolean centralizable;
    public Set supportedTypes = new TreeSet();
    
    static {
        ResourceBundle rb = ResourceBundle.getBundle("agentViewBean");
        for (Enumeration e = rb.getKeys(); e.hasMoreElements(); ) {
            String k = (String)e.nextElement();
            agentViewBeans.put(k, rb.getString(k));
        }
    }
    
    /**
     * Creates an instance of this view bean.
     */
    public AgentsViewBean() {
        super("Agents");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    protected void initialize() {
        if (!initialized) {
            String agentType = (String) getPageSessionAttribute(
                AgentsViewBean.PG_SESSION_AGENT_TYPE);
            if (agentType != null) {
                super.initialize();
                initialized = true;
                createPageTitleModel();
                createTableModel();
                registerChildren();
            }
        }
    }
 
    protected void createTabModel() {
        if (tabModel == null) {
            AMViewConfig amconfig = AMViewConfig.getInstance();
            String realmName = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            if (realmName != null) {
                tabModel = amconfig.getTabsModel("realms", realmName,
                    getRequestContext().getRequest());
                String tabIdx = (String) getPageSessionAttribute(
                    getTrackingTabIDName());
                if (tabIdx != null) {
                    selectedNode = addAgentsTab(Integer.parseInt(tabIdx));
                    setPageSessionAttribute("CCTabs.SelectedTabId", tabIdx);
                } else {
                    addAgentsTab(-1);
                }
                registerChild(TAB_COMMON, CCTabs.class);
            }
            tabModel.clear();
            registerChild(TAB_COMMON, CCTabs.class);
        }
    }

    protected void registerChildren() {
        super.registerChildren();
        registerChild(TF_FILTER, CCTextField.class);
        registerChild(TF_GROUP_FILTER, CCTextField.class);
        registerChild(BTN_SEARCH, CCButton.class);
        registerChild(PARENTAGE_PATH, CCBreadCrumbs.class);
        registerChild(PARENTAGE_PATH_HREF, HREF.class);
        registerChild(PAGETITLE, CCPageTitle.class);
        registerChild(TBL_SEARCH, CCActionTable.class);
        registerChild(TBL_SEARCH_GROUP, CCActionTable.class);
        ptModel.registerChildren(this);
        tblModel.registerChildren(this);
        tblGroupModel.registerChildren(this);
    }

    protected View createChild(String name) {
        View view = null;

        if (name.equals(PARENTAGE_PATH)) {
            view = createParentagePath(name);
        } else if (name.equals(PARENTAGE_PATH_HREF)) {
            view = new HREF(this, name, null);
        } else if (name.equals(TAB_COMMON)) {
            view = new CCTabs(this, tabModel, name);
        } else if (name.equals(TBL_SEARCH)) {
            populateTableModelEx();
            view = new CCActionTable(this, tblModel, name);
        } else if (name.equals(TBL_SEARCH_GROUP)) {
            populateTableModelEx();
            view = new CCActionTable(this, tblGroupModel, name);
        } else if (name.equals(PAGETITLE)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if ((tblModel != null) && tblModel.isChildSupported(name)) {
            view = tblModel.createChild(this, name);
        } else if ((tblGroupModel != null) && 
            tblGroupModel.isChildSupported(name)) {
            view = tblGroupModel.createChild(this, name);
        } else if ((ptModel != null) && ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else {
            view = super.createChild(name);
        }

        return view;
    }
    
    protected boolean handleRealmNameInTabSwitch(RequestContext rc) {
        createTabModel();
        return false;
    }
    
    /**
     * Returns <code>true>/code> if tab set of more than one tab.
     *
     * @param event Child Display Event.
     * @return <code>true>/code> if tab set of more than one tab.
     */
    public boolean beginTabCommonDisplay(ChildDisplayEvent event) {
        return (tabModel.getNodeCount() > 1);
    }

    /**
     * Sets the agent title and populates the agent and agent group
     * action table.
     *
     * @param event Display Event.
     * @throws ModelControlException if cannot access to framework model.
     */
    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        super.beginDisplay(event, false);
        resetButtonState(TBL_BUTTON_DELETE);
        AgentsModel model = (AgentsModel)getModel();
        String agentType = getDisplayIDType();
        
        Object[] param = {agentType};
        ptModel.setPageTitleText(model.getLocalizedString("agenttype." +
            agentType));
        ptModel.setPageTitleHelpMessage(model.getLocalizedString(
                "agenttype."+agentType+".help")); 
        tblModel.setTitle(MessageFormat.format(
            model.getLocalizedString("table.agents.title.name"), param));
        tblModel.setTitleLabel(MessageFormat.format(
            model.getLocalizedString("table.agents.summary"), param));
        tblModel.setSummary(MessageFormat.format(
            model.getLocalizedString("table.agents.summary"), param));

        tblGroupModel.setTitle(MessageFormat.format(
            model.getLocalizedString("table.agent.groups.title.name"), param));
        tblGroupModel.setTitleLabel(MessageFormat.format(
            model.getLocalizedString("table.agent.groups.summary"), param));
        tblGroupModel.setSummary(MessageFormat.format(
            model.getLocalizedString("table.agent.groups.summary"), param));
       
        getAgentNames();
        CCButton b = (CCButton)getChild(TBL_BUTTON_DELETE);
        b.setDisabled(true);
        b = (CCButton)getChild(TBL_BUTTON_DELETE_GROUP);
        b.setDisabled(true);
    }
    
    /**
     * Handles tab selected event.
     *
     * @param event Request Invocation Event.
     * @param nodeID Selected Node ID.
     */
    public void nodeClicked(RequestInvocationEvent event, int nodeID) {
        boolean forwardTo = false;
        String strNodeId = Integer.toString(nodeID);
        if (strNodeId.length() > 2) {
            String prefix = strNodeId.substring(0, 2);
            if (prefix.equals(AMAdminConstants.TAB_AGENT_PREFIX)) {
                setPageSessionAttribute(getTrackingTabIDName(), 
                    Integer.toString(nodeID));
                AMViewConfig amconfig = AMViewConfig.getInstance();
                List list = amconfig.getSupportedAgentTypes(getModel());
                strNodeId = strNodeId.substring(2);
                int idx = Integer.parseInt(strNodeId);
                if (idx < list.size()) {
                    setPageSessionAttribute(PG_SESSION_AGENT_TYPE, 
                        (String)list.get(idx));
                    AMPostViewBean vb = (AMPostViewBean)getViewBean(
                        AMPostViewBean.class);
                    passPgSessionMap(vb);
                    vb.setTargetViewBeanURL("../agentconfig/Agents");
                    vb.forwardTo(getRequestContext());
                    forwardTo = true;
                }
            }
        }
        
        if (!forwardTo) {
            try {
                AMViewBeanBase vb = getTabNodeAssociatedViewBean("realms",
                    nodeID);
                String tmp = (String) getPageSessionAttribute(
                    AMAdminConstants.PREVIOUS_REALM);
                vb.setPageSessionAttribute(AMAdminConstants.PREVIOUS_REALM,tmp);

                tmp = (String) getPageSessionAttribute(
                    AMAdminConstants.CURRENT_REALM);
                vb.setPageSessionAttribute(AMAdminConstants.CURRENT_REALM, tmp);

                tmp = (String) getPageSessionAttribute(
                    AMAdminConstants.PREVIOUS_TAB_ID);
                vb.setPageSessionAttribute(AMAdminConstants.PREVIOUS_TAB_ID, 
                    tmp);
                unlockPageTrailForSwapping();
                passPgSessionMap(vb);
                vb.forwardTo(getRequestContext());
            } catch (AMConsoleException e) {
                debug.error("AgentViewBean.nodeClicked", e);
                forwardTo();
            }
        }
    }
    
    /**
     * Handles 'Back to' button request. In this case, it takes you back
     * to the realm view.
     *
     * @param event Request Invocation Event.
     */
    public void handleButton1Request(RequestInvocationEvent event) {
        backTrail();
        // reset the current realm to be the realm that was being viewed before
        // the profile page was opened.
        String tmp = 
            (String)getPageSessionAttribute(AMAdminConstants.PREVIOUS_REALM);
        setPageSessionAttribute(AMAdminConstants.CURRENT_REALM, tmp);

        // reset the tab selected to the realm view
        tmp = (String)getPageSessionAttribute(AMAdminConstants.PREVIOUS_TAB_ID);
        setPageSessionAttribute(getTrackingTabIDName(), tmp);

        // and now forward on to the realm page...
        RMRealmViewBean vb = (RMRealmViewBean)getViewBean(
            RMRealmViewBean.class);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }
    
    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/oneBtnPageTitle.xml"));
        ptModel.setValue("button1", 
            getBackButtonLabel("page.title.back.realms"));
    }

    private CCNavNode addAgentsTab(int idx) {
        AgentsModel model = (AgentsModel)getModel();
        AMViewConfig config = AMViewConfig.getInstance();
        return config.addAgentTabs(tabModel, model, idx);
    }

    protected AMModel getModelInternal() {
        RequestContext rc = RequestManager.getRequestContext();
        HttpServletRequest req = rc.getRequest();
        return new AgentsModelImpl(req, getPageSessionAttributes());
    }

    private void createTableModel() {
        String agentType = getDisplayIDType();
        AMViewConfig viewCfg = AMViewConfig.getInstance();
        combinedType = viewCfg.isCombineAgentType(agentType); 
        centralizable = 
            agentType.equals(AgentConfiguration.AGENT_TYPE_J2EE) ||
            agentType.equals(AgentConfiguration.AGENT_TYPE_WEB);
        
        String xml = null;
        if (centralizable) {
            xml = "com/sun/identity/console/tblAgentsJ2EEWebAgent.xml";
        } else if (combinedType) {
            xml = "com/sun/identity/console/tblAgentsCombined.xml";
        } else {
            xml = "com/sun/identity/console/tblAgents.xml";
        }
        
        tblModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(xml));
        tblModel.setTitleLabel("label.items");
        tblModel.setActionValue(TBL_BUTTON_ADD, "table.agents.button.new");
        tblModel.setActionValue(TBL_BUTTON_DELETE,
            "table.agents.button.delete");
        tblModel.setActionValue(TBL_COL_NAME, "table.agents.name.column.name");
        tblModel.setActionValue(TBL_COL_LOCAL, "table.agents.name.column.repo");
        
        if (combinedType) {
            tblModel.setActionValue(TBL_COL_TYPE, 
                "table.agents.name.column.type");
        }
     
        xml = (!combinedType) ?
            "com/sun/identity/console/tblAgentGroups.xml" :
            "com/sun/identity/console/tblAgentGroupsCombined.xml";
        tblGroupModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(xml));

        tblGroupModel.setTitleLabel("label.items");
        tblGroupModel.setActionValue(TBL_BUTTON_ADD_GROUP, 
            "table.agent.groups.button.new");
        tblGroupModel.setActionValue(TBL_BUTTON_DELETE_GROUP,
            "table.agent.groups.button.delete");
        tblGroupModel.setActionValue(TBL_COL_NAME_GROUP,
            "table.agent.groups.name.column.name");
        
        if (combinedType) {
            tblGroupModel.setActionValue(TBL_COL_TYPE_GROUP,
                "table.agent.groups.name.column.type");
        }
    }

    private void getAgentNames() {
        AgentsModel model = (AgentsModel)getModel();
        String filter = ((String)getDisplayFieldValue(TF_FILTER));
        String gfilter = ((String)getDisplayFieldValue(TF_GROUP_FILTER));

        if ((filter == null) || (filter.length() == 0)) {
            filter = "*";
            setDisplayFieldValue(TF_FILTER, "*");
        } else {
            filter = filter.trim();
        }

        if ((gfilter == null) || (gfilter.length() == 0)) {
            gfilter = "*";
            setDisplayFieldValue(TF_GROUP_FILTER, "*");
        } else {
            gfilter = gfilter.trim();
        }
        
        // this is to faciliate combined types.
        String agentType = getDisplayIDType();
        AMViewConfig cfg = AMViewConfig.getInstance();
        Set agentTypes = cfg.getCombineAgentTypes(agentType);
        if (agentTypes == null) {
            agentTypes = new HashSet(2);
            agentTypes.add(agentType);
        } else {
            supportedTypes.addAll(agentTypes);
        }
        
        try {
            Set agents = new HashSet();
            String curRealm = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            int errorCode = model.getAgentNames(curRealm, agentTypes, 
                filter, agents);
            switch (errorCode) {
            case IdSearchResults.SIZE_LIMIT_EXCEEDED:
                setInlineAlertMessage(CCAlert.TYPE_WARNING, "message.warning",
                    "message.sizelimit.exceeded");
                break;
            case IdSearchResults.TIME_LIMIT_EXCEEDED:
                setInlineAlertMessage(CCAlert.TYPE_WARNING, "message.warning",
                    "message.timelimit.exceeded");
                break;
            }
            
            Set agentGroups = new HashSet();
            errorCode = model.getAgentGroupNames(curRealm, agentTypes, gfilter,
                agentGroups);
            switch (errorCode) {
            case IdSearchResults.SIZE_LIMIT_EXCEEDED:
                setInlineAlertMessage(CCAlert.TYPE_WARNING, "message.warning",
                    "message.sizelimit.exceeded");
                break;
            case IdSearchResults.TIME_LIMIT_EXCEEDED:
                setInlineAlertMessage(CCAlert.TYPE_WARNING, "message.warning",
                    "message.timelimit.exceeded");
                break;
            }
            
            Map results = new HashMap(4);
            results.put(CACHE_AGENTS, agents);
            results.put(CACHE_AGENT_GROUPS, agentGroups);
            populateTableModel(results);
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
            CCButton b = (CCButton)getChild(TBL_BUTTON_ADD);
            b.setDisabled(true);
            b = (CCButton)getChild(TBL_BUTTON_ADD_GROUP);
            b.setDisabled(true);
        }
    }

    private String getDisplayIDType() {
        String idType = (String)getPageSessionAttribute(PG_SESSION_AGENT_TYPE);

        if ((idType == null) || (idType.length() == 0)) {
            setPageSessionAttribute(PG_SESSION_AGENT_TYPE, DEFAULT_ID_TYPE);
            idType = DEFAULT_ID_TYPE;
        }

        return idType;
    }
    
    private void populateTableModelEx() {
        if (!tblModelPopulated) {
            tblModelPopulated = true;
            SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
            Map cache = (Map)szCache.getSerializedObj();
            
            if ((cache != null) && !cache.isEmpty()) {
                AgentsModel model = (AgentsModel)getModel();
                SSOToken ssoToken = model.getUserSSOToken();
                List cacheAgents = (List)cache.get(CACHE_AGENTS);
                List cacheAgentGroups = (List)cache.get(CACHE_AGENT_GROUPS);
                Map mapCache = new HashMap(4);
                
                if ((cacheAgents != null) && !cacheAgents.isEmpty()) {
                    List list = new ArrayList(cacheAgents.size());
                    
                    for (Iterator i = cacheAgents.iterator(); i.hasNext(); ) {
                        String id = (String)i.next();
                        try {
                            list.add(IdUtils.getIdentity(ssoToken, id));
                        } catch (IdRepoException e) {
                            //ignore since ID is not found.
                        }
                    }
                    mapCache.put(CACHE_AGENTS, list);
                }
                
                if ((cacheAgentGroups != null) && !cacheAgentGroups.isEmpty()) {
                    List list = new ArrayList(cacheAgentGroups.size());
                    
                    for (Iterator i = cacheAgentGroups.iterator(); i.hasNext();
                    ) {
                        String id = (String)i.next();
                        try {
                            list.add(IdUtils.getIdentity(ssoToken, id));
                        } catch (IdRepoException e) {
                            //ignore since ID is not found.
                        }
                    }
                    mapCache.put(CACHE_AGENT_GROUPS, list);
                }
                
                populateTableModel(mapCache);
            }
        }
    }
    
    private void populateTableModel(Map map) {
        tblModel.clearAll();
        tblGroupModel.clearAll();
        SerializedField szCache = (SerializedField)getChild(SZ_CACHE);

        if ((map != null) && !map.isEmpty()) {
            // set the paging size
            AgentsModel model = (AgentsModel)getModel();
            tblModel.setMaxRows(model.getPageSize());
            tblGroupModel.setMaxRows(model.getPageSize());

            Map mapCache = new HashMap(4);
            Collection agents = (Collection)map.get(CACHE_AGENTS);
            
            if ((agents != null) && !agents.isEmpty()) {
                int counter = 0;
                boolean firstEntry = true;
                List cacheAgents = new ArrayList(agents.size());
            
                for (Iterator iter = agents.iterator(); iter.hasNext(); ) {
                    AMIdentity entity = (AMIdentity)iter.next();
                    if (firstEntry) {
                        firstEntry = false;
                    } else {
                        tblModel.appendRow();
                    }
                    String name = AMFormatUtils.getIdentityDisplayName(
                        model, entity);
                    tblModel.setSelectionVisible(counter++, true);
                    String universalId = IdUtils.getUniversalId(entity);
                    tblModel.setValue(TBL_DATA_NAME, name);
                    tblModel.setValue(TBL_DATA_UNIVERSALNAME, universalId);
                    tblModel.setValue(TBL_DATA_ACTION_HREF, 
                        stringToHex(universalId));
                    if (centralizable) {
                        if (model.isCentralized(universalId)) {
                            tblModel.setValue(TBL_DATA_LOCAL, 
                                "table.agents.data.repo.centralized");
                        } else {
                            tblModel.setValue(TBL_DATA_LOCAL, 
                                "table.agents.data.repo.localized");
                        }
                    } else if (combinedType) {
                        try {
                            tblModel.setValue(TBL_DATA_TYPE, 
                                model.getLocalizedString("agenttype." +
                                model.getAgentType(entity)));
                        } catch (AMConsoleException e) {
                            setInlineAlertMessage(CCAlert.TYPE_ERROR, 
                                "message.error", model.getErrorString(e));
                        }
                    }
                    cacheAgents.add(universalId);
                }
                mapCache.put(CACHE_AGENTS, cacheAgents);
            }

            Collection agentgroups = (Collection)map.get(CACHE_AGENT_GROUPS);
            
            if ((agentgroups != null) && !agentgroups.isEmpty()) {
                int counter = 0;
                boolean firstEntry = true;
                List cacheAgentGroups = new ArrayList(agentgroups.size());
            
                for (Iterator iter = agentgroups.iterator(); iter.hasNext(); ) {
                    AMIdentity entity = (AMIdentity)iter.next();
                    if (firstEntry) {
                        firstEntry = false;
                    } else {
                        tblGroupModel.appendRow();
                    }
                    String name = AMFormatUtils.getIdentityDisplayName(
                        model, entity);
                    tblGroupModel.setSelectionVisible(counter++, true);
                    String universalId = IdUtils.getUniversalId(entity);
                    tblGroupModel.setValue(TBL_DATA_NAME_GROUP, name);
                    tblGroupModel.setValue(TBL_DATA_UNIVERSALNAME_GROUP, 
                        universalId);
                    tblGroupModel.setValue(TBL_DATA_ACTION_HREF_GROUP,
                        stringToHex(universalId));
                    if (combinedType) {
                        try {
                            tblGroupModel.setValue(TBL_DATA_TYPE_GROUP, 
                                model.getLocalizedString("agenttype." +
                                model.getAgentType(entity)));
                        } catch (AMConsoleException e) {
                            setInlineAlertMessage(CCAlert.TYPE_ERROR,
                                "message.error", model.getErrorString(e));
                        }
                    }
                    cacheAgentGroups.add(universalId);
                }
                mapCache.put(CACHE_AGENT_GROUPS, cacheAgentGroups);
            }
            
            szCache.setValue((Serializable)mapCache);
        } else {                         
            szCache.setValue(null);
        }
    }
    /**
     * Handles search request.
     *
     * @param event Request Invocation Event.
     */
    public void handleBtnGroupSearchRequest(RequestInvocationEvent event) {
        forwardTo();
    }
    
    /**
     * Handles search request.
     *
     * @param event Request Invocation Event.
     */
    public void handleBtnSearchRequest(RequestInvocationEvent event) {
        forwardTo();
    }

    /**
     * Forwards request to group creation view bean.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblButtonGroupAddRequest(RequestInvocationEvent event) {
        AgentGroupAddViewBean vb = (AgentGroupAddViewBean) getViewBean(
            AgentGroupAddViewBean.class);
        String hiddenType = getRequestContext().getRequest().getParameter(
            "agenttype");

        if ((hiddenType != null) && (hiddenType.trim().length() > 0)) {
            setPageSessionAttribute(PG_SESSION_SUPERCEDE_AGENT_TYPE,
                hiddenType);
        } else {
            setPageSessionAttribute(PG_SESSION_SUPERCEDE_AGENT_TYPE,
                getDisplayIDType());
        }
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }
    
    /**
     * Forwards request to creation view bean.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblButtonAddRequest(RequestInvocationEvent event) {
        AgentAddViewBean vb = (AgentAddViewBean) getViewBean(
            AgentAddViewBean.class);
        String hiddenType = getRequestContext().getRequest().getParameter(
            "agenttype");

        if ((hiddenType != null) && (hiddenType.trim().length() > 0)) {
            setPageSessionAttribute(PG_SESSION_SUPERCEDE_AGENT_TYPE,
                hiddenType);
        } else {
            setPageSessionAttribute(PG_SESSION_SUPERCEDE_AGENT_TYPE,
                getDisplayIDType());
        }
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Forwards request to edit agent view bean.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblDataActionHrefRequest(RequestInvocationEvent event) {
        AgentsModel model = (AgentsModel)getModel();
        String agentType = getDisplayIDType();
        String universalId = hexToString((String)getDisplayFieldValue(
            TBL_DATA_ACTION_HREF));
        setPageSessionAttribute(AgentProfileViewBean.UNIVERSAL_ID, 
            universalId);
        SSOToken ssoToken = model.getUserSSOToken();
        String realm = (String) getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        StringTokenizer st = new StringTokenizer(universalId, "=,");
        st.nextToken();
        String agentName = st.nextToken();
        try {
            AMIdentity amid = new AMIdentity(ssoToken, agentName,
                IdType.AGENTONLY, realm, null);

            if (agentType.equals(AgentsViewBean.AGENT_WEB) || 
                    (agentType.equals(AgentsViewBean.DEFAULT_ID_TYPE))) 
            { 
                if (isPropertiesLocallyStored(amid)) {
                    setPageSessionAttribute(LOCAL_OR_NOT, PROP_LOCAL);
                } else {
                    setPageSessionAttribute(LOCAL_OR_NOT, PROP_CENTRAL);
                }
            }
            Class clazz = getAgentCustomizedViewBean(model.getAgentType(amid));
            AMViewBeanBase vb = (AMViewBeanBase)getViewBean(clazz);
            setPageSessionAttribute(PG_SESSION_SUPERCEDE_AGENT_TYPE, 
                model.getAgentType(amid));
            removePageSessionAttribute(GenericAgentProfileViewBean.PS_TABNAME);
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        }  catch (IdRepoException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                model.getErrorString(e));
            forwardTo();
        } catch (SSOException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                model.getErrorString(e));
            forwardTo();
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                model.getErrorString(e));
            forwardTo();
        } catch (ClassNotFoundException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                model.getErrorString(e));
            forwardTo();
        }
    }

    /**
     * Forwards request to edit agent group view bean.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblDataActionGroupHrefRequest(
        RequestInvocationEvent event
    ) {
        AgentsModel model = (AgentsModel)getModel();
        String idType = getDisplayIDType();
        String universalId = hexToString((String)getDisplayFieldValue(
            TBL_DATA_ACTION_HREF_GROUP));
        setPageSessionAttribute(AgentProfileViewBean.UNIVERSAL_ID, 
            universalId);

        SSOToken ssoToken = model.getUserSSOToken();
        String realm = (String) getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        StringTokenizer st = new StringTokenizer(universalId, "=,");
        st.nextToken();
        String agentGrpName = st.nextToken();
        try {
            AMIdentity amid = new AMIdentity(ssoToken, agentGrpName,
                IdType.AGENTGROUP, realm, null);
            String vbName = (String)agentViewBeans.get(idType);
            if (vbName == null) {
                vbName = GENERIC_VIEW_BEAN;
            }
            Class clazz = 
                Thread.currentThread().getContextClassLoader().loadClass(
                vbName);
            AMViewBeanBase vb = (AMViewBeanBase)getViewBean(clazz);
            removePageSessionAttribute(GenericAgentProfileViewBean.PS_TABNAME);
            setPageSessionAttribute(PG_SESSION_SUPERCEDE_AGENT_TYPE, 
                model.getAgentType(amid));
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                model.getErrorString(e));
            forwardTo();
        } catch (ClassNotFoundException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                model.getErrorString(e));
            forwardTo();
        }
    }
    
    /**
     * Deletes agents.
     *
     * @param event Request Invocation Event.
     * @throws ModelControlException if table model cannot be restored.
     */
    public void handleTblButtonDeleteRequest(RequestInvocationEvent event)
        throws ModelControlException
    {
        CCActionTable table = (CCActionTable)getChild(TBL_SEARCH);
        table.restoreStateData();

        Integer[] selected = tblModel.getSelectedRows();
        Set names = new HashSet(selected.length *2);
        SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
        Map mapCache = (Map)szCache.getSerializedObj();
        List cache = (List)mapCache.get(CACHE_AGENTS);

        for (int i = 0; i < selected.length; i++) {
            names.add((String)cache.get(selected[i].intValue()));
        }

        try {
            AgentsModel model = (AgentsModel)getModel();
            String curRealm = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            model.deleteAgents(curRealm, names);

            if (selected.length == 1) {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    model.getLocalizedString("agents.message.deleted"));
            } else {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    model.getLocalizedString("agents.message.deleted.pural"));
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }

        forwardTo();
    }
    
    /**
     * Deletes agent groups.
     *
     * @param event Request Invocation Event.
     * @throws ModelControlException if table model cannot be restored.
     */
    public void handleTblButtonGroupDeleteRequest(RequestInvocationEvent event)
        throws ModelControlException
    {
        CCActionTable table = (CCActionTable)getChild(TBL_SEARCH_GROUP);
        table.restoreStateData();

        Integer[] selected = tblGroupModel.getSelectedRows();
        Set names = new HashSet(selected.length *2);
        SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
        Map mapCache = (Map)szCache.getSerializedObj();
        List cache = (List)mapCache.get(CACHE_AGENT_GROUPS);

        for (int i = 0; i < selected.length; i++) {
            names.add((String)cache.get(selected[i].intValue()));
        }

        try {
            AgentsModel model = (AgentsModel)getModel();
            String curRealm = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            model.deleteAgentGroups(curRealm, names);

            if (selected.length == 1) {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    model.getLocalizedString("agent.groups.message.deleted"));
            } else {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    model.getLocalizedString(
                        "agent.groups.message.deleted.pural"));
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }

        forwardTo();
    }

    static Class getAgentCustomizedViewBean(String agentType) 
        throws ClassNotFoundException {
        String vbName = (String)agentViewBeans.get(agentType);
        if (vbName == null) {
            vbName = GENERIC_VIEW_BEAN;
        }
        return Thread.currentThread().getContextClassLoader().loadClass(vbName);
    }

    /**
     * Returns <code>true</code> if agent type is not 2.2 agent.
     * @param event Child display event.
     * @return <code>true</code> if agent type is not 2.2 agent.
     */
    public boolean beginTblSearchGroupDisplay(ChildDisplayEvent event) {
        return !is2dot2Agent() && !isAgentAuthenticator();
    }

    /**
     * Returns <code>true</code> if agent type is not 2.2 agent.
     * @param event Child display event.
     * @return <code>true</code> if agent type is not 2.2 agent.
     */
    public boolean beginBtnGroupSearchDisplay(ChildDisplayEvent event) {
        return !is2dot2Agent() && !isAgentAuthenticator();
    }
    

    /**
     * Returns <code>true</code> if agent type is not 2.2 agent.
     * @param event Child display event.
     * @return <code>true</code> if agent type is not 2.2 agent.
     */
    public boolean beginTfGroupFilterDisplay(ChildDisplayEvent event) {
        return !is2dot2Agent() && !isAgentAuthenticator();
    }
    
    private boolean is2dot2Agent() {
        String agentType = getDisplayIDType();
        return (agentType != null) && 
            agentType.equals(AgentConfiguration.AGENT_TYPE_2_DOT_2_AGENT);
    }
    
    private boolean isAgentAuthenticator() {
        String agentType = getDisplayIDType();
        return (agentType != null) && 
            agentType.equals(AgentConfiguration.AGENT_TYPE_AGENT_AUTHENTICATOR);    
    }
    
    private static boolean isPropertiesLocallyStored(AMIdentity amid)
        throws IdRepoException, SSOException {
        boolean isLocal = false;
        Set setRepo = (Set)amid.getAttribute(ATTR_CONFIG_REPO);
        if ((setRepo != null) && !setRepo.isEmpty()) {
            String repo = (String) setRepo.iterator().next();
            isLocal = (repo.equalsIgnoreCase(PROP_LOCAL));
        }
        return isLocal;
    }


    protected String getBreadCrumbDisplayName() {
        String realm = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        AMModel model = (AMModel)getModel();

        String path = null;
        if ((realm != null) && (realm.trim().length() > 0)) {
            int idx = realm.lastIndexOf('/');
            if ((idx != -1) && (idx < (realm.length() -1))) {
                path = realm.substring(idx+1);
            }
        }
        if (path == null) {
            model.getStartDSDN();
            path = AMFormatUtils.DNToName(model, model.getStartDSDN());
        }

        // unescape the value to display '/' character
        String[] arg = {SMSSchema.unescapeName(path)};
        return MessageFormat.format(
        model.getLocalizedString("breadcrumbs.editRealm"), (Object[])arg);
    }

    protected boolean startPageTrail() {
        return false;
    }

    private CCBreadCrumbs createParentagePath(String name) {
        CCBreadCrumbsModel model = null;
        AMModel ammodel = (AMModel)getModel();
        String curRealm = getCurrentRealm();
        if (curRealm.charAt(0) != '/') {
            curRealm = "/" + curRealm;
        }

        String startDN = ammodel.getStartDN();
        if (startDN.charAt(0) != '/') {
            startDN = "/" + startDN;
        }

        String startDNString =
            AMFormatUtils.DNToName(ammodel, ammodel.getStartDSDN());

        if (curRealm.equals(startDN)) {
            model = new CCBreadCrumbsModel();
            setDisplayFieldValue(TXT_ROOT, startDNString);
        } else {
            int idx = curRealm.indexOf(startDN);
            String subRealm = (idx == 0) ?
                curRealm.substring(startDN.length()) : curRealm;
            List list = reverseParentagePath(subRealm);
            if (!list.isEmpty()) {
                list.remove(list.size() -1);
            }
            /*
             * The model is initialized with the name of the current realm.
             * This entry is not selectable, just displayed as a label.
             */
            idx = subRealm.lastIndexOf("/");
            if (idx != -1) {
              subRealm = subRealm.substring(idx+1);
            }
            model = new CCBreadCrumbsModel(SMSSchema.unescapeName(subRealm));

            StringBuilder baseDN = new StringBuilder(200);
            baseDN.append(startDN);
            /*
             * each row added to the model is a selectable entry in the
             * parentage path
             */
            model.appendRow();
            model.setValue(CCBreadCrumbsModel.LABEL,
                SMSSchema.unescapeName(startDNString));
            model.setValue(CCBreadCrumbsModel.COMMANDFIELD,
                PARENTAGE_PATH_HREF);
            model.setValue(CCBreadCrumbsModel.HREF_VALUE, baseDN.toString());

            for (Iterator iter = list.iterator(); iter.hasNext(); ) {
                String tok = (String)iter.next();
                if (!baseDN.toString().equals("/")) {
                    baseDN.append("/").append(tok);
                } else {
                    baseDN.append(tok);
                }
                model.appendRow();
                model.setValue(CCBreadCrumbsModel.LABEL,
                    SMSSchema.unescapeName(tok));
                model.setValue(CCBreadCrumbsModel.COMMANDFIELD,
                    PARENTAGE_PATH_HREF);
                model.setValue(CCBreadCrumbsModel.HREF_VALUE,
                    baseDN.toString());
            }
        }

        return new CCBreadCrumbs(this, model, name);
    }

    private List reverseParentagePath(String path) {
        List list = new ArrayList();
        StringTokenizer st = new StringTokenizer(path, "/");
        while (st.hasMoreTokens()) {
            list.add(st.nextToken());
        }
        return list;
    }
    
    /**
     * Handles parentage path request.
     *
     * @param event Request Invocation Event.
     * @throws ModelControlException if table model cannot be restored.
     */
    public void handleParentagepathHrefRequest(RequestInvocationEvent event) {
        String path = (String)getDisplayFieldValue(PARENTAGE_PATH_HREF);
        setPageSessionAttribute(AMAdminConstants.CURRENT_REALM, path);
        setCurrentLocation(path);
        unlockPageTrailForSwapping();
        forwardTo();
    }

}
