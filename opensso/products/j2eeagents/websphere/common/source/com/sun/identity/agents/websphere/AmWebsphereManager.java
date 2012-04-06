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
 * $Id: AmWebsphereManager.java,v 1.2 2008/11/21 22:21:45 leiming Exp $
 *
 */

package com.sun.identity.agents.websphere;

import com.sun.identity.agents.arch.AgentConfiguration;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.IConfigurationListener;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.arch.Module;
import com.sun.identity.agents.filter.IFilterConfigurationConstants;

/**
 * Websphere specific Manager class, one of agent frameworks.
 */
public class AmWebsphereManager extends Manager {
    
    private AmWebsphereManager() throws AgentException {
        super(AmWebsphereModule.getModule(), new String[] {
            IFilterConfigurationConstants.CONFIG_FILTER_MODE});
        
        setIdentityAsserter(
                AmWebsphereServiceFactory.getAmIdentityAsserter(this));
        
        setRealmUserRegistry(
                AmWebsphereServiceFactory.getAmRealmUserRegistry(this));
    }
    
    private void setIdentityAsserter(IAmIdentityAsserter asserter) {
        _identityAsserter = asserter;
    }
    
    private IAmIdentityAsserter getIdentityAsserter() {
        return _identityAsserter;
    }
    
    private void setRealmUserRegistry(IAmRealmUserRegistry registry) {
        _realmUserRegistry = registry;
    }
    
    private IAmRealmUserRegistry getRealmUserRegistry() {
        return _realmUserRegistry;
    }
    
    private IAmIdentityAsserter _identityAsserter;
    
    private IAmRealmUserRegistry _realmUserRegistry;
    
    //------------- static service API methods    ----------//
    
    public static IAmIdentityAsserter getAmIdentityAsserterInstance() {
        return getAmWebsphereManager().getIdentityAsserter();
    }
    
    public static IAmRealmUserRegistry getAmRealmUserRegistryInstance() {
        return getAmWebsphereManager().getRealmUserRegistry();
    }
    
    //------- static housekeeping methods and variables -----//
    
    private static synchronized AmWebsphereManager getAmWebsphereManager() {
        return _manager;
    }
    
    private static synchronized void setAmWebsphereManager(
            AmWebsphereManager manager) {
        _manager = manager;
    }
    
    private static void notifyConfigurationChange() {
        
        if(isLogMessageEnabled()) {
            logMessage(SUBSYSTEM_NAME
                    + ": Received configuration notification");
            logMessage(SUBSYSTEM_NAME + ": Swapping manager instance");
        }
        
        try {
            AmWebsphereManager manager = new AmWebsphereManager();
            setAmWebsphereManager(manager);
        } catch(Exception ex) {
            logError(SUBSYSTEM_NAME + ": Unable to swap manager instance",
                    ex);
            logError(SUBSYSTEM_NAME + ": Dynamic property load failed");
        }
    }
    
    static class AmWebsphereConfigurationListener
            implements IConfigurationListener {
        
        public void configurationChanged() {
            AmWebsphereManager.notifyConfigurationChange();
        }
        
        public String getName() {
            return SUBSYSTEM_NAME;
        }
    }
    
    private static void logError(String msg) {
        getAmWebsphereModule().logError(msg);
    }
    
    private static void logError(String msg, Throwable th) {
        getAmWebsphereModule().logError(msg, th);
    }
    
    private static void logMessage(String msg) {
        getAmWebsphereModule().logMessage(msg);
    }
    
    private static boolean isLogMessageEnabled() {
        return getAmWebsphereModule().isLogMessageEnabled();
    }
    
    private static Module getAmWebsphereModule() {
        return AmWebsphereModule.getModule();
    }
    
    private static AmWebsphereManager _manager = null;
    private static final String SUBSYSTEM_NAME = "AmWebsphere";
    
    static {
        try {
            _manager = new AmWebsphereManager();
        } catch(AgentException ex) {
            AmWebsphereModule.getModule().logError(
                    "AmWebsphereManager initialization failed", ex);
            
            throw new RuntimeException(
                    "Exception caught in AmWebsphereManager initializer: "
                    + ex.getMessage());
        }
        
        AgentConfiguration.addConfigurationListener(
                new AmWebsphereConfigurationListener());
    }
}
