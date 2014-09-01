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
 * $Id: AmFilterManager.java,v 1.2 2008/06/25 05:51:43 qcheng Exp $
 *
 */
 /*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.agents.filter;

import java.util.HashMap;

import com.sun.identity.agents.arch.AgentConfiguration;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.IConfigurationListener;
import com.sun.identity.agents.arch.IModuleAccess;
import com.sun.identity.agents.arch.ISystemAccess;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.arch.Module;
import com.sun.identity.agents.arch.ServiceFactory;
import com.sun.identity.agents.common.IPDPCache;

/**
 * The class manages agent filter component 
 */
public class AmFilterManager extends Manager 
implements IFilterConfigurationConstants 
{

    private AmFilterManager() throws AgentException {
        super(AmFilterModule.getModule(), new String[] {
                CONFIG_FILTER_MODE
                }
        );

        setAmSSOCache(ServiceFactory.getAmSSOCache(this));
        setPDPCache(ServiceFactory.getPDPCache(this));
    }
    
    private IAmFilter getAmFilter(AmFilterMode mode) throws AgentException {
        IAmFilter result = (IAmFilter) getFilterMap().get(mode);
        if (result == null) {
            synchronized (this) {
                result = (IAmFilter) getFilterMap().get(mode);
                if (result == null) {
                    result = ServiceFactory.getAmFilter(this, mode);
                    getFilterMap().put(mode, result);
                }
            }
        }
        return result;
    }
    
    private IAmFilter getAmFilter() throws AgentException {
        return getAmFilter(null);
    }
    
    private IAmSSOCache getAmSSOCacheProvider() {
        return _ssoCache;
    }

    private IPDPCache getPDPCacheProvider() {
        return _pdpCache;
    }
    
    private void setAmSSOCache(IAmSSOCache cache) {
        _ssoCache = cache;
    }

    private void setPDPCache(IPDPCache cache) {
        _pdpCache = cache;
    }
    
    private HashMap getFilterMap() {
        return _filters;
    }
    
    private HashMap _filters = new HashMap();
    private IAmSSOCache _ssoCache;
    private IPDPCache _pdpCache;
    
    //------------- static service API methods    ----------//
    
    public static IModuleAccess getModuleAccess() {
        return AmFilterModule.getModule().newModuleAccess();
    }
    
    public static ISystemAccess getSystemAccess() {
        return getAmFilterManager().newSystemAccess();
    }
    
    public static IAmSSOCache getAmSSOCache() {
        return getAmFilterManager().getAmSSOCacheProvider();
    }

    public static IPDPCache getPDPCache() {
        return getAmFilterManager().getPDPCacheProvider();
    }
    
    public static IAmFilter getAmFilterInstanceModeConfigured() 
    throws AgentException 
    {
        return getAmFilterManager().getAmFilter();
    }

    public static IAmFilter getAmFilterInstanceModeNone() 
    throws AgentException
    {
        return getAmFilterManager().getAmFilter(AmFilterMode.MODE_NONE);
    }
    
    public static IAmFilter getAmFilterInstanceModeSSOOnly() 
    throws AgentException
    {
        return getAmFilterManager().getAmFilter(AmFilterMode.MODE_SSO_ONLY);
    }
    
    public static IAmFilter getAmFilterInstanceModeJ2EEPolicy() 
    throws AgentException
    {
        return getAmFilterManager().getAmFilter(AmFilterMode.MODE_J2EE_POLICY);
    }
    
    public static IAmFilter getAmFilterInstanceModeURLPolicy() 
    throws AgentException
    {
        return getAmFilterManager().getAmFilter(AmFilterMode.MODE_URL_POLICY);
    }
    
    public static IAmFilter getAmFilterInstanceModeAll() 
    throws AgentException
    {
        return getAmFilterManager().getAmFilter(AmFilterMode.MODE_ALL);
    }
    
    public static IAmFilter getAmFilterInstance(AmFilterMode mode) 
    throws AgentException
    {
        return getAmFilterManager().getAmFilter(mode);
    }
    
    /**
     * Returns the AmSSOCache instance.
     * @deprecated
     * @return the AmSSOCache instance.
     */
    public static AmSSOCache getAmSSOCacheInstance() {
        return _amSSOCache;
    }
    
    //------- static housekeeping methods and variables -----//
    
    private static AmFilterManager getAmFilterManager() {
        return _manager;
    }
    
    private static void notifyConfigurationChange() {

        if(isLogMessageEnabled()) {
            logMessage(SUBSYSTEM_NAME
                       + ": Received configuration notification");
            logMessage(SUBSYSTEM_NAME + ": Swapping manager instance");
        }

        try {
            AmFilterManager manager = new AmFilterManager();

            _manager = manager;
        } catch(Exception ex) {
            logError(SUBSYSTEM_NAME + ": Unable to swap manager instance",
                     ex);
            logError(SUBSYSTEM_NAME + ": Dynamic property load failed");
        }
    }
    
    static class AmFilterConfigurationListener
                    implements IConfigurationListener 
    {

        public void configurationChanged() {
            AmFilterManager.notifyConfigurationChange();
        }

        public String getName() {
            return SUBSYSTEM_NAME;
        }
    }
    
    private static void logError(String msg) {
        getAmFilterModule().logError(msg);
    }
    
    private static void logError(String msg, Throwable th) {
        getAmFilterModule().logError(msg, th);
    }
    
    private static void logMessage(String msg) {
        getAmFilterModule().logMessage(msg);
    }
    
    private static boolean isLogMessageEnabled() {
        return getAmFilterModule().isLogMessageEnabled();
    }
    
    private static Module getAmFilterModule() {
        return AmFilterModule.getModule();
    }
    
    private static AmFilterManager _manager = null;
    private static final String SUBSYSTEM_NAME = "AmFilter";
    private static AmSSOCache _amSSOCache = new AmSSOCache();
    
    static {
        try {
            _manager = new AmFilterManager();
        } catch(AgentException ex) {
            AmFilterModule.getModule().logError(
                "AmFilterManager initialization failed", ex);

            throw new RuntimeException(
                "Exception caught in AmFilterManager initializer: "
                + ex.getMessage());
        }

        AgentConfiguration.addConfigurationListener(
            new AmFilterConfigurationListener());
    }
}
