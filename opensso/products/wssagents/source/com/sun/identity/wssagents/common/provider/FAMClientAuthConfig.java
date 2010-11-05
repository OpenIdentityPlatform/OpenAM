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
 * $Id: FAMClientAuthConfig.java,v 1.2 2008/06/25 05:54:47 qcheng Exp $
 *
 */

package com.sun.identity.wssagents.common.provider;

import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.config.ClientAuthConfig;
import javax.security.auth.message.config.ClientAuthContext;

public class FAMClientAuthConfig implements ClientAuthConfig {
    
    private String layer = null;
    private String appContext = null;
    private CallbackHandler callbackHandler = null;
    private FAMClientAuthContext clientAuthContext = null;
    
    private String secDisabled = null;
    private static final String TRUE="true";
    private static final String FALSE="false";
    
    /** Creates a new instance of FAMClientAuthConfig */
    public FAMClientAuthConfig(String layer, String appContext, 
        CallbackHandler callbackHandler) {
        this.layer = layer;
        this.appContext = appContext;
        this.callbackHandler = callbackHandler; 
    }

    public ClientAuthContext getAuthContext(String operation, Subject subject, 
        Map map) throws AuthException {

        map.put("providername", this.appContext);
        
        this.secDisabled = FALSE;
        
        if (clientAuthContext == null) {
            clientAuthContext = 
                new FAMClientAuthContext(operation, subject, map, 
                    callbackHandler);
        }
       
        return clientAuthContext;
    }

    public String getMessageLayer() {
        return layer;
    }

    public String getAppContext() {
        return appContext;
    }

    public String getOperation(MessageInfo messageInfo) {
        return null;
    }

    public void refresh() {
    }

    public String getAuthContextID(MessageInfo messageInfo) {
        return null;
    }

    public boolean isProtected() {
        return true;
    }
    
}
