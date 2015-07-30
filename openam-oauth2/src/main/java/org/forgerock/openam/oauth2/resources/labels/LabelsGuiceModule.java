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

package org.forgerock.openam.oauth2.resources.labels;

import javax.inject.Singleton;

import org.forgerock.openam.sm.ConnectionConfig;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.openam.sm.datalayer.impl.ldap.ExternalConnectionConfigProvider;
import org.forgerock.openam.sm.datalayer.impl.ldap.LdapDataLayerConfiguration;

import com.google.inject.Key;
import com.google.inject.PrivateModule;
import com.google.inject.name.Names;

/**
 * A private module for Guice access to labels stored in LDAP.
 */
public class LabelsGuiceModule extends PrivateModule {
    @Override
    protected void configure() {
        bind(ConnectionType.class).toInstance(ConnectionType.UMA_LABELS);
        bind(LdapDataLayerConfiguration.class).to(LabelsDataLayerConfiguration.class).in(Singleton.class);
        bind(Key.get(LdapDataLayerConfiguration.class, DataLayer.Types.typed(ConnectionType.UMA_LABELS)))
                .toProvider(getProvider(LdapDataLayerConfiguration.class));
        expose(Key.get(LdapDataLayerConfiguration.class, DataLayer.Types.typed(ConnectionType.UMA_LABELS)));
        bind(ConnectionConfig.class).annotatedWith(Names.named(DataLayerConstants.EXTERNAL_CONFIG))
                .toProvider(ExternalConnectionConfigProvider.class);
        bind(UmaLabelsStore.class);
        expose(UmaLabelsStore.class);
    }
}
