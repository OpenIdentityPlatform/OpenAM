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
 * $Id: AmWebsphereTAINotificationTaskHandler.java,v 1.2 2008/11/21 22:21:45 leiming Exp $
 *
 */

package com.sun.identity.agents.websphere;

import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.filter.AmFilterMode;
import com.sun.identity.agents.filter.AmFilterRequestContext;
import com.sun.identity.agents.filter.AmFilterResult;
import com.sun.identity.agents.filter.AmFilterResultStatus;
import com.sun.identity.agents.filter.ISSOContext;
import com.sun.identity.agents.filter.NotificationTaskHandler;

/**
 * Notification handling class, which does not handle notification in TAI.
 */
public class AmWebsphereTAINotificationTaskHandler extends
        NotificationTaskHandler {
    
    public AmWebsphereTAINotificationTaskHandler(Manager manager) {
        super(manager);
    }
    
    public void initialize(ISSOContext context, AmFilterMode mode)
    throws AgentException {
        super.initialize(context, mode);
        
        if (getManager() instanceof AmWebsphereManager) {
            setInWebsphereModuleFlag(true);
        } else {
            setInWebsphereModuleFlag(false);
        }
    }
    
    protected AmFilterResult handleNotification(AmFilterRequestContext ctx) {
        AmFilterResult result = null;
        if (isInWebsphereModule()) {
            // Notifications are processed by regular filter
            result = new AmFilterResult(AmFilterResultStatus.STATUS_CONTINUE);
            if (isLogMessageEnabled()) {
                logMessage("AmWebsphereTAINotificationTaskHandler: no handling"
                        + " needed in websphere module");
            }
        } else {
            result = super.handleNotification(ctx);
        }
        
        return result;
    }
    
    private boolean isInWebsphereModule() {
        return _isInWebsphereModule;
    }
    
    private void setInWebsphereModuleFlag(boolean flag) {
        _isInWebsphereModule = flag;
        
        if (isLogMessageEnabled()) {
            logMessage("AmWebsphereTAINotificationTaskHandler: " +
                    "in websphere module: " + _isInWebsphereModule);
        }
        
    }
    
    private boolean _isInWebsphereModule;
    
}
