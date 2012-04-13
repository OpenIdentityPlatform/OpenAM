/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
package org.forgerock.openam.agents.filter;

import com.sun.identity.agents.arch.AgentConfiguration;
import com.sun.identity.agents.arch.ServiceFactory;
import com.sun.identity.agents.filter.AmFilterRequestContext;
import com.sun.identity.agents.filter.AmFilterResult;
import com.sun.identity.agents.filter.AmFilterTaskHandler;
import com.sun.identity.agents.filter.ISSOContext;
import com.sun.identity.agents.filter.InitialPDPTaskHandler;

/**
 * This helper class can be used in cases, when the caller TaskHandler needs to
 * perform some redirection, but we still want the Post Data to preserved.
 *
 * For more details:
 * @see com.sun.identity.agents.filter.SSOTaskHandler
 * @see com.sun.identity.agents.filter.URLPolicyTaskHandler
 *
 * @author Peter Major
 */
public class PDPInitHelper {

    public static AmFilterResult initializePDP(AmFilterTaskHandler parentTask,
            AmFilterRequestContext ctx, ISSOContext ssoContext) {
        AmFilterResult pdpResult = null;
        try {
            String pdpTaskHandlerImplClass =
                    AgentConfiguration.getServiceResolver().
                    getInitialPDPTaskHandlerImpl();
            InitialPDPTaskHandler pdpHandler =
                    (InitialPDPTaskHandler) ServiceFactory.getServiceInstance(parentTask.getManager(),
                    pdpTaskHandlerImplClass);
            pdpHandler.initialize(ssoContext, ctx.getFilterMode());
            if (pdpHandler.isActive()) {
                pdpResult = pdpHandler.process(ctx);
            }
        } catch (Exception ex) {
            parentTask.logError("SSOTaskHandler: Error while "
                    + " delegating to PDPTaskHandler from " + parentTask.getHandlerName(), ex);
        }

        return pdpResult;
    }
}
