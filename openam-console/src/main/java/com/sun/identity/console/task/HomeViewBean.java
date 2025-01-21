/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: HomeViewBean.java,v 1.2 2008/06/25 05:43:21 qcheng Exp $
 *
 * Portions Copyright 2015 ForgeRock AS.
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package com.sun.identity.console.task;

import static com.sun.identity.console.XuiRedirectHelper.*;

import jakarta.servlet.http.HttpServletRequest;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMModelBase;

/**
 * Placement for Common task Home page
 */
public class HomeViewBean
    extends AMPrimaryMastHeadViewBean
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/task/Home.jsp";

    public HomeViewBean() {
        super("Home");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        registerChildren();
    }

    protected void registerChildren() {
        super.registerChildren();
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req = getRequestContext().getRequest();
        return new AMModelBase(req, getPageSessionAttributes());
    }

    @Override
    public void beginDisplay(DisplayEvent event) throws ModelControlException {
        if (!isJatoSessionRequestFromXUI(getRequestContext().getRequest()) && isXuiAdminConsoleEnabled()) {
            String redirectRealm = getAdministeredRealm(this);
            String authenticationRealm = getAuthenticationRealm(this);
            redirectToXui(getRequestContext().getRequest(), redirectRealm, authenticationRealm, "realms");
        }
    }
}
