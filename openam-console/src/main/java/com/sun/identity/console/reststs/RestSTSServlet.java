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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package com.sun.identity.console.reststs;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestContextImpl;
import com.iplanet.jato.ViewBeanManager;
import com.sun.identity.console.base.ConsoleServletBase;

public class RestSTSServlet extends ConsoleServletBase {
    public static final String DEFAULT_MODULE_URL = "../reststs";
    public static String PACKAGE_NAME =
            getPackageName(RestSTSServlet.class.getName());

    /**
     * Initialize request context and set the viewbean manager
     *
     * @param requestContext current request context
     */
    protected void initializeRequestContext(RequestContext requestContext) {
        super.initializeRequestContext(requestContext);
        ViewBeanManager viewBeanManager =
                new ViewBeanManager(requestContext, PACKAGE_NAME);
        ((RequestContextImpl) requestContext).setViewBeanManager(
                viewBeanManager);
    }

    /**
     * gets the modules URL
     *
     * @return Returns the module URL as String
     */
    public String getModuleURL() {
        return DEFAULT_MODULE_URL;
    }
}

