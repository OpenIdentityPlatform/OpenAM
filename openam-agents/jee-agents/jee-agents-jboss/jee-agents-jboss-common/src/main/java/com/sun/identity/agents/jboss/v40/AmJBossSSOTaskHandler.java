/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: AmJBossSSOTaskHandler.java,v 1.1 2010/02/05 00:31:44 leiming Exp $
 */
/**
 * Portions Copyrighted 2013 ForgeRock, Inc.
 */
package com.sun.identity.agents.jboss.v40;

import com.sun.identity.agents.filter.SSOTaskHandler;
import com.sun.identity.agents.filter.AmFilterRequestContext;
import com.sun.identity.agents.filter.AmFilterResult;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Manager;
import javax.servlet.http.HttpServletRequest;

/**
 * This task handler provides the necessary functionality to process incoming requests for Single Sign-On for JBoss when
 * running the agent in J2EE_POLICY/ALL mode.
 */
public class AmJBossSSOTaskHandler extends SSOTaskHandler {

    public AmJBossSSOTaskHandler(Manager manager) {
        super(manager);
    }

    /**
     * This method makes sure that the goto URL stays correct in case form based login is enabled.
     *
     * @param ctx the AmFilterRequestContext object that carries information about the incoming request and response
     * objects.
     * @return AmFilterResult object indicating the necessary action in order to obtain valid SSO credentials.
     */
    protected AmFilterResult doSSOLogin(AmFilterRequestContext ctx) throws AgentException {
        HttpServletRequest request = ctx.getHttpServletRequest();

        if (ctx.isFormLoginRequest()) {
            Object obj = request.getAttribute("javax.servlet.forward.request_uri");
            if (obj != null) {
                String uri = obj.toString().trim();
                if (uri != null && uri.length() > 0) {
                    String gotoURL = ctx.getBaseURL() + uri;
                    obj = request.getAttribute("javax.servlet.forward.query_string");
                    if (obj != null) {
                        String query = obj.toString().trim();
                        if (query != null && query.length() > 0) {
                            gotoURL = gotoURL + "?" + query;
                        }
                    }
                    ctx.setGotoFormLoginURL(gotoURL);
                }
            }
        }
        return super.doSSOLogin(ctx);
    }
}
