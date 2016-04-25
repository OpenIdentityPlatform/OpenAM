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
package org.forgerock.openam.services.push.sns;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.builders.JwtClaimsSetBuilder;
import org.forgerock.json.jose.builders.SignedJwtBuilderImpl;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SigningManager;
import org.testng.annotations.Test;

public class SnsRegistrationPredicateTest {


    AmazonSNSClient mockClient = mock(AmazonSNSClient.class);
    SnsRegistrationPredicate predicate = new SnsRegistrationPredicate(mockClient, "appleEndpoint", "googleEndpoint");

    @Test
    public void shouldSucceedPredicate() {

        //given
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

        JsonValue testData = json(object(field("jwt", jwt)));

        //when
        predicate.perform(testData);

        //then
        assertThat(testData.get("deviceId")).isString().contains("deviceId");
        assertThat(testData.get("communicationType")).isString().contains("communicationType");
        assertThat(testData.get("mechanismUid")).isString().contains("mechanismUid");
        assertThat(testData.get("deviceType")).isString().contains("deviceType");
    }


}
