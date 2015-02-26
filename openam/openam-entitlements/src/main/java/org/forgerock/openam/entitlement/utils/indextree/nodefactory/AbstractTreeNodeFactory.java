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

import java.util.HashMap;
import java.util.Map;

import org.forgerock.openam.entitlement.utils.indextree.nodecreator.NodeCreator;
import org.forgerock.openam.entitlement.utils.indextree.treenodes.TreeNode;

/**
 * Provides the boilerplate logic for a basic tree node factory.
 * 
 * @author apforrest
 */
public abstract class AbstractTreeNodeFactory implements TreeNodeFactory {

    private final Map<Character, NodeCreator> creatorCache;

    public AbstractTreeNodeFactory() {
        creatorCache = new HashMap<Character, NodeCreator>();
    }

    @Override
    public TreeNode getTreeNode(char nodeValue) {
        NodeCreator creator = creatorCache.get(nodeValue);
        return (creator == null) ? createDefaultNode(nodeValue) : creator.createNode(nodeValue);
    }

    /**
     * Add a node creator for the given node value.
     * 
     * @param nodeValue
     *            The node value.
     * @param creator
     *            The node creator instance.
     */
    protected void addNodeCreator(char nodeValue, NodeCreator creator) {
        creatorCache.put(nodeValue, creator);
    }

    /**
     * Creates a default tree node where no corresponding node creator has been identified.
     * 
     * @param nodeValue
     *            The node value.
     * @return A default tree node.
     */
    protected abstract TreeNode createDefaultNode(char nodeValue);

}
