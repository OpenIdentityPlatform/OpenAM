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
package org.forgerock.openam.radius.server.config;

import static org.assertj.core.api.Assertions.*;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

/**
 * Test for <code>DefaultClientSecretGenerator</code>.
 *
 * @see org.forgerock.openam.radius.server.config.DefaultClientSecretGenerator
 */
public class DefaultClientSecretGeneratorTest {

    Logger logger = LoggerFactory.getLogger(DefaultClientSecretGeneratorTest.class.getSimpleName());

    /**
     * Test for method getDefaultValues().
     *
     * @see org.forgerock.openam.radius.server.config.DefaultClientSecretGenerator#getDefaultValues
     */
    @Test
    public void testGetDefaultValues() {

        // given
        final DefaultClientSecretGenerator secretGenerator = new DefaultClientSecretGenerator();
        final int testIterations = 100;

        for (int i = 0; i < testIterations; ++i) {
            // when
            @SuppressWarnings("unchecked")
            final Set<String> secretHolder = secretGenerator.getDefaultValues();

            // then
            assertThat(secretHolder).isNotNull();
            final int expectedSize = 1;
            assertThat(secretHolder.size()).isEqualTo(expectedSize);

            final String secret = secretHolder.iterator().next();
            logger.debug("Generated secret was {}", secret);
            final boolean containsOnlyValidChars = secret.matches("[a-zA-Z0-9+/]{16}");
            logger.debug("containsOnlyValidChars is {}", containsOnlyValidChars);
            assertThat(secret.matches("[a-zA-Z0-9+/]{16}")).isTrue();
        }
    }
}
