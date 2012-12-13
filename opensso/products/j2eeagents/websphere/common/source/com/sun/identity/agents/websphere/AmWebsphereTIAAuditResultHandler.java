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
 * $Id: AmWebsphereTIAAuditResultHandler.java,v 1.2 2008/11/21 22:21:45 leiming Exp $
 *
 */

package com.sun.identity.agents.websphere;

import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.LocalizedMessage;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.filter.AmFilterMode;
import com.sun.identity.agents.filter.AuditResultHandler;
import com.sun.identity.agents.filter.ISSOContext;

/**
 * Websphere specific result handler for audit purpose.
 */
public class AmWebsphereTIAAuditResultHandler extends AuditResultHandler {
    
    public AmWebsphereTIAAuditResultHandler(Manager manager) {
        super(manager);
    }
    
    public void initialize(ISSOContext context, AmFilterMode mode)
    throws AgentException {
        super.initialize(context, mode);
        
        if (getManager() instanceof AmWebsphereManager) {
            setWebsphereModuleFlag(true);
        } else {
            setWebsphereModuleFlag(false);
        }
    }
    
    protected LocalizedMessage getAllowMessage(
            String userName, String requestURL) {
        LocalizedMessage result = null;
        if (isInWebsphereModule()) {
            result = getModule().makeLocalizableString(
                    IAmWebsphereModuleConstants.MSG_AM_WEBSPHERE_AUTH_SUCCESS,
                    new Object[] { userName, requestURL });
        } else {
            result = super.getAllowMessage(userName, requestURL);
        }
        return result;
    }
    
    protected LocalizedMessage getDenyMessage(
            String userName, String requestURL) {
        LocalizedMessage result = null;
        if (isInWebsphereModule()) {
            result = getModule().makeLocalizableString(
                    IAmWebsphereModuleConstants.MSG_AM_WEBSPHERE_AUTH_FAILED,
                    new Object[] { userName, requestURL });
        } else {
            result = super.getDenyMessage(userName, requestURL);
        }
        
        return result;
    }
    
    private boolean isInWebsphereModule() {
        return _isInWebsphereModule;
    }
    
    private void setWebsphereModuleFlag(boolean flag) {
        _isInWebsphereModule = flag;
        if (isLogMessageEnabled()) {
            logMessage("WebsphereTIAAuditResultHandler: " +
                    "inside websphere module: " + true);
        }
    }
    
    private boolean _isInWebsphereModule;
    
}
