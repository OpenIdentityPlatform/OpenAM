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
 * $Id: UrlResourcesHandler.java,v 1.19 2009/06/04 11:49:13 veiming Exp $
 */

package com.sun.identity.admin.handler;

import com.sun.identity.admin.Resources;
import com.sun.identity.admin.model.MessageBean;
import com.sun.identity.admin.model.MessagesBean;
import com.sun.identity.admin.model.Resource;
import com.sun.identity.admin.model.UrlResource;
import com.sun.identity.admin.model.UrlResourceParts;
import com.sun.identity.admin.model.UrlResourcesBean;
import com.sun.identity.admin.model.ViewEntitlement;
import com.sun.identity.entitlement.ValidateResourceResult;
import java.io.Serializable;
import java.util.List;
import javax.faces.application.FacesMessage;
import javax.faces.event.ActionEvent;
import javax.faces.event.FacesEvent;
import javax.faces.event.ValueChangeEvent;

public class UrlResourcesHandler implements Serializable {

    private UrlResourcesBean urlResourcesBean;
    private MessagesBean messagesBean;

    private UrlResource getUrlResource(FacesEvent event) {
        UrlResource ur = (UrlResource) event.getComponent().getAttributes().get("urlResource");
        assert (ur != null);

        return ur;
    }

    private ViewEntitlement getViewEntitlement(FacesEvent event) {
        ViewEntitlement ve = (ViewEntitlement) event.getComponent().getAttributes().get("viewEntitlement");
        assert (ve != null);

        return ve;
    }

    private List<Resource> getAvailableResources(FacesEvent event) {
        List<Resource> ar = (List<Resource>) event.getComponent().getAttributes().get("availableResources");
        assert (ar != null);

        return ar;
    }

    public void selectListener(ValueChangeEvent event) {
        ViewEntitlement ve = getViewEntitlement(event);
        /*
        List<Resource> resources = (List<Resource>) event.getNewValue();
        ve.setResources(resources);
         */
        Resource[] resourceArray = (Resource[]) event.getNewValue();
        ve.setResourceArray(resourceArray);
    }

    public void addListener(ActionEvent event) {
        List<Resource> ar = getAvailableResources(event);
        urlResourcesBean.setAddPopupAvailableResources(ar);

        if (urlResourcesBean.getAddPopupAvailableResources().size() > 0) {
            urlResourcesBean.setAddPopupVisible(true);

            UrlResource ur = (UrlResource) urlResourcesBean.getAddPopupAvailableResources().get(0);
            urlResourcesBean.setAddPopupResource(ur);
            urlResourcesBean.setAddPopupUrlResourceParts(ur.getUrlResourceParts());
        } else {
            MessageBean mb = new MessageBean();
            Resources r = new Resources();
            mb.setSummary(r.getString(this, "noAddSummary"));
            mb.setDetail(r.getString(this, "noAddDetail"));
            mb.setSeverity(FacesMessage.SEVERITY_WARN);
            messagesBean.addMessageBean(mb);
        }
    }

    private void resetAddPopup() {
        urlResourcesBean.setAddPopupResource(null);
        urlResourcesBean.setAddPopupUrlResourceParts(null);
        urlResourcesBean.setAddPopupVisible(false);
    }

    public void addPopupOkListener(ActionEvent event) {
        ViewEntitlement ve = getViewEntitlement(event);
        UrlResourceParts urps = urlResourcesBean.getAddPopupUrlResourceParts();
        if (!urps.isValid()) {
            MessageBean mb = new MessageBean();
            Resources r = new Resources();
            mb.setSummary(r.getString(this, "invalidPartsSummary"));
            mb.setDetail(r.getString(this, "invalidPartsDetail"));
            mb.setSeverity(FacesMessage.SEVERITY_ERROR);
            messagesBean.addMessageBean(mb);
            urlResourcesBean.setAddPopupVisible(false);
            return;
        }

        UrlResource ur = urps.getUrlResource();
        ValidateResourceResult vrr = ve.validateResource(ur);
        
        if (!vrr.isValid()) {
            MessageBean mb = new MessageBean();
            Resources r = new Resources();
            mb.setSummary(r.getString(this, "invalidResourceSummary"));
            mb.setDetail(vrr.getLocalizedMessage(r.getLocale()));
            mb.setSeverity(FacesMessage.SEVERITY_ERROR);
            messagesBean.addMessageBean(mb);
            urlResourcesBean.setAddPopupVisible(false);            
        } else if (!ve.getResources().contains(ur)) {
            ve.getResources().add(ur);
            List<Resource> ar = getAvailableResources(event);
            if (!ar.contains(ur)) {
                ar.add(ur);
            }

            resetAddPopup();
        } else {
            MessageBean mb = new MessageBean();
            Resources r = new Resources();
            mb.setSummary(r.getString(this, "noDuplicateSummary"));
            mb.setDetail(r.getString(this, "noDuplicateDetail"));
            mb.setSeverity(FacesMessage.SEVERITY_ERROR);
            messagesBean.addMessageBean(mb);
            urlResourcesBean.setAddPopupVisible(false);
        }
    }

