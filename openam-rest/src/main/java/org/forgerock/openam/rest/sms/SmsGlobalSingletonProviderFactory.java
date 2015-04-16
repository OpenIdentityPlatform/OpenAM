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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.rest.sms;

import java.util.List;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;

/**
 * A Guice factory interface for creating {@link SmsGlobalSingletonProvider} instances.
 *
 * @since 13.0.0
 */
public interface SmsGlobalSingletonProviderFactory {

    SmsGlobalSingletonProvider create(SmsJsonConverter globalConverter,
            @Assisted("global") ServiceSchema globalSchema, @Assisted("organization") ServiceSchema organizationSchema,
            @Assisted("dynamic") ServiceSchema dynamicSchema, SchemaType type, List<ServiceSchema> subSchemaPath,
            String uriPath, boolean serviceHasInstanceName);
}
