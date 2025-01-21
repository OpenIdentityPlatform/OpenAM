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

package org.forgerock.openam.core.rest.devices;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.iplanet.sso.SSOException;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;
import java.security.AccessController;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.forgerock.openam.core.rest.devices.deviceprint.TrustedDevicesDao;
import org.forgerock.openam.core.rest.devices.oath.OathDeviceSettings;
import org.forgerock.openam.core.rest.devices.oath.OathDevicesDao;
import org.forgerock.openam.core.rest.devices.push.PushDeviceSettings;
import org.forgerock.openam.core.rest.devices.push.PushDevicesDao;
import org.forgerock.openam.core.rest.devices.services.AuthenticatorDeviceServiceFactory;
import org.forgerock.openam.core.rest.devices.services.deviceprint.TrustedDeviceService;
import org.forgerock.openam.core.rest.devices.services.deviceprint.TrustedDeviceServiceFactory;
import org.forgerock.openam.core.rest.devices.services.oath.AuthenticatorOathService;
import org.forgerock.openam.core.rest.devices.services.oath.AuthenticatorOathServiceFactory;
import org.forgerock.openam.core.rest.devices.services.push.AuthenticatorPushService;
import org.forgerock.openam.core.rest.devices.services.push.AuthenticatorPushServiceFactory;

/**
 * Guice module for binding the device REST endpoints.
 *
 * @since 13.5.0
 */
public class CoreRestDevicesGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(new TypeLiteral<DeviceJsonUtils<OathDeviceSettings>>() {})
                .toInstance(new DeviceJsonUtils<>(OathDeviceSettings.class));
        bind(new TypeLiteral<DeviceJsonUtils<PushDeviceSettings>>() {})
                .toInstance(new DeviceJsonUtils<>(PushDeviceSettings.class));
    }

    @Provides
    @Inject
    public TrustedDevicesDao getTrustedDevicesDao(
            @Named(TrustedDeviceServiceFactory.FACTORY_NAME)
            AuthenticatorDeviceServiceFactory<TrustedDeviceService> serviceFactory) {
        return new TrustedDevicesDao(serviceFactory);
    }

    @Provides
    @Inject
    public PushDevicesDao getPushDevicesDao(
            @Named(AuthenticatorPushServiceFactory.FACTORY_NAME)
            AuthenticatorDeviceServiceFactory<AuthenticatorPushService> serviceFactory) {
        return new PushDevicesDao(serviceFactory);
    }

    @Provides
    @Inject
    public OathDevicesDao getOathDevicesDao(
            @Named(AuthenticatorOathServiceFactory.FACTORY_NAME)
            AuthenticatorDeviceServiceFactory<AuthenticatorOathService> serviceFactory) {
        return new OathDevicesDao(serviceFactory);
    }

    @Provides
    @Named(AuthenticatorPushService.SERVICE_NAME)
    ServiceConfigManager getAuthenticatorPushServiceManager() throws SMSException, SSOException {
        return new ServiceConfigManager(AccessController.doPrivileged(AdminTokenAction.getInstance()),
                AuthenticatorPushService.SERVICE_NAME, AuthenticatorPushService.SERVICE_VERSION);
    }

    @Provides
    @Named(AuthenticatorOathService.SERVICE_NAME)
    ServiceConfigManager getAuthenticatorOathServiceManager() throws SMSException, SSOException {
        return new ServiceConfigManager(AccessController.doPrivileged(AdminTokenAction.getInstance()),
                AuthenticatorOathService.SERVICE_NAME, AuthenticatorOathService.SERVICE_VERSION);
    }

    @Provides
    @Named(AuthenticatorOathServiceFactory.FACTORY_NAME)
    @Inject
    @Singleton
    AuthenticatorDeviceServiceFactory<AuthenticatorOathService> getAuthenticatorOathServiceFactory(
            @Named("frRest") Debug debug,
            @Named(AuthenticatorOathService.SERVICE_NAME) ServiceConfigManager serviceConfigManager) {
        return new AuthenticatorDeviceServiceFactory<>(debug, serviceConfigManager,
                new AuthenticatorOathServiceFactory());
    }

    @Provides
    @Named(AuthenticatorPushServiceFactory.FACTORY_NAME)
    @Inject
    @Singleton
    AuthenticatorDeviceServiceFactory<AuthenticatorPushService> getAuthenticatorPushServiceFactory(
            @Named("frRest") Debug debug,
            @Named(AuthenticatorPushService.SERVICE_NAME) ServiceConfigManager serviceConfigManager) {
        return new AuthenticatorDeviceServiceFactory<>(debug, serviceConfigManager,
                new AuthenticatorPushServiceFactory());
    }

    @Provides
    @Named(TrustedDeviceServiceFactory.FACTORY_NAME)
    @Inject
    AuthenticatorDeviceServiceFactory<TrustedDeviceService> getTrustedDeviceServiceFactory(
            @Named("frRest") Debug debug) {
        return new AuthenticatorDeviceServiceFactory<>(debug, null, new TrustedDeviceServiceFactory());
    }
}
