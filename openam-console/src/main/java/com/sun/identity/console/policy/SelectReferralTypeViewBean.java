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
 * $Id: SelectReferralTypeViewBean.java,v 1.2 2008/06/25 05:43:05 qcheng Exp $
 *
 */

package com.sun.identity.console.policy;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.policy.model.PolicyModel;

public class SelectReferralTypeViewBean
    extends SelectTypeViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/policy/SelectReferralType.jsp";
    private static final String ATTR_REFERRAL_NAME = "tfReferralName";
    private static final String ATTR_REFERRAL_TYPE = "radioReferralType";
    public static final String CALLING_VIEW_BEAN =
        "SelectReferralTypeViewBeanCallingVB";

    /**
     * Creates a view to prompt user for referral type before referral creation.
     */
    public SelectReferralTypeViewBean() {
        super("SelectReferralType", DEFAULT_DISPLAY_URL);
    }

    protected String getTypeOptionsChildName() {
        return ATTR_REFERRAL_TYPE;
    }

    protected OptionList getTypeOptions() {
        PolicyModel model = (PolicyModel)getModel();
        String curRealm = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        return createOptionList(model.getActiveReferralTypes(curRealm),
            model.getUserLocale());
    }

    protected String getPropertyXMLFileName() {
        return "com/sun/identity/console/propertyPMSelectReferralType.xml";
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
        String referralType = (String)propertySheetModel.getValue(
            ATTR_REFERRAL_TYPE);

            setPageSessionAttribute(ReferralOpViewBeanBase.CALLING_VIEW_BEAN,
                (String)getPageSessionAttribute(CALLING_VIEW_BEAN));
            String realmName = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            String viewBeanURL = model.getReferralViewBeanURL(
                realmName, referralType);
            unlockPageTrailForSwapping();

            if ((viewBeanURL != null) && (viewBeanURL.trim().length() > 0)) {
                forwardToURL(viewBeanURL, referralType, realmName);
            } else {
                forwardToViewBean(model, referralType, realmName);
            }
    }

    private void forwardToURL(
        String url,
        String referralType,
        String realmName
    ) {
        ReferralProxyViewBean vb = (ReferralProxyViewBean)getViewBean(
            ReferralProxyViewBean.class);
        passPgSessionMap(vb);
        vb.setURL(url, "add");
        vb.setDisplayFieldValue(ReferralProxyViewBean.TF_REFERRAL_TYPE_NAME,
            referralType);
        if ((realmName == null) || (realmName.trim().length() == 0)) {
            realmName = AMModelBase.getStartDN(
                getRequestContext().getRequest());
        }

        vb.setDisplayFieldValue(ReferralProxyViewBean.TF_REALM_NAME, realmName);
        vb.setDisplayFieldValue(ReferralProxyViewBean.TF_CACHED_ID,
            (String)getPageSessionAttribute(
                ProfileViewBeanBase.PG_SESSION_POLICY_CACHE_ID));
        vb.setDisplayFieldValue(ReferralProxyViewBean.TF_OP, "add");
        vb.forwardTo(getRequestContext());
    }

    private void forwardToViewBean(
        PolicyModel model,
        String referralType,
        String realmName
    ) {
        ReferralAddViewBean vb = (ReferralAddViewBean)getViewBean(
            ReferralAddViewBean.class);
        setPageSessionAttribute(ReferralOpViewBeanBase.PG_SESSION_REFERRAL_TYPE,
            referralType);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.selectReferralType";
    }

    protected boolean startPageTrail() {
        return false;
    }
}
