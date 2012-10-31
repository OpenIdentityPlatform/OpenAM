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
 * $Id: SelectConditionTypeViewBean.java,v 1.2 2008/06/25 05:43:05 qcheng Exp $
 *
 */

package com.sun.identity.console.policy;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.policy.model.PolicyModel;
                                                                             
public class SelectConditionTypeViewBean
    extends SelectTypeViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/policy/SelectConditionType.jsp";
    private static final String ATTR_CONDITION_NAME = "tfConditionName";
    private static final String ATTR_CONDITION_TYPE = "radioConditionType";
    public static final String CALLING_VIEW_BEAN =
        "SelectConditionTypeViewBeanCallingVB";

    /**
     * Creates a view to prompt user for condition type before condition
     * creation.
     */
    public SelectConditionTypeViewBean() {
        super("SelectConditionType", DEFAULT_DISPLAY_URL);
    }

    protected String getTypeOptionsChildName() {
        return ATTR_CONDITION_TYPE;
    }

    protected OptionList getTypeOptions() {
        PolicyModel model = (PolicyModel)getModel();
        String curRealm = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        return createOptionList(model.getActiveConditionTypes(curRealm),
            model.getUserLocale());
    }

    protected String getPropertyXMLFileName() {
        return "com/sun/identity/console/propertyPMSelectConditionType.xml";
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
        String conditionType = (String)propertySheetModel.getValue(
            ATTR_CONDITION_TYPE);

        setPageSessionAttribute(ConditionOpViewBeanBase.CALLING_VIEW_BEAN,
            (String)getPageSessionAttribute(CALLING_VIEW_BEAN));
        String realmName = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        String viewBeanURL = model.getConditionViewBeanURL(
            realmName, conditionType);
        unlockPageTrailForSwapping();
        if ((viewBeanURL != null) && (viewBeanURL.trim().length() > 0)) {
            forwardToURL(viewBeanURL, conditionType, realmName);
        } else {
            forwardToViewBean(model, conditionType, realmName);
        }
    }

    private void forwardToURL(
        String url,
        String conditionType,
        String realmName
    ) {
        ConditionProxyViewBean vb = (ConditionProxyViewBean)getViewBean(
            ConditionProxyViewBean.class);
        passPgSessionMap(vb);
        vb.setURL(url, "add");
        vb.setDisplayFieldValue(ConditionProxyViewBean.TF_CONDITION_TYPE_NAME,
            conditionType);
        if ((realmName == null) || (realmName.trim().length() == 0)) {
            realmName = AMModelBase.getStartDN(
                getRequestContext().getRequest());
        }

        vb.setDisplayFieldValue(
            ConditionProxyViewBean.TF_REALM_NAME, realmName);
        vb.setDisplayFieldValue(ConditionProxyViewBean.TF_CACHED_ID,
            (String)getPageSessionAttribute(
                ProfileViewBeanBase.PG_SESSION_POLICY_CACHE_ID));
        vb.setDisplayFieldValue(ConditionProxyViewBean.TF_OP, "add");
        vb.forwardTo(getRequestContext());
    }

    private void forwardToViewBean(
        PolicyModel model,
        String conditionType,
        String realmName
    ) {
        ConditionAddViewBean vb = (ConditionAddViewBean)getViewBean(
            ConditionAddViewBean.class);
        setPageSessionAttribute(
            ConditionOpViewBeanBase.PG_SESSION_CONDITION_TYPE,
            conditionType);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.selectConditionType";
    }

    protected boolean startPageTrail() {
        return false;
    }
}
