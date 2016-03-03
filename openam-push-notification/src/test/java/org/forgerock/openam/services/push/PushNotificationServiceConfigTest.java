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
package org.forgerock.openam.services.push;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

public class PushNotificationServiceConfigTest {

    @Test
    public void shouldCreateValidConfig() throws PushNotificationException {

        //given
        PushNotificationServiceConfig.Builder builder = new PushNotificationServiceConfig.Builder()
            .withEndpoint("www.forgerock.org")
            .withApiKey("apiKey")
            .withSenderId("senderId");

        //when
        PushNotificationServiceConfig config = builder.build();

        //then
        assertThat(config).isNotNull();
        assertThat(config.getEndpoint()).isEqualTo("www.forgerock.org");
        assertThat(config.getApiKey()).isEqualTo("apiKey");
        assertThat(config.getSenderId()).isEqualTo("senderId");
    }

    @Test (expectedExceptions = PushNotificationException.class)
    public void shouldNotCreateConfigMissingEndpoint() throws PushNotificationException {

        //given
        PushNotificationServiceConfig.Builder builder = new PushNotificationServiceConfig.Builder()
                .withApiKey("apiKey")
                .withSenderId("senderId");

        //when
        builder.build();

        //then
    }

    @Test (expectedExceptions = PushNotificationException.class)
    public void shouldNotCreateConfigMissingSenderId() throws PushNotificationException {

        //given
        PushNotificationServiceConfig.Builder builder = new PushNotificationServiceConfig.Builder()
                .withApiKey("apiKey")
                .withEndpoint("www.forgerock.org");

        //when
        builder.build();

        //then
    }

    @Test (expectedExceptions = PushNotificationException.class)
    public void shouldNotCreateConfigMissingApiKey() throws PushNotificationException {

        //given
        PushNotificationServiceConfig.Builder builder = new PushNotificationServiceConfig.Builder()
                .withSenderId("senderId")
                .withEndpoint("www.forgerock.org");

        //when
        builder.build();

        //then
    }

}
