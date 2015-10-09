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

package org.forgerock.openam.upgrade.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import com.iplanet.sso.SSOToken;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.upgrade.steps.UpgradeCTSMaxConnectionsConfigurationStep.ConnectionCount;
import org.forgerock.openam.upgrade.steps.UpgradeCTSMaxConnectionsConfigurationStep.ServerInstanceConfig;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class UpgradeCTSMaxConnectionsConfigurationStepTest {

    private UpgradeCTSMaxConnectionsConfigurationStep upgradeStep;

    @Mock
    private PrivilegedAction<SSOToken> adminTokenAction;
    @Mock
    private ConnectionFactory connectionFactory;
    @Mock
    private ConnectionCount connectionCount;
    @Mock
    private UpgradeCTSMaxConnectionsConfigurationStep.Helper helper;

    @Mock
    private ServerInstanceConfig defaultServerInstanceConfig;
    private Map<String, ServerInstanceConfig> serverInstanceConfigs;

    @BeforeMethod
    public void setup() throws Exception {
        initMocks(this);
        serverInstanceConfigs = new HashMap<>();
        upgradeStep = new UpgradeCTSMaxConnectionsConfigurationStep(adminTokenAction, connectionFactory,
                connectionCount, helper);

        given(helper.getDefaultServerConfig(any(SSOToken.class))).willReturn(defaultServerInstanceConfig);
        given(helper.getServerConfigs(any(SSOToken.class))).willReturn(serverInstanceConfigs);
        given(connectionCount.getConnectionCount(anyInt(), any(ConnectionType.class)))
                .willAnswer(new Answer<Integer>() {
                    @Override
                    public Integer answer(InvocationOnMock invocationOnMock) throws Throwable {
                        int max = (int) invocationOnMock.getArguments()[0];
                        return max / 2;
                    }
                });
    }

    @Test
    public void shouldUpgradeSingleServer() throws Exception {

        //Given
        setupDefaultServerConfiguration(true);
        ServerInstanceConfig serverInstance = setupServerInstanceConfiguration("SERVER_ONE", 10, null);

        //When
        upgradeStep.initialize();

        //Then
        assertThat(upgradeStep.isApplicable()).isTrue();

        //When
        upgradeStep.perform();

        //Then
        verify(defaultServerInstanceConfig).setCTSMaxConnections(Integer.toString(5));
        verify(serverInstance, never()).setCTSMaxConnections(anyString());
    }

    @Test
    public void shouldNotUpgradeServerWithExternalCTS() throws Exception {

        //Given
        setupDefaultServerConfiguration(false);
        ServerInstanceConfig serverInstance = setupServerInstanceConfiguration("SERVER_ONE", 10, null);

        //When
        upgradeStep.initialize();

        //Then
        assertThat(upgradeStep.isApplicable()).isFalse();
        verify(serverInstance, never()).setCTSMaxConnections(anyString());
    }

    @Test
    public void shouldUpgradeMultipleServersWithDifferentDirectoryConfigConnectionPoolSizes() throws Exception {

        //Given
        setupDefaultServerConfiguration(true);
        ServerInstanceConfig serverInstanceOne = setupServerInstanceConfiguration("SERVER_ONE", 10, null);
        ServerInstanceConfig serverInstanceTwo = setupServerInstanceConfiguration("SERVER_TWO", 11, null);
        ServerInstanceConfig serverInstanceThree = setupServerInstanceConfiguration("SERVER_THREE", 10, null);
        ServerInstanceConfig serverInstanceFour = setupServerInstanceConfiguration("SERVER_FOUR", 12, null);

        //When
        upgradeStep.initialize();

        //Then
        assertThat(upgradeStep.isApplicable()).isTrue();

        //When
        upgradeStep.perform();

        //Then
        verify(defaultServerInstanceConfig).setCTSMaxConnections(Integer.toString(5));
        verify(serverInstanceOne, never()).setCTSMaxConnections(anyString());
        verify(serverInstanceTwo).setCTSMaxConnections(Integer.toString(5));
        verify(serverInstanceThree, never()).setCTSMaxConnections(anyString());
        verify(serverInstanceFour).setCTSMaxConnections(Integer.toString(6));
    }

    @Test
    public void shouldUpgradeMultipleServersWithDifferentDirectoryConfigConnectionPoolSizesWithACTSMaxConnectionsSet()
            throws Exception {

        //Given
        setupDefaultServerConfiguration(true);
        ServerInstanceConfig serverInstanceOne = setupServerInstanceConfiguration("SERVER_ONE", 10, null);
        ServerInstanceConfig serverInstanceTwo = setupServerInstanceConfiguration("SERVER_TWO", 11, null);
        ServerInstanceConfig serverInstanceThree = setupServerInstanceConfiguration("SERVER_THREE", 10, null);
        ServerInstanceConfig serverInstanceFour = setupServerInstanceConfiguration("SERVER_FOUR", 12, 5);

        //When
        upgradeStep.initialize();

        //Then
        assertThat(upgradeStep.isApplicable()).isTrue();

        //When
        upgradeStep.perform();

        //Then
        verify(defaultServerInstanceConfig).setCTSMaxConnections(Integer.toString(5));
        verify(serverInstanceOne, never()).setCTSMaxConnections(anyString());
        verify(serverInstanceTwo).setCTSMaxConnections(Integer.toString(5));
        verify(serverInstanceThree, never()).setCTSMaxConnections(anyString());
        verify(serverInstanceFour, never()).setCTSMaxConnections(anyString());
    }

    private void setupDefaultServerConfiguration(boolean isDefaultStoreMode) {
        given(defaultServerInstanceConfig.getDirectoryConfigMaxConnections()).willReturn(null);
        given(defaultServerInstanceConfig.getCTSMaxConnections()).willReturn(null);
        given(defaultServerInstanceConfig.isDefaultStoreMode()).willReturn(isDefaultStoreMode);
    }

    private ServerInstanceConfig setupServerInstanceConfiguration(String serverName, int directoryConfigMaxConnections,
            Integer ctsMaxConnections) {
        ServerInstanceConfig serverInstance = mock(ServerInstanceConfig.class);
        given(serverInstance.getDirectoryConfigMaxConnections()).willReturn(directoryConfigMaxConnections);
        given(serverInstance.getCTSMaxConnections()).willReturn(ctsMaxConnections);
        serverInstanceConfigs.put(serverName, serverInstance);
        return serverInstance;
    }
}
