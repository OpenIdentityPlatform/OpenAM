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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.openam.guice;

import com.google.inject.Module;
import org.forgerock.openam.guice.test.TestModule1;
import org.forgerock.openam.guice.test.TestModule2;
import org.forgerock.openam.guice.test.TestModule3;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

public class GuiceModuleCreatorTest {

    private GuiceModuleCreator guiceModuleCreator;

    @BeforeClass
    public void setUp() {
        guiceModuleCreator = new GuiceModuleCreator();
    }

    @Test
    public void shouldCreateInstanceWithPublicNoArgConstructor() {

        //Given
        Class<? extends Module> moduleClass = TestModule2.class;

        //When
        Object module = guiceModuleCreator.createInstance(moduleClass);

        //Then
        assertNotNull(module);
    }

    @Test (expectedExceptions = ModuleCreationException.class)
    public void shouldFailToCreateInstanceWithPrivateNoArgConstructor() {

        //Given
        Class<? extends Module> moduleClass = TestModule1.class;

        //When
        guiceModuleCreator.createInstance(moduleClass);

        //Then
        fail();
    }

    @Test (expectedExceptions = ModuleCreationException.class)
    public void shouldFailToCreateInstanceWithPublicArgsConstructor() {

        //Given
        Class<? extends Module> moduleClass = TestModule3.class;

        //When
        guiceModuleCreator.createInstance(moduleClass);

        //Then
        fail();
    }
}
