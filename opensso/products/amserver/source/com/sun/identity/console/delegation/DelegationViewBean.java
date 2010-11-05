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
 * $Id: DelegationViewBean.java,v 1.4 2009/08/11 18:17:09 asyhuang Exp $
 *
 */

package com.sun.identity.console.delegation;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.sso.SSOToken;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMFormatUtils;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.components.view.html.SerializedField;
import com.sun.identity.console.delegation.model.DelegationModel;
import com.sun.identity.console.delegation.model.DelegationModelImpl;
import com.sun.identity.console.realm.RealmPropertiesBase;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCButton;
import com.sun.web.ui.view.html.CCTextField;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.view.table.CCActionTable;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.model.CCPageTitleModel;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class DelegationViewBean
    extends RealmPropertiesBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/delegation/Delegation.jsp";

    private static final String TF_FILTER = "tfFilter";
    private static final String BTN_SEARCH = "btnSearch";

    private static final String TBL_SEARCH = "tblSearch";
    private static final String TBL_BUTTON_ADD = "tblButtonAdd";
    private static final String TBL_BUTTON_DELETE = "tblButtonDelete";

    private static final String TBL_COL_SPACER = "tblColSpacer";
    private static final String TBL_COL_NAME = "tblColName";
    private static final String TBL_DATA_NAME = "tblDataName";
    private static final String TBL_SPACER = "tblSpacer";
    private static final String TBL_DATA_UNIVERSALNAME = "tblDataUniversalName";
    private static final String TBL_DATA_ACTION_HREF = "tblDataActionHref";

    private static final String PAGETITLE = "pgtitle";

    private CCActionTableModel tblModel = null;
    private boolean tblModelPopulated = false;

    /**
     * Creates a delegation view bean.
     */
    public DelegationViewBean() {
        super("Delegation");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    protected void initialize() {
        if (!initialized) {
            super.initialize();
            initialized = true;
            createPageTitleModel();
            createTableModel();
            registerChildren();
        }
    }

    protected void registerChildren() {
        super.registerChildren();
        registerChild(TF_FILTER, CCTextField.class);
        registerChild(BTN_SEARCH, CCButton.class);
        registerChild(PAGETITLE, CCPageTitle.class);
        registerChild(TBL_SEARCH, CCActionTable.class);
        ptModel.registerChildren(this);
        tblModel.registerChildren(this);
    }

    protected View createChild(String name) {
        View view = null;
        if (name.equals(TBL_SEARCH)) {
            populateTableModelEx();
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
        throws ModelControlException {
        super.beginDisplay(event);
        DelegationModel model = (DelegationModel)getModel();

        setPageTitle(model, "page.title.delegation");
        tblModel.setTitle("table.delegation.title.name");
        tblModel.setTitleLabel("table.delegation.summary");
        tblModel.setSummary("table.delegation.summary");
        getSubjects();
    }

    protected AMModel getModelInternal() {
        RequestContext rc = RequestManager.getRequestContext();
        HttpServletRequest req = rc.getRequest();
        return new DelegationModelImpl(req, getPageSessionAttributes());
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
                "com/sun/identity/console/tblDelegation.xml"));
        tblModel.setTitleLabel("label.items");
        tblModel.setActionValue(TBL_COL_NAME,
            "table.delegation.name.column.name");
    }

    private void getSubjects() {
        DelegationModel model = (DelegationModel)getModel();
        String filter = ((String)getDisplayFieldValue(TF_FILTER));

        if ((filter == null) || (filter.length() == 0)) {
            filter = "*";
            setDisplayFieldValue(TF_FILTER, "*");
        } else {
            filter = filter.trim();
        }

        try {
            String curRealm = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            populateTableModel(model.getSubjects(curRealm, filter));
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }

    private void populateTableModelEx() {
        if (!tblModelPopulated) {
            tblModelPopulated = true;
            SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
            List cache = (List)szCache.getSerializedObj();
            if ((cache != null) && !cache.isEmpty()) {
                populateTableModel(cache);
            }
        }
    }

    private void populateTableModel(Collection DelegationNames) {
        tblModel.clearAll();
        SerializedField szCache = (SerializedField)getChild(SZ_CACHE);

        if ((DelegationNames != null) && !DelegationNames.isEmpty()) {
            List cache = new ArrayList(DelegationNames.size());
            DelegationModel model = (DelegationModel)getModel();
            SSOToken ssoToken = model.getUserSSOToken();
            boolean firstEntry = true;

            for (Iterator iter = DelegationNames.iterator(); iter.hasNext(); ) {
                String id = (String)iter.next();
                try {
                    AMIdentity entity = IdUtils.getIdentity(ssoToken, id);
                    if (firstEntry) {
                        firstEntry = false;
                    } else {
                        tblModel.appendRow();
                    }

                    String name = AMFormatUtils.getIdentityDisplayName(
                        model, entity);
                    tblModel.setValue(TBL_DATA_NAME, name);
                    tblModel.setValue(TBL_DATA_UNIVERSALNAME, id);
                    tblModel.setValue(TBL_DATA_ACTION_HREF, stringToHex(id));

                    cache.add(id);
                } catch (IdRepoException e) {
                    //ignore since ID is not found.
                }
            }
            szCache.setValue((Serializable)cache);
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
        forwardTo();
    }

    /**
     * Forwards request to edit permision view bean.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblDataActionHrefRequest(RequestInvocationEvent event) {
        String name = hexToString(
            (String)getDisplayFieldValue(TBL_DATA_ACTION_HREF));
        setPageSessionAttribute(
            DelegationPropertiesViewBean.CURRENT_IDENTITY, name);
        DelegationPropertiesViewBean vb = (DelegationPropertiesViewBean)
            getViewBean(DelegationPropertiesViewBean.class);
        unlockPageTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Handles 'Back to' button request. In this case, it takes you back
     * to the realm view.
     *
     * @param event Request Invocation Event.
     */
    public void handleButton1Request(RequestInvocationEvent event) {
        backTrail();
        forwardToRealmView(event);
    }
}
