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

import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.IdCachedServices;
import com.sun.identity.idm.IdType;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;

public class LowerCaseIdCachedServicesDecoratorTest extends LowerCaseIdServicesDecoratorTest {
    private static final String NAME = "some name";
    private static final SSOToken TOKEN = mock(SSOToken.class);
    private static final IdType ID_TYPE = IdType.AGENT;
    private static final String AM_ORG_NAME = "amOrgName";
    private static final String AM_SDK_DN = "amSDKDN";
    private static final String KEY = "A Mixed Case Key";
    private static final String VALUE = "Some Value";

    private IdCachedServices mockDelegate;
    private LowerCaseIdCachedServicesDecorator decorator;

    @BeforeMethod
    public void setupMocks() {
        mockDelegate = mock(IdCachedServices.class);
        decorator = new LowerCaseIdCachedServicesDecorator(mockDelegate);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullDelegate() {
        new LowerCaseIdCachedServicesDecorator(null);
    }

    @Test
    public void shouldReturnLowerCaseAttributeNames() throws Exception {
        // Given
        Set attrNames = Collections.emptySet();
        boolean isString = true;

        given(mockDelegate.getAttributes(TOKEN, ID_TYPE, NAME, attrNames, AM_ORG_NAME, AM_SDK_DN, isString))
                .willReturn(Collections.singletonMap(KEY, VALUE));

        // When
        Map result = decorator.getAttributes(TOKEN, ID_TYPE, NAME, attrNames, AM_ORG_NAME, AM_SDK_DN, isString);

        // Then
        assertEquals(result, Collections.singletonMap(KEY.toLowerCase(), VALUE));
    }

    @Test
    public void shouldReturnLowerCaseAttributeNamesWithoutIsString() throws Exception {
        // Given
        given(mockDelegate.getAttributes(TOKEN, ID_TYPE, NAME, AM_ORG_NAME, AM_SDK_DN))
                .willReturn(Collections.singletonMap(KEY, VALUE));

        // When
        Map result = decorator.getAttributes(TOKEN, ID_TYPE, NAME, AM_ORG_NAME, AM_SDK_DN);

        // Then
        assertEquals(result, Collections.singletonMap(KEY.toLowerCase(), VALUE));
    }
}
