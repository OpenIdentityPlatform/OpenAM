/**
 * Copyright 2013 ForgeRock, Inc.
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
package org.forgerock.openam.forgerockrest.session.query;

import com.iplanet.dpro.session.share.SessionInfo;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author robert.wapshott@forgerock.com
 */
public class SessionQueryManagerTest {
    @Test
    public void shouldRetrieveQueryTypeForAllServerIds() {
        // Given
        String badger = "Badger";
        String weasel = "Weasel";

        List<String> ids = new LinkedList<String>();
        ids.add(badger);
        ids.add(weasel);

        SessionQueryFactory mockFactory = mock(SessionQueryFactory.class);
        given(mockFactory.getSessionQueryType(anyString())).willReturn(mock(SessionQueryType.class));
        SessionQueryManager manager = new SessionQueryManager(mockFactory);

        // When
        manager.getAllSessions(ids);

        // Then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockFactory, times(2)).getSessionQueryType(captor.capture());
        List<String> capturedValues = captor.getAllValues();

        assertTrue(capturedValues.contains(badger));
        assertTrue(capturedValues.contains(weasel));
    }

    @Test
    public void shouldUseSessionQueryTypeProvidedByFactory() {
        // Given
        SessionQueryType mockQueryType = mock(SessionQueryType.class);
        SessionQueryFactory mockFactory = mock(SessionQueryFactory.class);
        given(mockFactory.getSessionQueryType(anyString())).willReturn(mockQueryType);

        SessionQueryManager manager = new SessionQueryManager(mockFactory);

        // When
        manager.getAllSessions(Arrays.asList(new String[]{"badger"}));

        // Then
        verify(mockQueryType, times(1)).getAllSessions();
    }

    @Test
    public void shouldReturnAllSessionsReturnedByQueryTypes() {
        // Given
        String badger = "Badger";
        String weasel = "Weasel";

        SessionInfo one = mock(SessionInfo.class);
        SessionInfo two = mock(SessionInfo.class);

        SessionQueryType typeOne = mock(SessionQueryType.class);
        given(typeOne.getAllSessions()).willReturn(Arrays.asList(new SessionInfo[]{one}));
        SessionQueryType typeTwo = mock(SessionQueryType.class);
        given(typeTwo.getAllSessions()).willReturn(Arrays.asList(new SessionInfo[]{two}));

        SessionQueryFactory mockFactory = mock(SessionQueryFactory.class);
        given(mockFactory.getSessionQueryType(badger)).willReturn(typeOne);
        given(mockFactory.getSessionQueryType(weasel)).willReturn(typeTwo);

        SessionQueryManager manager = new SessionQueryManager(mockFactory);

        // When
        Collection<SessionInfo> sessions = manager.getAllSessions(Arrays.asList(new String[]{badger, weasel}));

        // Then
        assertEquals(2, sessions.size());
        assertTrue(sessions.contains(one));
        assertTrue(sessions.contains(two));
    }
}