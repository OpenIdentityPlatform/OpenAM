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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Unit test for RootNode.
 *
 * @author andrew.forrest@forgerock.com
 */
public class RootNodeTest {

    private TreeNode rootTreeNode;

    @BeforeMethod
    public void setUp() {
        rootTreeNode = new RootNode();
    }

    @Test
    public void verifyRootNodeCharacteristics() {
        assertTrue(rootTreeNode.isRoot());
        assertFalse(rootTreeNode.isWildcard());
        assertEquals('\u0000', rootTreeNode.getNodeValue());
    }

    /**
     * Exception thrown if hasInterestIn on root node.
     * Root node is an anchor and so have no behaviour.
     */
    @Test(expectedExceptions = IllegalAccessError.class)
    public void hasInterestInDisabled() {
        rootTreeNode.hasInterestIn('a', mock(SearchContext.class));
    }

    /**
     * Exception thrown if setSibling on root node.
     * Root node can not have siblings.
     */
    @Test(expectedExceptions = IllegalAccessError.class)
    public void setSiblingDisabled() {
        rootTreeNode.setSibling(mock(TreeNode.class));
    }

    /**
     * Exception thrown if setParent on root node.
     * Root is the top most node and so can not have a parent.
     */
    @Test(expectedExceptions = IllegalAccessError.class)
    public void setParentDisabled() {
        rootTreeNode.setParent(mock(TreeNode.class));
    }

}
