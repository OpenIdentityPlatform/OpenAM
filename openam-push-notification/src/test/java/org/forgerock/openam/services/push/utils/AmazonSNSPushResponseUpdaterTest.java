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
package org.forgerock.openam.services.push.utils;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.*;
import static org.mockito.BDDMockito.anyObject;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.builders.JwtClaimsSetBuilder;
import org.forgerock.json.jose.builders.SignedJwtBuilderImpl;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.openam.services.push.PushNotificationException;
import org.forgerock.openam.services.push.PushNotificationServiceConfig;
import org.forgerock.openam.services.push.sns.utils.SnsClientFactory;
import org.forgerock.openam.services.push.sns.utils.SnsPushResponseUpdater;
import org.testng.annotations.Test;

public class AmazonSNSPushResponseUpdaterTest {

    SnsClientFactory mockClientFactory = mock(SnsClientFactory.class);
    AmazonSNSClient mockClient = mock(AmazonSNSClient.class);
    PushNotificationServiceConfig config = createConfig();

    SnsPushResponseUpdater responseUpdater = new SnsPushResponseUpdater(mockClientFactory);

    @Test
    public void shouldSucceedWithValidInput() {

        //given
        given(mockClientFactory.produce(config)).willReturn(mockClient);

        CreatePlatformEndpointResult endpointResult = new CreatePlatformEndpointResult();
        endpointResult.setEndpointArn("endpointArn");

        given(mockClient.createPlatformEndpoint((CreatePlatformEndpointRequest) anyObject()))
                .willReturn(endpointResult);

        JwtClaimsSetBuilder jwtClaimsSetBuilder = new JwtClaimsSetBuilder()
                .claim("communicationType", "communicationType")
                .claim("deviceId", "deviceId")
                .claim("mechanismUid", "mechanismUid")
                .claim("deviceType", "deviceType");

        String jwt = new SignedJwtBuilderImpl(new SigningManager()
                .newNopSigningHandler())
                .claims(jwtClaimsSetBuilder.build())
                .headers().alg(JwsAlgorithm.NONE).done().build();

        JsonValue content = json(object(field("jwt", jwt)));

        //when
        boolean result = responseUpdater.updateResponse(config, content);

        //then
        org.assertj.core.api.Assertions.assertThat(result).isTrue();

        assertThat(content.get("deviceId")).isString().contains("deviceId");
        assertThat(content.get("communicationType")).isString().contains("communicationType");
        assertThat(content.get("mechanismUid")).isString().contains("mechanismUid");
        assertThat(content.get("deviceType")).isString().contains("deviceType");
        assertThat(content.get("communicationId")).isString().contains("endpointArn");
    }


    @Test
    public void shouldFailWithInvalidAmazonResponse() {

        //given
        given(mockClientFactory.produce(config)).willReturn(mockClient);

        CreatePlatformEndpointResult endpointResult = new CreatePlatformEndpointResult();

        given(mockClient.createPlatformEndpoint((CreatePlatformEndpointRequest) anyObject()))
                .willReturn(endpointResult);

        JwtClaimsSetBuilder jwtClaimsSetBuilder = new JwtClaimsSetBuilder()
                .claim("communicationType", "communicationType")
                .claim("deviceId", "deviceId")
                .claim("mechanismUid", "mechanismUid")
                .claim("deviceType", "deviceType");

        String jwt = new SignedJwtBuilderImpl(new SigningManager()
                .newNopSigningHandler())
                .claims(jwtClaimsSetBuilder.build())
                .headers().alg(JwsAlgorithm.NONE).done().build();

        JsonValue content = json(object(field("jwt", jwt)));

        //when
        boolean result = responseUpdater.updateResponse(config, content);

        //then
        org.assertj.core.api.Assertions.assertThat(result).isFalse();
    }

    @Test
    public void shouldFailWhenNoJwtInContent() {
        //given
        mockClientFactory.produce(config);
        JsonValue content = JsonValue.json(object(field("", "")));

        //when
        boolean result = responseUpdater.updateResponse(config, content);

        //then
        org.assertj.core.api.Assertions.assertThat(result).isFalse();
    }

    private PushNotificationServiceConfig createConfig() {
        PushNotificationServiceConfig config = null;

        try {
            config = new PushNotificationServiceConfig
                    .Builder().withDelegateFactory("delegateFactory")
                    .withSecret("secret")
                    .withAppleEndpoint("appleEndpoint")
                    .withGoogleEndpoint("googleEndpoint")
                    .withRegion("us_west_1")
                    .withAccessKey("accessKey")
                    .build();
        } catch (PushNotificationException e) {
            //doesn't happen
        }

        return config;
    }


}
