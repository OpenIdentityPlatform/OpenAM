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
 * $Id: ResponseProviderPluginViewBeanBase.java,v 1.2 2008/06/25 05:43:04 qcheng Exp $
 *
 * Portions Copyrighted 2025 3A Systems LLC.
 *
 */

package com.sun.identity.console.policy;

import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.HREF;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.policy.model.PolicyModelImpl;
import jakarta.servlet.http.HttpServletRequest;

public class ResponseProviderPluginViewBeanBase
    extends AMViewBeanBase
{
    static final String TF_OP = "tfOp";
    static final String TF_RESPONSEPROVIDER_TYPE_NAME = "providerType";
    static final String TF_RESPONSEPROVIDER_NAME = "providerName";
    static final String TF_REALM_NAME = "locDN";
    static final String TF_CACHED_ID = "cachedID";

    public ResponseProviderPluginViewBeanBase(String name, String url) {
        super(name);
        setDefaultDisplayURL(url);
        registerChildren();
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req = getRequestContext().getRequest();
        return new PolicyModelImpl(req, getPageSessionAttributes());
    }

    protected void registerChildren() {
        super.registerChildren();
        registerChild(TF_OP, HREF.class);
    }

    public View createChild(String name) {
        View view = null;

        if (name.equals(TF_OP)) {
            view = new HREF(this, name, "");
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    public void handleTfOpRequest(RequestInvocationEvent event) {
        String op = (String)getDisplayFieldValue(TF_OP);
        HttpServletRequest req = getRequestContext().getRequest();
        setPageSessionAttribute(AMAdminConstants.CURRENT_REALM,
            req.getParameter(TF_REALM_NAME));
        setPageSessionAttribute(ProfileViewBeanBase.PG_SESSION_POLICY_CACHE_ID,
            req.getParameter(TF_CACHED_ID));

        ResponseProviderOpViewBeanBase vb = op.equals("edit") ?
            (ResponseProviderOpViewBeanBase)getViewBean(
                getEditViewBeanClass()) :
            (ResponseProviderOpViewBeanBase)getViewBean(
                getAddViewBeanClass());
        setPageSessionAttribute(
            ResponseProviderOpViewBeanBase.PG_SESSION_PROVIDER_NAME,
            req.getParameter(TF_RESPONSEPROVIDER_NAME));
        setPageSessionAttribute(
            ResponseProviderOpViewBeanBase.PG_SESSION_PROVIDER_TYPE,
            req.getParameter(TF_RESPONSEPROVIDER_TYPE_NAME));
        setPageSessionAttribute(AMAdminConstants.CURRENT_REALM, 
            req.getParameter(TF_REALM_NAME));

        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    protected Class getAddViewBeanClass() {
        return ResponseProviderAddViewBean.class;
    }
                                                                                
    protected Class getEditViewBeanClass() {
        return ResponseProviderEditViewBean.class;
    }
}
