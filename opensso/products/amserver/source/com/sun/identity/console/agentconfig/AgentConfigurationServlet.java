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
 * $Id: AgentConfigurationServlet.java,v 1.2 2008/06/25 05:42:44 qcheng Exp $
 *
 */

package com.sun.identity.console.agentconfig;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestContextImpl;
import com.iplanet.jato.ViewBeanManager;
import com.sun.identity.console.base.ConsoleServletBase;

/**
 * Controller servlet for the realm authentication pages.
 */
public class AgentConfigurationServlet 
    extends ConsoleServletBase 
{
    public static final String DEFAULT_MODULE_URL = "../agentconfig";
    public static String PACKAGE_NAME=
        getPackageName(AgentConfigurationServlet.class.getName());
    
    /**
     * Initializes request context and set the viewbean manager.
     *
     * @param requestContext current request context
     */
    protected void initializeRequestContext(RequestContext requestContext) {
        super.initializeRequestContext(requestContext);        
        ViewBeanManager viewBeanManager =
            new ViewBeanManager(requestContext,PACKAGE_NAME);
        ((RequestContextImpl)requestContext).setViewBeanManager(
            viewBeanManager);
    }
    
    /**
     * Returns the module URL.
     *
     * @return Returns the module URL as String
     */
    public String getModuleURL() {
        return DEFAULT_MODULE_URL;
    }
}
