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

/**
 * Root node of the tree structure.
 * <p/>
 * As there can be many nodes at the root level, the idea of this root node was created to represent the parent of those
 * potential nodes. This helps when describing algorithms by having a single point of access into the tree. It has no
 * other function than to be an 'anchor' to the structure.
 *
 * @author apforrest
 */
public final class RootNode extends BasicTreeNode {

    private static final char NULL = '\u0000';

    @Override
    public char getNodeValue() {
        return NULL;
    }

    @Override
    public boolean isRoot() {
        return true;
    }

    /**
     * Match is not allowed at the root level.
     *
     * @throws IllegalAccessError
     *         when invoked.
     */
    @Override
    public boolean hasInterestIn(char value, SearchContext context) {
        throw new IllegalAccessError("This is the root node");
    }

    /**
     * The root node can not have a sibling node.
     *
     * @throws IllegalAccessError
     *         when invoked.
     */
    @Override
    public void setSibling(TreeNode sibling) {
        throw new IllegalAccessError("This is the root node");
    }

    /**
     * The root node can not have a parent node.
     *
     * @throws IllegalAccessError
     *         when invoked.
     */
    @Override
    public void setParent(TreeNode parent) {
        throw new IllegalAccessError("This is the root node");
    }

}
