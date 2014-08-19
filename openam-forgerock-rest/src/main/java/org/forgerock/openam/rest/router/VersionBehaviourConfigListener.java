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

package org.forgerock.openam.rest.router;

import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import org.forgerock.openam.forgerockrest.ServiceConfigUtils;
import org.forgerock.openam.rest.DefaultVersionBehaviour;

import javax.inject.Inject;
import java.security.AccessController;

/**
 * A {@code ServiceListener} to listen for changes to the default version behaviour configuration and update a
 * {@code VersionedRouter}.
 */
public class VersionBehaviourConfigListener implements ServiceListener {

    static final String VERSION_BEHAVIOUR_ATTRIBUTE = "openam-rest-apis-default-version";
    static final String WARNING_BEHAVIOUR_ATTRIBUTE = "openam-rest-apis-header-warning";

    private static final String SERVICE_NAME = "RestApisService";
    private static final String SERVICE_VERSION = "1.0";
    private static Debug debug = Debug.getInstance("frRest");

    private final VersionedRouter<?> router;
    private ServiceConfigManager mgr;

    @Inject
    public VersionBehaviourConfigListener(VersionedRouter<?> router) {
        this.router = router;
    }

    /**
     * Registers a new instance of this {@link ServiceListener} with a {@link ServiceConfigManager} so that the
     * provided {@link org.forgerock.openam.rest.router.VersionedRouter} can be kept in sync with changes to the
     * default version behaviour.
     *
     * @param router The VersionedRouter to update when default version behaviour changes
     */
    public static void bindToServiceConfigManager(VersionedRouter<?> router) {
        try {
            VersionBehaviourConfigListener configListener = new VersionBehaviourConfigListener(router);
            ServiceConfigManager mgr = new ServiceConfigManager(
                    AccessController.doPrivileged(AdminTokenAction.getInstance()), SERVICE_NAME, SERVICE_VERSION);
            configListener.register(mgr);
        } catch (Exception e) {
            debug.error("Cannot get ServiceConfigManager - cannot register default version config listener", e);
        }
    }

    /**
     * Registers the listener with the {@code ServiceConfigManager} and sets the initial default version behaviour.
     * @param mgr
     */
    public void register(ServiceConfigManager mgr) {
        this.mgr = mgr;
        updateSettings();
        if (mgr.addListener(this) == null) {
            debug.error("Could not add listener to ServiceConfigManager instance. Version behaviour changes will not " +
                    "be dynamically updated");
        }
    }

    private void updateSettings() {
        try {
            ServiceConfig serviceConfig = mgr.getGlobalConfig(null);
            String versionBehaviour = ServiceConfigUtils.getStringAttribute(serviceConfig, VERSION_BEHAVIOUR_ATTRIBUTE);
            router.setVersioning(DefaultVersionBehaviour.valueOf(versionBehaviour.toUpperCase()));
            final boolean warningBehaviour = ServiceConfigUtils.getBooleanAttribute(serviceConfig,
                    WARNING_BEHAVIOUR_ATTRIBUTE);
            router.setHeaderWarningEnabled(warningBehaviour);
            if (debug.messageEnabled()) {
                debug.message("Successfully updated rest version behaviour settings to " + versionBehaviour);
            }
        } catch (Exception e) {
            debug.error("Not able to set version behaviour for rest endpoints", e);
        }
    }

    /**
     * Not used
     */
    @Override
    public void schemaChanged(String serviceName, String version) {
        // Nothing to do
    }

    /**
     * Updates the default version behaviour when notified of changes.
     * @param serviceName
     *            name of the service.
     * @param version
     *            version of the service.
     * @param groupName
     *            name of the configuration grouping.
     * @param serviceComponent
     *            name of the service components that changed.
     * @param type
     */
    @Override
    public void globalConfigChanged(String serviceName, String version, String groupName, String serviceComponent,
                                    int type) {
        updateSettings();
    }

    /**
     * Not used.
     */
    @Override
    public void organizationConfigChanged(String serviceName, String version, String orgName, String groupName, String serviceComponent, int type) {
        // Nothing to do
    }
}
