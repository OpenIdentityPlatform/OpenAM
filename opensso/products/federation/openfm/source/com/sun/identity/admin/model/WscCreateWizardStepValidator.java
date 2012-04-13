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
 * $Id: WscCreateWizardStepValidator.java,v 1.2 2009/10/16 19:38:47 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import javax.faces.application.FacesMessage;

import com.sun.identity.admin.Resources;

public abstract class WscCreateWizardStepValidator extends WizardStepValidator {
    public WscCreateWizardStepValidator(WizardBean wb) {
        super(wb);
    }

    protected WscCreateWizardBean getWscCreateWizardBean() {
        return (WscCreateWizardBean)getWizardBean();
    }
    
    protected void showErrorMessage(String summaryKey, String detailKey) {
        MessageBean mb = new MessageBean();
        Resources r = new Resources();
        mb.setSummary(r.getString(this, summaryKey));
        mb.setDetail(r.getString(this, detailKey));
        mb.setSeverity(FacesMessage.SEVERITY_ERROR);
        
        getMessagesBean().addMessageBean(mb);
    }

}