    public void addExceptionPopupOkListener(ActionEvent event) {
        String name = urlResourcesBean.getAddExceptionPopupName();
        if (name == null || name.length() == 0) {
            MessageBean mb = new MessageBean();
            Resources r = new Resources();
            mb.setSummary(r.getString(this, "emptyExceptionNameSummary"));
            mb.setDetail(r.getString(this, "emptyExceptionNameDetail"));
            mb.setSeverity(FacesMessage.SEVERITY_ERROR);
            messagesBean.addMessageBean(mb);
            urlResourcesBean.setAddExceptionPopupVisible(false);
            return;
        }

        String prefix = urlResourcesBean.getAddExceptionPopupResource().getExceptionPrefix();

        UrlResource ur = new UrlResource();
        ur.setName(prefix + name);

        ViewEntitlement ve = getViewEntitlement(event);
        ValidateResourceResult vrr = ve.validateResource(ur);

        if (!vrr.isValid()) {
            MessageBean mb = new MessageBean();
            Resources r = new Resources();
            mb.setSummary(r.getString(this, "invalidExceptionSummary"));
            mb.setDetail(vrr.getLocalizedMessage(r.getLocale()));
            mb.setSeverity(FacesMessage.SEVERITY_ERROR);
            messagesBean.addMessageBean(mb);
            urlResourcesBean.setAddPopupVisible(false);
        } else if (!ve.getExceptions().contains(ur)) {
            ve.getExceptions().add(ur);

            urlResourcesBean.setAddExceptionPopupName(null);
            urlResourcesBean.setAddExceptionPopupVisible(false);
        } else {
            MessageBean mb = new MessageBean();
            Resources r = new Resources();
            mb.setSummary(r.getString(this, "noDuplicateExceptionSummary"));
            mb.setDetail(r.getString(this, "noDuplicateExceptionDetail"));
            mb.setSeverity(FacesMessage.SEVERITY_ERROR);
            messagesBean.addMessageBean(mb);
            urlResourcesBean.setAddExceptionPopupVisible(false);
        }
    }

    public void removeExceptionListener(ActionEvent event) {
        UrlResource ur = getUrlResource(event);
        ViewEntitlement ve = getViewEntitlement(event);

        ve.getExceptions().remove(ur);
    }

    public void addPopupCancelListener(ActionEvent event) {
        resetAddPopup();
    }

    public void addExceptionPopupCancelListener(ActionEvent event) {
        urlResourcesBean.setAddExceptionPopupVisible(false);
    }

    public void addExceptionListener(ActionEvent event) {
        UrlResource ur = getUrlResource(event);
        urlResourcesBean.setAddExceptionPopupResource(ur);
        urlResourcesBean.setAddExceptionPopupVisible(true);
    }

    public void setUrlResourcesBean(UrlResourcesBean urlResourcesBean) {
        this.urlResourcesBean = urlResourcesBean;
    }

    public void searchFilterChangedListener(ValueChangeEvent event) {
        String searchFilter = (String) event.getNewValue();
        List<Resource> availableResources = getViewEntitlement(event).getAvailableResources();

        for (Resource r : availableResources) {
            if (!r.getName().contains(searchFilter)) {
                r.setVisible(false);
            } else {
                r.setVisible(true);
            }
        }
    }

    public void addPopupResourceChangedListener(ValueChangeEvent event) {
        String addPopupResourceName = (String) event.getNewValue();
        if (addPopupResourceName != null) {
            UrlResource addPopupResource = new UrlResource();
            addPopupResource.setName(addPopupResourceName);

            UrlResourceParts urp = new UrlResourceParts(addPopupResource);
            urlResourcesBean.setAddPopupUrlResourceParts(urp);
        }
    }

    public void setMessagesBean(MessagesBean messagesBean) {
        this.messagesBean = messagesBean;
    }
}
