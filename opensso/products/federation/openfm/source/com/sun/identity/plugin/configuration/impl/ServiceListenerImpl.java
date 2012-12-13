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
 * $Id: ServiceListenerImpl.java,v 1.2 2008/06/25 05:49:57 qcheng Exp $
 *
 */

package com.sun.identity.plugin.configuration.impl;

import com.sun.identity.shared.debug.Debug;

import com.sun.identity.plugin.configuration.ConfigurationActionEvent;
import com.sun.identity.plugin.configuration.ConfigurationListener;
import com.sun.identity.sm.ServiceListener;

/**
 * The <code>ServiceListenerImpl</code> receives service data change
 * notifications.
 */

class ServiceListenerImpl implements ServiceListener {

    private ConfigurationListener configListener;
    private String componentName;
    private static Debug debug = ConfigurationInstanceImpl.debug;

    /**
     * This constructor takes a <code>ConfigurationListener</code>.
     * @param configListener a <code>ConfigurationListener</code>
     */
    ServiceListenerImpl(ConfigurationListener configListener,
        String componentName) {
        this.configListener = configListener;
        this.componentName = componentName;
    }

    /**
     * This methid will be invoked when a service's schema has been changed.
     *
     * @param seviceName name of the service
     * @param version version of the service
     */
    public void schemaChanged(String serviceName, String version) {

        if (debug.messageEnabled()) {
            debug.message("ServiceListenerImpl.schemaChanged: service = " +
                serviceName);
        }
        ConfigurationActionEvent caevent = new ConfigurationActionEventImpl(
            ConfigurationActionEvent.MODIFIED, null, componentName, null);
        configListener.configChanged(caevent);
    }

    /**
     * This method will be invoked when a service's global configuration
     * data has been changed. The parameter <code>groupName</code> denote
     * the name of the configuration grouping (e.g. default) and
     * <code>serviceComponent</code> denotes the service's sub-component
     * that changed (e.g. <code>/NamedPolicy</code>, <code>/Templates</code>).
     * 
     * @param serviceName name of the service.
     * @param version version of the service.
     * @param groupName name of the configuration grouping.
     * @param serviceComponent name of the service components that
     *     changed.
     * @param type change type, i.e., ADDED, REMOVED or MODIFIED.
     */
    public void globalConfigChanged(String serviceName, String version,
        String groupName, String serviceComponent, int type) {
    }

    /**
     * This method will be invoked when a service's organization
     * configuration data has been changed. The parameters <code>orgName</code>,
     * <code>groupName</code> and <code>serviceComponent</code> denotes the
     * organization name, configuration grouping name and
     * service's sub-component that are changed respectively.
     * 
     * @param serviceName name of the service
     * @param version version of the service
     * @param orgName organization name as DN
     * @param groupName name of the configuration grouping
     * @param serviceComponent the name of the service components that
     *     changed
     * @param type change type, i.e., ADDED, REMOVED or MODIFIED
     */
    public void organizationConfigChanged(String serviceName, String version,
        String orgName, String groupName, String serviceComponent, int type) {

        if (debug.messageEnabled()) {
            debug.message("ServiceListenerImpl.organizationConfigChanged: " +
                "service = " + serviceName);
        }
        ConfigurationActionEvent caevent = new ConfigurationActionEventImpl(
            typeMapping(type), serviceComponent, componentName, orgName);
        configListener.configChanged(caevent);
    }

    /**
     * Converts sms event type to configuration action event type.
     * @param smsType sms event type.
     * @return configuration event type.
     */ 
    private static int typeMapping(int smsType) {

        switch (smsType) {
            case ServiceListener.ADDED:
                return ConfigurationActionEvent.ADDED;
            case ServiceListener.REMOVED:
                return ConfigurationActionEvent.DELETED;
            case ServiceListener.MODIFIED:
                return ConfigurationActionEvent.MODIFIED;
        }

        return smsType;
    }
}
