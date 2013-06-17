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

package org.forgerock.openam.forgerockrest.authn;

import com.sun.identity.authentication.service.AMAuthErrorCode;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class AMAuthErrorCodeResponseStatusMappingTest {

    private AMAuthErrorCodeResponseStatusMapping amAuthErrorCodeResponseStatusMapping;

    @BeforeClass
    public void setUp() {
        amAuthErrorCodeResponseStatusMapping = new AMAuthErrorCodeResponseStatusMapping();
    }

    @Test
    public void shouldMapUnknownErrorCode() {

        //Given
        String errorCode = "UNKNOWN_ERROR_CODE";

        //When
        int httpStatusCode = amAuthErrorCodeResponseStatusMapping.getAuthLoginExceptionResponseStatus(errorCode);

        //Then
        assertEquals(httpStatusCode, 401);
    }

    @Test
    public void shouldMapAuthTimeoutErrorCode() {

        //Given
        String errorCode = AMAuthErrorCode.AUTH_TIMEOUT;

        //When
        int httpStatusCode = amAuthErrorCodeResponseStatusMapping.getAuthLoginExceptionResponseStatus(errorCode);

        //Then
        assertEquals(httpStatusCode, 408);
    }

    @Test
    public void shouldMapErrorCode() {

        //Given
        String errorCode = AMAuthErrorCode.AUTH_CONFIG_NOT_FOUND;

        //When
        int httpStatusCode = amAuthErrorCodeResponseStatusMapping.getAuthLoginExceptionResponseStatus(errorCode);

        //Then
        assertEquals(httpStatusCode, 400);
    }
}
