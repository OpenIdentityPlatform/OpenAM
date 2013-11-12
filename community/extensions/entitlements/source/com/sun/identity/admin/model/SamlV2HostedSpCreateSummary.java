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
 * $Id: SamlV2HostedSpCreateSummary.java,v 1.2 2009/06/24 21:55:08 asyhuang Exp $
 */
package com.sun.identity.admin.model;

import javax.faces.event.ActionEvent;

public abstract class SamlV2HostedSpCreateSummary
        extends Summary {

    private SamlV2HostedSpCreateWizardBean samlV2HostedSpCreateWizardBean;

    public SamlV2HostedSpCreateSummary(SamlV2HostedSpCreateWizardBean samlV2HostedSpCreateWizardBean) {
        this.samlV2HostedSpCreateWizardBean = samlV2HostedSpCreateWizardBean;
    }

    public abstract String getLabel();

    public abstract String getValue();

    public abstract String getTemplate();

    public abstract String getIcon();

    public abstract boolean isExpandable();

    public abstract int getGotoStep();

    protected SamlV2HostedSpCreateWizardStep getGotoStep(ActionEvent event) {
        Object o = event.getComponent().getAttributes().get("gotoStep");
        Integer i = (Integer) o;
        SamlV2HostedSpCreateWizardStep ws = SamlV2HostedSpCreateWizardStep.valueOf(i.intValue());
        return ws;
    }

    public SamlV2HostedSpCreateWizardBean getSamlV2HostedSpCreateWizardBean() {
        return samlV2HostedSpCreateWizardBean;
    }

    public void editListener(ActionEvent event) {
        SamlV2HostedSpCreateWizardStep gotoStep = getGotoStep(event);
        getSamlV2HostedSpCreateWizardBean().gotoStep(gotoStep.toInt());
    }

    public int getTabIndex() {
        return -1;
    }
}
