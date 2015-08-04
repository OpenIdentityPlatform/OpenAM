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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.xui;

import java.security.AccessController;
import java.util.Map;

import com.google.inject.Singleton;
import com.iplanet.sso.SSOException;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceListener;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

@Singleton
public class XUIState {

    enum XUIMode implements ServiceListener {
        XUI_MODE("iPlanetAMAuthService", "openam-xui-interface-enabled"),
        XUI_ADMIN_MODE("iPlanetAMAdminConsoleService", "xuiAdminConsoleEnabled");

        private final Debug DEBUG = Debug.getInstance("Configuration");
        private final String service;
        private final String attribute;
        private final ServiceSchemaManager schemaManager;
        private boolean enabled;
        private String listenerId;

        XUIMode(String service, String attribute) {
            try {
                this.service = service;
                this.attribute = attribute;
                this.schemaManager = new ServiceSchemaManager(service,
                        AccessController.doPrivileged(AdminTokenAction.getInstance()));
                detectMode(service, attribute);
            } catch (SMSException | SSOException e) {
                DEBUG.error("Could not get " + service, e);
                throw new IllegalStateException("Could not get " + service, e);
            }
        }

        private void detectMode(String service, String attribute) {
            try {
                ServiceSchema schema = schemaManager.getGlobalSchema();
                Map defaults = schema.getAttributeDefaults();
                enabled = Boolean.parseBoolean(CollectionHelper.getMapAttr(defaults, attribute, ""));
                if (listenerId == null) {
                    listenerId = schemaManager.addListener(this);
                }
            } catch (SMSException e) {
                DEBUG.error("Could not get " + service, e);
                throw new IllegalStateException("Could not get " + service, e);
            }
        }

        public static void destroy() {
            for (XUIMode state : XUIMode.values()) {
                if (state.listenerId != null) {
                    state.schemaManager.removeListener(state.listenerId);
                    state.listenerId = null;
                }
            }
        }

        @Override
        public void organizationConfigChanged(String serviceName, String version, String orgName, String groupName,
                String serviceComponent, int type) {
            // no op
        }

        @Override
        public void globalConfigChanged(String serviceName, String version,
                String groupName, String serviceComponent, int type) {
            detectMode(service, attribute);
        }

        @Override
        public void schemaChanged(String serviceName, String version) {
            detectMode(service, attribute);
        }
    }

    public boolean isXUIEnabled() {
        return XUIMode.XUI_MODE.enabled;
    }

    public boolean isXUIAdminEnabled() {
        return XUIMode.XUI_ADMIN_MODE.enabled;
    }

    public void destroy() {
        for (XUIMode state : XUIMode.values()) {
            if (state.listenerId != null) {
                state.schemaManager.removeListener(state.listenerId);
                state.listenerId = null;
            }
        }
    }
}
