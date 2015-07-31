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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.user.invocation;

import org.forgerock.openam.sts.TokenMarshalException;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

public class RestSTSTokenCancellationInvocationStateTest {
    private static final String SAML2_ASSERTION = "<saml:Assertion xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" " +
            "ID=\"s2ad0847ea78329efb63a0f36b74efb80c2c3d4700\" IssueInstant=\"2015-07-14T21:37:06Z\" Version=\"2.0\"> ...</saml:Assertion>";
    private static final String OIDC_TOKEN = "dfds.ffs.ss";

    @Test
    public void testEquals() throws TokenMarshalException {
        SAML2TokenState saml2TokenState = SAML2TokenState.builder().tokenValue(SAML2_ASSERTION).build();
        RestSTSTokenCancellationInvocationState invocationState =
                RestSTSTokenCancellationInvocationState.builder().cancelledTokenState(saml2TokenState.toJson()).build();
        RestSTSTokenCancellationInvocationState invocationState2 =
                RestSTSTokenCancellationInvocationState.builder().cancelledTokenState(saml2TokenState.toJson()).build();
        assertEquals(invocationState, invocationState2);

        OpenIdConnectTokenState openIdConnectTokenState = OpenIdConnectTokenState.builder().tokenValue(OIDC_TOKEN).build();
        invocationState =
                RestSTSTokenCancellationInvocationState.builder().cancelledTokenState(openIdConnectTokenState.toJson()).build();
        invocationState2 =
                RestSTSTokenCancellationInvocationState.builder().cancelledTokenState(openIdConnectTokenState.toJson()).build();
        assertEquals(invocationState, invocationState2);

        invocationState =
                RestSTSTokenCancellationInvocationState.builder().cancelledTokenState(openIdConnectTokenState.toJson()).build();
        invocationState2 =
                RestSTSTokenCancellationInvocationState.builder().cancelledTokenState(saml2TokenState.toJson()).build();
        assertNotEquals(invocationState, invocationState2);
    }

    @Test
    public void testJsonRoundTrip() throws TokenMarshalException {
        SAML2TokenState saml2TokenState = SAML2TokenState.builder().tokenValue(SAML2_ASSERTION).build();
        RestSTSTokenCancellationInvocationState invocationState =
                RestSTSTokenCancellationInvocationState.builder().cancelledTokenState(saml2TokenState.toJson()).build();
        assertEquals(invocationState, RestSTSTokenCancellationInvocationState.fromJson(invocationState.toJson()));

        OpenIdConnectTokenState openIdConnectTokenState = OpenIdConnectTokenState.builder().tokenValue(OIDC_TOKEN).build();
        invocationState =
                RestSTSTokenCancellationInvocationState.builder().cancelledTokenState(openIdConnectTokenState.toJson()).build();
        assertEquals(invocationState, RestSTSTokenCancellationInvocationState.fromJson(invocationState.toJson()));
    }

}
