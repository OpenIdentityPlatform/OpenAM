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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.selfservice.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

/**
 * Unit test for {@link CommonConsoleConfig}.
 *
 * @since 13.0.0
 */
public final class CommonConsoleConfigTest {

    @Test
    public void successfullyCreatesInstance() {
        // When
        MockConfig config = new MockConfigBuilder()
                .setEnabled(true)
                .setConfigProviderClass("abc")
                .setTokenExpiry(123L)
                .build();

        // Then
        assertThat(config.isEnabled()).isTrue();
        assertThat(config.getConfigProviderClass()).isEqualTo("abc");
        assertThat(config.getTokenExpiry()).isEqualTo(123L);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void failsWithNullConfigProviderClass() {
        // When
        new MockConfigBuilder()
                .setEnabled(true)
                .setEmailUrl("someurl")
                .setTokenExpiry(123L)
                .build();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void failsWithNegativeTokenExpiry() {
        // When
        new MockConfigBuilder()
                .setEnabled(true)
                .setConfigProviderClass("abc")
                .setEmailUrl("someurl")
                .build();
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void failsWithNullEmailUrl() {
        // When
        new MockConfigBuilder()
                .setEnabled(true)
                .setConfigProviderClass("abc")
                .setTokenExpiry(123L)
                .setEmailVerificationEnabled(true)
                .build();
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void failsWithMissingCaptchaConfig() {
        // When
        new MockConfigBuilder()
                .setEnabled(true)
                .setConfigProviderClass("abc")
                .setTokenExpiry(123L)
                .setCaptchaEnabled(true)
                .build();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void failsWithMissingKbaConfig() {
        // When
        new MockConfigBuilder()
                .setEnabled(true)
                .setConfigProviderClass("abc")
                .setTokenExpiry(123L)
                .setKbaEnabled(true)
                .build();
    }

    private static final class MockConfig extends CommonConsoleConfig {

        protected MockConfig(MockConfigBuilder builder) {
            super(builder);
        }

    }

    private static final class MockConfigBuilder
            extends CommonConsoleConfig.Builder<MockConfig, MockConfigBuilder> {

        @Override
        MockConfigBuilder getThis() {
            return this;
        }

        @Override
        MockConfig internalBuild() {
            return new MockConfig(this);
        }

    }

}