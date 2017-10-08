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
package org.forgerock.openam.plugin.configuration;

import java.util.Set;

import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.openam.audit.configuration.AMAuditServiceConfiguration;
import org.forgerock.openam.audit.configuration.AuditEventHandlerConfiguration;
import org.forgerock.openam.audit.configuration.AuditServiceConfigurationListener;
import org.forgerock.openam.audit.configuration.AuditServiceConfigurationProvider;

/**
 * No-Op AuditServiceConfigurationProvider implementation for the OpenAM Fedlet.
 */
public class FedletAuditServiceConfigurationProviderImpl implements AuditServiceConfigurationProvider {

    @Override
    public void setupComplete() {
        // this section intentionally left blank
    }

    @Override
    public void addConfigurationListener(AuditServiceConfigurationListener listener) {
        // this section intentionally left blank
    }

    @Override
    public void removeConfigurationListener(AuditServiceConfigurationListener listener) {
        // this section intentionally left blank
    }

    @Override
    public AMAuditServiceConfiguration getDefaultConfiguration() {
        return new AMAuditServiceConfiguration(false);
    }

    @Override
    public AMAuditServiceConfiguration getRealmConfiguration(String realm) {
        return new AMAuditServiceConfiguration(false);
    }

    @Override
    public Set<AuditEventHandlerConfiguration> getDefaultEventHandlerConfigurations() {
        return null;
    }

    @Override
    public Set<AuditEventHandlerConfiguration> getRealmEventHandlerConfigurations(String realm) {
        return null;
    }

    @Override
    public EventTopicsMetaData getEventTopicsMetaData() {
        return null;
    }
}