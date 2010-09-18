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
 * $Id: SCPlatform30ViewBean.java,v 1.2 2008/06/25 05:43:15 qcheng Exp $
 *
 */

package com.sun.identity.console.service;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.AMServiceProfileViewBeanBase;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.service.model.SCPlatformModel;
import com.sun.identity.console.service.model.SCPlatformModelImpl;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.table.CCActionTable;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/**
 * View bean for platform server revision 30.
 */
public class SCPlatform30ViewBean
    extends AMServiceProfileViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/service/SCPlatform30.jsp";
    public static final String PAGE_MODIFIED = "pageModified";

    private static final String TBL_CLIENT_CHAR_SETS_COL_CHAR_SET =
        "tblClientCharSetsColCharSet";
    private static final String TBL_CLIENT_CHAR_SETS_COL_CLIENT_TYPE =
        "tblClientCharSetsColClientType";
    private static final String TBL_CLIENT_CHAR_SETS_DATA_CHAR_SET =
        "tblClientCharSetsDataCharSet";
    private static final String TBL_CLIENT_CHAR_SETS_DATA_CLIENT_TYPE =
        "tblClientCharSetsDataClientType";
    private static final String TBL_CLIENT_CHAR_SETS_HREF_ACTION =
        "tblClientCharSetsHrefAction";
    private static final String TBL_CLIENT_CHAR_SETS_ADD_BTN =
        "tblClientCharSetsButtonAdd";
    private static final String TBL_CLIENT_CHAR_SETS_DELETE_BTN =
        "tblClientCharSetsButtonDelete";

    private boolean tablePopulated = false;

    /**
     * Creates a platform service profile view bean.
     */
    public SCPlatform30ViewBean() {
        super("SCPlatform30", DEFAULT_DISPLAY_URL,
            AMAdminConstants.PLATFORM_SERVICE);
    }

    protected View createChild(String name) {
        if (!tablePopulated) {
            prePopulateTable();
        }
        return super.createChild(name);
    }

    protected void createPropertyModel() {
        super.createPropertyModel();
        createClientCharSetsTableModel();
    }

    private void createClientCharSetsTableModel() {
        CCActionTableModel tblModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/tblPlatformClientCharSets.xml"));
        tblModel.setTitleLabel("label.items");
        tblModel.setActionValue(TBL_CLIENT_CHAR_SETS_COL_CLIENT_TYPE,
            "platform.service.table.clientCharSets.clientType");
        tblModel.setActionValue(TBL_CLIENT_CHAR_SETS_COL_CHAR_SET,
            "platform.service.table.clientCharSets.name");
        tblModel.setActionValue(TBL_CLIENT_CHAR_SETS_ADD_BTN,
            "platform.service.table.clientCharSets.add.button");
        tblModel.setActionValue(TBL_CLIENT_CHAR_SETS_DELETE_BTN,
            "platform.service.table.clientCharSets.delete.button");
        propertySheetModel.setModel(
            SCPlatformModelImpl.ATTRIBUTE_NAME_CLIENT_CHAR_SETS, tblModel);
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        try {
            return new SCPlatformModelImpl(
                req, serviceName, getPageSessionAttributes());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
        return null;
    }

    private void prePopulateTable() {
        Map attributeValues = (Map)removePageSessionAttribute(
            PROPERTY_ATTRIBUTE);
        if (attributeValues != null) {
            AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
            ps.setAttributeValues(attributeValues, getModel());
        }
        prePopulateClientCharSetsTable(attributeValues);
    }

    private void prePopulateClientCharSetsTable(Map attributeValues) {
        Set charsets = null;

        if (attributeValues != null) {
            charsets = new OrderedSet();
            Set set = (Set)attributeValues.get(
                SCPlatformModelImpl.ATTRIBUTE_NAME_CLIENT_CHAR_SETS);
            if ((set != null) && !set.isEmpty()) {
                charsets.addAll(set);
            }
        } else {
            charsets = (Set)removePageSessionAttribute(
                SCPlatformModelImpl.ATTRIBUTE_NAME_CLIENT_CHAR_SETS);
        }

        if (charsets != null) {
            populateClientCharSetsTable(charsets);
        }
    }

    /**
     * Populates client character set table.
     *
     * @param event Display Event.
     */
    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        super.beginDisplay(event);
        resetButtonState(TBL_CLIENT_CHAR_SETS_DELETE_BTN);
        
        if (!tablePopulated) {
            if (!isSubmitCycle()) {
                SCPlatformModel model = (SCPlatformModel)getModel();

                if (model != null) {
                    Set charsets = new OrderedSet();
                    charsets.addAll(model.getAttributeValues(
                        SCPlatformModelImpl.ATTRIBUTE_NAME_CLIENT_CHAR_SETS));
                    populateClientCharSetsTable(charsets);
                }
            }
        }

        if (!isInlineAlertMessageSet()) {
            String flag = (String)getPageSessionAttribute(PAGE_MODIFIED);
            if ((flag != null) && flag.equals("1")) {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "message.profile.modified");
            }
        }
    }

    private void populateClientCharSetsTable(Set charsets) {
        tablePopulated = true;
        CCActionTableModel tblModel =
            (CCActionTableModel)propertySheetModel.getModel(
                SCPlatformModelImpl.ATTRIBUTE_NAME_CLIENT_CHAR_SETS);
        tblModel.clearAll();
        boolean firstEntry = true;
        int counter = 0;

        for (Iterator iter = charsets.iterator(); iter.hasNext(); ) {
            String c = (String)iter.next();
            int idx = c.indexOf('|');

            if (idx != -1) {
                if (!firstEntry) {
                    tblModel.appendRow();
                } else {
                    firstEntry = false;
                }

                tblModel.setValue(TBL_CLIENT_CHAR_SETS_DATA_CLIENT_TYPE,
                    c.substring(0, idx));
                tblModel.setValue(TBL_CLIENT_CHAR_SETS_DATA_CHAR_SET,
                    c.substring(idx+1));
                tblModel.setValue(TBL_CLIENT_CHAR_SETS_HREF_ACTION,
                    Integer.toString(counter));
            }
            counter++;
        }

        setPageSessionAttribute(
            SCPlatformModelImpl.ATTRIBUTE_NAME_CLIENT_CHAR_SETS,
            (OrderedSet)charsets);
    }


    protected boolean onBeforeSaveProfile(Map attrValues) {
        Set charsets = (Set)getPageSessionAttribute(
            SCPlatformModelImpl.ATTRIBUTE_NAME_CLIENT_CHAR_SETS);

        if ((charsets != null) && !charsets.isEmpty()) {
            attrValues.put(SCPlatformModelImpl.ATTRIBUTE_NAME_CLIENT_CHAR_SETS,
                charsets);
        }
        return true;
    }
                                                                                
    protected void onBeforeResetProfile() {
        removePageSessionAttribute(
            SCPlatformModelImpl.ATTRIBUTE_NAME_CLIENT_CHAR_SETS);
        tablePopulated = false;
    }

    /**
     * Handles save request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException
    {
        removePageSessionAttribute(PAGE_MODIFIED);
        super.handleButton1Request(event);
    }

    /**
     * Handles reset request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event)
        throws ModelControlException
    {
        removePageSessionAttribute(PAGE_MODIFIED);
        super.handleButton2Request(event);
    }

    /**
     * Handles reset request.
     *
     * @param event Request invocation event
     */
    public void handleButton3Request(RequestInvocationEvent event) {
        backTrail();
        SCConfigSystemViewBean vb = (SCConfigSystemViewBean)
            getViewBean(SCConfigSystemViewBean.class);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Handles remove client character sets request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblClientCharSetsButtonDeleteRequest(
        RequestInvocationEvent event
    ) throws ModelControlException {
        setSubmitCycle(true);
        CCActionTable table = (CCActionTable)getChild(
            SCPlatformModelImpl.ATTRIBUTE_NAME_CLIENT_CHAR_SETS);
        table.restoreStateData();
        CCActionTableModel tblModel =
            (CCActionTableModel)propertySheetModel.getModel(
                SCPlatformModelImpl.ATTRIBUTE_NAME_CLIENT_CHAR_SETS);
        Integer[] selected = tblModel.getSelectedRows();

        if ((selected != null) && (selected.length > 0)) {
            OrderedSet tblValues = (OrderedSet)getPageSessionAttribute(
                SCPlatformModelImpl.ATTRIBUTE_NAME_CLIENT_CHAR_SETS);
            tblValues.removeAll(selected);
            setPageSessionAttribute(
                SCPlatformModelImpl.ATTRIBUTE_NAME_CLIENT_CHAR_SETS, tblValues);
            populateClientCharSetsTable(tblValues);
        }

        forwardTo();
    }

    /**
     * Handles add client character sets request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblClientCharSetsButtonAddRequest(
        RequestInvocationEvent event
    ) throws ModelControlException {
        try {
            Map values = getValues();
            onBeforeSaveProfile(values);
            setPageSessionAttribute(PROPERTY_ATTRIBUTE, (HashMap)values);
            SCPlatformClientCharSetsAddViewBean vb =
                (SCPlatformClientCharSetsAddViewBean)
                getViewBean(SCPlatformClientCharSetsAddViewBean.class);
            unlockPageTrail();
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }

    /**
     * Handles edit client character sets request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblClientCharSetsHrefActionRequest(
        RequestInvocationEvent event
    ) throws ModelControlException {
        try {
            Map values = getValues();
            onBeforeSaveProfile(values);
            setPageSessionAttribute(PROPERTY_ATTRIBUTE, (HashMap)values);
            SCPlatformClientCharSetsEditViewBean vb =
                (SCPlatformClientCharSetsEditViewBean)getViewBean(
                    SCPlatformClientCharSetsEditViewBean.class);
            unlockPageTrail();
            passPgSessionMap(vb);
            vb.populateValues((String)getDisplayFieldValue(
                TBL_CLIENT_CHAR_SETS_HREF_ACTION));
            vb.forwardTo(getRequestContext());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }

    protected String getBreadCrumbDisplayName() {
        SCPlatformModel model = (SCPlatformModel)getModel();
        Object[] arg = {
            model.getLocalizedServiceName(AMAdminConstants.PLATFORM_SERVICE)};
        return MessageFormat.format(model.getLocalizedString(
            "breadcrumbs.services.edit"), arg);
    }

    protected boolean startPageTrail() {
        return false;
    }
}
