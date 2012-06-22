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
 * $Id: ServerConfigBase.java,v 1.1 2008/11/22 02:41:22 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.plugin.services.server;

import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOToken;
import com.sun.identity.diagnostic.plugin.services.common.ServiceBase;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.ServiceManager;

/**
 * This is the base class for <code>Server</code> service.
 * Any class that needs server specific methods can use this base 
 * class.
 */
public abstract class ServerConfigBase extends ServiceBase implements 
    ConfigConstants, IServerValidate {

    /*
     * Check if the server entry is listed in the organization
     * alias list
     *
     * @param ssoToken of the admin user
     * @param instanceName name of the server instance
     * @return <code>true</code> if server instance exists in org alias 
     */
    protected boolean existsInOrganizationAlias(
        SSOToken ssoToken,
        String instanceName
    ) throws SMSException {
        OrganizationConfigManager ocm =
            new OrganizationConfigManager(ssoToken, "/");
        Map attrMap = ocm.getAttributes(ServiceManager.REALM_SERVICE);
        Set values = (Set)attrMap.get(OrganizationConfigManager.SUNORG_ALIAS);
        return  values.contains(instanceName);
    }
}
