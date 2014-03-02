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

import javax.xml.parsers.DocumentBuilder;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Unit tests for {@link SafeDocumentBuilderProvider}. Assumes that the underlying
 * {@link com.sun.identity.shared.xml.XMLUtils#getSafeDocumentBuilder(boolean)} code is already sufficiently tested.
 */
public class SafeDocumentBuilderProviderTest {
    private SafeDocumentBuilderProvider testProvider;

    @BeforeMethod
    public void setup() {
        testProvider = new SafeDocumentBuilderProvider();
    }

    @Test
    public void shouldReturnValidatingDocumentBuildersWhenAsked() throws Exception {
        // Given

        // When
        DocumentBuilder validatingBuilder = testProvider.getDocumentBuilder(true);

        // Then
        assertThat(validatingBuilder.isValidating()).isTrue();
    }

    @Test
    public void shouldReturnNonValidatingDocumentBuildersWhenAsked() throws Exception {
        // Given

        // When
        DocumentBuilder nonValidatingBuilder = testProvider.getDocumentBuilder(false);

        // Then
        assertThat(nonValidatingBuilder.isValidating()).isFalse();
    }
}
