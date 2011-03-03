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
 * $Id: WscCreateServiceSecuritySummary.java,v 1.2 2009/10/16 19:39:18 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.Resources;

public class WscCreateServiceSecuritySummary extends WscCreateWizardSummary {

    public WscCreateServiceSecuritySummary(WscCreateWizardBean wizardBean) {
        super(wizardBean);
    }

    @Override
    public String getLabel() {
        Resources r = new Resources();
        String label = r.getString(this, "label");
        return label;
    }

    @Override
    public String getValue() {
        WscCreateWizardBean wizardBean = getWscCreateWizardBean();
        SecurityMechanism sm = null;
        
        if( wizardBean.isUsingSts() ) {
            StsClientProfileBean stsClientProfile 
                = wizardBean.getStsClientProfileBean();

            if( stsClientProfile.getSecurityMechanism() != null ) {
                sm = SecurityMechanism.valueOf(stsClientProfile.getSecurityMechanism());
            }
            
        } else {
            WscProfileBean wscProfile = wizardBean.getWscProfileBean();
            
            if( wscProfile.getSecurityMechanism() != null ) {
                sm = SecurityMechanism.valueOf(wscProfile.getSecurityMechanism());
            }
        }
        
        return sm == null ? "" : sm.toLocaleString();
    }

    @Override
    public String getTemplate() {
        return null;
    }

    @Override
    public String getIcon() {
        return "../image/edit.png";
    }

    @Override
    public boolean isExpandable() {
        return false;
    }

    @Override
    public int getGotoStep() {
        return WscCreateWizardStep.WSC_SECURITY.toInt();
    }

}
