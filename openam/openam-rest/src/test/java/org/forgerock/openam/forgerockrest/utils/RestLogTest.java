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
package org.forgerock.openam.forgerockrest.utils;

import com.sun.identity.log.Logger;
import com.sun.identity.log.messageid.LogMessageProvider;
import com.sun.identity.shared.debug.Debug;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import javax.security.auth.Subject;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class RestLogTest {

    ServerContext mockContext = mock(ServerContext.class);
    Debug mockDebug;
    RestLog restLog;


    LogMessageProvider msgProvider = mock(LogMessageProvider.class);
    Logger accessLogger = mock(Logger.class);
    Logger authzLogger = mock(Logger.class);

    @BeforeMethod
    public void setup() { //you need this
        mockDebug = mock(Debug.class);
        restLog = new RestLog(msgProvider, accessLogger, authzLogger);
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldFailWithNoResource() {
        //given

        //when
        restLog.debugOperationAttemptAsPrincipal(null, "", mockContext, null, mockDebug);

        //then
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldFailWithNoOperation() {
        //given

        //when
        restLog.debugOperationAttemptAsPrincipal("", null, mockContext, null, mockDebug);

        //then
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldFailWithNoContext() {
        //given

        //when
        restLog.debugOperationAttemptAsPrincipal(null, "", mockContext, null, mockDebug);

        //then
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldFailWithNoDebug() {
        //given

        //when
        restLog.debugOperationAttemptAsPrincipal("", "", mockContext, null, null);

        //then
    }

    @Test
    public void shouldReturnNullWithNoPrincipalAndMessage() {
        //given
        SSOTokenContext tokenContext = generateTestSSOTokenContext(null);

        //when
        String principal = restLog.debugOperationAttemptAsPrincipal("", "", tokenContext, null, mockDebug);

        //then
        assertNull(principal);
        verify(mockDebug).message(anyString());

    }

    @Test
    public void shouldReturnPrincipalAndMessage() throws ResourceException {
        //given
        SSOTokenContext tokenContext = generateTestSSOTokenContext("test");

        //when
        String principal = restLog.debugOperationAttemptAsPrincipal("", "", tokenContext, null, mockDebug);

        //then
        assertEquals("test", principal);
        verify(mockDebug).message(anyString());
    }

    private SSOTokenContext generateTestSSOTokenContext(final String name) {
        SSOTokenContext tokenContext = mock(SSOTokenContext.class);
        HashSet <Principal> princes = new HashSet<Principal>();
        Principal p = new Principal() {
            @Override
            public String getName() {
                return name;
            }
        };
        princes.add(p);
        Subject subject = new Subject(false, princes, Collections.EMPTY_SET, Collections.EMPTY_SET);

        when(tokenContext.getCallerSubject()).thenReturn(subject);

        return tokenContext;
    }
}
