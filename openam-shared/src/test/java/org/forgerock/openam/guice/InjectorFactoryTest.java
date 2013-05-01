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

import com.google.inject.Injector;
import com.google.inject.Module;
import org.forgerock.openam.guice.test.BadModule;
import org.forgerock.openam.guice.test.TestModule1;
import org.forgerock.openam.guice.test.TestModule2;
import org.mockito.Matchers;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class InjectorFactoryTest {

    private InjectorFactory injectorFactory;

    private ClasspathScanner classpathScanner;
    private GuiceModuleCreator moduleCreator;
    private GuiceInjectorCreator injectorCreator;

    @BeforeClass
    public void setUp() {

        classpathScanner = mock(ClasspathScanner.class);
        moduleCreator = mock(GuiceModuleCreator.class);
        injectorCreator = mock(GuiceInjectorCreator.class);

        injectorFactory = new InjectorFactory(classpathScanner, moduleCreator, injectorCreator);
    }

    @Test
    public void shouldCreateInjector() {

        //Given
        Class<AMGuiceModule> moduleAnnotation = AMGuiceModule.class;

        Set<Class<?>> modules = new HashSet<Class<?>>(asList(TestModule1.class, TestModule2.class));
        given(classpathScanner.getTypesAnnotatedWith(moduleAnnotation)).willReturn(modules);

        TestModule1 testModule1 = mock(TestModule1.class);
        given(moduleCreator.createInstance(TestModule1.class)).willReturn(testModule1);

        TestModule2 testModule2 = mock(TestModule2.class);
        given(moduleCreator.createInstance(TestModule2.class)).willReturn(testModule2);

        Injector mockInjector = mock(Injector.class);
        given(injectorCreator.createInjector(Matchers.<Iterable<? extends Module>>anyObject()))
                .willReturn(mockInjector);

        //When
        Injector injector = injectorFactory.createInjector(moduleAnnotation);

        //Then
        assertEquals(injector, mockInjector);
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldFailToCreateInjectorIfModuleDoesNotImplementInterface() {

        //Given
        Class<AMGuiceModule> moduleAnnotation = AMGuiceModule.class;

        Set<Class<?>> modules = new HashSet<Class<?>>(asList(TestModule1.class, BadModule.class, TestModule2.class));
        given(classpathScanner.getTypesAnnotatedWith(moduleAnnotation)).willReturn(modules);

        //When
        injectorFactory.createInjector(moduleAnnotation);

        //Then
        fail();
    }
}
