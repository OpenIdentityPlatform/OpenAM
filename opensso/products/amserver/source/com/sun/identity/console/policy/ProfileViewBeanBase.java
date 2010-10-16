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
 * $Id: ProfileViewBeanBase.java,v 1.2 2008/06/25 05:43:04 qcheng Exp $
 *
 */

package com.sun.identity.console.policy;

import com.iplanet.jato.RequestManager;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.policy.model.CachedPolicy;
import com.sun.identity.console.policy.model.PolicyCache;
import com.sun.identity.console.policy.model.PolicyModel;
import com.sun.identity.console.policy.model.PolicyModelImpl;
import javax.servlet.http.HttpServletRequest;

public abstract class ProfileViewBeanBase
    extends AMPrimaryMastHeadViewBean
{
    protected static final String PG_SESSION_POLICY_CACHE_ID = "policyCacheID";

    /**
     * Creates a policy operation base view bean.
     *
     * @param name Name of view
     */
    public ProfileViewBeanBase(String name, String defaultDisplayURL) {
        super(name);
        setDefaultDisplayURL(defaultDisplayURL);
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        return new PolicyModelImpl(req, getPageSessionAttributes());
    }

    protected CachedPolicy getCachedPolicy()
        throws AMConsoleException
    {
        CachedPolicy policy = null;
        String cacheID = (String)getPageSessionAttribute(
            PG_SESSION_POLICY_CACHE_ID);

        if (cacheID != null) {
            PolicyCache cache = PolicyCache.getInstance();
            PolicyModel model = (PolicyModel)getModel();
            policy = model.getCachedPolicy(cacheID);
        }

        return policy;
    }

    /**
     * Creates the label used to take the user back to the previous page
     * they were viewing. The label is defined in the property file as
     * "Back to {0}" which lets us control the format of the label in
     * different locales.  The name of the current object is substituted.
     */
    protected String getBackButtonLabel() {
        return getBackButtonLabel("policy.back.button");
    }
}
