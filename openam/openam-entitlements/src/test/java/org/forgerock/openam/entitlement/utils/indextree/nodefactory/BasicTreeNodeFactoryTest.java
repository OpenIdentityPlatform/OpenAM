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
package org.forgerock.openam.entitlement.utils.indextree.nodefactory;

import org.forgerock.openam.entitlement.utils.indextree.nodecontext.SearchContext;
import org.forgerock.openam.entitlement.utils.indextree.nodecontext.MapSearchContext;
import org.forgerock.openam.entitlement.utils.indextree.treenodes.MultiWildcardNode;
import org.forgerock.openam.entitlement.utils.indextree.treenodes.SingleWildcardNode;
import org.forgerock.openam.entitlement.utils.indextree.treenodes.TreeNode;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Unit test for BasicTreeNodeFactory and consequently, AbstractTreeNodeFactory.
 *
 * @author andrew.forrest@forgerock.com
 */
public class BasicTreeNodeFactoryTest {

    private TreeNodeFactory factory;

    @BeforeMethod
    public void setUp() {
        factory = new BasicTreeNodeFactory();
    }

    /**
     * Verify that the factory does retrieve a root node.
     */
    @Test
    public void retrieveRootNode() {
        TreeNode rootNode = factory.getRootNode();
        assertNotNull(rootNode);
        assertTrue(rootNode.isRoot());
        assertFalse(rootNode.isWildcard());
    }

    /**
     * Verify that the factory does retrieve a default node.
     */
    @Test
    public void retrieveDefaultNode() {
        TreeNode defaultNode = factory.getTreeNode('a');
        assertNotNull(defaultNode);
        assertEquals('a', defaultNode.getNodeValue());
        assertFalse(defaultNode.isWildcard());
        assertFalse(defaultNode.isRoot());

        SearchContext context = new MapSearchContext();
        assertTrue(defaultNode.hasInterestIn('a', context));
        assertFalse(defaultNode.hasInterestIn('b', context));
    }

    /**
     * Verify that the factory does retrieve a multilevel wildcard node.
     */
    @Test
    public void retrieveMultiLevelWildcard() {
        TreeNode multiLevelWildcardNode = factory.getTreeNode(MultiWildcardNode.WILDCARD);
        assertNotNull(multiLevelWildcardNode);
        assertEquals(MultiWildcardNode.WILDCARD, multiLevelWildcardNode.getNodeValue());
        assertTrue(multiLevelWildcardNode.isWildcard());
        assertFalse(multiLevelWildcardNode.isRoot());

        SearchContext context = new MapSearchContext();
        assertTrue(multiLevelWildcardNode.hasInterestIn('a', context));
        assertTrue(multiLevelWildcardNode.hasInterestIn('b', context));
        assertTrue(multiLevelWildcardNode.hasInterestIn('/', context));
        assertTrue(multiLevelWildcardNode.hasInterestIn('c', context));
    }

    /**
     * Verify that the factory does retrieve a single level wildcard node.
     */
    @Test
    public void retrieveSingleLevelWildcard() {
        TreeNode singleLevelWildcardNode = factory.getTreeNode(SingleWildcardNode.WILDCARD);
        assertNotNull(singleLevelWildcardNode);
        assertEquals(SingleWildcardNode.WILDCARD, singleLevelWildcardNode.getNodeValue());
        assertTrue(singleLevelWildcardNode.isWildcard());
        assertFalse(singleLevelWildcardNode.isRoot());

        SearchContext context = new MapSearchContext();
        assertTrue(singleLevelWildcardNode.hasInterestIn('a', context));
        assertTrue(singleLevelWildcardNode.hasInterestIn('b', context));
        assertTrue(singleLevelWildcardNode.hasInterestIn('/', context));
        assertFalse(singleLevelWildcardNode.hasInterestIn('c', context));
    }

}
