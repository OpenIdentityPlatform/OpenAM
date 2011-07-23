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
 * $Id: WebExConfigWizardStep3Validator.java,v 1.1 2009/12/08 02:13:40 babysunil Exp $
 */

package com.sun.identity.admin.model;

import com.icesoft.faces.context.effects.Effect;
import com.sun.identity.admin.dao.SamlV2CreateSharedDao;
import com.sun.identity.admin.dao.WebExHostedIdpCreateDao;
import com.sun.identity.admin.dao.WebExRemoteSpCreateDao;
import com.sun.identity.admin.Resources;
import java.util.ArrayList;
import java.util.List;
import javax.faces.application.FacesMessage;

public class WebExConfigWizardStep3Validator
        extends WizardStepValidator {

    public WebExConfigWizardStep3Validator(WizardBean wb) {
        super(wb);
    }

    @Override
    public boolean validate() {
        boolean result = false;
        WebExConfigWizardBean wb = getWebExConfigWizardBean();

        if (wb.getChooseExistng() && (!validIdpName())) {
            return result;
        }

        if (wb.getChooseCot() && (!validCotName())) {
            return result;
        }

        result = createOpenssoConfig();

        return result;
    }

    private boolean validIdpName() {
        WebExConfigWizardBean wb = getWebExConfigWizardBean();
        String idpName = wb.getNewIdpName();
        if ((idpName == null) || (!(idpName.length() > 0))) {
            showErrorMessage(
                    "invalidNameSummary",
                    "invalidNameDetail");
            return false;
        }

        if (!SamlV2CreateSharedDao.getInstance().valideEntityName(idpName)){
            showErrorMessage(
                    "NameExistsSummary",
                    "NameExistsDetail");
            return false;
        }

        return true;
    }

    private boolean validCotName() {
        WebExConfigWizardBean wb = getWebExConfigWizardBean();
        String cotName = wb.getNewCotName();
        if ((cotName == null) || cotName.length() == 0) {
            showErrorMessage(
                    "invalidCotSummary",
                    "invalidCotDetail");
            return false;
        }

        if (!SamlV2CreateSharedDao.getInstance().validateCot(cotName)) {
            showErrorMessage(
                    "cotExistSummary",
                    "cotExistDetail");
            return false;
        }
        return true;
    }

    private boolean createOpenssoConfig() {
        boolean result = false;

        WebExConfigWizardBean wizardBean = getWebExConfigWizardBean();

        WebExHostedIdpCreateDao webExHostedIdpCreateDao =
                new WebExHostedIdpCreateDao();
        WebExRemoteSpCreateDao webExRemoteSpCreateDao =
                new WebExRemoteSpCreateDao();
        String realmName = wizardBean.getRealmName();
        String spName = wizardBean.getWebexSiteUrl();
        boolean spattr = wizardBean.getAutocrup();
        boolean signAuthnreq = wizardBean.getAuthnReq();
        boolean sloNeeded = wizardBean.getSloandsso();
        String cotName = null;
        String idpName = null;
        String certContent = null;

        if (signAuthnreq) {
            certContent = wizardBean.getSpCertFile();
        }

        boolean chooseFromExisintgCot = wizardBean.getChooseCot();
        if (chooseFromExisintgCot) {
            cotName = wizardBean.getNewCotName();
        } else {
            cotName = wizardBean.getSelectedCot();
        }

        boolean chooseFromExisintgIdp = wizardBean.getChooseExistng();
        if (chooseFromExisintgIdp) {
            idpName = wizardBean.getNewIdpName();
        } else {
            idpName = wizardBean.getSelectedIdp();
        }

        boolean chooseFromExistingIdp = wizardBean.getChooseExistng();
        if (chooseFromExistingIdp) {
            String key = wizardBean.getSelectedSigningKey();
             if ((key == null) || !(key.length() > 0)) {
                showErrorMessage(
                        "chooseavalidCertSummary", "chooseavalidCertDetail");
                return false;
            }
            List attrMapping = new ArrayList();

            result = webExHostedIdpCreateDao.createSamlv2HostedIdp(
                    realmName, idpName, cotName, key, attrMapping);
            if (!result) {
                showErrorMessage(
                        "creationIdpFailedSummary", "creationIdpFailedDetail");
                return result;
            }
        } else if (chooseFromExisintgCot) {
            idpName = wizardBean.getSelectedIdp();
            result = webExHostedIdpCreateDao.addIdptoCot(
                    realmName, cotName, idpName);
            if (!result) {
                showErrorMessage(
                        "addingCotFailedSummary", "addingCotFailedDetails");
                return result;
            }
        }

        //update Idp
        result = webExHostedIdpCreateDao.updateIdpforWebEx(
                realmName, idpName, signAuthnreq);
        if (!result) {
            showErrorMessage("updateofIdpFailed", "updateofIdpFailedDetails");
            return result;
        }

        //create remote SP
        List spAttrMapping = new ArrayList();
        if (spattr) {
            spAttrMapping.add("firstname=givenname");
            spAttrMapping.add("lastname=sn");
            spAttrMapping.add("email=mail");
            spAttrMapping.add("uid=uid");

        }

        result = webExRemoteSpCreateDao.createSamlv2RemoteSp(
                realmName, spName, cotName, certContent, spAttrMapping, signAuthnreq, sloNeeded);
        if (!result) {
            showErrorMessage("remoteSpCreateFailed", "remoteSpCreateFailedDetail");
            return result;
        }
        return result;
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
