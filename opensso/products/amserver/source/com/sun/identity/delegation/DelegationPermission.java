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
 * $Id: DelegationPermission.java,v 1.5 2008/06/25 05:43:24 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.delegation;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;
import java.util.Map;
import java.util.Set;

/**
 * The <code>DelegationPermission</code> class represents an access control
 * permission on a resource in a realm. It consists of a realm name, a service
 * name, a version number, a config type, a subconfig name, and a set of actions
 * that a user with the permission can perform. Additionally it also contains a
 * <code>Map</code> that can be used for future extensions.
 */

public class DelegationPermission {

    String orgName;

    private String serviceName;

    private String serviceVersion;

    private String configType;

    private String subConfigName;

    private Set actions;

    private Map extensions;

    static final Debug debug = DelegationManager.debug;

    static String WILDCARD = "*";

    /**
     * Default constructor for <code>DelegationPermission</code>.
     */

    public DelegationPermission() {
    }

    /**
     * Constructor for <code>DelegationPermission</code>. Constructs a
     * delegation permission object with a realm name, a service name, a service
     * version number, a config type, a subconfig name, a set of actions, and a
     * <code>Map</code> for extensions.
     * 
     * @param orgName The realm name in the delegation permission
     *
     * @param serviceName The service name in the delegation permission
     *
     * @param version The service version in the delegation permission
     *
     * @param configType The config type in the delegation permission
     *
     * @param subConfigName The subconfig name in the delegation permission
     *
     * @param actions The action names in the delegation permission
     *
     * @param extensions a placeholder for future extensions
     *
     * @throws DelegationException if unable to create the <code>
     *         DelegationPermission</code> object.
     */
    public DelegationPermission(
            String orgName,
            String serviceName,
            String version,
            String configType,
            String subConfigName,
            Set actions,
            Map extensions
    ) throws DelegationException {
        setOrganizationName(orgName);
        this.serviceName = serviceName;
        this.serviceVersion = version;
        this.configType = configType;
        this.subConfigName = subConfigName;
        this.actions = actions;
        this.extensions = extensions;
    }

    /**
     * Returns the realm name in the permission
     * 
     * @return <code>String</code> representing the realm name in the permission
     */

    public String getOrganizationName() {
        return orgName;
    }

    /**
     * Sets the realm name in the permission
     * 
     * @param name  <code>String</code> representing the realm name in the 
              <code>DelegationPermission</code>.
     * @throws DelegationException if name is invalid
     */

    public void setOrganizationName(String name) throws DelegationException {
        if (name != null) {
            int index = name.indexOf(WILDCARD);
            if (index != -1) {
                orgName = name;
                return;
            }
        }
        orgName = DNMapper.orgNameToDN(name);
    }

    /**
     * Returns the service name in the permission
     * 
     * @return <code>String</code> representing the service name in the 
               <code>DelegationPermission</code>
     */

    public String getServiceName() {
        return serviceName;
    }

    /**
     * Sets the service name in the permission
     * 
     * @param name  The service name in the delegation permission
     * @throws DelegationException if name is invalid
     */

    public void setServiceName(String name) throws DelegationException {
        serviceName = name;
    }

    /**
     * Returns the service version in the permission
     * 
     * @return the service version in the permission
     */

    public String getVersion() {
        return serviceVersion;
    }

    /**
     * Sets the service version in the permission
     * 
     * @param version The service version in the delegation permission
     * @throws DelegationException if version is invalid
     */

    public void setVersion(String version) throws DelegationException {
        serviceVersion = version;
    }

    /**
     * Returns the config type in the permission
     * 
     * @return the config type in the permission
     */

    public String getConfigType() {
        return configType;
    }

    /**
     * Sets the config type in the permission
     * 
     * @param configType  The config type in the delegation permission
     * @throws DelegationException if config type is invalid
     */

    public void setConfigType(String configType) throws DelegationException {
        this.configType = configType;
    }

    /**
     * Returns the subconfig name in the permission
     * 
     * @return the subconfig name in the permission
     */

    public String getSubConfigName() {
        return subConfigName;
    }

    /**
     * Sets the subconfig name in the permission
     * 
     * @param name The subconfig name in the delegation permission
     * @throws DelegationException if subconfig name is invalid
     */

    public void setSubConfigName(String name) throws DelegationException {
        subConfigName = name;
    }

    /**
     * Returns the action names in the permission
     * 
     * @return the action names in the permission
     */

    public Set getActions() {
        return actions;
    }

    /**
     * Sets the action names in the permission
     * 
     * @param actions  The action names in the delegation permission
     * @throws DelegationException if an action name is invalid
     */

    public void setActions(Set actions) throws DelegationException {
        this.actions = actions;
    }

    /**
     * Returns the extensions in the permission
     * 
     * @return the extensions in the permission
     */

    public Map getExtensions() {
        return extensions;
    }

    /**
     * Sets the extensions in the permission
     * 
     * @param extensions   The extensions in the delegation permission
     * @throws DelegationException if some info in extensions is invalid
     */

    public void setExtensions(Map extensions) throws DelegationException {
        this.extensions = extensions;
    }

     /**
      * Returns the <code>String</code> representation of this
      * object.
      *
      * @return the <code>String</code> representation of the
      * <code>DelegationPermission</code> object.
      */
    public String toString() {
        StringBuilder sb = new StringBuilder(200);
        sb.append("\nDelegationPermission Object:");
        sb.append("\norgName=");
        sb.append(orgName);
        sb.append("\nserviceName=");
        sb.append(serviceName);
        sb.append("\nserviceVersion=");
        sb.append(serviceVersion);
        sb.append("\nconfigType=");
        sb.append(configType);
        sb.append("\nsubConfigName=");
        sb.append(subConfigName);
        sb.append("\nactions=");
        sb.append(actions);
        sb.append("\nextensions=");
        sb.append(extensions);
        return sb.toString();
    }
}
