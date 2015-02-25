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
 * $Id: AmRealmManager.java,v 1.3 2008/06/25 05:51:58 qcheng Exp $
 *
 */

package com.sun.identity.agents.realm;

import com.sun.identity.agents.arch.AgentConfiguration;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.IConfigurationListener;
import com.sun.identity.agents.arch.IModuleAccess;
import com.sun.identity.agents.arch.ISystemAccess;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.arch.Module;
import com.sun.identity.agents.arch.ServiceFactory;

/**
 * The manager class for agent realm component
 */
public class AmRealmManager extends Manager 
implements IRealmConfigurationConstants 
{

    private AmRealmManager() throws AgentException {
        super(AmRealmModule.getModule(), new String[] {});        
        setAmRealm(ServiceFactory.getAmRealm(this));
    }
    
    private IAmRealm getAmRealm() {
        return _amRealm;
    }
    
    private void setAmRealm(IAmRealm amRealm) {
        _amRealm = amRealm;
    }
    
    private IAmRealm _amRealm;
    
    //------------- static service API methods    ----------//
    
    
    public static IModuleAccess getModuleAccess() {
        return AmRealmModule.getModule().newModuleAccess();
    }
    
    public static ISystemAccess getSystemAccess() {
        return getAmRealmManager().newSystemAccess();
    }
    
    public static IAmRealm getAmRealmInstance() {
        return getAmRealmManager().getAmRealm();
    }
    
    //------- static housekeeping methods and variables -----//
    
    private static AmRealmManager getAmRealmManager() {
        return _manager;
    }
    
    private static void notifyConfigurationChange() {

        if(isLogMessageEnabled()) {
            logMessage(SUBSYSTEM_NAME
                       + ": Received configuration notification");
            logMessage(SUBSYSTEM_NAME + ": Swapping manager instance");
        }

        try {
            AmRealmManager manager = new AmRealmManager();

            _manager = manager;
        } catch(Exception ex) {
            logError(SUBSYSTEM_NAME + ": Unable to swap manager instance",
                     ex);
            logError(SUBSYSTEM_NAME + ": Dynamic property load failed");
        }
    }
    
    static class AmRealmConfigurationListener
                    implements IConfigurationListener 
    {

        public void configurationChanged() {
            AmRealmManager.notifyConfigurationChange();
        }

        public String getName() {
            return SUBSYSTEM_NAME;
        }
    }
    
    private static void logError(String msg) {
        getAmRealmModule().logError(msg);
    }
    
    private static void logError(String msg, Throwable th) {
        getAmRealmModule().logError(msg, th);
    }
    
    private static void logMessage(String msg) {
        getAmRealmModule().logMessage(msg);
    }
    
    private static boolean isLogMessageEnabled() {
        return getAmRealmModule().isLogMessageEnabled();
    }
    
    private static Module getAmRealmModule() {
        return AmRealmModule.getModule();
    }
    
    private static AmRealmManager _manager = null;
    private static final String SUBSYSTEM_NAME = "AmRealm";
    static {
        try {
            _manager = new AmRealmManager();
        } catch(AgentException ex) {
            AmRealmModule.getModule().logError(
                "AmRealmManager initialization failed", ex);

            throw new RuntimeException(
                "Exception caught in AmRealmManager initializer: "
                + ex.getMessage());
        }

        AgentConfiguration.addConfigurationListener(
            new AmRealmConfigurationListener());
    }
}
