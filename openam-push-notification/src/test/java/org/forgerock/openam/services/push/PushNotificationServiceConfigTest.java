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
            .withAppleEndpoint("www.apple.com")
            .withGoogleEndpoint("www.google.com")
            .withAccessKey("apiKey")
            .withSecret("secret")
            .withRegion("region")
            .withDelegateFactory("classname");

        //when
        PushNotificationServiceConfig config = builder.build();

        //then
        assertThat(config).isNotNull();
        assertThat(config.getAppleEndpoint()).isEqualTo("www.apple.com");
        assertThat(config.getGoogleEndpoint()).isEqualTo("www.google.com");
        assertThat(config.getAccessKey()).isEqualTo("apiKey");
        assertThat(config.getSecret()).isEqualTo("secret");
        assertThat(config.getDelegateFactory()).isEqualTo("classname");
        assertThat(config.getRegion()).isEqualTo("region");
    }

    @Test (expectedExceptions = PushNotificationException.class)
    public void shouldNotCreateConfigMissingGoogleEndpoint() throws PushNotificationException {

        //given
        PushNotificationServiceConfig.Builder builder = new PushNotificationServiceConfig.Builder()
                .withAccessKey("accessKey")
                .withAppleEndpoint("www.apple.com")
                .withSecret("secret")
                .withRegion("region")
                .withDelegateFactory("classname");

        //when
        builder.build();

        //then
    }

    @Test (expectedExceptions = PushNotificationException.class)
    public void shouldNotCreateConfigMissingAppleEndpoint() throws PushNotificationException {

        //given
        PushNotificationServiceConfig.Builder builder = new PushNotificationServiceConfig.Builder()
                .withAccessKey("accessKey")
                .withGoogleEndpoint("www.google.com")
                .withSecret("secret")
                .withRegion("region")
                .withDelegateFactory("classname");

        //when
        builder.build();

        //then
    }

    @Test (expectedExceptions = PushNotificationException.class)
    public void shouldNotCreateConfigMissingSecret() throws PushNotificationException {

        //given
        PushNotificationServiceConfig.Builder builder = new PushNotificationServiceConfig.Builder()
                .withAccessKey("accessKey")
                .withAppleEndpoint("www.apple.com")
                .withGoogleEndpoint("www.google.com")
                .withDelegateFactory("classname")
                .withRegion("region");

        //when
        builder.build();

        //then
    }

    @Test (expectedExceptions = PushNotificationException.class)
    public void shouldNotCreateConfigMissingAccessKey() throws PushNotificationException {

        //given
        PushNotificationServiceConfig.Builder builder = new PushNotificationServiceConfig.Builder()
                .withSecret("secret")
                .withGoogleEndpoint("www.google.com")
                .withAppleEndpoint("www.apple.com")
                .withDelegateFactory("classname")
                .withRegion("region");

        //when
        builder.build();

        //then
    }

    @Test (expectedExceptions = PushNotificationException.class)
    public void shouldNotCreateConfigMissingDelegateFactory() throws PushNotificationException {

        //given
        PushNotificationServiceConfig.Builder builder = new PushNotificationServiceConfig.Builder()
                .withAccessKey("accessKey")
                .withSecret("secret")
                .withGoogleEndpoint("www.google.com")
                .withAppleEndpoint("www.apple.com")
                .withRegion("region");

        //when
        builder.build();

        //then
    }

    @Test (expectedExceptions = PushNotificationException.class)
    public void shouldNotCreateDelegateMissingRegion() throws PushNotificationException {

        //given
        PushNotificationServiceConfig.Builder builder = new PushNotificationServiceConfig.Builder()
                .withAccessKey("accessKey")
                .withSecret("secret")
                .withGoogleEndpoint("www.google.com")
                .withAppleEndpoint("www.apple.com")
                .withDelegateFactory("classname");

        //when
        builder.build();

        //then
    }

}
