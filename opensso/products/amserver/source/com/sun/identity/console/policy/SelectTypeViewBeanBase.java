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
 * $Id: SelectTypeViewBeanBase.java,v 1.2 2008/06/25 05:43:05 qcheng Exp $
 *
 */

package com.sun.identity.console.policy;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.Option;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.policy.model.CachedPolicy;
import com.sun.identity.console.policy.model.PolicyModel;
import com.sun.identity.console.policy.model.PolicyModelImpl;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.html.CCRadioButton;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import javax.servlet.http.HttpServletRequest;

public abstract class SelectTypeViewBeanBase
    extends AMPrimaryMastHeadViewBean
{
    private static final String PGTITLE_TWO_BTNS = "pgtitleTwoBtns";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";

    protected CCPageTitleModel ptModel;
    protected AMPropertySheetModel propertySheetModel;

    /**
     * Creates a view to prompt user for referral type before referral creation.
     *
     * @param name Name of view
     * @param defaultDisplayURL Default Display URL.
     */
    public SelectTypeViewBeanBase(String name, String defaultDisplayURL) {
        super(name);
        setDefaultDisplayURL(defaultDisplayURL);
        createPageTitleModel();
        createPropertyModel();
        registerChildren();
    }

    protected void registerChildren() {
        ptModel.registerChildren(this);
        propertySheetModel.registerChildren(this);
        registerChild(PGTITLE_TWO_BTNS, CCPageTitle.class);
        registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
        super.registerChildren();
    }

    protected View createChild(String name) {
        View view = null;

        if (name.equals(PGTITLE_TWO_BTNS)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (name.equals(PROPERTY_ATTRIBUTE)) {
            view = new AMPropertySheet(this, propertySheetModel, name);
        } else if (propertySheetModel.isChildSupported(name)) {
            view = propertySheetModel.createChild(this, name, getModel());
            if (name.equals(getTypeOptionsChildName())) {
                CCRadioButton rb = (CCRadioButton)view;
                rb.setOptions(getTypeOptions());
            }
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    protected CachedPolicy getCachedPolicy()
        throws AMConsoleException
    {
        CachedPolicy policy = null;
        String cacheID = (String)getPageSessionAttribute(
            ProfileViewBeanBase.PG_SESSION_POLICY_CACHE_ID);
        if (cacheID != null) {
            PolicyModel model = (PolicyModel)getModel();
            policy = model.getCachedPolicy(cacheID);
        }
        return policy;
    }

    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.back");
        ptModel.setValue("button2", "button.next");
        ptModel.setValue("button3", "button.cancel");
    }

    private void createPropertyModel() {
        propertySheetModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(
                getPropertyXMLFileName()));
        propertySheetModel.clear();
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        return new PolicyModelImpl(req, getPageSessionAttributes());
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        super.beginDisplay(event);
        disableButton("button1", true);

        String typeChildName = getTypeOptionsChildName();
        String type = (String)getDisplayFieldValue(typeChildName);

        if ((type == null) || (type.trim().length() == 0)) {
            setDefaultServiceOption((CCRadioButton)getChild(typeChildName));
        }
    }

    protected void setDefaultServiceOption(CCRadioButton rb) {
        OptionList optList = rb.getOptions();
        if ((optList != null) && (optList.size() > 0)) {
            Option opt = optList.get(0);
            rb.setValue(opt.getValue());
        }
    }

    /**
     * Handles cancel request.
     *
     * @param event Request invocation event
     */
    public void handleButton3Request(RequestInvocationEvent event) {
        try {
            Class clazz = Class.forName(
                (String)removePageSessionAttribute(
                    getCallingViewBeanPgSessionName()));
            PolicyOpViewBeanBase vb = (PolicyOpViewBeanBase)getViewBean(clazz);
            backTrail();
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } catch (ClassNotFoundException e) {
            debug.error("SelectTypeViewBeanBase.handleButton2Request", e);
            forwardTo();
        }
    }

    protected abstract String getTypeOptionsChildName();
    protected abstract OptionList getTypeOptions();
    protected abstract String getPropertyXMLFileName();
    protected abstract String getCallingViewBeanPgSessionName();
}
