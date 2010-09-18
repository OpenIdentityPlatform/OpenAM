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
 * $Id: RuleEditViewBean.java,v 1.2 2008/06/25 05:43:05 qcheng Exp $
 *
 */

package com.sun.identity.console.policy;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.policy.model.CachedPolicy;
import com.sun.identity.console.policy.model.PolicyModel;
import com.sun.identity.policy.ActionSchema;
import com.sun.identity.policy.NameAlreadyExistsException;
import com.sun.identity.policy.NameNotFoundException;
import com.sun.identity.policy.InvalidNameException;
import com.sun.identity.policy.Policy;
import com.sun.identity.policy.Rule;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Set;

public class RuleEditViewBean
    extends RuleOpViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/policy/RuleEdit.jsp";
    public static final String EDIT_RULE_NAME = "editRuleName";
    private static final String PGTITLE_THREE_BTNS = "pgtitleThreeBtns";

    RuleEditViewBean(String name, String defaultDisplayURL) {
        super(name, defaultDisplayURL);
    }

    /**
     * Creates a policy creation view bean.
     */
    public RuleEditViewBean() {
        super("RuleEdit", DEFAULT_DISPLAY_URL);
    }

    protected void registerChildren() {
        super.registerChildren();
        registerChild(PGTITLE_THREE_BTNS, CCPageTitle.class);
    }

    protected View createChild(String name) {
        View view = null;

        if (name.equals(PGTITLE_THREE_BTNS)) {
            view = new CCPageTitle(this, ptModel, name);
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        super.beginDisplay(event);
        if (!submitCycle) {
            try {
                Rule rule = getRule();
                propertySheetModel.setValue(RULE_NAME, rule.getName());

                String serviceType = rule.getServiceTypeName();
                propertySheetModel.setValue(SERVICE_TYPE, serviceType);

                PolicyModel model = (PolicyModel)getModel();
                Map map = model.getServiceTypeNames();
                String i18nName = (String)map.get(serviceType);
                propertySheetModel.setValue(SERVICE_TYPE_NAME_LBL, i18nName);
                propertySheetModel.setValue(SERVICE_TYPE_NAME, i18nName);
                propertySheetModel.setValue(RESOURCE_NAME,
                    rule.getResourceName());
            } catch (NameNotFoundException e) {
                debug.error(
                    "RuleEditViewBean.beginDisplay: rule not found.");
            } catch (AMConsoleException e) {
                debug.warning("RuleEditViewBean.beginDisplay", e);
            }
        } else {
            String i18nName = (String)propertySheetModel.getValue(
                SERVICE_TYPE_NAME);
            propertySheetModel.setValue(SERVICE_TYPE_NAME_LBL, i18nName);
        }

        try {
            populateActionsTable(submitCycle);
        } catch (AMConsoleException e) {
            debug.warning("RuleEditViewBean.beginDisplay", e);
            //NO-OP
        }
    }

    public Set getDefaultActionValues(ActionSchema as) {
        Set values = null;
        try {
            Rule rule = getRule();
            values = rule.getActionValues(as.getName());
            if (values == null) {
                values = as.getDefaultValues();
            }
        } catch (NameNotFoundException e) {
            values = as.getDefaultValues();
        } catch (AMConsoleException e) {
            debug.warning("RuleEditViewBean.getDefaultActionValues", e);
            values = as.getDefaultValues();
        }
        return values;
    }

    public boolean isActionSelected(ActionSchema as) {
        boolean selected = false;

        if (actionValues != null) {
            Set values = (Set)actionValues.get(as.getName());
            selected = (values != null) && !values.isEmpty();
        } else {
            try {
                Rule rule = getRule();
                Set actionNames = rule.getActionNames();
                selected = (actionNames != null) &&
                    actionNames.contains(as.getName());
            } catch (NameNotFoundException e) {
                selected = false;
            } catch (AMConsoleException e) {
                debug.warning("RuleEditViewBean.isActionSelected", e);
                selected = false;
            }
        }

        return selected;
    }

    protected Rule getRule()
        throws NameNotFoundException, AMConsoleException
    {
        CachedPolicy cachedPolicy = getCachedPolicy();
        Policy policy = cachedPolicy.getPolicy();
        String ruleName = (String)getPageSessionAttribute(EDIT_RULE_NAME);
        return policy.getRule(ruleName);
    }

    protected void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.save");
        ptModel.setValue("button2", "button.reset");
        ptModel.setValue("button3", getBackButtonLabel());
    }

    protected String getPropertyXMLFileName(boolean readonly) {
        if (!isReferralPolicy()) {
            return (readonly) ?
                "com/sun/identity/console/propertyPMRuleAdd_Readonly.xml" :
                "com/sun/identity/console/propertyPMRuleAdd.xml";
        } else {
            return (readonly) ?
            "com/sun/identity/console/propertyPMRuleAddNoAction_Readonly.xml" :
                "com/sun/identity/console/propertyPMRuleAddNoAction.xml";
        }
    }

    /**
     * Handles reset request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        forwardTo();
    }

    /**
     * Handles create policy request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException {
        submitCycle = true;
        boolean forwarded = false;
        Rule rule = createRule();
        if (rule != null) {
            try {
                CachedPolicy cachedPolicy = getCachedPolicy();
                Policy policy = cachedPolicy.getPolicy();
                String origRuleName = (String)getPageSessionAttribute(
                    EDIT_RULE_NAME);
                policy.removeRule(origRuleName); 
                policy.addRule(rule);
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "policy.rule.updated");
                cachedPolicy.setPolicyModified(true);
            } catch (NameAlreadyExistsException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    getModel().getErrorString(e));
            } catch (InvalidNameException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    getModel().getErrorString(e));
            } catch (AMConsoleException e) {
                debug.warning("RuleEditViewBean.handleButton1Request", e);
                redirectToStartURL();
                forwarded = true;
            }
        }

        if (!forwarded) {
            forwardTo();
        }
    }

    protected String getBreadCrumbDisplayName() {
        PolicyModel model = (PolicyModel)getModel();
        String origRuleName = (String)getPageSessionAttribute(EDIT_RULE_NAME);
        String[] arg = {origRuleName};
        return MessageFormat.format(
            model.getLocalizedString("breadcrumbs.editRule"), (Object[])arg);
    }
                                                                                
    protected boolean startPageTrail() {
        return false;
    }
}
