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

package org.forgerock.openam.session.service.access.persistence;

import static org.mockito.BDDMockito.*;
import static org.mockito.BDDMockito.inOrder;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

import java.util.Arrays;

import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;

public class InternalSessionStoreChainTest {

    @Mock
    InternalSessionStore mainStore;

    InternalSessionStoreChain testedChain;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldCallAllElementsInChain() throws Exception {
        InternalSessionStoreStep stepA = setupMock(true);
        InternalSessionStoreStep stepB = setupMock(true);
        InternalSessionStoreStep stepC = setupMock(true);

        InternalSessionStoreChain chain = new InternalSessionStoreChain(Arrays.asList(stepA, stepB, stepC), mainStore);
        SessionID sessionId = mock(SessionID.class);

        chain.getBySessionID(sessionId);

        verify(stepA).getBySessionID(eq(sessionId), any(InternalSessionStore.class));
        verify(stepB).getBySessionID(eq(sessionId), any(InternalSessionStore.class));
        verify(stepC).getBySessionID(eq(sessionId), any(InternalSessionStore.class));
        verify(mainStore).getBySessionID(eq(sessionId));
    }

    @Test
    public void shouldStopCallingIfNextNotCalled() throws Exception {
        InternalSessionStoreStep stepA = setupMock(true);
        InternalSessionStoreStep stepB = setupMock(false);
        InternalSessionStoreStep stepC = setupMock(true);

        InternalSessionStoreChain chain = new InternalSessionStoreChain(Arrays.asList(stepA, stepB, stepC), mainStore);
        SessionID sessionId = mock(SessionID.class);

        chain.getBySessionID(sessionId);

        verify(stepA).getBySessionID(eq(sessionId), any(InternalSessionStore.class));
        verify(stepB).getBySessionID(eq(sessionId), any(InternalSessionStore.class));
        verify(stepC, times(0)).getBySessionID(eq(sessionId), any(InternalSessionStore.class));
        verify(mainStore, times(0)).getBySessionID(eq(sessionId));
    }

    @Test
    public void shouldCallElementsInCorrectOrder() throws Exception {

        InternalSessionStoreStep stepA = setupMock(true);
        InternalSessionStoreStep stepB = setupMock(true);
        InternalSessionStoreStep stepC = setupMock(true);
        SessionID sessionId = mock(SessionID.class);

        InternalSessionStoreChain chain = new InternalSessionStoreChain(Arrays.asList(stepA, stepB, stepC), mainStore);

        chain.getBySessionID(sessionId);

        InOrder inOrder = inOrder(stepA, stepB, stepC, mainStore);
        inOrder.verify(stepA).getBySessionID(eq(sessionId), any(InternalSessionStore.class));
        inOrder.verify(stepB).getBySessionID(eq(sessionId), any(InternalSessionStore.class));
        inOrder.verify(stepC).getBySessionID(eq(sessionId), any(InternalSessionStore.class));
        inOrder.verify(mainStore).getBySessionID(eq(sessionId));
    }

    private InternalSessionStoreStep setupMock(boolean willCallNext) throws Exception {
        InternalSessionStoreStep step = mock(InternalSessionStoreStep.class);
        if (willCallNext) {
            given(step.getBySessionID(any(SessionID.class), any(InternalSessionStore.class))).willAnswer(new Answer<InternalSession>() {
                @Override
                public InternalSession answer(InvocationOnMock invocationOnMock) throws Throwable {
                    return ((InternalSessionStore) invocationOnMock.getArguments()[1]).getBySessionID((SessionID) invocationOnMock.getArguments()[0]);
                }
            });
        }
        return step;
    }


}
