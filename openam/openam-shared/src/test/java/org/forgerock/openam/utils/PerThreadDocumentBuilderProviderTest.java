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
import javax.xml.parsers.ParserConfigurationException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link PerThreadDocumentBuilderProvider}.
 */
public class PerThreadDocumentBuilderProviderTest {

    private DocumentBuilderProvider mockProvider;
    private PerThreadDocumentBuilderProvider testProvider;

    @BeforeMethod
    public void setup() {
        mockProvider = new MockDocumentBuilderProvider();
        testProvider = new PerThreadDocumentBuilderProvider(mockProvider);
    }

    @Test
    public void shouldReturnSameInstanceForSameThread() throws ParserConfigurationException {
        // Given
        boolean validating = false;

        // When
        DocumentBuilder builder1 = testProvider.getDocumentBuilder(validating);
        DocumentBuilder builder2 = testProvider.getDocumentBuilder(validating);

        // Then
        assertThat(builder1).isSameAs(builder2);
    }

    @Test
    public void shouldReturnDifferentInstancesForDifferentThreads() throws Exception {
        // Given
        boolean validating = false;
        ExecutorService otherThread = Executors.newSingleThreadExecutor();

        // When
        DocumentBuilder builder1 = testProvider.getDocumentBuilder(validating);
        DocumentBuilder builder2 = otherThread.submit(getDocBuilderAction(validating)).get();

        // Then
        assertThat(builder1).isNotSameAs(builder2);
    }

    @Test
    public void shouldEvictLeastRecentlyAccessedElements() throws Exception {
        // Given
        int maxSize = 1;
        boolean validating = false;
        ExecutorService otherThread = Executors.newSingleThreadExecutor();
        testProvider = new PerThreadDocumentBuilderProvider(new MockDocumentBuilderProvider(), maxSize);

        // When
        DocumentBuilder builder1 = testProvider.getDocumentBuilder(validating);
        // Get a doc builder from another thread, causing maxSize (1) to be exceeded, evicting the least recently accessed entry
        otherThread.submit(getDocBuilderAction(validating)).get();
        // Accessing again from the main thread should cause a new doc builder instance to be created
        DocumentBuilder builder2 = testProvider.getDocumentBuilder(validating);

        // Then
        assertThat(builder1).isNotSameAs(builder2);
    }

    @Test
    public void shouldCacheValidatingAndNonValidatingInstancesSeparately() throws Exception {
        // Given

        // When
        DocumentBuilder validatingBuilder = testProvider.getDocumentBuilder(true);
        DocumentBuilder nonValidatingBuilder = testProvider.getDocumentBuilder(false);

        // Then
        assertThat(validatingBuilder).isNotSameAs(nonValidatingBuilder);
    }

    private Callable<DocumentBuilder> getDocBuilderAction(final boolean validating) {
        return new Callable<DocumentBuilder>() {
            public DocumentBuilder call() throws Exception {
                return testProvider.getDocumentBuilder(validating);
            }
        };
    }

    private static class MockDocumentBuilderProvider implements DocumentBuilderProvider {
        public DocumentBuilder getDocumentBuilder(boolean validating) throws ParserConfigurationException {
            return mock(DocumentBuilder.class);
        }
    }
}
