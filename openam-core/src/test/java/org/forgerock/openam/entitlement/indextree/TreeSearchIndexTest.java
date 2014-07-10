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
 * Copyright 2013-2014 ForgeRock AS.
 */
package org.forgerock.openam.entitlement.indextree;

import com.sun.identity.entitlement.ResourceSearchIndexes;
import com.sun.identity.entitlement.interfaces.ISearchIndex;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.forgerock.openam.utils.CollectionUtils.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * Unit test for TreeSearchIndex.
 */
public class TreeSearchIndexTest {

    private IndexTreeService treeService;
    private ISearchIndex searchIndex;

    @BeforeMethod
    public void setUp() {
        treeService = mock(IndexTreeService.class);
        searchIndex = new TreeSearchIndexDelegate(treeService);
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
        assertEquals(asSet("://", "://.com", "://www.test.com", "://.test.com"), result.getHostIndexes());
        assertEquals(indexes, result.getPathIndexes());
        assertEquals(asSet("/"), result.getParentPathIndexes());

        // Verify the use of the mock object.
        verify(treeService).searchTree("http://www.test.com:80/", "/test-realm");
    }

    /**
     * Tests that the search index implementation normalises the passed resource URL.
     */
    @Test
    public void enforceLowerCase() throws Exception {
        // Execute the actual evaluation.
        searchIndex.getIndexes("HtTp://wWw.tESt1.CoM:80/", "/test-realm");

        // Verify the use of the mock object.
        verify(treeService).searchTree("http://www.test1.com:80/", "/test-realm");
    }

    /**
     * Tests that the search index parses retrieved indexes without encoding special characters.
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
        parsedIndexes.add("a-b-*-d-e");
        parsedIndexes.add("a-*-c-*-e");

        assertEquals(asSet("://", "://.com", "://www.test.com", "://.test.com"), result.getHostIndexes());
        assertEquals(parsedIndexes, result.getPathIndexes());
        assertEquals(asSet("/"), result.getParentPathIndexes());

        // Verify the use of the mock object.
        verify(treeService).searchTree("http://www.test.com:80/", "/test-realm");
    }

}
