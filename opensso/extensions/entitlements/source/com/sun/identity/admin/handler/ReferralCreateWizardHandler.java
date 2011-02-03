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
 * $Id: ReferralCreateWizardHandler.java,v 1.9 2009/07/13 19:42:42 farble1670 Exp $
 */

package com.sun.identity.admin.handler;

import com.sun.identity.admin.Resources;
import com.sun.identity.admin.model.LinkBean;
import com.sun.identity.admin.model.MessageBean;
import com.sun.identity.admin.model.NameReferralCreateWizardStepValidator;
import com.sun.identity.admin.model.NextPopupBean;
import com.sun.identity.admin.model.ReferralBean;
import com.sun.identity.admin.model.ReferralWizardStep;
import com.sun.identity.admin.model.ResourcesReferralWizardStepValidator;
import com.sun.identity.admin.model.SubjectsReferralWizardStepValidator;
import java.util.ArrayList;
import java.util.List;
import javax.faces.application.FacesMessage;
import javax.faces.event.ActionEvent;

public class ReferralCreateWizardHandler extends ReferralWizardHandler {
    @Override
    public void initWizardStepValidators() {
        getWizardStepValidators()[ReferralWizardStep.NAME.toInt()] = new NameReferralCreateWizardStepValidator(getWizardBean());
        getWizardStepValidators()[ReferralWizardStep.RESOURCES.toInt()] = new ResourcesReferralWizardStepValidator(getWizardBean());
        getWizardStepValidators()[ReferralWizardStep.SUBJECTS.toInt()] = new SubjectsReferralWizardStepValidator(getWizardBean());
    }

    public String getBeanName() {
        return "referralCreateWizardHandler";
    }

    public void finishListener(ActionEvent event) {
        if (!validateFinish(event)) {
            return;
        }

        ReferralBean rb = getReferralWizardBean().getReferralBean();
        getReferralDao().add(rb);

        getWizardBean().reset();
        getReferralManageBean().reset();

        doFinishNext();
    }

    public String createAction() {
        int realmsSize = getReferralWizardBean().getAvailableRealmBeans().size();
        if (realmsSize == 0) {
            MessageBean mb = new MessageBean();
            Resources r = new Resources();
            mb.setSummary(r.getString(this, "noAvailableSubjectsSummary"));
            mb.setDetail(r.getString(this, "noAvailableSubjectsDetail"));
            mb.setSeverity(FacesMessage.SEVERITY_ERROR);
            getMessagesBean().addMessageBean(mb);

            return null;
        }
        int resourcesSize = getReferralWizardBean().getAvailableResourcesSize();
        if (resourcesSize == 0) {
            MessageBean mb = new MessageBean();
            Resources r = new Resources();
            mb.setSummary(r.getString(this, "noAvailableResourcesSummary"));
            mb.setDetail(r.getString(this, "noAvailableResourcesDetail"));
            mb.setSeverity(FacesMessage.SEVERITY_ERROR);
            getMessagesBean().addMessageBean(mb);

            return null;

        }
        return "referral-create";
    }

    public void doFinishNext() {
        NextPopupBean npb = NextPopupBean.getInstance();
        npb.setVisible(true);
        Resources r = new Resources();
        npb.setTitle(r.getString(this, "finishTitle"));
        npb.setMessage(r.getString(this, "finishMessage"));
        npb.setLinkBeans(getFinishLinkBeans());

    }

    public void doCancelNext() {
        NextPopupBean npb = NextPopupBean.getInstance();
        npb.setVisible(true);
        Resources r = new Resources();
        npb.setTitle(r.getString(this, "cancelTitle"));
        npb.setMessage(r.getString(this, "cancelMessage"));
        npb.setLinkBeans(getCancelLinkBeans());

    }

    private List<LinkBean> getFinishLinkBeans() {
        List<LinkBean> lbs = new ArrayList<LinkBean>();
        lbs.add(LinkBean.HOME);
        lbs.add(LinkBean.REFERRAL_CREATE);
        lbs.add(LinkBean.REFERRAL_MANAGE);

        return lbs;
    }

    private List<LinkBean> getCancelLinkBeans() {
        List<LinkBean> lbs = new ArrayList<LinkBean>();
        lbs.add(LinkBean.HOME);
        lbs.add(LinkBean.REFERRAL_CREATE);
        lbs.add(LinkBean.REFERRAL_MANAGE);

        return lbs;
    }
}
