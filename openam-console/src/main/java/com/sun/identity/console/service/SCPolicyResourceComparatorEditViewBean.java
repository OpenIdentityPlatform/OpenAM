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
 * $Id: SCPolicyResourceComparatorEditViewBean.java,v 1.3 2008/06/25 05:43:16 qcheng Exp $
 *
 */

package com.sun.identity.console.service;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.delegation.model.DelegationConfig;
import com.sun.identity.console.service.model.PolicyResourceComparator;
import com.sun.identity.console.service.model.SCPolicyModel;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.web.ui.model.CCPageTitleModel;
import java.util.Map;

public class SCPolicyResourceComparatorEditViewBean
    extends SCPolicyResourceComparatorViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/service/SCPolicyResourceComparatorEdit.jsp";
    private static final String PGATTR_INDEX = "serverListTblIndex";
    private boolean populateValues = false;

    public SCPolicyResourceComparatorEditViewBean() {
        super("SCPolicyResourceComparatorEdit", DEFAULT_DISPLAY_URL);
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
                SCPolicyViewBean.PROPERTY_ATTRIBUTE);
            OrderedSet set = (OrderedSet)mapAttrs.get(
                SCPolicyModel.ATTRIBUTE_NAME_RESOURCE_COMPARATOR);
            setValues((String)set.get(index));
        }
    }

    protected void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setPageTitleText(getPageTitleText());
        ptModel.setValue("button1", "button.save");
        ptModel.setValue("button2", "button.reset");
        ptModel.setValue("button3", 
            getBackButtonLabel("breadcrumbs.services.policy"));
    }

    /**
     * Handles reset request.
     *
     * @param event Request Invocation Event.
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        populateValues = true;
        forwardTo();
    }

    protected String getButtonlLabel() {
        return "button.ok";
    }

    protected String getPageTitleText() {
        return "policy.service.resource.comparator.edit.page.title";
    }

    protected void handleButton1Request(PolicyResourceComparator rc) {
        SCPolicyViewBean vb = (SCPolicyViewBean)getViewBean(
            SCPolicyViewBean.class);

        Map mapAttrs = (Map)getPageSessionAttribute(
            SCPolicyViewBean.PROPERTY_ATTRIBUTE);
        OrderedSet resourceComparators = (OrderedSet)mapAttrs.get(
            SCPolicyModel.ATTRIBUTE_NAME_RESOURCE_COMPARATOR);
        int index = Integer.parseInt((String)
            getPageSessionAttribute(PGATTR_INDEX));
        resourceComparators.set(index, rc.toString());
        setPageSessionAttribute(SCPolicyViewBean.PAGE_MODIFIED, "1");
        backTrail();
        unlockPageTrailForSwapping();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Handles reset request.
     *   
     * @param event Request Invocation Event.
     */  
    public void handleButton3Request(RequestInvocationEvent event) {
        backTrail();
        unlockPageTrailForSwapping();
        SCPolicyViewBean vb = (SCPolicyViewBean)getViewBean(
            SCPolicyViewBean.class);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    protected void createPropertyModel() {
        DelegationConfig dConfig = DelegationConfig.getInstance();
        boolean canModify = dConfig.hasPermission("/", null,
            AMAdminConstants.PERMISSION_MODIFY,
            getRequestContext().getRequest(), getClass().getName());

        String xmlFile = (canModify) ?
            "com/sun/identity/console/propertySCPolicyResourceComparator.xml":
     "com/sun/identity/console/propertySCPolicyResourceComparator_Readonly.xml";
        propertySheetModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(xmlFile));
        propertySheetModel.clear();
    }

    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.services.policy.resource.comparator.edit";
    }

    protected boolean startPageTrail() {
        return false;
    }

}
