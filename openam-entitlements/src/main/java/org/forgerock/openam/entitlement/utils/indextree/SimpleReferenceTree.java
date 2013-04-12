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

import org.forgerock.openam.entitlement.utils.indextree.nodecontext.MapSearchContext;
import org.forgerock.openam.entitlement.utils.indextree.nodecontext.SearchContext;
import org.forgerock.openam.entitlement.utils.indextree.nodefactory.BasicTreeNodeFactory;
import org.forgerock.openam.entitlement.utils.indextree.nodefactory.TreeNodeFactory;
import org.forgerock.openam.entitlement.utils.indextree.treenodes.TreeNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This implementation makes use of simple tree node references to help improve tree navigation performance and to keep
 * the memory footprint to a minimal. It does this by using tree nodes that contain basic references to its position in
 * the tree as opposed to using {@link List} for instance.
 * <p/>
 * It makes use of a factory for the node creation so that the behavior of resource evaluation can be adapted.
 * <p/>
 * A search against the tree makes a single traversal down the tree and across the passed resource string. Each
 * character of the passed resource is evaluated against nodes at the corresponding position within the tree. A
 * node that has interest in the current resource character elects itself as a candidate. Each subsequent character of
 * the resource is evaluated against the elected candidates and their child nodes. Once the tree and resource is fully
 * traversed a collection of elected tree nodes would have been populated, where these nodes have an exact match against
 * the resource or by part match by use of wildcards. The tree path of these nodes represents an index rule and if such
 * a node is an end point, represents an index rule initially added to the tree.
 *
 * @author apforrest
 */
public class SimpleReferenceTree implements IndexRuleTree {

    private final TreeNodeFactory factory;
    private final TreeNode root;

    public SimpleReferenceTree() {
        this(new BasicTreeNodeFactory());
    }

    public SimpleReferenceTree(TreeNodeFactory factory) {
        root = factory.getRootNode();
        this.factory = factory;
    }

    @Override
    public void addIndexRule(String indexRule) {
        if (indexRule == null) {
            throw new IllegalArgumentException("Pattern must not be null");
        }

        char[] rule = indexRule.toCharArray();

        TreeNode parentNode = root, childNode = root;
        int index = 0, length = rule.length;

        // Identify creation point.
        for (; index < length; index++) {

            // Search for a matching child.
            childNode = parentNode.getChild();
            while (childNode != null && childNode.getNodeValue() != rule[index]) {
                childNode = childNode.getSibling();
            }

            if (childNode == null) {
                break;
            }

            parentNode = childNode;
        }

        // Build out additional tree nodes.
        for (; index < length; index++) {
            childNode = factory.getTreeNode(rule[index]);
            childNode.setParent(parentNode);
            childNode.setSibling(parentNode.getChild());
            parentNode.setChild(childNode);
            parentNode = childNode;
        }

        // Mark child node.
        childNode.markEndPoint();
    }

    @Override
    public void addIndexRules(Collection<String> indexRules) {
        for (String indexRule : indexRules) {
            addIndexRule(indexRule);
        }
    }

    @Override
    public Set<String> searchTree(String resource) {
        if (resource == null) {
            throw new IllegalArgumentException("The search term must not be null");
        }

        char[] searchTerm = resource.toCharArray();

        List<TreeNode> candidates = new ArrayList<TreeNode>();
        // Start with the root node as the candidate.
        candidates.add(root);

        // Create a new search context for the current search.
        SearchContext context = new MapSearchContext();

        for (int i = 0; i < searchTerm.length && !candidates.isEmpty(); i++) {
            // For each character of the search term.
            searchTree(searchTerm[i], candidates, context);
        }

        Set<String> results = new HashSet<String>();
        for (TreeNode candidate : candidates) {
            if (candidate.isEndPoint()) {
                // Filter out valid index rules.
                results.add(candidate.getFullPath());
            }
        }

        return results;
    }

    /**
     * Evaluate previous candidates for reelection and their children for first election.
     *
     * @param searchTerm
     *         Current search character.
     * @param candidates
     *         Elected candidates as potential matches.
     * @param context
     *         The shared search context.
     */
    private void searchTree(char searchTerm, List<TreeNode> candidates, SearchContext context) {
        // Every candidate has to be reelected.
        List<TreeNode> previousCandidates = new ArrayList<TreeNode>(candidates);
        candidates.clear();

        for (TreeNode previousCandidate : previousCandidates) {

            if (previousCandidate.isWildcard() && previousCandidate.hasInterestIn(searchTerm, context)) {
                // Reelect previous candidate.
                candidates.add(previousCandidate);
            }

            if (previousCandidate.hasChild()) {
                // Evaluate previous candidates children.
                evaluateChildren(searchTerm, previousCandidate.getChild(), candidates, context);
            }

        }
    }

    /**
     * Evaluate each child tree node against the given value.
     *
     * @param searchTerm
     *         Current search character.
     * @param child
     *         Current tree node position.
     * @param candidates
     *         Elected candidates as potential matches.
     * @param context
     *         The shared search context.
     */
    private void evaluateChildren(char searchTerm, TreeNode child, List<TreeNode> candidates, SearchContext context) {
        while (child != null) {

            if (child.hasInterestIn(searchTerm, context)) {
                // Elect child as a candidate.
                candidates.add(child);

                if (child.isWildcard() && child.hasChild()) {
                    // This scenario handles zero or more characters.
                    evaluateChildren(searchTerm, child.getChild(), candidates, context);
                }
            }

            // Next child
            child = child.getSibling();
        }
    }

    @Override
    public String toString() {
        return String.valueOf(root);
    }

}
