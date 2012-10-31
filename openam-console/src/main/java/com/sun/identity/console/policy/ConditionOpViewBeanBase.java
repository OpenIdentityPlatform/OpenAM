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
 * $Id: ConditionOpViewBeanBase.java,v 1.5 2009/12/01 20:41:43 veiming Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */

package com.sun.identity.console.policy;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.delegation.model.DelegationConfig;
import com.sun.identity.console.policy.model.CachedPolicy;
import com.sun.identity.console.policy.model.PolicyCache;
import com.sun.identity.console.policy.model.PolicyModel;
import com.sun.identity.policy.interfaces.Condition;
import com.sun.web.ui.model.CCAddRemoveModel;
import com.sun.web.ui.model.CCEditableListModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.editablelist.CCEditableList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

public abstract class ConditionOpViewBeanBase
    extends ProfileViewBeanBase
{
    protected static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    public static final String CALLING_VIEW_BEAN = 
        "ConditionOpViewBeanBaseCallingVB";
    public static final String PG_SESSION_CONDITION_NAME = "conditionName";
    public static final String PG_SESSION_CONDITION_TYPE = "conditionType";

    protected static final String REALM_NAME = "tfRealmName";
    protected static final String CONDITION_NAME = "tfConditionName";
    protected static final String CONDITION_TYPE = "conditionTypeName";
    protected static final String CONDITION_TYPE_NAME = "tfConditionTypeName";
    public static final String VALUES_TEXT_VALUE = "valuesTextValue";
    public static final String VALUES_SINGLE_CHOICE_VALUE =
        "valuesSingleChoiceValue";
    public static final String VALUES_MULTIPLE_CHOICE_VALUE =
        "valuesMultipleChoiceValue";

    protected CCPageTitleModel ptModel;
    protected String realmName;
    protected CCAddRemoveModel addRemoveModel;
    protected AMPropertySheetModel propertySheetModel;
    protected boolean canModify;
    protected boolean submitCycle;

    public ConditionOpViewBeanBase(String name, String defaultDisplayURL) {
        super(name, defaultDisplayURL);
    }

    protected void initialize() {
        if (!initialized) {
            initialized = createPropertyModel();

            if (initialized) {
                super.initialize();
                createPageTitleModel();
                createTableModel();
                registerChildren();
            }
        }
    }

    private boolean createPropertyModel() {
        boolean init = false;
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();

        String condType = (String)getPageSessionAttribute(
            PG_SESSION_CONDITION_TYPE);
        if ((condType == null) || (condType.trim().length() == 0)) {
            condType = req.getParameter(getName() + "." + CONDITION_TYPE);

            if ((condType != null) && (condType.trim().length() > 0)) {
                setPageSessionAttribute(PG_SESSION_CONDITION_TYPE, condType);
            }
        }

        String curRealm = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        if ((curRealm == null) || (curRealm.trim().length() == 0)) {
            curRealm = req.getParameter(getName() + "." + REALM_NAME);
            if ((curRealm == null) || (curRealm.trim().length() == 0)) {
                setPageSessionAttribute(
                    AMAdminConstants.CURRENT_REALM, curRealm);
            } else {
                realmName = curRealm;
            }
        } else {
            realmName = curRealm;
        }

        init = (curRealm != null) && (curRealm.trim().length() > 0) &&
            (condType != null) && (condType.trim().length() > 0);

        if (init) {
            DelegationConfig dConfig = DelegationConfig.getInstance();
            canModify = dConfig.hasPermission(realmName, null,
                AMAdminConstants.PERMISSION_MODIFY, getModel(),
                getClass().getName());
            propertySheetModel = new AMPropertySheetModel(
                getConditionXML(curRealm, condType, !canModify));
            propertySheetModel.clear();

            if (canModify) {
                addRemoveModel = new CCAddRemoveModel();
                addRemoveModel.setShowMoveUpDownButtons("false");
                addRemoveModel.clear();
                propertySheetModel.setModel(
                    VALUES_MULTIPLE_CHOICE_VALUE, addRemoveModel);
            }
        }

        return init;
    }


    protected void registerChildren() {
        super.registerChildren();
        registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
        ptModel.registerChildren(this);
        propertySheetModel.registerChildren(this);
    }

    protected View createChild(String name) {
        View view = null;

        if ((ptModel != null) && ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else if (name.equals(PROPERTY_ATTRIBUTE)) {
            view = new AMPropertySheet(this, propertySheetModel, name);
        } else if ((propertySheetModel != null) &&
             propertySheetModel.isChildSupported(name)
        ) {
            view = propertySheetModel.createChild(this, name, getModel());
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    public AMPropertySheetModel getPropertySheetModel() {
        return propertySheetModel;
    }

    public void setErrorMessage(String msg) {
        setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error", msg);
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        super.beginDisplay(event);

        if (!submitCycle) {
            PolicyModel model = (PolicyModel)getModel();

            String conditionName = (String)getPageSessionAttribute(
                PG_SESSION_CONDITION_NAME);
            if ((conditionName != null) && (conditionName.length() > 0)) {
                propertySheetModel.setValue(CONDITION_NAME, conditionName);
            }
            Map map = model.getActiveConditionTypes(realmName);
            String condType = (String)getPageSessionAttribute(
                PG_SESSION_CONDITION_TYPE);
            String i18nName = (String)map.get(condType);
            propertySheetModel.setValue(CONDITION_TYPE_NAME, i18nName);
            propertySheetModel.setValue(REALM_NAME, realmName);
            setPropertiesValues(getDefaultValues());
        }
    }

    protected void setPropertiesValues(Map values) {
        if ((values != null) && !values.isEmpty()) {
            for (Iterator i = values.keySet().iterator(); i.hasNext(); ) {
                String propName = (String)i.next();
                Set val = (Set)values.get(propName);
                if ((val != null) && !val.isEmpty()) {
                    propertySheetModel.setValues(
                        propName, val.toArray(), getModel());
                }
            }
        }
    }

    protected Condition createCondition()
        throws ModelControlException, AMConsoleException
    {
        Condition condition = null;
        String conditionType = (String)getPageSessionAttribute(
            PG_SESSION_CONDITION_TYPE);
        String conditionName = getConditionName();
        Map values = getValues(conditionType);

        if (conditionName != null) {
            if (values != null) {
                condition = createCondition(conditionType, values);
            }
        }

        return condition;
    }

    private Condition createCondition(String conditionType, Map values)
        throws AMConsoleException {
        String realmName = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        PolicyModel model = (PolicyModel)getModel();
        return model.createCondition(realmName, conditionType, values);
    }

    private String getConditionName() {
        String conditionName = (String)propertySheetModel.getValue(
            CONDITION_NAME);
        conditionName = conditionName.trim();
        if (conditionName.length() == 0) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                "policy.missing.conditionName");
            conditionName = null;
        }
        return conditionName;
    }

    protected Map getValues(String conditionType)
        throws ModelControlException {
        PolicyModel model = (PolicyModel)getModel();
        String realmName = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        Map values = getConditionValues(model, realmName, conditionType);

        if ((values == null) || values.isEmpty()) {
            if (!isInlineAlertMessageSet()) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                        getMissingValuesMessage());
            }
            values = null;
        }

        return values;
    }

    protected Map getConditionValues(
        PolicyModel model,
        String realmName,
        String conditionType
    ) {
        List propertyNames = model.getConditionPropertyNames(
            realmName, conditionType);
        Map values = new HashMap(propertyNames.size() *2);

        for (Iterator iter = propertyNames.iterator(); iter.hasNext(); ) {
            String name = (String)iter.next();
            View child = getChild(name);
            if (child instanceof CCEditableList) {
                CCEditableList list = (CCEditableList)child;
                list.restoreStateData();
                CCEditableListModel m = (CCEditableListModel)list.getModel();
                Set selected = getValues(m.getOptionList());
 
                if ((selected != null) && !selected.isEmpty()) {
                    values.put(name, selected);
                }
             } else {
                Object[] array = propertySheetModel.getValues(name);

                if ((array != null) && (array.length > 0)) {
                    if (array.length == 1) {
                        String v = array[0].toString();
                        if ((v != null) && (v.trim().length() > 0)) {
                            Set val = new HashSet(2);
                            val.add(v.trim());
                            values.put(name, val);
                        }
                    } else {
                        values.put(name, AMAdminUtils.toSet(array));
                    }
                }
            }
        }

        return values;
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

    protected void forwardToPolicyViewBean() {
        try {
            Class clazz = Class.forName(
                (String)removePageSessionAttribute(CALLING_VIEW_BEAN));
            PolicyOpViewBeanBase vb = (PolicyOpViewBeanBase)getViewBean(clazz);
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } catch (ClassNotFoundException e) {
            debug.error("ConditionOpViewBeanBase.forwardToPolicyViewBean",e);
            forwardTo();
        }
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

    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException {
        SelectConditionTypeViewBean vb = (SelectConditionTypeViewBean)
            getViewBean(SelectConditionTypeViewBean.class);
        unlockPageTrailForSwapping();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    
    protected String getConditionXML(
        String curRealm,
        String condType,
        boolean readonly
    ) {
        PolicyModel model = (PolicyModel)getModel();
        return model.getConditionXML(curRealm, condType, readonly);
    }

    protected String getMissingValuesMessage() {
        return "policy.missing.condition.value";
    }

    protected void createTableModel() {
        // do nothing.
    }

    protected Map getLabelValueMap(Set values) {
        Map map = new HashMap(values.size() *2);
        for (Iterator iter = values.iterator(); iter.hasNext(); ) {
            String val = (String)iter.next();
            if ((val == null) || (val.length() == 0)) {
                map.put("", getModel().getLocalizedString(
                    "policy.condition.null.realm"));
            } else {
                map.put(val, getPath(val));
            }
        }
        return map;
    }

    protected abstract void createPageTitleModel();
    protected abstract boolean hasValues();
    protected abstract Map getDefaultValues();
}
