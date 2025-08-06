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
 * Copyright 2014-2016 ForgeRock AS.
 * Portions Copyrighted 2015 Nomura Research Institute, Ltd.
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package org.forgerock.openam.core.rest.devices.deviceprint;

import jakarta.inject.Singleton;
import org.forgerock.openam.core.rest.devices.UserDevicesDao;
import org.forgerock.openam.core.rest.devices.services.AuthenticatorDeviceServiceFactory;
import org.forgerock.openam.core.rest.devices.services.deviceprint.TrustedDeviceService;

/**
 * Dao for handling the retrieval and saving of a user's trusted devices.
 *
 * @since 12.0.0
 */
@Singleton
public class TrustedDevicesDao extends UserDevicesDao {

    public TrustedDevicesDao(AuthenticatorDeviceServiceFactory<TrustedDeviceService> serviceFactory) {
        super(serviceFactory);
    }

}
