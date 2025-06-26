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
package org.forgerock.oauth2.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.openam.oauth2.AgentClientRegistration;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.oauth2.OAuth2RealmResolver;
import org.forgerock.openam.oauth2.resources.ResourceSetStoreFactory;
import org.forgerock.openam.sm.ServiceConfigManagerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;

public class OAuth2ProviderSettingsFactoryTest {

    private OAuth2ProviderSettingsFactory factory;

    @BeforeMethod
    public void setUpTest() throws SSOException, SMSException {
        ResourceSetStoreFactory resourceSetStoreFactory = mock(ResourceSetStoreFactory.class);
        OAuth2RealmResolver realmResolver = mock(OAuth2RealmResolver.class);
        ServiceConfigManager serviceConfigManager = mock(ServiceConfigManager.class);
        ServiceConfigManagerFactory serviceConfigManagerFactory = mock(ServiceConfigManagerFactory.class);
        when(serviceConfigManagerFactory.create(OAuth2Constants.OAuth2ProviderService.NAME,
                OAuth2Constants.OAuth2ProviderService.VERSION)).thenReturn(serviceConfigManager);
        factory = new OAuth2ProviderSettingsFactory(resourceSetStoreFactory,
                realmResolver, serviceConfigManagerFactory);
    }

    @Test
    public void shouldReturnAgentProviderSettingsIfRequestIncludesAgentProviderParameter() throws NotFoundException {
        OAuth2Request request = mock(OAuth2Request.class);
        when(request.getClientRegistration()).thenReturn(mock(AgentClientRegistration.class));

        OAuth2ProviderSettings providerSettings = factory.get(request);

        assertThat(providerSettings).isInstanceOf(AgentOAuth2ProviderSettings.class);
    }
}
