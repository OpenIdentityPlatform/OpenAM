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
 * $Id: SamlV2HostedIdpCreateSummary.java,v 1.1 2009/06/24 21:55:09 asyhuang Exp $
 */
package com.sun.identity.admin.model;

import javax.faces.event.ActionEvent;

public abstract class SamlV2HostedIdpCreateSummary
        extends Summary {

    private SamlV2HostedIdpCreateWizardBean samlV2HostedIdpCreateWizardBean;

    public SamlV2HostedIdpCreateSummary(
            SamlV2HostedIdpCreateWizardBean samlV2HostedIdpCreateWizardBean) {
        this.samlV2HostedIdpCreateWizardBean = samlV2HostedIdpCreateWizardBean;
    }

    public abstract String getLabel();

    public abstract String getValue();

    public abstract String getTemplate();

    public abstract String getIcon();

    public abstract boolean isExpandable();

    public abstract int getGotoStep();

    protected SamlV2HostedIdpCreateWizardStep getGotoStep(ActionEvent event) {
        Object o = event.getComponent().getAttributes().get("gotoStep");
        Integer i = (Integer) o;
        SamlV2HostedIdpCreateWizardStep rws = SamlV2HostedIdpCreateWizardStep.valueOf(i.intValue());

        return rws;
    }

    public SamlV2HostedIdpCreateWizardBean getSamlV2HostedIdpCreateWizardBean() {
        return samlV2HostedIdpCreateWizardBean;
    }

    public void editListener(ActionEvent event) {
        SamlV2HostedIdpCreateWizardStep gotoStep = getGotoStep(event);
        getSamlV2HostedIdpCreateWizardBean().gotoStep(gotoStep.toInt());
    }

    public int getTabIndex() {
        return -1;
    }
}
