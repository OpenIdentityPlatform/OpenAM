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
 * $Id: FSSAMLTargetURLsEditViewBean.java,v 1.4 2009/10/09 18:29:01 babysunil Exp $
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
import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.web.ui.model.CCPageTitleModel;
import java.util.Iterator;
import java.util.Map;

public class FSSAMLTargetURLsEditViewBean
    extends FSSAMLTargetURLsViewBeanBase {
    public static final String DEFAULT_DISPLAY_URL =
        "/console/federation/FSSAMLTargetURLsEdit.jsp";
    private static final String PGATTR_INDEX = "tblIndex";
    private boolean populateValues = false;
    
    public FSSAMLTargetURLsEditViewBean() {
        super("FSSAMLTargetURLsEdit", DEFAULT_DISPLAY_URL);
    }
    
    void populateValues(String index) {
        setPageSessionAttribute(PGATTR_INDEX, index);
        populateValues = true;
    }
    
    public void beginDisplay(DisplayEvent event)
    throws ModelControlException {
        super.beginDisplay(event);
        
        if (populateValues) {
            int index = Integer.parseInt((String)
            getPageSessionAttribute(PGATTR_INDEX));
            
            Map mapAttrs = (Map)getPageSessionAttribute(
                FSSAMLServiceViewBean.PROPERTY_ATTRIBUTE);
            OrderedSet targets = (OrderedSet)mapAttrs.get(
                FSSAMLServiceViewBean.TABLE_TARGET_URLS);
            setValues((String)targets.get(index));
        }
    }
    
    protected void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setPageTitleText(
            "saml.profile.targetURLs.edit.page.title");
        ptModel.setValue("button1", "button.save");
        ptModel.setValue("button2", "button.reset");
        ptModel.setValue("button3", "button.back");
    }
    
    protected void handleButton1Request(String value)
    throws AMConsoleException {
        FSSAMLServiceViewBean vb = (FSSAMLServiceViewBean)getViewBean(
            FSSAMLServiceViewBean.class);
        
        Map mapAttrs = (Map)getPageSessionAttribute(
            FSSAMLServiceViewBean.PROPERTY_ATTRIBUTE);
        OrderedSet targets = (OrderedSet)mapAttrs.get(
            FSSAMLServiceViewBean.TABLE_TARGET_URLS);
        int index = Integer.parseInt((String)
        getPageSessionAttribute(PGATTR_INDEX));
        int count = 0;
        
        for (Iterator i = targets.iterator(); i.hasNext(); ) {
            String t = (String)i.next();
            if ((count != index) && t.equals(value)) {
                throw new AMConsoleException(
                    "saml.profile.targetURLs.already.exists");
            }
            count++;
        }
        targets.set(index, value);
        backTrail();
        unlockPageTrailForSwapping();
        setPageSessionAttribute(FSSAMLServiceViewBean.MODIFIED, "1");
        passPgSessionMap(vb);
        vb.setValues();
        vb.forwardTo(getRequestContext());
    }
    
    protected void createPropertyModel() {
        DelegationConfig dConfig = DelegationConfig.getInstance();
        String startDN = AMModelBase.getStartDN(
            getRequestContext().getRequest());
        boolean canModify = dConfig.hasPermission(startDN, null,
            AMAdminConstants.PERMISSION_MODIFY,
            getRequestContext().getRequest(), getClass().getName());
        
        // TBD : add readonly xml back
        // "com/sun/identity/console/propertyFSSAMLTargetURLsProfile_Readonly.xml";
        String xmlFile = (canModify) ?
            "com/sun/identity/console/propertyFSSAMLTargetURLsProfile.xml" :
            "com/sun/identity/console/propertyFSSAMLTargetURLsProfile.xml";
        
        propertySheetModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(xmlFile));
        propertySheetModel.clear();
    }
    
    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.saml.editTargetURLs";
    }
    
    protected boolean startPageTrail() {
        return false;
    }

    /**
     * Handles reset request.
     *
     * @param event Request Invocation Event.
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        FSSAMLTargetURLsEditViewBean vb = (FSSAMLTargetURLsEditViewBean)
            getViewBean(FSSAMLTargetURLsEditViewBean.class);
        backTrail();
        unlockPageTrailForSwapping();
        passPgSessionMap(vb);
        populateValues = true;
        vb.forwardTo(getRequestContext());
    }
    
    /**
     * Handles back request.
     *
     * @param event Request Invocation Event.
     */
    public void handleButton3Request(RequestInvocationEvent event) {
        FSSAMLServiceViewBean vb = (FSSAMLServiceViewBean)getViewBean(
            FSSAMLServiceViewBean.class);
        backTrail();
        unlockPageTrailForSwapping();
        passPgSessionMap(vb);
        vb.setValues();
        vb.forwardTo(getRequestContext());
    }
}
