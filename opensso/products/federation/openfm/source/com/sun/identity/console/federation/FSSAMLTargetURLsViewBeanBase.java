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
 * $Id: FSSAMLTargetURLsViewBeanBase.java,v 1.2 2008/06/25 05:49:35 qcheng Exp $
 *
 */

package com.sun.identity.console.federation;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.federation.model.FSSAMLServiceModelImpl;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.net.MalformedURLException;
import java.net.URL;
import javax.servlet.http.HttpServletRequest;

public abstract class FSSAMLTargetURLsViewBeanBase
    extends AMPrimaryMastHeadViewBean {
    private static Debug debug = Debug.getInstance(
        AMAdminConstants.CONSOLE_DEBUG_FILENAME);
    
    private static final String PGTITLE = "pgtitle";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    
    static final String ATTR_TARGET = "targetURLs-target";
    static final String ATTR_PROTOCOL = "targetURLs-protocol";
    static final String ATTR_PORT = "targetURLs-port";
    static final String ATTR_PATH = "targetURLs-path";
    
    protected CCPageTitleModel ptModel;
    protected AMPropertySheetModel propertySheetModel;
    
    public FSSAMLTargetURLsViewBeanBase(
        String pageName,
        String defaultDisplayURL
        ) {
        super(pageName);
        setDefaultDisplayURL(defaultDisplayURL);
    }
    
    protected void initialize() {
        if (!initialized) {
            super.initialize();
            initialized = true;
            createPageTitleModel();
            createPropertyModel();
            registerChildren();
        }
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
    
    protected void createPropertyModel() {
        propertySheetModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/propertyFSSAMLTargetURLsProfile.xml")
            );
        propertySheetModel.clear();
    }
    
    protected AMModel getModelInternal() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        return new FSSAMLServiceModelImpl(req, getPageSessionAttributes());
    }
    
    /**
     * Handles reset request.
     *
     * @param event Request Invocation Event.
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        FSSAMLServiceViewBean vb = (FSSAMLServiceViewBean)getViewBean(
            FSSAMLServiceViewBean.class);
        backTrail();
        unlockPageTrailForSwapping();
        passPgSessionMap(vb);
        vb.setValues();
        vb.forwardTo(getRequestContext());
    }
    
    protected void setValues(String value) {
        try {
            URL url = new URL(value);
            propertySheetModel.setValue(ATTR_PROTOCOL, url.getProtocol());
            propertySheetModel.setValue(ATTR_TARGET, url.getHost());
            propertySheetModel.setValue(ATTR_PATH, url.getPath());
            int port = url.getPort();
            
            if (port == -1) {
                propertySheetModel.setValue(ATTR_PORT, "");
            } else {
                propertySheetModel.setValue(ATTR_PORT, Integer.toString(port));
            }
        } catch (MalformedURLException e) {
            debug.warning("FSSAMLTargetURLsViewBeanBase.setValues", e);
        }
    }
    
    private boolean getValues(String[] results) {
        boolean ok = false;
        String protocol =
            ((String)propertySheetModel.getValue(ATTR_PROTOCOL)).trim();
        String target =
            ((String)propertySheetModel.getValue(ATTR_TARGET)).trim();
        String port =
            ((String)propertySheetModel.getValue(ATTR_PORT)).trim();
        String path =
            ((String)propertySheetModel.getValue(ATTR_PATH)).trim();
        
        String errorMsg = null;
        StringBuffer buff = new StringBuffer(200);
        buff.append(protocol)
        .append("://")
        .append(target);
        
        if (port.length() > 0) {
            buff.append(":")
            .append(port);
        }
        
        if (path.length() > 0) {
            if (path.charAt(0) == '/') {
                buff.append(path);
            } else {
                buff.append("/")
                .append(path);
            }
        }
        
        String url = buff.toString();
        
        try {
            new URL(url);
            results[0] = url;
            results[1] = null;
            
            if ((protocol.length() == 0) || (target.length() == 0)) {
                results[1] = "saml.profile.targetURLs.incorrect.url";
            } else {
                ok = true;
            }
        } catch (MalformedURLException e) {
            results[1] = "saml.profile.targetURLs.incorrect.url";
        }
        
        return ok;
    }
    
    public void handleButton1Request(RequestInvocationEvent event)
    throws ModelControlException {
        String[] results = new String[2];
        
        if (getValues(results)) {
            try {
                handleButton1Request(results[0]);
            } catch (AMConsoleException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
                forwardTo();
            }
        } else {
            setInlineAlertMessage(
                CCAlert.TYPE_ERROR, "message.error", results[1]);
            forwardTo();
        }
    }
    
    protected abstract void createPageTitleModel();
    protected abstract void handleButton1Request(String value)
    throws AMConsoleException;
}
