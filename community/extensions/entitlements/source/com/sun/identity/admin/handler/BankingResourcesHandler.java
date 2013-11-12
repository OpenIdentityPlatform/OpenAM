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
 * $Id: BankingResourcesHandler.java,v 1.4 2009/06/04 11:49:12 veiming Exp $
 */

package com.sun.identity.admin.handler;

import com.icesoft.faces.component.selectinputtext.SelectInputText;
import com.sun.identity.admin.ManagedBeanResolver;
import com.sun.identity.admin.Resources;
import com.sun.identity.admin.dao.SubjectDao;
import com.sun.identity.admin.model.BankingResource;
import com.sun.identity.admin.model.BankingResourcesBean;
import com.sun.identity.admin.model.IdRepoUserViewSubject;
import com.sun.identity.admin.model.MessageBean;
import com.sun.identity.admin.model.MessagesBean;
import com.sun.identity.admin.model.SubjectType;
import com.sun.identity.admin.model.ViewEntitlement;
import com.sun.identity.admin.model.ViewSubject;
import java.io.Serializable;
import java.util.List;
import javax.faces.application.FacesMessage;
import javax.faces.event.ActionEvent;
import javax.faces.event.FacesEvent;
import javax.faces.event.ValueChangeEvent;

public class BankingResourcesHandler implements Serializable {

    private BankingResourcesBean bankingResourcesBean;
    private SubjectDao subjectDao = null;
    private SubjectType subjectType;
    private MessagesBean messagesBean;

    private BankingResource getBankingResource(FacesEvent event) {
        BankingResource br = (BankingResource) event.getComponent().getAttributes().get("bankingResource");
        assert (br != null);

        return br;
    }

    private ViewEntitlement getViewEntitlement(FacesEvent event) {
        ViewEntitlement ve = (ViewEntitlement) event.getComponent().getAttributes().get("viewEntitlement");
        assert (ve != null);

        return ve;
    }

    public void addListener(ActionEvent event) {
        List<ViewSubject> viewSubjects = subjectDao.getViewSubjects();
        bankingResourcesBean.setViewSubjects(viewSubjects);

        ViewEntitlement ve = getViewEntitlement(event);
        if (ve.getResources().contains(BankingResource.ALL_ACCOUNTS)) {
            bankingResourcesBean.setAllAccounts(true);
        }
        bankingResourcesBean.setAddPopupVisible(true);
    }

    public void removeListener(ActionEvent event) {
        BankingResource br = getBankingResource(event);
        ViewEntitlement ve = getViewEntitlement(event);

        ve.getResources().remove(br);
    }

    public void addPopupOkListener(ActionEvent event) {
        ViewEntitlement ve = getViewEntitlement(event);
        IdRepoUserViewSubject idus = (IdRepoUserViewSubject) bankingResourcesBean.getAddPopupViewSubject();

        if (bankingResourcesBean.isAllAccounts()) {
            BankingResource br = BankingResource.ALL_ACCOUNTS;
            ve.getResources().clear();
            ve.getResources().add(br);

            bankingResourcesBean.reset();
        } else {
            ve.getResources().remove(BankingResource.ALL_ACCOUNTS);

            if (idus == null) {
                MessageBean mb = new MessageBean();
                mb.setSummary("No match");
                mb.setDetail("No matching account found");
                mb.setSeverity(FacesMessage.SEVERITY_ERROR);
                ManagedBeanResolver mbr = new ManagedBeanResolver();
                MessagesBean msb = (MessagesBean) mbr.resolve("messagesBean");
                msb.addMessageBean(mb);
            } else {
                BankingResource br = new BankingResource();
                br.setName(idus.getEmployeeNumber());
                br.setOwner(idus);
                if (!ve.getResources().contains(br)) {
                    ve.getResources().add(br);
                    bankingResourcesBean.reset();
                } else {
                    MessageBean mb = new MessageBean();
                    Resources r = new Resources();
                    mb.setSummary(r.getString(this, "noDuplicateSummary"));
                    mb.setDetail(r.getString(this, "noDuplicateDetail"));
                    mb.setSeverity(FacesMessage.SEVERITY_ERROR);
                    messagesBean.addMessageBean(mb);
                    bankingResourcesBean.setAddPopupVisible(false);
                }
            }
        }
    }

    public void addPopupCancelListener(ActionEvent event) {
        bankingResourcesBean.reset();
    }

    public void setBankingResourcesBean(BankingResourcesBean bankingResourcesBean) {
        this.bankingResourcesBean = bankingResourcesBean;
    }

    public void allAccountsListener(ValueChangeEvent event) {
        Boolean val = (Boolean) event.getNewValue();
        bankingResourcesBean.setAddPopupAccountNumber(null);
        bankingResourcesBean.setAddPopupViewSubject(null);
    }

    public void addPopupAccountNumberChangedListener(ValueChangeEvent event) {
        if (event.getComponent() instanceof SelectInputText) {
            SelectInputText ac = (SelectInputText) event.getComponent();
            String newWord = (String) event.getNewValue();
            List<ViewSubject> viewSubjects = subjectDao.getViewSubjects(newWord);
            bankingResourcesBean.setViewSubjects(viewSubjects);

            if (ac.getSelectedItem() != null) {
                ViewSubject vs = (ViewSubject) ac.getSelectedItem().getValue();
                bankingResourcesBean.setAddPopupViewSubject(vs);
            } else {
                if (viewSubjects.size() == 1) {
                    bankingResourcesBean.setAddPopupViewSubject(viewSubjects.get(0));
                } else {
                    bankingResourcesBean.setAddPopupViewSubject(null);
                }
            }
        }
    }

    public void setSubjectDao(SubjectDao subjectDao) {
        this.subjectDao = subjectDao;
    }

    public SubjectType getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(SubjectType subjectType) {
        this.subjectType = subjectType;
    }

    public void setMessagesBean(MessagesBean messagesBean) {
        this.messagesBean = messagesBean;
    }
}
