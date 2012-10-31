/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: FSSAMLSetTrustedPartnerTypeViewBean.java,v 1.2 2008/06/25 05:49:34 qcheng Exp $
 *
 */

package com.sun.identity.console.federation;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.federation.model.FSSAMLServiceModelImpl;
import com.sun.identity.console.federation.SAMLProperty;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCButton;
import com.sun.web.ui.view.html.CCCheckBox;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class FSSAMLSetTrustedPartnerTypeViewBean
    extends FSSAMLSelectTrustedPartnerTypeViewBean {
    public static final String DEFAULT_DISPLAY_URL =
        "/console/federation/FSSAMLSetTrustedPartnerType.jsp";
    
    public FSSAMLSetTrustedPartnerTypeViewBean() {
        super("FSSAMLSetTrustedPartnerType");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        createPageTitleModel();
        createPropertyModel();
        registerChildren();
    }
    
    protected void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/twoBtnsPageTitle.xml"));
        ptModel.setPageTitleText(
            "saml.profile.trustedPartners.selectType.page.title");
        ptModel.setValue("button1", "button.ok");
        ptModel.setValue("button2", "button.cancel");
    }
    
    private void createPropertyModel() {
        propertySheetModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/propertyFSSAMLSetTrustedPartnerType.xml"));
        propertySheetModel.clear();
    }
    
    protected AMModel getModelInternal() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        return new FSSAMLServiceModelImpl(req, getPageSessionAttributes());
    }
    
    public void beginDisplay(DisplayEvent event)
    throws ModelControlException {
        super.beginDisplay(event);
        
        List selected = (List)getPageSessionAttribute(
            FSSAMLTrustedPartnersViewBeanBase.PROFILES);
        for (Iterator iter = selected.iterator(); iter.hasNext(); ) {
            SAMLProperty p = (SAMLProperty)iter.next();
            setCheck(p.getRole() + "-" + p.getBindMethod());
        }
        enableButton();
    }
    
    protected void enableButton() {
        CCButton btnBack = (CCButton)getChild("button1");
        btnBack.setDisabled(false);
    }
        
    public void handleButton1Request(RequestInvocationEvent event)
    throws ModelControlException {
        List selected = getSelectedProfile();
        
        if (selected.isEmpty()) {
            setInlineAlertMessage(
                CCAlert.TYPE_ERROR, "message.error",
                "saml.profile.trustedPartner.missing.profile");
            forwardTo();
        } else {
            FSSAMLTrustedPartnersEditViewBean vb =
                (FSSAMLTrustedPartnersEditViewBean)getViewBean(
                FSSAMLTrustedPartnersEditViewBean.class);
            setPageSessionAttribute(FSSAMLTrustedPartnersViewBeanBase.PROFILES,
                (ArrayList)selected);
            backTrail();
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        }
    }
    
    public void handleButton2Request(RequestInvocationEvent event)
    throws ModelControlException {
        FSSAMLTrustedPartnersEditViewBean vb =
            (FSSAMLTrustedPartnersEditViewBean)getViewBean(
            FSSAMLTrustedPartnersEditViewBean.class);
        backTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }
    
    private void setCheck(String childName) {
        CCCheckBox cb = (CCCheckBox)getChild(childName);
        cb.setChecked(true);
    }
}
