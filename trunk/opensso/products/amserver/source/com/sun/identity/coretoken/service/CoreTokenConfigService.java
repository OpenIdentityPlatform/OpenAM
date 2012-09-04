/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * If applicable, addReferral the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: CoreTokenConfigService.java,v 1.1 2009/11/19 00:07:40 qcheng Exp $
 */
package com.sun.identity.coretoken.service;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.coretoken.CoreTokenConstants;
import com.sun.identity.coretoken.CoreTokenUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.ServiceListener;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import java.security.AccessController;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This <code>OpenSSOCoreTokenStore</code> implements the core token store
 * using SM store.
 */

public class CoreTokenConfigService implements ServiceListener {
    private static final String IMPL_CLASS_ATTR = "tokenStoreImplClass";
    static final String SEARCHABLE_ATTR = "searchableAttributes";
    static final String CLEANUP_INTERVAL = "tokenCleanupInterval";
    static final String TYPES_WITHOUT_ETAG_ENFORCE =
        "tokenTypesWithoutEtagEnforcement";
    // default cleanup interval in mini-seconds
    static final long DEFAULT_CLEANUP_INTERVAL = 180000;
    
    public static String implClassName =  null;
    // searchable attributes set
    public static Set<String> searchableAttrs = new HashSet<String>();
    public static long cleanupInt = 180000;
    public static Set<String> noETagEnfTypes = new HashSet<String>();

    // TODO : implement SMS listener for changes

    static {
        try {
            CoreTokenConfigService service = new CoreTokenConfigService();
            service.initServiceConfig();
            SSOToken adminToken = (SSOToken) AccessController
                .doPrivileged(AdminTokenAction.getInstance());
            ServiceConfigManager serviceConfigManager =
                new ServiceConfigManager(adminToken,
                CoreTokenConstants.CORE_TOKEN_CONFIG_SERVICE_NAME, "1.0");
            String notificationId = serviceConfigManager.addListener(service);
            if (CoreTokenUtils.debug.messageEnabled()) {
                CoreTokenUtils.debug.message("CoreTokenConfigService.static "
                    + " add service notification " + notificationId);
            }
        } catch (SMSException ex) {
            CoreTokenUtils.debug.error("CoreTokenConfigService.static", ex);
        } catch (SSOException ex) {
            CoreTokenUtils.debug.error("CoreTokenConfigService.static", ex);
        }
    }
    
    public void CoreTokenConfigService() {
    }

    private synchronized void initServiceConfig() {
        try {
            SSOToken dsameUserToken = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
            ServiceConfigManager mgr = new ServiceConfigManager(
                CoreTokenConstants.CORE_TOKEN_CONFIG_SERVICE_NAME,
                dsameUserToken);
            ServiceConfig globalConf = mgr.getGlobalConfig(null);
            if (globalConf != null) {
                Map<String, Set<String>> map = globalConf.getAttributes();
                if (map != null) {
                    Set<String> set = map.get(IMPL_CLASS_ATTR);
                    if ((set != null) && !set.isEmpty()) {
                        implClassName = set.iterator().next();
                    }
                    set = map.get(SEARCHABLE_ATTR);
                    Set<String> tmpSet = new HashSet<String>();
                    if ((set != null) && !set.isEmpty()) {
                        Iterator<String> it = set.iterator();
                        while (it.hasNext()) {
                            tmpSet.add(it.next().toLowerCase());
                        }
                    }
                    searchableAttrs = tmpSet;
                    set = map.get(CLEANUP_INTERVAL);
                    if ((set != null) && !set.isEmpty()) {
                        String tmp = set.iterator().next();
                        try {
                            cleanupInt = Integer.parseInt(tmp) * 1000;
                        } catch (NumberFormatException ne) {
                            CoreTokenUtils.debug.error("CoreTokenConfigService"
                                + ".init. invalid interval : " + tmp, ne);
                            cleanupInt = DEFAULT_CLEANUP_INTERVAL;
                        }
                    }
                    set = map.get(TYPES_WITHOUT_ETAG_ENFORCE);
                    tmpSet = new HashSet<String>();
                    if ((set != null) && !set.isEmpty()) {
                        Iterator<String> it = set.iterator();
                        while (it.hasNext()) {
                            tmpSet.add(it.next().toLowerCase());
                        }
                    }
                    noETagEnfTypes = tmpSet;
                }
            }
            if (CoreTokenUtils.debug.messageEnabled()) {
                CoreTokenUtils.debug.message("CoreTokenConfigServcie.init: " +
                    "searchable Attrs=" + searchableAttrs +
                    "; token store impl class=" + implClassName +
                    "; cleanup interval=" + cleanupInt +
                    "; token types without ETag enforcement=" + noETagEnfTypes);
            }
        } catch (SMSException ex) {
            CoreTokenUtils.debug.error("CoreTokenConfigService.init", ex);
        } catch (SSOException ex) {
            CoreTokenUtils.debug.error("CoreTokenConfigService.init", ex);
        }
    }

    public void schemaChanged(String serviceName, String version) {
        if (CoreTokenUtils.debug.messageEnabled()) {
            CoreTokenUtils.debug.message("CoreTokenConfigService.schema"
                + "Change. serviceName=" + serviceName + ", version=");
        }
        // ignore
    }

    public void globalConfigChanged(String serviceName, String version,
        String groupName, String serviceComponent, int type) {
        if (CoreTokenUtils.debug.messageEnabled()) {
            CoreTokenUtils.debug.message("CoreTokenConfigService.globalConfig"
                + "Change. serviceName=" + serviceName + ", version="
                + version + ", groupName=" + groupName + ", serviceComponent="
                + serviceComponent + ", type=" + type);
        }
        initServiceConfig();
    }

    public void organizationConfigChanged(String serviceName, String version,
        String orgName, String groupName, String serviceComponent, int type) {
        if (CoreTokenUtils.debug.messageEnabled()) {
            CoreTokenUtils.debug.message("CoreTokenConfigService.orgConfig"
                + "Change. serviceName=" + serviceName + ", version="
                + version + ", groupName=" + groupName + ", serviceComponent="
                + serviceComponent + ", type=" + type);
        }
        // ignore
    }
}
