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

package org.forgerock.oauth2.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit test for {@link StatefulAccessToken}.
 *
 * @since 14.0.0
 */
public final class StatefulAccessTokenTest {

    private static final String CLIENT_1 = "client1";
    private static final String CLIENT_1_WITH_SPACE = "client 1";

    private StatefulAccessToken openAMAccessToken;

    @BeforeMethod
    public void setUp() throws Exception {
        openAMAccessToken = newStatefulAccessToken();
    }

    @Test
    public void testGetClientId() throws Exception {
        //Given
        openAMAccessToken.setClientId(CLIENT_1);

        //When
        String clientId = openAMAccessToken.getClientId();

        //Then
        assertThat(clientId).isEqualTo(CLIENT_1);
    }

    @Test
    public void testGetClientIdClientNameWithSpace() throws Exception {
        //Given
        openAMAccessToken.setClientId(CLIENT_1_WITH_SPACE);

        //When
        String clientId = openAMAccessToken.getClientId();

        //Then
        assertThat(clientId).isEqualTo(CLIENT_1_WITH_SPACE);
    }

    private StatefulAccessToken newStatefulAccessToken() {
        String id = "2dec6816-cf19-4207-8d88-818c809ea6cc";
        String authorizationCode = null;
        String resourceOwnerId = "demo";
        String client = CLIENT_1;
        String redirectUri = null;
        Set<String> scope = new HashSet<>(Arrays.asList("cn"));
        long expiryTime = 1234567L;
        RefreshToken refreshToken = null;
        String tokenName = "access_token";
        String grantType = "password";
        String nonce = null;
        String realm = "/";
        String claims = null;
        String auditTrackingId = "4ed857de-5d18-4afa-bc85-56991b0f8d3d";

        return new StatefulAccessToken(id, authorizationCode, resourceOwnerId, client, redirectUri,
                scope, expiryTime, refreshToken, tokenName, grantType, nonce, realm, claims, auditTrackingId);
    }

}
