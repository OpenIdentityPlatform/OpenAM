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

package org.forgerock.openam.selfservice.config.flows;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.forgerock.openam.selfservice.config.ConsoleConfigExtractor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Unit test for {@link ForgottenUsernameExtractor}.
 *
 * @since 13.0.0
 */
public final class ForgottenUsernameExtractorTest {

    @Mock
    private ConsoleConfigExtractor<KbaConsoleConfig> kbaExtractor;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void createsValidConfigInstance() {
        // Given
        Map<String, Set<String>> consoleAttributes = new HashMap<>();

        consoleAttributes.put("forgerockRESTSecurityForgotUsernameEnabled", Collections.singleton("true"));
        consoleAttributes.put("forgerockRESTSecurityForgotUsernameEmailUsernameEnabled", Collections.singleton("true"));
        consoleAttributes.put("forgerockRESTSecurityForgotUsernameShowUsernameEnabled", Collections.singleton("true"));
        consoleAttributes.put("forgerockRESTSecurityForgotUsernameConfirmationUrl", Collections.singleton("someurl"));
        consoleAttributes.put("forgerockRESTSecurityForgotUsernameTokenTTL", Collections.singleton("1234"));
        consoleAttributes.put("forgerockRESTSecurityForgotUsernameServiceConfigClass", Collections.singleton("someclass"));
        consoleAttributes.put("forgerockRESTSecurityForgotUsernameKbaEnabled", Collections.singleton("true"));
        consoleAttributes.put("forgerockRESTSecurityForgotUsernameCaptchaEnabled", Collections.singleton("true"));
        consoleAttributes.put("forgerockRESTSecurityCaptchaSiteKey", Collections.singleton("someKey"));
        consoleAttributes.put("forgerockRESTSecurityCaptchaSecretKey", Collections.singleton("someSecret"));
        consoleAttributes.put("forgerockRESTSecurityCaptchaVerificationUrl", Collections.singleton("someUrl"));
        consoleAttributes.put("forgerockRESTSecurityForgotUsernameEmailSubject", Collections.singleton("en|The Subject!"));
        consoleAttributes.put("forgerockRESTSecurityForgotUsernameEmailBody", Collections.singleton("de|Hallo Welt!"));

        KbaConsoleConfig kbaConsoleConfig = KbaConsoleConfig
                .newBuilder()
                .setSecurityQuestions(singletonMap("123", singletonMap("en", "abc")))
                .setMinimumAnswersToDefine(5)
                .setMinimumAnswersToVerify(3)
                .build();

        given(kbaExtractor.extract(consoleAttributes)).willReturn(kbaConsoleConfig);

        // When
        ConsoleConfigExtractor<ForgottenUsernameConsoleConfig> extractor = new ForgottenUsernameExtractor(kbaExtractor);
        ForgottenUsernameConsoleConfig config = extractor.extract(consoleAttributes);

        // Then
        assertThat(config.isEnabled()).isTrue();
        assertThat(config.isEmailEnabled()).isTrue();
        assertThat(config.isShowUsernameEnabled()).isTrue();
        assertThat(config.getTokenExpiry()).isEqualTo(1234L);
        assertThat(config.getConfigProviderClass()).isEqualTo("someclass");
        assertThat(config.isKbaEnabled()).isTrue();
        assertThat(config.getSecurityQuestions()).containsEntry("123", Collections.singletonMap("en", "abc"));
        assertThat(config.getMinimumAnswersToVerify()).isEqualTo(3);
        assertThat(config.isCaptchaEnabled()).isTrue();
        assertThat(config.getCaptchaSiteKey()).isEqualTo("someKey");
        assertThat(config.getCaptchaSecretKey()).isEqualTo("someSecret");
        assertThat(config.getCaptchaVerificationUrl()).isEqualTo("someUrl");
        assertThat(config.getSubjectTranslations()).containsOnlyKeys(Locale.ENGLISH);
        assertThat(config.getSubjectTranslations()).containsValues("The Subject!");
        assertThat(config.getMessageTranslations()).containsOnlyKeys(Locale.GERMAN);
        assertThat(config.getMessageTranslations()).containsValues("Hallo Welt!");
    }

}
