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
 * $Id: ServiceListener.java,v 1.5 2009/01/28 05:35:03 ww203982 Exp $
 *
 */

package com.sun.identity.sm;

import com.sun.identity.shared.ldap.controls.LDAPPersistSearchControl;

/**
 * The interface <code>ServiceListener</code> needs to be implemented by
 * applications in order to receive service data change notifications. The
 * method <code>schemaChanged()</code> is invoked when a service schema data
 * has been changed. The method <code>globalConfigChanged()</code> and
 * <code>organizationConfigChanged()</code> are invoked when the service
 * configuration data has been changed.
 *
 * @supported.all.api
 */
public interface ServiceListener {

    /**
     * The change type specifies that the entry has been added.
     */
    public static final int ADDED = LDAPPersistSearchControl.ADD;

    /**
     * The change type specifies that the entry has been removed.
     */
    public static final int REMOVED = LDAPPersistSearchControl.DELETE;

    /**
     * The change type specifies that the entry has been modified.
     */
    public static final int MODIFIED = LDAPPersistSearchControl.MODIFY;

    /**
     * This method will be invoked when a service's schema has been changed.
     * 
     * @param serviceName
     *            name of the service
     * @param version
     *            version of the service
     */
    public void schemaChanged(String serviceName, String version);

    /**
     * This method will be invoked when a service's global configuration data
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
    public void globalConfigChanged(String serviceName, String version,
            String groupName, String serviceComponent, int type);

    /**
     * This method will be invoked when a service's organization configuration
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
    public void organizationConfigChanged(String serviceName, String version,
            String orgName, String groupName, String serviceComponent, 
            int type);
}
