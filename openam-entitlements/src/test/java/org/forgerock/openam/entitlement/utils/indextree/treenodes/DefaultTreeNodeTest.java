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
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Unit test for DefaultTreeNode.
 *
 * @author andrew.forrest@forgerock.com
 */
public class DefaultTreeNodeTest {

    @Test
    public void verifyDefaultNodeCharacteristics() {
        TreeNode defaultTreeNode = new DefaultTreeNode('a');
        assertFalse(defaultTreeNode.isRoot());
        assertFalse(defaultTreeNode.isWildcard());
        assertEquals('a', defaultTreeNode.getNodeValue());

        SearchContext context = new MapSearchContext();
        assertTrue(defaultTreeNode.hasInterestIn('a', context));
        assertFalse(defaultTreeNode.hasInterestIn('b', context));
    }

}
