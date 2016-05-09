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

package org.forgerock.openam.core.rest.devices;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import org.forgerock.openam.core.rest.devices.oath.OathDeviceSettings;
import org.forgerock.openam.core.rest.devices.push.PushDeviceSettings;

/**
 * Guice module for binding the device REST endpoints.
 *
 * @since 14.0.0
 */
public class CoreRestDevicesGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(new TypeLiteral<DeviceJsonUtils<OathDeviceSettings>>() {})
                .toInstance(new DeviceJsonUtils<>(OathDeviceSettings.class));
        bind(new TypeLiteral<DeviceJsonUtils<PushDeviceSettings>>() {})
                .toInstance(new DeviceJsonUtils<>(PushDeviceSettings.class));
    }
}
