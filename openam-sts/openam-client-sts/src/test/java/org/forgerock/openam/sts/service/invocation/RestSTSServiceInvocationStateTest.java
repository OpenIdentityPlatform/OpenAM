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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.service.invocation;

import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.token.SAML2SubjectConfirmation;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class RestSTSServiceInvocationStateTest {
    private static final String USERNAME = "bobo";
    private static final String PASSWORD = "dodo";
    private static final String SESSION_ID = "AQIC5wM2LY4SfczNfYrVEX9Z0D3wB3T5TMCX8CFKzQOEi-s";
    private static final String TOKEN_VALUE = "eyJhb.eyJpc3MiOiJhY2N.SqcfMU-BsrS69tGLIFRq";

    @Test
    public void testRountTripWithUntAndSaml() throws TokenMarshalException {
        SAML2TokenState saml2TokenState =
                SAML2TokenState.builder()
                        .saml2SubjectConfirmation(SAML2SubjectConfirmation.BEARER)
                        .build();
        UsernameTokenState usernameTokenState =
                UsernameTokenState.builder().username(USERNAME.getBytes()).password(PASSWORD.getBytes()).build();
        RestSTSServiceInvocationState invocationState =
                RestSTSServiceInvocationState.builder()
                        .inputTokenState(usernameTokenState.toJson())
                        .outputTokenState(saml2TokenState.toJson())
                        .build();
        RestSTSServiceInvocationState roundTripInvocationState = RestSTSServiceInvocationState.fromJson(invocationState.toJson());
        assertEquals(usernameTokenState, UsernameTokenState.fromJson(roundTripInvocationState.getInputTokenState()));
        assertEquals(saml2TokenState, SAML2TokenState.fromJson(roundTripInvocationState.getOutputTokenState()));
    }

    @Test
    public void testRountTripWithOpenAMAndSaml() throws TokenMarshalException {
        SAML2TokenState saml2TokenState =
                SAML2TokenState.builder()
                        .saml2SubjectConfirmation(SAML2SubjectConfirmation.BEARER)
                        .build();
        OpenAMTokenState openAMTokenState = OpenAMTokenState.builder().sessionId(SESSION_ID).build();
        RestSTSServiceInvocationState invocationState =
                RestSTSServiceInvocationState.builder()
                        .inputTokenState(openAMTokenState.toJson())
                        .outputTokenState(saml2TokenState.toJson())
                        .build();
        RestSTSServiceInvocationState roundTripInvocationState = RestSTSServiceInvocationState.fromJson(invocationState.toJson());
        assertEquals(openAMTokenState, OpenAMTokenState.fromJson(roundTripInvocationState.getInputTokenState()));
        assertEquals(saml2TokenState, SAML2TokenState.fromJson(roundTripInvocationState.getOutputTokenState()));
    }

    @Test
    public void testRountTripWithOpenIdConnectAndSaml() throws TokenMarshalException {
        SAML2TokenState saml2TokenState =
                SAML2TokenState.builder()
                        .saml2SubjectConfirmation(SAML2SubjectConfirmation.BEARER)
                        .build();
        OpenIdConnectTokenState idConnectTokenState = OpenIdConnectTokenState.builder().tokenValue(TOKEN_VALUE).build();
        RestSTSServiceInvocationState invocationState =
                RestSTSServiceInvocationState.builder()
                        .inputTokenState(idConnectTokenState.toJson())
                        .outputTokenState(saml2TokenState.toJson())
                        .build();
        RestSTSServiceInvocationState roundTripInvocationState = RestSTSServiceInvocationState.fromJson(invocationState.toJson());
        assertEquals(idConnectTokenState, OpenIdConnectTokenState.fromJson(roundTripInvocationState.getInputTokenState()));
        assertEquals(saml2TokenState, SAML2TokenState.fromJson(roundTripInvocationState.getOutputTokenState()));
    }
}
