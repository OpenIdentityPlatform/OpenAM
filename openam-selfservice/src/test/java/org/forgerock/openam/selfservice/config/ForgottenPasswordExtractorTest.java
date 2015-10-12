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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Unit test for {@link ForgottenPasswordExtractor}.
 *
 * @since 13.0.0
 */
public final class ForgottenPasswordExtractorTest {

    @Test
    public void createsValidConfigInstance() {
        // Given
        Map<String, Set<String>> consoleAttributes = new HashMap<>();

        consoleAttributes.put("forgerockRESTSecurityForgotPasswordEnabled", Collections.singleton("true"));
        consoleAttributes.put("forgerockRESTSecurityForgotPassConfirmationUrl", Collections.singleton("someurl"));
        consoleAttributes.put("forgerockRESTSecurityForgotPassTokenTTL", Collections.singleton("1234"));
        consoleAttributes.put("forgerockRESTSecurityForgotPassServiceConfigClass", Collections.singleton("someclass"));

        // When
        ConsoleConfigExtractor<ForgottenPasswordConsoleConfig> extractor = new ForgottenPasswordExtractor();
        ForgottenPasswordConsoleConfig config = extractor.extract(consoleAttributes);

        // Then
        assertThat(config.isEnabled()).isTrue();
        assertThat(config.getEmailUrl()).isEqualTo("someurl");
        assertThat(config.getTokenExpiry()).isEqualTo(1234L);
        assertThat(config.getConfigProviderClass()).isEqualTo("someclass");
    }

}