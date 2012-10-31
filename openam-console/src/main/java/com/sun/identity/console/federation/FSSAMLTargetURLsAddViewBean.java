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
 * $Id: FSSAMLTargetURLsAddViewBean.java,v 1.3 2008/06/25 05:49:35 qcheng Exp $
 *
 */

package com.sun.identity.console.federation;

import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.model.ModelControlException;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.web.ui.model.CCPageTitleModel;
import java.util.Map;
import java.util.Set;

public class FSSAMLTargetURLsAddViewBean
    extends FSSAMLTargetURLsViewBeanBase {
    public static final String DEFAULT_DISPLAY_URL =
        "/console/federation/FSSAMLTargetURLsAdd.jsp";
    
    public FSSAMLTargetURLsAddViewBean(
        String pageName,
        String defaultDisplayURL
        ) {
        super(pageName, defaultDisplayURL);
    }
    
    public FSSAMLTargetURLsAddViewBean() {
        super("FSSAMLTargetURLsAdd", DEFAULT_DISPLAY_URL);
    }
    
    public void beginDisplay(DisplayEvent event)
    throws ModelControlException {
        String protocol = (String)getDisplayFieldValue(ATTR_PROTOCOL);
        
        if ((protocol == null) || (protocol.length() == 0)) {
            setDisplayFieldValue(ATTR_PROTOCOL, "http");
        }
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
        return "saml.profile.targetURLs.create.page.title";
    }
    
    protected void handleButton1Request(String value)
    throws AMConsoleException {
        FSSAMLServiceViewBean vb = (FSSAMLServiceViewBean)getViewBean(
            FSSAMLServiceViewBean.class);
        
        Map mapAttrs = (Map)getPageSessionAttribute(
            FSSAMLServiceViewBean.PROPERTY_ATTRIBUTE);
        Set targetURLs = (Set)mapAttrs.get(
            FSSAMLServiceViewBean.TABLE_TARGET_URLS);
        
        if ((targetURLs == null) || targetURLs.isEmpty()) {
            targetURLs = new OrderedSet();
            mapAttrs.put(FSSAMLServiceViewBean.TABLE_TARGET_URLS,
                (OrderedSet)targetURLs);
        }
        
        if (targetURLs.contains(value)) {
            throw new AMConsoleException(
                "saml.profile.targetURLs.already.exists");
        }
        
        targetURLs.add(value);
        backTrail();
        unlockPageTrailForSwapping();
        setPageSessionAttribute(FSSAMLServiceViewBean.MODIFIED, "1");
        passPgSessionMap(vb);
        vb.setValues();
        vb.forwardTo(getRequestContext());
    }
    
    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.saml.addTargetURLs";
    }
    
    protected boolean startPageTrail() {
        return false;
    }
}
