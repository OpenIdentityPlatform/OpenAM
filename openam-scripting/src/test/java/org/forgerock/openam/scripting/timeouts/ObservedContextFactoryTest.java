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

import org.forgerock.openam.scripting.ScriptEngineConfiguration;
import org.forgerock.openam.scripting.StandardScriptEngineManager;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

public class ObservedContextFactoryTest {

    private StandardScriptEngineManager scriptEngineManager;
    private ObservedContextFactory testContextFactory;

    @BeforeMethod
    public void setupTests() {
        scriptEngineManager = new StandardScriptEngineManager();
        testContextFactory = new ObservedContextFactory(scriptEngineManager);
    }

    @Test(expectedExceptions = Error.class)
    public void testThrowsErrorWhenInterrupted() {

        //given
        ObservedContextFactory.ObservedJavaScriptContext mockContext
                = mock(ObservedContextFactory.ObservedJavaScriptContext.class);

        int instructionCount = 0;
        setTimeout(1);
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

        setTimeout(0);
        //when
        testContextFactory.observeInstructionCount(mockContext, instructionCount);

        //then
    }

    private void setTimeout(int timeout) {
        scriptEngineManager.setConfiguration(ScriptEngineConfiguration.builder()
                .withTimeout(timeout, TimeUnit.SECONDS).build());
    }
}
