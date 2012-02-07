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
 * $Id: ReferralWizardHandler.java,v 1.14 2009/07/13 19:42:42 farble1670 Exp $
 */
package com.sun.identity.admin.handler;

import com.sun.identity.admin.dao.ReferralDao;
import com.sun.identity.admin.model.MessagesBean;
import com.sun.identity.admin.model.QueuedActionBean;
import com.sun.identity.admin.model.RealmBean;
import com.sun.identity.admin.model.ReferralManageBean;
import com.sun.identity.admin.model.ReferralWizardBean;
import java.util.List;
import javax.faces.event.ActionEvent;

public abstract class ReferralWizardHandler extends WizardHandler {

    private MessagesBean messagesBean;
    private QueuedActionBean queuedActionBean;
    private ReferralDao referralDao;
    private ReferralManageBean referralManageBean;

    public void setMessagesBean(MessagesBean messagesBean) {
        this.messagesBean = messagesBean;
    }

    public abstract void doFinishNext();

    public abstract void doCancelNext();

    @Override
    public abstract void finishListener(ActionEvent event);

    @Override
    public void cancelListener(ActionEvent event) {
        getWizardBean().reset();

        doCancelNext();
    }

    public ReferralWizardBean getReferralWizardBean() {
        return (ReferralWizardBean) getWizardBean();
    }

    public abstract String getBeanName();

    public void subjectsAddListener(ActionEvent event) {
        /*
        PhaseEventAction pea = new PhaseEventAction();
        pea.setDoBeforePhase(true);
        pea.setPhaseId(PhaseId.RENDER_RESPONSE);
        pea.setAction("#{" + getBeanName() + ".handleSubjectsAdd}");
        pea.setParameters(new Class[]{});
        pea.setArguments(new Object[]{});

        queuedActionBean.getPhaseEventActions().add(pea);
         */
        handleSubjectsAdd();
    }

    public void handleSubjectsAdd() {
        List<RealmBean> availableValue = getReferralWizardBean().getSelectedAvailableRealmBeans();
        getReferralWizardBean().setSelectedAvailableRealmBeans(null);
        List<RealmBean> available = getReferralWizardBean().getAvailableRealmBeans();
        List<RealmBean> selected = getReferralWizardBean().getReferralBean().getRealmBeans();

        available.removeAll(availableValue);
        selected.addAll(availableValue);
    }

    public void subjectsRemoveListener(ActionEvent event) {
        /*
        PhaseEventAction pea = new PhaseEventAction();
        pea.setDoBeforePhase(true);
        pea.setPhaseId(PhaseId.RENDER_RESPONSE);
        pea.setAction("#{" + getBeanName() + ".handleSubjectsRemove}");
        pea.setParameters(new Class[]{});
        pea.setArguments(new Object[]{});

        queuedActionBean.getPhaseEventActions().add(pea);
         */
        handleSubjectsRemove();
    }

    public void handleSubjectsRemove() {
        List<RealmBean> selectedValue = getReferralWizardBean().getSelectedRealmBeans();
        getReferralWizardBean().setSelectedRealmBeans(null);
        List<RealmBean> available = getReferralWizardBean().getAvailableRealmBeans();
        List<RealmBean> selected = getReferralWizardBean().getReferralBean().getRealmBeans();

        selected.removeAll(selectedValue);
        getReferralWizardBean().resetAvailableRealmBeans();
    }

    public void setQueuedActionBean(QueuedActionBean queuedActionBean) {
        this.queuedActionBean = queuedActionBean;
    }

    public void setReferralDao(ReferralDao referralDao) {
        this.referralDao = referralDao;
    }

    public MessagesBean getMessagesBean() {
        return messagesBean;
    }

    public ReferralDao getReferralDao() {
        return referralDao;
    }

    public void setReferralManageBean(ReferralManageBean referralManageBean) {
        this.referralManageBean = referralManageBean;
    }

    public ReferralManageBean getReferralManageBean() {
        return referralManageBean;
    }
}
