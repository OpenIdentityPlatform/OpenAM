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
 * $Id: AmWLSSOTaskHandler.java,v 1.2 2008/06/25 05:52:22 qcheng Exp $
 *
 */

package com.sun.identity.agents.weblogic.v10;

import com.sun.identity.agents.filter.SSOTaskHandler;
import com.sun.identity.agents.filter.AmFilterRequestContext;
import com.sun.identity.agents.filter.AmFilterResult;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Manager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import weblogic.servlet.internal.session.SessionInternal;

/**
 * This task handler provides the necessary functionality to process incoming
 * requests for Single Sign-On for Weblogic
 *
 */
public class AmWLSSOTaskHandler extends SSOTaskHandler {
    
    public AmWLSSOTaskHandler(Manager manager) {
        super(manager);
    }
    
    /**
     * Method doSSOLogin
     *
     * @param ctx the AmFilterRequestContext object that carries
     * information about the incoming request and response objects
     *
     * @return AmFilterResult object indicating the necessary action in
     * order to obtain valid SSO credentials
     *
     */
    protected AmFilterResult doSSOLogin(AmFilterRequestContext ctx)
    throws AgentException {
        HttpServletRequest request = ctx.getHttpServletRequest();
        String gotoURL = null;
        
        HttpSession session = request.getSession(false);
        if (session != null && ctx.isFormLoginRequest()) {
            // Checks if originally requested URL exists
            // in Weblogic internal session
            SessionInternal sessionInternal = (SessionInternal) session;
            if (sessionInternal != null) {
                gotoURL = (String) sessionInternal.
                        getInternalAttribute("weblogic.formauth.targeturl");
            }
            if (gotoURL != null) {
                ctx.setGotoFormLoginURL(gotoURL);
            }
        }
        return super.doSSOLogin(ctx);
    }
}


