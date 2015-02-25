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
 * $Id: SessionAttributeTaskHandler.java,v 1.2 2008/06/25 05:51:49 qcheng Exp $
 *
 */

package com.sun.identity.agents.filter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOToken;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Manager;


/**
 * <p>
 * This task handler provides the necessary functionality to process incoming
 * requests for providing access to Session Attributes for the current
 * user. 
 * </p>
 */

public class SessionAttributeTaskHandler extends AttributeTaskHandler implements
        ISessionAttributeTaskHandler {
    
    public SessionAttributeTaskHandler(Manager manager) {
        super(manager);
    }

    public void initialize(ISSOContext context, AmFilterMode mode) 
    throws AgentException {
        super.initialize(context, mode, CONFIG_SESSION_ATTRIBUTE_FETCH_MODE,
                CONFIG_SESSION_ATTRIBUTE_MAP);
    }

    public String getHandlerName() {
        return AM_FILTER_SESSION_ATTRIBUTE_TASK_HANDLER_NAME;
    }
    
    protected Map getUserAttributes(AmFilterRequestContext ctx, Map queryMap) 
    throws AgentException
    {
        Map userAttributeMap = new HashMap();
        try {
            SSOToken token = ctx.getSSOValidationResult().getSSOToken();
            if (token != null) {
                Iterator it = queryMap.keySet().iterator();
                while (it.hasNext()) {
                    String sessionPropName = (String) it.next();
                    String userAttributeName = (String)
                                            queryMap.get(sessionPropName);
                    Set valueSet = new HashSet();
                    String value = token.getProperty(sessionPropName);
                    if (value != null) {
                        valueSet.add(value);
                    }
                    userAttributeMap.put(userAttributeName, valueSet);
                }
            }
        } catch (Exception ex) {
            throw new AgentException("Unable to get session attributes", ex);
        }
        return userAttributeMap;
    }
    
    protected String getRequestMarker() {
        return AM_FILTER_SESSION_ATTRIBUTE_REQUEST_MARKER;
    }
}
