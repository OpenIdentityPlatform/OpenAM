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
 * $Id: SamlV2HostedCreateWizardHandler.java,v 1.2 2009/12/08 00:02:09 babysunil Exp $
 */
package com.sun.identity.admin.handler;

import com.icesoft.faces.component.dragdrop.DndEvent;
import com.icesoft.faces.component.dragdrop.DropEvent;

import com.sun.identity.admin.Resources;
import com.sun.identity.admin.model.LinkBean;
import com.sun.identity.admin.model.MessagesBean;
import com.sun.identity.admin.model.NextPopupBean;
import com.sun.identity.admin.model.SamlV2ViewAttribute;
import com.sun.identity.admin.model.SamlV2HostedCreateWizardBean;
import com.sun.identity.admin.model.ViewAttribute;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.faces.event.ActionEvent;
import javax.faces.event.FacesEvent;
import javax.faces.event.ValueChangeEvent;


public class SamlV2HostedCreateWizardHandler
        extends WizardHandler
        implements Serializable {

    private MessagesBean messagesBean;

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
        lbs.add(LinkBean.COMMON_TASKS);
        lbs.add(LinkBean.SAMLV2_HOSTED_IDP_CREATE);
        lbs.add(LinkBean.SAMLV2_REMOTE_IDP_CREATE);
        lbs.add(LinkBean.SAMLV2_HOSTED_SP_CREATE);
        lbs.add(LinkBean.SAMLV2_REMOTE_SP_CREATE);
        return lbs;
    }

    private List<LinkBean> getCancelLinkBeans() {
        List<LinkBean> lbs = new ArrayList<LinkBean>();
        lbs.add(LinkBean.HOME);
        lbs.add(LinkBean.COMMON_TASKS);
        lbs.add(LinkBean.SAMLV2_HOSTED_IDP_CREATE);
        lbs.add(LinkBean.SAMLV2_REMOTE_IDP_CREATE);
        lbs.add(LinkBean.SAMLV2_HOSTED_SP_CREATE);
        lbs.add(LinkBean.SAMLV2_REMOTE_SP_CREATE);
        return lbs;
    }

    public void setMessagesBean(MessagesBean messagesBean) {
        this.messagesBean = messagesBean;
    }

    public MessagesBean getMessagesBean() {
        return messagesBean;
    }

     // for attrmapping

    public void dropListener(DropEvent dropEvent) {
        int type = dropEvent.getEventType();
        if (type == DndEvent.DROPPED) {
            Object dragValue = dropEvent.getTargetDragValue();
            assert (dragValue != null);
            ViewAttribute va = (ViewAttribute) dragValue;
            getSamlV2HostedCreateWizardBean().getViewAttributes().add(va);
        }
    }

    protected ViewAttribute getViewAttribute(FacesEvent event) {
        ViewAttribute va = (ViewAttribute) event.getComponent().
                getAttributes().get("viewAttribute");
        assert (va != null);
        return va;
    }

    public void removeListener(ActionEvent event) {
        ViewAttribute va = getViewAttribute(event);
        getSamlV2HostedCreateWizardBean().getViewAttributes().remove(va);
    }

    public void addListener(ActionEvent event) {
        ViewAttribute va = newViewAttribute();
        va.setEditable(true);
        SamlV2ViewAttribute sva = (SamlV2ViewAttribute) va;
        sva.setValueEditable(true);
        sva.setAdded(true);
        getSamlV2HostedCreateWizardBean().getViewAttributes().add(va);
    }

    public void editNameListener(ActionEvent event) {
        ViewAttribute va = (ViewAttribute) getViewAttribute(event);
        va.setNameEditable(true);
    }

    public void nameEditedListener(ValueChangeEvent event) {
        ViewAttribute va = (ViewAttribute) getViewAttribute(event);
        va.setNameEditable(false);
    }

    public void editValueListener(ActionEvent event) {
        SamlV2ViewAttribute sva = (SamlV2ViewAttribute) getViewAttribute(event);
        sva.setValueEditable(true);
    }

    public void valueEditedListener(ValueChangeEvent event) {
        SamlV2ViewAttribute sva = (SamlV2ViewAttribute) getViewAttribute(event);
        sva.setValueEditable(false);
    }

    public ViewAttribute newViewAttribute() {
        return new SamlV2ViewAttribute();
    }

    private SamlV2HostedCreateWizardBean getSamlV2HostedCreateWizardBean() {
        return (SamlV2HostedCreateWizardBean) getWizardBean();
    }
}
