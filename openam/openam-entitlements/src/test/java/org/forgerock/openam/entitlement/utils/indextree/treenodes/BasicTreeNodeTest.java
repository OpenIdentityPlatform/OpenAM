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

import org.forgerock.openam.entitlement.utils.indextree.nodecontext.SearchContext;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Unit test for abstract BasicTreeNode.
 *
 * @author andrew.forrest@forgerock.com
 */
public class BasicTreeNodeTest {

    private TreeNode basicTreeNode;

    @Before
    public void setUp() {
        basicTreeNode = new TestBasicTreeNode('\u0000');
    }

    /**
     * Verifies that correct relationships are set for a tree node.
     */
    @Test
    public void verifyCorrectRelationships() {
        TreeNode siblingNode = mock(TreeNode.class);
        TreeNode parentNode = mock(TreeNode.class);
        TreeNode childNode = mock(TreeNode.class);

        assertTrue(basicTreeNode.isLeafNode());

        basicTreeNode.setSibling(siblingNode);
        basicTreeNode.setParent(parentNode);
        basicTreeNode.setChild(childNode);

        assertTrue(basicTreeNode.hasSibling());
        assertEquals(siblingNode, basicTreeNode.getSibling());
        assertTrue(basicTreeNode.hasParent());
        assertEquals(parentNode, basicTreeNode.getParent());
        assertTrue(basicTreeNode.hasChild());
        assertEquals(childNode, basicTreeNode.getChild());

        // No longer leaf node that a child has been added.
        assertFalse(basicTreeNode.isLeafNode());
    }

    /**
     * Verify the end point behaviour. End points are used to mark the end path of a given rule.
     */
    @Test
    public void verifyEndPointBehaviour() {
        // By default has not end point until marked.
        assertFalse(basicTreeNode.isEndPoint());

        // Now add end point should be present.
        basicTreeNode.markEndPoint();
        assertTrue(basicTreeNode.isEndPoint());

        // Removing the end point should result in no end points.
        basicTreeNode.removeEndPoint();
        assertFalse(basicTreeNode.isEndPoint());

        // Now add two end points.
        basicTreeNode.markEndPoint();
        basicTreeNode.markEndPoint();

        // Gradually remove the end points.
        assertTrue(basicTreeNode.isEndPoint());
        basicTreeNode.removeEndPoint();
        assertTrue(basicTreeNode.isEndPoint());
        basicTreeNode.removeEndPoint();
        assertFalse(basicTreeNode.isEndPoint());
    }

    /**
     * Verifies that a given node gives the correct full node path.
     */
    @Test
    public void verifyCorrectNodePath() {
        String path = "abcdefghijklmnopqrstuvwxyz";
        char[] pathChars = path.toCharArray();

        TreeNode node = null;
        // Construct single root tree from the path.
        for (int i = pathChars.length - 1; i >= 0; i--) {
            TreeNode newNode = new TestBasicTreeNode(pathChars[i]);
            if (node != null) {
                node.setParent(newNode);
            }
            newNode.setChild(node);
            node = newNode;
        }

        // Verify each node for correct path name.
        for (int i = 0; i < pathChars.length; i++) {
            assertEquals(path.substring(0, i + 1), node.getFullPath());
            node = node.getChild();
        }
    }

    /**
     * Verify the toString construction.
     */
    @Test
    public void verifyToStringConstruction() {
        String path = "abcdefghijklmnopqrstuvwxyz";
        char[] pathChars = path.toCharArray();

        TreeNode node = null;
        // Construct single root tree from the path.
        for (int i = pathChars.length - 1; i >= 0; i--) {
            TreeNode newNode = new TestBasicTreeNode(pathChars[i]);
            if (node != null) {
                node.setParent(newNode);
            }
            newNode.setChild(node);
            node = newNode;
        }

        // Test string without end point marking.
        assertEquals(path, node.toString(false));

        // Test string with end point marking.
        assertEquals(path, node.toString(true));
        // Now mark an end point.
        node.markEndPoint();
        assertEquals("a(1)bcdefghijklmnopqrstuvwxyz", node.toString(true));
    }

    // Simple implementation to test the BasicTreeNode features.
    private static class TestBasicTreeNode extends BasicTreeNode {

        private final char value;

        public TestBasicTreeNode(char value) {
            this.value = value;
        }

        @Override
        public boolean hasInterestIn(char value, SearchContext context) {
            return this.value == value;
        }

        @Override
        public char getNodeValue() {
            return value;
        }

    }

}
