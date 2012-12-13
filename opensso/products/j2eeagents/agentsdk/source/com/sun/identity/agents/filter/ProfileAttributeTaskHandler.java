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
 * $Id: ProfileAttributeTaskHandler.java,v 1.2 2008/06/25 05:51:48 qcheng Exp $
 *
 */

package com.sun.identity.agents.filter;

import java.util.Map;

import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.common.IProfileAttributeHelper;

/**
 * <p>
 * This task handler provides the necessary functionality to process incoming
 * requests for providing access to various Profile Attributes for the current
 * user in various forms such as request attributes, cookies and HTTP headers
 * depending upon the set Agent Configuration.
 * </p>
 */
public class ProfileAttributeTaskHandler extends AttributeTaskHandler
        implements IProfileAttributeTaskHandler {
    
    public ProfileAttributeTaskHandler(Manager manager) {
        super(manager);
    }
    
    public void initialize(ISSOContext context, AmFilterMode mode) 
    throws AgentException {
        super.initialize(context, mode, CONFIG_PROFILE_ATTRIBUTE_FETCH_MODE,
                CONFIG_PROFILE_ATTRIBUTE_MAP);
        if (isAttributeFetchEnabled()) {
            setProfileAttributeHelper(
                    getCommonFactory().newProfileAttributeHelper());
        }
    }
    
    public String getHandlerName() {
        return AM_FILTER_PROFILE_ATTRIBUTE_TASK_HANDLER_NAME;
    }
    
    protected Map getUserAttributes(AmFilterRequestContext ctx, Map queryMap)
    throws AgentException {
        return getProfileAttributeHelper().getAttributeMap(
                ctx.getSSOValidationResult(), queryMap);
    }
    
    protected String getRequestMarker() {
        return AM_FILTER_PROFILE_ATTRIBUTE_REQUEST_MARKER;
    }
    
    private void setProfileAttributeHelper(IProfileAttributeHelper helper) {
        _attributeHelper = helper;
    }
    
    private IProfileAttributeHelper getProfileAttributeHelper() {
        return _attributeHelper;
    }
    
    private IProfileAttributeHelper _attributeHelper;
}
