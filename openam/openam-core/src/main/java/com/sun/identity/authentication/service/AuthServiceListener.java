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
 * $Id: AuthServiceListener.java,v 1.4 2008/11/10 22:56:55 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.authentication.service;

import java.security.AccessController;
import java.util.HashMap;
import java.util.Map;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import com.sun.identity.sm.ServiceNotFoundException;
import com.sun.identity.sm.SMSException;

/**
 * This class is a cache and Listener for Authentication 
 * Configuration Service.
 */
public class AuthServiceListener implements ServiceListener{
    static Debug debug = Debug.getInstance("amAuth");

    protected static final String AUTHCONFIG_SERVICE = 
        "iPlanetAMAuthConfiguration";
    private static Map serviceAttributeCache = new HashMap();
    private static AuthServiceListener serviceListener = new 
        AuthServiceListener();
    static {
        SSOToken ssot = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        try {
            ServiceConfigManager scm = new ServiceConfigManager(
                AUTHCONFIG_SERVICE, ssot);
            scm.addListener(serviceListener);
            debug.message("AuthServiceListener: Listener added.");
        } catch (ServiceNotFoundException exp) {
            debug.error("AuthServiceListener: Can not register"
                + " Listener", exp);
        } catch (SMSException smsExp) {
            debug.error("AuthServiceListener: Can not register"
                + " Listener", smsExp);
        } catch (SSOException ssoExp) {
            debug.error("AuthServiceListener: Can not register"
                + " Listener", ssoExp);
        }
    }
    
    /**
     * Constructs an instance of <code>AuthServiceListener</code>
     */    
    private AuthServiceListener() {
    }
    
    /**
     * Returns Auth Config Service attributes from cache.
     *
     * @param orgDN
     *            organization/realm name.
     * @param serviceName
     *            auth configuration service name.
     * @return service attributes.
     */
    public static Map getServiceAttributeCache(String orgDN, String 
        serviceName) {
        Map retVal = (Map) serviceAttributeCache.get(serviceName+","+orgDN);
        if (debug.messageEnabled()) {
     	    debug.message("AuthServiceListener." +
                "getServiceAttributeCache(): Returning from cache = " + retVal
                + ", orgDN=" + orgDN);
        }
        return retVal;
    }
    
    /**
     * Sets Auth Config Service attributes in cache.
     *
     * @param orgDN
     *            organization/realm name.
     * @param serviceName
     *            auth configuration service name.
     * @param serviceAttributes
     *            auth service attributes.
     */
    public static void setServiceAttributeCache(String orgDN, String 
        serviceName, Map serviceAttributes) {
        serviceAttributeCache.put(serviceName+","+orgDN, serviceAttributes);
        if (debug.messageEnabled()) {
     	    debug.message("AuthServiceListener." +
                "setServiceAttributeCache(): Cache after add= " 
                + serviceAttributeCache + ", orgDN=" + orgDN);
        }
    }

    /**
     * This method will be invoked when a service's global configuration data
     * has been changed. It is a no-op for this implementation. 
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
     * @param orgName
     *            Name of the organization.
     * @param goupName
     *		  name of the configuration grouping.
     * @param serviceComponent
     *		  name of the service components that changed.
     * @param type
     *		  change type, i.e., ADDED, REMOVED or MODIFIED.
     */
    public void organizationConfigChanged(String serviceName, String version, 
        String orgName, String goupName, String serviceComponent, int type) {
        if (debug.messageEnabled()) {
            debug.message("AuthServiceListener."
            + "organizationConfigChanged : Config changed for Org="
            + orgName + "Service = "+serviceName+ " Change type = "+ type);
        }
        if (type != ADDED) {
            serviceAttributeCache.remove(serviceName + "," + orgName);
        }
    }
}
