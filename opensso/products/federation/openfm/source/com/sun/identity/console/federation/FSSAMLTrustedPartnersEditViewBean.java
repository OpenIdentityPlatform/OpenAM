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
 * $Id: FSSAMLTrustedPartnersEditViewBean.java,v 1.3 2008/06/25 05:49:35 qcheng Exp $
 *
 */

package com.sun.identity.console.federation;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.delegation.model.DelegationConfig;
import com.sun.identity.console.federation.SAMLPropertyXMLBuilder;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.html.CCButton;
import com.sun.web.ui.view.alert.CCAlert;
import java.text.MessageFormat;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class FSSAMLTrustedPartnersEditViewBean
    extends FSSAMLTrustedPartnersViewBeanBase {
    public static final String DEFAULT_DISPLAY_URL =
        "/console/federation/FSSAMLTrustedPartnersEdit.jsp";
    private static final String BTN_MODIFY_PROFILE = "btnModifyProfile";
    private static final String TRACKING_VALUES = "trackingValues";
    
    public FSSAMLTrustedPartnersEditViewBean(
        String pageName,
        String defaultDisplayURL
        ) {
        super(pageName, defaultDisplayURL);
    }
    
    protected void registerChildren() {
        super.registerChildren();
        registerChild(BTN_MODIFY_PROFILE, CCButton.class);
    }
    
    public FSSAMLTrustedPartnersEditViewBean() {
        super("FSSAMLTrustedPartnersEdit", DEFAULT_DISPLAY_URL);
    }
    
    protected void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setPageTitleText(getPageTitleText());
        ptModel.setValue("button1", "button.save");
        ptModel.setValue("button2", "button.reset");
        ptModel.setValue("button3", getBackButtonLabel());
    }
    
    protected String getBackButtonLabel() {
        String[] arg = { getModel().getLocalizedString(
            "breadcrumbs.federation.authdomains")};
        return MessageFormat.format(
            getModel().getLocalizedString("back.button"), arg);
    }
    
    public void beginDisplay(DisplayEvent event)
    throws ModelControlException {
        super.beginDisplay(event);
        Map values = (Map)getPageSessionAttribute(TRACKING_VALUES);
        
        if ((values != null) && !values.isEmpty()) {
            setValues(values);
            removePageSessionAttribute(TRACKING_VALUES);
        }
    }
    
    /**
     * Handles reset request.
     *
     * @param event Request Invocation Event.
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        propertySheetModel.clear();
        populateValues = true;
        forwardTo();
    }
    
    public void handleBtnModifyProfileRequest(RequestInvocationEvent event) {
        Map values = new HashMap();
        try {
            getValues(values);
            setPageSessionAttribute(TRACKING_VALUES, (HashMap)values);
            
            FSSAMLSetTrustedPartnerTypeViewBean vb =
                (FSSAMLSetTrustedPartnerTypeViewBean)getViewBean(
                FSSAMLSetTrustedPartnerTypeViewBean.class);
            
            unlockPageTrail();
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
            forwardTo();
        }
    }
    
    protected String getPageTitleText() {
        return "saml.profile.trustedpartners.edit.page.title";
    }
    
    /**
     * Handles save request.
     *
     * @param values to be saved for the entry.
     */
    protected void handleButton1Request(Map values) throws AMConsoleException {
        editEntry(values);
    }
    
    /**
     * Handles back request.
     *
     * @param event Request Invocation Event.
     */
    public void handleButton3Request(RequestInvocationEvent event) {
        forwardToFederationView();
    }
    
    protected boolean isCreateViewBean() {
        return false;
    }
    
    protected boolean createPropertyModel() {
        List profiles = (List)getPageSessionAttribute(PROFILES);
        
        if (profiles != null) {
            DelegationConfig dConfig = DelegationConfig.getInstance();
            String startDN = AMModelBase.getStartDN(
                getRequestContext().getRequest());
            boolean canModify = dConfig.hasPermission(startDN, null,
                AMAdminConstants.PERMISSION_MODIFY,
                getRequestContext().getRequest(), getClass().getName());
            
            SAMLPropertyXMLBuilder builder =
                SAMLPropertyXMLBuilder.getInstance();
            
            // TBD : change to set flag to Readonly (true)
            if (!canModify) {
                builder.setAllAttributeReadOnly(false);
            }
            
            propertySheetModel = new AMPropertySheetModel(
                builder.getXML(profiles, !isCreateViewBean()));
            propertySheetModel.clear();
            return true;
        }
        return false;
    }
    
    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.saml.editTrustedPartner";
    }
    
    protected boolean startPageTrail() {
        return false;
    }
}
