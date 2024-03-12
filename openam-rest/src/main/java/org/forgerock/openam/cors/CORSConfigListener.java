/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2024 3A Systems LLC.
 */

package org.forgerock.openam.cors;

import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;

import java.security.AccessController;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CORSConfigListener implements ServiceListener {

    private static final Debug debug = Debug.getInstance("frRest");

    private final static int DEFAULT_TIMEOUT = 600; //10 mins

    private ServiceConfigManager schemaManager;

    private CORSService corsService;

    public CORSConfigListener() {
        try {
            SSOToken token = AccessController.doPrivileged(AdminTokenAction.getInstance());
            ServiceConfigManager schemaManager =
                    new ServiceConfigManager(
                            "CORSService", token);

            register(schemaManager);
        } catch (Exception e) {
            debug.error("Cannot get ServiceConfigManager - cannot register default version config listener", e);
        }
    }

    public void register(ServiceConfigManager schemaManager) {
        this.schemaManager = schemaManager;
        updateSettings();
        if (this.schemaManager.addListener(this) == null) {
            debug.error("Could not add listener to ServiceConfigManager instance. Version behaviour changes will not "
                    + "be dynamically updated");
        }
    }

    private void updateSettings() {
        try {
            ServiceConfig serviceConfig = schemaManager.getGlobalConfig(null);
            Map<String, Set<String>> attrs = serviceConfig.getAttributes();

            final boolean enabled = CollectionHelper.getBooleanMapAttr(attrs, "cors-enabled", false);
            final List<String> allowedOrigins = new ArrayList<>(attrs.get("allowed-origins"));
            final List<String> acceptedMethods = new ArrayList<>(attrs.get("accepted-methods"));
            final List<String> acceptedHeaders = new ArrayList<>(attrs.get("accepted-headers"));
            final List<String> exposedHeaders =new ArrayList<>(attrs.get("exposed-headers"));
            final String expectedHostname = CollectionHelper.getMapAttr(attrs, "expected-hostname");
            final boolean allowCredentials = CollectionHelper.getBooleanMapAttr(attrs, "allow-credentials", false);

            final int maxAge = CollectionHelper.getIntMapAttr(attrs, "max-age", DEFAULT_TIMEOUT, debug);
            if (debug.messageEnabled()) {
                debug.message("Successfully updated CORS settings");
            }

            corsService = new CORSService(enabled, allowedOrigins, acceptedMethods, acceptedHeaders,
                    exposedHeaders, maxAge, allowCredentials, expectedHostname);
        } catch (Exception e) {
            debug.error("Not able to set version behaviour for rest endpoints", e);
        }
    }


    public CORSService getCorsService() {
        return this.corsService;
    }

    @Override
    public void schemaChanged(String serviceName, String version) {

    }

    @Override
    public void globalConfigChanged(String serviceName, String version, String groupName, String serviceComponent, int type) {
        updateSettings();
    }

    @Override
    public void organizationConfigChanged(String serviceName, String version, String orgName, String groupName, String serviceComponent, int type) {

    }
}
