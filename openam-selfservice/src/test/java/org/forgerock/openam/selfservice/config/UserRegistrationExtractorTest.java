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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Unit test for {@link UserRegistrationExtractor}.
 *
 * @since 13.0.0
 */
public final class UserRegistrationExtractorTest {

    @Test
    public void createsValidConfigInstance() {
        // Given
        Map<String, Set<String>> consoleAttributes = new HashMap<>();

        Set<String> subjectTranslations = new HashSet<>();
        subjectTranslations.add("it|Italian");
        subjectTranslations.add("fr|French");

        Set<String> messageTranslations = new HashSet<>();
        messageTranslations.add("da|Danish");
        messageTranslations.add("es|Spanish");

        consoleAttributes.put("forgerockRESTSecuritySelfRegistrationEnabled", Collections.singleton("true"));
        consoleAttributes.put("forgerockRESTSecuritySelfRegEmailVerificationEnabled", Collections.singleton("true"));
        consoleAttributes.put("forgerockRESTSecuritySelfRegConfirmationUrl", Collections.singleton("someurl"));
        consoleAttributes.put("forgerockRESTSecuritySelfRegTokenTTL", Collections.singleton("1234"));
        consoleAttributes.put("forgerockRESTSecuritySelfRegServiceConfigClass", Collections.singleton("someclass"));
        consoleAttributes.put("forgerockRESTSecuritySelfRegKbaEnabled", Collections.singleton("true"));
        consoleAttributes.put("forgerockRESTSecurityKBAQuestions", Collections.singleton("123|en|abc"));
        consoleAttributes.put("forgerockRESTSecurityAnswersUserMustProvide", Collections.singleton("3"));
        consoleAttributes.put("forgerockRESTSecuritySelfRegCaptchaEnabled", Collections.singleton("true"));
        consoleAttributes.put("forgerockRESTSecurityCaptchaSiteKey", Collections.singleton("someKey"));
        consoleAttributes.put("forgerockRESTSecurityCaptchaSecretKey", Collections.singleton("someSecret"));
        consoleAttributes.put("forgerockRESTSecurityCaptchaVerificationUrl", Collections.singleton("someUrl"));
        consoleAttributes.put("forgerockRESTSecuritySelfRegSubjectText", subjectTranslations);
        consoleAttributes.put("forgerockRESTSecuritySelfRegEmailText", messageTranslations);

        // When
        ConsoleConfigExtractor<UserRegistrationConsoleConfig> extractor = new UserRegistrationExtractor();
        UserRegistrationConsoleConfig config = extractor.extract(consoleAttributes);

        // Then
        assertThat(config.isEnabled()).isTrue();
        assertThat(config.isEmailVerificationEnabled()).isTrue();
        assertThat(config.getEmailUrl()).isEqualTo("someurl");
        assertThat(config.getTokenExpiry()).isEqualTo(1234L);
        assertThat(config.getConfigProviderClass()).isEqualTo("someclass");
        assertThat(config.isKbaEnabled()).isTrue();
        assertThat(config.getSecurityQuestions()).containsEntry("123", Collections.singletonMap("en", "abc"));
        assertThat(config.getMinAnswersToProvide()).isEqualTo(3);
        assertThat(config.isCaptchaEnabled()).isTrue();
        assertThat(config.getCaptchaSiteKey()).isEqualTo("someKey");
        assertThat(config.getCaptchaSecretKey()).isEqualTo("someSecret");
        assertThat(config.getCaptchaVerificationUrl()).isEqualTo("someUrl");
        assertThat(config.getSubjectTranslations()).hasSize(2);
        assertThat(config.getSubjectTranslations()).containsKey(Locale.ITALIAN);
        assertThat(config.getSubjectTranslations()).containsKey(Locale.FRENCH);
        assertThat(config.getMessageTranslations()).hasSize(2);
        assertThat(config.getMessageTranslations()).containsKey(Locale.forLanguageTag("da"));
        assertThat(config.getMessageTranslations()).containsKey(Locale.forLanguageTag("es"));
    }
}
