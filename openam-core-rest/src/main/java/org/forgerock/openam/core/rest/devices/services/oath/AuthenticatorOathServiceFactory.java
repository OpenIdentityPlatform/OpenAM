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
* Copyright 2015-2016 ForgeRock AS.
* Portions copyright 2025 3A Systems LLC.
*/
package org.forgerock.openam.core.rest.devices.services.oath;

import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;
import jakarta.inject.Singleton;
import org.forgerock.openam.core.rest.devices.services.DeviceServiceFactory;

/**
 * Produces AuthenticatorOathService's for a specific realm.
 */
@Singleton
public class AuthenticatorOathServiceFactory implements DeviceServiceFactory<AuthenticatorOathService> {

    /** Name of this factory for Guice purposes. */
    public static final String FACTORY_NAME = "AuthenticatorOathServiceFactory";

    @Override
    public AuthenticatorOathService create(ServiceConfigManager serviceConfigManager, String realm)
            throws SSOException, SMSException {
        return new AuthenticatorOathService(serviceConfigManager, realm);
    }

}
