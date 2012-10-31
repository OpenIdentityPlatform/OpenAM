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
 * $Id: ReferralOpViewBeanBase.java,v 1.2 2008/06/25 05:43:04 qcheng Exp $
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
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.delegation.model.DelegationConfig;
import com.sun.identity.console.policy.model.CachedPolicy;
import com.sun.identity.console.policy.model.PolicyCache;
import com.sun.identity.console.policy.model.PolicyModel;
import com.sun.identity.policy.Syntax;
import com.sun.identity.policy.ValidValues;
import com.sun.identity.policy.interfaces.Referral;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCSelect;
import com.sun.web.ui.view.html.CCStaticTextField;
import com.sun.web.ui.view.html.CCTextField;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


public abstract class ReferralOpViewBeanBase
    extends ProfileViewBeanBase
{
    protected static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    public static final String CALLING_VIEW_BEAN = 
        "ReferralOpViewBeanBaseCallingVB";
    public static final String PG_SESSION_REFERRAL_NAME = "referralName";
    public static final String PG_SESSION_REFERRAL_TYPE = "referralTypType";

    protected static final String SYNTAX = "tfSyntax";
    protected static final String REFERRAL_NAME = "tfReferralName";
    protected static final String REFERRAL_TYPE = "referralTypeName";
    protected static final String REFERRAL_TYPE_NAME = "tfReferralTypeName";
    protected static final String LBL_FILTER = "lblFilter";
    protected static final String FILTER = "tfFilter";
    protected static final String BTN_FILTER = "btnFilter";
    public static final String VALUES_TEXT_VALUE = "valuesTextValue";
    public static final String VALUES_SINGLE_CHOICE_VALUE =
        "valuesSingleChoiceValue";
    public static final String VALUES_MULTIPLE_CHOICE_VALUE =
        "valuesMultipleChoiceValue";

    protected CCPageTitleModel ptModel;
    public AMPropertySheetModel propertySheetModel;
    private boolean canModify = true;
    protected boolean submitCycle;

    /**
     * Creates a policy operation base view bean.
     *
     * @param name Name of view
     */
    public ReferralOpViewBeanBase(String name, String defaultDisplayURL) {
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

        if (!submitCycle) {
            String realmName = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            PolicyModel model = (PolicyModel)getModel();
            String referralName = (String)getPageSessionAttribute(
                PG_SESSION_REFERRAL_NAME);
            String referralType = (String)getPageSessionAttribute(
                PG_SESSION_REFERRAL_TYPE);

            Syntax syntax = model.getReferralSyntax(realmName, referralType);
            propertySheetModel.setValue(SYNTAX, 
                Integer.toString(AMDisplayType.getDisplaySyntax(syntax)));

            propertySheetModel.setValue(REFERRAL_NAME, referralName);
            propertySheetModel.setValue(REFERRAL_TYPE, referralType);
            Map map = model.getActiveReferralTypes(realmName);
            String i18nName = (String)map.get(referralType);
            propertySheetModel.setValue(REFERRAL_TYPE_NAME, i18nName);
        }
    }

    protected Referral createReferral()
        throws ModelControlException, AMConsoleException {
        Referral referral = null;
        String referralType = (String)propertySheetModel.getValue(
            REFERRAL_TYPE);
        String referralName = getReferralName();

        if (referralName != null) {
            Set values = getValues(referralType);
            if (values != null) {
                String realmName = (String)getPageSessionAttribute(
                    AMAdminConstants.CURRENT_REALM);
                PolicyModel model = (PolicyModel)getModel();
                referral = model.createReferral(
                    realmName, referralType, values);
            }
        }

        return referral;
    }

    private String getReferralName() {
        String referralName = (String)propertySheetModel.getValue(
            REFERRAL_NAME);
        referralName = referralName.trim();
        if (referralName.length() == 0) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                "policy.missing.referralName");
            referralName = null;
        }
        return referralName;
    }

    private Set getValues(String referralType)
        throws ModelControlException {
        PolicyModel model = (PolicyModel)getModel();
        String realmName = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        Syntax syntax = model.getReferralSyntax(realmName, referralType);
        Set values = getActionSchemaValues(syntax);

        if (values.isEmpty()) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                "policy.missing.referral.value");
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
            values = AMAdminUtils.toSet(
                propertySheetModel.getValues(VALUES_MULTIPLE_CHOICE_VALUE));
            break;
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

        if ((childName.indexOf(LBL_FILTER) != -1) ||
            (childName.indexOf(BTN_FILTER) != -1) ||
            (childName.indexOf(FILTER) != -1)
        ) {
            display = (syntax == AMDisplayType.SYNTAX_SINGLE_CHOICE) ||
                (syntax == AMDisplayType.SYNTAX_MULTIPLE_CHOICE);
        } else if (childName.indexOf(VALUES_TEXT_VALUE) != -1) {
            display = (syntax == AMDisplayType.SYNTAX_TEXTFIELD);
            Set values = getDefaultValues();

            if ((values != null) && !values.isEmpty()) {
                if (canModify) {
                    CCTextField tf = (CCTextField)getChild(childName);
                    tf.setValue(values.iterator().next());
                } else {
                    CCStaticTextField tf = (CCStaticTextField)getChild(
                        childName);
                    tf.setValue(values.iterator().next());
                }
            }
        } else if (childName.indexOf(VALUES_SINGLE_CHOICE_VALUE) != -1) {
            display = (syntax == AMDisplayType.SYNTAX_SINGLE_CHOICE);
            if (display) {
                Set values = getDefaultValues();

                if (canModify) {
                    CCSelect child = (CCSelect)getChild(childName);
                    setPossibleValues(child);

                    if ((values != null) && !values.isEmpty()) {
                        child.setValue(values.iterator().next());
                    }
                } else {
                    CCStaticTextField tf = (CCStaticTextField)getChild(
                        childName);
                    tf.setValue(values.iterator().next());
                }
            }
        } else if (childName.indexOf(VALUES_MULTIPLE_CHOICE_VALUE) != -1) {
            display = (syntax == AMDisplayType.SYNTAX_MULTIPLE_CHOICE);
            if (display) {
                Set values = getDefaultValues();

                if (canModify) {
                    CCSelect child = (CCSelect)getChild(childName);
                    setPossibleValues(child);
                    if ((values != null) && !values.isEmpty()) {
                        child.setValues(values.toArray());
                    }
                } else {
                    CCStaticTextField tf = (CCStaticTextField)getChild(
                        childName);
                    tf.setValue(AMAdminUtils.getString(values, ",", false));
                }
            }
        }

        return display;
    }

    private void setPossibleValues(CCSelect selectView) {
        String filter = (String)propertySheetModel.getValue(FILTER);
        String realmName = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        String referralType = (String)propertySheetModel.getValue(
            REFERRAL_TYPE);
        PolicyModel model = (PolicyModel)getModel();
        ValidValues validValues = model.getReferralPossibleValues(
            realmName, referralType, filter);

        if (validValues != null) {
            int errCode = validValues.getErrorCode();
            if (errCode == ValidValues.SIZE_LIMIT_EXCEEDED) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    "policy.referral.sizelimit.exceeded.message");
            } else if (errCode == ValidValues.SIZE_LIMIT_EXCEEDED) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    "policy.referral.timelimit.exceeded.message");
            }
            OptionList optList = createOptionList(
                validValues.getSearchResults());
            selectView.setOptions(optList);
        }
    }

    protected OptionList createOptionList(Set values) {
        OptionList optList = new OptionList();

        if ((values != null) && !values.isEmpty()) {
            PolicyModel model = (PolicyModel)getModel();
            String realmName = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            String referralType = (String)propertySheetModel.getValue(
                REFERRAL_TYPE);
            Map mapLabels = model.getDisplayNameForReferralValues(
                realmName, referralType, values);

            for (Iterator iter = values.iterator(); iter.hasNext(); ) {
                String value = (String)iter.next();
                optList.add((String)mapLabels.get(value), value);
            }
        }

        return optList;
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
        SelectReferralTypeViewBean vb = (SelectReferralTypeViewBean)
            getViewBean(SelectReferralTypeViewBean.class);
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
        submitCycle = true;
        forwardTo();
    }

    protected abstract Set getDefaultValues();
}
