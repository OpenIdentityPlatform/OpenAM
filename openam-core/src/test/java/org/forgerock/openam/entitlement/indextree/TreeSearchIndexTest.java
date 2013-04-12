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
 * Copyright 2013 ForgeRock Inc.
 */
package org.forgerock.openam.entitlement.indextree;

import com.sun.identity.entitlement.ResourceSearchIndexes;
import com.sun.identity.entitlement.interfaces.ISearchIndex;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for TreeSearchIndex.
 *
 * @author andrew.forrest@forgerock.com
 */
public class TreeSearchIndexTest {

    private IndexTreeService treeService;
    private ISearchIndex searchIndex;

    @Before
    public void setUp() {
        treeService = mock(IndexTreeService.class);
        searchIndex = new TreeSearchIndex(treeService);
    }

    /**
     * Tests a simple straight through scenario, where a normalised URL is passed to the search index implementation.
     */
    @Test
    public void simpleScenario() throws Exception {
        // Record that the indexes set should be returned when given the test url.
        Set<String> indexes = new HashSet<String>();
        indexes.add("some-test-index-1");
        indexes.add("some-test-index-2");
        when(treeService.searchTree("http://www.test.com:80/", "/test-realm")).thenReturn(indexes);

        // Execute the actual evaluation.
        ResourceSearchIndexes result = searchIndex.getIndexes("http://www.test.com:80/", "/test-realm");

        // Verify the test results
        assertEquals(indexes, result.getPathIndexes());
        assertTrue(result.getHostIndexes().isEmpty());
        assertTrue(result.getParentPathIndexes().isEmpty());

        // Verify the use of the mock object.
        verify(treeService).searchTree("http://www.test.com:80/", "/test-realm");
    }

    /**
     * Tests that the search index implementation normalises the passed resource URL.
     */
    @Test
    public void normaliseURL() throws Exception {
        // Execute the actual evaluation.
        searchIndex.getIndexes("http://www.test1.com:80", "/test-realm");
        searchIndex.getIndexes("http://www.test2.com:80/helloworld", "/test-realm");
        searchIndex.getIndexes("http://www.test3.com", "/test-realm");
        searchIndex.getIndexes("https://www.test4.com", "/test-realm");
        searchIndex.getIndexes("https://www.test5.com/hello?a=b,c=d", "/test-realm");
        searchIndex.getIndexes("   http://www.test6.com:80/   ", "/test-realm");
        searchIndex.getIndexes("HtTp://wWw.tESt7.CoM:80/", "/test-realm");

        // Verify the use of the mock object.
        verify(treeService).searchTree("http://www.test1.com:80/", "/test-realm");
        verify(treeService).searchTree("http://www.test2.com:80/helloworld", "/test-realm");
        verify(treeService).searchTree("http://www.test3.com:80/", "/test-realm");
        verify(treeService).searchTree("https://www.test4.com:443/", "/test-realm");
        verify(treeService).searchTree("https://www.test5.com:443/hello?a=b,c=d", "/test-realm");
        verify(treeService).searchTree("http://www.test6.com:80/", "/test-realm");
        verify(treeService).searchTree("http://www.test7.com:80/", "/test-realm");
    }

    /**
     * Tests that the search index parses retrieved indexes, encoding special LDAP characters.
     */
    @Test
    public void parseRetrievedIndexes() throws Exception {
        // Record that the indexes set should be returned when given the test url.
        Set<String> indexes = new HashSet<String>();
        indexes.add("a-b-*-d-e");
        indexes.add("a-*-c-*-e");
        when(treeService.searchTree("http://www.test.com:80/", "/test-realm")).thenReturn(indexes);

        // Execute the actual evaluation.
        ResourceSearchIndexes result = searchIndex.getIndexes("http://www.test.com:80/", "/test-realm");

        // Verify the test results
        Set<String> parsedIndexes = new HashSet<String>();
        parsedIndexes.add("a-b-\\2A-d-e");
        parsedIndexes.add("a-\\2A-c-\\2A-e");
        assertEquals(parsedIndexes, result.getPathIndexes());
        assertTrue(result.getHostIndexes().isEmpty());
        assertTrue(result.getParentPathIndexes().isEmpty());

        // Verify the use of the mock object.
        verify(treeService).searchTree("http://www.test.com:80/", "/test-realm");
    }

    /**
     * Though the framework may be passed an invalid URL, it should still attempt to authorise it.
     */
    @Test
    public void handleInvalidURL() throws Exception {
        // Execute the actual evaluation.
        searchIndex.getIndexes("abcdefgh", "/test-realm");

        // Verify the use of the mock object.
        verify(treeService).searchTree("abcdefgh", "/test-realm");
    }

}
