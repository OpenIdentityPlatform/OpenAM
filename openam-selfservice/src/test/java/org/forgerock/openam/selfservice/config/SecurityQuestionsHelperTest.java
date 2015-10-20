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
import java.util.List;
import java.util.Map;

/**
 * Unit test for {@link SecurityQuestionsHelper}.
 *
 * @since 13.0.0
 */
public final class SecurityQuestionsHelperTest {

    @Test
    public void correctlyParsesTheQuestion() {
        // Given
        List<String> questions = Arrays.asList("1|en|question1", "1|en_US|question1_us", "2|en|question2");

        // When
        Map<String, Map<String, String>> parsedQuestions = SecurityQuestionsHelper.parseQuestions(questions);

        // Then
        assertThat(parsedQuestions).containsOnlyKeys("1", "2");
        assertThat(parsedQuestions.get("1")).containsEntry("en", "question1").containsEntry("en_US", "question1_us");
        assertThat(parsedQuestions.get("2")).containsEntry("en", "question2");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void parsingFailsWithInvalidFormat() {
        // Given
        List<String> questions = Collections.singletonList("1 2|e n|question|something else");

        // When
        SecurityQuestionsHelper.parseQuestions(questions);
    }

}
