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
 * $Id: RuleAddViewBean.java,v 1.2 2008/06/25 05:43:05 qcheng Exp $
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
import com.sun.identity.policy.InvalidNameException;
import com.sun.identity.policy.Policy;
import com.sun.identity.policy.Rule;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.util.Map;
import java.util.Set;

public class RuleAddViewBean
    extends RuleOpViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/policy/RuleAdd.jsp";

    private static final String PGTITLE_TWO_BTNS = "pgtitleTwoBtns";
    public String serviceType;
    public String ruleName;

    RuleAddViewBean(String name, String defaultDisplayURL) {
        super(name, defaultDisplayURL);
    }

    /**
     * Creates a policy creation view bean.
     */
    public RuleAddViewBean() {
        super("RuleAdd", DEFAULT_DISPLAY_URL);
    }

    protected void registerChildren() {
        super.registerChildren();
        registerChild(PGTITLE_TWO_BTNS, CCPageTitle.class);
    }

    protected View createChild(String name) {
        View view = null;

        if (name.equals(PGTITLE_TWO_BTNS)) {
            view = new CCPageTitle(this, ptModel, name);
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        super.beginDisplay(event);

        if (ruleName != null) {
            propertySheetModel.setValue(RULE_NAME, ruleName);
        }
        if (serviceType != null) {
            propertySheetModel.setValue(SERVICE_TYPE, serviceType);
            PolicyModel model = (PolicyModel)getModel();
            Map map = model.getServiceTypeNames();
            String i18nName = (String)map.get(serviceType);
            propertySheetModel.setValue(SERVICE_TYPE_NAME_LBL, i18nName);
            propertySheetModel.setValue(SERVICE_TYPE_NAME, i18nName);
        } else {
            String i18nName = (String)propertySheetModel.getValue(
                SERVICE_TYPE_NAME);
            propertySheetModel.setValue(SERVICE_TYPE_NAME_LBL, i18nName);
        }

        try {
            populateActionsTable(true);
        } catch (AMConsoleException e) {
            debug.warning("RuleAddViewBean.beginDisplay", e);
            //NO-OP
        }
    }

    protected void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.back");
        ptModel.setValue("button2", "button.finish");
        ptModel.setValue("button3", "button.cancel");
    }

    protected String getPropertyXMLFileName(boolean readonly) {
        return (!isReferralPolicy()) ?
            "com/sun/identity/console/propertyPMRuleAdd.xml" :
            "com/sun/identity/console/propertyPMRuleAddNoAction.xml";
    }

    public Set getDefaultActionValues(ActionSchema as) {
        return as.getDefaultValues();
    }

    public boolean isActionSelected(ActionSchema as) {
        return false;
    }

    public void handleButton1Request(RequestInvocationEvent event) {
        SelectServiceTypeViewBean vb = (SelectServiceTypeViewBean)
            getViewBean(SelectServiceTypeViewBean.class);
        unlockPageTrailForSwapping();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Handles create policy request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event)
        throws ModelControlException {
        submitCycle = true;
        Rule rule = createRule();
        if (rule != null) {
            try {
                CachedPolicy cachedPolicy = getCachedPolicy();
                Policy policy = cachedPolicy.getPolicy();
                policy.addRule(rule);
                backTrail();
                cachedPolicy.setPolicyModified(true);
                forwardToPolicyViewBean();
            } catch (NameAlreadyExistsException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    getModel().getErrorString(e));
                forwardTo();
            } catch (InvalidNameException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    getModel().getErrorString(e));
                forwardTo();
            } catch (AMConsoleException e) {
                debug.warning("RuleAddViewBean.handleButton2Request", e);
                redirectToStartURL();
            }
        } else {
            forwardTo();
        }
    }

    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.addRule";
    }

    protected boolean startPageTrail() {
        return false;
    }

}
