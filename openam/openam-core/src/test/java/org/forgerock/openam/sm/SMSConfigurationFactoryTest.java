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
 * Copyright 2014 ForgeRock AS.
 */
package org.forgerock.openam.sm;

import com.iplanet.services.ldap.LDAPUser;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.sm.exceptions.ConnectionCredentialsNotFound;
import org.forgerock.openam.sm.exceptions.ServerConfigurationNotFound;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;

public class SMSConfigurationFactoryTest {

    private ServerConfigurationFactory mockFactory;
    private SMSConfigurationFactory factory;

    @BeforeMethod
    public void setup() {
        mockFactory = mock(ServerConfigurationFactory.class);
        factory = new SMSConfigurationFactory(mockFactory, mock(Debug.class));
    }
    @Test (expectedExceptions = IllegalStateException.class)
    public void shouldFailIfNoConfigurationsDefined() throws ConnectionCredentialsNotFound, ServerConfigurationNotFound {
        given(mockFactory.getServerConfiguration(
                anyString(), any(LDAPUser.Type.class)))
                .willThrow(mock(ServerConfigurationNotFound.class));
        factory.getSMSConfiguration();
    }

    @Test
    public void shouldReturnFirstValidConfiguration() throws ConnectionCredentialsNotFound, ServerConfigurationNotFound {
        // Given
        ServerGroupConfiguration first = mock(ServerGroupConfiguration.class);
        ServerGroupConfiguration second = mock(ServerGroupConfiguration.class);
        given(mockFactory.getServerConfiguration(anyString(), any(LDAPUser.Type.class)))
                .willReturn(first)
                .willReturn(second);

        // When
        ServerGroupConfiguration result = factory.getSMSConfiguration();

        // Then
        assertThat(result).isEqualTo(first);
    }
}