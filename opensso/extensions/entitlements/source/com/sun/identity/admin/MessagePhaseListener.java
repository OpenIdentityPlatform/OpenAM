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
 * $Id: MessagePhaseListener.java,v 1.1 2009/08/13 21:30:22 farble1670 Exp $
 */

package com.sun.identity.admin;

import com.sun.identity.admin.model.MessageBean;
import com.sun.identity.admin.model.MessagesBean;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

public class MessagePhaseListener implements PhaseListener {
    public PhaseId getPhaseId() {
        return PhaseId.RENDER_RESPONSE;
    }

    public void beforePhase(PhaseEvent phaseEvent) {
        ManagedBeanResolver mbr = new ManagedBeanResolver();
        MessagesBean messagesBean = (MessagesBean)mbr.resolve("messagesBean");
        FacesContext fc = FacesContext.getCurrentInstance();
        for (MessageBean mb: messagesBean.getPhasedMessageBeans()) {
            fc.addMessage(null, mb.toFacesMessage());
        }
    }

    public void afterPhase(PhaseEvent phaseEvent) {
    }
}
