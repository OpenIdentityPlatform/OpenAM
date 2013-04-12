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
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for SingleWildcardNode.
 *
 * @author andrew.forrest@forgerock.com
 */
public class SingleWildcardNodeTest {

    private TreeNode singleWildcardTreeNode;

    @Before
    public void setUp() {
        singleWildcardTreeNode = new SingleWildcardNode();
    }

    @Test
    public void verifySingleWildcardNodeCharacteristics() {
        assertFalse(singleWildcardTreeNode.isRoot());
        assertTrue(singleWildcardTreeNode.isWildcard());
        assertEquals(SingleWildcardNode.WILDCARD, singleWildcardTreeNode.getNodeValue());
    }

    @Test
    public void disallowMultiLevelMatching() {
        SearchContext context = new MapSearchContext();
        assertTrue(singleWildcardTreeNode.hasInterestIn('a', context));
        assertTrue(singleWildcardTreeNode.hasInterestIn('b', context));
        assertTrue(singleWildcardTreeNode.hasInterestIn('/', context));
        assertFalse(singleWildcardTreeNode.hasInterestIn('c', context));
    }

    @Test
    public void disallowQuestionMarkMatching() {
        SearchContext context = new MapSearchContext();
        assertFalse(singleWildcardTreeNode.hasInterestIn('?', context));
    }

    @Test
    public void disallowBookmarkMatching() {
        SearchContext context = new MapSearchContext();
        assertFalse(singleWildcardTreeNode.hasInterestIn('#', context));
    }

}
