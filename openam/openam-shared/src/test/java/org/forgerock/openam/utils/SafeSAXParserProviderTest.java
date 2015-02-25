/*
 * Copyright 2014 ForgeRock, AS.
 *
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
 */

package org.forgerock.openam.utils;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.xml.parsers.SAXParser;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Basic unit tests for {@link SafeSAXParserProvider}. Assumes the underlying {@link org.forgerock.util.xml.XMLUtils}
 * implementation is already tested.
 */
public class SafeSAXParserProviderTest {
    private SafeSAXParserProvider testProvider;

    @BeforeMethod
    public void createProvider() {
        testProvider = new SafeSAXParserProvider();
    }

    @Test
    public void shouldReturnAValidatingParserWhenAskedTo() throws Exception {
        // Given

        // When
        SAXParser result = testProvider.getSAXParser(true);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isValidating()).isTrue();
    }

    @Test
    public void shouldReturnANonValidatingParserWhenAskedTo() throws Exception {
        // Given

        // When
        SAXParser result = testProvider.getSAXParser(false);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isValidating()).isFalse();
    }

}
