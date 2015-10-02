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
 * $Id: PolicyViewBean.java,v 1.4 2009/10/08 21:56:11 asyhuang Exp $
 *
 * Portions Copyrighted 2012-2015 ForgeRock AS.
 * Portions Copyrighted 2012 Open Source Solution Technology Corporation
 */
package com.sun.identity.console.policy;

import static com.sun.identity.console.XuiRedirectHelper.getRedirectRealm;
import static com.sun.identity.console.XuiRedirectHelper.redirectToXui;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.policy.model.PolicyModelImpl;
import com.sun.identity.console.realm.HasEntitiesTabs;
import com.sun.identity.console.realm.RealmPropertiesBase;

import javax.servlet.http.HttpServletRequest;
import java.text.MessageFormat;

/**
 * The policy view bean will redirect the user to the XUI Policy Editor.
 */
public class PolicyViewBean extends RealmPropertiesBase implements HasEntitiesTabs {

    public static final String DEFAULT_DISPLAY_URL = "/console/policy/Policy.jsp";

    /**
     * Creates a policy view bean.
     */
    public PolicyViewBean() {
        super("Policy");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    @Override
    protected void initialize() {
        if (!initialized) {
            super.initialize();
            initialized = true;
        }
    }

    @Override
    public void beginDisplay(DisplayEvent event) throws ModelControlException {
        redirectToXui(getRequestContext().getRequest(), getRedirectRealm(this),
                MessageFormat.format("realms/{0}/policySets/list", getCurrentRealmEncoded()));
    }

    @Override
    protected AMModel getModelInternal() {
        RequestContext rc = RequestManager.getRequestContext();
        HttpServletRequest req = rc.getRequest();
        return new PolicyModelImpl(req, getPageSessionAttributes());
    }
}
