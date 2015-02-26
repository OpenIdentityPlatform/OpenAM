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
 * $Id: IAmWebPolicy.java,v 1.3 2008/06/25 05:51:57 qcheng Exp $
 *
 */

package com.sun.identity.agents.policy;
	
import javax.servlet.http.HttpServletRequest;

import com.iplanet.sso.SSOToken;
import com.sun.identity.agents.arch.AgentException;

/**
 * The interface for web policy module
 */
public interface IAmWebPolicy {
    public abstract void initialize() throws AgentException;

    public abstract AmWebPolicyResult checkPolicyForResource(SSOToken ssoToken,
            String resource, String action, String ipAddress, String hostName,
            HttpServletRequest request);

    public static final String AM_WEB_SERVICE_NAME = "iPlanetAMWebAgentService";

    public static final String ALLOW_VALUE = "allow";

    public static final String DENY_VALUE = "deny";

    public static final String AUTH_SCHEME_ADVICE_RESPONSE = 
            "AuthSchemeConditionAdvice";

    public static final String AUTH_SCHEME_URL_PREFIX = "module";

    public static final String AUTH_LEVEL_ADVICE_RESPONSE = 
            "AuthLevelConditionAdvice";

    public static final String AUTH_LEVEL_URL_PREFIX = "authlevel";

}
