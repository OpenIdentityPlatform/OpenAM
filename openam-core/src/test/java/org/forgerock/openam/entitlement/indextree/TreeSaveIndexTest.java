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

import com.sun.identity.entitlement.ResourceSaveIndexes;
import com.sun.identity.entitlement.interfaces.ISaveIndex;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Unit test for TreeSaveIndex.
 *
 * @author andrew.forrest@forgerock.com
 */
public class TreeSaveIndexTest {

    private ISaveIndex saveIndex;

    @BeforeMethod
    public void setUp() {
        saveIndex = new TreeSaveIndex();
    }

    @Test
    public void simpleScenario() {
        ResourceSaveIndexes result = saveIndex.getIndexes("http://www.test.com/*");

        Set<String> expectedResults = new HashSet<String>();
        expectedResults.add("http://www.test.com/*");

        assertEquals(Arrays.asList("://www.test.com"), result.getHostIndexes());
        assertEquals(expectedResults, result.getPathIndexes());
        assertEquals(Arrays.asList("/"), result.getParentPathIndexes());
    }

    @Test
    public void normaliseSpecialWildcards() {
        ResourceSaveIndexes result = saveIndex.getIndexes("http://www.test.com/-*-/hello");

        Set<String> expectedResults = new HashSet<String>();
        expectedResults.add("http://www.test.com/^/hello");

        assertEquals(Arrays.asList("://www.test.com"), result.getHostIndexes());
        assertEquals(expectedResults, result.getPathIndexes());
        assertEquals(Arrays.asList("/"), result.getParentPathIndexes());
    }

    @Test
    public void enforceLowerCase() {
        ResourceSaveIndexes result = saveIndex.getIndexes("HtTp://wWw.tESt.CoM/*");

        Set<String> expectedResults = new HashSet<String>();
        expectedResults.add("http://www.test.com/*");

        assertEquals(Arrays.asList("://www.test.com"), result.getHostIndexes());
        assertEquals(expectedResults, result.getPathIndexes());
        assertEquals(Arrays.asList("/"), result.getParentPathIndexes());
    }

}
