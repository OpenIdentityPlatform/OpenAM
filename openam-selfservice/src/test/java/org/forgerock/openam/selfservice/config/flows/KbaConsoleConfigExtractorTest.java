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

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.utils.CollectionUtils.asSet;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Unit test for {@link KbaConsoleConfigExtractor}.
 *
 * @since 13.0.0
 */
public final class KbaConsoleConfigExtractorTest {

    private KbaConsoleConfigExtractor extractor;

    @BeforeMethod
    public void setUp() {
        extractor = new KbaConsoleConfigExtractor();
    }

    @Test
    public void correctlyParsesTheQuestion() {
        // Given
        Map<String, Set<String>> consoleAttributes = new HashMap<>();
        consoleAttributes.put("forgerockRESTSecurityKBAQuestions", asSet("1|en|question1", "1|en_US|question1_us", "2|en|question2"));
        consoleAttributes.put("forgerockRESTSecurityAnswersUserMustProvide", singleton("5"));
        consoleAttributes.put("forgerockRESTSecurityQuestionsUserMustAnswer", singleton("3"));

        // When
        KbaConsoleConfig kbaConsoleConfig = extractor.extract(consoleAttributes);

        // Then
        assertThat(kbaConsoleConfig.getSecurityQuestions()).containsOnlyKeys("1", "2");
        assertThat(kbaConsoleConfig.getSecurityQuestions().get("1")).containsEntry("en", "question1").containsEntry("en_US", "question1_us");
        assertThat(kbaConsoleConfig.getSecurityQuestions().get("2")).containsEntry("en", "question2");
        assertThat(kbaConsoleConfig.getMinimumAnswersToDefine()).isEqualTo(5);
        assertThat(kbaConsoleConfig.getMinimumAnswersToVerify()).isEqualTo(3);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*question format.*")
    public void parsingFailsWithInvalidFormat() {
        // Given
        Map<String, Set<String>> consoleAttributes = new HashMap<>();
        consoleAttributes.put("forgerockRESTSecurityKBAQuestions", singleton("1 2|e n|question|something else"));
        consoleAttributes.put("forgerockRESTSecurityAnswersUserMustProvide", singleton("5"));
        consoleAttributes.put("forgerockRESTSecurityQuestionsUserMustAnswer", singleton("3"));

        // When
        extractor.extract(consoleAttributes);
    }

}