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
 * $Id: ReferralSummary.java,v 1.2 2009/08/06 15:25:31 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import javax.faces.event.ActionEvent;

public abstract class ReferralSummary extends Summary {

    private ReferralWizardBean referralWizardBean;

    public ReferralSummary(ReferralWizardBean referralWizardBean) {
        this.referralWizardBean = referralWizardBean;
    }

    public abstract String getLabel();

    public abstract String getValue();

    public abstract String getTemplate();

    public abstract String getIcon();

    public abstract boolean isExpandable();

    public abstract int getGotoStep();

    protected ReferralWizardStep getGotoStep(ActionEvent event) {
        Object o = event.getComponent().getAttributes().get("gotoStep");
        Integer i = (Integer)o;
        ReferralWizardStep rws = ReferralWizardStep.valueOf(i.intValue());

        return rws;
    }

    public ReferralWizardBean getReferralWizardBean() {
        return referralWizardBean;
    }

    public void editListener(ActionEvent event) {
        ReferralWizardStep gotoStep = getGotoStep(event);
        getReferralWizardBean().gotoStep(gotoStep.toInt());
    }

    public int getTabIndex() {
        return -1;
    }
}
