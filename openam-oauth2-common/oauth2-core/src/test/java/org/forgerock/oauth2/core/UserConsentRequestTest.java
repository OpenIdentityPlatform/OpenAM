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

package org.forgerock.oauth2.core;

import org.testng.annotations.Test;

import java.util.Collections;

import static org.forgerock.oauth2.core.UserConsentResponse.UserConsentResponseBuilder;
import static org.forgerock.oauth2.core.UserConsentResponse.createUserConsentResponse;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * @since 12.0.0
 */
public class UserConsentRequestTest {

    @Test
    public void shouldCreateUserConsentResponseWhenAllParametersSet() {

        //Given
        final UserConsentResponseBuilder builder = createUserConsentResponse();

        builder.consentGiven(true);
        builder.saveConsent(true);
        builder.scope("SCOPE");
        builder.state("STATE");
        builder.nonce("NONCE");
        builder.responseType("RESPONSE_TYPE");
        builder.redirectUri("REDIRECT_URI");
        builder.clientId("CLIENT_ID");
        builder.context(Collections.<String, Object>emptyMap());

        //When
        final UserConsentResponse userConsentResponse = builder.build();

        //Then
        assertNotNull(userConsentResponse);
    }

    @Test
    public void shouldGetSetAuthorizationCodeAccessTokenRequestParameters() {

        //Given
        final UserConsentResponseBuilder builder = createUserConsentResponse();

        builder.consentGiven(true);
        builder.saveConsent(true);
        builder.scope("SCOPE");
        builder.state("STATE");
        builder.nonce("NONCE");
        builder.responseType("RESPONSE_TYPE");
        builder.redirectUri("REDIRECT_URI");
        builder.clientId("CLIENT_ID");
        builder.context(Collections.<String, Object>emptyMap());

        //When
        final UserConsentResponse userConsentResponse = builder.build();

        //Then
        assertTrue(userConsentResponse.isConsentGiven());
        assertTrue(userConsentResponse.isSaveConsent());
        assertEquals(userConsentResponse.getScope(), Collections.singleton("SCOPE"));
        assertEquals(userConsentResponse.getState(), "STATE");
        assertEquals(userConsentResponse.getNonce(), "NONCE");
        assertEquals(userConsentResponse.getResponseType(), Collections.singleton("RESPONSE_TYPE"));
        assertEquals(userConsentResponse.getRedirectUri(), "REDIRECT_URI");
        assertEquals(userConsentResponse.getClientId(), "CLIENT_ID");
        assertEquals(userConsentResponse.getContext(), Collections.<String, Object>emptyMap());
    }
}
