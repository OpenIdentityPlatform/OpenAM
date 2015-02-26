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
 * $Id: PolicyReferralViewBeanBase.java,v 1.2 2008/06/25 05:43:04 qcheng Exp $
 *
 */

package com.sun.identity.console.policy;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.policy.model.PolicyModel;
import com.sun.web.ui.view.html.CCButton;
import com.sun.web.ui.view.html.CCTextField;
import com.sun.web.ui.view.pagetitle.CCPageTitle;

public abstract class PolicyReferralViewBeanBase
    extends PolicyOpViewBeanBase
{
    protected static final String PGTITLE_TWO_BTNS = "pgtitleTwoBtns";

    public PolicyReferralViewBeanBase(String name, String url) {
        super(name, url);
    }

    protected void registerChildren() {
        super.registerChildren();
        registerChild(PGTITLE_TWO_BTNS, CCPageTitle.class);
        registerChild(PolicyModel.TF_NAME, CCTextField.class);
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

        try {
            populateAttributes();
            PolicyModel model = (PolicyModel)getModel();
            String realmName = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            if (model.getActiveReferralTypes(realmName).isEmpty()) {
                CCButton btn = (CCButton)getChild("tblReferralsButtonAdd");
                btn.setDisabled(true);
            }

            CCButton btn = (CCButton)getChild("tblReferralsButtonDelete");
            btn.setDisabled(true);
        } catch (AMConsoleException e) {
            CCButton btn = (CCButton)getChild("tblReferralsButtonAdd");
            btn.setDisabled(true);
            btn = (CCButton)getChild("tblReferralsButtonDelete");
            btn.setDisabled(true);
        }
    }

    protected String getPropertyXMLFileName(boolean readonly) {
        return (readonly) ?
            "com/sun/identity/console/propertyPMPolicyReferral_Readonly.xml" :
            "com/sun/identity/console/propertyPMPolicyReferral.xml";
    }

    protected void createTableModels() {
        createRuleTableModels();
        createReferralTableModels();
    }

    protected void populateTables() {
        try {
            populateRulesTable();
            populateReferralsTable();
        } catch (AMConsoleException e) {
            //NO-OP
            //table will not be populated if cached policy cannot be located.
        }
    }

    protected abstract void createPageTitleModel();
}
