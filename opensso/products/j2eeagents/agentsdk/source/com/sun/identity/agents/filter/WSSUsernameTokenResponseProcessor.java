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
 * $Id: WSSUsernameTokenResponseProcessor.java,v 1.1 2008/10/07 17:36:32 huacui Exp $
 *
 */

package com.sun.identity.agents.filter;

import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.ISystemAccess;

/**
 * WSSUsernameTokenResponseProcessor class provides implementation of 
 * processing the web service response. It needs to be used with
 * WSSUsernameTokenAuthenticator
 */
public class WSSUsernameTokenResponseProcessor implements IWebServiceResponseProcessor {
    
    public WSSUsernameTokenResponseProcessor() throws AgentException {        
        ISystemAccess systemAccess = AmFilterManager.getSystemAccess();
        setSystemAccess(systemAccess);
    }

    /**
     * Processes the response
     */
    public String process(String providerName, String respContent) {
        if (getSystemAccess().isLogMessageEnabled()) {
            getSystemAccess().logMessage(
            "WSSUsernameTokenResponseProcessor: processed response content= "
            + respContent);
        }
        return respContent;
    }

    
    private ISystemAccess getSystemAccess() {
        return _systemAccess;
    }
    
    private void setSystemAccess(ISystemAccess systemAccess) {
        _systemAccess = systemAccess;
    }
    
    private ISystemAccess _systemAccess;
}
