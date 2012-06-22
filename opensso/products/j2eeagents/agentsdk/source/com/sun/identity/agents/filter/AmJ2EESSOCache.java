/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AmJ2EESSOCache.java,v 1.2 2008/06/25 05:51:43 qcheng Exp $
 *
 */

package com.sun.identity.agents.filter;

import java.security.Principal;

import javax.ejb.EJBContext;
import javax.servlet.http.HttpServletRequest;

import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Manager;

public class AmJ2EESSOCache extends AmBaseSSOCache {
    
    public AmJ2EESSOCache(Manager manager) throws AgentException {
        super(manager);
    }
    
    public String getSSOTokenForUser(Object ejbContextOrServletRequest) {
        String result = null;
        if (ejbContextOrServletRequest != null) {
            if (ejbContextOrServletRequest instanceof HttpServletRequest) {
                HttpServletRequest request = (HttpServletRequest)
                ejbContextOrServletRequest;
                result = getSSOTokenValidator().getSSOTokenValue(request);
            } else if (ejbContextOrServletRequest instanceof EJBContext){
                EJBContext context = (EJBContext)ejbContextOrServletRequest;
                if (context != null) {
                    Principal principal = context.getCallerPrincipal();
                    if (principal != null) {
                        result = getSSOTokenForUserInternal(
                                principal.getName());
                    }
                }
            } else {
                if (isLogWarningEnabled()) {
                    String type =
                            ejbContextOrServletRequest.getClass().getName();
                    logWarning("AmJ2EESSOCache: Invalid object type for sso "
                            + "cache lookup: " + type);
                }
            }
        } else {
            if (isLogWarningEnabled()) {
                logWarning("AmJ2EESSOCache: null cache lookup key specified");
            }
        }
        
        return result;
    }
    
}
