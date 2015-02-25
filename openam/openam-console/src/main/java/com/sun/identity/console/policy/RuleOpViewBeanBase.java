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
 * $Id: RuleOpViewBeanBase.java,v 1.2 2008/06/25 05:43:05 qcheng Exp $
 *
 */

package com.sun.identity.console.policy;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMDisplayType;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.delegation.model.DelegationConfig;
import com.sun.identity.console.policy.model.CachedPolicy;
import com.sun.identity.console.policy.model.PolicyCache;
import com.sun.identity.console.policy.model.PolicyModel;
import com.sun.identity.policy.ActionSchema;
import com.sun.identity.policy.InvalidNameException;
import com.sun.identity.policy.NameNotFoundException;
import com.sun.identity.policy.Policy;
import com.sun.identity.policy.Rule;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.model.CCEditableListModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCSelectableList;
import com.sun.web.ui.view.table.CCActionTable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;


public abstract class RuleOpViewBeanBase
    extends ProfileViewBeanBase
{
    protected static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    public static final String CALLING_VIEW_BEAN = 
        "RuleOpViewBeanBaseCallingVB";
    public static final String WITH_RESOURCE = "withResource";
    private static final String PG_SESSION_ACTION_SCHEMA_NAMES
        = "actionSchemaNames";
    private static final String SELECTION_CHKBOX_NAME =
        "tblActions.SelectionCheckbox";

    protected static final String SERVICE_TYPE = "tfServiceType";
    protected static final String SERVICE_TYPE_NAME_LBL = "serviceTypeName";
    protected static final String SERVICE_TYPE_NAME = "tfServiceTypeName";
    protected static final String RULE_NAME = "tfRuleName";
    protected static final String MANAGED_RESOURCES = "cbManagedResources";
    protected static final String RESOURCE_NAME = "tfResourceName";
    public static final String ACTIONS_TILED_VIEW = "actionTiledView";
    public static final String TBL_ACTIONS = "tblActions";
    public static final String TBL_ACTIONS_COL_NAME = "tblActionsColName";
    public static final String TBL_ACTIONS_COL_VALUE = "tblActionsColAction";
    public static final String TBL_ACTIONS_DATA_NAME = "tblActionsDataName";
    public static final String TBL_ACTIONS_TEXT_VALUE = "tblActionsTextValue";
    public static final String TBL_ACTIONS_PASSWORD_VALUE =
        "tblActionsPasswordValue";
    public static final String TBL_ACTIONS_RADIO_VALUE = "tblActionsRadioValue";
    public static final String TBL_ACTIONS_CHECKBOX_VALUE =
        "tblActionsCheckBoxValue";
    public static final String TBL_ACTIONS_TEXTAREA_VALUE =
        "tblActionsTextAreaValue";
    public static final String TBL_ACTIONS_DROPDOWN_MENU =
        "tblActionsDropDownMenuValue";
    public static final String TBL_ACTIONS_SELECTABLE_LIST =
        "tblActionsSelectableListValue";
    public static final String TBL_ACTIONS_EDITABLE_LIST =
        "tblActionsEditableListValue";

    protected CCPageTitleModel ptModel;
    private CCActionTableModel tblActionsModel;
    public AMPropertySheetModel propertySheetModel;
    protected boolean submitCycle;
    protected boolean canModify;
    protected Map actionValues = null;

    private List actionSchemas = new ArrayList();

    /**
     * Creates a policy operation base view bean.
     *
     * @param name Name of view
     */
    public RuleOpViewBeanBase(String name, String defaultDisplayURL) {
        super(name, defaultDisplayURL);
        createPageTitleModel();
    }

    protected void initialize() {
        if (!initialized) {
            try {
                CachedPolicy cachedPolicy = getCachedPolicy();
                if (cachedPolicy != null) {
                    super.initialize();
                    createPropertyModel();
                    createTableModels();
                    registerChildren();
                    initialized = true;
                }
            } catch (AMConsoleException e) {
                debug.warning("RuleOpViewBeanBase.initialize", e);
                //NO-OP
            }
        }
    }

    protected void createPropertyModel() {
        String realmName = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        DelegationConfig dConfig = DelegationConfig.getInstance();
        canModify = dConfig.hasPermission(realmName, null,
            AMAdminConstants.PERMISSION_MODIFY, getModel(),
            getClass().getName());
        propertySheetModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(
                getPropertyXMLFileName(!canModify)));
        propertySheetModel.clear();
    }

    private void createTableModels() {
        tblActionsModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/tblPMRulesActions.xml"));
        tblActionsModel.setTitleLabel("label.items");
        tblActionsModel.setActionValue(TBL_ACTIONS_COL_NAME,
            "policy.rules.actions.table.column.name");
        tblActionsModel.setActionValue(TBL_ACTIONS_COL_VALUE,
            "policy.rules.actions.table.column.value");
        tblActionsModel.setModel(TBL_ACTIONS_EDITABLE_LIST,
            new CCEditableListModel());
        propertySheetModel.setModel(TBL_ACTIONS, tblActionsModel);
    }

    protected abstract void createPageTitleModel();
    protected abstract String getPropertyXMLFileName(boolean readonly);

    protected void registerChildren() {
        super.registerChildren();
        registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
        registerChild(ACTIONS_TILED_VIEW, ActionTiledView.class);
        ptModel.registerChildren(this);
        propertySheetModel.registerChildren(this);
    }

    protected View createChild(String name) {
        View view = null;

        if (name.equals(ACTIONS_TILED_VIEW)) {
            view = new ActionTiledView(this, tblActionsModel, name);
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else if (name.equals(TBL_ACTIONS)) {
            CCActionTable table = new CCActionTable(
                this, tblActionsModel, name);
            table.setTiledView((ActionTiledView)getChild(ACTIONS_TILED_VIEW));
            view = table;
        } else if (name.equals(PROPERTY_ATTRIBUTE)) {
            view = new AMPropertySheet(this, propertySheetModel, name);
        } else if (propertySheetModel.isChildSupported(name)) {
            view = propertySheetModel.createChild(this, name, getModel());
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        super.beginDisplay(event);
    }

    protected void getManagedResources() {
        if (canModify) {
            PolicyModel model = (PolicyModel)getModel();
            String realmName = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            String serviceType = (String)propertySheetModel.getValue(
                SERVICE_TYPE);
            CCSelectableList cb = (CCSelectableList)getChild(MANAGED_RESOURCES);
            cb.setOptions(createOptionList(model.getManagedResources(
                realmName, serviceType)));
        }
    }

    protected void populateActionsTable(boolean retainSelectedEntry) 
        throws ModelControlException, AMConsoleException
    {
        CCActionTable table = (CCActionTable)getChild(TBL_ACTIONS);
        table.resetStateData();
        tblActionsModel.clearAll();
        PolicyModel model = (PolicyModel)getModel();
        String realmName = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        String serviceType = (String)propertySheetModel.getValue(SERVICE_TYPE);

        boolean withResource = false;
        Boolean b = ((Boolean)getPageSessionAttribute(WITH_RESOURCE));
        if (b != null) {
            withResource = b.booleanValue();
        } 

        actionSchemas = new ArrayList(model.getActionSchemas(
            getCachedPolicy().getPolicy(), realmName, serviceType,
            withResource));

        if ((actionSchemas != null) && !actionSchemas.isEmpty()) {
            int sz = actionSchemas.size();
            ArrayList actionSchemaNames = new ArrayList(sz);

            for (int i = 0;  i < sz; i++) {
                if (i > 0) {
                    tblActionsModel.appendRow();
                }

                ActionSchema as = (ActionSchema)actionSchemas.get(i);
                boolean sel = isActionSelected(as);
                tblActionsModel.setRowSelected(i, sel);

                actionSchemaNames.add(as.getName());
                tblActionsModel.setValue(TBL_ACTIONS_DATA_NAME, 
                    model.getActionSchemaLocalizedName(serviceType, as));
            }
            setPageSessionAttribute(PG_SESSION_ACTION_SCHEMA_NAMES,
                actionSchemaNames);
        }
    }

    protected Rule createRule()
        throws ModelControlException {
        Rule rule = null;
        String serviceType = (String)propertySheetModel.getValue(SERVICE_TYPE);
        String ruleName = getRuleName();

        Boolean b = ((Boolean)getPageSessionAttribute(WITH_RESOURCE));
        boolean withResource = (b != null) ? b.booleanValue() : false;
        String resourceName = (withResource) ? getResourceName() : null;

        // Get action values if this is not a referral policy
        actionValues = (!isReferralPolicy()) ?
            getActionValues(serviceType, withResource) : Collections.EMPTY_MAP;

        if ((ruleName != null) &&
            (!withResource || (resourceName != null)) &&
            (actionValues != null) 
        ) {
            rule = createRule(ruleName, serviceType, resourceName,actionValues);
        }

        return rule;
    }

    private Rule createRule(
        String ruleName,
        String serviceType,
        String resourceName,
        Map actionValues
    ) {
        Rule rule = null;
        try {
            rule = new Rule(ruleName, serviceType, resourceName, actionValues);
        } catch (NameNotFoundException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                getModel().getErrorString(e));
        } catch (InvalidNameException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                getModel().getErrorString(e));
        }
        return rule;
    }

    private String getRuleName() {
        String ruleName = (String)propertySheetModel.getValue(RULE_NAME);
        ruleName = ruleName.trim();
        if (ruleName.length() == 0) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                "policy.missing.ruleName");
            ruleName = null;
        }
        return ruleName;
    }

    private String getResourceName() {
        String resourceName = (String)propertySheetModel.getValue(
            RESOURCE_NAME);
        resourceName = resourceName.trim();
        if (resourceName.length() == 0) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                "policy.missing.resourceName");
            resourceName = null;
        }
        return resourceName;
    }

    private Map getActionValues(String serviceType, boolean withResource)
        throws ModelControlException {
        Map actionValues = new HashMap();
        PolicyModel model = (PolicyModel)getModel();
        String realmName = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);

        try {
            Map mapActionSchemas = mapActionSchemaNameToActionSchema(
                model.getActionSchemas(getCachedPolicy().getPolicy(),
                    realmName, serviceType, withResource));

            CCActionTable table = (CCActionTable)getChild(TBL_ACTIONS);
            table.restoreStateData();
            List actionSchemaNames = (List)getPageSessionAttribute(
                PG_SESSION_ACTION_SCHEMA_NAMES);

            if ((actionSchemaNames != null) && !actionSchemaNames.isEmpty()) {
                HttpServletRequest req = getRequestContext().getRequest();
                String chkName = getName() + "." + SELECTION_CHKBOX_NAME;
                int sz = actionSchemaNames.size();
    
                for (int i = 0; i < sz; i++) {
                    String chkValue = req.getParameter(chkName + i);
    
                    if ((chkValue != null) && chkValue.equals("true")) {
                        String actionSchemaName =
                            (String)actionSchemaNames.get(i);
                        ActionSchema actionSchema =
                            (ActionSchema)mapActionSchemas.get(actionSchemaName);
                        Set values = getActionSchemaValues(actionSchema, i);
                        actionValues.put(actionSchemaName, values);
                    }
                }
            }

            if (actionValues.isEmpty()) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    "policy.missing.actionValues");
                actionValues = null;
            }
        } catch (AMConsoleException e) {
            debug.warning("RuleOpViewBeanBase.getActionValues", e);
            //NO-OP
        }

        return actionValues;
    }

    private Set getActionSchemaValues(ActionSchema actionSchema, int idx) {
        Set values = null;
        int type = AMDisplayType.getInstance().getDisplayType(actionSchema);
        int syntax = AMDisplayType.getInstance().getDisplaySyntax(actionSchema);
        tblActionsModel.setRowIndex(idx);

        if (type == AMDisplayType.TYPE_SINGLE) {
            if (syntax == AMDisplayType.SYNTAX_RADIO) {
                values = AMAdminUtils.toSet(
                    tblActionsModel.getValues(TBL_ACTIONS_RADIO_VALUE));
            } else if ((syntax == AMDisplayType.SYNTAX_PASSWORD) ||
                (syntax == AMDisplayType.SYNTAX_ENCRYPTED_PASSWORD)
            ) {
                values = AMAdminUtils.toSet(
                    tblActionsModel.getValues(TBL_ACTIONS_PASSWORD_VALUE));
            } else if (syntax == AMDisplayType.SYNTAX_BOOLEAN) {
                values = AMAdminUtils.toSet(
                    tblActionsModel.getValues(TBL_ACTIONS_CHECKBOX_VALUE));
            } else if (syntax == AMDisplayType.SYNTAX_PARAGRAPH) {
                values = AMAdminUtils.toSet(
                    tblActionsModel.getValues(TBL_ACTIONS_TEXTAREA_VALUE));
            } else {
                values = AMAdminUtils.toSet(
                    tblActionsModel.getValues(TBL_ACTIONS_TEXT_VALUE));
            }
        } else if (type == AMDisplayType.TYPE_LIST) {
            values = AMAdminUtils.toSet(
                tblActionsModel.getValues(TBL_ACTIONS_EDITABLE_LIST));
        } else if (type == AMDisplayType.TYPE_SINGLE_CHOICE) {
            values = AMAdminUtils.toSet(
                tblActionsModel.getValues(TBL_ACTIONS_DROPDOWN_MENU));
        } else if (type == AMDisplayType.TYPE_MULTIPLE_CHOICE) {
            values = AMAdminUtils.toSet(
                tblActionsModel.getValues(TBL_ACTIONS_SELECTABLE_LIST));
        }

        return (values == null) ? Collections.EMPTY_SET : values;
    }

    private Map mapActionSchemaNameToActionSchema(Set actionSchemas) {
        Map map = new HashMap(actionSchemas.size() *2);
        for (Iterator iter = actionSchemas.iterator(); iter.hasNext(); ) {
            ActionSchema as = (ActionSchema)iter.next();
            map.put(as.getName(), as);
        }
        return map;
    }

    protected CachedPolicy getCachedPolicy()
        throws AMConsoleException
    {
        CachedPolicy policy = null;
        String cacheID = (String)getPageSessionAttribute(
            ProfileViewBeanBase.PG_SESSION_POLICY_CACHE_ID);
        if (cacheID != null) {
            PolicyCache cache = PolicyCache.getInstance();
            PolicyModel model = (PolicyModel)getModel();
            policy = model.getCachedPolicy(cacheID);
        }
        return policy;
    }

    /**
     * Handles cancel request.
     *
     * @param event Request invocation event
     */
    public void handleButton3Request(RequestInvocationEvent event) {
        backTrail();
        forwardToPolicyViewBean();
    }

    protected void forwardToPolicyViewBean() {
        try {
            Class clazz = Class.forName(
                (String)removePageSessionAttribute(CALLING_VIEW_BEAN));
            PolicyOpViewBeanBase vb = (PolicyOpViewBeanBase)getViewBean(clazz);
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } catch (ClassNotFoundException e) {
            debug.error("RuleOpViewBeanBase.forwardToPolicyViewBean", e);
            forwardTo();
        }
    }

    public OptionList getChoiceValues(String serviceType, ActionSchema as) {
        PolicyModel model = (PolicyModel)getModel();
        return model.getChoiceValues(serviceType, as);
    }

    public ActionSchema getActionSchema(int idx) {
        return (ActionSchema)actionSchemas.get(idx);
    }

    protected boolean isReferralPolicy() {
        try {
            CachedPolicy cachedPolicy = getCachedPolicy();
            Policy policy = cachedPolicy.getPolicy();
            return policy.isReferralPolicy();
        } catch (AMConsoleException e) {
            debug.warning("RuleOpViewBeanBase.isReferralPolicy", e);
            return false;
        }
    }

    public boolean isSubmitCycle() {
        return submitCycle;
    }

    public Set getCurrentActionValues(ActionSchema as) {
        Set values = null;
        if (actionValues != null)  {
            values = (Set)actionValues.get(as.getName());
        }
        return (values == null) ? Collections.EMPTY_SET : values;
    }

    public abstract Set getDefaultActionValues(ActionSchema as);
    public abstract boolean isActionSelected(ActionSchema as);
}
