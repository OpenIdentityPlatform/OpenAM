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
package org.forgerock.openam.entitlement.utils.indextree;

import java.util.Collection;
import java.util.Set;

/**
 * Maintains a collections of index rules in a tree structure, where each tree node is a single character from the index
 * rule. Therefore a given tree will be as deep as the number of characters in the longest index rule. As a result tree
 * searches are not effected by the size of the tree and should give a constant time. This is because adding additional
 * index rules will effect the width of the tree, whereas the search is interested in the depth of the tree.
 *
 * @author apforrest
 */
public interface IndexRuleTree {

    /**
     * Adds an index rule to the tree.
     *
     * @param indexRule
     *         The index rule.
     */
    public void addIndexRule(String indexRule);

    /**
     * Adds a collection of index rules to the tree.
     *
     * @param indexRules
     *         The collection of index rules.
     */
    public void addIndexRules(Collection<String> indexRules);

    /**
     * Removes the given index rule from the tree.
     *
     * @param indexRule
     *         The index rule to be removed.
     */
    public void removeIndexRule(String indexRule);

    /**
     * Given a resource searches the tree for all matching index rules.
     *
     * @param resource
     *         The resource to be used to search the tree.
     * @return A set of matched index rules.
     */
    public Set<String> searchTree(String resource);

}
