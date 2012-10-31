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
 * $Id: MAPClientManagerViewBean.java,v 1.2 2008/06/25 05:43:14 qcheng Exp $
 *
 */

package com.sun.identity.console.service;

import com.iplanet.jato.NavigationException;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.ChildContentDisplayEvent;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.CloseWindowViewBean;
import com.sun.identity.console.base.MessageViewBean;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.service.model.MAPServiceModel;
import com.sun.identity.console.service.model.MAPServiceModelImpl;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.model.CCNavNode;
import com.sun.web.ui.model.CCTabsModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCButton;
import com.sun.web.ui.view.html.CCDropDownMenu;
import com.sun.web.ui.view.html.CCTextField;
import com.sun.web.ui.view.masthead.CCSecondaryMasthead;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.view.table.CCActionTable;
import com.sun.web.ui.view.tabs.CCNodeEventHandlerInterface;
import com.sun.web.ui.view.tabs.CCTabs;
import java.util.Iterator;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

public class MAPClientManagerViewBean
    extends AMViewBeanBase
    implements CCNodeEventHandlerInterface
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/service/MAPClientManager.jsp";
    static final String PAGE_SESSION_PROFILE_NAME = "pgSessionProfileName";
    static final String PAGE_SESSION_STYLE_NAME = "pgSessionStyleName";

    private static final String SEC_MH_COMMON = "secMhCommon";
    private static final String TAB_CLIENT_DETECTION = "tabClientDetection";
    private static final String PGTITLE = "pgtitle";
    private static final String TF_FILTER = "tfFilter";
    private static final String BTN_SEARCH = "btnSearch";
    private static final String TBL_CLIENTS = "tblClients";

    private static final String TBL_BUTTON_ADD = "tblButtonAdd";
    private static final String TBL_COL_NAME = "tblColName";
    private static final String TBL_DATA_NAME = "tblDataName";
    private static final String TBL_CUSTOMIZABLE = "tblCustomizable";
    private static final String TBL_COL_ACTION = "tblColAction";
    private static final String TBL_DATA_ACTION_HREF = "tblDataActionHref";
    private static final String TBL_DATA_ACTION_LABEL = "tblDataActionLabel";
    private static final String TBL_DATA_ACTION_DUPLICATE_HREF =
        "tblDataActionDuplicateHref";
    private static final String TBL_DATA_ACTION_DUPLICATE_LABEL =
        "tblDataActionDuplicateLabel";
    private static final String TBL_DATA_ACTION_DELETE_HREF =
        "tblDataActionDeleteHref";
    private static final String TBL_DATA_ACTION_DELETE_LABEL =
        "tblDataActionDeleteLabel";
    private static final String TBL_DATA_ACTION_DEFAULT_HREF =
        "tblDataActionDefaultHref";
    private static final String TBL_DATA_ACTION_DEFAULT_LABEL =
        "tblDataActionDefaultLabel";
    private static final String SINGLECHOICE_STYLE = "singleChoiceStyle";

    private CCActionTableModel tblModel = null;
    private CCPageTitleModel ptModel;

    public MAPClientManagerViewBean() {
        super("MAPClientManager");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        createPageTitleModel();
        createTableModel();
        registerChildren();
    }

    /**
     * Registers user interface components used by this view bean.
     */
    protected void registerChildren() {
        super.registerChildren();
        registerChild(TAB_CLIENT_DETECTION, CCTabs.class);
        registerChild(PGTITLE, CCPageTitle.class);
        registerChild(SEC_MH_COMMON, CCSecondaryMasthead.class);
        registerChild(TF_FILTER, CCTextField.class);
        registerChild(BTN_SEARCH, CCButton.class);
        registerChild(TBL_CLIENTS, CCActionTable.class);
        registerChild(SINGLECHOICE_STYLE, CCDropDownMenu.class);
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

        if (name.equals(TBL_CLIENTS)) {
            view = new CCActionTable(this, tblModel, name);
        } else if (name.equals(TAB_CLIENT_DETECTION)) {
            view = createTab(name);
        } else if (name.equals(SEC_MH_COMMON)) {
            view = new CCSecondaryMasthead(this, name);
        } else if (name.equals(PGTITLE)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else if (tblModel.isChildSupported(name)) {
            view = tblModel.createChild(this, name);
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/oneBtnPageTitle.xml"));
        ptModel.setValue("button1", "button.close");
        ptModel.setPageTitleText("map.client.manager.window.title");
    }

    private View createTab(String name) {
        CCTabsModel tabModel = new CCTabsModel();
        MAPServiceModel model = (MAPServiceModel)getModel();
        Set profileNames = model.getProfileNames();

        if ((profileNames != null) && !profileNames.isEmpty()) {
            for (Iterator iter = profileNames.iterator(); iter.hasNext(); ) {
                String val = (String)iter.next();
                tabModel.addNode(new CCNavNode(val.hashCode(), val, val, val));
            }
            tabModel.setSelectedNode(getProfileName().hashCode());
        }

        return new CCTabs(this, tabModel, name);
    }

    private void createTableModel() {
        tblModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/tblMAPClientDetection.xml"));
        tblModel.setTitleLabel("label.items");
        tblModel.setMaxRows(getModel().getPageSize());
        tblModel.setActionValue(TBL_COL_NAME,
            "table.clientDetection.client.column.name");
        tblModel.setActionValue(TBL_COL_ACTION,
            "table.clientDetection.action.column.name");
        tblModel.setActionValue(TBL_BUTTON_ADD,
            "clientDetection.newDevice.button");
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event);
        setStyles();
        populateTableModel();
    }

    private void setStyles() {
        MAPServiceModel model = (MAPServiceModel)getModel();
        Set styles = model.getStyleNames(getProfileName());

        if ((styles != null) && !styles.isEmpty()) {
            OptionList styleList = new OptionList();

            for (Iterator iter = styles.iterator(); iter.hasNext(); ) {
                String name = (String)iter.next();
                styleList.add(name, name);
            }

            CCDropDownMenu menu = (CCDropDownMenu)getChild(SINGLECHOICE_STYLE);
            menu.setOptions(styleList);
        }
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
            return new MAPServiceModelImpl(req, getPageSessionAttributes());
    }

    /**
     * Handles tab selected event.
     *
     * @param event Request Invocation Event.
     * @param nodeID Selected Node ID.
     */
    public void nodeClicked(RequestInvocationEvent event, int nodeID) {
        String profileName = getProfileName(nodeID);
        setPageSessionAttribute(PAGE_SESSION_PROFILE_NAME, profileName);
        removePageSessionAttribute(PAGE_SESSION_STYLE_NAME);
        setDisplayFieldValue(SINGLECHOICE_STYLE, "");
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
     * Handles add device request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblButtonAddRequest(RequestInvocationEvent event) {
        MAPCreateDeviceViewBean vb = (MAPCreateDeviceViewBean)getViewBean(
            MAPCreateDeviceViewBean.class);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Handles edit device profile request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblDataActionHrefRequest(RequestInvocationEvent event) {
        String name = (String)getDisplayFieldValue(TBL_DATA_ACTION_HREF);
        MAPDeviceProfileViewBean vb = (MAPDeviceProfileViewBean)getViewBean(
            MAPDeviceProfileViewBean.class);
        passPgSessionMap(vb);
        vb.deviceName = name;
        vb.forwardTo(getRequestContext());
    }

    /**
     * Handles delete device profile request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblDataActionDefaultHrefRequest(
        RequestInvocationEvent event
    ) {
        String name = (String)getDisplayFieldValue(
            TBL_DATA_ACTION_DEFAULT_HREF);
        deleteDevice(name, "clientDetection.client.defaulted.message");
        forwardTo();
    }

    /**
     * Handles delete device profile request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblDataActionDeleteHrefRequest(
        RequestInvocationEvent event) {
        String name = (String)getDisplayFieldValue(
            TBL_DATA_ACTION_DELETE_HREF);
        deleteDevice(name, "clientDetection.client.deleted.message");
        forwardTo();
    }

    private void deleteDevice(String deviceName, String message) {
        MAPServiceModel model = (MAPServiceModel)getModel();

        try {
            model.removeClient(deviceName);
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                message);
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }

    /**
     * Handles duplicate device request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblDataActionDuplicateHrefRequest(
        RequestInvocationEvent event) {
        String name = (String)getDisplayFieldValue(
            TBL_DATA_ACTION_DUPLICATE_HREF);
        MAPServiceModel model = (MAPServiceModel)getModel();
        MAPDuplicationDeviceViewBean vb = (MAPDuplicationDeviceViewBean)
            getViewBean(MAPDuplicationDeviceViewBean.class);
        vb.clientType = model.getClientTypePrefix() + name;
        vb.deviceName = model.getDeviceNamePrefix() +
            model.getDeviceUserAgent(name);
        vb.setDisplayFieldValue(
            MAPDuplicationDeviceViewBean.TF_ORIG_CLIENT_TYPE, name);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    private String getProfileName(int nodeID) {
        MAPServiceModel model = (MAPServiceModel)getModel();
        Set profileNames = model.getProfileNames();
        String profileName = null;

        if ((profileNames != null) && !profileNames.isEmpty()) {
            for (Iterator iter = profileNames.iterator();
                iter.hasNext() && (profileName == null);
            ) {
                String val = (String)iter.next();
                if (val.hashCode() == nodeID) {
                    profileName = val;
                }
            }
        }

        return profileName;
    }

    private String getBaseStyle() {
        String baseId = (String)getDisplayFieldValue(SINGLECHOICE_STYLE);

        if ((baseId == null) || (baseId.trim().length() == 0)) {
            baseId = (String)getPageSessionAttribute(PAGE_SESSION_STYLE_NAME);
            if ((baseId == null) || (baseId.trim().length() == 0)) {
                baseId = getProfileName();
            }
            setDisplayFieldValue(SINGLECHOICE_STYLE, baseId);
        }

        setPageSessionAttribute(PAGE_SESSION_STYLE_NAME, baseId);
        return baseId;
    }

    private String getProfileName() {
        String name = (String)getPageSessionAttribute(
            PAGE_SESSION_PROFILE_NAME);

        if ((name == null) || (name.trim().length() == 0)) {
            name = MAPServiceModel.DEFAULT_PROFILE_NAME;
        }

        setPageSessionAttribute(PAGE_SESSION_PROFILE_NAME, name);

        return name;
    }

    private void populateTableModel() {
        tblModel.clearAll();
        boolean first = true;
        String filter = (String)getDisplayFieldValue(TF_FILTER);
        MAPServiceModel model = (MAPServiceModel)getModel();
        Set devices = model.getDeviceNames(
            getProfileName(), getBaseStyle(), filter);

        if ((devices != null) && !devices.isEmpty()) {
            for (Iterator iter = devices.iterator(); iter.hasNext(); ) {
                if (!first) {
                    tblModel.appendRow();
                } else {
                    first = false;
                }

                String name = (String)iter.next();
                tblModel.setValue(TBL_DATA_NAME, name);
                tblModel.setValue(TBL_DATA_ACTION_HREF, name);
                tblModel.setValue(TBL_DATA_ACTION_DEFAULT_HREF, name);
                tblModel.setValue(TBL_DATA_ACTION_DUPLICATE_HREF, name);
                tblModel.setValue(TBL_DATA_ACTION_DELETE_HREF, name);
                tblModel.setValue(TBL_DATA_ACTION_LABEL,
                    "clientDetection.edit.hyperlink.label");
                tblModel.setValue(TBL_DATA_ACTION_DUPLICATE_LABEL,
                    "clientDetection.duplicate.hyperlink.label");

                if (model.isCustomizable(name)) {
                    tblModel.setValue(TBL_CUSTOMIZABLE,
                        "clientDetection.customizable.label");
                } else {
                    tblModel.setValue(TBL_CUSTOMIZABLE, "");
                }

                if (model.hasDefaultSetting(name)) {
                    tblModel.setValue(TBL_DATA_ACTION_DEFAULT_LABEL,
                        "clientDetection.default.hyperlink.label");
                } else {
                    tblModel.setValue(TBL_DATA_ACTION_DEFAULT_LABEL, "");
                }

                if (model.canBeDeleted(name)) {
                    tblModel.setValue(TBL_DATA_ACTION_DELETE_LABEL,
                        "clientDetection.delete.hyperlink.label");
                } else {
                    tblModel.setValue(TBL_DATA_ACTION_DELETE_LABEL, "");
                }
            }
        }
    }

    /**
     * Returns empty string if current device does not have default setting.
     *
     * @param event Childe content display event.
     * @return empty string if current device does not have default setting.
     */
    public String endTblDataActionDefaultHrefDisplay(
        ChildContentDisplayEvent event
    ) {
        String lbl = (String)tblModel.getValue(TBL_DATA_ACTION_DEFAULT_LABEL);
        return ((lbl != null) && (lbl.length() > 0)) ? event.getContent() : "";
    }

    /**
     * Returns empty string if current device cannot be deleted.
     *
     * @param event Childe content display event.
     * @return empty string if current device cannot be deleted.
     */
    public String endTblDataActionDeleteHrefDisplay(
        ChildContentDisplayEvent event
    ) {
        String lbl = (String)tblModel.getValue(TBL_DATA_ACTION_DELETE_LABEL);
        return ((lbl != null) && (lbl.length() > 0)) ? event.getContent() : "";
    }

    /**
     * Handles close browser window request.
     *
     * @param event Request invocation event.
     */
    public void handleButton1Request(RequestInvocationEvent event) {
        CloseWindowViewBean vb = (CloseWindowViewBean)getViewBean(
            CloseWindowViewBean.class);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Forwards to Message View Bean if there are no profiles.
     *
     * @param reqContext Request Context.
     * @throws NavigationException if model object is not accessible.
     */
    public void forwardTo(RequestContext reqContext)
        throws NavigationException
    {
        MAPServiceModel model = (MAPServiceModel)getModel();
        Set profileNames = model.getProfileNames();

        if ((profileNames == null) || profileNames.isEmpty()) {
            MessageViewBean vb = (MessageViewBean)getViewBean(
                MessageViewBean.class);
            vb.setMessage(CCAlert.TYPE_INFO, "message.information",
                "map.no.profiles");
            vb.forwardTo(reqContext);
        } else {
            super.forwardTo(reqContext);
        }
    }
}
