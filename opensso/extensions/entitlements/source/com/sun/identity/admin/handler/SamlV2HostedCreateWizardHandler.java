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
 * $Id: SamlV2HostedCreateWizardHandler.java,v 1.5 2009/07/02 01:02:44 asyhuang Exp $
 */
package com.sun.identity.admin.handler;

import com.sun.identity.admin.Resources;
import com.sun.identity.admin.model.LinkBean;
import com.sun.identity.admin.model.MessagesBean;
import com.sun.identity.admin.model.NextPopupBean;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
}
