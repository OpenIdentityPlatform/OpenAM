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
 * $Id: ApplicationWizardHandler.java,v 1.2 2009/08/10 15:18:37 farble1670 Exp $
 */

package com.sun.identity.admin.handler;

import com.sun.identity.admin.dao.ViewApplicationDao;
import com.sun.identity.admin.model.ApplicationWizardBean;
import com.sun.identity.admin.model.MessagesBean;
import com.sun.identity.admin.model.QueuedActionBean;
import com.sun.identity.admin.model.ViewApplication;
import javax.faces.event.ActionEvent;

public abstract class ApplicationWizardHandler extends WizardHandler {
    private QueuedActionBean queuedActionBean;
    private MessagesBean messagesBean;

    public void setQueuedActionBean(QueuedActionBean queuedActionBean) {
        this.queuedActionBean = queuedActionBean;
    }

    public void setMessagesBean(MessagesBean messagesBean) {
        this.messagesBean = messagesBean;
    }

    public abstract void doFinishNext();

    public abstract void doCancelNext();

    public ApplicationWizardBean getApplicationWizardBean() {
        return (ApplicationWizardBean)getWizardBean();
    }

    @Override
    public void finishListener(ActionEvent event) {
        if (!validateFinish(event)) {
            return;
        }

        ViewApplication va = getApplicationWizardBean().getViewApplication();
        ViewApplicationDao.getInstance().setViewApplication(va);

        doFinishNext();
    }

    @Override
    public void cancelListener(ActionEvent event) {
        doCancelNext();
    }
}
