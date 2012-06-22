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
 * $Id: MessagesBean.java,v 1.6 2009/08/13 21:30:23 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.ManagedBeanResolver;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;

public class MessagesBean implements Serializable {
    private String clientId;
    private List<MessageBean> phasedMessageBeans = new ArrayList<MessageBean>();

    public static String getClientId(String componentId) {
        FacesContext context = FacesContext.getCurrentInstance();
        UIViewRoot root = context.getViewRoot();

        UIComponent c = findComponent(root, componentId);
        return c.getClientId(context);
    }

    private static UIComponent findComponent(UIComponent c, String id) {
        if (id.equals(c.getId())) {
            return c;
        }
        Iterator<UIComponent> kids = c.getFacetsAndChildren();
        while (kids.hasNext()) {
            UIComponent found = findComponent(kids.next(), id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    public void setComponentId(String componentId) {
        if (componentId == null || componentId.length() == 0) {
            return;
        }

        if (componentId.equals("_global")) {
            this.clientId = componentId;
        } else {
            this.clientId = getClientId(componentId);
        }
    }

    public List<MessageBean> getMessageBeans() {
        FacesContext context = FacesContext.getCurrentInstance();
        Iterator<FacesMessage> i;
        if (clientId == null) {
            i = context.getMessages();
        } else if (clientId.equals("_global")) {
            i = context.getMessages(null);
        } else {
            i = context.getMessages(clientId);
        }

        List<MessageBean> msgs = new ArrayList<MessageBean>();
        while (i.hasNext()) {
            msgs.add(new MessageBean(i.next()));
        }

        return msgs;
    }

    public boolean isExists() {
        FacesContext fc = FacesContext.getCurrentInstance();
        boolean exists = false;
        if (clientId == null) {
            exists = fc.getMessages().hasNext();
        } else if (clientId.equals("_global")) {
            exists = fc.getMessages(null).hasNext();
        } else {
            exists = fc.getMessages(clientId).hasNext();
        }

        return exists;
    }

    public boolean isExistsError() {
        for (MessageBean mb: getMessageBeans()) {
            if (mb.isError()) {
                return true;
            }
        }
        return false;
    }

    public boolean isExistsWarning() {
        for (MessageBean mb: getMessageBeans()) {
            if (mb.isWarning()) {
                return true;
            }
        }
        return false;
    }

    public boolean isExistsInfo() {
        for (MessageBean mb: getMessageBeans()) {
            if (mb.isInfo()) {
                return true;
            }
        }
        return false;
    }

    public void addMessageBean(MessageBean mb) {
        FacesMessage fm = mb.toFacesMessage();
        FacesContext fc = FacesContext.getCurrentInstance();
        fc.addMessage(null, fm);
    }

    public static MessagesBean getInstance() {
        ManagedBeanResolver mbr = new ManagedBeanResolver();
        MessagesBean msb = (MessagesBean)mbr.resolve("messagesBean");
        return msb;
    }

    public List<MessageBean> getPhasedMessageBeans() {
        return phasedMessageBeans;
    }
}
