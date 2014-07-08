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
package org.forgerock.openam.scripting;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class StandardScriptEngineManagerTest {

    StandardScriptEngineManager testManager = new StandardScriptEngineManager();


    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testTimeoutCannotBeNegative() {
        //given

        //when
        testManager.configureTimeout(-1);

        //then - caught by exception
    }

    @Test
    public void testSettingContextFactorySwallowsStateException() {

        //given

        //when
        testManager.configureTimeout(10);
        testManager.configureTimeout(5);

        //then
        assertEquals(testManager.getTimeoutSeconds(), 5);
        assertEquals(testManager.getTimeoutMillis(), 5000);
    }

    @Test
    public void testPreconfiguredWithLanguages() {

        //given - these should be loaded automatically as we support them

        //when

        //then
        assertTrue(testManager.getEngineByName(SupportedScriptingLanguage.JAVASCRIPT_ENGINE_NAME) != null);
        assertTrue(testManager.getEngineByName(SupportedScriptingLanguage.GROOVY_ENGINE_NAME) != null);
    }

}
