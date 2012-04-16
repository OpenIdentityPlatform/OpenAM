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
 * $Id: ServicesViewBean.java,v 1.2 2008/06/25 05:43:12 qcheng Exp $
 *
 */

package com.sun.identity.console.realm;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMFormatUtils;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.components.view.html.SerializedField;
import com.sun.identity.console.realm.model.ServicesModel;
import com.sun.identity.console.realm.model.ServicesModelImpl;
import com.sun.identity.console.service.model.SCUtils;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCButton;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.view.table.CCActionTable;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.model.CCPageTitleModel;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ServicesViewBean
    extends RealmPropertiesBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/realm/Services.jsp";

    private static final String CHILD_TBL_TILED_VIEW = "tableTiledView";
    private static final String TBL_SEARCH = "tblSearch";
    private static final String TBL_BUTTON_ADD = "tblButtonAdd";
    private static final String TBL_BUTTON_DELETE = "tblButtonDelete";
    private static final String TBL_COL_NAME = "tblColName";
    private static final String TBL_DATA_NAME = "tblDataName";
    private static final String TF_DATA_NAME = "tfDataName";
    private static final String TBL_COL_ACTION = "tblColAction";
    private static final String TBL_DATA_ACTION_LABEL = "tblDataActionLabel";
    private static final String PAGETITLE = "pgtitle";
    static final String TBL_DATA_ACTION_HREF = "tblDataActionHref";

    private CCActionTableModel tblModel = null;

    /**
     * Creates a services view bean.
     */
    public ServicesViewBean() {
        super("Services");
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
        registerChild(CHILD_TBL_TILED_VIEW, ServicesTiledView.class);
    }

    protected View createChild(String name) {
        View view = null;

        if (name.equals(CHILD_TBL_TILED_VIEW)) {
            SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
            populateTableModel((Map)szCache.getSerializedObj());
            view = new ServicesTiledView(this, tblModel, name);
        } else if (name.equals(TBL_SEARCH)) {
            ServicesTiledView tView = (ServicesTiledView)getChild(
                CHILD_TBL_TILED_VIEW);
            CCActionTable child = new CCActionTable(this, tblModel, name);
            child.setTiledView(tView);
            view = child;
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
        getServiceNames();
        setPageTitle(getModel(), "page.title.realms.services");

        ServicesModel model = (ServicesModel)getModel();
        String curRealm = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        try {
            if (model.getAssignableServiceNames(curRealm).isEmpty()) {
                CCButton btnAdd = (CCButton)getChild(TBL_BUTTON_ADD);
                btnAdd.setDisabled(true);
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "services.noservices.for.assignment.message");
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
            CCButton btnAdd = (CCButton)getChild(TBL_BUTTON_ADD);
            btnAdd.setDisabled(true);
        }
    }

    protected AMModel getModelInternal() {
        RequestContext rc = RequestManager.getRequestContext();
        HttpServletRequest req = rc.getRequest();
        return new ServicesModelImpl(req, getPageSessionAttributes());
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
                "com/sun/identity/console/tblRMServices.xml"));
        tblModel.setTitleLabel("label.items");
        tblModel.setActionValue(TBL_BUTTON_ADD, "table.services.button.new");
        tblModel.setActionValue(TBL_BUTTON_DELETE,
            "table.services.button.delete");
        tblModel.setActionValue(TBL_COL_NAME,
            "table.services.name.column.name");
        tblModel.setActionValue(TBL_COL_ACTION,
            "table.services.action.column.name");
    }

    private void getServiceNames() {
        ServicesModel model = (ServicesModel)getModel();

        try {
            String curRealm = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            populateTableModel(model.getAssignedServiceNames(curRealm));
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }

    private void populateTableModel(Map nameToDisplayNames) {
        tblModel.clearAll();
        SerializedField szCache = (SerializedField)getChild(SZ_CACHE);

        if ((nameToDisplayNames != null) && !nameToDisplayNames.isEmpty()) {
            Map reverseMap = AMFormatUtils.reverseStringMap(nameToDisplayNames);
            ServicesModel model = (ServicesModel)getModel();
            List list = AMFormatUtils.sortKeyInMap(reverseMap,
                model.getUserLocale());

            boolean firstEntry = true;

            for (Iterator iter = list.iterator(); iter.hasNext(); ) {
                if (firstEntry) {
                    firstEntry = false;
                } else {
                    tblModel.appendRow();
                }

                String displayName = (String)iter.next();
                String name = (String)reverseMap.get(displayName);
                tblModel.setValue(TF_DATA_NAME, name);
                tblModel.setValue(TBL_DATA_NAME, displayName);
                tblModel.setValue(TBL_DATA_ACTION_HREF, name);
                tblModel.setValue(TBL_DATA_ACTION_LABEL,
                    "table.services.action.edit");
            }
            szCache.setValue((Serializable)nameToDisplayNames);
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
        ServicesSelectViewBean vb = (ServicesSelectViewBean)getViewBean(
            ServicesSelectViewBean.class);
        unlockPageTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Forwards request to edit Service view bean.
     *
     * @param serviceName name of the service to be edited
     */
    public void handleTblDataActionHrefRequest(String serviceName) {
        ServicesModel model = (ServicesModel)getModel();

        SCUtils utils = new SCUtils(serviceName, model);
        String propertiesViewBeanURL = utils.getServiceDisplayURL();

        if ((propertiesViewBeanURL != null) &&
            (propertiesViewBeanURL.trim().length() > 0)
        ) {
            String curRealm = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            if (curRealm == null) {
                curRealm = AMModelBase.getStartDN(
                    getRequestContext().getRequest());
            }

            try {
                String pageTrailID = (String)getPageSessionAttribute(
                    PG_SESSION_PAGE_TRAIL_ID);
                propertiesViewBeanURL += "?ServiceName=" + serviceName +
                    "&Location=" +
                    Locale.URLEncodeField(curRealm, getCharset(model)) +
                    "&Template=true&Op=" + AMAdminConstants.OPERATION_EDIT +
                    "&" + PG_SESSION_PAGE_TRAIL_ID + "=" + pageTrailID;
                HttpServletResponse response =
                    getRequestContext().getResponse();
                response.sendRedirect(propertiesViewBeanURL);
            } catch (UnsupportedEncodingException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
                forwardTo();
            } catch (IOException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
                forwardTo();
            }
        } else {
            ServicesEditViewBean vb = (ServicesEditViewBean)getViewBean(
                ServicesEditViewBean.class);
            setPageSessionAttribute(ServicesEditViewBean.SERVICE_NAME,
                serviceName);
            // set save vb to return to this view after selecting back 
            // button in services edit viewbean.
            setPageSessionAttribute(
                AMAdminConstants.SAVE_VB_NAME, getClass().getName());
            unlockPageTrail();
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        }
    }

    /**
     * Deletes ID Repo.
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

        for (int i = 0; i < selected.length; i++) {
            tblModel.setRowIndex(selected[i].intValue());
            names.add((String)tblModel.getValue(TF_DATA_NAME));
        }

        try {
            ServicesModel model = (ServicesModel)getModel();
            String curRealm = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            model.unassignServices(curRealm, names);

            if (selected.length == 1) {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "services.message.deleted");
            } else {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "services.message.deleted.pural");
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
