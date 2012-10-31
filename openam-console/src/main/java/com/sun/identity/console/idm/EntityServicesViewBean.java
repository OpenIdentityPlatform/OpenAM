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
 * $Id: EntityServicesViewBean.java,v 1.7 2009/12/01 20:42:42 veiming Exp $
 *
 */

package com.sun.identity.console.idm;

import com.sun.identity.shared.locale.Locale;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMFormatUtils;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.components.view.html.SerializedField;
import com.sun.identity.console.idm.model.EntitiesModel;
import com.sun.identity.console.service.model.SCUtils;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.table.CCActionTable;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;

public class EntityServicesViewBean
    extends EntityEditViewBean
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/idm/EntityServices.jsp";

    private static final String CHILD_TBL_TILED_VIEW = "tableTiledView";
    private static final String TBL_SEARCH = "tblSearch";
    private static final String TBL_BUTTON_ADD = "tblButtonAdd";
    private static final String TBL_BUTTON_DELETE = "tblButtonDelete";
    private static final String TBL_COL_NAME = "tblColName";
    private static final String TBL_DATA_NAME = "tblDataName";
    private static final String TBL_DATA_NAME_EX = "tblDataNameEx";
    static final String TBL_DATA_ACTION_HREF = "tblDataActionHref";

    private CCActionTableModel tblModel = null;

    public EntityServicesViewBean() {
        super("EntityServices", DEFAULT_DISPLAY_URL);
        createTableModel();
    }

    protected void registerChildren() {
        super.registerChildren();
        registerChild(TBL_SEARCH, CCActionTable.class);
        registerChild(CHILD_TBL_TILED_VIEW, ServicesTiledView.class);
        tblModel.registerChildren(this);
    }

    protected View createChild(String name) {
        View view = null;

        if (name.equals(CHILD_TBL_TILED_VIEW)) {
            SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
            populateTableModelEx((List)szCache.getSerializedObj());
            view = new ServicesTiledView(this, tblModel, name);
        } else if (name.equals(TBL_SEARCH)) {
            ServicesTiledView tView = (ServicesTiledView)getChild(
                CHILD_TBL_TILED_VIEW);
            CCActionTable child = new CCActionTable(this, tblModel, name);
            child.setTiledView(tView);
            view = child;
        } else if (tblModel.isChildSupported(name)) {
            view = tblModel.createChild(this, name);
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
    }

    protected void disableSaveAndResetButton() {
    }

    protected void setSelectedTab() {
    }

    protected void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/oneBtnPageTitle.xml"));

        ptModel.setValue("button1", 
            getBackButtonLabel("tab.sub.subjects.label"));
        setPageSessionAttribute(
               getTrackingTabIDName(), Integer.toString(TAB_SERVICES));
    }

    private void createTableModel() {
        tblModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/tblEntityServices.xml"));
        tblModel.setTitleLabel("label.items");
        tblModel.setActionValue(TBL_BUTTON_ADD, "table.services.button.new");
        tblModel.setActionValue(TBL_BUTTON_DELETE,
            "table.services.button.delete");
        tblModel.setActionValue(TBL_COL_NAME,
            "table.services.name.column.name");
    }

    private void getServiceNames() {
        EntitiesModel model = (EntitiesModel)getModel();
        boolean hasServicesToAssign = false;

        try {
            String universalId = (String)getPageSessionAttribute(
                EntityEditViewBean.UNIVERSAL_ID);
            populateTableModel(model.getAssignedServiceNames(universalId));

            hasServicesToAssign = 
                !model.getAssignableServiceNames(universalId).isEmpty();
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
        
        disableButton(TBL_BUTTON_ADD, !hasServicesToAssign ||
            !model.isServicesSupported());
    }

    private void populateTableModelEx(List serviceNames) {
        if ((serviceNames != null) && !serviceNames.isEmpty()) {
            Map map = new HashMap(serviceNames.size() *2);
            EntitiesModel model = (EntitiesModel)getModel();

            for (Iterator iter = serviceNames.iterator(); iter.hasNext(); ) {
                String serviceName = (String)iter.next();
                map.put(serviceName,
                    model.getLocalizedServiceName(serviceName));
            }
            populateTableModel(map);
        }
    }

    private void populateTableModel(Map nameToDisplayNames) {
        tblModel.clearAll();
        SerializedField szCache = (SerializedField)getChild(SZ_CACHE);

        if ((nameToDisplayNames != null) && !nameToDisplayNames.isEmpty()) {
            Map reverseMap = AMFormatUtils.reverseStringMap(nameToDisplayNames);
            EntitiesModel model = (EntitiesModel)getModel();
            List list = AMFormatUtils.sortKeyInMap(reverseMap,
                model.getUserLocale());
            List cache = new ArrayList(list.size());
            boolean firstEntry = true;

            for (Iterator iter = list.iterator(); iter.hasNext(); ) {
                if (firstEntry) {
                    firstEntry = false;
                } else {
                    tblModel.appendRow();
                }

                String displayName = (String)iter.next();
                String name = (String)reverseMap.get(displayName);

                tblModel.setValue(TBL_DATA_NAME_EX, "");
                tblModel.setValue(TBL_DATA_NAME, displayName);
                tblModel.setValue(TBL_DATA_ACTION_HREF, name);

                cache.add(name);
            }
            szCache.setValue((Serializable)cache);
        } else {
            szCache.setValue(null);
        }
    }

    public void handleTblButtonAddRequest(RequestInvocationEvent event) {
        ServicesSelectViewBean vb = (ServicesSelectViewBean)getViewBean(
            ServicesSelectViewBean.class);
        unlockPageTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    public void handleTblButtonDeleteRequest(RequestInvocationEvent event)
        throws ModelControlException {
        CCActionTable table = (CCActionTable)getChild(TBL_SEARCH);
        table.restoreStateData();

        Integer[] selected = tblModel.getSelectedRows();
        Set names = new HashSet(selected.length *2);
        SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
        List cache = (List)szCache.getSerializedObj();

        if (selected.length > 0) {
            for (int i = 0; i < selected.length; i++) {
                names.add((String)cache.get(selected[i].intValue()));
            }

            try {
                EntitiesModel model = (EntitiesModel)getModel();
                String universalId = (String)getPageSessionAttribute(
                    EntityEditViewBean.UNIVERSAL_ID);
                model.unassignServices(universalId, names);

                if (selected.length == 1) {
                    setInlineAlertMessage(CCAlert.TYPE_INFO,
                        "message.information",
                        "entities.message.service.unassigned");
                } else {
                    setInlineAlertMessage(CCAlert.TYPE_INFO,
                        "message.information",
                        "entities.message.service.unassigned.pural");
                }
            } catch (AMConsoleException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
            }
        } else {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                "entities.message.service.unassigned.non.selected");
        }

        forwardTo();
    }

    public void handleTblDataActionHrefRequest(String serviceName)
        throws ModelControlException {
        EntitiesModel model = (EntitiesModel)getModel();
        SCUtils utils = new SCUtils(serviceName, model);
        String propertiesViewBeanURL = utils.getServiceDisplayURL();
        String universalId = (String)getPageSessionAttribute(
            EntityEditViewBean.UNIVERSAL_ID);

        // Work around not showing auth config service custom view. 
        // This is not needed in 7.0, but is still used by old 6.3 console.
        if (serviceName.equals(AMAdminConstants.AUTH_CONFIG_SERVICE)) {
            propertiesViewBeanURL = null;
        }

        if ((propertiesViewBeanURL != null) &&
            (propertiesViewBeanURL.trim().length() > 0)
        ) {
            try {
                String realm = (String)getPageSessionAttribute(
                    AMAdminConstants.CURRENT_REALM);
                String pageTrailID = (String)getPageSessionAttribute(
                    PG_SESSION_PAGE_TRAIL_ID);

                setPageSessionAttribute(
                   getTrackingTabIDName(), Integer.toString(TAB_SERVICES));
                propertiesViewBeanURL += "?ServiceName=" + serviceName +
                    "&User=" +
                    Locale.URLEncodeField(stringToHex(universalId), 
                        getCharset(model)) +
                    "&Op=" + AMAdminConstants.OPERATION_EDIT +
                    "&realm=" +
                    Locale.URLEncodeField(realm, getCharset(model)) +
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
            unlockPageTrail();
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        }
    }

    protected AMPropertySheetModel handleNoAttributeToDisplay(
        AMConsoleException e) {
        hasNoAttributeToDisplay = true;
        return new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/propertyBlank.xml"));
    }

    /**
     * Handles "back to" page request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event) {
        removePageSessionAttribute(getTrackingTabIDName());
        setPageSessionAttribute(
            getTrackingTabIDName(), Integer.toString(TAB_PROFILE));
        forwardToEntitiesViewBean();
    }
}
