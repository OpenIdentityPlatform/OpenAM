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
 * $Id: CMListener.java,v 1.4 2008/06/25 05:41:44 qcheng Exp $
 *
 */

package com.iplanet.ums;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceListener;

/**
 * CMListener implements ServiceListener interface. The listener is registered
 * for event notification via
 * 
 * <pre>
 * _smsapi.addListener()
 * </pre>
 * 
 * CMListener is added by ConfigManagerUMS (a single listener at the root of the
 * directory tree.) Event notification callsback the method
 * 
 * <pre>
 * configChanged
 * </pre>. configChanged deletes the modified subtree from the ConfigManager
 * cache.
 */

public class CMListener implements ServiceListener {

    private static Debug _debug = Debug.getInstance(IUMSConstants.UMS_DEBUG);

    public CMListener() {
    }

    public void schemaChanged(String serviceName, String version) {
        // do nothing
    }

    /**
     * This method is called back by SM when a service's global configuration
     * data has been changed. The parameter <code>groupName</code> denote
     * the name of the configuration grouping (e.g. default) and
     * <code>componentName</code> denotes the service's sub-component
     * that changed.
     *
     * @param serviceName name of the service.
     * @param version version of the service.
     * @param groupName name of the configuration grouping.
     * @param componentName name of the service component that
     *        changed.
     * @param type change type, i.e. <code>ADDED</code>, <code>REMOVED</code> or
     *         <code>MODIFIED</code>.
     */ 
    public void globalConfigChanged(
        String serviceName,
        String version,
        String groupName,
        String componentName,
        int type
    ) {
        organizationConfigChanged(serviceName, version, null, groupName,
                componentName, type);
    }

    /**
     * This method is called back by SM when a service's organization
     * configuration data has been changed. The parameters <code>org</code>,
     * <code>groupname</code> and <code>componentName</code>
     * denotes the organization name, configuration grouping name and
     * service's sub-component that are changed respectively.
     *
     * @param service name of the service.
     * @param version version of the service.
     * @param org organization name as DN.
     * @param groupname name of the configuration grouping.
     * @param componentName the name of the service components that
     *        changed.
     * @param type change type, i.e. <code>ADDED</code>, <code>REMOVED</code> or
     *        <code>MODIFIED</code>.
     */ 
    public void organizationConfigChanged(
        String service,
        String version,
        String org,
        String groupname,
        String componentName,
        int type
    ) {
        try {
            ConfigManagerUMS cm = ConfigManagerUMS.getConfigManager();
            if (org == null)
                org = "";
            if (service == null) {
                _debug.error("CMListener-> serviceName is null!");
                return;
            }
            if (_debug.messageEnabled())
                _debug.message("CMListener-> Service modified: " + service
                        + " for Org:" + org);

            if (service.equals(ConfigManagerUMS.UMS_SRVC)) {
                synchronized (ConfigManagerUMS._cch) {
                    cm.deleteOrgFromCache(org);
                    // cm.updateCache(org); Update cache for this org only
                    // when it is needed.
                }
            }
        } catch (Exception e) {
            _debug.error("CMListener-> Caught exception: ", e);
        }
    }

}
