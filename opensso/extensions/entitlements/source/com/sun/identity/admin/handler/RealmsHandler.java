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
 * $Id: RealmsHandler.java,v 1.11 2009/07/25 00:30:05 farble1670 Exp $
 */
package com.sun.identity.admin.handler;

import com.sun.identity.admin.Resources;
import com.sun.identity.admin.model.LinkBean;
import com.sun.identity.admin.model.MessageBean;
import com.sun.identity.admin.model.MessagesBean;
import com.sun.identity.admin.model.PhaseEventAction;
import com.sun.identity.admin.model.PolicyCreateWizardBean;
import com.sun.identity.admin.model.PolicyEditWizardBean;
import com.sun.identity.admin.model.PolicyManageBean;
import com.sun.identity.admin.model.QueuedActionBean;
import com.sun.identity.admin.model.RealmsBean;
import com.sun.identity.admin.model.ReferralCreateWizardBean;
import com.sun.identity.admin.model.ReferralManageBean;
import com.sun.identity.admin.model.ViewApplicationsBean;
import java.io.Serializable;
import javax.faces.application.FacesMessage;
import javax.faces.event.ActionEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;

public class RealmsHandler implements Serializable {

    private RealmsBean realmsBean;
    private QueuedActionBean queuedActionBean;
    private MessagesBean messagesBean;

    public void realmChanged(ValueChangeEvent event) {
        PhaseEventAction pea = new PhaseEventAction();
        pea.setDoBeforePhase(true);
        pea.setPhaseId(PhaseId.RENDER_RESPONSE);
        pea.setAction("#{realmsHandler.handleReset}");
        pea.setParameters(new Class[]{});
        pea.setArguments(new Object[]{});

        queuedActionBean.getPhaseEventActions().add(pea);
    }

    public void realmSelectListener(ActionEvent event) {
        realmsBean.setRealmSelectPopupRealmBean(realmsBean.getRealmBean());
        realmsBean.setRealmSelectPopupVisible(true);
    }

    public void realmSelectPopupOkListener(ActionEvent event) {
        if (realmsBean.getRealmSelectPopupRealmBean() != null) {
            realmsBean.setRealmBean(realmsBean.getRealmSelectPopupRealmBean());
            handleReset();
        } else {
            MessageBean mb = new MessageBean();
            Resources r = new Resources();
            mb.setSummary(r.getString(this, "emptyRealmSummary"));
            mb.setDetail(r.getString(this, "emptyRealmDetail"));
            mb.setSeverity(FacesMessage.SEVERITY_ERROR);
            messagesBean.addMessageBean(mb);
        }


        realmsBean.resetRealmSelectPopup();
        LinkBean.HOME.redirect();
    }

    public void realmSelectPopupCancelListener(ActionEvent event) {
        realmsBean.resetRealmSelectPopup();
    }

    public void handleReset() {
        ViewApplicationsBean vasb = ViewApplicationsBean.getInstance();
        vasb.reset();
        PolicyManageBean pmb = PolicyManageBean.getInstance();
        pmb.reset();
        PolicyCreateWizardBean pcwb = PolicyCreateWizardBean.getInstance();
        pcwb.reset();
        PolicyEditWizardBean pewb = PolicyEditWizardBean.getInstance();
        pewb.reset();
        ReferralCreateWizardBean rcwb = ReferralCreateWizardBean.getInstance();
        rcwb.reset();
        ReferralManageBean rmb = ReferralManageBean.getInstance();
        rmb.reset();
    }

    public void setRealmsBean(RealmsBean realmsBean) {
        this.realmsBean = realmsBean;
    }

    public void setQueuedActionBean(QueuedActionBean queuedActionBean) {
        this.queuedActionBean = queuedActionBean;
    }

    public void setMessagesBean(MessagesBean messagesBean) {
        this.messagesBean = messagesBean;
    }
}
