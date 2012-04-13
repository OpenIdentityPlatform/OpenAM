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
 * $Id: AmWebPolicyManager.java,v 1.2 2008/06/25 05:51:57 qcheng Exp $
 *
 */

package com.sun.identity.agents.policy;

import com.sun.identity.agents.arch.AgentConfiguration;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.IConfigurationListener;
import com.sun.identity.agents.arch.IModuleAccess;
import com.sun.identity.agents.arch.ISystemAccess;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.arch.Module;
import com.sun.identity.agents.arch.ServiceFactory;

/**
 * The manager class of the web policy module
 */
public class AmWebPolicyManager extends Manager {
    
    private AmWebPolicyManager() throws AgentException {
        super(AmWebPolicyModule.getModule(), new String[] {});
        
        setAmWebPolicy(ServiceFactory.getAmWebPolicy(this));
    }
    
    private IAmWebPolicy getAmWebPolicy() {
        return _amWebPolicy;
    }
    
    private void setAmWebPolicy(IAmWebPolicy amWebPolicy) {
        _amWebPolicy = amWebPolicy;
    }
    
    private IAmWebPolicy _amWebPolicy;
    
    
    public static IModuleAccess getModuleAccess() {
        return AmWebPolicyModule.getModule().newModuleAccess();
    }
    
    public static IAmWebPolicy getAmWebPolicyInstance() {
        return getAmWebPolicyManager().getAmWebPolicy();
    }
    
    public static ISystemAccess getSystemAccess() {
        return getAmWebPolicyManager().newSystemAccess();
    }
    
    private static AmWebPolicyManager getAmWebPolicyManager() {
        return _manager;
    }

    private static void notifyConfigurationChange() {

        if(isLogMessageEnabled()) {
            logMessage(SUBSYSTEM_NAME
                       + ": Received configuration notification");
            logMessage(SUBSYSTEM_NAME + ": Swapping manager instance");
        }

        try {
            AmWebPolicyManager manager = new AmWebPolicyManager();

            _manager = manager;
        } catch(Exception ex) {
            logError(SUBSYSTEM_NAME + ": Unable to swap manager instance", ex);
            logError(SUBSYSTEM_NAME + ": Dynamic property load failed");
        }
    }

    static class AmWebPolicyConfigurationListener
            implements IConfigurationListener {

        public void configurationChanged() {
            AmWebPolicyManager.notifyConfigurationChange();
        }

        public String getName() {
            return SUBSYSTEM_NAME;
        }
    }

    
    private static void logError(String msg) {
        getAmWebPolicyModule().logError(msg);
    }
    
    private static void logError(String msg, Throwable th) {
        getAmWebPolicyModule().logError(msg, th);
    }
    
    private static void logMessage(String msg) {
        getAmWebPolicyModule().logMessage(msg);
    }
    
    private static boolean isLogMessageEnabled() {
        return getAmWebPolicyModule().isLogMessageEnabled();
    }
    
    private static Module getAmWebPolicyModule() {
        return AmWebPolicyModule.getModule();
    }
 
    private static AmWebPolicyManager _manager = null;
    private static final String SUBSYSTEM_NAME = "AmWebPolicy";
    
    static {
        try {
            _manager = new AmWebPolicyManager();
        } catch(AgentException ex) {
            AmWebPolicyModule.getModule().logError(
                "AmWebPolicyManager initialization failed", ex);

            throw new RuntimeException(
                "Exception caught in AmWebPolicyManager initializer: "
                + ex.getMessage());
        }

        AgentConfiguration.addConfigurationListener(
            new AmWebPolicyConfigurationListener());
    }    
}
