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
 * $Id: DelegationManageHandler.java,v 1.2 2009/12/22 23:33:14 farble1670 Exp $
 */
package com.sun.identity.admin.handler;

import com.sun.identity.admin.Resources;
import com.sun.identity.admin.dao.DelegationDao;
import com.sun.identity.admin.model.MessageBean;
import com.sun.identity.admin.model.MessagesBean;
import com.sun.identity.admin.model.PhaseEventAction;
import com.sun.identity.admin.model.FilterHolder;
import com.sun.identity.admin.model.DelegationManageBean;
import com.sun.identity.admin.model.DelegationBean;
import com.sun.identity.admin.model.QueuedActionBean;
import com.sun.identity.admin.model.ViewApplicationsBean;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.faces.application.FacesMessage;
import javax.faces.event.ActionEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;

public class DelegationManageHandler implements Serializable {

    private DelegationManageBean delegationManageBean;
    private QueuedActionBean queuedActionBean;
    private DelegationDao delegationDao;
    private MessagesBean messagesBean;

    public DelegationBean getDelegationBean(ActionEvent event) {
        DelegationBean db = (DelegationBean) event.getComponent().getAttributes().get("delegationBean");
        assert (db != null);
        return db;
    }

    public FilterHolder getFilterHolder(ActionEvent event) {
        FilterHolder fh = (FilterHolder) event.getComponent().getAttributes().get("filterHolder");
        assert (fh != null);
        return fh;
    }

    public DelegationManageBean getDelegationManageBean() {
        return delegationManageBean;
    }

    public void setDelegationManageBean(DelegationManageBean delegationManageBean) {
        this.delegationManageBean = delegationManageBean;
    }

    public void sortTableListener(ActionEvent event) {
        PhaseEventAction pea = new PhaseEventAction();
        pea.setDoBeforePhase(true);
        pea.setPhaseId(PhaseId.RENDER_RESPONSE);
        pea.setAction("#{delegationManageHandler.handleSort}");
        pea.setParameters(new Class[]{});
        pea.setArguments(new Object[]{});

        queuedActionBean.getPhaseEventActions().add(pea);
    }

    private void addResetEvent() {
        PhaseEventAction pea = new PhaseEventAction();
        pea.setDoBeforePhase(true);
        pea.setPhaseId(PhaseId.RENDER_RESPONSE);
        pea.setAction("#{delegationManageHandler.handleReset}");
        pea.setParameters(new Class[]{});
        pea.setArguments(new Object[]{});

        queuedActionBean.getPhaseEventActions().add(pea);
    }

    public void handleSort() {
        delegationManageBean.getDelegationManageTableBean().sort();
    }

    public void handleReset() {
        delegationManageBean.reset();
    }

    public void viewOptionsListener(ActionEvent event) {
        delegationManageBean.getViewOptionsPopupColumnsVisible().clear();
        delegationManageBean.getViewOptionsPopupColumnsVisible().addAll(delegationManageBean.getDelegationManageTableBean().getColumnsVisible());
        delegationManageBean.setViewOptionsPopupRows(delegationManageBean.getDelegationManageTableBean().getRows());

        delegationManageBean.setViewOptionsPopupVisible(true);
    }

    public void viewOptionsPopupOkListener(ActionEvent event) {
        delegationManageBean.getDelegationManageTableBean().getColumnsVisible().clear();
        delegationManageBean.getDelegationManageTableBean().getColumnsVisible().addAll(delegationManageBean.getViewOptionsPopupColumnsVisible());
        delegationManageBean.getDelegationManageTableBean().setRows(delegationManageBean.getViewOptionsPopupRows());

        delegationManageBean.setViewOptionsPopupVisible(false);
    }

    public void viewOptionsPopupCancelListener(ActionEvent event) {
        delegationManageBean.setViewOptionsPopupVisible(false);
    }

    public void removePopupOkListener(ActionEvent event) {
        PhaseEventAction pea = new PhaseEventAction();
        pea.setDoBeforePhase(true);
        pea.setPhaseId(PhaseId.RENDER_RESPONSE);
        pea.setAction("#{delegationManageHandler.handleRemoveAction}");
        pea.setParameters(new Class[]{});
        pea.setArguments(new Object[]{});

        queuedActionBean.getPhaseEventActions().add(pea);

        delegationManageBean.setRemovePopupVisible(false);
    }

    public void selectAllListener(ActionEvent event) {
        selectListener(event, true);
    }

    public void selectNoneListener(ActionEvent event) {
        selectListener(event, false);
    }

    private void selectListener(ActionEvent event, boolean select) {
        int size = delegationManageBean.getDelegationBeans().size();
        int first = delegationManageBean.getDataPaginator().getFirstRow();
        int rows = delegationManageBean.getDataPaginator().getRows();
        int last = Math.min(first+rows, size);

        for (int i = first; i < last; i++) {
            DelegationBean db = delegationManageBean.getDelegationBeans().get(i);
            db.setSelected(select);
        }
    }

    public void removePopupCancelListener(ActionEvent event) {
        delegationManageBean.setRemovePopupVisible(false);
    }

    public void addViewFilterListener(ActionEvent event) {
        delegationManageBean.newFilterHolder();
        addResetEvent();
    }

    public void viewFilterChangedListener(ValueChangeEvent event) {
        addResetEvent();
    }

    public void viewFilterChangedListener(ActionEvent event) {
        addResetEvent();
    }

    public void removeViewFilterListener(ActionEvent event) {
        FilterHolder fh = getFilterHolder(event);
        delegationManageBean.getFilterHolders().remove(fh);
        addResetEvent();
    }

    public void removeListener(ActionEvent event) {
        if (!delegationManageBean.isRemovePopupVisible()) {
            if (delegationManageBean.getSizeSelected() == 0) {
                MessageBean mb = new MessageBean();
                Resources r = new Resources();
                // TODO: add res strings
                mb.setSummary(r.getString(this, "removeNoneSelectedSummary"));
                mb.setDetail(r.getString(this, "removeNoneSelectedDetail"));
                mb.setSeverity(FacesMessage.SEVERITY_ERROR);
                messagesBean.addMessageBean(mb);
            } else {
                delegationManageBean.setRemovePopupVisible(true);
            }
        } else {
            delegationManageBean.setRemovePopupVisible(false);
        }
    }

    public void handleRemoveAction() {
        Set<DelegationBean> removed = new HashSet<DelegationBean>();
        for (DelegationBean db : delegationManageBean.getDelegationBeans()) {
            if (db.isSelected()) {
                removed.add(db);
                delegationDao.remove(db.getName());
            }
            db.setSelected(false);
        }
        delegationManageBean.getDelegationBeans().removeAll(removed);
    }

    public void setQueuedActionBean(QueuedActionBean queuedActionBean) {
        this.queuedActionBean = queuedActionBean;
    }

    public void setDelegationDao(DelegationDao delegationDao) {
        this.delegationDao = delegationDao;
    }

    public void setMessagesBean(MessagesBean messagesBean) {
        this.messagesBean = messagesBean;
    }

    public String manageAction() {
        ViewApplicationsBean vasb = ViewApplicationsBean.getInstance();
        if (vasb.getViewApplications() == null || vasb.getViewApplications().size() == 0) {
            MessageBean mb = new MessageBean();
            Resources r = new Resources();
            mb.setSummary(r.getString(this, "noApplicationsSummary"));
            mb.setDetail(r.getString(this, "noApplicationsDetail"));
            mb.setSeverity(FacesMessage.SEVERITY_ERROR);
            messagesBean.addMessageBean(mb);

            return null;
        } else {
            return "delegation-manage";
        }
    }
}

