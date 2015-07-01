/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package com.sun.identity.console.task;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManager;
import com.sun.identity.console.XuiRedirectHelper;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;

public abstract class RedirectToRealmHomeViewBean extends AMPrimaryMastHeadViewBean {
    public RedirectToRealmHomeViewBean(String name) {
        super(name);
    }

    protected void redirectToHome() {
        if (XuiRedirectHelper.isXuiAdminConsoleEnabled()) {
            RequestContext rc = RequestManager.getRequestContext();
            try {
                String realm = URLEncoder.encode(rc.getRequest().getParameter("realm"), "UTF-8");
                rc.getResponse().sendRedirect("../XUI#realms/" + realm + "/dashboard");
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException("UTF-8 not supported", e);
            } catch (IOException e) {
                debug.message("Unexpected IOException during redirect", e);
            }
        } else {
            HomeViewBean vb = (HomeViewBean) getViewBean(HomeViewBean.class);
            backTrail();
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        }
    }
}
