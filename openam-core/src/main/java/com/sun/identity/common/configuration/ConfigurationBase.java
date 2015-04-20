/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ConfigurationBase.java,v 1.4 2009/07/07 06:14:12 veiming Exp $
 *
 * Portions Copyrighted 2010-2015 ForgeRock AS.
 */

package com.sun.identity.common.configuration;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.security.AccessController;
import java.util.LinkedHashSet;

/**
 * This is the base case for <code>ServerConfiguration</code> and
 * <code>SiteConfiguration</code> classes.
 */
public abstract class ConfigurationBase {
    protected static final String OLD_ATTR_SITE_LIST = 
        "iplanet-am-platform-site-list";
    protected static final String OLD_ATTR_SERVER_LIST = 
        "iplanet-am-platform-server-list";

    public static final String CONFIG_SERVERS = "com-sun-identity-servers";
    public static final String SUBSCHEMA_SERVER = "server";
    protected static final String ATTR_SERVER_ID = "serverid";
    
    public static final String SUBSCHEMA_SITE = "site";
    public static final String CONFIG_SITES = "com-sun-identity-sites";
    protected static final String SUBCONFIG_ACCESS_URL = "accesspoint";
    protected static final String ATTR_PRIMARY_SITE_ID = "primary-siteid";
    protected static final String ATTR_PRIMARY_SITE_URL = "primary-url";
    protected static final String SUBCONFIG_SEC_URLS = "secondary-urls";
    protected static final String ATTR_SEC_ID = "secondary-siteid";

    protected static String getNextId(SSOToken ssoToken) 
        throws SMSException, SSOException {
        Set currentIds = new HashSet();
        
        currentIds.addAll(getServerConfigurationId(getRootServerConfig(ssoToken)));
        currentIds.addAll(getSiteConfigurationId(getRootSiteConfig(ssoToken)));

        return getNextId(currentIds);
    }
    
    protected static Set getServerConfigurationId(ServiceConfig svc) 
        throws SMSException, SSOException {
        Set currentIds = new HashSet();
        Set names = svc.getSubConfigNames("*");
        
        if ((names != null) && !names.isEmpty()) {
            for (Iterator i = names.iterator(); i.hasNext(); ) {
                String name = (String)i.next();
                ServiceConfig sc = svc.getSubConfig(name);
                Map map = sc.getAttributes();
                Set set = (Set)map.get(ATTR_SERVER_ID);
                if ((set != null) && !set.isEmpty()) {
                    currentIds.add(set.iterator().next());
                }
            }
        }
        return currentIds;
    }

    protected static Set getSiteConfigurationId(
        ServiceConfig svc
    ) throws SMSException, SSOException {
        Set currentIds = new HashSet();
        Set names = svc.getSubConfigNames("*");
        
        if ((names != null) && !names.isEmpty()) {
            for (Iterator i = names.iterator(); i.hasNext(); ) {
                String name = (String)i.next();
                currentIds.addAll(
                    getSiteConfigurationIds(null, svc, name, false));
            }
        }
        return currentIds;
    }

    protected static Set getSiteConfigurationIds(
        SSOToken ssoToken,
        ServiceConfig rootNode,
        String name,
        boolean bPrimaryOnly
    ) throws SMSException, SSOException {
        if (rootNode == null) {
            rootNode = getRootSiteConfig(ssoToken);
        }

        ServiceConfig sc = rootNode.getSubConfig(name);
        if (sc == null) {
            return Collections.EMPTY_SET;
        }

        Set currentIds = new LinkedHashSet();
        ServiceConfig accessPoint = sc.getSubConfig(SUBCONFIG_ACCESS_URL);

        Map map = accessPoint.getAttributes();
        Set set = (Set)map.get(ATTR_PRIMARY_SITE_ID);
        currentIds.add(set.iterator().next());
        
        if (!bPrimaryOnly) {
            Set failovers = accessPoint.getSubConfigNames("*");
            if ((failovers != null) && !failovers.isEmpty()) {
                for (Iterator i = failovers.iterator(); i.hasNext(); ) {
                    String foName = (String)i.next();
                    ServiceConfig s = accessPoint.getSubConfig(foName);
                    Map mapValues = s.getAttributes();
                    set = (Set)mapValues.get(ATTR_SEC_ID);
                    if ((set != null) && !set.isEmpty()) {
                        currentIds.add(set.iterator().next());
                    }
                }
            }
        }
        return currentIds;
    }

    protected static String getNextId(Set currentIds) {
        String id = null;
        if (!currentIds.isEmpty()) {
            for (int i = 1; (id == null); i++) {
                String test =  (i < 10) ? "0" + Integer.toString(i)
                : Integer.toString(i);
                if (!currentIds.contains(test)) {
                    id = test;
                }
            }
        }
        return (id == null) ? "01" : id;
    }

    protected static void updateOrganizationAlias(
        SSOToken ssoToken,
        String instanceName,
        boolean bAdd
    ) throws SMSException {
        String hostName = null;
        try {
            URL url = new URL(instanceName);
            hostName = url.getHost();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e.getMessage());
        }
        OrganizationConfigManager ocm = new OrganizationConfigManager(
            ssoToken, "/");
        Map allAttrs = ocm.getAttributes(ServiceManager.REALM_SERVICE);
        Set values = (Set)allAttrs.get(OrganizationConfigManager.SUNORG_ALIAS);
        if (bAdd) {
            if (!values.contains(hostName)) {
                values.add(hostName);
                ocm.setAttributes(ServiceManager.REALM_SERVICE, allAttrs);
            }
        } else {
            if (values.contains(hostName)) {
                values.remove(hostName);
                ocm.setAttributes(ServiceManager.REALM_SERVICE, allAttrs);
            }
        }
    }

    protected static ServiceConfig getRootServerConfig(SSOToken ssoToken)
        throws SMSException, SSOException {
        ServiceConfigManager scm = new ServiceConfigManager(
            Constants.SVC_NAME_PLATFORM, ssoToken);
        ServiceConfig globalSvcConfig = scm.getGlobalConfig(null);
        return (globalSvcConfig != null) ?
            globalSvcConfig.getSubConfig(CONFIG_SERVERS) : null;
    }
    
    protected static ServiceConfig getServerConfig(
        SSOToken ssoToken, 
        String name
    ) throws SMSException, SSOException {
        ServiceConfig sc = getRootServerConfig(ssoToken);
        return (sc != null) ? sc.getSubConfig(name) : null;
    }
    
    protected static ServiceConfig getRootSiteConfig(SSOToken ssoToken) 
        throws SMSException, SSOException {
        ServiceConfigManager scm = new ServiceConfigManager(
            Constants.SVC_NAME_PLATFORM, ssoToken);
        ServiceConfig globalSvcConfig = scm.getGlobalConfig(null);
        return (globalSvcConfig != null) ?
            globalSvcConfig.getSubConfig(CONFIG_SITES) : null;
    }
}
