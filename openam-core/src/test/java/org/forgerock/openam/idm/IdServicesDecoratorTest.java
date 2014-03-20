/*
 * Copyright 2014 ForgeRock, AS.
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

package org.forgerock.openam.idm;

import com.sun.identity.idm.IdServices;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.NoInjection;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.Iterator;

import static org.forgerock.openam.idm.DecoratorTestUtils.generateArguments;
import static org.forgerock.openam.idm.DecoratorTestUtils.getDeclaredMethods;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class IdServicesDecoratorTest {

    private IdServices mockDelegate;
    private IdServicesDecorator decorator;

    @BeforeMethod
    public void setupMocks() {
        mockDelegate = mock(IdServices.class);
        decorator = new IdServicesDecorator(mockDelegate);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullDelegate() {
        new IdServicesDecorator(null);
    }

    /**
     * Returns all of the methods defined by the IdServices interface.
     */
    @DataProvider(name = "IdServicesMethods")
    public Iterator<Object[]> getIdServicesMethods() {
        return getDeclaredMethods(IdServices.class);
    }

    /**
     * Tests that calls to any method on the decorator result in the same method being invoked on the delegate with
     * the same arguments. Uses a data generator to supply the methods so that new methods are automatically tested.
     */
    @Test(dataProvider = "IdServicesMethods")
    public void shouldDelegateMethodCalls(@NoInjection Method interfaceMethod) throws Exception {
        // Given
        Object[] args = generateArguments(interfaceMethod);

        // When
        interfaceMethod.invoke(decorator, args);

        // Then
        interfaceMethod.invoke(verify(mockDelegate), args);
    }


}
