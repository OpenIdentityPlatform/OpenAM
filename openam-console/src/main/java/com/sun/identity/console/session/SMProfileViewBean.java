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
 * $Id: SMProfileViewBean.java,v 1.3 2009/10/13 21:17:03 asyhuang Exp $
 */

/**
 * Portions copyright 2013 ForgeRock, Inc.
 */

package com.sun.identity.console.session;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.ChildDisplayEvent;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.base.AMConsoleConfig;
import com.sun.identity.console.base.AMViewConfig;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.components.view.html.SerializedField;
import com.sun.identity.console.realm.HasEntitiesTabs;
import com.sun.identity.console.session.model.SMProfileModel;
import com.sun.identity.console.session.model.SMProfileModelImpl;
import com.sun.identity.console.session.model.SMSessionCache;
import com.sun.identity.console.session.model.SMSessionData;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.model.CCNavNodeInterface;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCButton;
import com.sun.web.ui.view.html.CCDropDownMenu;
import com.sun.web.ui.view.html.CCHref;
import com.sun.web.ui.view.html.CCStaticTextField;
import com.sun.web.ui.view.html.CCTextField;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.view.table.CCActionTable;
import com.sun.web.ui.view.tabs.CCTabs;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SMProfileViewBean
    extends SMViewBeanBase
    implements HasEntitiesTabs
{

    public static final String DEFAULT_DISPLAY_URL =
        "/console/session/SMProfile.jsp";

    private static final String TF_FILTER = "tfFilter";
    private static final String BTN_SEARCH = "btnSearch";
    private static final String TBL_SESSIONS = "tblSessions";
    private static final String TBL_BUTTON_INVALIDATE = "tblButtonInvalidate";

    private static final String TBL_COL_USER_ID = "tblColUserId";
    private static final String TBL_COL_TIME_LEFT = "tblColTimeLeft";
    private static final String TBL_COL_MAX_SESSION_TIME = 
        "tblColMaxSessionTime";
    private static final String TBL_COL_IDLE_TIME = "tblColIdleTime";
    private static final String TBL_COL_MAX_IDLE_TIME = "tblColMaxIdleTime";

    private static final String TBL_DATA_USER_ID = "tblDataUserId";
    private static final String TBL_DATA_TIME_LEFT = "tblDataTimeLeft";
    private static final String TBL_DATA_MAX_SESSION_TIME = 
        "tblDataMaxSessionTime";
    private static final String TBL_DATA_IDLE_TIME = "tblDataIdleTime";
    private static final String TBL_DATA_MAX_IDLE_TIME = "tblDataMaxIdleTime";
    private static final String TBL_DATA_SESSION_ID = "sessionId";

    public static final String CHILD_SERVER_NAME_HREF = "serverNameHref";
    public static final String CHILD_SERVER_NAME_MENU = "serverNameMenu";
    private static final String LOGOUT_URL = "logoutUrl";
    private static final String SERVER_NAME = "SERVER_NAME";

    protected CCActionTableModel tblModel = null;
    private CCPageTitleModel ptModel;
    protected int curPage = 0;
    private boolean validSession = true;

    /**
     * Creates a default named session view bean.
     */
    public SMProfileViewBean() {
        super("SMProfile");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        createTableModel();
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/simplePageTitle.xml"));
        registerChildren();
    }

    /**
     * Creates a session view bean by name.
     */
    public SMProfileViewBean(String name) {
        super(name);
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        createTableModel();
        ptModel = new CCPageTitleModel(
                getClass().getClassLoader().getResourceAsStream(
                        "com/sun/identity/console/simplePageTitle.xml"));
        registerChildren();
    }

    /**
     * Registers user interface components used by this view bean.
     */
    protected void registerChildren() {
        super.registerChildren();
        registerChild(TF_FILTER, CCTextField.class);
        registerChild(BTN_SEARCH, CCButton.class);
        registerChild(PAGETITLE, CCPageTitle.class);
        registerChild(TBL_SESSIONS, CCActionTable.class);
        registerChild(CHILD_SERVER_NAME_HREF, CCHref.class);
        registerChild(CHILD_SERVER_NAME_MENU, CCDropDownMenu.class);
        registerChild(LOGOUT_URL, CCStaticTextField.class);
        ptModel.registerChildren(this);
        tblModel.registerChildren(this);
    }

    /**
     * Creates user interface components used by this view bean.
     *
     * @param name of component
     * @return child component
     */
    protected View createChild(String name) {
        View view = null;
        if (name.equals(TBL_SESSIONS)) {
            SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
            populateTableModel((List)szCache.getSerializedObj());
            view = new CCActionTable(this, tblModel, name);
        } else if (name.equals(PAGETITLE)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (tblModel.isChildSupported(name)) {
            view = tblModel.createChild(this, name);
        } else if (name.equals(CHILD_SERVER_NAME_MENU)) {
            view = new CCDropDownMenu(this, name, null);
        } else if (name.equals(LOGOUT_URL)) {
            return new CCStaticTextField(this, LOGOUT_URL, "");
        } else if (name.equals(CHILD_SERVER_NAME_HREF)) {
            view = new CCHref(this, name, null);
        } else {
                 view = super.createChild(name);
        }

        return view;
    }

    /**
     * Sets the required information to display the page.
     *
     * @param event display event.
     * @throws ModelControlException if problem access value of component.
     */
    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        if (validSession) {
            super.beginDisplay(event);
            SMProfileModel model = (SMProfileModel)getModel();
            Map map = model.getServerNames();
            OptionList optList = new OptionList();
            CCDropDownMenu child = 
                (CCDropDownMenu)getChild(CHILD_SERVER_NAME_MENU);
            String value = (String)child.getValue();
            if (map != null &&  !map.isEmpty()) {
                for (Iterator iter=map.keySet().iterator(); iter.hasNext(); ) {
                    String str = (String)iter.next();
                    String val = (String)map.get(str);
                    optList.add(str, val);
                    if (value == null) {
                        child.setValue(val);
                    }
                }
            }
            child.setOptions(optList);
            value = (String)child.getValue();
            model.setProfileServerName(value);
            SMSessionCache cache = null;
            try {
                cache = model.getSessionCache(getFilterString());
                if (cache != null) {
                    populateTableModel(cache.getSessions());
                    String errorMessage = cache.getErrorMessage();
                    if (errorMessage != null && errorMessage.length() > 0) {
                        setInlineAlertMessage(CCAlert.TYPE_WARNING, 
                            "message.warning", errorMessage);
                    }
                }
            } catch (AMConsoleException ae) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, 
                    "message.error", ae.getMessage());
            }
            if (cache == null) {           
                populateTableModel(Collections.EMPTY_LIST);
            }
            setPageSessionAttribute(SERVER_NAME, value);
            // Set our Sub-Tabs
            addSessionsTab(model,1);
            // Disable, if SFO (not the Airport) HA is enabled and the Type is specified as well.
            // Both the SFO is Enabled and Repository Type has been Specified for view of HA Tabs.
            if ( (!SystemPropertiesManager.get(CoreTokenConstants.IS_SFO_ENABLED, "false").equalsIgnoreCase("true")) &&
                 (SystemPropertiesManager.get(CoreTokenConstants.SYS_PROPERTY_SESSION_HA_REPOSITORY_TYPE, "None").equalsIgnoreCase("None")) )
            {
                removeSessionsTab();
            }
        }
    }

    // Assign the Session SubTabs
    protected void addSessionsTab(SMProfileModel model, int selectedNode) {
        AMViewConfig config = AMViewConfig.getInstance();
        config.addSessionTabs(tabModel, model);
        registerChild(TAB_COMMON, CCTabs.class);
        tabModel.setSelectedNode(selectedNode);
    }

    // Remove all Session Tabs, since HA not available, disable associated Tabs.
    protected void removeSessionsTab() {
        if (tabModel != null) {
            tabModel.clear();
            // removeSessionsTab(551); Current Sessions, Leave!
            removeSessionsTab(552);
        }
    }

    private void removeSessionsTab(int tabNodeId) {
            CCNavNodeInterface tabNode = tabModel.getNodeById(tabNodeId);
            if (tabNode != null)
            {
                tabNode.setVisible(false);
                tabNode.setAcceptsChildren(false);
                tabNode.getParent().removeChild(tabNode);
            }
    }

    /**
     * Returns model for this view bean.
     *
     * @return model for view bean.
     */
    protected AMModel getModelInternal() {
        HttpServletRequest req = getRequestContext().getRequest();
        return new SMProfileModelImpl(req, getPageSessionAttributes());
    }

    private void createTableModel() {
        tblModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/tblSMSessions.xml"));
        tblModel.setTitleLabel("label.items");
        tblModel.setActionValue(TBL_BUTTON_INVALIDATE, "button.invalidate");
        tblModel.setActionValue(TBL_COL_USER_ID, 
            "table.session.userid.column.name");
        tblModel.setActionValue(TBL_COL_TIME_LEFT, 
            "table.session.timeleft.column.name");
        tblModel.setActionValue(TBL_COL_MAX_SESSION_TIME, 
            "table.session.maxsessiontime.column.name");
        tblModel.setActionValue(TBL_COL_IDLE_TIME, 
            "table.session.idletime.column.name");
        tblModel.setActionValue(TBL_COL_MAX_IDLE_TIME, 
            "table.session.maxidletime.column.name");
    }

    /**
     * Returns filter (wildcards) string.
     *
     * @return filter string.
     */
    protected String getFilterString() {
        String filter = (String) getDisplayFieldValue(TF_FILTER);

        if (filter == null) {
            filter = "*";
        } else {
            filter = filter.trim();
            if (filter.length() == 0) {
                filter = "*";
            }
        }
        setDisplayFieldValue(TF_FILTER, filter);
        return filter;
    }


    private void populateTableModel(List sessionList) {
        SMProfileModel model = (SMProfileModel)getModel();
        tblModel.clearAll();
        tblModel.setMaxRows(model.getPageSize());
        SerializedField szCache = (SerializedField)getChild(SZ_CACHE);

        if (sessionList != null && !sessionList.isEmpty()) {
            for (int i = 0; i < sessionList.size(); i++) {
                SMSessionData sData =
                    (SMSessionData)sessionList.get(i);
                tblModel.appendRow();
                tblModel.setValue(TBL_DATA_SESSION_ID, 
                    sData.getId());
                tblModel.setValue(TBL_DATA_USER_ID, 
                    sData.getUserId());
                tblModel.setValue(TBL_DATA_TIME_LEFT, 
                    String.valueOf(sData.getTimeRemain()));
                tblModel.setValue(TBL_DATA_MAX_SESSION_TIME, 
                    String.valueOf(sData.getMaxSessionTime()));
                tblModel.setValue(TBL_DATA_IDLE_TIME, 
                    String.valueOf(sData.getIdleTime()));
                tblModel.setValue(TBL_DATA_MAX_IDLE_TIME, 
                    String.valueOf(sData.getMaxIdleTime()));
            }
            szCache.setValue((Serializable)sessionList);
        } else {
            szCache.setValue(null);
        }
    }

    /**
     * Handles search request.
     *
     * @param event Request Invocation Event.
     */
    public void handleBtnSearchRequest(RequestInvocationEvent event) {
        SMProfileModel model = (SMProfileModel)getModel();
        String serverName = (String)getPageSessionAttribute(SERVER_NAME);
        model.setProfileServerName(serverName);
        forwardTo();
    }

    /**
     * Handles the event request for <i>Invalidate</i> button.
     *
     * @param event request invocation event.
     * @throws ModelControlException if table model cannot be restored.
     */
    public void handleTblButtonInvalidateRequest(RequestInvocationEvent event)
        throws ModelControlException
    {
        SMProfileModel model = (SMProfileModel)getModel();
        String serverName = (String)getPageSessionAttribute(SERVER_NAME);
        model.setProfileServerName(serverName);

        CCActionTable child = (CCActionTable)getChild(TBL_SESSIONS);
        child.restoreStateData();

        Integer[] selected = tblModel.getSelectedRows();
        List names = new ArrayList(selected.length *2);

        for (int i = 0; i < selected.length; i++) {
            tblModel.setRowIndex(selected[i].intValue());
            names.add((String)tblModel.getValue(TBL_DATA_SESSION_ID));
        }
        boolean error = false;
        List failList = null;
        try {
            failList = model.invalidateSessions(names, getFilterString());
        } catch (AMConsoleException e) {
            error = true;
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
        if (!error && failList != null && !failList.isEmpty()) {
            // TOFIX:  need to display mutiple message.
        }

        if (!model.isSessionValid()) {
            validSession = false;
        }
        forwardTo();
    }

    /**
     * Handles the event request for dropdown menu.
     *
     * @param event request invocation event.
     */
    public void handleServerNameHrefRequest(RequestInvocationEvent event) {
        forwardTo();
    }

    /**
     * Returns true if the current session is invalid.
     *
     * @param event  child display event.
     * @return true if the current session is invalid.
     */
    public boolean beginToLogoutDisplay(ChildDisplayEvent event) {
        boolean display = false;
        if (!validSession) {
            HttpServletRequest req = getRequestContext().getRequest();
            AMConsoleConfig config = AMConsoleConfig.getInstance();
            setDisplayFieldValue(LOGOUT_URL, config.getLogoutURL(req));
            display = true;
        }
        return display;
    }


}
