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
 * $Id: AuthenticatedViewBean.java,v 1.2 2008/06/25 05:42:48 qcheng Exp $
 *
 * Portions Copyrighted 2025 3A Systems LLC.
 *
 */

package com.sun.identity.console.base;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.web.ui.view.alert.CCAlert;
import jakarta.servlet.http.HttpServletRequest;

/**
 * This view just display a message indicating that user has authenticated.
 */
public class AuthenticatedViewBean
    extends AMViewBeanBase
{
    private static final String PAGE_NAME = "Authenticated";
    private static final String DEFAULT_DISPLAY_URL 
        = "/console/base/Authenticated.jsp";

    /**
     * Constructs a login view bean
     */
    public AuthenticatedViewBean() {
        super(PAGE_NAME);
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    public void forwardTo(RequestContext rc) {
        super.bypassForwardTo(rc);
    }

    public void beginDisplay(DisplayEvent e)
        throws ModelControlException {
        super.beginDisplay(e);
        setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
            "authenticated.message");
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        return new AMModelBase(req, getPageSessionAttributes());
    }

}
