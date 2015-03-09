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

package com.iplanet.dpro.session.service.cluster;

import static org.fest.assertions.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.SessionServerConfig;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.dpro.session.service.SessionServiceConfig;
import com.iplanet.dpro.session.service.cluster.MultiServerClusterMonitor.ClusterStateServiceFactory;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.utils.CollectionUtils;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.Map;

public class MultiServerClusterMonitorTest {

    private SessionService mockSessionService;
    private Debug mockDebug;
    private SessionServerConfig mockServerConfig;
    private SessionServiceConfig mockServiceConfig;
    private ClusterStateServiceFactory mockFactory;

    @BeforeTest
    public void setUp() {
        mockSessionService = mock(SessionService.class);
        mockDebug = mock(Debug.class);
        mockServerConfig = mock(SessionServerConfig.class);
        mockServiceConfig = mock(SessionServiceConfig.class);
        mockFactory = mock(ClusterStateServiceFactory.class);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void requiresSessionFailover() throws Exception {
        // Given
        given(mockServiceConfig.isSessionFailoverEnabled()).willReturn(false);

        // When
        new MultiServerClusterMonitor(mockSessionService, mockDebug, mockServiceConfig, mockServerConfig);

        // Then
        // Expect IllegalStateException
    }

    @Test
    public void canEstablishHostServerIdForSession() throws Exception {
        // Given

        given(mockServiceConfig.isSessionFailoverEnabled()).willReturn(true);
        given(mockServiceConfig.isUseInternalRequestRoutingEnabled()).willReturn(true);

        given(mockServerConfig.getServerIDsInLocalSite()).willReturn(CollectionUtils.asSet("01", "02"));
        given(mockServerConfig.getServerFromID("01")).willReturn("http://openam1.example.com:18080/openam");
        given(mockServerConfig.getServerFromID("02")).willReturn("http://openam2.example.com:28080/openam");
        given(mockServerConfig.getServerFromID("03")).willReturn("http://openam.example.com:8080/openam"); // the 'site'
        given(mockServerConfig.getLocalServerID()).willReturn("01");
        given(mockServerConfig.getLocalServerSessionServiceURL()).willReturn(new URL("http://openam1.example.com:18080/openam/sessionservice"));
        given(mockServerConfig.getAllServerIDs()).willReturn(CollectionUtils.asList("02", "01"));
        given(mockServerConfig.isSite("02")).willReturn(false);
        given(mockServerConfig.isSite("01")).willReturn(false);
        given(mockServerConfig.isLocalServer("03")).willReturn(false);
        given(mockServerConfig.isPrimaryServer("03")).willReturn(true);

        ClusterStateService mockClusterService = mock(ClusterStateService.class);
        given(mockFactory.createClusterStateService(
                eq(mockSessionService), eq(mockServerConfig), eq(mockServiceConfig), any(Map.class), any(Map.class)))
                .willReturn(mockClusterService);
        given(mockClusterService.isUp("01")).willReturn(true);

        SessionID mockSessionID = mock(SessionID.class);
        given(mockSessionID.getExtension(SessionID.PRIMARY_ID)).willReturn("01");
        given(mockSessionID.getSessionServerID()).willReturn("03");

        // When
        MultiServerClusterMonitor clusterMonitor = new MultiServerClusterMonitor(
                mockSessionService, mockDebug, mockServiceConfig, mockServerConfig, mockFactory);
        String currentHostServer = clusterMonitor.getCurrentHostServer(mockSessionID);

        // Then
        assertThat(currentHostServer).isEqualTo("01");
    }

    @Test
    public void canDeterministicallyEstablishFailoverHostServerIdForSessionIfHomeServerIsDown() throws Exception {
        // Given

        // A site with 2x AM servers, if server "01" is presented a session that is homed
        // on server "02" which is down, then a deterministic routing algorithm should be
        // used to generate a sequence of alternative servers to try to re-home the session
        // to. The first of these alternate servers that is found to be up will become the
        // new home server for that session.

        given(mockServiceConfig.isSessionFailoverEnabled()).willReturn(true);
        given(mockServiceConfig.isUseInternalRequestRoutingEnabled()).willReturn(true);

        given(mockServerConfig.getServerIDsInLocalSite()).willReturn(CollectionUtils.asSet("01", "02"));
        given(mockServerConfig.getServerFromID("01")).willReturn("http://openam1.example.com:18080/openam");
        given(mockServerConfig.getServerFromID("02")).willReturn("http://openam2.example.com:28080/openam");
        given(mockServerConfig.getServerFromID("03")).willReturn("http://openam.example.com:8080/openam"); // the 'site'
        given(mockServerConfig.getLocalServerID()).willReturn("02");
        given(mockServerConfig.getLocalServerSessionServiceURL()).willReturn(new URL("http://openam2.example.com:28080/openam/sessionservice"));
        given(mockServerConfig.getAllServerIDs()).willReturn(CollectionUtils.asList("02", "01"));
        given(mockServerConfig.isSite("02")).willReturn(false);
        given(mockServerConfig.isSite("01")).willReturn(false);
        given(mockServerConfig.isLocalServer("03")).willReturn(false);
        given(mockServerConfig.isPrimaryServer("03")).willReturn(true);

        ClusterStateService mockClusterService = mock(ClusterStateService.class);
        given(mockFactory.createClusterStateService(
                eq(mockSessionService), eq(mockServerConfig), eq(mockServiceConfig), any(Map.class), any(Map.class)))
                .willReturn(mockClusterService);
        given(mockClusterService.isUp("01")).willReturn(true);
        given(mockClusterService.isUp("02")).willReturn(false);
        given(mockClusterService.getServerSelectionListSize()).willReturn(2);
        given(mockClusterService.getServerSelection(0)).willReturn("01");

        SessionID mockSessionID = mock(SessionID.class);
        given(mockSessionID.getExtension(SessionID.PRIMARY_ID)).willReturn("02");
        given(mockSessionID.getSessionServerID()).willReturn("03");
        given(mockSessionID.getExtension(SessionID.STORAGE_KEY)).willReturn("4059025133086137527");

        // When
        MultiServerClusterMonitor clusterMonitor = new MultiServerClusterMonitor(
                mockSessionService, mockDebug, mockServiceConfig, mockServerConfig, mockFactory);
        String currentHostServer = clusterMonitor.getCurrentHostServer(mockSessionID);

        // Then
        assertThat(currentHostServer).isEqualTo("01");
    }



}
