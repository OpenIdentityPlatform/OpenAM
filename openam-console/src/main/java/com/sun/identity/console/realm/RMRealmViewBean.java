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
 * $Id: RMRealmViewBean.java,v 1.3 2008/07/07 20:39:20 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.realm;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.AMViewConfig;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMFormatUtils;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.components.view.html.SerializedField;
import com.sun.identity.console.realm.model.RMRealmModel;
import com.sun.identity.console.realm.model.RMRealmModelImpl;
import com.sun.identity.sm.SMSSchema;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCButton;
import com.sun.web.ui.view.html.CCTextField;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.view.table.CCActionTable;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.model.CCPageTitleModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

public class RMRealmViewBean
    extends RMRealmViewBeanBase
    implements HasEntitiesTabs
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/realm/RMRealm.jsp";

    private static final String TBL_SEARCH = "tblSearch";
    private static final String TF_FILTER = "tfFilter";
    private static final String BTN_SEARCH = "btnSearch";
    private static final String TBL_BUTTON_ADD = "tblButtonAdd";
    private static final String TBL_BUTTON_DELETE = "tblButtonDelete";

    // name column
    private static final String TBL_COL_NAME = "tblColName";
    private static final String TBL_DATA_ACTION_HREF = "tblDataActionHref";
    private static final String TBL_DATA_NAME = "tblDataName";

    // location column
    private static final String TBL_COL_PATH = "tblColPath";
    private static final String TBL_DATA_PATH = "tblDataPath";

    private static final String PAGETITLE = "pgtitle";

    private CCActionTableModel tblModel = null;
    private CCPageTitleModel ptModel;

    /**
     * Creates a authentication domains view bean.
     */
    public RMRealmViewBean() {
        super("RMRealm");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        createTableModel();
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/simplePageTitle.xml"));
        registerChildren();
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
            SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
            populateTableModel((List)szCache.getSerializedObj());
            view = new CCActionTable(this, tblModel, name);
        } else if (name.equals(PAGETITLE)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (tblModel.isChildSupported(name)) {
            view = tblModel.createChild(this, name);
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
        getRealmNames();
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req = getRequestContext().getRequest();
        return new RMRealmModelImpl(req, getPageSessionAttributes());
    }

    private void createTableModel() {
        tblModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/tblRMRealm.xml"));
        tblModel.setTitleLabel("label.items");
        tblModel.setActionValue(TBL_BUTTON_ADD, "table.realm.button.new");
        tblModel.setActionValue(TBL_BUTTON_DELETE, "table.realm.button.delete");
        tblModel.setActionValue(TBL_COL_NAME, "table.realm.name.column.name");
        tblModel.setActionValue(TBL_COL_PATH, "table.realm.path.column.name");
    }

    private void getRealmNames() {
        RMRealmModel model = (RMRealmModel)getModel();
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
            populateTableModel(model.getRealmNames(curRealm, filter));
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }

    private void populateTableModel(Collection realmNames) {
        tblModel.clearAll();
        SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
        RMRealmModel model = (RMRealmModel)getModel();
        tblModel.setMaxRows(model.getPageSize());

        if ((realmNames != null) && !realmNames.isEmpty()) {
            String curRealm = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            List cache = new ArrayList(realmNames.size());
            boolean firstEntry = true;
            String startDN = model.getStartDN();
            int counter = 0;

            for (Iterator iter = realmNames.iterator(); iter.hasNext();
                counter++
            ) {
                if (firstEntry) {
                    firstEntry = false;
                } else {
                    tblModel.appendRow();
                }

                String name = (String)iter.next();
                String fqName = null;
                String displayName = null;
                
                if (name.equals(startDN)) {
                    tblModel.setSelectionVisible(counter, false);
                    fqName = name;
                    displayName = AMFormatUtils.DNToName(
                        model, model.getStartDSDN());
                } else {
                    int idx = name.lastIndexOf('/');
                    displayName = (idx == -1) ? name : name.substring(idx+1);
                    tblModel.setSelectionVisible(counter, true);
                    fqName = name;
                }


                /*
                 * Set name column info. Need to unescape the value as it
                 * may contain a '/' character.
                 */
                tblModel.setValue(TBL_DATA_ACTION_HREF, stringToHex(fqName));
                tblModel.setValue(TBL_DATA_NAME, 
                    SMSSchema.unescapeName(displayName));

                // set location column info
                tblModel.setValue(TBL_DATA_PATH, getPath(name));

                cache.add(name);
            }
            szCache.setValue((ArrayList)cache);
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
     * Forwards request to creation view bean.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblButtonAddRequest(RequestInvocationEvent event) {
        RMRealmAddViewBean vb = (RMRealmAddViewBean)getViewBean(
            RMRealmAddViewBean.class);
        unlockPageTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Deletes realms.
     *
     * @param event Request Invocation Event.
     * @throws ModelControlException if table model cannot be restored.
     */
    public void handleTblButtonDeleteRequest(RequestInvocationEvent event)
        throws ModelControlException
    {
        CCActionTable table = (CCActionTable)getChild(TBL_SEARCH);
        table.restoreStateData();
        String curRealm = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        
        Integer[] selected = tblModel.getSelectedRows();
        SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
        List list = (List)szCache.getSerializedObj();
        Set names = new HashSet(selected.length *2);

        for (int i = 0; i < selected.length; i++) {
            String name = (String)list.get(selected[i].intValue());
            /* 
             * make sure the realm name starts with / to give it a
             * fully qualified look. This value will be set in the log file
             */
            if (!name.startsWith("/")) {
                name = "/" + name;
            }  
            names.add(name);
        }

        try {
            RMRealmModel model = (RMRealmModel)getModel();
            model.deleteSubRealms(curRealm, names);
            
            if (selected.length == 1) {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "realm.message.deleted");
            } else {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "realm.message.deleted.pural");
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }

        forwardTo();
    }

    /**
     * Handles edit realm request.
     *
     * @param event Request Invocation Event.
     * @throws ModelControlException if table model cannot be restored.
     */
    public void handleTblDataActionHrefRequest(RequestInvocationEvent event)
        throws ModelControlException
    {
        // store the current realm name and the current selected tab
        // in the page session. Need to do this so after returning 
        // from profile object we get placed in the correct location.
        String prevRealm = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        setPageSessionAttribute(AMAdminConstants.PREVIOUS_REALM, prevRealm);
        
        String newRealm = hexToString(
            (String)getDisplayFieldValue(TBL_DATA_ACTION_HREF));
        setPageSessionAttribute(AMAdminConstants.CURRENT_REALM, newRealm);
        setCurrentLocation(newRealm);

        // store the current selected tab in the page session
        String tmp = (String)getPageSessionAttribute(getTrackingTabIDName());
        setPageSessionAttribute(AMAdminConstants.PREVIOUS_TAB_ID, tmp);

        AMViewConfig config = AMViewConfig.getInstance();
        RMRealmModel model = (RMRealmModel)getModel();
        unlockPageTrail();

        try {
            AMViewBeanBase vb = config.getTabViewBean(
                this, newRealm, model, "realms", -1, -1);
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } catch (AMConsoleException e) {
            setPageSessionAttribute(AMAdminConstants.CURRENT_REALM, prevRealm);
            removePageSessionAttribute(AMAdminConstants.PREVIOUS_TAB_ID);
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
            forwardTo();
        }
    }

    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.realms";
    }
}
