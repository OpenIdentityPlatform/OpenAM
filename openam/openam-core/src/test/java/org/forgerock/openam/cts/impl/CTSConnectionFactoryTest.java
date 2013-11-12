/**
 * Copyright 2013 ForgeRock AS.
 *
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
 */
package org.forgerock.openam.cts.impl;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.ExternalTokenConfig;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.openam.sm.DataLayerConnectionFactory;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.forgerock.opendj.ldap.LDAPOptions;
import org.forgerock.opendj.ldap.ResultHandler;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Set;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author robert.wapshott@forgerock.com
 */
@PrepareForTest(LDAPUtils.class)
public class CTSConnectionFactoryTest extends PowerMockTestCase {

    private LDAPConfig mockLDAPConfig;
    private DataLayerConnectionFactory mockDLCF;
    private ExternalTokenConfig mockExternal;
    private CTSConnectionFactory factory;

    @BeforeMethod
    public void setup() {
        mockDLCF = mock(DataLayerConnectionFactory.class);
        mockLDAPConfig = mock(LDAPConfig.class);
        mockExternal = mock(ExternalTokenConfig.class);

        given(mockExternal.getStoreMode()).willReturn(ExternalTokenConfig.StoreMode.DEFAULT);
        given(mockLDAPConfig.hasChanged()).willReturn(true);

        factory = new CTSConnectionFactory(
                mockDLCF,
                mockLDAPConfig,
                mockExternal,
                mock(Debug.class));
    }

    @Test
    public void shouldCloseFactoryOnShutdown() {
        // Given
        // When
        factory.shutdown();
        // Then
        verify(mockDLCF).close();
    }

    @Test
    public void shouldCloseFactoryOnClose() {
        // Given
        // When
        factory.close();
        // Then
        verify(mockDLCF).close();
    }

    @Test
    public void shouldUseDLCFByDefault() throws ErrorResultException {
        // Given
        // When
        factory.getConnection();
        // Then
        verify(mockDLCF).getConnection();
    }

    @Test
    public void shouldUseFactoryForAsynConnection() {
        // Given
        // When
        factory.getConnectionAsync(mock(ResultHandler.class));
        // Then
        verify(mockDLCF).getConnectionAsync(any(ResultHandler.class));
    }

    @Test
    public void shouldUseLDAPUtilsForExternalConfiguration() {
        // Given
        PowerMockito.mockStatic(LDAPUtils.class);
        ConnectionFactory mockConnectionFactory = mock(ConnectionFactory.class);
        given(LDAPUtils.newFailoverConnectionPool(
                any(Set.class),
                anyString(),
                any(char[].class),
                anyInt(),
                anyInt(),
                anyString(),
                any(LDAPOptions.class))).willReturn(mockConnectionFactory);

        given(mockExternal.getStoreMode()).willReturn(ExternalTokenConfig.StoreMode.EXTERNAL);
        given(mockExternal.getHostname()).willReturn("badger");
        given(mockExternal.getMaxConnections()).willReturn("50");
        given(mockExternal.getPassword()).willReturn("weasel");
        given(mockExternal.getPort()).willReturn("1234");
        given(mockExternal.getUsername()).willReturn("ferret");

        given(mockExternal.hasChanged()).willReturn(true);

        // When
        factory.updateConnection();
        // Then
        PowerMockito.verifyStatic();
        LDAPUtils.newFailoverConnectionPool(
                any(Set.class),
                anyString(),
                any(char[].class),
                anyInt(),
                anyInt(),
                anyString(),
                any(LDAPOptions.class));
    }

    @Test
    public void shouldPreventInvalidNumbers() throws ErrorResultException {
        // Given
        String invalidNumber = "123abc";

        given(mockExternal.getStoreMode()).willReturn(ExternalTokenConfig.StoreMode.EXTERNAL);
        given(mockExternal.getHostname()).willReturn("badger");
        given(mockExternal.getMaxConnections()).willReturn("50");
        given(mockExternal.getPassword()).willReturn("weasel");
        given(mockExternal.getPort()).willReturn(invalidNumber);
        given(mockExternal.getUsername()).willReturn("ferret");
        given(mockExternal.hasChanged()).willReturn(true);
        factory.updateConnection();

        // When
        factory.getConnection();
        // Then
        // verify that the external mode was not selected
        verify(mockDLCF).getConnection();
    }

    @Test
    public void shouldCatchMissingValues() throws ErrorResultException {
        // Given
        String missingValue = null;

        given(mockExternal.getStoreMode()).willReturn(ExternalTokenConfig.StoreMode.EXTERNAL);
        given(mockExternal.getHostname()).willReturn(missingValue);
        given(mockExternal.getMaxConnections()).willReturn("50");
        given(mockExternal.getPassword()).willReturn("weasel");
        given(mockExternal.getPort()).willReturn("1234");
        given(mockExternal.getUsername()).willReturn("ferret");
        given(mockExternal.hasChanged()).willReturn(true);
        factory.updateConnection();

        // When
        factory.getConnection();
        // Then
        // verify that the external mode was not selected
        verify(mockDLCF).getConnection();
    }

    @Test
    public void shouldNotCloseDefaultFactoryOnChange() {
        // Given
        given(mockExternal.getStoreMode()).willReturn(ExternalTokenConfig.StoreMode.EXTERNAL);
        given(mockExternal.getHostname()).willReturn("badger");
        given(mockExternal.getMaxConnections()).willReturn("50");
        given(mockExternal.getPassword()).willReturn("weasel");
        given(mockExternal.getPort()).willReturn("1234");
        given(mockExternal.getUsername()).willReturn("ferret");
        given(mockExternal.hasChanged()).willReturn(true);
        // When
        factory.updateConnection();
        // Then
        verify(mockDLCF, never()).close();
    }
}
