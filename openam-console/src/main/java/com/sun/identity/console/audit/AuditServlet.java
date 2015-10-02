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
package com.sun.identity.console.audit;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestContextImpl;
import com.iplanet.jato.ViewBeanManager;
import com.sun.identity.console.base.ConsoleServletBase;

/**
 * Servlet responsible for initializing the request context and setting the viewbean manager for Audit configuration.
 *
 * @since 13.0.0
 */
public class AuditServlet extends ConsoleServletBase {

    private static final String DEFAULT_MODULE_URL = "../audit";
    private static final String PACKAGE_NAME = getPackageName(AuditServlet.class.getName());

    /**
     * Initialize request context and set the viewbean manager.
     *
     * @param requestContext current request context
     */
    protected void initializeRequestContext(RequestContext requestContext) {
        super.initializeRequestContext(requestContext);
        ViewBeanManager viewBeanManager = new ViewBeanManager(requestContext, PACKAGE_NAME);
        ((RequestContextImpl) requestContext).setViewBeanManager(viewBeanManager);
    }

    /**
     * Gets the modules URL
     *
     * @return Returns the module URL as String
     */
    public String getModuleURL() {
        return DEFAULT_MODULE_URL;
    }
}

