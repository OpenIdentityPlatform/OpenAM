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
 * $Id: PolicyNormalViewBeanBase.java,v 1.3 2008/07/10 00:27:57 veiming Exp $
 *
 */

package com.sun.identity.console.policy;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.QueryResults;
import com.sun.identity.console.policy.model.PolicyModel;
import com.sun.web.ui.view.html.CCButton;
import com.sun.web.ui.view.html.CCTextField;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.util.Map;

public abstract class PolicyNormalViewBeanBase
    extends PolicyOpViewBeanBase
{
    protected static final String PGTITLE_TWO_BTNS = "pgtitleTwoBtns";

    public PolicyNormalViewBeanBase(String name, String url) {
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
        } catch (AMConsoleException e) {
            //NO-OP
            /* attribute values will not be populated if cached policy
             * cannot be located.
             */
        }

        CCButton btn = (CCButton)getChild("tblSubjectsButtonDelete");
        btn.setDisabled(true);
        btn = (CCButton)getChild("tblResponseProvidersButtonDelete");
        btn.setDisabled(true);
        btn = (CCButton)getChild("tblConditionsButtonDelete");
        btn.setDisabled(true);
        
        PolicyModel model = (PolicyModel)getModel();
        String realm = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        QueryResults subjectsQuery = model.getActiveSubjectTypes(realm);
        Map subjects = (Map)subjectsQuery.getResults();
        if ((subjects == null) || subjects.isEmpty()) {
            ((CCButton)getChild("tblSubjectsButtonAdd")).setDisabled(true);
            setDisplayFieldValue("tblSubjectsNote", 
                model.getLocalizedString("policy.no.subject.types"));
        } else {
            setDisplayFieldValue("tblSubjectsNote", "");
        }
        
        Map conditions = model.getActiveConditionTypes(realm);
        if ((conditions == null) || conditions.isEmpty()) {
            ((CCButton)getChild("tblConditionsButtonAdd")).setDisabled(true);
            setDisplayFieldValue("tblConditionsNote", 
                model.getLocalizedString("policy.no.condition.types"));
        } else {
            setDisplayFieldValue("tblConditionsNote", "");
        }
        
        Map responseProviders = model.getActiveResponseProviderTypes(realm);
        if ((responseProviders == null) || responseProviders.isEmpty()) {
            ((CCButton)getChild("tblResponseProvidersButtonAdd")).
                setDisabled(true);
            setDisplayFieldValue("tblResponseProvidersNote", 
                model.getLocalizedString("policy.no.response.providers.types"));
        } else {
            setDisplayFieldValue("tblResponseProvidersNote", "");
        }
    }

    protected abstract void createPageTitleModel();

    protected String getPropertyXMLFileName(boolean readonly) {
        return (readonly) ?
            "com/sun/identity/console/propertyPMPolicyNormal_Readonly.xml" :
            "com/sun/identity/console/propertyPMPolicyNormal.xml";
    }

    protected void createTableModels() {
        createRuleTableModels();
        createSubjectTableModels();
        createResponseProviderTableModels();
        createConditionTableModels();
    }

    protected void populateTables() {
        try {
            populateRulesTable();
            populateSubjectsTable();
            populateResponseProvidersTable();
            populateConditionsTable();
        } catch (AMConsoleException e) {
            //NO-OP
            //table will not be populated if cached policy cannot be located.
        }
    }
}
