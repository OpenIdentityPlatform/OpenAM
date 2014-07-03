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
package org.forgerock.openam.scripting.timeouts;

import org.forgerock.openam.scripting.StandardScriptEngineManager;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mozilla.javascript.Context;
import org.testng.annotations.Test;

public class ObservedContextFactoryTest {

    private StandardScriptEngineManager mockScriptEngineManager = mock(StandardScriptEngineManager.class);
    private ObservedContextFactory testContextFactory = new ObservedContextFactory(mockScriptEngineManager);

    @Test(expectedExceptions = Error.class)
    public void testThrowsErrorWhenInterrupted() {

        //given
        ObservedContextFactory.ObservedJavaScriptContext mockContext
                = mock(ObservedContextFactory.ObservedJavaScriptContext.class);

        int instructionCount = 0;

        when(mockScriptEngineManager.getTimeoutMillis()).thenReturn(1l);
        when(mockContext.getStartTime()).thenReturn(1l);

        //when
        testContextFactory.observeInstructionCount(mockContext, instructionCount);

        //then

    }

    @Test
    public void testTimeoutDisabledNoInterrupt() {

        //given
        ObservedContextFactory.ObservedJavaScriptContext mockContext
                = mock(ObservedContextFactory.ObservedJavaScriptContext.class);
        int instructionCount = 0;

        when(mockScriptEngineManager.getTimeoutMillis()).thenReturn(0l);

        //when
        testContextFactory.observeInstructionCount(mockContext, instructionCount);

        //then
    }

}
