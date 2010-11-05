/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: PolicySummary.java,v 1.5 2009/06/05 21:39:27 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import javax.faces.event.ActionEvent;

public abstract class PolicySummary
        extends Summary {

    private PolicyWizardBean policyWizardBean;

    public PolicySummary(PolicyWizardBean policyWizardBean) {
        this.policyWizardBean = policyWizardBean;
    }

    public abstract String getLabel();

    public abstract String getValue();

    public abstract String getTemplate();

    public abstract String getIcon();

    public abstract boolean isExpandable();

    public int getTabIndex() {
        return PolicyWizardAdvancedTabIndex.ACTIONS.toInt();
    }

    public PolicyWizardBean getPolicyWizardBean() {
        return policyWizardBean;
    }

    protected PolicyWizardAdvancedTabIndex getGotoTabIndex(ActionEvent event) {
        Object o = event.getComponent().getAttributes().get("gotoTabIndex");
        Integer i = (Integer)o;
        PolicyWizardAdvancedTabIndex index = PolicyWizardAdvancedTabIndex.valueOf(i.intValue());

        return index;
    }

    protected PolicyWizardStep getGotoStep(ActionEvent event) {
        Object o = event.getComponent().getAttributes().get("gotoStep");
        Integer i = (Integer)o;
        PolicyWizardStep pws = PolicyWizardStep.valueOf(i.intValue());

        return pws;
    }

    public void editListener(ActionEvent event) {
        PolicyWizardStep gotoStep = getGotoStep(event);
        getPolicyWizardBean().gotoStep(gotoStep.toInt());

        if (gotoStep.equals(PolicyWizardStep.ADVANCED)) {
            PolicyWizardAdvancedTabIndex gotoIndex = getGotoTabIndex(event);
            getPolicyWizardBean().setAdvancedTabsetIndex(gotoIndex.toInt());
        }
    }
}
