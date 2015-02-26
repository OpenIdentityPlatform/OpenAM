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
 * $Id: PolicySelectTypeViewBean.java,v 1.2 2008/06/25 05:43:04 qcheng Exp $
 *
 */

package com.sun.identity.console.policy;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.policy.model.PolicyModel;
import com.sun.identity.console.policy.model.PolicyModelImpl;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCTextField;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import javax.servlet.http.HttpServletRequest;


public class PolicySelectTypeViewBean
    extends AMPrimaryMastHeadViewBean
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/policy/PolicySelectType.jsp";
    private static final String PGTITLE_TWO_BTNS = "pgtitleTwoBtns";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    private static final String ATTR_POLICY_TYPE = "tfPolicyType";
    private static final String ATTR_VALUE_POLICY_TYPE_NORMAL = "normal";

    private CCPageTitleModel ptModel;
    private AMPropertySheetModel propertySheetModel;

    /**
     * Creates a view to prompt user for policy type before policy creation.
     */
    public PolicySelectTypeViewBean() {
        super("PolicySelectType");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        createPageTitleModel();
        createPropertyModel();
        registerChildren();
    }

    protected void registerChildren() {
        ptModel.registerChildren(this);
        propertySheetModel.registerChildren(this);
        registerChild(PGTITLE_TWO_BTNS, CCPageTitle.class);
        registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
        registerChild(PolicyModel.TF_NAME, CCTextField.class);
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
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/twoBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.next");
        ptModel.setValue("button2", "button.cancel");
    }
                                                                                
    private void createPropertyModel() {
        propertySheetModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/propertyPMPolicySelectType.xml"));
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
        String type = (String)getDisplayFieldValue(ATTR_POLICY_TYPE);

        if ((type == null) || (type.trim().length() == 0)) {
            setDisplayFieldValue(
                ATTR_POLICY_TYPE, ATTR_VALUE_POLICY_TYPE_NORMAL);
        }
    }

    /**
     * Handles cancel request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        PolicyViewBean vb = (PolicyViewBean)getViewBean(PolicyViewBean.class);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Handles next button request.
     *
     * @param event Request invocation event.
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException {
        PolicyModel model = (PolicyModel)getModel();
        String policyName = (String)propertySheetModel.getValue(
            PolicyModel.TF_NAME);
        policyName = policyName.trim();
        String policyType = (String)propertySheetModel.getValue(
            ATTR_POLICY_TYPE);

        if (policyName.length() > 0) {
            try {
                boolean referralType = !policyType.equals(
                    ATTR_VALUE_POLICY_TYPE_NORMAL);
                String id = model.cachePolicy(
                    policyName, "", referralType, true);
                setPageSessionAttribute(
                    PolicyOpViewBeanBase.PG_SESSION_POLICY_CACHE_ID, id);
                PolicyOpViewBeanBase vb = null;

                if (referralType) {
                    vb = (PolicyReferralAddViewBean)getViewBean(
                        PolicyReferralAddViewBean.class);
                } else {
                    vb = (PolicyNormalAddViewBean)getViewBean(
                        PolicyNormalAddViewBean.class);
                }

                passPgSessionMap(vb);
                vb.forwardTo(getRequestContext());
            } catch (AMConsoleException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
                forwardTo();
            }
        } else {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                "policy.missing.policyName");
            forwardTo();
        }
    }
}
