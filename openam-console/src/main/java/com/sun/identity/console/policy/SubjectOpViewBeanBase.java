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
 * $Id: SubjectOpViewBeanBase.java,v 1.2 2008/06/25 05:43:06 qcheng Exp $
 *
 */

package com.sun.identity.console.policy;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.ChildDisplayEvent;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMDisplayType;
import com.sun.identity.console.base.model.AMFormatUtils;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.base.model.QueryResults;
import com.sun.identity.console.delegation.model.DelegationConfig;
import com.sun.identity.console.policy.model.CachedPolicy;
import com.sun.identity.console.policy.model.PolicyCache;
import com.sun.identity.console.policy.model.PolicyModel;
import com.sun.identity.policy.Syntax;
import com.sun.identity.policy.ValidValues;
import com.sun.identity.policy.interfaces.Subject;
import com.sun.web.ui.model.CCAddRemoveModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.addremove.CCAddRemove;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCSelect;
import com.sun.web.ui.view.html.CCStaticTextField;
import com.sun.web.ui.view.html.CCTextField;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


public abstract class SubjectOpViewBeanBase
    extends ProfileViewBeanBase
{
    protected static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    public static final String CALLING_VIEW_BEAN = 
        "SubjectOpViewBeanBaseCallingVB";
    public static final String PG_SESSION_SUBJECT_NAME = "subjectName";
    public static final String PG_SESSION_SUBJECT_TYPE = "subjectType";

    protected static final String SYNTAX = "tfSyntax";
    protected static final String SUBJECT_NAME = "tfSubjectName";
    protected static final String SUBJECT_TYPE = "subjectTypeName";
    protected static final String SUBJECT_TYPE_NAME = "tfSubjectTypeName";
    protected static final String EXCLUSIVE = "cbExclusive";
    protected static final String LBL_FILTER = "lblFilter";
    protected static final String FILTER = "tfFilter";
    protected static final String BTN_FILTER = "btnFilter";
    public static final String VALUES_TEXT_VALUE = "valuesTextValue";
    public static final String VALUES_SINGLE_CHOICE_VALUE =
        "valuesSingleChoiceValue";
    public static final String VALUES_MULTIPLE_CHOICE_VALUE =
        "valuesMultipleChoiceValue";

    protected CCPageTitleModel ptModel;
    protected CCAddRemoveModel addRemoveModel;
    public AMPropertySheetModel propertySheetModel;
    protected boolean canModify = true;
    protected boolean bFilter = false;
    protected boolean submitCycle;

    /**
     * Creates a policy operation base view bean.
     *
     * @param name Name of view
     */
    public SubjectOpViewBeanBase(String name, String defaultDisplayURL) {
        super(name, defaultDisplayURL);
    }

    protected void initialize() {
        if (!initialized) {
            String realmName = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);

            if (realmName != null) {
                initialized = true;
                super.initialize();
                createPageTitleModel();
                createPropertyModel(realmName);
                registerChildren();
            }
        }
    }

    protected void createPropertyModel(String realmName) {
        DelegationConfig dConfig = DelegationConfig.getInstance();
        canModify = dConfig.hasPermission(realmName, null,
            AMAdminConstants.PERMISSION_MODIFY, getModel(),
            getClass().getName());

        propertySheetModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(
                getPropertyXMLFileName(!canModify)));
        propertySheetModel.clear();

        if (canModify) {
            addRemoveModel = new CCAddRemoveModel();
            addRemoveModel.setShowMoveUpDownButtons("false");
            addRemoveModel.clear();
            propertySheetModel.setModel(
                VALUES_MULTIPLE_CHOICE_VALUE, addRemoveModel);
        }
    }

    protected abstract void createPageTitleModel();
    protected abstract String getPropertyXMLFileName(boolean readonly);

    protected void registerChildren() {
        super.registerChildren();
        registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
        ptModel.registerChildren(this);
        propertySheetModel.registerChildren(this);
    }

    protected View createChild(String name) {
        View view = null;

        if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else if (name.equals(PROPERTY_ATTRIBUTE)) {
            view = new AMPropertySheet(this, propertySheetModel, name);
        } else if (propertySheetModel.isChildSupported(name)) {
            view = propertySheetModel.createChild(this, name, getModel());
            if (name.equals(FILTER)) {
                ((CCTextField)view).setValue("*");
            }
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        super.beginDisplay(event);
        String realmName = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        String subjectType = (String)getPageSessionAttribute(
            SubjectOpViewBeanBase.PG_SESSION_SUBJECT_TYPE);
        PolicyModel model = (PolicyModel)getModel();
        Syntax syntax = model.getSubjectSyntax(realmName, subjectType);
        int intSyntax = AMDisplayType.getDisplaySyntax(syntax);

        if (!submitCycle) {
            String subjectName = (String)getPageSessionAttribute(
                SubjectOpViewBeanBase.PG_SESSION_SUBJECT_NAME);
            propertySheetModel.setValue(SYNTAX, Integer.toString(intSyntax));

            propertySheetModel.setValue(SUBJECT_NAME, subjectName);
            propertySheetModel.setValue(SUBJECT_TYPE, subjectType);
            QueryResults qr = model.getActiveSubjectTypes(realmName);
            Map map = (Map)qr.getResults();
            String i18nName = (String)map.get(subjectType);
            propertySheetModel.setValue(SUBJECT_TYPE_NAME, i18nName);

            propertySheetModel.setValue(EXCLUSIVE,
                isSubjectExclusive() ? "true" : "false");
        }

        if (intSyntax == AMDisplayType.SYNTAX_MULTIPLE_CHOICE) {
            setAddRemoveModel();
        }
    }

    protected Subject createSubject()
        throws ModelControlException, AMConsoleException {
        Subject subject = null;
        String subjectType = (String)propertySheetModel.getValue(
            SUBJECT_TYPE);
        String subjectName = getSubjectName();
        Set values = null;

        if (hasValues()) {
            values = getValues(subjectType);
            if ((subjectName != null) && (values != null)) {
                subject = createSubject(subjectType, values);
            }
        } else if (subjectName != null) {
            subject = createSubject(subjectType, values);
        }

        return subject;
    }

    protected Set getValues()
        throws ModelControlException
    {
        String subjectType = (String)propertySheetModel.getValue(SUBJECT_TYPE);
        Set values = null;
        if (hasValues()) {
            values = getValues(subjectType, false);
        }
        return values;
    }

    private Subject createSubject(String subjectType, Set values)
        throws AMConsoleException {
        String realmName = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        PolicyModel model = (PolicyModel)getModel();
        return model.createSubject(realmName, subjectType, values);
    }

    protected boolean isExclusive() {
        String exclusive = (String)propertySheetModel.getValue(EXCLUSIVE);
        return exclusive.equals("true");
    }

    private String getSubjectName() {
        String subjectName = (String)propertySheetModel.getValue(
            SUBJECT_NAME);
        subjectName = subjectName.trim();
        if (subjectName.length() == 0) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                "policy.missing.subjectName");
            subjectName = null;
        }
        return subjectName;
    }

    protected Set getValues(String subjectType)
        throws ModelControlException {
        return getValues(subjectType, true);
    }

    protected Set getValues(String subjectType, boolean bAlert)
        throws ModelControlException {
        PolicyModel model = (PolicyModel)getModel();
        String realmName = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        Syntax syntax = model.getSubjectSyntax(realmName, subjectType);
        Set values = getActionSchemaValues(syntax);

        if (bAlert && values.isEmpty()) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                "policy.missing.subject.value");
            values = null;
        }

        return values;
    }

    private Set getActionSchemaValues(Syntax syntax) {
        Set values = null;
        int syn = AMDisplayType.getDisplaySyntax(syntax);

        switch (syn) {
        case AMDisplayType.SYNTAX_TEXT:
            values = AMAdminUtils.toSet(
                propertySheetModel.getValues(VALUES_TEXT_VALUE));
            break;
        case AMDisplayType.SYNTAX_SINGLE_CHOICE:
            values = AMAdminUtils.toSet(
                propertySheetModel.getValues(VALUES_SINGLE_CHOICE_VALUE));
            break;
        case AMDisplayType.SYNTAX_MULTIPLE_CHOICE:
            if (addRemoveModel != null) {
                CCAddRemove child = (CCAddRemove)getChild(
                    VALUES_MULTIPLE_CHOICE_VALUE);
                child.restoreStateData();
                values = getValues(addRemoveModel.getSelectedOptionList());
                break;
            }
        }

        return (values == null) ? Collections.EMPTY_SET : values;
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
            forwardTo();
        }
    }

    public boolean beginChildDisplay(ChildDisplayEvent event) {
        super.endDisplay(event);
        boolean display = true;
        String childName = event.getChildName();
        int syntax = Integer.parseInt(
            (String)propertySheetModel.getValue(SYNTAX));

        if ((childName.indexOf(FILTER) != -1) ||
            (childName.indexOf(LBL_FILTER) != -1) ||
            (childName.indexOf(BTN_FILTER) != -1)
        ) {
            display = (syntax == AMDisplayType.SYNTAX_SINGLE_CHOICE) ||
                (syntax == AMDisplayType.SYNTAX_MULTIPLE_CHOICE);
        } else if (childName.indexOf(VALUES_TEXT_VALUE) != -1) {
            display = (syntax == AMDisplayType.SYNTAX_TEXTFIELD);
            String value = "";
            if (bFilter) {
                value = (String)getDisplayFieldValue(childName);
            } else {
                Set values = getDefaultValues();
                if ((values != null) && !values.isEmpty()) {
                    value = (String)values.iterator().next();
                }
            }

            if (canModify) {
                CCTextField tf = (CCTextField)getChild(childName);
                tf.setValue(value);
            } else {
                CCStaticTextField tf = (CCStaticTextField)getChild(childName);
                tf.setValue(value);
            }
        } else if (childName.indexOf(VALUES_SINGLE_CHOICE_VALUE) != -1) {
            display = (syntax == AMDisplayType.SYNTAX_SINGLE_CHOICE);
            if (display) {
                setPossibleValues(childName);
            }
        } else if (childName.indexOf(VALUES_MULTIPLE_CHOICE_VALUE) != -1) {
            display = (syntax == AMDisplayType.SYNTAX_MULTIPLE_CHOICE);
        }

        return display;
    }

    protected void setAddRemoveModel()
        throws ModelControlException {
        if (canModify) {
            Set values = getValidValues();
            CCAddRemove child = (CCAddRemove)getChild(
                VALUES_MULTIPLE_CHOICE_VALUE);

            Set defaultValues = (bFilter) ? getValues() : getDefaultValues();
            child.resetStateData();
    
            if (values != null) {
                if (defaultValues != null) {
                    values.removeAll(defaultValues);
                }
                addRemoveModel.setAvailableOptionList(createOptionList(
                    values));
            } else {
                addRemoveModel.setAvailableOptionList(new OptionList());
            }
            addRemoveModel.setSelectedOptionList(createOptionList(
                defaultValues));
        }
    }

    private void setPossibleValues(String childName) {
        if (canModify) {
            CCSelect selectView = (CCSelect)getChild(childName);
            Set values = getValidValues();

            if (values != null){
                OptionList optList = createOptionList(values);
                selectView.setOptions(optList);
            }

            String defaultVal = "";
            if (!bFilter) {
                Set defaultsValues = getDefaultValues();
                if ((defaultsValues != null) && !defaultsValues.isEmpty()) {
                    defaultVal = (String)defaultsValues.iterator().next();
                }
                selectView.setValue("");
            }
        } else {
            if (!bFilter) {
                Set defaultsValues = getDefaultValues();
                propertySheetModel.setValue(childName,
                    defaultsValues.iterator().next());
            }
        }
    }

    protected Set getValidValues() {
        Set values = null;
        String filter = (String)propertySheetModel.getValue(FILTER);
        String realmName = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        String subjectType = (String)propertySheetModel.getValue(
            SUBJECT_TYPE);
        PolicyModel model = (PolicyModel)getModel();

        try {
            ValidValues validValues = model.getSubjectPossibleValues(
                realmName, subjectType, filter);
            if (validValues != null) {
                int errCode = validValues.getErrorCode();
                if (errCode == ValidValues.SIZE_LIMIT_EXCEEDED) {
                    setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                        "policy.subject.sizelimit.exceeded.message");
                } else if (errCode == ValidValues.SIZE_LIMIT_EXCEEDED) {
                    setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                        "policy.subject.timelimit.exceeded.message");
                }
                values = validValues.getSearchResults();
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }

        return values;
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
        SelectSubjectTypeViewBean vb = (SelectSubjectTypeViewBean)
            getViewBean(SelectSubjectTypeViewBean.class);
        unlockPageTrailForSwapping();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Handles filter results request.
     *
     * @param event Request invocation event.
     */
    public void handleBtnFilterRequest(RequestInvocationEvent event) {
        bFilter = true;
        submitCycle = true;
        forwardTo();
    }

    protected OptionList createOptionList(Set values) {
        OptionList optList = new OptionList();

        if ((values != null) && !values.isEmpty()) {
            PolicyModel model = (PolicyModel)getModel();
            String realmName = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            String subjectType = (String)propertySheetModel.getValue(
                SUBJECT_TYPE);
            Map mapLabels = model.getDisplayNameForSubjectValues(
                realmName, subjectType, values);

            List tmp = AMFormatUtils.sortItems(values, model.getUserLocale());
            for (Iterator iter = tmp.iterator(); iter.hasNext(); ) {
                String value = (String)iter.next();
                optList.add((String)mapLabels.get(value), value);
            }
        }

        return optList;
    }

    protected abstract boolean hasValues();
    protected abstract Set getDefaultValues();
    protected abstract boolean isSubjectExclusive();
}
