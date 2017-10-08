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
package org.forgerock.openam.oauth2.validation;

import static org.forgerock.json.JsonValue.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.forgerock.json.JsonException;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.openam.oauth2.OAuth2Utils;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit test for {@link ConfirmationKeyValidator}.
 *
 * @since 14.0.0
 */
public final class ConfirmationKeyValidatorTest {

    private ConfirmationKeyValidator validator;
    private OAuth2Request request;

    @Mock
    private OAuth2Utils utils;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        validator = new ConfirmationKeyValidator(utils);
        request = mock(OAuth2Request.class);
    }

    @Test
    public void whenNoCnfKeyParameterValidatorDoesntThrowException() throws Exception {
        when(utils.getConfirmationKey(request)).thenReturn(null);
        validator.validateRequest(request);
    }

    @Test(expectedExceptions = InvalidRequestException.class)
    public void whenCnfKeyIsNotParsableValidatorThrowsException() throws Exception {
        when(utils.getConfirmationKey(request)).thenThrow(new JsonException());
        validator.validateRequest(request);
    }

    @Test(expectedExceptions = InvalidRequestException.class)
    public void whenCnfKeyIsNotValidJWKValidatorThrowsException() throws Exception {
        when(utils.getConfirmationKey(request)).thenReturn(json(object(field("some-field", "some-value"))));
        validator.validateRequest(request);
    }

    @Test
    public void whenCnfKeyIsValidJWKValidatorDoesntThrowException() throws Exception {
        when(utils.getConfirmationKey(request)).thenReturn(json(object(field("jwk", object()))));
        validator.validateRequest(request);
    }
}