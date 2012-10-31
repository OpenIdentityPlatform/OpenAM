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
 * $Id: ResponseProviderProxyViewBean.java,v 1.2 2008/06/25 05:43:05 qcheng Exp $
 *
 */

package com.sun.identity.console.policy;

import com.iplanet.jato.view.View;
import com.iplanet.jato.view.html.TextField;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.policy.model.PolicyModelImpl;
import javax.servlet.http.HttpServletRequest;

public class ResponseProviderProxyViewBean
    extends AMViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/policy/ResponseProviderProxy.jsp";
    private static final String TF_URL = "tfURL";
    static final String TF_RESPONSEPROVIDER_TYPE_NAME = "providerType";
    static final String TF_RESPONSEPROVIDER_NAME = "providerName";
    static final String TF_REALM_NAME = "locDN";
    static final String TF_CACHED_ID = "cachedID";
    static final String TF_OP = "tfOp";

    public ResponseProviderProxyViewBean() {
        super("ResponseProviderProxy");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    public View createChild(String name) {
        View view = null;

        if (name.equals(TF_URL) ||
            name.equals(TF_OP) ||
            name.equals(TF_RESPONSEPROVIDER_TYPE_NAME) ||
            name.equals(TF_RESPONSEPROVIDER_NAME) ||
            name.equals(TF_REALM_NAME) ||
            name.equals(TF_CACHED_ID) ||
            name.equals(TF_CACHED_ID)
        ) {
            view = new TextField(this, name, "");
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req = getRequestContext().getRequest();
        return new PolicyModelImpl(req, getPageSessionAttributes());
    }

    void setURL(String url, String op) {
        int idx = url.lastIndexOf('/');
        if (idx != -1) {
            url += "?" + url.substring(idx+1) + "." + TF_OP + "=" + op;
        }
        setDisplayFieldValue(TF_URL, url);
    }
}
