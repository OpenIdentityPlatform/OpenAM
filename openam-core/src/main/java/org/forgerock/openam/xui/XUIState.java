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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.xui;

import com.google.inject.Singleton;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceListener;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

import java.security.AccessController;
import java.util.Map;

@Singleton
public class XUIState implements ServiceListener {

    private static final String XUI_INTERFACE = "openam-xui-interface-enabled";
    private static final String SERVICE_NAME = "iPlanetAMAuthService";
    private static final Debug DEBUG = Debug.getInstance("Configuration");
    private String listenerID;
    private boolean initialized;
    private ServiceSchemaManager scm;
    private boolean xuiEnabled;

    public boolean isXUIEnabled() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    detectXUIMode();
                }
            }
        }

        return xuiEnabled;
    }

    /**
     * detectXUIMode will detect if XUI is enabled or disabled by inspecting the service
     */
    protected void detectXUIMode() {
        try {
            SSOToken dUserToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
            scm = new ServiceSchemaManager(SERVICE_NAME, dUserToken);
            ServiceSchema schema = scm.getGlobalSchema();
            Map attrs = schema.getAttributeDefaults();
            xuiEnabled = Boolean.parseBoolean(CollectionHelper.getMapAttr(attrs, XUI_INTERFACE, ""));
            if (listenerID == null) {
                listenerID = scm.addListener(this);
            }
            initialized = true;
        } catch (SMSException smse) {
            DEBUG.error("Could not get iPlanetAMAuthService", smse);
        } catch (SSOException ssoe) {
            DEBUG.error("Could not get iPlanetAMAuthService", ssoe);
        }
    }

    public void destroy() {
        if (listenerID != null && scm != null) {
            scm.removeListener(listenerID);
            listenerID = null;
            initialized = false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void organizationConfigChanged(String serviceName, String version,
            String orgName, String groupName, String serviceComponent,
            int type) {
        // no op
    }

    /**
     * {@inheritDoc}
     */
    public void globalConfigChanged(String serviceName, String version,
            String groupName, String serviceComponent, int type) {
        detectXUIMode();
    }

    /**
     * {@inheritDoc}
     */
    public void schemaChanged(String serviceName, String version) {
        detectXUIMode();
    }
}
