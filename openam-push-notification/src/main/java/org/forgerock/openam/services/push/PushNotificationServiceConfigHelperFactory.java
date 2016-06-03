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
* Copyright 2016 ForgeRock AS.
*/
package org.forgerock.openam.services.push;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iplanet.sso.SSOException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;

/**
 * Decouples the PushNotificationService from its config to aid testing.
 */
@Singleton
public class PushNotificationServiceConfigHelperFactory {

    private final ServiceConfigManager serviceConfigManager;
    private final Debug debug;

    /**
     * Construct a new PushNotificationServiceConfigHelperFactory which will produce
     * PushNotificationServiceConfigHelpers for the provided realm.
     *
     * Be sure to call addListener on this object after it's newly constructed to ensure
     * the services are kept up-to-date with any alterations.
     *
     * @param serviceConfigManager Used to access the individual org configs.
     * @param debug for debug output.
     */
    @Inject
    public PushNotificationServiceConfigHelperFactory(
            @Named("PushNotificationService") ServiceConfigManager serviceConfigManager, @Named("frPush") Debug debug) {
        this.serviceConfigManager = serviceConfigManager;
        this.debug = debug;
    }

    /**
     * Add a listener to the service config manager held by this factory.
     *
     * @param listener A ServiceListener capable of keeping appropriate services up-to-date with changes.
     */
    public void addListener(ServiceListener listener) {
        serviceConfigManager.addListener(listener);
    }

    /**
     * Produces a config helper for a given realm.
     *
     * @param realm The realm under which to read the config from.
     * @return A new PushNotificationServiceConfigHelper for the service's config for the provided realm.
     * @throws SSOException If the user does not have privs to read the org config.
     * @throws SMSException If the retrieved org config was null.
     */
    public PushNotificationServiceConfigHelper getConfigHelperFor(String realm)
            throws SSOException, SMSException {
        ServiceConfig serviceConfig = serviceConfigManager.getOrganizationConfig(realm, null);

        if (serviceConfig == null) {
            debug.error("Unable to retrieve instance of the ServiceConfig for realm {}.", realm);
            throw new SMSException("Unable to retrieve instance of the ServiceConfig.");
        }

        return new PushNotificationServiceConfigHelper(serviceConfig, debug);
    }
}
