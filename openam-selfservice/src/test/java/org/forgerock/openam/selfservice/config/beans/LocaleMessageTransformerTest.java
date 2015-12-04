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

import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class LocaleMessageTransformerTest {

    private ConfigTransformer<Map<Locale, String>> localeMessageTransformer;

    @BeforeMethod
    public void setUp() {
        localeMessageTransformer = new LocaleMessageTransformer();
    }

    @Test
    public void parsesValidMessages() {
        // Given
        Set<String> messages = asSet("en|Some message", "fr|Another message");

        // When
        Map<Locale, String> localeMessages = localeMessageTransformer.transform(messages, Map.class);

        // Then
        assertThat(localeMessages).containsEntry(Locale.ENGLISH, "Some message");
        assertThat(localeMessages).containsEntry(Locale.FRENCH, "Another message");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void failsToParseInvalidMessage() {
        // Given
        Set<String> messages = asSet("en|Some message", "fr-Another message");

        // When
        localeMessageTransformer.transform(messages, Map.class);
    }

}