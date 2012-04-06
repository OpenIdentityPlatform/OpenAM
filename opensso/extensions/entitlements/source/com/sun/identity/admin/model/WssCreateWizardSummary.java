/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
 * $Id: WssCreateWizardSummary.java,v 1.2 2009/07/23 20:46:54 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import java.io.Serializable;
import javax.faces.event.ActionEvent;

public abstract class WssCreateWizardSummary
        extends Summary
        implements Serializable
{

    private WssCreateWizardBean wssCreateWizardBean;

    public WssCreateWizardSummary(WssCreateWizardBean wssCreateWizardBean) {
        this.wssCreateWizardBean = wssCreateWizardBean;
    }

    protected WssCreateWizardStep getGotoStep(ActionEvent event) {
        Object o = event.getComponent().getAttributes().get("gotoStep");
        Integer i = (Integer) o;
        WssCreateWizardStep wizardStep = WssCreateWizardStep.valueOf(i.intValue());

        return wizardStep;
    }

    public void editListener(ActionEvent event) {
        WssCreateWizardStep gotoStep = getGotoStep(event);
        wssCreateWizardBean.gotoStep(gotoStep.toInt());
    }

    public int getTabIndex() {
        return -1;
    }

    public WssCreateWizardBean getWssCreateWizardBean() {
        return wssCreateWizardBean;
    }

    public void setWssCreateWizardBean(WssCreateWizardBean wssCreateWizardBean) {
        this.wssCreateWizardBean = wssCreateWizardBean;
    }

}
