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
 * $Id: BooleanActionsHandler.java,v 1.7 2009/08/05 14:37:15 farble1670 Exp $
 */
package com.sun.identity.admin.handler;

import com.sun.identity.admin.ManagedBeanResolver;
import com.sun.identity.admin.dao.ViewApplicationDao;
import com.sun.identity.admin.model.BooleanAction;
import com.sun.identity.admin.model.BooleanActionsBean;
import com.sun.identity.admin.model.MessageBean;
import com.sun.identity.admin.model.MessagesBean;
import com.sun.identity.admin.model.ViewApplication;
import com.sun.identity.admin.model.ViewApplicationsBean;
import java.io.Serializable;
import javax.faces.application.FacesMessage;
import javax.faces.event.ActionEvent;

public class BooleanActionsHandler
        implements Serializable {

    private BooleanActionsBean booleanActionsBean;

    protected BooleanAction getBooleanAction(ActionEvent event) {
        BooleanAction ba = (BooleanAction) event.getComponent().getAttributes().get("booleanAction");
        assert (ba != null);

        return ba;
    }

    protected ViewApplicationsBean getViewApplicationsBean(ActionEvent event) {
        ViewApplicationsBean vab = (ViewApplicationsBean) event.getComponent().getAttributes().get("viewApplicationsBean");
        assert (vab != null);

        return vab;
    }

    protected ViewApplication getViewApplication(ActionEvent event) {
        ViewApplication va = (ViewApplication)event.getComponent().getAttributes().get("viewApplication");
        return va;
    }

    protected ViewApplicationDao getViewApplicationDao(ActionEvent event) {
        ViewApplicationDao vadao = (ViewApplicationDao) event.getComponent().getAttributes().get("viewApplicationDao");
        assert (vadao != null);

        return vadao;
    }

    public void removeListener(ActionEvent event) {
        BooleanAction ba = getBooleanAction(event);
        booleanActionsBean.getActions().remove(ba);
    }

    public void addListener(ActionEvent event) {
        booleanActionsBean.setAddPopupVisible(!booleanActionsBean.isAddPopupVisible());
    }

    public void addPopupOkListener(ActionEvent event) {
        BooleanAction ba = new BooleanAction();
        ba.setName(booleanActionsBean.getAddPopupName());
        ba.setAllow(true);

        if (booleanActionsBean.getActions().contains(ba)) {
            // TODO: localize
            MessageBean mb = new MessageBean();
            mb.setSummary("Duplicates not allowed");
            mb.setDetail("Duplicate action names are not allowed.");
            mb.setSeverity(FacesMessage.SEVERITY_ERROR);

            ManagedBeanResolver mbr = new ManagedBeanResolver();
            MessagesBean msb = (MessagesBean) mbr.resolve("messagesBean");
            msb.addMessageBean(mb);
        } else {
            booleanActionsBean.getActions().add(ba);

            ViewApplication va = getViewApplication(event);
            if (va != null) {
                va.getBooleanActionsBean().getActions().add(ba);
                getViewApplicationDao(event).setViewApplication(va);
            }
        }

        getViewApplicationsBean(event).reset();
        booleanActionsBean.setAddPopupName(null);
        booleanActionsBean.setAddPopupVisible(false);
    }

    public void addPopupCancelListener(ActionEvent event) {
        booleanActionsBean.setAddPopupName(null);
        booleanActionsBean.setAddPopupVisible(false);
    }

    public void setBooleanActionsBean(BooleanActionsBean booleanActionsBean) {
        this.booleanActionsBean = booleanActionsBean;
    }
}
