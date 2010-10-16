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
 * $Id: ResponseAttributeTaskHandler.java,v 1.2 2008/06/25 05:51:48 qcheng Exp $
 *
 */

package com.sun.identity.agents.filter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Manager;

public class ResponseAttributeTaskHandler extends AttributeTaskHandler
        implements IResponseAttributeTaskHandler {
    
    public ResponseAttributeTaskHandler(Manager manager) {
        super(manager);
    }
    
    public void initialize(ISSOContext context, AmFilterMode mode) 
    throws AgentException {
        super.initialize(context, mode, 
                CONFIG_RESPONSE_ATTRIBUTE_FETCH_MODE, 
                CONFIG_RESPONSE_ATTRIBUTE_MAP);
    }
    
    public String getHandlerName() {
        return AM_FILTER_RESPONSE_ATTRIBUTE_TASK_HANDLER_NAME;
    }

    protected Map getUserAttributes(AmFilterRequestContext ctx, Map queryMap)
            throws AgentException {
        Map result = new HashMap(); 
        Map allAttributes = ctx.getPolicyResponseAttributes();
        if (allAttributes == null) {
            result = Collections.EMPTY_MAP;
        }
        Iterator it = queryMap.keySet().iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            Set value = (Set) allAttributes.get(name);
            if (value == null) {
                value = Collections.EMPTY_SET;
            }
            String mappedName = (String) queryMap.get(name);
            result.put(mappedName, value);
        }
        if (isLogMessageEnabled()) {
            logMessage("ResponseAttributeTaskHandler: Adding attributes: "
                    + result);
        }
        return result;
    }   
    
    protected String getRequestMarker() {
        return AM_FILTER_RESPONSE_ATTRIBUTE_REQUEST_MARKER;
    }
    
    protected boolean isAttributeFetchEnabled() {
       /**
        * Along with the checks done by the base class to see if the handler
        * should be active or not, this particular handler will only be
        * active if the mode URL policy is active in the containing filter.
        */
        return (super.isAttributeFetchEnabled() && isModeURLPolicyActive());
    }
}
