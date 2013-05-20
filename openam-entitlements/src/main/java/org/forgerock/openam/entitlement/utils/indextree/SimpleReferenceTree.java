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
 * The core purpose of the tree is to identify all matching rules given a resource and it does this by the use of an
 * election approach that is iterative. Starting at the top of the tree and starting with the first character of the
 * resource, the first tree node and its immediate children are asked whether they have interest in the character. For
 * all nodes that have an interest, they are elected into the pool. This is the first iteration completed. The second
 * character is taken of the resource and this time all the previous elected nodes now in the pool are asked whether
 * they and their immediate children have interest in the character. Any previously elected node that has interest is
 * reelected and any child nodes that have interest are elected for the first time; all other previously elected nodes
 * are disregarded. The same approach is taken for each character of the resource.
 * <p />
 * Once this process has completed for all characters of the resource, any nodes that exist in the election pool will
 * have in some way matched the resource, whether explicitly or implicitly via the use of wildcards. Each node in the
 * pool is now asked whether it represents an end point and hence a previously added rule. The list of those that do
 * is the list that is returned.
 * <p/>
 * Rules are added in a tree structure, each node representing the next character within a rule. The tree node that
 * represents the last character of a rule is know as an end point; end point nodes mark this fact. The following
 * rules end up in the proceeding structure:
 * <pre>
 *     Sample urls:
 *     http://www.example.com/
 *     http://www.example.com/index.jsp
 *     http://www.test.com/home.html
 *
 *     Tree structure:
 *     http://www.example.com/
 *                            index.jsp
 *                test.com/home.html
 * </pre>
 * In the above scenario '/', 'p' and 'l' all become end points, marking in the tree where a policy rule ends. The three
 * rules equate to 85 characters, but once in the tree structure this reduces to 50 characters, giving a compression
 * just over 40% in this scenario.
 * <p/>
 * Only additions to the tree actually modify the trees structure and therefore concise synchronisation has been added
 * to this method, to ensure it's thread safe whilst not hindering the performance of the tree. Tree removes never
 * actually remove node elements but instead reduce end point markers on the matching nodes. Searches can rest assured
 * that the tree structure will always be stable, however due to the lack of synchronisation to keep performance high,
 * there's a chance that a read may retrieve stale data if an add occurs at the same time.
 * <p />
 * This implementation makes use of simple tree node references to help improve tree navigation performance and to keep
 * the memory footprint to a minimal. It does this by using tree nodes that contain basic references to its position in
 * the tree as opposed to using other structures to assist, such as {@link List}.
 * <p/>
 * It makes use of a factory for the node creation so that the behavior of resource evaluation can be adapted.
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

        StringBuilder rule = new StringBuilder(indexRule);
        TreeNode parentNode = null, childNode = null;

        // Locates the last matching node.
        TreeNode lockNode = findLastMatchingNode(root, rule);

        // Lock on the node to ensure thread safety during tree modifications.
        // FIXME: Synchronisation needs to be reconsidered as this fails with three or more threads.
        synchronized (lockNode) {

            // Verify no additional matching sub-nodes have been added during the lock.
            parentNode = childNode = findLastMatchingNode(lockNode, rule);

            // Build out additional tree nodes.
            for (int index = 0; index < rule.length(); index++) {
                childNode = factory.getTreeNode(rule.charAt(index));
                childNode.setParent(parentNode);
                childNode.setSibling(parentNode.getChild());
                parentNode.setChild(childNode);
                parentNode = childNode;
            }
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
    public void removeIndexRule(String indexRule) {
        if (indexRule == null) {
            throw new IllegalArgumentException("Pattern must not be null");
        }

        StringBuilder rule = new StringBuilder(indexRule);

        // Identify creation point.
        TreeNode parentNode = null;
        // Locates the last matching node.
        parentNode = findLastMatchingNode(root, rule);

        if (!parentNode.isRoot()) {
            parentNode.removeEndPoint();
        }
    }

    /**
     * Locates the deepest matching tree node given the passed rule.
     *
     * @param startNode
     *         The node from which to start matching.
     * @param rule
     *         The rule for comparison.
     * @return The last matching node.
     */
    private TreeNode findLastMatchingNode(TreeNode startNode, StringBuilder rule) {
        assert (startNode != null) : "The start node must not be null";

        TreeNode parentNode = startNode;
        TreeNode childNode = null;

        while (rule.length() > 0) {

            // Search for a matching child.
            childNode = parentNode.getChild();
            while (childNode != null && childNode.getNodeValue() != rule.charAt(0)) {
                childNode = childNode.getSibling();
            }

            if (childNode == null) {
                break;
            }

            // Removed matched character from the rule.
            rule.delete(0, 1);
            parentNode = childNode;
        }

        return parentNode;
    }

    @Override
    public Set<String> searchTree(String resource) {
        if (resource == null) {
            throw new IllegalArgumentException("The search term must not be null");
        }

        char[] searchTerm = resource.toCharArray();

        List<TreeNode> electionPool = new ArrayList<TreeNode>();
        // Start with the root node as the candidate.
        electionPool.add(root);

        // Create a new search context for the current search.
        SearchContext context = new MapSearchContext();

        for (int i = 0, l = searchTerm.length; i < l && !electionPool.isEmpty(); i++) {
            // For each character of the search term.
            searchTree(searchTerm[i], electionPool, context);
        }

        Set<String> results = new HashSet<String>();
        for (TreeNode candidate : electionPool) {
            if (candidate.isEndPoint()) {
                // Filter out valid index rules.
                results.add(candidate.getFullPath());
            }
        }

        return results;
    }

    /**
     * Evaluate previous electionPool for reelection and their children for first election.
     *
     * @param searchTerm
     *         Current search character.
     * @param electionPool
     *         The pool of elected candidates.
     * @param context
     *         The shared search context.
     */
    private void searchTree(char searchTerm, List<TreeNode> electionPool, SearchContext context) {
        // Every candidate has to be reelected.
        List<TreeNode> previousPool = new ArrayList<TreeNode>(electionPool);
        electionPool.clear();

        for (TreeNode previousCandidate : previousPool) {

            if (previousCandidate.isWildcard() && previousCandidate.hasInterestIn(searchTerm, context)) {
                // Reelect previous candidate.
                electionPool.add(previousCandidate);
            }

            if (previousCandidate.hasChild()) {
                // Evaluate previous electionPool children.
                evaluateChildren(searchTerm, previousCandidate.getChild(), electionPool, context);
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
     * @param electionPool
     *         The pool of elected candidates.
     * @param context
     *         The shared search context.
     */
    private void evaluateChildren(char searchTerm, TreeNode child, List<TreeNode> electionPool, SearchContext context) {
        while (child != null) {

            if (child.hasInterestIn(searchTerm, context)) {
                // Elect child as a candidate.
                electionPool.add(child);

                if (child.isWildcard() && child.hasChild()) {
                    // This scenario handles zero or more characters.
                    evaluateChildren(searchTerm, child.getChild(), electionPool, context);
                }
            }

            // Next child
            child = child.getSibling();
        }
    }

    @Override
    public String toString() {
        return root.toString(false);
    }

}
