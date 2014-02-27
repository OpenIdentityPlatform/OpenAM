/**
 * Copyright 2014 ForgeRock AS.
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
package com.iplanet.dpro.session.service;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.services.naming.ServerEntryNotFoundException;
import com.iplanet.services.naming.WebtopNamingQuery;
import com.sun.identity.shared.debug.Debug;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class WebtopNamingSiteUtilsTest {

    private WebtopNamingQuery mockQuery;
    private WebtopNamingSiteUtils utils;
    private Session mockSession;
    private SessionID mockSessionID;

    @BeforeMethod
    public void setUp() {
        mockQuery = mock(WebtopNamingQuery.class);
        utils = new WebtopNamingSiteUtils(mock(Debug.class), mockQuery);

        mockSession = mock(Session.class);
        mockSessionID = mock(SessionID.class);
        given(mockSession.getID()).willReturn(mockSessionID);
    }

    @Test
    public void shouldResolveServerToBePartOfSite() throws Exception {
        // Given
        String serverID = "SERVER";
        String siteID = "SITE";
        given(mockSessionID.getSessionServerID()).willReturn(serverID);
        given(mockQuery.getSiteID(eq(serverID))).willReturn(siteID);

        // When
        utils.getSiteNodes(mockSession);

        // Then
        verify(mockQuery).getSiteNodes(eq(siteID));
    }

    @Test
    public void shouldReturnServerIDIfServerNotInSite() {
        // Given
        String serverID = "SERVER";
        given(mockSessionID.getSessionServerID()).willReturn(serverID);
        given(mockQuery.getSiteID(eq(serverID))).willReturn(null);

        // When
        Set<String> result = utils.getSiteNodes(mockSession);

        // Then
        result.contains(serverID);
    }

    @Test (expectedExceptions = IllegalStateException.class)
    public void shouldReturnThrowRuntimeIfErrorQueryingWebtopNaming() throws Exception {
        // Given
        String serverID = "SERVER";
        String siteID = "SITE";
        given(mockSessionID.getSessionServerID()).willReturn(serverID);
        given(mockQuery.getSiteID(eq(serverID))).willReturn(siteID);
        given(mockQuery.getSiteNodes(anyString())).willThrow(new Exception());

        // When / Then
        utils.getSiteNodes(mockSession);
    }

    @Test
    public void shouldReturnURLsForServersInSite() throws ServerEntryNotFoundException {
        // Given
        String serverID = "SERVER";
        given(mockSessionID.getSessionServerID()).willReturn(serverID);
        given(mockQuery.getSiteID(eq(serverID))).willReturn(null);
        given(mockQuery.getServerFromID(anyString())).willReturn("http://localhost");

        // When
        Set<URL> result = utils.getSiteNodeURLs(mockSession);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test (expectedExceptions = IllegalStateException.class)
    public void shouldThrowRuntimeExceptionForInvalidURLFormat() throws ServerEntryNotFoundException {
        // Given
        String serverID = "SERVER";
        given(mockSessionID.getSessionServerID()).willReturn(serverID);
        given(mockQuery.getSiteID(eq(serverID))).willReturn(null);
        given(mockQuery.getServerFromID(anyString())).willReturn("badgerbadger...");

        // When / Then
        utils.getSiteNodeURLs(mockSession);
    }
}
