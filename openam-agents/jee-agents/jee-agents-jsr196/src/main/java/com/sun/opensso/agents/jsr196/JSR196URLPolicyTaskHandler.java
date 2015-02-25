/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: JSR196URLPolicyTaskHandler.java,v 1.1 2009/01/30 12:09:41 kalpanakm Exp $
 *
 */

package com.sun.opensso.agents.jsr196;

import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Manager;

import com.sun.identity.agents.filter.URLPolicyTaskHandler;
import com.sun.identity.agents.filter.AmFilterResult;
import com.sun.identity.agents.filter.AmFilterRequestContext;

/**
 * @author kalpana
 * 
 * JSR196URLPolicyTaskHandler is a psuedo implementation of URLPolicyTaskHandler
 * to satisfy the J2EE Policy 
 * 
 */
public class JSR196URLPolicyTaskHandler extends URLPolicyTaskHandler {
    
     public JSR196URLPolicyTaskHandler(Manager manager) {
        super(manager);
    }
     
     /**
      * 
      * @param ctx <code> AmFilterRequestContext </code>
      * @return results Redirect Result irrespective of the context
      *         
      * @throws com.sun.identity.agents.arch.AgentException
      */
     
    @Override
    public AmFilterResult process(AmFilterRequestContext ctx)
    throws AgentException {
        // Need not do any processing right now.
        // Just Return the Redirect Result, let the processing happen later
        // from OpenSSOJACCConfiguration
        return ctx.getAuthRedirectResult();
    }
    
}
