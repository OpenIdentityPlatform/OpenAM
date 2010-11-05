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
 * $Id: WssCreateWizardSummaryWscSecurity.java,v 1.2 2009/07/23 20:46:53 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.Resources;
import java.io.Serializable;

public class WssCreateWizardSummaryWscSecurity
        extends WssCreateWizardSummary
        implements Serializable
{

    public WssCreateWizardSummaryWscSecurity(WssCreateWizardBean wssCreateWizardBean)
    {
        super(wssCreateWizardBean);
    }

    @Override
    public int getGotoStep() {
        return WssCreateWizardStep.WSC_SERVICENAME_SECURITY.toInt();
    }

    @Override
    public String getIcon() {
        return "../image/edit.png";
    }

    @Override
    public String getLabel() {
        Resources r = new Resources();
        String label = r.getString(this, "label");
        return label;
    }

    @Override
    public String getTemplate() {
        return null;
    }

    @Override
    public String getValue() {
        WssCreateWizardBean wizardBean
                = (WssCreateWizardBean) getWssCreateWizardBean();
        SecurityMechanism securityMechanism
                = SecurityMechanism.valueOf(wizardBean.getWscSecurityMechanism());
        return securityMechanism.toLocaleString();
    }

    @Override
    public boolean isExpandable() {
        return false;
    }

}
