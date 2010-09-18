/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AuthConfigMonitor.java,v 1.3 2008/06/25 05:42:04 qcheng Exp $
 *
 */


package com.sun.identity.authentication.service;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.sm.ServiceListener;
import com.sun.identity.sm.ServiceSchemaManager;

/**
 * This class is converted from <code>AuthenticatorManager</code>
 * to monitor the authentication related configuration changes.
 */
public class AuthConfigMonitor implements ServiceListener {
    
    ServiceSchemaManager schemaManager = null;
    static Debug debug;         

    protected AuthConfigMonitor(ServiceSchemaManager schemaManager) {
        this.schemaManager = schemaManager;
        String schemaName = schemaManager.getName();
        debug = Debug.getInstance("amAuth");
        addServiceListener(schemaName);
    }

    /**
     * This method gets invoked when a service's schema has been changed.
     * 
     * @param serviceName
     *            name of the service
     * @param version
     *            version of the service
     */
    public void schemaChanged(String serviceName, String version) {
        if (debug.messageEnabled()) {
            debug.message("Global config changed " + serviceName);
        }
        try {
            AuthD authd = AuthD.getAuth();
            if (serviceName.equals(ISAuthConstants.AUTH_SERVICE_NAME)) {
                authd.updateAuthServiceGlobals(schemaManager);
            } else if (serviceName.equals(
                ISAuthConstants.AUTHCONFIG_SERVICE_NAME)) {
                authd.updateAuthConfigGlobals(schemaManager);
            } else if (serviceName.equals(
                ISAuthConstants.PLATFORM_SERVICE_NAME)) {
                authd.updatePlatformServiceGlobals(schemaManager);
            } else if (serviceName.equals(
                ISAuthConstants.SESSION_SERVICE_NAME)) {
                authd.updateSessionServiceDynamics(schemaManager);
            }
        } catch (Exception e) {
            debug.error("Error schemaChanged : " + e.getMessage());
            if (debug.messageEnabled()) {
                debug.message("Stack trace: ", e);
            }
        }
    }

    /**
     * This method gets invoked when a service's global configuration data
     * has been changed. The parameter <code>groupName</code> denote the name
     * of the configuration grouping (e.g. default) and
     * <code>serviceComponent</code> denotes the service's sub-component that
     * changed (e.g. <code>/NamedPolicy</code>, <code>/Templates</code>).
     * 
     * @param serviceName
     *            name of the service.
     * @param version
     *            version of the service.
     * @param groupName
     *            name of the configuration grouping.
     * @param serviceComponent
     *            name of the service components that changed.
     * @param type
     *            change type, i.e., ADDED, REMOVED or MODIFIED.
     */
    public void globalConfigChanged(
        String serviceName,
        String version,
        String groupName, 
        String serviceComponent,
        int type) {
        // nothing to do
    }

    /**
     * This method gets invoked when a service's organization configuration
     * data has been changed. The parameters <code>orgName</code>,
     * <code>groupName</code> and <code>serviceComponent</code> denotes the
     * organization name, configuration grouping name and service's
     * sub-component that are changed respectively.
     * 
     * @param serviceName
     *            name of the service
     * @param version
     *            version of the service
     * @param orgName
     *            organization name as DN
     * @param groupName
     *            name of the configuration grouping
     * @param serviceComponent
     *            the name of the service components that changed
     * @param type
     *            change type, i.e., ADDED, REMOVED or MODIFIED
     */
    public void organizationConfigChanged(
        String serviceName, 
        String version,
        String orgName, 
        String groupName, 
        String serviceComponent, 
        int type) { 
        // nothing to do
    }


    /**
     *  Adds the listener - note for global config
     *  changes need to use schema manager 
     *  @param service name of service listener is added for
     */
    void addServiceListener(String service) {
        debug.message("Adding service listener");
        try {
            // add SM ServiceListener
            schemaManager.addListener(this);
        } catch (Exception ee) {
            debug.error("addServiceListener: " + ee.getMessage());
            if (debug.messageEnabled()) {
                debug.message("Stack trace:", ee);
            }
        }
    }
}
