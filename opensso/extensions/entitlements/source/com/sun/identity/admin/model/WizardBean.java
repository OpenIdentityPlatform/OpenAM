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
 * $Id: WizardBean.java,v 1.7 2009/06/04 11:49:19 veiming Exp $
 */

package com.sun.identity.admin.model;

import java.io.Serializable;

public class WizardBean implements Serializable {

    private WizardStepBean[] wizardStepBeans = null;
    private int steps;

    public void reset() {
        wizardStepBeans = new WizardStepBean[getSteps()];
        for (int i = 0; i < getSteps(); i++) {
            wizardStepBeans[i] = new WizardStepBean();
        }

        if (getWizardStepBeans().length > 0) {
            WizardStepBean first = getWizardStepBeans()[0];
            first.setEnabled(true);
            first.setExpanded(true);
        }
    }

    public WizardStepBean[] getWizardStepBeans() {
        return wizardStepBeans;
    }

    public boolean isStepEnabled(int step) {
        WizardStepBean wsb = getWizardStepBeans()[step];
        return wsb.isEnabled();
    }

    public boolean isFinishRendered() {
        for (WizardStepBean wsb : wizardStepBeans) {
            if (!wsb.isEnabled()) {
                return false;
            }
        }
        return true;
    }

    public void gotoStep(int gotoStep) {
        setAllExpanded(false);

        WizardStepBean next = getWizardStepBeans()[gotoStep];
        next.setEnabled(true);
        next.setExpanded(true);
    }

    public void setAllEnabled(boolean enabled) {
        for (WizardStepBean wsb : getWizardStepBeans()) {
            wsb.setEnabled(enabled);
        }
    }

    public void setAllExpanded(boolean expaned) {
        for (WizardStepBean wsb : getWizardStepBeans()) {
            wsb.setExpanded(expaned);
        }
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
        reset();
    }
}
