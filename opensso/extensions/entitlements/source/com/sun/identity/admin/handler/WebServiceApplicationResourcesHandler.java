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
 * $Id: WebServiceApplicationResourcesHandler.java,v 1.3 2009/08/13 21:30:22 farble1670 Exp $
 */
package com.sun.identity.admin.handler;

import com.icesoft.faces.component.inputfile.InputFile;
import com.sun.identity.admin.Resources;
import com.sun.identity.admin.dao.ViewApplicationDao;
import com.sun.identity.admin.model.BooleanAction;
import com.sun.identity.admin.model.MessageBean;
import com.sun.identity.admin.model.MessagesBean;
import com.sun.identity.admin.model.UrlResource;
import com.sun.identity.admin.model.ViewApplication;
import com.sun.identity.admin.model.ViewApplicationType;
import com.sun.identity.admin.model.WebServiceApplicationResourcesBean;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.WebServiceApplication;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;
import javax.faces.application.FacesMessage;
import javax.faces.event.ActionEvent;
import javax.faces.event.FacesEvent;

public class WebServiceApplicationResourcesHandler implements Serializable {

    private MessageBean noWsdlFileMessageBean;
    private MessageBean invalidWsdlFileMessageBean;

    private MessagesBean messagesBean;
    private WebServiceApplicationResourcesBean webServiceApplicationResourcesBean;

    public WebServiceApplicationResourcesHandler() {
        noWsdlFileMessageBean = new MessageBean();
        invalidWsdlFileMessageBean = new MessageBean();
    }

    private ViewApplication getViewApplication(FacesEvent event) {
        ViewApplication va = (ViewApplication) event.getComponent().getAttributes().get("viewApplication");
        assert (va != null);

        return va;
    }

    public MessagesBean getMessagesBean() {
        return messagesBean;
    }

    public void setMessagesBean(MessagesBean messagesBean) {
        this.messagesBean = messagesBean;
    }

    public void uploadListener(ActionEvent event) {
        InputFile inputFile = (InputFile) event.getComponent();
        File wsdlFile = inputFile.getFileInfo().getFile();
        webServiceApplicationResourcesBean.setWsdlFile(wsdlFile);

        ViewApplication va = getViewApplication(event);
        initWebServiceApplication(va);
    }

    public void importListener(ActionEvent event) {
        ViewApplication va = getViewApplication(event);
        initWebServiceApplication(va);
    }

    private void initWebServiceApplication(ViewApplication va) {
        ViewApplicationType vat = va.getViewApplicationType();
        Application a = ViewApplicationDao.getInstance().newApplication("dummy", vat);
        WebServiceApplication wsa = (WebServiceApplication) a;
        try {
            if (webServiceApplicationResourcesBean.getLocation().equals("remote")) {
                wsa.initialize(webServiceApplicationResourcesBean.getWsdlUrl());
            } else {
                File wsdlFile = webServiceApplicationResourcesBean.getWsdlFile();
                if (wsdlFile == null) {
                    Resources r = new Resources();
                    noWsdlFileMessageBean.setSummary(r.getString(this, "noWsdlFileSummary"));
                    noWsdlFileMessageBean.setDetail(r.getString(this, "noWsdlFileDetail"));
                    noWsdlFileMessageBean.setSeverity(FacesMessage.SEVERITY_WARN);
                    messagesBean.getPhasedMessageBeans().add(noWsdlFileMessageBean);

                    return;
                }
                InputStream is;
                try {
                    is = new FileInputStream(wsdlFile);
                } catch (IOException ioe) {
                    Resources r = new Resources();
                    invalidWsdlFileMessageBean.setSummary(r.getString(this, "invalidWsdlFileSummary", ioe.getMessage()));
                    invalidWsdlFileMessageBean.setDetail(r.getString(this, "invalidWsdlFileDetail", ioe.getMessage()));
                    invalidWsdlFileMessageBean.setSeverity(FacesMessage.SEVERITY_WARN);
                    messagesBean.getPhasedMessageBeans().add(invalidWsdlFileMessageBean);

                    return;
                }
                wsa.initialize(is);
                messagesBean.getPhasedMessageBeans().remove(noWsdlFileMessageBean);
                messagesBean.getPhasedMessageBeans().remove(invalidWsdlFileMessageBean);
            }
        } catch (EntitlementException ee) {
            MessageBean mb = new MessageBean();
            Resources r = new Resources();
            mb.setSummary(r.getString(this, "invalidWsdlUrlSummary"));
            mb.setDetail(r.getString(this, "invalidWsdlUrlDetail"));
            mb.setSeverity(FacesMessage.SEVERITY_WARN);
            messagesBean.addMessageBean(mb);

            return;
        }

        // resources
        va.getResources().clear();
        for (String s : wsa.getResources()) {
            UrlResource urlResource = new UrlResource();
            urlResource.setName(s);
            va.getResources().add(urlResource);
        }
        if (va.getResources().size() == 0) {
            va.getResources().add(new UrlResource());
        }

        // actions
        va.getBooleanActionsBean().getActions().clear();
        for (Map.Entry<String, Boolean> e : wsa.getActions().entrySet()) {
            BooleanAction action = new BooleanAction();
            action.setName(e.getKey());
            action.setAllow(e.getValue());
            va.getBooleanActionsBean().getActions().add(action);

        }
    }

    public void setWebServiceApplicationResourcesBean(WebServiceApplicationResourcesBean webServiceApplicationResourcesBean) {
        this.webServiceApplicationResourcesBean = webServiceApplicationResourcesBean;
    }
}
