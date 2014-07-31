/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package com.sun.identity.console.reststs;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.components.view.html.SerializedField;
import com.sun.identity.console.realm.RealmPropertiesBase;
import com.sun.identity.console.reststs.model.RestSTSModel;
import com.sun.identity.console.reststs.model.RestSTSModelImpl;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.view.table.CCActionTable;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class RestSTSHomeViewBean extends RealmPropertiesBase {
    public static final String DEFAULT_DISPLAY_URL =
            "/console/reststs/RestSTSHome.jsp";
    /*
    Used to set a session attribute
     */
    public static final String INSTANCE_NAME = "instanceName";
    private static final String TBL_SEARCH = "tblSearch";
    private static final String TBL_BUTTON_ADD = "tblButtonAdd";
    private static final String TBL_BUTTON_DELETE = "tblButtonDelete";
    private static final String TBL_COL_NAME = "tblColName";
    private static final String TBL_DATA_NAME = "tblDataName";
    private static final String TBL_COL_ACTION = "tblColAction";
    private static final String PAGETITLE = "pgtitle";
    static final String TBL_DATA_ACTION_HREF = "tblDataActionHref";

    private CCActionTableModel tblModel = null;

    public RestSTSHomeViewBean() {
        /*
        must be class name minus 'ViewBean', and the .jsp in the DEFAULT_DISPLAY_URL must match this pattern as well,
        as does the url in the entry in amConsoleConfig.xml
         */
        super("RestSTSHome");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        createTableModel();
        createPageTitleModel();
        registerChildren();
    }

    protected void registerChildren() {
        super.registerChildren();
        registerChild(PAGETITLE, CCPageTitle.class);
        registerChild(TBL_SEARCH, CCActionTable.class);
        ptModel.registerChildren(this);
        tblModel.registerChildren(this);
    }

    protected View createChild(String name) {
        View view;

        if (name.equals(TBL_SEARCH)) {
            SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
            populateTableModel((Set)szCache.getSerializedObj());
            view = new CCActionTable(this, tblModel, name);
        } else if (name.equals(PAGETITLE)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (tblModel.isChildSupported(name)) {
            view = tblModel.createChild(this, name);
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this,name);
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    public void beginDisplay(DisplayEvent event)
            throws ModelControlException {
        super.beginDisplay(event);
        resetButtonState(TBL_BUTTON_DELETE);
        setRestSTSInstanceNamesInTable();
        setPageTitle(getModel(), "rest.sts.home.page.title");
    }

    protected AMModel getModelInternal() {
        RequestContext rc = RequestManager.getRequestContext();
        HttpServletRequest req = rc.getRequest();
        try {
            return new RestSTSModelImpl(req, getPageSessionAttributes());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error", e.getMessage());
            throw new IllegalStateException("Exception getting model in RestSTSAddViewBean: " + e.getMessage(), e);
        }
    }

    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
                getClass().getClassLoader().getResourceAsStream(
                        "com/sun/identity/console/oneBtnPageTitle.xml"));
        ptModel.setValue("button1", getBackButtonLabel());
    }

    private void createTableModel() {
        tblModel = new CCActionTableModel(
                getClass().getClassLoader().getResourceAsStream(
                        "com/sun/identity/console/tblRestSTSInstances.xml"));
        tblModel.setTitleLabel("label.items");
        tblModel.setActionValue(TBL_BUTTON_ADD, "rest.sts.home.instances.table.button.new");
        tblModel.setActionValue(TBL_BUTTON_DELETE,
                "rest.sts.home.instances.table.button.delete");
        tblModel.setActionValue(TBL_COL_NAME,
                "rest.sts.home.instances.table.column.name");
        tblModel.setActionValue(TBL_COL_ACTION,
                "rest.sts.home.instances.table.action.column.name");
    }

    private void setRestSTSInstanceNamesInTable() {
        RestSTSModel model = (RestSTSModel)getModel();

        try {
            String curRealm = (String)getPageSessionAttribute(
                    AMAdminConstants.CURRENT_REALM);
            populateTableModel(model.getPublishedInstances(curRealm));
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
        }
    }

    private void populateTableModel(Set<String> publishedInstances) {
        tblModel.clearAll();
        SerializedField szCache = (SerializedField)getChild(SZ_CACHE);

        if ((publishedInstances != null) && !publishedInstances.isEmpty()) {
            boolean firstEntry = true;
            for (String instanceName : publishedInstances) {
                if (firstEntry) {
                    firstEntry = false;
                } else {
                    tblModel.appendRow();
                }

                tblModel.setValue(TBL_DATA_NAME, instanceName);
                tblModel.setValue(TBL_DATA_ACTION_HREF, instanceName);
            }
            szCache.setValue((Serializable)publishedInstances);
        } else {
            szCache.setValue(null);
        }
    }

    /**
     * Forwards request to creation view bean.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblButtonAddRequest(RequestInvocationEvent event) {
        RestSTSAddViewBean vb = (RestSTSAddViewBean)getViewBean(RestSTSAddViewBean.class);
        unlockPageTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Forwards request to ViewBean to edit Rest STS instance
     */
    public void handleTblDataActionHrefRequest(RequestInvocationEvent event) {
        RestSTSEditViewBean vb = (RestSTSEditViewBean)getViewBean(
                RestSTSEditViewBean.class);
        String instanceName = hexToString((String)getDisplayFieldValue(
                TBL_DATA_ACTION_HREF));
        setPageSessionAttribute(INSTANCE_NAME, instanceName);
        setPageSessionAttribute(AMAdminConstants.SAVE_VB_NAME, getClass().getName());
        unlockPageTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    public void handleTblButtonDeleteRequest(RequestInvocationEvent event) throws ModelControlException {
        CCActionTable table = (CCActionTable)getChild(TBL_SEARCH);
        table.restoreStateData();

        Integer[] selected = tblModel.getSelectedRows();
        Set<String> instanceNames = new HashSet<String>(selected.length);

        for (int i = 0; i < selected.length; i++) {
            tblModel.setRowIndex(selected[i].intValue());
            instanceNames.add((String) tblModel.getValue(TBL_DATA_NAME));
        }

        try {
            RestSTSModel model = (RestSTSModel)getModel();
            model.deleteInstances(instanceNames);

            if (selected.length == 1) {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                        "rest.sts.home.instance.deleted");
            } else {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                        "rest.sts.home.instances.deleted");
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
        }

        forwardTo();
    }

    /**
     * Handles "back to" page request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event) {
        backTrail();
        forwardToRealmView(event);
    }
}
