/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: Help2Servlet.java,v 1.1 2009/08/04 21:54:48 veiming Exp $
 */
package com.sun.identity.console.help;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestContextImpl;
import com.iplanet.jato.ViewBeanManager;
import com.sun.identity.console.base.AMViewBeanBase;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

public class Help2Servlet extends com.sun.web.ui.servlet.help2.Help2Servlet {
    @Override
    protected void initializeRequestContext(RequestContext requestContext) {
        super.initializeRequestContext(requestContext);
        ViewBeanManager viewBeanManager =
            new ViewBeanManager(requestContext, getPackageName(
               Help2Servlet.class.getName()));
        ((RequestContextImpl)requestContext).
            setViewBeanManager(viewBeanManager);
    }

    @Override
    protected void onRequestHandlerNotFound(
        RequestContext requestContext,
        String handlerName
    ) throws ServletException {
        AMViewBeanBase.debug.error("Help2Servlet.onRequestHandlerNotFound: " + 
            handlerName);
    }

    @Override
    protected void onRequestHandlerNotSpecified(RequestContext requestContext)
        throws ServletException {
        AMViewBeanBase.debug.error("Help2Servlet.onRequestHandlerNotSpecified");
    }

    @Override
    protected void onUncaughtException(
        RequestContext requestContext,
        Exception e
    ) throws ServletException, IOException {
        HttpServletRequest httpRequest = (HttpServletRequest)
            requestContext.getRequest();
        AMViewBeanBase.debug.error("Help2Servlet.onUncaughtException", e);
        String redirectUrl = Help2ViewBean.getCurrentURL(httpRequest) +
                "/base/AMUncaughtException";
        requestContext.getResponse().sendRedirect(redirectUrl);
    }
}
