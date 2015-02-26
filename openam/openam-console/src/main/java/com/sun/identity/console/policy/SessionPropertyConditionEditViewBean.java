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
 * $Id: SessionPropertyConditionEditViewBean.java,v 1.3 2008/07/07 20:39:21 veiming Exp $
 *
 */

/*
 * Portions Copyrighted 2013 ForgeRock, Inc.
 */

package com.sun.identity.console.policy;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.View;
import com.sun.identity.console.policy.model.PolicyModel;
import com.sun.identity.policy.plugins.SessionPropertyCondition;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.view.html.CCCheckBox;
import com.sun.web.ui.view.table.CCActionTable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SessionPropertyConditionEditViewBean
    extends ConditionEditViewBean {
    private static SessionPropertyConditionHelper helper = 
        SessionPropertyConditionHelper.getInstance();
    private static final String PG_SESSION_ORIG = "origValues";
    private CCActionTableModel tblValuesModel;
    public static final String DEFAULT_DISPLAY_URL =
        "/console/policy/SessionPropertyConditionEdit.jsp";

    public SessionPropertyConditionEditViewBean() {
        super("SessionPropertyConditionEdit", DEFAULT_DISPLAY_URL);
    }

    protected View createChild(String name) {
        View view = null;

        if (name.equals(SessionPropertyConditionHelper.ATTR_VALUES)) {
            // Only populate the model if it is in an undefined state.
            if (tblValuesModel.getRowIndex() == -1) {
                Map map = (Map)getPageSessionAttribute(
                        SessionPropertyConditionHelper.PG_SESSION_PROPERTY_VALUES);
                if (map != null) {
                  helper.populateTable(map, propertySheetModel);
                }
            }
            view = new CCActionTable(this, tblValuesModel, name);
        } else {
            view = super.createChild(name);
        }
        return view;
    }

    protected String getConditionXML(
        String curRealm,
        String condType,
        boolean readonly
    ) {
        return helper.getConditionXML(false, readonly);
    }

    protected String getMissingValuesMessage() {
        return helper.getMissingValuesMessage();
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event);
        String name = (String)getPageSessionAttribute(
            SessionPropertyConditionHelper.PG_SESSION_PROPERTY_NAME);
        if (name != null) {
            propertySheetModel.setValue(CONDITION_NAME, name);
        }
        disableButton("tblPolicySessionButtonDelete", true);

        Map map = (Map)getPageSessionAttribute(
            SessionPropertyConditionHelper.PG_SESSION_PROPERTY_VALUES);
        if (map != null) {
            Set set = (Set)map.get(
                SessionPropertyCondition.VALUE_CASE_INSENSITIVE);
            if ((set != null) && !set.isEmpty()) {
                String cbCase = (String)set.iterator().next();
                setDisplayFieldValue(SessionPropertyConditionHelper.CB_CASE,
                    cbCase);
            } else {
                setDisplayFieldValue(SessionPropertyConditionHelper.CB_CASE,
                    "true");
            }
        }
    }

    protected void setPropertiesValues(Map values) {
        Object cached = getPageSessionAttribute(
            SessionPropertyConditionHelper.PG_SESSION_PROPERTY_VALUES);
        if (cached == null) {
            Map map = new HashMap();
            map.putAll(values);
            setPageSessionAttribute(
                SessionPropertyConditionHelper.PG_SESSION_PROPERTY_VALUES,
                (HashMap)map);
            try {
                CCActionTable tbl = (CCActionTable)getChild(
                    SessionPropertyConditionHelper.ATTR_VALUES);
                tbl.resetStateData();
            } catch (ModelControlException e) {
                //no-op, cannot reset action table
            }
            helper.populateTable(map, propertySheetModel);
            Map orig = new HashMap();
            orig.putAll(values);
            setPageSessionAttribute(PG_SESSION_ORIG, (HashMap)orig);
        }
    }

    protected Map getConditionValues(
        PolicyModel model,
        String realmName,
        String conditionType
    ) {
        return getConditionValues(true);
    }

    protected Map getConditionValues(boolean validate) {
        Map map = (Map)getPageSessionAttribute(
            SessionPropertyConditionHelper.PG_SESSION_PROPERTY_VALUES);
        if (map != null) {
            map.remove(SessionPropertyCondition.VALUE_CASE_INSENSITIVE);

            if (!validate || !map.isEmpty()) {
                CCCheckBox cb = (CCCheckBox)getChild(
                    SessionPropertyConditionHelper.CB_CASE);
                Set set = new HashSet(2);
                set.add(cb.isChecked() ? "true" : "false");
                map.put(SessionPropertyCondition.VALUE_CASE_INSENSITIVE, set);
            }
        } else if (!validate) {
            map = new HashMap();
            setPageSessionAttribute(
                SessionPropertyConditionHelper.PG_SESSION_PROPERTY_VALUES,
                (HashMap)map);
            CCCheckBox cb = (CCCheckBox)getChild(
                SessionPropertyConditionHelper.CB_CASE);
            Set set = new HashSet(2);
            set.add(cb.isChecked() ? "true" : "false");
            map.put(SessionPropertyCondition.VALUE_CASE_INSENSITIVE, set);
        }

        return map;
    }

    protected void createTableModel() {
        tblValuesModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/tblPolicySessionProperty.xml"));
        tblValuesModel.setTitleLabel("label.items");
        tblValuesModel.setActionValue("tblPolicySessionColName",
            "policy.table.condition.session.property.column.name");
        tblValuesModel.setActionValue("tblPolicySessionColValues",
            "policy.table.condition.session.property.column.values");
        tblValuesModel.setActionValue("tblPolicySessionButtonAdd",
            "policy.table.condition.session.property.button.add");
        tblValuesModel.setActionValue("tblPolicySessionButtonDelete",
            "policy.table.condition.session.property.button.delete");
        propertySheetModel.setModel(SessionPropertyConditionHelper.ATTR_VALUES,
            tblValuesModel);
    }

    public void handleButton2Request(RequestInvocationEvent event)
        throws ModelControlException
    {
        Map orig = (Map)getPageSessionAttribute(PG_SESSION_ORIG);
        Map map = new HashMap();
        map.putAll(orig);
        setPageSessionAttribute(
            SessionPropertyConditionHelper.PG_SESSION_PROPERTY_VALUES,
            (HashMap)map);
        CCActionTable tbl = (CCActionTable)getChild(
            SessionPropertyConditionHelper.ATTR_VALUES);
        tbl.resetStateData();
        helper.populateTable(map, propertySheetModel);
        forwardTo();
    }

    public void handleTblPolicySessionButtonAddRequest(
        RequestInvocationEvent event
    ) {
        getConditionValues(false);
        SessionPropertyAddViewBean vb = (SessionPropertyAddViewBean)
            getViewBean(SessionPropertyAddViewBean.class);
        setPageSessionAttribute(SessionPropertyAddViewBean.CALL_VIEW_BEAN,
            getClass().getName());
        setPageSessionAttribute(
            SessionPropertyConditionHelper.PG_SESSION_PROPERTY_NAME,
            (String)getDisplayFieldValue(CONDITION_NAME));
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    public void handleTblPolicySessionHrefActionRequest(
        RequestInvocationEvent event
    ) {
        String propertyName = hexToString((String)getDisplayFieldValue(
            SessionPropertyConditionHelper.TBL_DATA_ACTION));
        SessionPropertyEditViewBean vb = (SessionPropertyEditViewBean)
            getViewBean(SessionPropertyEditViewBean.class);
        setPageSessionAttribute(SessionPropertyAddViewBean.CALL_VIEW_BEAN,
            getClass().getName());
        setPageSessionAttribute(
            SessionPropertyConditionHelper.PG_SESSION_PROPERTY_NAME,
            (String)getDisplayFieldValue(CONDITION_NAME));
        setPageSessionAttribute(SessionPropertyEditViewBean.PROPERTY_NAME,
            propertyName);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    public void handleTblPolicySessionButtonDeleteRequest(
        RequestInvocationEvent event
    ) throws ModelControlException {
        Map map = (Map)getPageSessionAttribute(
            SessionPropertyConditionHelper.PG_SESSION_PROPERTY_VALUES);
        CCActionTable tbl = (CCActionTable)getChild(
            SessionPropertyConditionHelper.ATTR_VALUES);
        tbl.restoreStateData();
        Integer[] selected = tblValuesModel.getSelectedRows();

        for (int i = 0; i < selected.length; i++) {
            int idx = selected[i].intValue();
            tblValuesModel.setLocation(idx);
            String propertyName = hexToString((String)tblValuesModel.getValue(
                SessionPropertyConditionHelper.TBL_DATA_ACTION));
            map.remove(propertyName);
            tblValuesModel.removeRow(idx);
        }
        tbl.resetStateData();
        helper.populateTable(map, propertySheetModel);
        forwardTo();
    }

    protected void forwardToPolicyViewBean() {
        removePageSessionAttribute(
            SessionPropertyConditionHelper.PG_SESSION_PROPERTY_NAME);
        removePageSessionAttribute(
            SessionPropertyConditionHelper.PG_SESSION_PROPERTY_VALUES);
        super.forwardToPolicyViewBean();
    }
}
