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
 * $Id: SCPolicyViewBean.java,v 1.5 2009/04/24 01:43:02 ericow Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.service;

import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.model.ModelControlException;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMServiceProfileModel;
import com.sun.identity.console.property.PropertyXMLBuilderBase;
import com.sun.identity.console.service.model.PolicyResourceComparator;
import com.sun.identity.console.service.model.SCPolicyModel;
import com.sun.identity.policy.PolicyConfig;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.table.CCActionTable;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * SCPolicyViewBean is responsible for constructing the Policy Config
 * service view. It is also responsible for handling the save request for
 * modifying the service properties.  This view is found under the 
 * <code>Configuration - Policy </code> tab.
 */
public class SCPolicyViewBean extends SCServiceProfileViewBean
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/service/SCPolicy.jsp";
    public static final String PAGE_MODIFIED = "pageModified";

    private static final String TBL_RESOURCE =
        "tblResourceComparator";
    private static final String TBL_RESOURCE_COMPARATOR_COL_NAME =
        "tblResourceComparatorColName";
    private static final String TBL_RESOURCE_COMPARATOR_COL_ACTION =
        "tblResourceComparatorColAction";
    private static final String TBL_RESOURCE_COMPARATOR_DATA_NAME =
        "tblResourceComparatorDataName";
    private static final String TBL_RESOURCE_COMPARATOR_HREF_ACTION =
        "tblResourceComparatorHrefAction";
    private static final String TBL_RESOURCE_COMPARATOR_LABEL_ACTION =
        "tblResourceComparatorLabelAction";
    private static final String TBL_RESOURCE_COMPARATOR_ADD_BTN =
        "tblResourceComparatorButtonAdd";
    private static final String TBL_RESOURCE_COMPARATOR_DELETE_BTN =
        "tblResourceComparatorButtonDelete";

    private boolean tablePopulated = false;
    private boolean submitCycle = false;

    /**
     * Creates a policy configuration view bean.
     */
    public SCPolicyViewBean() {
        super("SCPolicy", DEFAULT_DISPLAY_URL);
    }

    protected View createChild(String name) {
        if (!tablePopulated) {
            prePopulateTable();
        }
        return super.createChild(name);
    }

    protected void createPropertyModel() {
        super.createPropertyModel();
        createResourceComparatorTableModel();
    }

    private void createResourceComparatorTableModel() {
        CCActionTableModel tblModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/tblPolicyResourceComparator.xml"));
        tblModel.setTitleLabel("label.items");
        tblModel.setActionValue(TBL_RESOURCE_COMPARATOR_COL_NAME,
            "policy.service.table.resource.comparator.name");
        tblModel.setActionValue(TBL_RESOURCE_COMPARATOR_COL_ACTION,
            "policy.service.table.resource.comparator.action");
        tblModel.setActionValue(TBL_RESOURCE_COMPARATOR_ADD_BTN,
            "policy.service.table.resource.comparator.add.button");
        tblModel.setActionValue(TBL_RESOURCE_COMPARATOR_DELETE_BTN,
            "policy.service.table.resource.comparator.delete.button");
        propertySheetModel.setModel(TBL_RESOURCE, tblModel);
    }

    protected String getPropertySheetXML(AMServiceProfileModel model)
        throws AMConsoleException
    {
        String xmlString = super.getPropertySheetXML(model);
        String xml = AMAdminUtils.getStringFromInputStream(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/policyProperties.xml"));
        return PropertyXMLBuilderBase.removeSubSection(
            xmlString, PolicyConfig.RESOURCE_COMPARATOR, xml);
    }

    private void prePopulateTable() {
        Map attributeValues = (Map)getPageSessionAttribute(
            PROPERTY_ATTRIBUTE);
        submitCycle = false;
        prePopulateResourceComparatorTable(attributeValues);
    }

    private void prePopulateResourceComparatorTable(Map attributeValues) {
        Set resourceComp = null;

        if (attributeValues != null) {
            resourceComp = (Set)attributeValues.get(
                SCPolicyModel.ATTRIBUTE_NAME_RESOURCE_COMPARATOR);
            if (resourceComp == null) {
                resourceComp = new OrderedSet();
            }
        } else {
            resourceComp = (Set)removePageSessionAttribute(
                SCPolicyModel.ATTRIBUTE_NAME_RESOURCE_COMPARATOR);
        }

        if (resourceComp != null) {
            populateResourceComparatorTable(resourceComp);
        }
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        super.beginDisplay(event);
        resetButtonState(TBL_RESOURCE_COMPARATOR_DELETE_BTN);
                                                                                
        if (!tablePopulated && !submitCycle) {
            AMServiceProfileModel model = (AMServiceProfileModel)getModel();
            Set resourceComp = new OrderedSet();
            Map values = model.getAttributeValues();
            resourceComp.addAll((Set)values.get(
                SCPolicyModel.ATTRIBUTE_NAME_RESOURCE_COMPARATOR));
            populateResourceComparatorTable(resourceComp);
        }
                                                                                
        if (!isInlineAlertMessageSet()) {
            String flag = (String)getPageSessionAttribute(PAGE_MODIFIED);
            if ((flag != null) && flag.equals("1")) {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "message.profile.modified");
            }
        }
    }

    private void populateResourceComparatorTable(Set resourceComp) {
        tablePopulated = true;
        CCActionTableModel tblModel =
            (CCActionTableModel)propertySheetModel.getModel(TBL_RESOURCE);
        tblModel.clearAll();
        boolean firstEntry = true;
        int counter = 0;

        for (Iterator iter = resourceComp.iterator(); iter.hasNext(); ) {
            String c = (String)iter.next();
            PolicyResourceComparator comp = new PolicyResourceComparator(c);

            if (!firstEntry) {
                tblModel.appendRow();
            } else {
                firstEntry = false;
            }

            tblModel.setValue(TBL_RESOURCE_COMPARATOR_DATA_NAME,
                comp.getServiceType());
            tblModel.setValue(TBL_RESOURCE_COMPARATOR_HREF_ACTION,
                Integer.toString(counter));
            tblModel.setValue(TBL_RESOURCE_COMPARATOR_LABEL_ACTION,
                "policy.service.table.resource.comparator.action.edit.label");
            counter++;
        }

        setPageSessionAttribute(
            SCPolicyModel.ATTRIBUTE_NAME_RESOURCE_COMPARATOR,
            (OrderedSet)resourceComp);
    }

    protected boolean onBeforeSaveProfile(Map attrValues) {
        String flag = (String)getPageSessionAttribute(PAGE_MODIFIED);
        if ((flag != null) && flag.equals("1")) {
            Set resourceComp = (Set)getPageSessionAttribute(
                SCPolicyModel.ATTRIBUTE_NAME_RESOURCE_COMPARATOR);
            if ((resourceComp != null) && !resourceComp.isEmpty()) {
                attrValues.put(SCPolicyModel.ATTRIBUTE_NAME_RESOURCE_COMPARATOR,
                    resourceComp);
            }
        }
        return true;
    }

    protected boolean onBeforeDisplayProfile(Map attrValues) {
        Set resourceComp = (Set)getPageSessionAttribute(
            SCPolicyModel.ATTRIBUTE_NAME_RESOURCE_COMPARATOR);
        if ((resourceComp != null) && !resourceComp.isEmpty()) {
            attrValues.put(SCPolicyModel.ATTRIBUTE_NAME_RESOURCE_COMPARATOR,
                resourceComp);
        }
        return true;
    }

    protected void onBeforeResetProfile() {
        removePageSessionAttribute(
            SCPolicyModel.ATTRIBUTE_NAME_RESOURCE_COMPARATOR);
        tablePopulated = false;
    }

    protected Map getAttributeValues() {
        Map values = (Map)removePageSessionAttribute(PROPERTY_ATTRIBUTE);
        return (values == null) ? super.getAttributeValues() : values;
    }

    /**
     * Handles remove resource comparator request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblResourceComparatorButtonDeleteRequest(
        RequestInvocationEvent event
    ) throws ModelControlException {
        submitCycle = true;
        CCActionTable table = (CCActionTable)getChild(TBL_RESOURCE);
        table.restoreStateData();
        CCActionTableModel tblModel =
            (CCActionTableModel)propertySheetModel.getModel(TBL_RESOURCE);
        Integer[] selected = tblModel.getSelectedRows();

        if ((selected != null) && (selected.length > 0)) {
            OrderedSet tblValues = (OrderedSet)getPageSessionAttribute(
                SCPolicyModel.ATTRIBUTE_NAME_RESOURCE_COMPARATOR);
            tblValues.removeAll(selected);
            setPageSessionAttribute(
                SCPolicyModel.ATTRIBUTE_NAME_RESOURCE_COMPARATOR, tblValues);
            populateResourceComparatorTable(tblValues);
        }

        try {
            Map values = getAllValues();
            setPageSessionAttribute(PROPERTY_ATTRIBUTE, (HashMap)values);
            setPageSessionAttribute(SCPolicyViewBean.PAGE_MODIFIED, "1");
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
        forwardTo();
    }

    /**
     * Handles add resource comparator request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblResourceComparatorButtonAddRequest(
        RequestInvocationEvent event
    ) throws ModelControlException {
        try {
            Map values = getAllValues();
            onBeforeDisplayProfile(values);
            setPageSessionAttribute(PROPERTY_ATTRIBUTE, (HashMap)values);
            SCPolicyResourceComparatorAddViewBean vb =
                (SCPolicyResourceComparatorAddViewBean)getViewBean(
                    SCPolicyResourceComparatorAddViewBean.class);
            unlockPageTrail();
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }

    protected Map getAllValues()
        throws ModelControlException, AMConsoleException
    {
        Map values = null;
        AMServiceProfileModel model = (AMServiceProfileModel)getModel();

        if (model != null) {
            AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
            values = ps.getAttributeValues(
                model.getAttributeValues(), false, false, model);
        }

        return values;
    }


    /**
     * Handles edit resource comparator request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblResourceComparatorHrefActionRequest(
        RequestInvocationEvent event
    ) throws ModelControlException {
        AMServiceProfileModel model = (AMServiceProfileModel)getModel();
        Map values = model.getAttributeValues();
        onBeforeDisplayProfile(values);
        setPageSessionAttribute(PROPERTY_ATTRIBUTE, (HashMap)values);
        SCPolicyResourceComparatorEditViewBean vb =
            (SCPolicyResourceComparatorEditViewBean)getViewBean(
                SCPolicyResourceComparatorEditViewBean.class);
        unlockPageTrail();
        passPgSessionMap(vb);
        vb.populateValues((String)getDisplayFieldValue(
            TBL_RESOURCE_COMPARATOR_HREF_ACTION));
        vb.forwardTo(getRequestContext());
    }
}
