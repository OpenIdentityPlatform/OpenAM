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
 * $Id: SelectServiceTypeViewBean.java,v 1.2 2008/06/25 05:43:05 qcheng Exp $
 *
 */

package com.sun.identity.console.policy;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMFormatUtils;
import com.sun.identity.console.policy.model.PolicyModel;
import com.sun.identity.policy.Policy;
import com.sun.web.ui.view.html.CCRadioButton;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class SelectServiceTypeViewBean
    extends SelectTypeViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/policy/SelectServiceType.jsp";
    private static final String ATTR_RULE_NAME = "tfRuleName";
    private static final String ATTR_SERVICE_TYPE = "radioServiceType";
    private static final String WITH_RESOURCE_SUFFIX = "1";
    private static final String WITHOUT_RESOURCE_SUFFIX = "0";
    public static final String CALLING_VIEW_BEAN =
        "SelectServiceTypeViewBeanCallingVB";

    /**
     * Creates a view to prompt user for policy type before policy creation.
     */
    public SelectServiceTypeViewBean() {
        super("SelectServiceType", DEFAULT_DISPLAY_URL);
    }

    protected String getTypeOptionsChildName() {
        return ATTR_SERVICE_TYPE;
    }

    protected OptionList getTypeOptions() {
        PolicyModel model = (PolicyModel)getModel();
        String curRealm = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);

        try {
            Policy policy = getCachedPolicy().getPolicy();
            Map map = model.getServiceTypeNames();
            Map options = new HashMap();

            for (Iterator iter = map.keySet().iterator(); iter.hasNext(); ) {
                String name = (String)iter.next();
                String label = (String)map.get(name);

                if (model.requiredResourceName(policy, curRealm, name)) {
                    String[] param = {label};
                    String lbl = MessageFormat.format(
                        model.getLocalizedString(
                            "policy.rules.withResourceName"), (Object[])param);
                    options.put(name + "|" + WITH_RESOURCE_SUFFIX, lbl);
                }

                if (model.notRequiredResourceName(policy, curRealm, name)) {
                    String[] param = {label};
                    String lbl = MessageFormat.format(
                        model.getLocalizedString(
                            "policy.rules.withoutResourceName"), 
                            (Object[])param);
                    options.put(name + "|" + WITHOUT_RESOURCE_SUFFIX, lbl);
                }
            }

            return AMFormatUtils.getSortedOptionList(
                options, model.getUserLocale());
        } catch (AMConsoleException e) {
            debug.message("SelectServiceTypeViewBean.getTypeOptions " +
                "creating empty option list");
            return new OptionList();
        }
    }

    protected String getPropertyXMLFileName() {
        return "com/sun/identity/console/propertyPMSelectServiceType.xml";
    }

    protected String getCallingViewBeanPgSessionName() {
        return CALLING_VIEW_BEAN;
    }

    /**
     * Handles next button request.
     *
     * @param event Request invocation event.
     */
    public void handleButton2Request(RequestInvocationEvent event)
        throws ModelControlException {
        PolicyModel model = (PolicyModel)getModel();
        String formatServiceType = (String)propertySheetModel.getValue(
            ATTR_SERVICE_TYPE);
        int idx = formatServiceType.indexOf("|");
        String serviceType = formatServiceType.substring(0, idx);
        boolean withResource = formatServiceType.substring(idx +1).equals(
            WITH_RESOURCE_SUFFIX);

        setPageSessionAttribute(RuleOpViewBeanBase.CALLING_VIEW_BEAN,
            (String)getPageSessionAttribute(CALLING_VIEW_BEAN));
        setPageSessionAttribute(RuleOpViewBeanBase.WITH_RESOURCE,
            withResource ? Boolean.TRUE : Boolean.FALSE);
        RuleAddViewBean vb = null;

        if (withResource) {
            String realmName = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);

            if (model.canCreateNewResource(realmName, serviceType)) {
                vb = (RuleAddViewBean)getViewBean(RuleAddViewBean.class);
            } else {
                vb = (RuleAddViewBean)getViewBean(
                    RuleWithPrefixAddViewBean.class);
            }
        } else {
            vb = (RuleNoResourceAddViewBean)getViewBean(
                RuleNoResourceAddViewBean.class);
        }

        unlockPageTrailForSwapping();
        passPgSessionMap(vb);
        vb.serviceType = serviceType;
        vb.forwardTo(getRequestContext());
    }

    protected void setDefaultServiceOption(CCRadioButton rb) {
        /*
         * 20050406 Dennis
         * bug 6234672
         * Defaulting service to agent service.
         */
        rb.setValue("iPlanetAMWebAgentService|1");
    }

    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.selectServiceType";
    }

    protected boolean startPageTrail() {
        return false;
    }
}
