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
 * $Id: WebExConfigWizardStep2Validator.java,v 1.1 2009/12/08 02:13:40 babysunil Exp $
 */

package com.sun.identity.admin.model;

import com.icesoft.faces.context.effects.Effect;
import com.sun.identity.admin.dao.SamlV2CreateSharedDao;
import com.sun.identity.admin.effect.InputFieldErrorEffect;
import com.sun.identity.admin.effect.MessageErrorEffect;
import com.sun.identity.admin.Resources;

import java.net.MalformedURLException;
import java.net.URL;
import javax.faces.application.FacesMessage;

public class WebExConfigWizardStep2Validator
        extends WizardStepValidator {

    public static final String pattern = ".{1,1024}?";

    public WebExConfigWizardStep2Validator(WizardBean wb) {
        super(wb);
    }

    @Override
    public boolean validate() {
        if (!validSiteUrl()) {
            return false;
        }

         if (!isSPNamevalid()) {
            return false;
        }

        return true;
    }

    private boolean validSiteUrl() {
        WebExConfigWizardBean wb = getWebExConfigWizardBean();
        String siteurl = wb.getWebexSiteUrl();
        try {
            new URL(siteurl);
            if (siteurl != null && siteurl.matches(pattern)) {
                return true;
            }
        } catch (MalformedURLException ex) {
            // do nothing but flow through below for error message
        }
        Effect e;
        e = new InputFieldErrorEffect();
        wb.setWebexSiteUrlNameInputEffect(e);

        e = new MessageErrorEffect();
        wb.setWebexSiteUrlMessageEffect(e);

        showErrorMessage("invalidWebExSiteUrlError",
                "invalidWebExSiteUrlDetails");
        return false;
    }

    private boolean isSPNamevalid() {
        WebExConfigWizardBean wb = getWebExConfigWizardBean();
        String spName = wb.getWebexSiteUrl();
        if ((spName == null) || (!(spName.length() > 0)) ||
                (!SamlV2CreateSharedDao.getInstance().valideEntityName(spName))) {
            Effect e;
            e = new InputFieldErrorEffect();
            wb.setWebexSiteUrlNameInputEffect(e);

            e = new MessageErrorEffect();
            wb.setWebexSiteUrlMessageEffect(e);

            showErrorMessage("invalidSPNameSummary",
                    "invalidSPNameDetail");
            return false;
        }

        return true;
    }

    protected WebExConfigWizardBean getWebExConfigWizardBean() {
        return (WebExConfigWizardBean) getWizardBean();
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
