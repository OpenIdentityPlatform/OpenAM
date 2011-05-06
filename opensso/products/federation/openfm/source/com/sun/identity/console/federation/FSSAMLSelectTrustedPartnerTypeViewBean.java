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
 * $Id: FSSAMLSelectTrustedPartnerTypeViewBean.java,v 1.2 2008/06/25 05:49:34 qcheng Exp $
 *
 */

package com.sun.identity.console.federation;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.federation.model.FSSAMLServiceModelImpl;
import com.sun.identity.console.federation.SAMLPropertyXMLBuilder;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCButton;
import com.sun.web.ui.view.html.CCCheckBox;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class FSSAMLSelectTrustedPartnerTypeViewBean
    extends AMPrimaryMastHeadViewBean {
    public static final String DEFAULT_DISPLAY_URL =
        "/console/federation/FSSAMLSelectTrustedPartnerType.jsp";
    
    private static final String PGTITLE = "pgtitle";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    
    static final String ATTR_DESTINATION_ARTIFACT = "destination-artifact";
    static final String ATTR_DESTINATION_POST = "destination-post";
    static final String ATTR_SOURCE_ARTIFACT = "source-artifact";
    static final String ATTR_SOURCE_POST = "source-post";
    static final String ATTR_DESTINATION_SOAP = "destination-soap";
    
    protected CCPageTitleModel ptModel;
    protected AMPropertySheetModel propertySheetModel;
    
    public FSSAMLSelectTrustedPartnerTypeViewBean(String name) {
        super(name);
    }
    
    public FSSAMLSelectTrustedPartnerTypeViewBean() {
        super("FSSAMLSelectTrustedPartnerType");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        createPageTitleModel();
        createPropertyModel();
        registerChildren();
    }
    
    protected void registerChildren() {
        super.registerChildren();
        registerChild(PGTITLE, CCPageTitle.class);
        ptModel.registerChildren(this);
        registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
        propertySheetModel.registerChildren(this);
    }
    
    protected View createChild(String name) {
        View view = null;
        if (name.equals(PGTITLE)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else if (name.equals(PROPERTY_ATTRIBUTE)) {
            view = new AMPropertySheet(this, propertySheetModel, name);
        } else if (propertySheetModel.isChildSupported(name)) {
            view = propertySheetModel.createChild(this, name, getModel());
        } else {
            view = super.createChild(name);
        }
        
        return view;
    }
    
    protected void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setPageTitleText(
            "saml.profile.trustedPartners.selectType.page.title");
        ptModel.setValue("button1", "button.previous");
        ptModel.setValue("button2", "button.next");
        ptModel.setValue("button3", "button.cancel");
    }
    
    private void createPropertyModel() {
        propertySheetModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/propertyFSSAMLSelectTrustedPartnerType.xml"));
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
        disableButton();
    }
    
    protected void disableButton() {
        CCButton btnBack = (CCButton)getChild("button1");
        btnBack.setDisabled(true);
    }
    
    /**
     * Handles reset request.
     *
     * @param event Request Invocation Event.
     */
    public void handleButton3Request(RequestInvocationEvent event) {
        FederationViewBean vb = (FederationViewBean)
        getViewBean(FederationViewBean.class);
        backTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }
    
    public void handleButton2Request(RequestInvocationEvent event)
    throws ModelControlException {
        List selected = getSelectedProfile();
        if (selected.isEmpty()) {
            setInlineAlertMessage(
                CCAlert.TYPE_ERROR, "message.error",
                "saml.profile.trustedPartner.missing.profile");
            forwardTo();
        } else {
            FSSAMLTrustedPartnersAddViewBean vb =
                (FSSAMLTrustedPartnersAddViewBean)getViewBean(
                FSSAMLTrustedPartnersAddViewBean.class);
            setPageSessionAttribute(FSSAMLTrustedPartnersViewBeanBase.PROFILES,
                (ArrayList)selected);
            unlockPageTrailForSwapping();
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        }
    }
    
    protected List getSelectedProfile() {
        List selected = new ArrayList();
        if (isChecked(ATTR_DESTINATION_ARTIFACT)) {
            selected.add(SAMLPropertyXMLBuilder.getSAMLProperty(
                SAMLPropertyXMLBuilder.DESTINATION_ARTIFACT));
        }
        if (isChecked(ATTR_DESTINATION_POST)) {
            selected.add(SAMLPropertyXMLBuilder.getSAMLProperty(
                SAMLPropertyXMLBuilder.DESTINATION_POST));
        }
        if (isChecked(ATTR_SOURCE_ARTIFACT)) {
            selected.add(SAMLPropertyXMLBuilder.getSAMLProperty(
                SAMLPropertyXMLBuilder.SOURCE_ARTIFACT));
        }
        if (isChecked(ATTR_SOURCE_POST)) {
            selected.add(SAMLPropertyXMLBuilder.getSAMLProperty(
                SAMLPropertyXMLBuilder.SOURCE_POST));
        }
        if (isChecked(ATTR_DESTINATION_SOAP)) {
            selected.add(SAMLPropertyXMLBuilder.getSAMLProperty(
                SAMLPropertyXMLBuilder.DESTINATION_SOAP));
        }
        return selected;
    }
    
    private boolean isChecked(String childName) {
        CCCheckBox cb = (CCCheckBox)getChild(childName);
        return cb.isChecked();
    }
    
    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.saml.selectPartnerType";
    }
    
    protected boolean startPageTrail() {
        return false;
    }
}
