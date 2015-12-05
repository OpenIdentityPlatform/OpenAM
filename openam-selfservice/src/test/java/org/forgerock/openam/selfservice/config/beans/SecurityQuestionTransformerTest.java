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

package org.forgerock.openam.selfservice.config.beans;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.utils.CollectionUtils.asSet;

import org.forgerock.openam.sm.config.ConfigTransformer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Unit test for {@link SecurityQuestionTransformer}.
 *
 * @since 13.0.0
 */
public final class SecurityQuestionTransformerTest {

    private ConfigTransformer<Map<String, Map<String, String>>> securityQuestionTransformer;

    @BeforeMethod
    public void setUp() {
        securityQuestionTransformer = new SecurityQuestionTransformer();
    }

    @Test
    public void parsesValidMessages() {
        // Given
        Set<String> questions = asSet("123|en|Some question", "abc|fr|Another question");

        // When
        Map<String, Map<String, String>> parsedQuestions = securityQuestionTransformer.transform(questions, Map.class);

        // Then
        assertThat(parsedQuestions).containsEntry("123", Collections.singletonMap("en", "Some question"));
        assertThat(parsedQuestions).containsEntry("abc", Collections.singletonMap("fr", "Another question"));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void failsToParseInvalidMessage() {
        // Given
        Set<String> questions = asSet("en|Some question", "fr-Another question");

        // When
        securityQuestionTransformer.transform(questions, Map.class);
    }

}