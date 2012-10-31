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
 * $Id: SelectResponseProviderTypeViewBean.java,v 1.2 2008/06/25 05:43:05 qcheng Exp $
 *
 */

package com.sun.identity.console.policy;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.policy.model.PolicyModel;
                                                                             
public class SelectResponseProviderTypeViewBean
    extends SelectTypeViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/policy/SelectResponseProviderType.jsp";
    private static final String ATTR_RESPONSEPROVIDER_NAME =
        "tfResponseProviderName";
    private static final String ATTR_RESPONSEPROVIDER_TYPE =
        "radioResponseProviderType";
    public static final String CALLING_VIEW_BEAN =
        "SelectResponseProviderTypeViewBeanCallingVB";

    /**
     * Creates a view to prompt user for response provider type before response
     * provider creation.
     */
    public SelectResponseProviderTypeViewBean() {
        super("SelectResponseProviderType", DEFAULT_DISPLAY_URL);
    }

    protected String getTypeOptionsChildName() {
        return ATTR_RESPONSEPROVIDER_TYPE;
    }

    protected OptionList getTypeOptions() {
        PolicyModel model = (PolicyModel)getModel();
        String curRealm = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        return createOptionList(model.getActiveResponseProviderTypes(curRealm),
            model.getUserLocale());
    }

    protected String getPropertyXMLFileName() {
        return
        "com/sun/identity/console/propertyPMSelectResponseAttributeType.xml";
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
        String providerType = (String)propertySheetModel.getValue(
            ATTR_RESPONSEPROVIDER_TYPE);

        setPageSessionAttribute(
            ResponseProviderOpViewBeanBase.CALLING_VIEW_BEAN,
            (String)getPageSessionAttribute(CALLING_VIEW_BEAN));
        String realmName = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        String viewBeanURL = model.getResponseProviderViewBeanURL(
            realmName, providerType);
        unlockPageTrailForSwapping();

        if ((viewBeanURL != null) && (viewBeanURL.trim().length() > 0)) {
            forwardToURL(viewBeanURL, providerType, realmName);
        } else {
            forwardToViewBean(model, providerType, realmName);
        }
    }

    private void forwardToURL(
        String url,
        String providerType,
        String realmName
    ) {
        ResponseProviderProxyViewBean vb =
            (ResponseProviderProxyViewBean)getViewBean(
            ResponseProviderProxyViewBean.class);
        passPgSessionMap(vb);
        vb.setURL(url, "add");
        vb.setDisplayFieldValue(
            ResponseProviderProxyViewBean.TF_RESPONSEPROVIDER_TYPE_NAME,
            providerType);
        if ((realmName == null) || (realmName.trim().length() == 0)) {
            realmName = AMModelBase.getStartDN(
                getRequestContext().getRequest());
        }

        vb.setDisplayFieldValue(
            ResponseProviderProxyViewBean.TF_REALM_NAME, realmName);
        vb.setDisplayFieldValue(ResponseProviderProxyViewBean.TF_CACHED_ID,
            (String)getPageSessionAttribute(
                ProfileViewBeanBase.PG_SESSION_POLICY_CACHE_ID));
        vb.setDisplayFieldValue(ResponseProviderProxyViewBean.TF_OP, "add");
        vb.forwardTo(getRequestContext());
    }

    private void forwardToViewBean(
        PolicyModel model,
        String providerType,
        String realmName
    ) {
        ResponseProviderAddViewBean vb = (ResponseProviderAddViewBean)
            getViewBean(ResponseProviderAddViewBean.class);
        setPageSessionAttribute(
            ResponseProviderOpViewBeanBase.PG_SESSION_PROVIDER_TYPE,
            providerType);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.selectResponseProviderType";
    }

    protected boolean startPageTrail() {
        return false;
    }
}
