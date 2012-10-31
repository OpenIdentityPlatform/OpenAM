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
 * $Id: PolicyOpViewBeanBase.java,v 1.5 2008/12/18 20:49:32 veiming Exp $
 *
 */

package com.sun.identity.console.policy;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMDisplayType;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.base.model.QueryResults;
import com.sun.identity.console.delegation.model.DelegationConfig;
import com.sun.identity.console.policy.model.CachedPolicy;
import com.sun.identity.console.policy.model.PolicyModel;
import com.sun.identity.policy.InvalidNameException;
import com.sun.identity.policy.NameNotFoundException;
import com.sun.identity.policy.Policy;
import com.sun.identity.policy.Rule;
import com.sun.identity.policy.Syntax;
import com.sun.identity.policy.interfaces.Condition;
import com.sun.identity.policy.interfaces.Referral;
import com.sun.identity.policy.interfaces.ResponseProvider;
import com.sun.identity.policy.interfaces.Subject;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCButton;
import com.sun.web.ui.view.table.CCActionTable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


public abstract class PolicyOpViewBeanBase
    extends ProfileViewBeanBase
{
    protected static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    public static final String WIZARD = "wizard";

    private static final String TBL_RULES = "tblRules";
    private static final String TBL_RULES_COL_NAME = "tblRulesColName";
    private static final String TBL_RULES_COL_TYPE = "tblRulesColType";
    private static final String TBL_RULES_DATA_NAME = "ruleName";
    private static final String TBL_RULES_DATA_TYPE = "ruleType";
    private static final String TBL_RULES_ACTION_HREF = "tblRulesEditLink";

    private static final String TBL_REFERRALS = "tblReferrals";
    private static final String TBL_REFERRALS_COL_NAME = "tblReferralsColName";
    private static final String TBL_REFERRALS_COL_TYPE = "tblReferralsColType";
    private static final String TBL_REFERRALS_DATA_NAME = "referralName";
    private static final String TBL_REFERRALS_DATA_TYPE = "referralType";
    private static final String TBL_REFERRALS_ACTION_HREF =
        "tblReferralsEditLink";

    private static final String TBL_SUBJECTS = "tblSubjects";
    private static final String TBL_SUBJECTS_COL_NAME = "tblSubjectsColName";
    private static final String TBL_SUBJECTS_COL_TYPE = "tblSubjectsColType";
    private static final String TBL_SUBJECTS_COL_ACTION =
        "tblSubjectsColAction";
    private static final String TBL_SUBJECTS_DATA_NAME = "subjectName";
    private static final String TBL_SUBJECTS_DATA_TYPE = "subjectType";
    private static final String TBL_SUBJECTS_ACTION_HREF =
        "tblSubjectsEditLink";

    private static final String TBL_RESPONSE_ATTRIBUTES =
        "tblResponseProviders";
    private static final String TBL_RESPONSE_ATTRIBUTES_COL_NAME =
        "tblResponseProvidersColName";
    private static final String TBL_RESPONSE_ATTRIBUTES_COL_TYPE =
        "tblResponseProvidersColType";
    private static final String TBL_RESPONSE_ATTRIBUTES_COL_ACTION =
        "tblResponseProvidersColAction";
    private static final String TBL_RESPONSE_ATTRIBUTES_DATA_NAME =
        "responseProviderName";
    private static final String TBL_RESPONSE_ATTRIBUTES_DATA_TYPE =
        "responseProviderType";
    private static final String TBL_RESPONSE_ATTRIBUTES_ACTION_HREF =
        "tblResponseProvidersEditLink";

    private static final String TBL_CONDITIONS = "tblConditions";
    private static final String TBL_CONDITIONS_COL_NAME =
        "tblConditionsColName";
    private static final String TBL_CONDITIONS_COL_TYPE =
        "tblConditionsColType";
    private static final String TBL_CONDITIONS_DATA_NAME = "conditionName";
    private static final String TBL_CONDITIONS_DATA_TYPE = "conditionType";
    private static final String TBL_CONDITIONS_ACTION_HREF =
        "tblConditionsEditLink";

    private static final String ATTR_DESCRIPTION = "tfDescription";
    private static final String ATTR_ISACTIVE = "cbIsActive";
    private static Map PROPERTY_MAPPING = new HashMap(4);

    static {
        PROPERTY_MAPPING.put(PolicyModel.TF_NAME, Collections.EMPTY_SET);
        PROPERTY_MAPPING.put(ATTR_DESCRIPTION, Collections.EMPTY_SET);
        PROPERTY_MAPPING.put(ATTR_ISACTIVE, Collections.EMPTY_SET);
    }

    protected CCPageTitleModel ptModel;
    private CCActionTableModel tblRulesModel;
    private CCActionTableModel tblReferralsModel;
    private CCActionTableModel tblResponseProvidersModel;
    private CCActionTableModel tblSubjectsModel;
    private CCActionTableModel tblConditionsModel;

    private AMPropertySheetModel propertySheetModel;
    private boolean bPopulateTables;

    /**
     * Creates a policy operation base view bean.
     *
     * @param name Name of view
     */
    public PolicyOpViewBeanBase(String name, String defaultDisplayURL) {
        super(name, defaultDisplayURL);
    }

    protected void initialize() {
        if (!initialized) {
            String realmName = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);

            if (realmName != null) {
                super.initialize();
                initialized = true;
                createPageTitleModel();
                createPropertyModel(realmName);
                createTableModels();
                registerChildren();
            }
        }
    }

    protected void createPropertyModel(String realmName) {
        DelegationConfig dConfig = DelegationConfig.getInstance();
        boolean canModify = dConfig.hasPermission(realmName, null,
            AMAdminConstants.PERMISSION_MODIFY, getModel(),
            getClass().getName());

        propertySheetModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(
                getPropertyXMLFileName(!canModify)));
        propertySheetModel.clear();
    }

    protected void createRuleTableModels() {
        tblRulesModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/tblPMRules.xml"));
        tblRulesModel.setTitleLabel("label.items");
        tblRulesModel.setActionValue("tblRulesButtonAdd", "button.new");
        tblRulesModel.setActionValue("tblRulesButtonDelete", "button.delete");
        tblRulesModel.setActionValue(TBL_RULES_COL_NAME,
            "policy.rules.table.column.name");
        tblRulesModel.setActionValue(TBL_RULES_COL_TYPE,
            "policy.rules.table.column.type");
        propertySheetModel.setModel(TBL_RULES, tblRulesModel);
    }

    protected void createReferralTableModels() {
        tblReferralsModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/tblPMReferrals.xml"));
        tblReferralsModel.setTitleLabel("label.items");
        tblReferralsModel.setActionValue("tblReferralsButtonAdd", "button.new");
        tblReferralsModel.setActionValue("tblReferralsButtonDelete",
            "button.delete");
        tblReferralsModel.setActionValue(TBL_REFERRALS_COL_NAME,
            "policy.referrals.table.column.name");
        tblReferralsModel.setActionValue(TBL_REFERRALS_COL_TYPE,
            "policy.referrals.table.column.type");
        propertySheetModel.setModel(TBL_REFERRALS, tblReferralsModel);
    }

    protected void createSubjectTableModels() {
        tblSubjectsModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/tblPMSubjects.xml"));
        tblSubjectsModel.setTitleLabel("label.items");
        tblSubjectsModel.setActionValue("tblSubjectsButtonAdd", "button.new");
        tblSubjectsModel.setActionValue("tblSubjectsButtonDelete",
            "button.delete");
        tblSubjectsModel.setActionValue(TBL_SUBJECTS_COL_NAME,
            "policy.subjects.table.column.name");
        tblSubjectsModel.setActionValue(TBL_SUBJECTS_COL_TYPE,
            "policy.subjects.table.column.type");
        tblSubjectsModel.setActionValue(TBL_SUBJECTS_COL_ACTION,
            "policy.subjects.table.column.action");
        propertySheetModel.setModel(TBL_SUBJECTS, tblSubjectsModel);
    }

    protected void createResponseProviderTableModels() {
        tblResponseProvidersModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/tblPMResponseProviders.xml"));
        tblResponseProvidersModel.setTitleLabel("label.items");
        tblResponseProvidersModel.setActionValue(
            "tblResponseProvidersButtonAdd", "button.new");
        tblResponseProvidersModel.setActionValue(
            "tblResponseProvidersButtonDelete", "button.delete");
        tblResponseProvidersModel.setActionValue(
            TBL_RESPONSE_ATTRIBUTES_COL_NAME,
            "policy.responseproviders.table.column.name");
        tblResponseProvidersModel.setActionValue(
            TBL_RESPONSE_ATTRIBUTES_COL_TYPE,
            "policy.responseproviders.table.column.type");
        tblResponseProvidersModel.setActionValue(
            TBL_RESPONSE_ATTRIBUTES_COL_ACTION,
            "policy.responseproviders.table.column.action");
        propertySheetModel.setModel(TBL_RESPONSE_ATTRIBUTES,
            tblResponseProvidersModel);
    }

    protected void createConditionTableModels() {
        tblConditionsModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/tblPMConditions.xml"));
        tblConditionsModel.setTitleLabel("label.items");
        tblConditionsModel.setActionValue("tblConditionsButtonAdd",
            "button.new");
        tblConditionsModel.setActionValue("tblConditionsButtonDelete",
            "button.delete");
        tblConditionsModel.setActionValue(TBL_CONDITIONS_COL_NAME,
            "policy.conditions.table.column.name");
        tblConditionsModel.setActionValue(TBL_CONDITIONS_COL_TYPE,
            "policy.conditions.table.column.type");
        propertySheetModel.setModel(TBL_CONDITIONS, tblConditionsModel);
    }

    protected void registerChildren() {
        super.registerChildren();
        registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
        ptModel.registerChildren(this);
        propertySheetModel.registerChildren(this);
    }

    protected View createChild(String name) {
        View view = null;

        if (!bPopulateTables) {
            populateTables();
            bPopulateTables = true;
        }

        if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
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

        try {
            populateRulesTable();
            CachedPolicy cachedPolicy = getCachedPolicy();
            Policy policy = cachedPolicy.getPolicy();
        
            String realmName = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            PolicyModel model = (PolicyModel)getModel();
            CCButton btn = (CCButton)getChild("tblRulesButtonAdd");
            if (!model.canCreateRule(policy, realmName)) {
                btn.setDisabled(true);
            }

            btn = (CCButton)getChild("tblRulesButtonDelete");
            btn.setDisabled(true);
            
            Map serviceTypes = model.getServiceTypeNames();
            if ((serviceTypes == null) || serviceTypes.isEmpty()) {
                ((CCButton)getChild("tblRulesButtonDelete")).setDisabled(true);
                setDisplayFieldValue("tblRulesNote", 
                    model.getLocalizedString("policy.no.service.types"));
            } else {
                setDisplayFieldValue("tblRulesNote", "");
            }

            if (!isInlineAlertMessageSet() &&
                cachedPolicy.isPolicyModified() && isProfilePage()
            ) {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "policy.unsaved.message");
            }
        } catch (AMConsoleException e) {
            CCButton btn = (CCButton)getChild("tblRulesButtonAdd");
            btn.setDisabled(true);
            btn = (CCButton)getChild("tblRulesButtonDelete");
            btn.setDisabled(true);
        }
    }

    protected void populateReferralsTable()
        throws AMConsoleException
    {
        tblReferralsModel.clearAll();

        CachedPolicy cachedPolicy = getCachedPolicy();
        Policy policy = cachedPolicy.getPolicy();
        Set referralsNames = policy.getReferralNames();
   
        if ((referralsNames != null) && !referralsNames.isEmpty()) {
            PolicyModel model = (PolicyModel)getModel();
            String realmName = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            Map localizedRflTypeNames = model.getActiveReferralTypes(realmName);
            boolean firstEntry = true;
    
            for (Iterator iter = referralsNames.iterator(); iter.hasNext(); ) {
                if (firstEntry) {
                    firstEntry = false;
                } else {
                    tblReferralsModel.appendRow();
                }

                try {
                    String name = (String)iter.next();
                    Referral referral = policy.getReferral(name);
                    tblReferralsModel.setValue(TBL_REFERRALS_DATA_NAME, name);
    
                    String rflTypeName = referral.getReferralTypeName();
                    String displayName = (String)localizedRflTypeNames.get(
                        rflTypeName);
                    if (displayName == null) {
                        displayName = rflTypeName;
                    }
       
                    tblReferralsModel.setValue(TBL_REFERRALS_DATA_TYPE,
                        displayName);
                    tblReferralsModel.setValue(TBL_REFERRALS_ACTION_HREF, 
                        stringToHex(name));
                } catch (NameNotFoundException e) {
                    debug.warning(
                        "PolicyOpViewBeanBase.populateReferralsTable", e);
                }
            }
        }
    }

    protected void populateSubjectsTable()  
        throws AMConsoleException
    {
        tblSubjectsModel.clearAll();

        CachedPolicy cachedPolicy = getCachedPolicy();
        Policy policy = cachedPolicy.getPolicy();
        Set subjectsNames = policy.getSubjectNames();

        if ((subjectsNames != null) && !subjectsNames.isEmpty()) {
            PolicyModel model = (PolicyModel)getModel();
            String realmName = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
                
            QueryResults queryResults = model.getActiveSubjectTypes(realmName);
            Map localizedSbjTypeNames = (Map)queryResults.getResults();
            boolean firstEntry = true;

            for (Iterator iter = subjectsNames.iterator(); iter.hasNext(); ) {
                if (firstEntry) {
                    firstEntry = false;
                } else {
                    tblSubjectsModel.appendRow();
                }

                try {
                    String name = (String)iter.next();
                    Subject subject = policy.getSubject(name);
                    tblSubjectsModel.setValue(TBL_SUBJECTS_DATA_NAME, name);

                    String sbjTypeName = model.getSubjectTypeName(
                        realmName, subject);
                    String displayName = (String)localizedSbjTypeNames.get(
                        sbjTypeName);
                    if (displayName == null) {
                        displayName = sbjTypeName;
                    }

                    tblSubjectsModel.setValue(TBL_SUBJECTS_DATA_TYPE,
                        displayName);
                    tblSubjectsModel.setValue(TBL_SUBJECTS_ACTION_HREF, 
                        stringToHex(name));
                } catch (NameNotFoundException e) {
                    debug.warning(
                        "PolicyOpViewBeanBase.populateSubjectsTable", e);
                }
            }
        }
    }

    protected void populateResponseProvidersTable()
        throws AMConsoleException
    {
        tblResponseProvidersModel.clearAll();

        CachedPolicy cachedPolicy = getCachedPolicy();
        Policy policy = cachedPolicy.getPolicy();
        Set responseProviderNames = policy.getResponseProviderNames();

        if ((responseProviderNames != null) &&
            !responseProviderNames.isEmpty()
           ) {
            PolicyModel model = (PolicyModel)getModel();
            String realmName = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            Map localizedRespAttrTypeNames =
                model.getActiveResponseProviderTypes(realmName);
            boolean firstEntry = true;

            for (Iterator i = responseProviderNames.iterator(); i.hasNext(); ) {
                if (firstEntry) {
                    firstEntry = false;
                } else {
                    tblResponseProvidersModel.appendRow();
                }

                try {
                    String name = (String)i.next();
                    ResponseProvider provider =
                        policy.getResponseProvider(name);
                    tblResponseProvidersModel.setValue(
                        TBL_RESPONSE_ATTRIBUTES_DATA_NAME, name);

                    String respAttrTypeName =
                        model.getResponseProviderTypeName(realmName, provider);
                    String displayName = (String)localizedRespAttrTypeNames.get(
                        respAttrTypeName);
                    if (displayName == null) {
                        displayName = respAttrTypeName;
                    }

                    tblResponseProvidersModel.setValue(
                        TBL_RESPONSE_ATTRIBUTES_DATA_TYPE, displayName);
                    tblResponseProvidersModel.setValue(
                        TBL_RESPONSE_ATTRIBUTES_ACTION_HREF, 
                        stringToHex(name));
                } catch (NameNotFoundException e) {
                    debug.warning(
                        "PolicyOpViewBeanBase.populateResponseProvidersTable",
                        e);
                }
            }
        }
    }

    protected void populateConditionsTable()
        throws AMConsoleException
    {
        tblConditionsModel.clearAll();

        CachedPolicy cachedPolicy = getCachedPolicy();
        Policy policy = cachedPolicy.getPolicy();
        Set conditionNames = policy.getConditionNames();

        if ((conditionNames != null) && !conditionNames.isEmpty()) {
            PolicyModel model = (PolicyModel)getModel();
            String realmName = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            Map localizedCndTypeNames = model.getActiveConditionTypes(
                realmName);
            boolean firstEntry = true;

            for (Iterator iter = conditionNames.iterator(); iter.hasNext(); ) {
                if (firstEntry) {
                    firstEntry = false;
                } else {
                    tblConditionsModel.appendRow();
                }

                try {
                    String name = (String)iter.next();
                    Condition condition = policy.getCondition(name);
                    tblConditionsModel.setValue(TBL_CONDITIONS_DATA_NAME, name);

                    String cndTypeName = model.getConditionTypeName(
                        realmName, condition);
                    String displayName = (String)localizedCndTypeNames.get(
                        cndTypeName);
                    if (displayName == null) {
                        displayName = cndTypeName;
                    }

                    tblConditionsModel.setValue(TBL_CONDITIONS_DATA_TYPE,
                        displayName);
                    tblConditionsModel.setValue(
                        TBL_CONDITIONS_ACTION_HREF, stringToHex(name));
                } catch (NameNotFoundException e) {
                    debug.warning(
                        "PolicyOpViewBeanBase.populateConditionsTable", e);
                }
            }
        }
    }

    protected void populateRulesTable()
        throws AMConsoleException
    {
        tblRulesModel.clearAll();

        CachedPolicy cachedPolicy = getCachedPolicy();
        Policy policy = cachedPolicy.getPolicy();
        Set ruleNames = policy.getRuleNames();

        if ((ruleNames != null) && !ruleNames.isEmpty()) {
            PolicyModel model = (PolicyModel)getModel();
            Map localizedSvcTypeNames = model.getServiceTypeNames();
            boolean firstEntry = true;

            for (Iterator iter = ruleNames.iterator(); iter.hasNext(); ) {
                if (firstEntry) {
                    firstEntry = false;
                } else {
                    tblRulesModel.appendRow();
                }

                try {
                    String name = (String)iter.next();
                    Rule rule = policy.getRule(name);
                    tblRulesModel.setValue(TBL_RULES_DATA_NAME, name);
                    tblRulesModel.setValue(TBL_RULES_DATA_TYPE,
                        localizedSvcTypeNames.get(rule.getServiceTypeName()));
                    tblRulesModel.setValue(TBL_RULES_ACTION_HREF, 
                        stringToHex(name));
                } catch (NameNotFoundException e) {
                    debug.warning(
                        "PolicyOpViewBeanBase.populateRulesTable", e);
                }
            }
        }
    }

    protected void populateAttributes()
        throws AMConsoleException
    {
        CachedPolicy cachedPolicy = getCachedPolicy();
        Policy policy = cachedPolicy.getPolicy();
        propertySheetModel.setValue(PolicyModel.TF_NAME, policy.getName());
        propertySheetModel.setValue(ATTR_DESCRIPTION, policy.getDescription());
        propertySheetModel.setValue(ATTR_ISACTIVE,
            policy.isActive() ? "true" : "false");
    }

    protected void forwardToPolicyViewBean() {
        PolicyViewBean vb = (PolicyViewBean)getViewBean(PolicyViewBean.class);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Handles add new rule request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblRulesButtonAddRequest(RequestInvocationEvent event)
        throws ModelControlException {
        PolicyModel model = (PolicyModel)getModel();

        try {
            if (!reconstructPolicy()) {
                setPageSessionAttribute(
                    SelectServiceTypeViewBean.CALLING_VIEW_BEAN,
                    getClass().getName());
                SelectServiceTypeViewBean vb = (SelectServiceTypeViewBean)
                    getViewBean(SelectServiceTypeViewBean.class);
                unlockPageTrail();
                passPgSessionMap(vb);
                vb.forwardTo(getRequestContext());
            } else {
                forwardTo();
            }
        } catch (AMConsoleException e) {
            debug.warning(
                "PolicyOpViewBeanBase.handleTblRulesButtonAddRequest", e);
            redirectToStartURL();
        }
    }

    /**
     * Handles delete rule request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblRulesButtonDeleteRequest(RequestInvocationEvent event)
        throws ModelControlException {
        CCActionTable table = (CCActionTable)getChild(TBL_RULES);
        table.restoreStateData();

        try {
            CachedPolicy cachedPolicy = getCachedPolicy();
            Policy policy = cachedPolicy.getPolicy();
            Integer[] selected = tblRulesModel.getSelectedRows();

            for (int i = 0; i < selected.length; i++) {
                tblRulesModel.setRowIndex(selected[i].intValue());
                String ruleName = (String)tblRulesModel.getValue(
                    TBL_RULES_DATA_NAME);
                policy.removeRule(ruleName);
            }

            cachedPolicy.setPolicyModified(true);
            forwardTo();
        } catch (AMConsoleException e) {
            debug.warning(
                "PolicyOpViewBeanBase.handleTblRulesButtonDeleteRequest", e);
            redirectToStartURL();
        }
    }

    /**
     * Handles edit rule request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblRulesEditLinkRequest(RequestInvocationEvent event)
        throws ModelControlException {
        PolicyModel model = (PolicyModel)getModel();

        String name = hexToString(
            (String)getDisplayFieldValue(TBL_RULES_ACTION_HREF));
        setPageSessionAttribute(RuleEditViewBean.CALLING_VIEW_BEAN,
            getClass().getName());
        setPageSessionAttribute(RuleEditViewBean.EDIT_RULE_NAME, name);

        try {
            CachedPolicy cachedPolicy = getCachedPolicy();
            Policy policy = cachedPolicy.getPolicy();
            Rule rule = policy.getRule(name);
            RuleEditViewBean vb = null;
            String resName = rule.getResourceName();
            if ((resName == null) || (resName.length() == 0)) {
                vb = (RuleNoResourceEditViewBean)getViewBean(
                    RuleNoResourceEditViewBean.class);
                setPageSessionAttribute(RuleOpViewBeanBase.WITH_RESOURCE,
                    Boolean.FALSE);
            } else {
                String realmName = (String)getPageSessionAttribute(
                   AMAdminConstants.CURRENT_REALM);
                String serviceType = rule.getServiceTypeName();

                if (model.canCreateNewResource(realmName, serviceType)) {
                    vb = (RuleEditViewBean)getViewBean(
                        RuleEditViewBean.class);
                } else {
                    vb = (RuleEditViewBean)getViewBean(
                        RuleWithPrefixEditViewBean.class);
                }
                setPageSessionAttribute(RuleOpViewBeanBase.WITH_RESOURCE,
                    Boolean.TRUE);
            }

            unlockPageTrail();
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } catch (NameNotFoundException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                getModel().getErrorString(e));
            forwardTo();
        } catch (AMConsoleException e) {
            debug.warning(
                "PolicyOpViewBeanBase.handleTblRulesEditLinkRequest", e);
            redirectToStartURL();
        }
    }

    protected boolean reconstructPolicy()
        throws ModelControlException, AMConsoleException {
        PolicyModel model = (PolicyModel)getModel();
        return reconstructPolicyGeneral(model);
    }

    private boolean reconstructPolicyGeneral(PolicyModel model)
        throws AMConsoleException
    {
        boolean bError = false;
        String policyName = (String)propertySheetModel.getValue(
            PolicyModel.TF_NAME);
        policyName = policyName.trim();
        String description = (String)propertySheetModel.getValue(
            ATTR_DESCRIPTION);
        description = description.trim();
        String isActive = (String)propertySheetModel.getValue(
            ATTR_ISACTIVE);
        boolean active = (isActive != null) && isActive.equals("true");

        if (policyName.length() == 0) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                "policy.missing.policyName");
            bError = true;   
            return bError;
        }
        
        CachedPolicy cachedPolicy = getCachedPolicy();
        Policy policy = cachedPolicy.getPolicy();
        try {
            policy.setDescription(description);
            policy.setActive(active);
            policy.setName(policyName);
        } catch (InvalidNameException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                model.getErrorString(e));
            bError = true;
        }

        return bError;
    }

    public void handleTblReferralsButtonAddRequest(RequestInvocationEvent event)
        throws ModelControlException {
        String curRealm = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        PolicyModel model = (PolicyModel)getModel();

        try {
            if (!reconstructPolicy()) {
                unlockPageTrail();
                Map activeReferralTypes = model.getActiveReferralTypes(
                    curRealm);

                if (activeReferralTypes.size() == 1) {
                    String referralType = 
                        (String)activeReferralTypes.keySet().iterator().next();
                    String viewBeanURL = model.getReferralViewBeanURL(
                        curRealm, referralType);
                    removePageSessionAttribute(WIZARD);
                    setPageSessionAttribute(
                        ReferralOpViewBeanBase.CALLING_VIEW_BEAN,
                        getClass().getName());

                    if ((viewBeanURL != null) &&
                        (viewBeanURL.trim().length() > 0)
                    ){
                        forwardToReferralURL(viewBeanURL, "", referralType,
                            curRealm, "add");
                    } else {
                        forwardToReferralAddViewBean(referralType);
                    }
                } else {
                    setPageSessionAttribute(
                        SelectReferralTypeViewBean.CALLING_VIEW_BEAN,
                        getClass().getName());
                    SelectReferralTypeViewBean vb = (SelectReferralTypeViewBean)
                        getViewBean(SelectReferralTypeViewBean.class);
                    setPageSessionAttribute(WIZARD, "true");
                    passPgSessionMap(vb);
                    vb.forwardTo(getRequestContext());
                }
            } else {
                forwardTo();
            }
        } catch (AMConsoleException e) {
            debug.warning(
                "PolicyOpViewBeanBase.handleTblReferralsButtonAddRequest", e);
            redirectToStartURL();
        }
    }

    private void forwardToReferralAddViewBean(String referralType) {
        ReferralAddViewBean vb = (ReferralAddViewBean)getViewBean(
            ReferralAddViewBean.class);
        setPageSessionAttribute(ReferralAddViewBean.CALLING_VIEW_BEAN,
            getClass().getName());
        setPageSessionAttribute(ReferralOpViewBeanBase.PG_SESSION_REFERRAL_TYPE,
            referralType);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    private void forwardToReferralURL(
        String url,
        String referralName,
        String referralType,
        String realmName,
        String op
    ) {
        ReferralProxyViewBean vb = (ReferralProxyViewBean)getViewBean(
            ReferralProxyViewBean.class);
        passPgSessionMap(vb);
        vb.setURL(url, op);
        vb.setDisplayFieldValue(ReferralProxyViewBean.TF_REFERRAL_TYPE_NAME,
            referralType);
        vb.setDisplayFieldValue(ReferralProxyViewBean.TF_REFERRAL_NAME,
            referralName);

        if ((realmName == null) || (realmName.trim().length() == 0)) {
            realmName = AMModelBase.getStartDN(
                getRequestContext().getRequest());
        }

        vb.setDisplayFieldValue(ReferralProxyViewBean.TF_REALM_NAME, realmName);
        vb.setDisplayFieldValue(ReferralProxyViewBean.TF_CACHED_ID,
            (String)getPageSessionAttribute(
                ProfileViewBeanBase.PG_SESSION_POLICY_CACHE_ID));
        vb.setDisplayFieldValue(ReferralProxyViewBean.TF_OP, op);
        vb.forwardTo(getRequestContext());
    }

    public void handleTblReferralsButtonDeleteRequest(
        RequestInvocationEvent event
    ) throws ModelControlException {
        CCActionTable table = (CCActionTable)getChild(TBL_REFERRALS);
        table.restoreStateData();

        try {
            CachedPolicy cachedPolicy = getCachedPolicy();
            Policy policy = cachedPolicy.getPolicy();
            Integer[] selected = tblReferralsModel.getSelectedRows();

            for (int i = 0; i < selected.length; i++) {
                tblReferralsModel.setRowIndex(selected[i].intValue());
                String name = (String)tblReferralsModel.getValue(
                    TBL_REFERRALS_DATA_NAME);
                policy.removeReferral(name);
            }

            cachedPolicy.setPolicyModified(true);
            populateReferralsTable();
            forwardTo();
        } catch (AMConsoleException e) {
            debug.warning(
                "PolicyOpViewBeanBase.handleTblReferralsButtonDeleteRequest",
                e);
            redirectToStartURL();
        }
    }

    public void handleTblReferralsEditLinkRequest(RequestInvocationEvent event)
        throws ModelControlException {
        PolicyModel model = (PolicyModel)getModel();

        String name = hexToString(
            (String)getDisplayFieldValue(TBL_REFERRALS_ACTION_HREF));
        setPageSessionAttribute(ReferralEditViewBean.CALLING_VIEW_BEAN,
            getClass().getName());
        setPageSessionAttribute(ReferralEditViewBean.EDIT_REFERRAL_NAME, name);

        try {
            CachedPolicy cachedPolicy = getCachedPolicy();
            Policy policy = cachedPolicy.getPolicy();
            Referral referral = policy.getReferral(name);
            String realmName = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            String referralType = referral.getReferralTypeName();
            String viewBeanURL = model.getReferralViewBeanURL(
                realmName, referralType);
            unlockPageTrail();
    
            if ((viewBeanURL != null) && (viewBeanURL.trim().length() > 0)){
                forwardToReferralURL(viewBeanURL, name, referralType,
                    realmName, "edit");
            } else {
                forwardToReferralEditViewBean(
                    model, realmName, name, referralType);
            }
        } catch (NameNotFoundException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
            getModel().getErrorString(e));
            forwardTo();
        } catch (AMConsoleException e) {
            debug.warning(
                "PolicyOpViewBeanBase.handleTblReferralsEditLinkRequest",
                e);
            redirectToStartURL();
        }
    }

    private void forwardToReferralEditViewBean(
        PolicyModel model,
        String realmName,
        String name,
        String referralType
    ) {
        ReferralEditViewBean vb = (ReferralEditViewBean)getViewBean(
            ReferralEditViewBean.class);
        setPageSessionAttribute(ReferralEditViewBean.CALLING_VIEW_BEAN,
            getClass().getName());
        setPageSessionAttribute(ReferralOpViewBeanBase.PG_SESSION_REFERRAL_NAME,
            name);
        setPageSessionAttribute(ReferralOpViewBeanBase.PG_SESSION_REFERRAL_TYPE,
            referralType);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    public void handleTblSubjectsButtonAddRequest(RequestInvocationEvent event)
        throws ModelControlException {
        String curRealm = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        PolicyModel model = (PolicyModel)getModel();

        try {
            if (!reconstructPolicy()) {
                QueryResults queryResults = 
                    model.getActiveSubjectTypes(curRealm);
                Map activeSubjectTypes = (Map)queryResults.getResults();
                unlockPageTrail();

                if (activeSubjectTypes.size() == 1) {
                    String subjectType =
                        (String)activeSubjectTypes.keySet().iterator().next();
                    String viewBeanURL = model.getSubjectViewBeanURL(
                        curRealm, subjectType);
                    removePageSessionAttribute(WIZARD);
                    setPageSessionAttribute(
                        SubjectOpViewBeanBase.CALLING_VIEW_BEAN,
                        getClass().getName());

                    if ((viewBeanURL != null) &&
                        (viewBeanURL.trim().length() > 0)
                    ){
                        forwardToSubjectURL(viewBeanURL, "", subjectType,
                            curRealm, "add");
                    } else {
                        forwardToSubjectAddViewBean(curRealm, subjectType);
                    }
                } else {
                    setPageSessionAttribute(
                        SelectSubjectTypeViewBean.CALLING_VIEW_BEAN,
                    getClass().getName());
                    SelectSubjectTypeViewBean vb = (SelectSubjectTypeViewBean)
                        getViewBean(SelectSubjectTypeViewBean.class);
                    setPageSessionAttribute(WIZARD, "true");
                    passPgSessionMap(vb);
                    vb.forwardTo(getRequestContext());
                }
            } else {
                forwardTo();
            }
        } catch (AMConsoleException e) {
            debug.warning(
                "PolicyOpViewBeanBase.handleTblSubjectsButtonAddRequest", e);
            redirectToStartURL();
        }
    }

    private void forwardToSubjectURL(
        String url,
        String subjectName,
        String subjectType,
        String realmName,
        String op
    ) {
        SubjectProxyViewBean vb = (SubjectProxyViewBean)getViewBean(
            SubjectProxyViewBean.class);
        passPgSessionMap(vb);
        vb.setURL(url, op);
        vb.setDisplayFieldValue(SubjectProxyViewBean.TF_SUBJECT_TYPE_NAME,
            subjectType);
        vb.setDisplayFieldValue(SubjectProxyViewBean.TF_SUBJECT_NAME,
            subjectName);

        if ((realmName == null) || (realmName.trim().length() == 0)) {
            realmName = AMModelBase.getStartDN(
                getRequestContext().getRequest());
        }

        vb.setDisplayFieldValue(SubjectProxyViewBean.TF_REALM_NAME, realmName);
        vb.setDisplayFieldValue(SubjectProxyViewBean.TF_CACHED_ID,
            (String)getPageSessionAttribute(
                ProfileViewBeanBase.PG_SESSION_POLICY_CACHE_ID));
        vb.setDisplayFieldValue(SubjectProxyViewBean.TF_OP, op);
        vb.forwardTo(getRequestContext());
    }

    private void forwardToSubjectAddViewBean(
        String curRealm,
        String subjectType
    ) {
        SubjectAddViewBean vb = subjectHasValueSyntax(curRealm, subjectType) ?
            (SubjectAddViewBean)getViewBean(SubjectAddViewBean.class) :
            (SubjectAddViewBean)getViewBean(SubjectNoneAddViewBean.class);

        setPageSessionAttribute(SubjectAddViewBean.CALLING_VIEW_BEAN,
            getClass().getName());
        setPageSessionAttribute(SubjectOpViewBeanBase.PG_SESSION_SUBJECT_TYPE,
            subjectType);

        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    public void handleTblSubjectsEditLinkRequest(RequestInvocationEvent event)
        throws ModelControlException {
        PolicyModel model = (PolicyModel)getModel();

        String name = hexToString((String)getDisplayFieldValue(
            TBL_SUBJECTS_ACTION_HREF));
        setPageSessionAttribute(SubjectEditViewBean.CALLING_VIEW_BEAN,
            getClass().getName());
        setPageSessionAttribute(SubjectEditViewBean.EDIT_SUBJECT_NAME, name);

        try {
            CachedPolicy cachedPolicy = getCachedPolicy();
            Policy policy = cachedPolicy.getPolicy();
            Subject subject = policy.getSubject(name);
            String realmName = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            String subjectType = model.getSubjectTypeName(
                realmName, subject);
            String viewBeanURL = model.getSubjectViewBeanURL(
                realmName, subjectType);
            unlockPageTrail();

            if ((viewBeanURL != null) && (viewBeanURL.trim().length() > 0)){
                forwardToSubjectURL(viewBeanURL, name, subjectType,
                    realmName, "edit");
            } else {
                forwardToSubjectEditViewBean(
                    model, realmName, name, subjectType);
            }
        } catch (NameNotFoundException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                model.getErrorString(e));
            forwardTo();
        } catch (AMConsoleException e) {
            debug.warning(
                "PolicyOpViewBeanBase.handleTblSubjectsEditLinkRequest",
                e);
            redirectToStartURL();
        }
    }

    private void forwardToSubjectEditViewBean(
        PolicyModel model,
        String realmName,
        String name,
        String subjectType
    ) {
        SubjectEditViewBean vb =
            subjectHasValueSyntax(realmName, subjectType) ?
            (SubjectEditViewBean)getViewBean(SubjectEditViewBean.class) :
            (SubjectEditViewBean)getViewBean(SubjectNoneEditViewBean.class);
        setPageSessionAttribute(SubjectEditViewBean.CALLING_VIEW_BEAN,
            getClass().getName());
        setPageSessionAttribute(SubjectOpViewBeanBase.PG_SESSION_SUBJECT_NAME,
            name);
        setPageSessionAttribute(SubjectOpViewBeanBase.PG_SESSION_SUBJECT_TYPE,
            subjectType);

        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    private boolean subjectHasValueSyntax(String realmName, String subjectType){
        PolicyModel model = (PolicyModel)getModel();
        Syntax syntax = model.getSubjectSyntax(realmName, subjectType);
        int displaySyntax = AMDisplayType.getDisplaySyntax(syntax);
        return (displaySyntax == AMDisplayType.SYNTAX_TEXT) ||
            (displaySyntax == AMDisplayType.SYNTAX_SINGLE_CHOICE) ||
            (displaySyntax == AMDisplayType.SYNTAX_MULTIPLE_CHOICE);
    }

    /**
     * Handles delete subject request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblSubjectsButtonDeleteRequest(
        RequestInvocationEvent event
    ) throws ModelControlException {
        CCActionTable table = (CCActionTable)getChild(TBL_SUBJECTS);
        table.restoreStateData();

        try {
            CachedPolicy cachedPolicy = getCachedPolicy();
            Policy policy = cachedPolicy.getPolicy();
            Integer[] selected = tblSubjectsModel.getSelectedRows();

            for (int i = 0; i < selected.length; i++) {
                tblSubjectsModel.setRowIndex(selected[i].intValue());
                String subjectName = (String)tblSubjectsModel.getValue(
                    TBL_SUBJECTS_DATA_NAME);
                policy.removeSubject(subjectName);
            }
            cachedPolicy.setPolicyModified(true);

            populateSubjectsTable();
            forwardTo();
        } catch (AMConsoleException e) {
            debug.warning(
                "PolicyOpViewBeanBase.handleTblSubjectsButtonDeleteRequest",
                e);
            redirectToStartURL();
        }
    }

    public void handleTblResponseProvidersButtonAddRequest(
        RequestInvocationEvent event)
        throws ModelControlException {
        String curRealm = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        PolicyModel model = (PolicyModel)getModel();

        try {
            if (!reconstructPolicy()) {
                Map activeTypes = model.getActiveResponseProviderTypes(
                    curRealm);
                unlockPageTrail();

                if (activeTypes.size() == 1) {
                    String respAttrType =
                        (String)activeTypes.keySet().iterator().next();
                    String viewBeanURL = model.getResponseProviderViewBeanURL(
                        curRealm, respAttrType);
                    removePageSessionAttribute(WIZARD);
                    setPageSessionAttribute(
                        ResponseProviderOpViewBeanBase.CALLING_VIEW_BEAN,
                        getClass().getName());

                    if ((viewBeanURL != null) &&
                        (viewBeanURL.trim().length() > 0)
                    ){
                        forwardToResponseProviderURL(
                            viewBeanURL, "", respAttrType, curRealm, "add");
                    } else {
                        forwardToResponseProviderAddViewBean(
                            curRealm, respAttrType);
                    }
                } else {
                    setPageSessionAttribute(
                        SelectResponseProviderTypeViewBean.CALLING_VIEW_BEAN,
                        getClass().getName());
                    SelectResponseProviderTypeViewBean vb =
                        (SelectResponseProviderTypeViewBean)
                        getViewBean(SelectResponseProviderTypeViewBean.class);
                    setPageSessionAttribute(WIZARD, "true");
                    passPgSessionMap(vb);
                    vb.forwardTo(getRequestContext());
                }
            } else {
                forwardTo();
            }
        } catch (AMConsoleException e) {
            debug.warning(
            "PolicyOpViewBeanBase.handleTblResponseProvidersButtonAddRequest",
                e);
            redirectToStartURL();
        }
    }

    private void forwardToResponseProviderURL(
        String url,
        String responseProviderName,
        String responseProviderType,
        String realmName,
        String op
    ) {
        ResponseProviderProxyViewBean vb = (ResponseProviderProxyViewBean)
            getViewBean(ResponseProviderProxyViewBean.class);
        setPageSessionAttribute(ResponseProviderAddViewBean.CALLING_VIEW_BEAN,
            getClass().getName());
        passPgSessionMap(vb);
        vb.setURL(url, op);
        vb.setDisplayFieldValue(
            ResponseProviderProxyViewBean.TF_RESPONSEPROVIDER_TYPE_NAME,
            responseProviderType);
        vb.setDisplayFieldValue(
            ResponseProviderProxyViewBean.TF_RESPONSEPROVIDER_NAME,
            responseProviderName);

        if ((realmName == null) || (realmName.trim().length() == 0)) {
            realmName = AMModelBase.getStartDN(
                getRequestContext().getRequest());
        }

        vb.setDisplayFieldValue(ResponseProviderProxyViewBean.TF_REALM_NAME,
            realmName);
        vb.setDisplayFieldValue(ResponseProviderProxyViewBean.TF_CACHED_ID,
            (String)getPageSessionAttribute(
                ProfileViewBeanBase.PG_SESSION_POLICY_CACHE_ID));
        vb.setDisplayFieldValue(ResponseProviderProxyViewBean.TF_OP, op);
        vb.forwardTo(getRequestContext());
    }

    private void forwardToResponseProviderAddViewBean(
        String curRealm,
        String responseProviderType
    ) {
        ResponseProviderAddViewBean vb = (ResponseProviderAddViewBean)
            getViewBean(ResponseProviderAddViewBean.class);
        setPageSessionAttribute(ResponseProviderAddViewBean.CALLING_VIEW_BEAN,
            getClass().getName());
        setPageSessionAttribute(
            ResponseProviderOpViewBeanBase.PG_SESSION_PROVIDER_TYPE,
                responseProviderType);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    public void handleTblResponseProvidersEditLinkRequest(
        RequestInvocationEvent event)
        throws ModelControlException {
        PolicyModel model = (PolicyModel)getModel();
                                                                                
        String name = hexToString((String)getDisplayFieldValue(
            TBL_RESPONSE_ATTRIBUTES_ACTION_HREF));
        setPageSessionAttribute(ResponseProviderEditViewBean.CALLING_VIEW_BEAN,
            getClass().getName());
        setPageSessionAttribute(
            ResponseProviderEditViewBean.EDIT_RESPONSEPROVIDER_NAME, name);

        try {
            CachedPolicy cachedPolicy = getCachedPolicy();
            Policy policy = cachedPolicy.getPolicy();
            ResponseProvider provider = policy.getResponseProvider(name);
            String realmName = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            String responseProviderType = model.getResponseProviderTypeName(
                realmName, provider);
            String viewBeanURL = model.getResponseProviderViewBeanURL(
                realmName, responseProviderType);
            unlockPageTrail();
                                                                                
            if ((viewBeanURL != null) && (viewBeanURL.trim().length() > 0)){
                forwardToResponseProviderURL(viewBeanURL, name,
                    responseProviderType, realmName, "edit");
            } else {
                forwardToResponseProviderEditViewBean(
                    model, realmName, name, responseProviderType);
            }
        } catch (NameNotFoundException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                model.getErrorString(e));
            forwardTo();
        } catch (AMConsoleException e) {
            debug.warning(
               "PolicyOpViewBeanBase.handleTblResponseProvidersEditLinkRequest",
                e);
            redirectToStartURL();
        }
    }
                                                                                
    private void forwardToResponseProviderEditViewBean(
        PolicyModel model,
        String realmName,
        String name,
        String responseProviderType
    ) {
        ResponseProviderEditViewBean vb = (ResponseProviderEditViewBean)
            getViewBean(ResponseProviderEditViewBean.class);
        setPageSessionAttribute(ResponseProviderEditViewBean.CALLING_VIEW_BEAN,
            getClass().getName());
        setPageSessionAttribute(
            ResponseProviderOpViewBeanBase.PG_SESSION_PROVIDER_NAME, name);
        setPageSessionAttribute(
            ResponseProviderOpViewBeanBase.PG_SESSION_PROVIDER_TYPE,
                responseProviderType);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Handles delete response attribute request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblResponseProvidersButtonDeleteRequest(
        RequestInvocationEvent event
    ) throws ModelControlException {
        CCActionTable table = (CCActionTable)getChild(TBL_RESPONSE_ATTRIBUTES);
        table.restoreStateData();

        try {
            CachedPolicy cachedPolicy = getCachedPolicy();
            Policy policy = cachedPolicy.getPolicy();
            Integer[] selected = tblResponseProvidersModel.getSelectedRows();

            for (int i = 0; i < selected.length; i++) {
                tblResponseProvidersModel.setRowIndex(selected[i].intValue());
                String responseProviderName =
                    (String)tblResponseProvidersModel.getValue(
                    TBL_RESPONSE_ATTRIBUTES_DATA_NAME);
                policy.removeResponseProvider(responseProviderName);
            }
            cachedPolicy.setPolicyModified(true);

            populateResponseProvidersTable();
            forwardTo();
        } catch (AMConsoleException e) {
            debug.warning(
           "PolicyOpViewBeanBase.handleTblResponseProvidersButtonDeleteRequest",
                e);
            redirectToStartURL();
        }
    }



    public void handleTblConditionsButtonAddRequest(
        RequestInvocationEvent event
    ) throws ModelControlException {
        PolicyModel model = (PolicyModel)getModel();

        try {
            if (!reconstructPolicy()) {
                String curRealm = (String)getPageSessionAttribute(
                    AMAdminConstants.CURRENT_REALM);
                Map activeConditionTypes = model.getActiveConditionTypes(
                    curRealm);
                unlockPageTrail();

                if (activeConditionTypes.size() == 1) {
                    String conditionType =
                        (String)activeConditionTypes.keySet().iterator().next();
                    String viewBeanURL = model.getConditionViewBeanURL(
                        curRealm, conditionType);
                    removePageSessionAttribute(WIZARD);
                    setPageSessionAttribute(
                        ConditionOpViewBeanBase.CALLING_VIEW_BEAN,
                        getClass().getName());

                    if ((viewBeanURL != null) &&
                        (viewBeanURL.trim().length() > 0)
                    ){
                        forwardToConditionURL(viewBeanURL, "", conditionType,
                            curRealm, "add");
                    } else {
                        forwardToConditionAddViewBean(curRealm, conditionType);
                    }
                } else {
                    setPageSessionAttribute(
                        SelectConditionTypeViewBean.CALLING_VIEW_BEAN,
                        getClass().getName());
                    SelectConditionTypeViewBean vb =
                        (SelectConditionTypeViewBean)
                        getViewBean(SelectConditionTypeViewBean.class);
                    setPageSessionAttribute(WIZARD, "true");
                    passPgSessionMap(vb);
                    vb.forwardTo(getRequestContext());
                }
            } else {
                forwardTo();
            }
        } catch (AMConsoleException e) {
            debug.warning(
                "PolicyOpViewBeanBase.handleTblConditionsButtonAddRequest", e);
            redirectToStartURL();
        }
    }

    private void forwardToConditionURL(
        String url,
        String conditionName,
        String conditionType,
        String realmName,
        String op
    ) {
        ConditionProxyViewBean vb = (ConditionProxyViewBean)getViewBean(
            ConditionProxyViewBean.class);
        passPgSessionMap(vb);
        vb.setURL(url, op);
        vb.setDisplayFieldValue(ConditionProxyViewBean.TF_CONDITION_TYPE_NAME,
            conditionType);
        vb.setDisplayFieldValue(ConditionProxyViewBean.TF_CONDITION_NAME,
            conditionName);

        if ((realmName == null) || (realmName.trim().length() == 0)) {
            realmName = AMModelBase.getStartDN(
                getRequestContext().getRequest());
        }

        vb.setDisplayFieldValue(
            ConditionProxyViewBean.TF_REALM_NAME, realmName);
        vb.setDisplayFieldValue(ConditionProxyViewBean.TF_CACHED_ID,
            (String)getPageSessionAttribute(
                ProfileViewBeanBase.PG_SESSION_POLICY_CACHE_ID));
        vb.setDisplayFieldValue(ConditionProxyViewBean.TF_OP, op);
        vb.forwardTo(getRequestContext());
    }

    private void forwardToConditionAddViewBean(
        String curRealm,
        String conditionType
    ) {
        ConditionAddViewBean vb = (ConditionAddViewBean)getViewBean(
            ConditionAddViewBean.class);
        setPageSessionAttribute(ConditionAddViewBean.CALLING_VIEW_BEAN,
            getClass().getName());
        setPageSessionAttribute(ConditionAddViewBean.PG_SESSION_CONDITION_TYPE,
            conditionType);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    private void forwardToConditionEditViewBean(
        PolicyModel model,
        String curRealm,
        String name,
        String conditionType
    ) {
        ConditionEditViewBean vb = (ConditionEditViewBean)getViewBean(
            ConditionEditViewBean.class);
        setPageSessionAttribute(ConditionEditViewBean.CALLING_VIEW_BEAN,
            getClass().getName());
        setPageSessionAttribute(ConditionAddViewBean.PG_SESSION_CONDITION_NAME,
            name);
        setPageSessionAttribute(ConditionAddViewBean.PG_SESSION_CONDITION_TYPE,
            conditionType);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    public void handleTblConditionsEditLinkRequest(RequestInvocationEvent event)
        throws ModelControlException {
        PolicyModel model = (PolicyModel)getModel();

        String name = hexToString(
            (String)getDisplayFieldValue(TBL_CONDITIONS_ACTION_HREF));
        setPageSessionAttribute(ConditionEditViewBean.CALLING_VIEW_BEAN,
            getClass().getName());
        setPageSessionAttribute(
            ConditionEditViewBean.EDIT_CONDITION_NAME, name);

        try {
            CachedPolicy cachedPolicy = getCachedPolicy();
            Policy policy = cachedPolicy.getPolicy();

            Condition condition = policy.getCondition(name);
            String realmName = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            String conditionType = model.getConditionTypeName(
                realmName, condition);
            String viewBeanURL = model.getConditionViewBeanURL(
                realmName, conditionType);
            unlockPageTrail();

            if ((viewBeanURL != null) && (viewBeanURL.trim().length() > 0)) {
                forwardToConditionURL(viewBeanURL, name, conditionType,
                    realmName, "edit");
            } else {
                forwardToConditionEditViewBean(
                    model, realmName, name, conditionType);
            }
        } catch (NameNotFoundException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                model.getErrorString(e));
            forwardTo();
        } catch (AMConsoleException e) {
            debug.warning(
                "PolicyOpViewBeanBase.handleTblConditionsButtonDeleteRequest",
                e);
            redirectToStartURL();
        }
    }

    /**
     * Handles delete condition request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblConditionsButtonDeleteRequest(
        RequestInvocationEvent event
    ) throws ModelControlException {
        CCActionTable table = (CCActionTable)getChild(TBL_CONDITIONS);
        table.restoreStateData();

        try {
            CachedPolicy cachedPolicy = getCachedPolicy();
            Policy policy = cachedPolicy.getPolicy();
            Integer[] selected = tblConditionsModel.getSelectedRows();

            for (int i = 0; i < selected.length; i++) {
                tblConditionsModel.setRowIndex(selected[i].intValue());
                String conditionName = (String)tblConditionsModel.getValue(
                    TBL_CONDITIONS_DATA_NAME);
                policy.removeCondition(conditionName);
            }
            cachedPolicy.setPolicyModified(true);

            populateConditionsTable();
            forwardTo();
        } catch (AMConsoleException e) {
            debug.warning(
                "PolicyOpViewBeanBase.handleTblConditionsButtonDeleteRequest",
                e);
            redirectToStartURL();
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

    protected String getBackButtonLabel() {
        return getBackButtonLabel("table.policy.title.name");
    }

    protected abstract void createTableModels();
    protected abstract void createPageTitleModel();
    protected abstract String getPropertyXMLFileName(boolean readonly);
    protected abstract void populateTables();
    protected abstract boolean isProfilePage();
}
