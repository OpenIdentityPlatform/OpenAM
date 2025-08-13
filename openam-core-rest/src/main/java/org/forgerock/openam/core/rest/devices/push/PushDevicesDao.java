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

package org.forgerock.openam.core.rest.devices.push;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.forgerock.openam.core.rest.devices.UserDevicesDao;
import org.forgerock.openam.core.rest.devices.services.AuthenticatorDeviceServiceFactory;
import org.forgerock.openam.core.rest.devices.services.push.AuthenticatorPushService;
import org.forgerock.openam.core.rest.devices.services.push.AuthenticatorPushServiceFactory;

/**
 * A DAO instance for accessing Push user devices.
 *
 * @since 13.5.0
 */
public class PushDevicesDao extends UserDevicesDao {

    /**
     * Construct a new PushDevicesDao.
     *
     * @param serviceFactory Factory used to retrieve the Push Service for this dao.
     */
    @Inject
    public PushDevicesDao(@Named(AuthenticatorPushServiceFactory.FACTORY_NAME)
                          AuthenticatorDeviceServiceFactory<AuthenticatorPushService> serviceFactory) {
        super(serviceFactory);
    }

}
