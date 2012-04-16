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
 * $Id: IdRemoteServicesProviderImpl.java,v 1.3 2008/06/25 05:43:31 qcheng Exp $
 *
 */

package com.sun.identity.idm.remote;

import com.sun.identity.shared.debug.Debug;
import com.iplanet.am.util.SystemProperties;
import com.sun.identity.idm.IdServicesProvider;
import com.sun.identity.idm.IdServices;

/*
 * A factory class which determines the correct implementation of
 * IdRepoDataStoreServices and returns that instance.
 * 
 */
public class IdRemoteServicesProviderImpl implements IdServicesProvider {
    
    private static final String GLOBAL_CACHE_ENABLED_DISABLED_KEY = 
        "com.iplanet.am.sdk.caching.enabled";

    private static final String IDM_CACHE_ENABLED_DISABLED_KEY = 
        "com.sun.identity.idm.cache.enabled";

    private static Debug debug = IdRemoteServicesImpl.getDebug();

    private static boolean cachingEnabled;

    private static IdServices idServices = null;

    static {
        // Check if the global caching property is set in System runtime.
        String cachingMode = System
                .getProperty(GLOBAL_CACHE_ENABLED_DISABLED_KEY);
        if ((cachingMode == null) || (cachingMode.length() == 0)) {
            // Check if caching property is set in AMConfig
            cachingMode = SystemProperties.get(
                    GLOBAL_CACHE_ENABLED_DISABLED_KEY, "true");
        }

        if (cachingMode.equalsIgnoreCase("true")) { // Global Caching property
            // set to true. Hence, enable caching.
            cachingEnabled = true;
        } else { // Global Caching mode disabled. So, check individual
            // component level property for AM SDK.
            cachingMode = SystemProperties.get(IDM_CACHE_ENABLED_DISABLED_KEY);
            if (cachingMode != null && cachingMode.length() > 0) {
                cachingEnabled = (cachingMode.equalsIgnoreCase("true")) ? true
                        : false;
            } else {
                cachingEnabled = false;
            }
        }

        if (cachingEnabled) {
            idServices = IdRemoteCachedServicesImpl.getInstance();
            if (debug.messageEnabled()) {
                debug.message("IdRemoteServicesProviderImpl.static{} - "
                        + "Caching Mode: " + cachingEnabled + "Using "
                        + "implementation Class IdRemoteCachedServicesImpl");
            }
        } else {
            idServices = IdRemoteServicesImpl.getInstance();
            if (debug.messageEnabled()) {
                debug.message("IdRemoteServicesProviderImpl.static{} - "
                        + "Caching Mode: " + cachingEnabled + "Using "
                        + "implementation Class IdRemoteServicesImpl");
            }
        }
    }

    public static boolean isCachingEnabled() {
        return cachingEnabled;
    }

    public IdServices getProvider() {
        return idServices;
    }
}
