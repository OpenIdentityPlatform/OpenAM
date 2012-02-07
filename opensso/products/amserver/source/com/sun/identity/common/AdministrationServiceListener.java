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
 * $Id: AdministrationServiceListener.java,v 1.1 2008/08/13 15:54:42 pawand Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.common;

import java.security.AccessController;
import java.util.Map;
import java.util.HashMap;

import com.iplanet.am.sdk.AMConstants;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import com.sun.identity.sm.ServiceNotFoundException;
import com.sun.identity.sm.SMSException;

/**
 * This class is a cache and Listener for Administration Service
 * 
 */
public class AdministrationServiceListener implements AMConstants, ServiceListener{
    static Debug debug = Debug.getInstance("amProfile_ldap");

    private static String globalInvalidChars = null;
    private static String globalPluginName = null;
    private static Map invalidCharCache = new HashMap();
    private static Map pluginNameCache = new HashMap();
    private static AdministrationServiceListener serviceListener = new 
        AdministrationServiceListener();
    static {
        SSOToken ssot = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        try {
            ServiceConfigManager scm = new ServiceConfigManager(
                ADMINISTRATION_SERVICE, ssot);
            scm.addListener(serviceListener);
            debug.message("AdministrationServiceListener: Listener added.");
        } catch (ServiceNotFoundException exp) {
            debug.error("AdministrationServiceListener: Can not register"
                + " Listener", exp);
        } catch (SMSException smsExp) {
            debug.error("AdministrationServiceListener: Can not register"
                + " Listener", smsExp);
        } catch (SSOException ssoExp) {
            debug.error("AdministrationServiceListener: Can not register"
                + " Listener", ssoExp);
        }
    }
     
    /**
     * Constructs an instance of <code>AdministrationServiceListener</code> 
     */   
    private AdministrationServiceListener() {
    }

    /**
     * Returns invalid characters from cache.
     *
     * @param orgDN
     *		  organization/realm name.
     * @return Invalid characters for username and password.
     */   
    public static String getOrgInvalidCharsFromCache(String orgDN) {
        String retVal = (String) invalidCharCache.get(orgDN);
        if (debug.messageEnabled()) {
            debug.message("AdministrationServiceListener."+
                "getOrgInvalidCharsFromCache: Returning from cache = " 
                + retVal);
        }
        return retVal;
    }
    
    /**
     * Sets invalid characters in cache.
     *
     * @param orgDN
     *		  organization/realm name.
     * @param invalidChars
     *		  invalid characters for username and password.
     */   
    public static void setOrgInvalidCharsInCache(String orgDN, String 
        invalidChars) {
        invalidCharCache.put(orgDN, invalidChars);
    }

    /**
     * Returns global invalid characters from cache.
     *
     * @return global invalid characters for username and password.
     */   
    public static String getGlobalInvalidCharsFromCache() {
        return globalInvalidChars;
    }
    
    /**
     * Sets global Invalid characters in cache.
     *
     * @param invalidChars
     *		  global invalid characters for username and password.
     */   
    public static void setGlobalInvalidCharsInCache(String invalidChars) {
        globalInvalidChars = invalidChars;
    }

    /**
     * Returns user/password validation plugin name from cache.
     *
     * @param orgDN
     *		  organization/realm name.
     * @return  user/password validation plugin name from cache.
     */   
    public static String getOrgPluginNameFromCache(String orgDN) {
        String retVal = (String) pluginNameCache.get(orgDN);
        if (debug.messageEnabled()) {
            debug.message("AdministrationServiceListener."+
                "getOrgPluginNameFromCache: Returning from cache = " 
                + retVal);
        }
        return retVal;
    }
    
    /**
     * Sets  user/password validation plugin name in cache.
     *
     * @param orgDN
     *		  organization/realm name.
     * @param pluginName
     *		  user/password validation plugin name.
     */   
    public static void setOrgPluginNameInCache(String orgDN, String 
        pluginName) {
        pluginNameCache.put(orgDN, pluginName);
    }

    /**
     * Returns global user/password validation plugin name from cache.
     *
     * @return global user/password validation plugin name.
     */   
    public static String getGlobalPluginNameFromCache() {
        return globalPluginName;
    }
    
    /**
     * Sets  global user/password validation plugin name in cache.
     *
     * @param pluginName
     *		  global user/password validation plugin name.
     */   
    public static void setGlobalPluginNameInCache(String pluginName) {
        globalPluginName = pluginName;
    }

    /**
     * This method will be invoked when a service's global configuration data
     * has been changed.
     *
     * @param serviceName
     *		  name of the service.
     * @param version
     *		  version of the service.
     * @param groupName
     *		  name of the configuration grouping.
     * @param serviceComponent
     *		  name of the service components that changed.
     * @param type
     *		  change type, i.e., ADDED, REMOVED or MODIFIED.
     */   
    public void globalConfigChanged(String serviceName, String version, String
        groupName, String serviceComponent, int type) {
        if (debug.messageEnabled()) {
            debug.message("AdministrationServiceListener."
            + "globalConfigChanged : Global Config changed for "
            + "Service = "+serviceName+ " Change type = "+ type);
        }
        globalInvalidChars = null;
        globalPluginName = null;
    }

    /**
     * This method will be invoked when a service's schema has been changed. 
     * It is a no-op for this implementation.
     *
     * @param serviceName
     *		  name of the service
     * @param version
     *		  version of the service
     */
    public void schemaChanged(String serviceName, String version)  {
    }
    
    /**
     * This method will be invoked when a service's organization 
     * configuration data has been changed. It removes the invalid charecters
     * cache if service configuration is modified or removed. 
     *
     * @param serviceName
     *		  name of the service.
     * @param version
     *		  version of the service.
     * @param goupName
     *		  name of the configuration grouping.
     * @param serviceComponent
     *		  name of the service components that changed.
     * @param type
     *		  change type, i.e., ADDED, REMOVED or MODIFIED.
     */
    public void organizationConfigChanged(String serviceName, String version, 
        String orgName,String  goupName, String serviceComponent, int type) {
        if (debug.messageEnabled()) {
            debug.message("AdministrationServiceListener."
            + "organizationConfigChanged : Config changed for Org="
            + orgName + "Service = "+serviceName+ " Change type = "+ type);
        }
        if (type != ADDED) {
            invalidCharCache.remove(orgName);
            pluginNameCache.remove(orgName);
        }
    }
}
