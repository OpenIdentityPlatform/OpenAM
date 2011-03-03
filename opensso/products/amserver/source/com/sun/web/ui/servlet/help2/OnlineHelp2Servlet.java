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
 * $Id: OnlineHelp2Servlet.java,v 1.1 2009/07/29 22:32:15 asyhuang Exp $
 *
 */
/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.web.ui.servlet.help2;

import com.iplanet.jato.*;
import java.io.IOException;
import javax.servlet.*;
import javax.servlet.http.*;

public class OnlineHelp2Servlet extends Help2Servlet {

    /**
     * Forwards to login view bean, in case of an invalid target
     * request handler (page).
     *
     * @param requestContext - request context
     * @param handlerName - name of handler
     * @throws ServletException
     */
    protected void onRequestHandlerNotFound(
            RequestContext requestContext,
            String handlerName)
            throws ServletException {
    }

    /**
     * Forwards to invalid URL view bean, in case of no handler specified
     *
     * @param requestContext - request context
     * @throws ServletException
     */
    protected void onRequestHandlerNotSpecified(RequestContext requestContext)
            throws ServletException {
    }

    protected void onUncaughtException(
            RequestContext requestContext,
            Exception e)
            throws ServletException, IOException {
        HttpServletRequest httpRequest =
                (HttpServletRequest) requestContext.getRequest();
        String redirectUrl = httpRequest.getScheme() + "://" +
                httpRequest.getServerName() + ":" +
                httpRequest.getServerPort() +
                httpRequest.getContextPath() +
                "/base/AMUncaughtException";
        requestContext.getResponse().sendRedirect(
                redirectUrl);
    }
}
