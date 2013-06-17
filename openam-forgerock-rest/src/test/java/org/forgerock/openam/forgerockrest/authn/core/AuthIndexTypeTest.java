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

package org.forgerock.openam.forgerockrest.authn.core;

import com.sun.identity.authentication.AuthContext;
import org.testng.annotations.Test;

import static org.junit.Assert.assertNull;
import static org.testng.Assert.assertEquals;

public class AuthIndexTypeTest {

    @Test
    public void shouldGetAuthIndexTypeNoneFromNullString() {

        //Given

        //When
        AuthIndexType authIndexType = AuthIndexType.getAuthIndexType(null);

        //Then
        assertEquals(authIndexType, AuthIndexType.NONE);
    }

    @Test
    public void shouldGetAuthIndexTypeFromEnumString() {

        //Given
        String enumString = AuthIndexType.COMPOSITE.toString();

        //When
        AuthIndexType authIndexType = AuthIndexType.getAuthIndexType(enumString);

        //Then
        assertEquals(authIndexType, AuthIndexType.COMPOSITE);
    }

    @Test
    public void shouldGetAuthIndexTypeFromAuthContextIndexTypeString() {

        //Given
        String authContextIndexTypeString = AuthContext.IndexType.MODULE_INSTANCE.toString();

        //When
        AuthIndexType authIndexType = AuthIndexType.getAuthIndexType(authContextIndexTypeString);

        //Then
        assertEquals(authIndexType, AuthIndexType.MODULE);
    }

    @Test
    public void shouldReturnNullForAuthIndexTypeNone() {

        //Given
        AuthIndexType authIndexType = AuthIndexType.NONE;

        //When
        AuthContext.IndexType authContextIndexType = authIndexType.getIndexType();

        //Then
        assertNull(authContextIndexType);
    }

    @Test
    public void shouldReturnAuthContextIndexTypeUserForAuthIndexTypeUser() {

        //Given
        AuthIndexType authIndexType = AuthIndexType.USER;

        //When
        AuthContext.IndexType authContextIndexType = authIndexType.getIndexType();

        //Then
        assertEquals(authContextIndexType, AuthContext.IndexType.USER);
    }

    @Test
    public void shouldReturnAuthContextIndexTypeRoleForAuthIndexTypeRole() {

        //Given
        AuthIndexType authIndexType = AuthIndexType.ROLE;

        //When
        AuthContext.IndexType authContextIndexType = authIndexType.getIndexType();

        //Then
        assertEquals(authContextIndexType, AuthContext.IndexType.ROLE);
    }

    @Test
    public void shouldReturnAuthContextIndexTypeServiceForAuthIndexTypeService() {

        //Given
        AuthIndexType authIndexType = AuthIndexType.SERVICE;

        //When
        AuthContext.IndexType authContextIndexType = authIndexType.getIndexType();

        //Then
        assertEquals(authContextIndexType, AuthContext.IndexType.SERVICE);
    }

    @Test
    public void shouldReturnAuthContextIndexTypeLevelForAuthIndexTypeLevel() {

        //Given
        AuthIndexType authIndexType = AuthIndexType.LEVEL;

        //When
        AuthContext.IndexType authContextIndexType = authIndexType.getIndexType();

        //Then
        assertEquals(authContextIndexType, AuthContext.IndexType.LEVEL);
    }

    @Test
    public void shouldReturnAuthContextIndexTypeModuleInstanceForAuthIndexTypeModule() {

        //Given
        AuthIndexType authIndexType = AuthIndexType.MODULE;

        //When
        AuthContext.IndexType authContextIndexType = authIndexType.getIndexType();

        //Then
        assertEquals(authContextIndexType, AuthContext.IndexType.MODULE_INSTANCE);
    }

    @Test
    public void shouldReturnAuthContextIndexTypeResourceForAuthIndexTypeResource() {

        //Given
        AuthIndexType authIndexType = AuthIndexType.RESOURCE;

        //When
        AuthContext.IndexType authContextIndexType = authIndexType.getIndexType();

        //Then
        assertEquals(authContextIndexType, AuthContext.IndexType.RESOURCE);
    }

    @Test
    public void shouldReturnAuthContextIndexTypeCompositeAdviceForAuthIndexTypeComposite() {

        //Given
        AuthIndexType authIndexType = AuthIndexType.COMPOSITE;

        //When
        AuthContext.IndexType authContextIndexType = authIndexType.getIndexType();

        //Then
        assertEquals(authContextIndexType, AuthContext.IndexType.COMPOSITE_ADVICE);
    }
}
