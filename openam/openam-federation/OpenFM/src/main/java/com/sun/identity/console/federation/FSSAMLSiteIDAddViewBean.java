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
 * $Id: FSSAMLSiteIDAddViewBean.java,v 1.2 2008/06/25 05:49:35 qcheng Exp $
 *
 */

package com.sun.identity.console.federation;

import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.web.ui.model.CCPageTitleModel;
import java.util.Map;
import java.util.Set;

public class FSSAMLSiteIDAddViewBean
    extends FSSAMLSiteIDViewBeanBase {
    public static final String DEFAULT_DISPLAY_URL =
        "/console/federation/FSSAMLSiteIDAdd.jsp";
    
    public FSSAMLSiteIDAddViewBean(String pageName, String defaultDisplayURL) {
        super(pageName, defaultDisplayURL);
    }
    
    public FSSAMLSiteIDAddViewBean() {
        super("FSSAMLSiteIDAdd", DEFAULT_DISPLAY_URL);
    }
    
    protected void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/twoBtnsPageTitle.xml"));
        ptModel.setPageTitleText(getPageTitleText());
        ptModel.setValue("button1", "button.ok");
        ptModel.setValue("button2", "button.cancel");
    }
    
    protected String getPageTitleText() {
        return "saml.profile.siteid.create.page.title";
    }
    
    protected void handleButton1Request(Map values)
    throws AMConsoleException {
        FSSAMLServiceViewBean vb = (FSSAMLServiceViewBean)getViewBean(
            FSSAMLServiceViewBean.class);
        
        Map mapAttrs = (Map)getPageSessionAttribute(
            FSSAMLServiceViewBean.PROPERTY_ATTRIBUTE);
        Set siteIDs = (Set)mapAttrs.get(FSSAMLServiceViewBean.TABLE_SITE_ID);
        FSSAMLSiteID siteIDContainer = new FSSAMLSiteID(siteIDs);
        siteIDContainer.addSiteID(values);
        mapAttrs.put(FSSAMLServiceViewBean.TABLE_SITE_ID,
            siteIDContainer.getValues());
        backTrail();
        setPageSessionAttribute(FSSAMLServiceViewBean.MODIFIED, "1");
        passPgSessionMap(vb);
        vb.setValues();
        vb.unlockPageTrailForSwapping();
        vb.forwardTo(getRequestContext());
    }
    
    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.saml.addSiteId";
    }
    
    protected boolean startPageTrail() {
        return false;
    }
}
