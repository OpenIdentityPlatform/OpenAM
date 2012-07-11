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
 * $Id: AgentConstants.java,v 1.1 2008/11/22 02:41:19 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.plugin.services.agent;

import com.sun.identity.diagnostic.plugin.services.common.ClientConstants;
import com.sun.identity.diagnostic.plugin.services.common.ServiceConstants;

/**
 * This interface contains the property names used by the
 * Agent configuration service
 */
public interface AgentConstants extends ServiceConstants, ClientConstants {
    
    /**
     * Resource file name used by Agent service
     */
    String AGENT_RESOURCE_BUNDLE = "AgentConfiguration";
    
    /**
     * Generic agent related properties shared for J2EE and Web agent
     */
    String AGENT_CDSSO_ENABLE = "com.sun.identity.agents.config.cdsso.enable";
    String AGENT_CDSSO_URL = 
        "com.sun.identity.agents.config.cdsso.cdcservlet.url";
}
