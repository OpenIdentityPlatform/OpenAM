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
 * $Id: UrlApplicationResourcesHandler.java,v 1.1 2009/08/04 18:50:45 farble1670 Exp $
 */

package com.sun.identity.admin.handler;

import com.sun.identity.admin.model.MessagesBean;
import com.sun.identity.admin.model.UrlApplicationResourcesBean;
import com.sun.identity.admin.model.UrlResource;
import com.sun.identity.admin.model.ViewApplication;
import com.sun.identity.admin.model.ViewEntitlement;
import java.io.Serializable;
import javax.faces.event.ActionEvent;
import javax.faces.event.FacesEvent;

public class UrlApplicationResourcesHandler implements Serializable {

    private MessagesBean messagesBean;
    private UrlApplicationResourcesBean urlApplicationResourcesBean;

    private UrlResource getUrlResource(FacesEvent event) {
        UrlResource ur = (UrlResource) event.getComponent().getAttributes().get("urlResource");
        assert (ur != null);

        return ur;
    }

    private ViewApplication getViewApplication(FacesEvent event) {
        ViewApplication va = (ViewApplication) event.getComponent().getAttributes().get("viewApplication");
        assert (va != null);

        return va;
    }

    public void addListener(ActionEvent event) {
        ViewApplication va = getViewApplication(event);
        va.getResources().add(new UrlResource());
    }

    public void removeListener(ActionEvent event) {
        ViewApplication va = getViewApplication(event);
        UrlResource ur = getUrlResource(event);
        va.getResources().remove(ur);
    }

    private void resetAddPopup() {
        urlApplicationResourcesBean.setAddPopupVisible(false);
        urlApplicationResourcesBean.setAddPopupResource(null);
    }

    public void addPopupOkListener(ActionEvent event) {
        // TODO
        resetAddPopup();
    }

    public void addPopupCancelListener(ActionEvent event) {
        resetAddPopup();
    }

    public void setMessagesBean(MessagesBean messagesBean) {
        this.messagesBean = messagesBean;
    }

    public UrlApplicationResourcesBean getUrlApplicationResourcesBean() {
        return urlApplicationResourcesBean;
    }

    public void setUrlApplicationResourcesBean(UrlApplicationResourcesBean urlApplicationResourcesBean) {
        this.urlApplicationResourcesBean = urlApplicationResourcesBean;
    }
}
