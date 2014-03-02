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
import org.xml.sax.SAXException;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.ParserConfigurationException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link PerThreadSAXParserProvider}.
 */
public class PerThreadSAXParserProviderTest {
    private SAXParserProvider mockProvider;
    private PerThreadSAXParserProvider testProvider;

    @BeforeMethod
    public void setup() {
        mockProvider = new MockSAXParserProvider();
        testProvider = new PerThreadSAXParserProvider(mockProvider);
    }

    @Test
    public void shouldReturnSameInstanceForSameThread() throws Exception {
        // Given
        boolean validating = false;

        // When
        SAXParser parser1 = testProvider.getSAXParser(validating);
        SAXParser parser2 = testProvider.getSAXParser(validating);

        // Then
        assertThat(parser1).isSameAs(parser2);
    }

    @Test
    public void shouldReturnDifferentInstancesForDifferentThreads() throws Exception {
        // Given
        boolean validating = false;
        ExecutorService otherThread = Executors.newSingleThreadExecutor();

        // When
        SAXParser parser1 = testProvider.getSAXParser(validating);
        SAXParser parser2 = otherThread.submit(getParserAction(validating)).get();

        // Then
        assertThat(parser1).isNotSameAs(parser2);
    }

    @Test
    public void shouldEvictLeastRecentlyAccessedElements() throws Exception {
        // Given
        int maxSize = 1;
        boolean validating = false;
        ExecutorService otherThread = Executors.newSingleThreadExecutor();
        testProvider = new PerThreadSAXParserProvider(new MockSAXParserProvider(), maxSize);

        // When
        SAXParser parser1 = testProvider.getSAXParser(validating);
        // Get a parser from another thread, causing maxSize (1) to be exceeded, evicting the least recently accessed entry
        otherThread.submit(getParserAction(validating)).get();
        // Accessing again from the main thread should cause a new parser instance to be created
        SAXParser parser2 = testProvider.getSAXParser(validating);

        // Then
        assertThat(parser1).isNotSameAs(parser2);
    }

    @Test
    public void shouldCacheValidatingAndNonValidatingInstancesSeparately() throws Exception {
        // Given

        // When
        SAXParser validatingParser = testProvider.getSAXParser(true);
        SAXParser nonValidatingParser = testProvider.getSAXParser(false);

        // Then
        assertThat(validatingParser).isNotSameAs(nonValidatingParser);
    }

    private Callable<SAXParser> getParserAction(final boolean validating) {
        return new Callable<SAXParser>() {
            public SAXParser call() throws Exception {
                return testProvider.getSAXParser(validating);
            }
        };
    }

    private static class MockSAXParserProvider implements SAXParserProvider {
        public SAXParser getSAXParser(boolean validating) throws ParserConfigurationException, SAXException {
            return mock(SAXParser.class);
        }
    }
}
