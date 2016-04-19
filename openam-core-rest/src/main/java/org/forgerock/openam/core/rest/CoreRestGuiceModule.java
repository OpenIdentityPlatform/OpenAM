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
 */

package org.forgerock.openam.core.rest;

import com.google.inject.AbstractModule;
import org.forgerock.guice.core.GuiceModule;
import org.forgerock.openam.core.rest.authn.CoreRestAuthenticationGuiceModule;
import org.forgerock.openam.core.rest.cts.CoreRestCtsGuiceModule;
import org.forgerock.openam.core.rest.devices.CoreRestDevicesGuiceModule;
import org.forgerock.openam.core.rest.record.CoreRestRecordGuiceModule;
import org.forgerock.openam.core.rest.session.CoreRestSessionGuiceModule;
import org.forgerock.openam.core.rest.sms.CoreRestSmsGuiceModule;

/**
 * Guice module for binding the core REST endpoints.
 *
 * @since 13.0.0
 */
@GuiceModule
public class CoreRestGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new CoreRestSmsGuiceModule());
        install(new CoreRestAuthenticationGuiceModule());
        install(new CoreRestSessionGuiceModule());
        install(new CoreRestCtsGuiceModule());
        install(new CoreRestIdentityGuiceModule());
        install(new CoreRestDevicesGuiceModule());
        install(new CoreRestRecordGuiceModule());
    }
}