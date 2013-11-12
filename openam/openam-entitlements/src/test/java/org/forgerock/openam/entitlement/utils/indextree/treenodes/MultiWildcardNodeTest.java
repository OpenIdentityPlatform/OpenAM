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
package org.forgerock.openam.entitlement.utils.indextree.treenodes;

import org.forgerock.openam.entitlement.utils.indextree.nodecontext.MapSearchContext;
import org.forgerock.openam.entitlement.utils.indextree.nodecontext.SearchContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Unit test for MultiWildcardNode.
 *
 * @author andrew.forrest@forgerock.com
 */
public class MultiWildcardNodeTest {

    private TreeNode multiWildcardTreeNode;

    @BeforeMethod
    public void setUp() {
        multiWildcardTreeNode = new MultiWildcardNode();
    }

    @Test
    public void verifySingleWildcardNodeCharacteristics() {
        assertFalse(multiWildcardTreeNode.isRoot());
        assertTrue(multiWildcardTreeNode.isWildcard());
        assertEquals(MultiWildcardNode.WILDCARD, multiWildcardTreeNode.getNodeValue());
    }

    @Test
    public void allowMultiLevelMatching() {
        SearchContext context = new MapSearchContext();
        assertTrue(multiWildcardTreeNode.hasInterestIn('a', context));
        assertTrue(multiWildcardTreeNode.hasInterestIn('b', context));
        assertTrue(multiWildcardTreeNode.hasInterestIn('/', context));
        assertTrue(multiWildcardTreeNode.hasInterestIn('c', context));
    }

    @Test
    public void disallowQuestionMarkMatching() {
        SearchContext context = new MapSearchContext();
        assertFalse(multiWildcardTreeNode.hasInterestIn('?', context));
    }

    @Test
    public void disallowBookmarkMatching() {
        SearchContext context = new MapSearchContext();
        assertFalse(multiWildcardTreeNode.hasInterestIn('#', context));
    }

}
