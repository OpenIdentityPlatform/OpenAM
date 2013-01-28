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
package org.forgerock.openam.forgerockrest.session;

import org.forgerock.json.resource.ResultHandler;
import org.forgerock.openam.forgerockrest.session.query.SessionQueryManager;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.mockito.BDDMockito.mock;
import static org.mockito.Mockito.*;

/**
 * @author robert.wapshott@forgerock.com
 */
public class SessionResourceTest {
    @Test
    public void shouldUseSessionQueryManagerForAllSessionsQuery() {
        // Given
        String badger = "badger";
        String weasel = "weasel";

        SessionQueryManager mockManager = mock(SessionQueryManager.class);
        ResultHandler mockHandler = mock(ResultHandler.class);

        SessionResource resource = spy(new SessionResource(mockManager));
        List<String> list = Arrays.asList(new String[]{badger, weasel});
        doReturn(list).when(resource).getAllServerIds();

        // When
        resource.readInstance(null, SessionResource.KEYWORD_ALL, null, mockHandler);

        // Then
        List<String> result = Arrays.asList(new String[]{badger, weasel});
        verify(mockManager, times(1)).getAllSessions(result);
    }

    @Test
    public void shouldQueryNamedServerInServerMode() {
        // Given
        String badger = "badger";

        SessionQueryManager mockManager = mock(SessionQueryManager.class);
        ResultHandler mockHandler = mock(ResultHandler.class);

        SessionResource resource = spy(new SessionResource(mockManager));

        // When
        resource.readInstance(null, badger, null, mockHandler);

        // Then
        verify(resource, times(0)).getAllServerIds();

        List<String> result = Arrays.asList(new String[]{badger});
        verify(mockManager, times(1)).getAllSessions(result);
    }
}
