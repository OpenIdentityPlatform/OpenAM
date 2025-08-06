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
* Portions copyright 2025 3A Systems LLC.
*/
package org.forgerock.openam.core.rest.devices.services.push;

import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;
import jakarta.inject.Singleton;
import org.forgerock.openam.core.rest.devices.services.DeviceServiceFactory;

/**
 * Produces AuthenticatorPushServices for a specific realm.
 */
@Singleton
public class AuthenticatorPushServiceFactory implements DeviceServiceFactory<AuthenticatorPushService> {

    /** Name of this factory for Guice purposes. */
    public static final String FACTORY_NAME = "AuthenticatorPushServiceFactory";

    @Override
    public AuthenticatorPushService create(ServiceConfigManager serviceConfigManager, String realm)
            throws SMSException, SSOException {
        return new AuthenticatorPushService(serviceConfigManager, realm);
    }
}
