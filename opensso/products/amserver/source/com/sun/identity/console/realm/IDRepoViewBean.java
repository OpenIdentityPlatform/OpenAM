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
 * $Id: IDRepoViewBean.java,v 1.3 2008/07/07 20:39:20 veiming Exp $
 *
 */

package com.sun.identity.console.realm;

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
import com.sun.identity.console.realm.model.IDRepoModel;
import com.sun.identity.console.realm.model.IDRepoModelImpl;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCButton;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.view.table.CCActionTable;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.model.CCPageTitleModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

public class IDRepoViewBean
    extends RealmPropertiesBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/realm/IDRepo.jsp";

    private static final String PAGETITLE = "pgtitle";

    private static final String TBL_SEARCH = "tblSearch";
    private static final String TBL_BUTTON_ADD = "tblButtonAdd";
    private static final String TBL_BUTTON_DELETE = "tblButtonDelete";

    private static final String TBL_COL_NAME = "tblColName";
    private static final String TBL_DATA_NAME = "tblDataName";
    private static final String TBL_DATA_ACTION_HREF = "tblDataActionHref";

    private static final String TBL_COL_TYPE = "tblColType";
    private static final String TBL_DATA_TYPE = "tblDataType";
    
    private CCActionTableModel tblModel = null;

    /**
     * Creates a ID Repo view bean.
     */
    public IDRepoViewBean() {
        super("IDRepo");
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
        View view = null;

        if (name.equals(TBL_SEARCH)) {
            SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
            populateTableModel((List)szCache.getSerializedObj());
            view = new CCActionTable(this, tblModel, name);
        } else if (name.equals(PAGETITLE)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (tblModel.isChildSupported(name)) {
            view = tblModel.createChild(this, name);
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event);
        resetButtonState(TBL_BUTTON_DELETE);
        getIDRepoNames();
        setPageTitle(getModel(), "page.title.realms.idrepo");
    }

    protected AMModel getModelInternal() {
        RequestContext rc = RequestManager.getRequestContext();
        HttpServletRequest req = rc.getRequest();
        return new IDRepoModelImpl(req, getPageSessionAttributes());
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
                "com/sun/identity/console/tblRMIDRepo.xml"));
        tblModel.setTitleLabel("label.items");
        tblModel.setActionValue(TBL_BUTTON_ADD, "table.idrepo.button.new");
        tblModel.setActionValue(TBL_BUTTON_DELETE,
            "table.idrepo.button.delete");
        tblModel.setActionValue(TBL_COL_NAME, "table.idrepo.name.column.name");
        tblModel.setActionValue(TBL_COL_TYPE, "table.idrepo.name.column.type");
    }

    private void getIDRepoNames() {
        IDRepoModel model = (IDRepoModel)getModel();

        try {
            String curRealm = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
            populateTableModel(model.getIDRepoNames(curRealm));
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
            // disable the button if the current location is invalid
            CCButton btnAdd = (CCButton)getChild(TBL_BUTTON_ADD);
            btnAdd.setDisabled(true);
        }
    }

    private void populateTableModel(Collection idRepoNames) {
        tblModel.clearAll();
        SerializedField szCache = (SerializedField)getChild(SZ_CACHE);

        if ((idRepoNames != null) && !idRepoNames.isEmpty()) {
            List cache = new ArrayList(idRepoNames.size());
            IDRepoModel model = (IDRepoModel)getModel();
            String curRealm = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            Map mapTypeToDisplayName = null;

            try {
                mapTypeToDisplayName = model.getIDRepoTypesMap();
            } catch (AMConsoleException e) {
                mapTypeToDisplayName = Collections.EMPTY_MAP;
            }

            boolean firstEntry = true;

            for (Iterator iter = idRepoNames.iterator(); iter.hasNext(); ) {
                if (firstEntry) {
                    firstEntry = false;
                } else {
                    tblModel.appendRow();
                }

                String name = (String)iter.next();
                tblModel.setValue(TBL_DATA_NAME, name);

                try {
                    String type = model.getIDRepoType(curRealm, name);
                    String displayName = (String)mapTypeToDisplayName.get(type);
                    if (displayName == null) {
                        displayName = type;
                    }
                    tblModel.setValue(TBL_DATA_TYPE, displayName);
                } catch (AMConsoleException e) {
                    tblModel.setValue(TBL_DATA_TYPE, "");
                }

                tblModel.setValue(TBL_DATA_ACTION_HREF, 
                    stringToHex(name));
                cache.add(name);
            }
            szCache.setValue((ArrayList)cache);
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
        IDRepoModel model = (IDRepoModel)getModel();
        try {
            Map map = model.getIDRepoTypesMap();

/*
 * This causes the first step of the New Data Store wizard to be skipped
 * when only one Data Store type is available. The ideal fix is to modify the 
 * second step page to act as a single page. This is only a temporary fix 
 * until the page two work can be done.
 */
             /********
            if (map.size() == 1) {
                IDRepoAddViewBean vb = (IDRepoAddViewBean)getViewBean(
                    IDRepoAddViewBean.class);
                setPageSessionAttribute(IDRepoAddViewBean.IDREPO_NAME, "");
                setPageSessionAttribute(IDRepoAddViewBean.IDREPO_TYPE,
                    (String)map.keySet().iterator().next());
                unlockPageTrail();
                passPgSessionMap(vb);
                vb.forwardTo(getRequestContext());
            } else {
                IDRepoSelectTypeViewBean vb =
                    (IDRepoSelectTypeViewBean)getViewBean(
                    IDRepoSelectTypeViewBean.class);
                unlockPageTrail();
                passPgSessionMap(vb);
                vb.forwardTo(getRequestContext());
            }
            ********/

            IDRepoSelectTypeViewBean vb =
                (IDRepoSelectTypeViewBean)getViewBean(
                IDRepoSelectTypeViewBean.class);
            unlockPageTrail();
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
            forwardTo();
        }
    }

    /**
     * Forwards request to edit ID Repo view bean.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblDataActionHrefRequest(RequestInvocationEvent event) {
        String idRepoName = hexToString((String)getDisplayFieldValue(
            TBL_DATA_ACTION_HREF));
        String curRealm = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);

        try {
            IDRepoModel model = (IDRepoModel)getModel();
            setPageSessionAttribute(IDRepoOpViewBeanBase.IDREPO_NAME,
                idRepoName);
            setPageSessionAttribute(IDRepoOpViewBeanBase.IDREPO_TYPE,
                model.getIDRepoType(curRealm, idRepoName));
            IDRepoEditViewBean vb = (IDRepoEditViewBean)getViewBean(
                IDRepoEditViewBean.class);
            unlockPageTrail();
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
            forwardTo();
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
        SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
        List list = (List)szCache.getSerializedObj();
        Set names = new HashSet(selected.length *2);

        for (int i = 0; i < selected.length; i++) {
            names.add(list.get(selected[i].intValue()));
        }

        try {
            IDRepoModel model = (IDRepoModel)getModel();
            String curRealm = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            model.deleteIDRepos(curRealm, names);

            if (selected.length == 1) {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "idRepo.message.deleted");
            } else {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "idRepo.message.deleted.pural");
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
